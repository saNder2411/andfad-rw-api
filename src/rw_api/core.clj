(ns rw-api.core
  (:gen-class)
  (:require [com.stuartsierra.component :as component]
            [next.jdbc.connection :as connection]
            [rw-api.components.in-memory-state-comp :as in-memory-state-comp]
            [rw-api.components.pedestal-comp :as pedestal-comp]
            [rw-api.config :as config])
  (:import (com.zaxxer.hikari HikariDataSource)
           (org.flywaydb.core Flyway)))

(defn data-source-comp [config]
  (connection/component
   HikariDataSource
   (assoc
    (:db-spec config)
    :init-fn (fn [datasource]
               (.migrate
                (.. (Flyway/configure)
                    (dataSource datasource) 
                    (locations (into-array String ["classpath:database/migrations"]))
                    (table "schema_version")
                    (load)))))))


(defn rw-api-system [config]
  (component/system-map
   :in-memory-state-comp (in-memory-state-comp/create-in-memory-state-comp config)
   :data-source (data-source-comp config)
   :pedestal-comp (component/using
                   (pedestal-comp/create-pedestal-comp config)
                   [:data-source :in-memory-state-comp])))

(defn -main []
  (let [system (-> (config/read-config)
                   (rw-api-system)
                   (component/start-system))]
    (println "Starting Real World API")
    (.addShutdownHook (Runtime/getRuntime) (new Thread #(component/stop-system system)))))

