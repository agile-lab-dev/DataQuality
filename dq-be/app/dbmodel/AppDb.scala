package dbmodel

import dbmodel.results.{CheckResultsItemDB, MetricResultsColumnarItemDB, MetricResultsFileItemDB}
import dbmodel.sources.{MetricItem, SourceItemDB, TagCheckItem, TagItem}
import dbmodel.checks.{Check, CheckParameter, SnapshotCheck, SqlCheck, TrendCheck}
import dbmodel.meta.{CheckMeta, CheckParamMeta, CheckRule, MetricMeta, MetricParamMeta}
import dbmodel.metrics.{ColumnMetric, ComposedMetric, ComposedMetricConnection, FileMetric, Metric, MetricParameter}
import dbmodel.sources.{DBTable, Database, FileField, HdfsFile, HiveTable, Source, VirtualSourceDB}
import dbmodel.targets.{Mail, Target, TargetToChecks}
import org.squeryl.PrimitiveTypeMode.oneToManyRelation
import org.squeryl.{Schema, Table}

object MyOwnTypeMode extends org.squeryl.PrimitiveTypeMode {
  // custom types, etc, go here
}
import dbmodel.MyOwnTypeMode._
object AppDB extends Schema {
  val CheckResultsTable: Table[CheckResultsItemDB] = table[CheckResultsItemDB]("results_check")
  val MetricItemTable: Table[MetricItem] = table[MetricItem]("metrics")
  val MetricResultsColumnarItemTable: Table[MetricResultsColumnarItemDB] = table[MetricResultsColumnarItemDB]("results_metric_columnar")
  val MetricResultsFileItemTable: Table[MetricResultsFileItemDB] = table[MetricResultsFileItemDB]("results_metric_file")
  val SourcesTable: Table[SourceItemDB] = table[SourceItemDB]("sources")
  val TagsTable: Table[TagItem] = table[TagItem]("tag")
  val TagChecksTable: Table[TagCheckItem] = table[TagCheckItem]("tag_check")
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////




  val metricMetaTable: Table[MetricMeta] = table[MetricMeta]("meta_metric")
  val metricParamMetaTable: Table[MetricParamMeta] = table[MetricParamMeta]("meta_metric_parameters")
  val metaMetricToParameters: _root_.org.squeryl.PrimitiveTypeMode.OneToManyRelationImpl[MetricMeta, MetricParamMeta] =
    oneToManyRelation(metricMetaTable, metricParamMetaTable).
      via((t,m) => t.id === m.metric)

  val checkMetaTable: Table[CheckMeta] = table[CheckMeta]("meta_check")
  val checkParamMetaTable: Table[CheckParamMeta] = table[CheckParamMeta]("meta_check_parameters")
  val checkRuleTable: Table[CheckRule] = table[CheckRule]("meta_trend_check_rules")

  // Sources
  val databaseTable: Table[Database] = table[Database]("databases")
  val sourceTable: Table[Source] = table[Source]("sources")
  val dbTableTable: Table[DBTable] = table("db_tables")
  val hiveTableTable: Table[HiveTable] = table("hive_tables")
  val fileTable: Table[HdfsFile] = table("hdfs_files")
  val fileSchemaTable: Table[FileField] = table("file_fields")
  val virtualSourceTable: Table[VirtualSourceDB] = table("virtual_sources")

  val sourceToFields: _root_.org.squeryl.PrimitiveTypeMode.OneToManyRelationImpl[HdfsFile, FileField] =
    oneToManyRelation(fileTable,  fileSchemaTable).
      via((t,m) => t.id === m.owner)

  // Metrics
  val metricsTable: Table[Metric] = table[Metric]("metrics")
  val fileMetricsTable: Table[FileMetric] = table[FileMetric]("file_metrics")
  val columnMetricsTable: Table[ColumnMetric] = table[ColumnMetric]("column_metrics")
  val metricParametersTable: Table[MetricParameter] = table[MetricParameter]("metric_parameters")
  val composedMetricsTable: Table[ComposedMetric] = table[ComposedMetric]("composed_metrics")
  val composedMetricConnectionsTable: Table[ComposedMetricConnection] = table[ComposedMetricConnection]("composed_metric_connections")

  val metricToParameters: _root_.org.squeryl.PrimitiveTypeMode.OneToManyRelationImpl[ColumnMetric, MetricParameter] =
    oneToManyRelation(columnMetricsTable, metricParametersTable).
      via((t,m) => t.id === m.owner)

  // Checks
  val checksTable: Table[Check] = table[Check]("checks")
  val sqlChecksTable: Table[SqlCheck] = table[SqlCheck]("sql_checks")
  val snapshotChecksTable: Table[SnapshotCheck] = table[SnapshotCheck]("snapshot_checks")
  val trendChecksTable: Table[TrendCheck] = table[TrendCheck]("trend_checks")
  val checkParametersTable: Table[CheckParameter] = table[CheckParameter]("check_parameters")

  val snapshotCheckToParameters: _root_.org.squeryl.PrimitiveTypeMode.OneToManyRelationImpl[SnapshotCheck, CheckParameter] =
    oneToManyRelation(snapshotChecksTable, checkParametersTable).
      via((t,m) => t.id === m.owner)

  val trendCheckToParameters: _root_.org.squeryl.PrimitiveTypeMode.OneToManyRelationImpl[TrendCheck, CheckParameter] =
    oneToManyRelation(trendChecksTable, checkParametersTable).
      via((t,m) => t.id === m.owner)

  // Targets
  val targetTable: Table[Target] = table[Target]("targets")
  val mailTable: Table[Mail] = table[Mail]("mails")
  val targetToChecksTable: Table[TargetToChecks] = table[TargetToChecks]("target_to_checks")

  val targetToMails: _root_.org.squeryl.PrimitiveTypeMode.OneToManyRelationImpl[Target, Mail] =
    oneToManyRelation(targetTable,  mailTable).
      via((t,m) => t.id === m.owner)

  val targetToChecks: _root_.org.squeryl.PrimitiveTypeMode.OneToManyRelationImpl[Target, TargetToChecks] =
    oneToManyRelation(targetTable,  targetToChecksTable).
      via((t,c) => t.id === c.targetId)

  def resetAll(): Int = {
    targetToChecksTable.deleteWhere(_ => 1===1)
    mailTable.deleteWhere(_ => 1===1)
    targetTable.deleteWhere(_ => 1===1)

    checkParametersTable.deleteWhere(_ => 1===1)
    trendChecksTable.deleteWhere(_ => 1===1)
    snapshotChecksTable.deleteWhere(_ => 1===1)
    sqlChecksTable.deleteWhere(_ => 1===1)
    checksTable.deleteWhere(_ => 1===1)

    composedMetricsTable.deleteWhere(_ => 1===1)
    metricParametersTable.deleteWhere(_ => 1===1)
    fileMetricsTable.deleteWhere(_ => 1===1)
    columnMetricsTable.deleteWhere(_ => 1===1)
    metricsTable.deleteWhere(_ => 1===1)

    virtualSourceTable.deleteWhere(_ => 1===1)
    fileSchemaTable.deleteWhere(_ => 1===1)
    fileTable.deleteWhere(_ => 1===1)
    hiveTableTable.deleteWhere(_ => 1===1)
    dbTableTable.deleteWhere(_ => 1===1)
    sourceTable.deleteWhere(_ => 1===1)
    databaseTable.deleteWhere(_ => 1===1)
  }

}
