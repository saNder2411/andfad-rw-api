(ns rw-api.components.pedestal-comp
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]))

(defn respond-hello [_]
  {:status 200 :body "Hi youtube!"})

(def routes (route/expand-routes
             #{["/greet" :get respond-hello :route-name :greet]}))

(defrecord PedestalComp [config example-comp]
  component/Lifecycle

  (start [component]
    (println "Starting PedestalComp!")
    (let [server (-> {::http/routes routes
                      ::http/type :jetty
                      ::http/join? false
                      ::http/port (-> config :server :port)}
                     (http/create-server)
                     (http/start))]
      (assoc component :server server)))

  (stop [component]
    (println "Stoping PedestalComp!")
    (when-let [server (:server component)]
      (http/stop server))
    (assoc component :server nil)))

(defn create-pedestal-comp [config]
  (map->PedestalComp {:config config}))

