(defproject b-social/liberator-mixin "0.0.65"
  :description "An extension to liberator allowing for composable mixins."
  :url "https://github.com/b-social/liberator-mixin"

  :license {:name "The MIT License"
            :url  "https://opensource.org/licenses/MIT"}

  :dependencies [[b-social/jason "0.1.7"]
                 [buddy/buddy-auth "3.0.1"]
                 [liberator "0.15.3"]
                 
                 [b-social/hype "1.0.0"]
                 
                 [com.auth0/java-jwt "3.18.2"
                  :exclusions [com.fasterxml.jackson.core/jackson-databind]]

                 ;; we only need halboy for halboy.resource and halboy.json
                 ;; but it pulls in a lot of extra deps and also
                 ;; we let buddy-auth pull in more recent version of cheshire
                 [halboy "5.1.1"
                  :exclusions [http-kit
                               http-kit.fake
                               tigris
                               cheshire
                               org.eclipse.jetty/jetty-server]]
                 
                 ;; brought in by cheshire for buddy-auth
                 ;; picking same version of jackson libs
                 ;; as in b-social/jason to avoid compat issues
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-cbor "2.17.2"]
                 [com.fasterxml.jackson.dataformat/jackson-dataformat-smile "2.17.2"]
                 
                 ;; force upgrade from 1.68 pulled in by buddy-auth to fix vulnerability
                 [org.bouncycastle/bcprov-jdk15on "1.69"]]

  :plugins [[lein-cloverage "1.0.13"]
            [lein-shell "0.5.0"]
            [lein-ancient "0.6.15"]
            [lein-changelog "0.3.2"]
            [lein-eftest "0.5.8"]
            [lein-codox "0.10.7"]
            [lein-cljfmt "0.6.4"]
            [lein-kibit "0.1.6"]
            [lein-bikeshed "0.5.1"]]

  :profiles {:shared {:dependencies
                      [[org.clojure/clojure "1.11.3"]
                       [camel-snake-kebab "0.4.3"]
                       [ring/ring-core "1.12.1"]
                       [ring/ring-mock "0.4.0"
                        :exclusions [cheshire]]
                       [eftest "0.5.9"]]}
             :dev    [:shared {:source-paths ["dev"]
                               :eftest       {:multithread? false}}]
             :test   [:shared {:eftest {:multithread? false}}]}

  :cloverage
  {:ns-exclude-regex [#"^user"]}

  :codox
  {:namespaces  [#"^liberator-mixin\."]
   :metadata    {:doc/format :markdown}
   :output-path "docs"
   :doc-paths   ["docs"]
   :source-uri  "https://github.com/b-social/liberator-mixin/blob/{version}/{filepath}#L{line}"}

  :cljfmt {:indents ^:replace {#".*" [[:inner 0]]}}

  :deploy-repositories
  {"releases" {:url "https://repo.clojars.org" :creds :gpg}}

  :release-tasks
  [["shell" "git" "diff" "--exit-code"]
   ["change" "version" "leiningen.release/bump-version" "release"]
   ["codox"]
   ["changelog" "release"]
   ["shell" "sed" "-E" "-i" "" "s/\"[0-9]+\\.[0-9]+\\.[0-9]+\"/\"${:version}\"/g" "README.md"]
   ["shell" "git" "add" "."]
   ["vcs" "commit"]
   ["vcs" "tag"]
   ["deploy"]
   ["change" "version" "leiningen.release/bump-version"]
   ["vcs" "commit"]
   ["vcs" "tag"]
   ["vcs" "push"]]

  :aliases {"test"      ["with-profile" "test" "eftest" ":all"]
            "precommit" ["do"
                         ["check"]
                         ["kibit" "--replace"]
                         ["cljfmt" "fix"]
                         ["with-profile" "test" "bikeshed"
                          "--name-collisions" "false"
                          "--verbose" "true"]
                         ["test"]]})
