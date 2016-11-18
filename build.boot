(def project 'allstate-claims)
(def version "0.1.0-SNAPSHOT")

(set-env! :resource-paths #{"resources" "src"}
          :source-paths   #{"test"}
          :dependencies   '[[org.clojure/clojure "1.9.0-alpha14"]
                            [adzerk/boot-test "RELEASE" :scope "test"]

                            [me.raynes/conch "RELEASE"]
                            [clojure-csv "RELEASE"]
                            [semantic-csv "RELEASE"]
                            [huri "0.7.0-SNAPSHOT"]
                            [com.rpl/specter "RELEASE"]])

(task-options!
 aot {:namespace   #{'allstate-claims.core}}
 pom {:project     project
      :version     version
      :description "FIXME: write description"
      :url         "http://example/FIXME"
      :scm         {:url "https://github.com/yourname/allstate-claims"}
      :license     {"Eclipse Public License"
                    "http://www.eclipse.org/legal/epl-v10.html"}}
 jar {:main        'allstate-claims.core
      :file        (str "allstate-claims-" version "-standalone.jar")})

(deftask build
  "Build the project locally as a JAR."
  [d dir PATH #{str} "the set of directories to write to (target)."]
  (let [dir (if (seq dir) dir #{"target"})]
    (comp (aot) (pom) (uber) (jar) (target :dir dir))))

(deftask run
  "Run the project."
  [a args ARG [str] "the arguments for the application."]
  (require '[allstate-claims.core :as app])
  (apply (resolve 'app/-main) args))

(require '[adzerk.boot-test :refer [test]])
