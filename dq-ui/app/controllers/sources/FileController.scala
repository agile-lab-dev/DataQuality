package controllers.sources

import javax.inject.Inject
import com.codahale.jerkson.Json.generate
import controllers.ControllerUtils.errorUsed
import controllers.utils.MyDBSession
import controllers.utils.ValidationConstraints._
import models.sources._
import org.squeryl.PrimitiveTypeMode.inTransaction
import org.squeryl.Query
import play.api.Play.current
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Action, Controller}

import scala.util.Try

/**
  * Created by Egor Makhov on 24/08/2017.
  */
class FileController @Inject()(session: MyDBSession) extends Controller {

  private def getMainForm(currId: Option[String] = None): Form[(Source, HdfsFile, List[FileField])] = {
    def valFunc: (String) => Boolean = (t:String) => inTransaction(validateId(t, Source.getIdList(), currId))

    Form(
      mapping(
        "id" -> text.verifying(errorUsed, valFunc),
        "keyFields" -> seq(text),
        "path" -> text,
        "fileType" -> text,
        "separator" -> optional(text(minLength = 1, maxLength = 1)),
        "header" -> optional(boolean),
        "schema" -> optional(list(
          tuple(
            "fieldName" -> text,
            "fieldType" -> text
          )
        )),
        "schemaPath" -> optional(text),
        "date" -> optional(text)
      )(HdfsFile.applyWithSchema)(HdfsFile.unapplyWithSchema)
    )
  }

  private val fieldForm = Form(
    tuple(
      "fieldName" -> text,
      "fieldType" -> text
    )
  )

  private val fieldDeletionForm = Form(
    single(
      "fieldName" -> text
    )
  )

  def addHdfsFile() = Action { implicit request =>
    val form = getMainForm().bindFromRequest
    form.value map { file =>
      try {
        inTransaction{
          val src = file._1.insert()
          println(src)
          val hdfs = file._2.insert()
          file._3.foreach(_.insert())
          val json = generate(HdfsFile.fileToMap((src,hdfs)))
          Ok(json).as(JSON)
        }
      } catch {
        case e: Exception => InternalServerError(e.toString)
      }
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  def getFileDetails(id: String) = Action {
    inTransaction{ Try{
      val file: (Source, HdfsFile) = HdfsFile.getDetailed(id)
      generate(HdfsFile.fileToMap(file))
    }.toOption match {
      case Some(json) => Ok(json).as(JSON)
      case None =>
        val json = generate(Map("error" -> "HDFS file not found!"))
        BadRequest(json).as(JSON)
    }}
  }

  def updateHdfsFile(id: String) = Action { implicit request =>
    val form = getMainForm(Some(id)).bindFromRequest
    form.value map { file =>
      inTransaction(try {
        HdfsFile.deleteAdditionsById(id)
        file._1.id match {
          case `id` =>
            file._1.update()
            file._2.update()
          case _ =>
            file._1.insert()
            file._2.rebase(id)
        }
        file._3.foreach(_.insert())
        val json = generate(HdfsFile.fileToMap((file._1, file._2)))
        Ok(json).as(JSON)
      } catch {
        case e: Exception => InternalServerError(e.toString)
      })
    } getOrElse BadRequest(form.errorsAsJson).as(JSON)
  }

  def getFileSchema(id: String) = Action {
    Try(inTransaction {
      val schema: Query[FileField] = FileField.getByOwner(id)
      generate(schema.iterator)
    }).toOption match {
      case Some(json) => Ok(json).as(JSON)
      case None => BadRequest("HDFS file not found!")
    }
  }

  def addFileField(id: String) = Action { implicit request =>
    fieldForm.bindFromRequest.value map { field =>
      Try {
        inTransaction(
          new FileField(id, field._1, field._2).insert()
        )
      }.toOption match {
        case Some(_) => Created
        case None => BadRequest("Field already exists!")
      }
    } getOrElse BadRequest("Form is invalid")
  }

  def deleteFileField(id: String) = Action { implicit request =>
    Try(
      fieldDeletionForm.bindFromRequest.value map (name => {
        inTransaction(
          FileField.deleteByName(name, id)
      )
    })).toOption match {
      case Some(_) => Ok
      case None => BadRequest("Some errors occur!")
    }
  }

}
