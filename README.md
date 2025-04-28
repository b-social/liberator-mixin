# liberator-mixin

An extension to liberator allowing for composable mixins.

## Upgrade to 1.x

The change to version 1.x was done to denote a breaking change rather than signal a newly achieved degree of stability. The hal.core functionality that implements Representation now requires a `discovery-url-fn` function to be included in the context if you want it to automatically add a discovery url to the resource. This was done to avoid tying this library to the hype library, and thus to to the bidi router.

To maintain prior behaviour, apply the following change to your code following the upgrade. Add this new mixin and include it as part of your resource:

```clojure
(defn with-discovery-url-fn []
  {:initialize-context
   (fn [{:keys [request routes]}]
     {:discovery-url-fn #(hype/absolute-url-for request routes :discovery)})})
```


## Install

Add the following to your `project.clj` file:

```clj
[b-social/liberator-mixin "0.0.65"]
```

## Documentation

* [API Docs](http://b-social.github.io/liberator-mixin)

## Contributing

### Testing

Run tests using:

```
lein test
```

### Releasing a new version

Prerequisites:
- You must have git configured with your GPG credentials (https://git-scm.com/book/en/v2/Git-Tools-Signing-Your-Work).
- You must have lein configured with your Clojars credentials (instructions: https://github.com/technomancy/leiningen/blob/master/doc/DEPLOY.md#gpg).
- You must be a member of the b-social group on Clojars.

You should then be able to run:
```
lein release
```

This will handle changing the versions everywhere, and adding the version tags.
It will deploy the latest to Clojars and push all the changes to GitHub.

## License

Copyright © 2018 B-Social Ltd.

Distributed under the terms of the 
[MIT License](http://opensource.org/licenses/MIT).
