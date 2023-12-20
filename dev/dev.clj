(ns dev
  (:require [com.stuartsierra.component.repl :as component-repl]
            [rw-api.core :as rw-api]))

(component-repl/set-init
 (fn [_old-system]
   (rw-api/rw-api-system {:server {:port 3001}
                          :htmx {:server {:port 3002}}
                          :db-spec {:jdbcUrl "jdbc:postgresql://localhost:5432/rwa"
                                    :username "rwa"
                                    :password "rwa"}})))

(comment
  (component-repl/reset))
