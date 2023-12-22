(defproject real-world-api "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]

                 [aero "1.1.6"]

                 [io.pedestal/pedestal.service "0.6.1"]
                 [io.pedestal/pedestal.route "0.6.1"]
                 [io.pedestal/pedestal.jetty "0.6.1"]

                 [org.slf4j/slf4j-simple "2.0.9"]

                 [com.stuartsierra/component "1.1.0"]
                 [com.stuartsierra/component.repl "0.2.0"]

                 [clj-http "3.12.3"]
                 [cheshire "5.12.0"]
                 [prismatic/schema "1.4.1"]

                 [org.testcontainers/testcontainers "1.19.3"]
                 [org.testcontainers/postgresql "1.19.3"]

                 [com.github.seancorfield/next.jdbc "1.3.909"]
                 [org.postgresql/postgresql "42.7.1"]

                 [com.zaxxer/HikariCP "5.1.0"]
                 [hiccup/hiccup "2.0.0-RC2"]
                 [org.flywaydb/flyway-core "9.21.2"]
                 [com.github.seancorfield/honeysql "2.5.1103"]
                 [faker "0.3.2"]]
  :main ^:skip-aot rw-api.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
