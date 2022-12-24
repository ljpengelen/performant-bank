# Bank

An HTTP API for managing bank accounts.

## Requirements

* [Java 8+](https://adoptium.net/)
* [Clojure](https://clojure.org/)

## Development

If you're a beginner to Clojure and don't have a favorite setup yet, give [Visual Studio Code](https://code.visualstudio.com/) in combination with the [Calva extension](https://calva.io/) a try.

Once you've installed Visual Studio Code and Calva, [connect Calva to the project](https://calva.io/connect/) using the project type `deps.edn` and the alias `:dev`, and start development.

A convenient way to get started is opening `dev/user.clj` and evaluating expressions using `alt+enter`.

## Running the app

Run `clojure -X:run` to start the app.
Clearly, this requires Clojure.

Alternatively, run `clojure -T:build clean && clojure -T:build uber` to create an uberjar, followed by `java -jar target/bank-<version>-standalone.jar` to start the application.
Starting the application like this doesn't require Clojure, only Java.

## Running tests

Execute `bin/kaocha --watch` to run tests continuously.

## Using a different server

By default, this app is configured to use [http-kit](https://github.com/http-kit/http-kit).
By changing the server type in `resources/config.edn`, you can run the app using a different web server.

| Server type     | Web server | Arity of handler function |
|-----------------|------------|---------------------------|
| :http-kit       | http-kit   | 1                         |
| :jetty-sync     | Jetty      | 1                         |
| :jetty-async    | Jetty      | 3                         |
| :undertow-sync  | Undertow   | 1                         |
| :undertow-async | Undertow   | 3                         |

Running the app using Jetty requires the profile `:jetty9` or `:jetty11` to be active.
