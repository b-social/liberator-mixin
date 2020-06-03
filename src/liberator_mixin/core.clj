(ns liberator-mixin.core
  "Functions for defining liberator mixins: partial liberator resource
  definitions that can be composed together to build up a liberator resource
  with canned functionality.

  The most important function in this namespace is [[build-resource]] which
  accepts a sequence of liberator mixins (or resource definition maps) and
  produces a liberator resource."
  (:require
    [clojure.string :as str]

    [liberator.core :as liberator]
    [liberator.util :as liberator-util]))

(defn is-decision?
  "Returns `true` if `k`, a keyword, represents a liberator decision, `false`
  otherwise."
  [k]
  (str/ends-with? (name k) "?"))

(defn is-action?
  "Returns `true` if `k`, a keyword, represents a liberator action, `false`
  otherwise."
  [k]
  (or (= k :initialize-context) (str/ends-with? (name k) "!")))

(defn is-handler?
  "Returns `true` if `k`, a keyword, represents a liberator handler, `false`
  otherwise."
  [k]
  (str/starts-with? (name k) "handle"))

(defn is-configuration?
  "Return `true` if `k`, a keyword, represents a liberator configuration
  parameter, `false` otherwise."
  [k]
  (let [n (name k)]
    (or
      (= k :patch-content-types)
      (and (not (is-decision? k))
        (or
          (str/starts-with? n "available")
          (str/starts-with? n "allowed")
          (str/starts-with? n "known"))))))

(defn or-comparator
  [left right]
  (or left right))

(defn and-comparator
  [left right]
  (and left right))

(defn merge-decisions
  "Merges together two liberator decisions, `left` and `right`.

  Decisions can return various different shapes of result:

    - boolean, i.e., `true` or `false`
    - truthy, e.g., `{:foo :bar}` which represents `true` and is used to update
      the context
    - vector of boolean and context update, e.g., `[true, {:foo :bar}]`

  The resulting decision merges these return values in such a way that both
  the boolean result of the decision is retained and all context updates are
  made correctly.

  The decisions are applied in the order `left` first, then `right`, such that
  the `right` decision will see any context updates made by the `left`."
  [left right comparator]
  (fn [context]
    (letfn [(if-vector? [thing f]
              (if (vector? thing) (f thing) thing))
            (execute-and-update [[result context] f]
              (if (and (= comparator :and) (false? result))
                [result context]
                (let [decision (f context)
                      comparator (case comparator
                                   :and and-comparator
                                   :or or-comparator)
                      comparison (if (nil? result)
                                   (if-vector? decision first)
                                   (comparator result
                                     (if-vector? decision first)))
                      result (boolean comparison)
                      context-update (if-vector? decision second)
                      context (liberator/update-context context context-update)]
                  [result context])))]
      (-> [nil context]
        (execute-and-update (liberator-util/make-function left))
        (execute-and-update (liberator-util/make-function right))))))

(defn merge-actions
  "Merges together two liberator actions, `left` and `right`.

  The resulting action will execute both actions in the order `left` first,
  then `right`, such that the `right` action will see any context updates made
  by the `left`. The result will be that of the `right` action."
  [left right]
  (fn [context]
    (letfn [(execute-and-update [context f]
              (liberator/update-context context (f context)))]
      (let [left-result (execute-and-update context
                          (liberator-util/make-function left))
            right-result (execute-and-update left-result
                           (liberator-util/make-function right))]
        right-result))))

(defn merge-handlers
  "Merges together two liberator handlers, `left` and `right`.

  Currently, the `left` handler is discarded and the `right` is used in its
  place. In future, this may be improved such that some aspect of the `left`
  handler is retained."
  [left right]
  ; TODO: Can we do better than this
  right)

(defn merge-configurations
  "Merges together two liberator configuration parameters, `left` and `right`.

  The resulting configuration parameter will be deduced as follows:

    - If `right` includes `:replace` in its metadata, the result will be
      `right`.
    - If `left` results in a list, the result will be a list containing all
      elements from `right` followed by all elements from `left`, such that
      `right` takes precedence.
    - If `left` results in a vector, the result will be a vector containing all
      elements from `right` followed by all elements from `left`, such that
      `right` takes precedence.
    - If `left` results in a set, the result will be a set containing the union
      of `left` and `right`.
    - Otherwise, the result will be `right`.

  Both `left` and `right` can also be functions taking `context` returning
  in line with the above types."
  [left right]
  (fn merged
    ([] (merged {}))
    ([context]
     (let [left-conf ((liberator-util/make-function left) context)
           right-conf ((liberator-util/make-function right) context)]
       (cond
         (-> right-conf meta :replace)
         right-conf

         (and (list? left-conf) (coll? right-conf))
         (apply list (concat right-conf left-conf))

         (and (vector? left-conf) (coll? right-conf))
         (into right-conf left-conf)

         (and (set? left-conf) (coll? right-conf))
         (into left-conf right-conf)

         :otherwise right-conf)))))

(def or-decisions
  #{:malformed?
    :can-post-to-gone?
    :conflict?
    :existed?
    :moved-permanently?
    :moved-temporarily?
    :multiple-representations?
    :post-redirect?
    :put-to-different-url?
    :respond-with-entity?
    :uri-too-long?})

(defn get-comparator
  [decision]
  (if (contains? or-decisions decision)
    :or
    :and))

(defn merge-resource-definitions
  "Merges together multiple liberator resource definitions, specified as maps.

  For the mechanism employed:

    - for liberator decisions, see [[merge-decisions]],
    - for liberator actions, see [[merge-actions]],
    - for liberator handlers, see [[merge-handlers]],
    - for liberator configuration, see [[merge-configurations]].

  Any other map keys that do not correspond to the above liberator definition
  types will be retained in the resulting resource definition. If the same
  non-liberator definition map key is specified more than once, the rightmost
  definition takes precedence."
  [& maps]
  (let [definition-pieces (mapcat vec maps)]
    (reduce
      (fn [result [k override]]
        (if (contains? result k)
          (let [current (get result k)]
            (assoc result
              k (cond
                  (is-decision? k) (merge-decisions
                                     current
                                     override
                                     (get-comparator k))
                  (is-action? k) (merge-actions current override)
                  (is-handler? k) (merge-handlers current override)
                  (is-configuration? k) (merge-configurations current override)
                  :else override)))
          (assoc result k override)))
      {}
      definition-pieces)))

(defn build-resource
  "Builds a liberator resource from the specified resource definitions,
  specified as either maps or sequences of maps.

  This function represents the core of the mixin functionality in that
  each mixin produces either a map or a sequence of maps representing partial
  resource definitions.

  The resource definitions are merged together using
  [[merge-resource-definitions]]. See the documentation there for specific
  details of the merge process used."
  [& ms-or-seqs]
  (liberator/resource
    (apply merge-resource-definitions (flatten ms-or-seqs))))
