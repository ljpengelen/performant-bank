(ns bank.async
  (:require [clojure.core.async :as async]))

(defn throw-err [v]
  (if (instance? java.lang.Throwable v) (throw v) v))

(defmacro <? [c]
  `(throw-err (async/<! ~c)))

(defmacro <?? [c]
  `(throw-err (async/<!! ~c)))

(defmacro with-channel [[channel-name] & body]
  `(let [~channel-name (async/chan)]
     (async/go
       (try
         ~@body
         (catch Exception e#
           (async/put! ~channel-name e#))))
     ~channel-name))
