package org.openapitools

import api._
import play.api.inject.{Binding, Module => PlayModule}
import play.api.{Configuration, Environment}

@javax.annotation.Generated(value = Array("org.openapitools.codegen.languages.ScalaPlayFrameworkServerCodegen"), date = "2019-11-19T15:05:19.949+01:00[Europe/Rome]")
class Module extends PlayModule {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = Seq(
    bind[ChecksApi].to[ChecksApiImpl],
    bind[MetricsApi].to[MetricsApiImpl],
    bind[SourcesApi].to[SourcesApiImpl]
  )
}
