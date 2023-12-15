(ns rw-api.core
  (:gen-class)
  (:require [rw-api.config :as config]
            [rw-api.components.template-comp :as template-comp]
            [rw-api.components.pedestal-comp :as pedestal-comp]
            [rw-api.components.in-memory-state-comp :as in-memory-state-comp]
            [com.stuartsierra.component :as component]))


(defn rw-api-system [config]
  (component/system-map
   :example-comp (template-comp/create-template-comp config)
   :in-memory-state-comp (in-memory-state-comp/create-in-memory-state-comp config)
   :pedestal-comp (component/using
                   (pedestal-comp/create-pedestal-comp config)
                   [:example-comp :in-memory-state-comp])))

(defn -main []
  (let [system (-> (config/read-config)
                   (rw-api-system)
                   (component/start-system))]
    (println "Starting Real World API")
    (.addShutdownHook (Runtime/getRuntime) (new Thread #(component/stop-system system)))))
