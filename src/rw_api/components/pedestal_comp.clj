(ns rw-api.components.pedestal-comp
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]))

(defn response [status body & {:as headers}]
  {:status status :body body :headers headers})

(def ok (partial response 200))

(defn get-todo-by-id [{:keys [in-memory-state-comp]} id]
  (->> @(:state-atom in-memory-state-comp)
       (filter #(= (:id %) id))
       (first)))

(def get-todo-handler
  {:name :get-todo-handler
   :enter (fn [{:keys [dependencies] :as context}]
            (let [request (:request context)
                  response (ok
                            (get-todo-by-id dependencies
                                            (-> request
                                                :path-params
                                                :todo-id
                                                (parse-uuid))))]
              (assoc context :response response)))})

(comment
  [{:id (random-uuid)
    :name "List Name"
    :items [{:id (random-uuid)
             :name "Item Name"
             :status :created}]}
   {:id (random-uuid)
    :name "List Name"
    :items []}])

(defn respond-hello [_]
  {:status 200 :body "Hi youtube!"})

(def routes (route/expand-routes
             #{["/greet" :get respond-hello :route-name :greet]
               ["/todo/:todo-id" :get get-todo-handler :route-name :get-todo]}))

(defn inject-dependencies [dependencies]
  (interceptor/interceptor
   {:name ::inject-dependencies
    :enter #(assoc % :dependencies dependencies)}))

(defrecord PedestalComp [config example-comp in-memory-state-comp]
  component/Lifecycle

  (start [component]
    (println "Starting PedestalComp")
    (let [server (-> {::http/routes routes
                      ::http/type :jetty
                      ::http/join? false
                      ::http/port (-> config :server :port)}
                     (http/default-interceptors)
                     (update ::http/interceptors concat [(inject-dependencies component)])
                     (http/create-server)
                     (http/start))]
      (assoc component :server server)))

  (stop [component]
    (println "Stoping PedestalComp")
    (when-let [server (:server component)]
      (http/stop server))
    (assoc component :server nil)))

(defn create-pedestal-comp [config]
  (map->PedestalComp {:config config}))

