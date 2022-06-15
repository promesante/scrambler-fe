(ns scrambler.views
  (:require
   [goog.object :as gobj]
   [clojure.string :as str]
   [reagent.core :as reagent]
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [day8.re-frame.http-fx]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [scrambler.config :as config]
   ["@material-ui/core/CssBaseline" :default CssBaseline]
   ["@material-ui/core" :refer [Link Box Typography Container Grid Avatar TextField
                                FormControlLabel Checkbox Button Card CardHeader CardContent
                                AppBar Toolbar]]
   ["@material-ui/core/styles" :refer [withStyles]]
   ["@material-ui/icons/LockOutlined" :default LockOutlinedIcon]
   ["@material-ui/icons/VpnKey" :default VpnKeyIcon]))


;;; Styles

(defn input-styles [^js/Mui.Theme theme]
  (clj->js
   {:paper {:marginTop (.spacing theme 8)
            :display "flex"
            :flexDirection "column"
            :alignItems "center"}
    :toolbar {:paddingRight 24}
    :appBar {:zIndex (+ (.. theme -zIndex -drawer) 1)
             :transition (.. theme -transitions
                             (create #js ["width" "margin"]
                                     #js {:easing (.. theme -transitions -easing -sharp)
                                          :duration (.. theme -transitions -duration -leavingScreen)}))}
    :appBarSpacer (.. theme -mixins -toolbar)
    :form {:width "100%"
           :marginTop (.spacing theme 1)}
    :cardHeader {:backgroundColor (gobj/get (.. theme -palette -grey) 200)}
    :content {:flexGrow 1
              :height "100vh"
              :overflow "auto"}
    :cardPricing {:display "flex"
                  :justifyContent "center"
                  :alignItems "baseline"
                  :marginBottom (.spacing theme 2)}
    :submit {:margin (.spacing theme 3 0 2)}}))

(def with-input-styles (withStyles input-styles))


;;; Subs

(rf/reg-sub
 :input/errors
 :<- [:errors]
 (fn [errors _]
   (:input errors)))

(rf/reg-sub
 :output/data
 (fn [db _]
   (:output db)))


;;; Events

(comment
(rf/reg-event-fx
 :input
 (fn [{:keys [db]} [_ form]]
   (if (str/includes? (:superset form) "@")
     {:db (assoc db :user (dissoc form :subset))
      :navigate! [:routes/home]}
     {:db (assoc-in db [:errors :input]
                    {:superset "Invalid Superset"})})))
)

(def request-defaults
  {:timeout 6000
   :response-format (ajax/json-response-format {:keywords? true})
   :on-failure [::set-error]})

