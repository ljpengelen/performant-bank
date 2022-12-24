(ns build
  (:require [clojure.tools.build.api :as b]))

(def version (format "0.1.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(defn basis [aliases]
  (b/create-basis {:aliases aliases}))
(def uber-file (format "target/bank-%s-standalone.jar" version))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [{:keys [aliases]}]
  (let [basis (basis aliases)]
    (clean nil)
    (b/copy-dir {:src-dirs ["src" "resources"]
                 :target-dir class-dir})
    (b/compile-clj {:basis basis
                    :src-dirs ["src"]
                    :class-dir class-dir})
    (b/uber {:class-dir class-dir
             :uber-file uber-file
             :basis basis
             :main 'bank.core})))
