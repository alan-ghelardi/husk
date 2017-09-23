(ns husk.core
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [husk.specs :as specs]))

(def ^:private default-options {:encoding    "UTF-8"
                                :env-vars    (into {} (System/getenv))
                                :stdout-fn   println
                                :stderr-fn   println
                                :working-dir (io/file ".")})

(defn- read-from-stream
  [process-stream output-fn]
  (with-open [stream (io/reader process-stream)]
    (loop []
      (when-let [line (.readLine stream)]
        (output-fn line)
        (recur)))))

(defn- env-vars-map->string-array
  [env-vars]
  (->> env-vars
       (reduce-kv #(conj %1 (str %2 "=" %3)) [])
       (into-array String)))

(defn- nested-merge
  [& maps]
  (apply (partial merge-with #(if (map? %1)
                                (merge %1 %2)
                                %2)) maps))

(defn- parse-args
  [args]
  (let [result (s/conform ::specs/arguments+options args)]
    (if-not (s/invalid? result)
      result
      (throw (ex-info "Malformed args"
                      (s/explain-data ::specs/arguments+options args))))))

(defn $
  [& args]
  (let [{:keys [arguments options]}                                 (parse-args args)
        {:keys [encoding env-vars stdout-fn stderr-fn working-dir]} (nested-merge default-options options)
        args                                                        (into-array String arguments)
        process                                                     (-> (Runtime/getRuntime)
                                                                        (.exec args (env-vars-map->string-array env-vars) (io/file working-dir)))
        stdout                                                      (future (read-from-stream (.getInputStream process) stdout-fn))
        stderr                                                      (future (read-from-stream (.getErrorStream process) stderr-fn))
        exit-code                                                   (.waitFor process)]
    @stdout
    @stderr
    {:exit-code exit-code}))
