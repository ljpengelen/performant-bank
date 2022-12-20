(ns bank.async
  (:require [clojure.core.async :as async]))

(defn throw-err [v]
  (if (instance? java.lang.Throwable v) (throw v) v))

(defmacro <? [c]
  `(throw-err (async/<! ~c)))

(defmacro <?? [c]
  `(throw-err (async/<!! ~c)))
