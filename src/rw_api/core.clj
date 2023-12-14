(ns rw-api.core
  (:gen-class)
  (:require [rw-api.config :as config]
            [rw-api.components.example-comp :as example-comp]
            [rw-api.components.pedestal-comp :as pedestal-comp]
            [com.stuartsierra.component :as component]))


(defn rw-api-system [config]
  (component/system-map
   :example-comp (example-comp/create-example-comp config)
   :pedestal-comp (component/using
                   (pedestal-comp/create-pedestal-comp config)
                   [:example-comp])))

(defn -main []
  (let [system (-> (config/read-config)
                   (rw-api-system)
                   (component/start-system))]
    (println "Starting Real World API")
    (.addShutdownHook (Runtime/getRuntime) (new Thread #(component/stop-system system)))))
