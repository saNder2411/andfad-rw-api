(ns dev
  (:require [com.stuartsierra.component.repl :as component-repl]
            [rw-api.core :as rw-api]))

(component-repl/set-init (fn [_old-system]
                           (rw-api/rw-api-system {:server {:port 3001}})))

(comment
  (component-repl/reset))
