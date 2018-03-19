package it.agilelab.bigdata.DataQuality.utils

import org.apache.log4j.Logger

trait Logging {
  @transient lazy val log = Logger.getLogger(getClass.getName)
}