(rf/reg-event-fx
 :input/post
 (fn [_ [_ data]]
 ; (fn [_ [_ {:keys [values]}]]
 (js/console.log "input/post - data: " data)
 (js/console.log "input/post - url: " (str config/API_URL "/scrambler/scramble"))
   ; (js/console.log "input/post - values: " values)
   {:http-xhrio (assoc request-defaults
                       :method :post
                       :uri (str config/API_URL "/scrambler/scramble")
                       :params data
                       ; :params values
                       :format (ajax/json-request-format)
                       :on-success [:output/http-success data]
                       ; :on-success [:output/http-success :output values]
                       ; :on-success [:http-success [:output :scramble?]]
                       ; :on-success [::navigate :todo-app.views/list]
                       :on-failure [:output/http-fail]
                       ; :on-failure [:http-fail [:transit :routes route-id]]
                       )}))

(rf/reg-event-db
 :output/http-success
 (fn [db [_ data result]]
   (let [scramble? (:scramble? result)
         output (merge data {:scramble? (str scramble?) :error ""})]
     (do
       (js/console.log "output/http-success - data: " data)
       (js/console.log "output/http-success - result: " result)
       (js/console.log "output/http-success - output: " output)
       (assoc db :output output)
       ))))

(rf/reg-event-db
 :output/http-fail
 (fn [db [_]]
   (assoc-in db [:output :error] "error invoking back-end api")))

(rf/reg-event-db
 :input/clear-errors
 (fn-traced [db [_ field]]
   (update-in db [:errors :input] dissoc field)))


(defn input []
  (let [empty-form {:superset "" :subset ""}
        form (reagent/atom empty-form)
        errors (rf/subscribe [:input/errors])]
    (fn [{:keys [^js classes] :as props}]
      [:div {:class (.-paper classes)}
       [:> Card
        [:> CardHeader {:title "Input"
                        :titleTypographyProps {:align "center"}
                        :subheaderTypographyProps {:align "center"}
                        :class (.-cardHeader classes)}]
        [:> CardContent
         [:div {:class (.-cardPricing classes)}
          [:form {:class (.-form classes)
                  :on-submit (fn [e]
                               (js/console.log e)
                               (.preventDefault e)
                               (rf/dispatch [:input/post @form])
                               (reset! form empty-form))
                  :no-validate true}
           [:> Grid {:container true :spacing 2}
            [:> Grid {:item true :xs 12}
             [:> TextField {:autoComplete "superset"
                            :name         "superset"
                            :variant      "outlined"
                            :required     true
                            :fullWidth    true
                            :id           "superset"
                            :label        "Superset"
                            :auto-focus true
                            :error        (boolean (:superset @errors))
                            :helperText   (:superset @errors)
                            :on-focus     #(rf/dispatch [:input/clear-errors :superset])
                            :value        (:superset @form)
                            :on-change    #(swap! form assoc :superset (-> % .-target .-value))}]]
            [:> Grid {:item true :xs 12}
             [:> TextField {:autoComplete "current-subset"
                            :name         "subset"
                            :variant      "outlined"
                            :required     true
                            :fullWidth    true
                            :id           "subset"
                            :label        "Subset"
                            :auto-focus true
                            :error        (boolean (:subset @errors))
                            :helperText   (:subset @errors)
                            :value        (:subset @form)
                            :on-focus     #(rf/dispatch [:input/clear-errors :subset])
                            :on-change    #(swap! form assoc :subset (-> % .-target .-value))}]]
            [:> Button {:type "submit"
                        :full-width true
                        :variant "contained"
                        :color "primary"
                        :class (.-submit classes)}
             "Submit"]]]]]]])))


(defn output []
  (let [data (rf/subscribe [:output/data])]
    (fn [{:keys [^js classes] :as props}]
      [:div {:class (.-paper classes)}
       [:> Card
        [:> CardHeader {:title "Output"
                        :titleTypographyProps {:align "center"}
                        :subheaderTypographyProps {:align "center"}
                        :class (.-cardHeader classes)}]
        [:> CardContent
         [:div {:class (.-cardPricing classes)}
          [:> Grid {:container true :spacing 2}
           [:> Grid {:item true :xs 6}
            [:> Typography {:component "h1" :variant "h6"} "Superset"]]
           [:> Grid {:item true :xs 6}
            [:> Typography {:component "h1" :variant "h6"} (:superset @data)]]
           [:> Grid {:item true :xs 6}
            [:> Typography {:component "h1" :variant "h6"} "Subset"]]
           [:> Grid {:item true :xs 6}
            [:> Typography {:component "h1" :variant "h6"} (:subset @data)]]
           [:> Grid {:item true :xs 6}
            [:> Typography {:component "h1" :variant "h6"} "Result"]]
           [:> Grid {:item true :xs 6}
            [:> Typography {:component "h1" :variant "h6"} (:scramble? @data)]]]]]]])))


(defn page []
  (fn [{:keys [^js classes] :as props}]
    [:> Container {:component "main" :max-width "xs"}
     [:> CssBaseline]
     [:> AppBar {:position "absolute"
                 :class [(.-appBar classes)]}
      [:> Toolbar {:class (.-toolbar classes)}
       [:> Typography {:component "h1"
                       :variant "h6"
                       :color "inherit"
                       :no-wrap true
                       :class (.-title classes)}
        "Scrambler"]]]
     [:div {:class (.-appBarSpacer classes)}]
     [:> (with-input-styles (reagent/reactify-component input))]
     [:> (with-input-styles (reagent/reactify-component output))]]))


(defn main [{:keys [router current-route]}]
  [:> (with-input-styles (reagent/reactify-component (page)))])
