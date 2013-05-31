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
    (ddl-connect/open-global :korma-test-connection db-details)
    (dml-db/defdb db db-details)

    ; Now we can execute the callback with lobos and korma configured
    (ddl-connect/with-connection :korma-test-connection (callback db))
    )
  )

(defn korma-test [db]
  ; We need a DB schema setup for korma so lobos can do that for us:
  (ddl-core/create
    (ddl-schema/table :owners
                      (ddl-schema/integer :id :primary-key)
                      (ddl-schema/varchar :name 100)))
  (ddl-core/create
    (ddl-schema/table :pets
                      (ddl-schema/integer :id :primary-key)
                      (ddl-schema/integer :owners_id [:refer :owners :id])
                      (ddl-schema/varchar :name 100)))

  ; Because you want to refer to entities from entities you need to declare them
  (declare owner pet)

  ; Korma allows you to define an "entity" which represents a table in the DB.  It reflects
  ; the Rails ActiveRecord in some ways in that it allows you to follow a convention for the
  ; column names and get a lot for free.
  (dml/defentity owner
    (dml/database db)
    (dml/table :owners)
    (dml/has-many pet))
  (dml/defentity pet
    (dml/database db)
    (dml/table :pets)
    (dml/belongs-to owner))

  ; Once you have an entity you can do some DB stuff ...
  (facts "insert"
         (dml-db/transaction
           (fact "inserts one or more entries"
                 (-> (dml/insert* owner) (dml/values {:id 1, :name "foo"}) (dml/insert))                        => nil
                 (-> (dml/insert* owner) (dml/values [{:id 2, :name "bar"} {:id 3, :name "bar"}]) (dml/insert)) => nil)

           (dml-db/rollback))
         )

  (facts "select"
         (dml-db/transaction
           (-> (dml/insert* owner)
               (dml/values [{:id 1, :name "foo"}  {:id 2, :name "bar"} {:id 3, :name "baz"}])
               (dml/insert))
           (-> (dml/insert* pet)
               (dml/values [{:id 1, :owners_id 1, :name "frood"}])
               (dml/insert)
               )

           (fact "can be built by threading"
                 (-> (dml/select* owner) (dml/as-sql))                             => "SELECT \"owners\".* FROM \"owners\""
                 (-> (dml/select* owner) (dml/select))                             => [{:id 1, :name "foo"} {:id 2, :name "bar"} {:id 3, :name "baz"}]
                 (-> (dml/select* owner) (dml/order :id :desc) (dml/select))       => [{:id 3, :name "baz"} {:id 2, :name "bar"} {:id 1, :name "foo"}]
                 (-> (dml/select* owner) (dml/where (= :name "foo")) (dml/select)) => [{:id 1, :name "foo"}])

           (facts "can include joins"
                  (fact "with belongs-to or has-one use fields to rename the joined field names"
                        (-> (dml/select* pet) (dml/with owner (dml/fields :name)) (dml/select))                => [{:id 1, :name "frood", :owners_id 1, :name_2 "foo"}]
                        (-> (dml/select* pet) (dml/with owner (dml/fields [:name :owners_name])) (dml/select)) => [{:id 1, :name "frood", :owners_id 1, :owners_name "foo"}]
                        )
                  (fact "with has-many you automatically get an array"
                        (-> (dml/select* owner) (dml/with pet) (dml/select)) => [
                                                                                 {:id 1, :name "foo", :pets [{:id 1, :name "frood", :owners_id 1}]}
                                                                                 {:id 2, :name "bar", :pets []}
                                                                                 {:id 3, :name "baz", :pets []}
                                                                                 ]
                        )
                 )

           (dml-db/rollback))
         )
  )

(with-test-db korma-test)
