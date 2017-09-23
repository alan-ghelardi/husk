(ns husk.specs
  (:require [clojure.spec.alpha :as s]))

(s/def ::arguments (s/* string?))

(s/def ::encoding string?)

(s/def ::env-vars (s/map-of string? string? :min-count 1))

(s/def ::working-dir string?)

(s/def ::stdout-fn fn?)

(s/def ::stderr-fn fn?)

(s/def ::options (s/keys* :opt-un [::encoding ::env-vars ::working-dir ::stdout-fn ::stderr-fn]))

(s/def ::arguments+options (s/cat :arguments ::arguments :options ::options))
