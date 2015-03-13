(ns coldwine.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :refer [blank?]]
            [clojure.core.async :as async]
            [clojure.java.io :as io]
            [clj-progress.core :refer [init done tick-to]]
            [medley.core :refer [map-vals]])
  (:import
    (java.io FileInputStream)
    (java.nio.charset Charset)
    (java.nio.channels FileChannel)
    (sigrun.serialization FormatEntry
                          BinaryHeaderFormat
                          BinaryHeaderFormatBuilder
                          TraceHeaderFormat
                          TraceHeaderFormatBuilder)
    (sigrun.common SEGYStreamFactory
                   ParseProgressListener))
  (:gen-class))

(def cli-options
  [["-i" "--input-file INPUT_FILE" "Input sgy file"
      :id :input-file]
   ["-o" "--output-file OUTPUT_FILE" "Output navigation file"]
      :id :output-file])

(defn fmt-entry
  [from to]
  (FormatEntry/create from to))

(defn make-segy-rev1-bhf
  []
  (.. 
    (BinaryHeaderFormatBuilder/aBinaryHeaderFormat)
    (withLineNumberFormat (fmt-entry 4 8))
    (withSampleIntervalFormat (fmt-entry 16 18))
    (withSamplesPerDataTraceFormat (fmt-entry 20 22))
    (withDataSampleCodeFormat (fmt-entry 24 26))
    (withSegyFormatRevNumberFormat (fmt-entry 300 302))
    (withFixedLengthTraceFlagFormat (fmt-entry 302 304))
    (withNumberOf3200ByteFormat (fmt-entry 304 306))
    (build)))

(defn make-segy-rev1-thf
  []
  (.. 
    (TraceHeaderFormatBuilder/aTraceHeaderFormat)
    (withTraceSequenceNumberWLFormat (fmt-entry 0 4))
    (withInLineNumberFormat (fmt-entry 188 192))
    (withCrossLineNumberFormat (fmt-entry 192 196))
    (withDelayRecordingTimeFormat (fmt-entry 108 110))
    (withSourceXFormat (fmt-entry 72 76))
    (withSourceYFormat (fmt-entry 76 80))
    (withXOfCDPPositionFormat (fmt-entry 180 184))
    (withYOfCDPPositionFormat (fmt-entry 184 188))
    (withNumberOfSamplesFormat (fmt-entry 114 116))
    (build)))

;; Format is a map that contains sigrun.common.TraceHeader method calls.
; Todo: consider replace naive lambda declarations to macro.
(def default-nav-fmt
  { :inline #(.. (.getHeader %) (getInLineNumber))
    :xline  #(.. (.getHeader %) (getCrossLineNumber))
    :x      #(.. (.getHeader %) (getSourceX))
    :y      #(.. (.getHeader %) (getSourceY))})

(defn make-nav-line-rec
  ([trace] (make-nav-line-rec trace default-nav-fmt))
  ([trace format]
    (map-vals (fn [v] (v trace)) format)))

(defn process-seismic
  [seismic-stream]
  (println "Started seismic processing")
  (mapv make-nav-line-rec seismic-stream))

(defn stream-factory
  []
  (SEGYStreamFactory/create
    (Charset/forName "Cp1047")
    (make-segy-rev1-bhf)
    (make-segy-rev1-thf)))

(defn make-chan
  [path]
  (.. (FileInputStream. path)
      getChannel))

(defn make-listener-set
  [ch] 
  (hash-set
    (reify ParseProgressListener
      (progress [_ p] (async/put! ch p)))))

(defn make-stream
  ([^FileChannel chan c] (make-stream chan (stream-factory) c))
  ([^FileChannel chan factory c] (.makeStream factory chan (make-listener-set c))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [[input-file & _] args
        size (float (.length (io/file input-file)))
        prch (async/chan (async/sliding-buffer 1))]
    (async/go
      (init size)
      (loop []
        (when-some [v (async/<! prch)]
          (tick-to v)
          (recur)))
      (done))
    (process-seismic (-> (make-chan input-file)
                         (make-stream prch)))))

