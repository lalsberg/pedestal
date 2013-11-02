; Copyright 2013 Relevance, Inc.

; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0)
; which can be found in the file epl-v10.html at the root of this distribution.
;
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
;
; You must not remove this notice, or any other, from this software.

(ns io.pedestal.app.model
  (:require [io.pedestal.app.diff :as diff]
            [clojure.core.async :refer [go chan <! >!]]))

(defn transform-to-inform
  "Given a data-model and a transform message, return an inform message."
  [old-model transform]
  (let [new-model (reduce (fn [m [path f & args]]
                            (apply update-in m path f args))
                          old-model
                          transform)]
    [(diff/model-diff-inform (map first transform) old-model new-model) new-model]))

(defn transform->inform
  "Given a data model and an inform channel, returns a transform channel.
  When a transform message is put on the transform channel, the resulting
  inform message will be put on the inform channel."
  [data-model inform-c]
  (let [transform-c (chan 10)]
    (go (loop [data-model data-model]
          (let [transform (<! transform-c)]
            (when transform
              (let [[inform-msgs new-model] (transform-to-inform data-model transform)]
                (>! inform-c inform-msgs)
                (recur new-model))))))
    transform-c))
