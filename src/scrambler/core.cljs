(ns scrambler.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   ["@material-ui/core" :refer [ThemeProvider createMuiTheme]]
   [scrambler.events :as events]
   [scrambler.views :as views]
   [scrambler.config :as config]))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))


;;; Styles

(def custom-theme
  (createMuiTheme (clj->js {:palette {:type "light"}
                            :status {:danger "red"}})))

;;; Views

(defn main-shell []
  [:> ThemeProvider {:theme custom-theme}
   [views/main]])


;;; Core

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [main-shell] root-el)))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
