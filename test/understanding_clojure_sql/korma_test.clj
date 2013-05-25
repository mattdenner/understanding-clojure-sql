(ns understanding-clojure-sql.korma-test
  (:use midje.sweet))

; Let's make the Korma stuff explicit so that we can see what lies where
(require '[korma.core :as dml]
         '[korma.db   :as dml-db])

; You cannot '(:use lobos.connectivity)' because it causes clashes with the korma.db namespace.
(require '[lobos.core         :as ddl-core]
         '[lobos.schema       :as ddl-schema]
         '[lobos.connectivity :as ddl-connect])

; Now we can ensure that anything we do runs within the context of an in-memory H2 database.
(defn with-test-db [callback]
  (let [db-details (dml-db/h2 {:db "mem:test_mem"})]
    ; Both lobos and korma need a connection, but they won't cooperate in how they do this by
    ; the looks of it.  Therefore we have to do both, although I guess JNDI would be the common
    ; solution.
    (ddl-connect/open-global :lobos-connection db-details)
    (dml-db/defdb db db-details)

    ; Now we can execute the callback with lobos and korma configured
    (ddl-connect/with-connection :lobos-connection (callback db))
    )
  )

(defn korma-test [db]
  ; We need a DB schema setup for korma so lobos can do that for us:
  (ddl-core/create
    (ddl-schema/table :entries
                      (ddl-schema/integer :id :primary-key)
                      (ddl-schema/varchar :name 100))) 

  ; Korma allows you to define an "entity" which represents a table in the DB.  It reflects
  ; the Rails ActiveRecord in some ways in that it allows you to follow a convention for the
  ; column names and get a lot for free.
  (dml/defentity entries
    (dml/database db))

  ; Once you have an entity you can do some DB stuff ...
  (facts "insert"
         (dml-db/transaction
           (fact "inserts one or more entries"
                 (-> (dml/insert* entries) (dml/values {:id 1, :name "foo"}) (dml/insert))                        => nil
                 (-> (dml/insert* entries) (dml/values [{:id 2, :name "bar"} {:id 3, :name "bar"}]) (dml/insert)) => nil)

           (dml-db/rollback))
         )

  (facts "select"
         (dml-db/transaction
           (-> (dml/insert* entries)
               (dml/values [{:id 1, :name "foo"}  {:id 2, :name "bar"} {:id 3, :name "baz"}])
               (dml/insert))

           (fact "can be built by threading"
                 (-> (dml/select* entries) (dml/as-sql))                             => "SELECT \"entries\".* FROM \"entries\""
                 (-> (dml/select* entries) (dml/select))                             => [{:id 1, :name "foo"} {:id 2, :name "bar"} {:id 3, :name "baz"}]
                 (-> (dml/select* entries) (dml/order :id :desc) (dml/select))       => [{:id 3, :name "baz"} {:id 2, :name "bar"} {:id 1, :name "foo"}]
                 (-> (dml/select* entries) (dml/where (= :name "foo")) (dml/select)) => [{:id 1, :name "foo"}])

           (dml-db/rollback))
         )
  )

(with-test-db korma-test)
