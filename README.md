# Bank

An HTTP API for managing bank accounts.

## Requirements

* [Java 8+](https://adoptium.net/)
* [Clojure](https://clojure.org/)

## Development

If you're a beginner to Clojure and don't have a favorite setup yet, give [Visual Studio Code](https://code.visualstudio.com/) in combination with the [Calva extension](https://calva.io/) a try.

Once you've installed Visual Studio Code and Calva, [connect Calva to the project](https://calva.io/connect/) using the project type `deps.edn` and the alias `:dev`, and start development.

A convenient way to get started is opening `src/user.clj` and evaluating expressions using `alt+enter`.

## Running the app

Run `clojure -X:run` to start the app.
Obviously, this requires Clojure.

Alternatively, run `clojure -T:build clean && clojure -T:build uber` to create an uberjar, followed by `java -jar target/bank-<version>-standalone.jar` to start the application.
Starting the application like this doesn't require Clojure, only Java.
