package it.agilelab.bigdata.DataQuality.utils

/**
  * Created by Egor Makhov on 13/10/2017.
  */
object Mail {
  implicit def stringToSeq(single: String): Seq[String] = Seq(single)
  implicit def liftToOption[T](t: T): Option[T] = Some(t)

  sealed abstract class MailType
  case object Plain extends MailType
  case object Rich extends MailType
  case object MultiPart extends MailType

  def a(mail: Mail)(implicit mailer: Mailer) {
    import org.apache.commons.mail._

    val format =
      if (mail.attachment.isDefined) MultiPart
      else if (mail.richMessage.isDefined) Rich
      else Plain

    val commonsMail: Email = format match {
      case Plain => new SimpleEmail().setMsg(mail.message)
      case Rich =>
        new HtmlEmail()
          .setHtmlMsg(mail.richMessage.get)
          .setTextMsg(mail.message)
      case MultiPart => {
        val attachment = new EmailAttachment()
        attachment.setPath(mail.attachment.get.getAbsolutePath)
        attachment.setDisposition(EmailAttachment.ATTACHMENT)
        attachment.setName(mail.attachment.get.getName)
        new MultiPartEmail().attach(attachment).setMsg(mail.message)
      }
    }

    commonsMail.setHostName(mailer.hostName)
    commonsMail.setSmtpPort(mailer.smtpPortSSL)
    commonsMail.setAuthenticator(
      new DefaultAuthenticator(mailer.username, mailer.password))
    commonsMail.setSSLOnConnect(mailer.sslOnConnect)

    // Can't add these via fluent API because it produces exceptions
    mail.to foreach (commonsMail.addTo(_))
    mail.cc foreach (commonsMail.addCc(_))
    mail.bcc foreach (commonsMail.addBcc(_))

    commonsMail
      .setFrom(mail.from._1, mail.from._2)
      .setSubject(mail.subject)
      .send()
  }
}

case class Mail(
    from: (String, String), // (email -> name)
    to: Seq[String],
    cc: Seq[String] = Seq.empty,
    bcc: Seq[String] = Seq.empty,
    subject: String,
    message: String,
    richMessage: Option[String] = None,
    attachment: Option[(java.io.File)] = None
)
