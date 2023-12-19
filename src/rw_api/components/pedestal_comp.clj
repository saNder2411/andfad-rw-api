(ns rw-api.components.pedestal-comp
  (:require [com.stuartsierra.component :as component]
            [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.interceptor :as interceptor]
            [io.pedestal.http.content-negotiation :as content-negotiation]
            [io.pedestal.http.body-params :as body-params]
            [cheshire.core :as json]
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

(defn get-todo-by-id [{:keys [in-memory-state-comp]} id]
  (->> @(:state-atom in-memory-state-comp)
       (filter #(= (:id %) id))
       (first)))

(defn save-todo! [{:keys [in-memory-state-comp]} todo]
  (swap! (:state-atom in-memory-state-comp)  conj todo))

(def get-todo-handler
  {:name :get-todo-handler
   :enter (fn [{:keys [dependencies] :as context}]
            (let [request (:request context)
                  todo (get-todo-by-id dependencies (-> request :path-params :todo-id))
                  response (if todo
                             (ok todo)
                             (not-found))]
              (assoc context :response response)))})


(schema/defschema TodoItem
  {:id schema/Str
   :name schema/Str
   :status schema/Str})

(schema/defschema Todo
  {:id schema/Str
   :name schema/Str
   :items [TodoItem]})


(def post-todo-handler
  {:name :post-todo-handler
   :enter (fn [{:keys [dependencies] :as context}]
            (let [request (:request context)
                  todo (schema/validate Todo (:json-params request))]
              (save-todo! dependencies todo)
              (assoc context :response (created todo))))})


(defn respond-hello [_]
  {:status 200 :body "Hi youtube!"})

(def routes (route/expand-routes
             #{["/greet" :get respond-hello :route-name :greet]
               ["/todo/:todo-id" :get get-todo-handler :route-name :get-todo]
               ["/todo" :post [(body-params/body-params) post-todo-handler] :route-name :post-todo]}))

(def url-for (route/url-for-routes routes))

(defn inject-dependencies [dependencies]
  (interceptor/interceptor
   {:name ::inject-dependencies
    :enter #(assoc % :dependencies dependencies)}))

(def content-negotiation-interceptor
  (content-negotiation/negotiate-content ["text/html"
                                          "application/json"]))

(defrecord PedestalComp [config example-comp in-memory-state-comp]
  component/Lifecycle

  (start [comp]
    (println "Starting PedestalComp")
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
    (println "Stoping PedestalComp")
    (when-let [server (:server comp)]
      (http/stop server))
    (assoc comp :server nil)))

(defn create-pedestal-comp [config]
  (map->PedestalComp {:config config}))



;; PedestalComp

;; {:config {:server {:port 50255}},
;;  :example-comp
;;  {:config {:server {:port 50255}}
;;   :state :started}

;;  :in-memory-state-comp
;;  {:config {:server {:port 50255}}
;;   :state-atom #atom[[{:id #uuid "8e4265ec-8e43-45e6-9b69-9715b67d0583"
;;                       :name "Todo for test"
;;                       :items [{:id #uuid "21c9396e-4dd2-4b07-8225-d3b19d5e62fb"
;;                                :name "Finish the test"}]}]
;;                     0xe979f8]}}