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

## Some remarks

* For now, I'm ignoring the fact that one of the general requirements is that the API should service 1000 concurrent requests per seconds *asynchronously*.
  I'll revisit this later to see how asynchronous processing would affect performance in practice because I am curious about the results.
  I didn't want to spend too much time evaluating tooling initially and stuck to what I knew.
  I did use `core.async` for an earlier hobby project, however: https://github.com/ljpengelen/clojure-energy.
* SQLite isn't my go-to database.
  Usually, I'd start with Postgres running in a Docker container.
  However, I don't have any images for Postgres on my laptop at the moment, and I'm working on this project from France, without a proper internet connnection.
  I didn't want to spend all my mobile data on downloading Docker images, so I decided to see how far SQLite would take this app.

  ~~Initially, SQLite seemed to perform quite well (`dev/benchmark.clj`), but some more testing indicates that it's probably not fit for this job (`dev/load_test.clj`).`~~

  With a pool size of one, a throughput of 1000 requests per second is achievable using SQLite (`dev/load_test.clj`).

  Some additional load testing with Postgres indicates a lower throughput of about half the number of requests per second.


* I could have written a lot more tests, but I didn't feel like it.
  Although I think that a proper test suite is a necessity for production-grade software, I hardly ever write tests for the applications I write using Clojure.
  Those applications are usually hobby projects, prototypes, etc. for which extensive testing doesn't make much sense.
  As a result, my experience with testing Clojure applications is rather limited, but I know the basics.
  This is probably reflected by the few tests that I did write.
* At least three of the handlers I wrote contain too much business logic and are hardly unit-testable.
  I'll revisit those later.
  I was curious to perform some load tests on the app, so I wanted to get it into a working state as soon as possible.
  Now that I know how http-kit and SQLite perform together, I'm ready to do some spring cleaning.
