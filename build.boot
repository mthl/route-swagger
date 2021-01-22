(set-env!
 :source-paths   #{"src"}
 :resource-paths #{}
 :dependencies
 '[[org.clojure/clojure "1.10.1"]

   [metosin/ring-swagger "0.26.2" :exclusions [frankiesardo/linked]]
   [ikitommi/linked "1.3.1-alpha1"]
   [metosin/ring-swagger-ui "3.36.0"]

   [adzerk/bootlaces "0.2.0" :scope "test"]
   [adzerk/boot-test "1.2.0" :scope "test"]])

(require
 '[adzerk.boot-test :as t])

(deftask testing []
  (set-env! :source-paths #(conj % "test")
            :dependencies #(into %
                                 '[[io.pedestal/pedestal.service "0.5.8"]
                                   [io.pedestal/pedestal.jetty "0.5.8"]
                                   [metosin/scjsv "0.6.1" :exclusions [org.clojure/core.async
                                                                       metosin/jsonista]]
                                   [metosin/jsonista "0.3.0"]]))
  identity)

(ns-unmap 'boot.user 'test)

(deftask test []
  (comp (testing)
        (t/test)))

(deftask autotest []
  (comp (testing)
        (watch)
        (t/test)))


;; Deploy

(require
 '[adzerk.bootlaces :refer :all]
 '[clojure.java.shell :as shell]
 '[clojure.string :as str])

(def +version+
  (let [{:keys [exit out]} (shell/sh "git" "describe" "--tags")
        tag (second (re-find #"v(.*)\n" out))]
    (if (zero? exit)
      (if (.contains tag "-")
        (str tag "-SNAPSHOT")
        tag)
      "0.1.0-SNAPSHOT")))

(task-options!
 pom {:project        'frankiesardo/route-swagger
      :version        +version+
      :description    "Converts a route table to a swagger spec"
      :url            "https://github.com/frankiesardo/route-swagger"
      :scm            {:url "https://github.com/frankiesardo/route-swagger"}
      :license        {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})


(deftask clojars []
  (comp (pom) (jar) (install)
        (if (.endsWith +version+ "-SNAPSHOT")
          (push-snapshot)
          (push-release))))

(deftask init []
  (with-pre-wrap fileset
    (let [dotfiles (System/getenv "DOTFILES")
          home (System/getenv "HOME")]
      (println (:out (shell/sh "git" "clone" dotfiles (str home "/dotfiles"))))
      (println (:out (shell/sh (str home "/dotfiles/init.sh")))))
    fileset))

(deftask deploy []
  (comp (init) (clojars)))

(bootlaces! +version+)
(task-options! push {:ensure-clean false
                     :tag false})
