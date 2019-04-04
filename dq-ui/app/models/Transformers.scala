package models

import com.agilelab.dataquality.common.models.{CommonModel, DatabaseCommon}
import com.agilelab.dataquality.common.parsers.CommonToUiTransformer
import models.sources.Database

object Transformers {
  implicit val databaseTransformer: CommonToUiTransformer[Database] = new CommonToUiTransformer[Database] {
    override def transform(v: CommonModel): Database = v match {
      case db: DatabaseCommon => new Database(
        db.id,
        db.subtype,
        db.host,
        db.port,
        db.service,
        db.user,
        db.password
      )
    }
  }
}
