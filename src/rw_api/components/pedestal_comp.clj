(ns rw-api.components.pedestal-comp
  (:require [cheshire.core :as json]
            [com.stuartsierra.component :as component]
            [honey.sql :as sql]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [schema.core :as schema]))

(defn response
  ([status body]
   (merge
    {:status status
     :headers {"Content-Type" "application/json"}}
    (when body {:body (json/encode body)})))

  ([status] (response status nil)))

(def ok (partial response 200))

(def created (partial response 201))

(def not-found (partial response 404))

(defn save-todo! [{:keys [in-memory-state-comp]} todo]
  (swap! (:state-atom in-memory-state-comp)  conj todo))

(defn get-todo-by-id [{:keys [in-memory-state-comp]} id]
  (->> @(:state-atom in-memory-state-comp)
       (filter #(= (:id %) id))
       (first)))

(def get-todo-handler
  {:name :get-todo-handler
   :enter (fn [{:keys [dependencies] :as context}]
            (let [request (:request context)
                  todo (get-todo-by-id dependencies (-> request :path-params :todo-id))
                  response (if todo (ok todo) (not-found))]
              (assoc context :response response)))})


(def db-get-todo-handler
  {:name :db-get-todo-handler
   :enter (fn [{:keys [dependencies] :as context}]
            (let [{:keys [data-source]} dependencies
                  request (:request context)
                  todo-id (-> request :path-params :todo-id (parse-uuid))
                  select-query (-> {:select :*
                                    :from :todo
                                    :where [:= :todo-id todo-id]}
                                   (sql/format))
                  todo (jdbc/execute-one!
                        (data-source) select-query {:builder-fn rs/as-unqualified-kebab-maps})
                  response (if todo (ok todo) (not-found))]
              (assoc context :response response)))})


(schema/defschema TodoItem
  {:id schema/Str
   :title schema/Str
   :status schema/Str})

(schema/defschema Todo
  {:id schema/Str
   :title schema/Str
   :items [TodoItem]})


(def post-todo-handler
  {:name :post-todo-handler
   :enter (fn [{:keys [dependencies] :as context}]
            (let [request (:request context)
                  todo (schema/validate Todo (:json-params request))]
              (save-todo! dependencies todo)
              (assoc context :response (created todo))))})

(def info-handler
  {:name :info-handler
   :enter (fn [{:keys [dependencies] :as context}]
            (let [{:keys [data-source]} dependencies
                  db-res (first (jdbc/execute! (data-source) ["SHOW SERVER_VERSION"]))
                  body (str "Database server version: " (:server_version db-res))]
              (assoc context :response {:status 200 :body body})))})

(defn respond-hello [_]
  {:status 200 :body "Hi youtube!"})

(def routes (route/expand-routes
             #{["/greet" :get respond-hello :route-name :greet]
               ["/info" :get info-handler :route-name :info]
               ["/todo/:todo-id" :get get-todo-handler :route-name :get-todo]
               ["/todo" :post [(body-params/body-params) post-todo-handler] :route-name :post-todo]
               ["/db/todo/:todo-id" :get db-get-todo-handler :route-name :db-get-todo]}))

(def url-for (route/url-for-routes routes))

(defn inject-dependencies [dependencies]
  (interceptor/interceptor
   {:name ::inject-dependencies
    :enter #(assoc % :dependencies dependencies)}))

(def content-negotiation-interceptor
  (content-negotiation/negotiate-content ["text/html"
                                          "application/json"]))

(defrecord PedestalComp [config data-source in-memory-state-comp]
  component/Lifecycle

  (start [comp]
    (println "Starting PedestalComp!!")
    (let [server (-> {::http/routes routes
                      ::http/type :jetty
                      ::http/join? false
                      ::http/port (-> config :server :port)}
                     (http/default-interceptors)
                     (update ::http/interceptors concat [(inject-dependencies comp)
                                                         content-negotiation-interceptor])
                     (http/create-server)
                     (http/start))]
      (assoc comp :server server)))

  (stop [comp]
    (println "Stopping PedestalComp")
    (when-let [server (:server comp)]
      (http/stop server))
    (assoc comp :server nil)))

(defn create-pedestal-comp [config]
  (map->PedestalComp {:config config}))



;; PedestalComp

;; {:config {:server {:port 50255}},

;;  :in-memory-state-comp
;;  {:config {:server {:port 50255}}
;;   :state-atom #atom[[{:id #uuid "8e4265ec-8e43-45e6-9b69-9715b67d0583"
;;                       :name "Todo for test"
;;                       :items [{:id #uuid "21c9396e-4dd2-4b07-8225-d3b19d5e62fb"
;;                                :name "Finish the test"}]}]
;;                     0xe979f8]}}