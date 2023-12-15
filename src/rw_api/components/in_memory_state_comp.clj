(ns rw-api.components.in-memory-state-comp
  (:require [com.stuartsierra.component :as component]))

(defrecord InMemoryStateComp [config]
  component/Lifecycle

  (start [comp]
    (println "Starting InMemoryStateComp")
    (assoc comp :state-atom (atom [])))

  (stop [comp]
    (println "Stoping InMemoryStateComp")
    (assoc comp :state-atom nil)))

(defn create-in-memory-state-comp [config]
  (map->InMemoryStateComp {:config config}))
