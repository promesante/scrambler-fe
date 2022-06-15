(ns scrambler.db)

(def intial-value-output "no operation run yet")

(def default-db
  {:input
   {:error ""}
   :output
   {:superset intial-value-output
    :subset intial-value-output
    :scramble? intial-value-output
    :error ""}})
