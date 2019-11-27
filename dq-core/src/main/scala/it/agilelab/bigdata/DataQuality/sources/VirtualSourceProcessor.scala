package it.agilelab.bigdata.DataQuality.sources

import it.agilelab.bigdata.DataQuality.utils._

import org.apache.spark.storage.StorageLevel
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.storage.StorageLevel

import scala.collection.JavaConversions.asJavaCollection

/**
  * Created by Rocco Caruso on 12/10/17.
  */
object VirtualSourceProcessor {

  def getActualSources(initialVirtualSourcesMap: Map[String, VirtualFile], initialSourceMap: Map[String, Source])(
      implicit sqlContext: SQLContext,
      settings: DQSettings): Map[String, Source] = {

    @scala.annotation.tailrec
    def loop(virtualSourcesMap: Map[String, VirtualFile], actualSourcesMapAccumulator: Map[String, Source])(
        implicit sqlContext: SQLContext): Map[String, Source] = {

      log.info(s"Virtual sources to load: ${virtualSourcesMap.size}")

      if (virtualSourcesMap.isEmpty) {
        log.info(s"[SUCCESS] Virtual sources loading is complete.")
        actualSourcesMapAccumulator
      } else {
        val firstLevelVirtualSources: Map[String, VirtualFile] =
          virtualSourcesMap.filter {
            case (sourceId, conf: VirtualFile) =>
              val parentIds = conf.parentSourceIds
              log.info(s"  * Virtual source $sourceId | parents: ${parentIds.mkString(", ")}")
              actualSourcesMapAccumulator.keySet.containsAll(parentIds)
          }

        val otherSources: Map[String, Source] = firstLevelVirtualSources
          .map {
            case (vid, virtualFile) =>
              virtualFile match {
                case VirtualFileSelect(id, parentSourceIds, sqlCode, keyfields, save, persist) =>
                  val firstParent = parentSourceIds.head
                  log.info(s"Processing '$id', type: 'FILTER-SQL', parent: '$firstParent'")
                  log.info(s"SQL: $sqlCode")
                  val dfSource = actualSourcesMapAccumulator.get(firstParent).head

                  dfSource.df.registerTempTable(firstParent)
                  val virtualSourceDF = sqlContext.sql(sqlCode)

                  //persist feature
                  if (persist.isDefined) {
                    virtualSourceDF.persist(persist.getOrElse(throw new RuntimeException("Something is wrong!")))
                    log.info(s"Persisting VS $id (${persist.get.description})...")
                  }

                  Source(vid, settings.refDateString, virtualSourceDF, keyfields)

                case VirtualFileJoinSql(id, parentSourceIds, sqlCode, keyfields, save, persist) =>
                  val leftParent  = parentSourceIds.head
                  val rightParent = parentSourceIds(1)
                  log.info(s"Processing '$id', type: 'JOIN-SQL', parent: L:'$leftParent', R:'$rightParent'")
                  log.info(s"SQL: $sqlCode")

                  val dfSourceLeft: DataFrame = actualSourcesMapAccumulator(leftParent).df
                  val dfSourceRight: DataFrame = actualSourcesMapAccumulator(rightParent).df
                  val colLeft  = dfSourceLeft.columns.toSeq.mkString(",")
                  val colRight = dfSourceRight.columns.toSeq.mkString(",")

                  dfSourceLeft.registerTempTable(leftParent)
                  dfSourceRight.registerTempTable(rightParent)

                  log.debug(s"column left $colLeft")
                  log.debug(s"column right $colRight")
                  val virtualSourceDF = sqlContext.sql(sqlCode)

                  //persist feature
                  if (persist.isDefined) {
                    virtualSourceDF.persist(persist.getOrElse(throw new RuntimeException("Something is wrong!")))
                    log.info(s"Persisting VS $id (${persist.get.description})...")
                  }

                  Source(vid, settings.refDateString, virtualSourceDF, keyfields)

                case VirtualFileJoin(id, parentSourceIds, joiningColumns, joinType, keyfields, _) =>
                  val leftParent  = parentSourceIds.head
                  val rightParent = parentSourceIds(1)
                  log.info(s"Processing '$id', type: 'JOIN', parent: L:'$leftParent', R:'$rightParent'")

                  val dfSourceLeft = actualSourcesMapAccumulator(leftParent).df
                  val dfSourceRight = actualSourcesMapAccumulator(rightParent).df

                  val colLeftRenamedLeft: Array[(String, String)] =
                    dfSourceLeft.columns
                      .filter(c => !joiningColumns.contains(c))
                      .map(colName => (colName, s"l_$colName"))
                  val colLeftRenamedRight: Array[(String, String)] =
                    dfSourceRight.columns
                      .filter(c => !joiningColumns.contains(c))
                      .map(colName => (colName, s"r_$colName"))

                  val dfLeftRenamed = colLeftRenamedLeft
                    .foldLeft(dfSourceLeft)((dfAcc, cols) => dfAcc.withColumnRenamed(cols._1, cols._2))
                  val dfRightRenamed = colLeftRenamedRight
                    .foldLeft(dfSourceRight)((dfAcc, cols) => dfAcc.withColumnRenamed(cols._1, cols._2))

                  val colLeft  = dfLeftRenamed.columns.toSeq.mkString(",")
                  val colRight = dfRightRenamed.columns.toSeq.mkString(",")

                  dfLeftRenamed.registerTempTable(leftParent)
                  dfRightRenamed.registerTempTable(rightParent)

                  log.debug(s"column left $colLeft")
                  log.debug(s"column right $colRight")

                  val virtualSourceDF =
                    dfLeftRenamed.join(dfRightRenamed, joiningColumns, joinType)

                  Source(vid, settings.refDateString, virtualSourceDF, keyfields)
              }

          }
          .map(s => (s.id, s))
          .toMap
        val virtualSourcesToProcess = virtualSourcesMap -- firstLevelVirtualSources.keySet

        val processed = firstLevelVirtualSources.size

        val newActualSources = actualSourcesMapAccumulator ++ otherSources
        if (otherSources.isEmpty) {
          log.error("SOMETHING WRONG")
          throw new Exception(
            s"processed $processed : ${firstLevelVirtualSources.keySet.mkString("-")} but head only addedSize")
        }
        loop(virtualSourcesToProcess, newActualSources)
      }
    }
    loop(initialVirtualSourcesMap, initialSourceMap)
  }

}
