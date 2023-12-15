(ns rw-api.components.template-comp
  (:require [com.stuartsierra.component :as component]))

(defrecord TemplateComponent [config]
  component/Lifecycle

  (start [comp]
    (println "Starting TemplateComponent")
    (assoc comp :state ::started))

  (stop [comp]
    (println "Stoping TemplateComponent")
    (assoc comp :state nil)))

(defn create-template-comp [config]
  (map->TemplateComponent {:config config}))
