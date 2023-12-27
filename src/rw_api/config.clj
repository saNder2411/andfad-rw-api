(ns rw-api.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [malli.core :as m]
            [malli.error :as me]
            [malli.util :as mu]))

(defn read-config []
  (-> "config.edn"
      (io/resource)
      (aero/read-config)))

(comment
  (read-config)
  )

(comment

  (def config-schema (m/schema
                      [:map
                       [:id :string]]))
  )
