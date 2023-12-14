(ns rw-api.components.example-comp
  (:require [com.stuartsierra.component :as component]))

(defrecord ExampleComponent [config]
  component/Lifecycle

  (start [component]
    (println "Starting ExampleComponent!")
    (assoc component :state ::started))

  (stop [component]
    (println "Stoping ExampleComponent!")
    (assoc component :state nil)))

(defn create-example-comp [config]
  (map->ExampleComponent {:config config}))
