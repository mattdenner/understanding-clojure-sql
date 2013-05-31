; The migrations have to be defined in a special namespace in order that
; Lobos can pick them up.  We'll leave them here for the moment
(ns lobos.migrations)

(require '[lobos.core         :as ddl-core]
         '[lobos.schema       :as ddl-schema]
         '[lobos.migration    :as ddl-migrate]
         )

; Migrations allow us to build a DB in a managed way, just like the Rails migrations
(ddl-migrate/defmigration create-table-1
  (up   (ddl-core/create (ddl-schema/table :table-1 (ddl-schema/integer :id :primary-key))))
  (down (ddl-core/drop   (ddl-schema/table :table-1))))
(ddl-migrate/defmigration create-table-2
  (up   (ddl-core/create (ddl-schema/table :table-2 (ddl-schema/integer :id :primary-key))))
  (down (ddl-core/drop   (ddl-schema/table :table-2))))
(ddl-migrate/defmigration create-table-3
  (up   (ddl-core/create (ddl-schema/table :table-3 (ddl-schema/integer :id :primary-key))))
  (down (ddl-core/drop   (ddl-schema/table :table-3))))
