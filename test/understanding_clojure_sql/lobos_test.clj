(ns understanding-clojure-sql.lobos-test
  (:use midje.sweet))

; Use korma for the DB connection stuff
(require '[korma.core :as dml]
         '[korma.db   :as dml-db])

; You cannot '(:use lobos.connectivity)' because it causes clashes with the korma.db namespace.
(require '[lobos.core         :as ddl-core]
         '[lobos.schema       :as ddl-schema]
         '[lobos.connectivity :as ddl-connect]
         '[lobos.migration    :as ddl-migrate]
         )

(defn with-test-db [callback]
  (let [details (dml-db/h2 {:db "mem:test_lobos"})]
    (ddl-connect/open-global :lobos-test-connection details)
    (ddl-connect/with-connection :lobos-test-connection (callback))))

; Returns all of the tables that we care
(defn dump-tables-after [callback]
  (clojure.set/intersection #{"table-1" "table-2" "table-3"}
                            (set (map :TABLE_NAME (dml/exec-raw "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES" :results)))))

(defn lobos-test []
  (facts "lobos"
         (facts "migrations"
                (fact "all migrations can be executed at once"
                      (dump-tables-after (ddl-core/migrate))       => #{"table-1" "table-2" "table-3"}
                      (dump-tables-after (ddl-core/rollback :all)) => #{})

                (facts "up"
                       (fact "by name"
                             (dump-tables-after (ddl-core/migrate "create-table-1")) => #{"table-1"}
                             (dump-tables-after (ddl-core/migrate "create-table-2")) => #{"table-1" "table-2"}
                             (dump-tables-after (ddl-core/migrate "create-table-3")) => #{"table-1" "table-2" "table-3"}
                             (dump-tables-after (ddl-core/rollback :all)) => #{})

                       (fact "by name and a different order"
                             (dump-tables-after (ddl-core/migrate "create-table-2")) => #{"table-2"}
                             (dump-tables-after (ddl-core/migrate "create-table-1")) => #{"table-1" "table-2"}
                             (dump-tables-after (ddl-core/migrate "create-table-3")) => #{"table-1" "table-2" "table-3"}
                             (dump-tables-after (ddl-core/rollback :all)) => #{})
                       )

                (facts "down"
                       (fact "you can rollback migrations piecemeal"
                             (dump-tables-after (ddl-core/migrate))  => #{"table-1" "table-2" "table-3"}
                             (dump-tables-after (ddl-core/rollback)) => #{"table-1" "table-2"}
                             (dump-tables-after (ddl-core/rollback)) => #{"table-1"}
                             (dump-tables-after (ddl-core/rollback)) => #{})

                       (fact "you can rollback a number of migrations"
                             (dump-tables-after (ddl-core/migrate))    => #{"table-1" "table-2" "table-3"}
                             (dump-tables-after (ddl-core/rollback 2)) => #{"table-1"}
                             (dump-tables-after (ddl-core/rollback))   => #{})

                       (fact "rollback can be by name"
                             (dump-tables-after (ddl-core/migrate))                   => #{"table-1" "table-2" "table-3"}
                             (dump-tables-after (ddl-core/rollback "create-table-2")) => #{"table-1" "table-3"}
                             (dump-tables-after (ddl-core/rollback "create-table-1")) => #{"table-3"}
                             (dump-tables-after (ddl-core/rollback "create-table-3")) => #{})
                       )
                )
         )
  )

(with-test-db lobos-test)
