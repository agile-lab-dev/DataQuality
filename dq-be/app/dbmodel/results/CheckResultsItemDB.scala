package dbmodel.results

import org.squeryl.annotations.Column

/**
  * Represents the Swagger definition for CheckResultsItem.
  */
@javax.annotation.Generated(value = Array("org.openapitools.codegen.languages.ScalaPlayFrameworkServerCodegen"),
                            date = "2019-11-19T15:05:19.949+01:00[Europe/Rome]")
case class CheckResultsItemDB(
    @Column("check_id")
    checkId: Option[String],
    @Column("check_name")
    checkName: Option[String],
    description: Option[String],
    @Column("checked_file")
    checkedFile: Option[String],
    @Column("base_metric")
    baseMetric: Option[String],
    @Column("compared_metric")
    comparedMetric: Option[String],
    @Column("compared_threshold")
    comparedThreshold: Option[String],
    status: Option[String],
    message: Option[String],
    @Column("exec_date")
    execDate: Option[String]
)
