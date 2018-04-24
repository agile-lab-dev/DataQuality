package com.agilelab.dataquality.common.parsers

import cats.data.ValidatedNel
import cats.implicits._
import com.agilelab.dataquality.common.models.CommonModel
import com.agilelab.dataquality.common.parsers.DQConfig.AllErrorsOr
import com.typesafe.config.Config

trait ConfigReader[A] {
  def read(value: Config, enums: Set[String]*): AllErrorsOr[A]
}

object DQConfig {
  type AllErrorsOr[A] = ValidatedNel[String,A] // Validated[NonEmptyList[String], A]
  def parse[A](conf: Config, enums: Set[String]*)(implicit r: ConfigReader[A]): AllErrorsOr[A] = r.read(conf, enums:_*)
  def traverseResults[A](res: List[AllErrorsOr[A]]): AllErrorsOr[List[A]] = res.sequence
}

trait CommonToUiTransformer[A] {
  def transform(v: CommonModel): A
}

object CommonTransform {
  def toUI[A](v: CommonModel)(implicit tfm: CommonToUiTransformer[A]): A = tfm.transform(v)
}

