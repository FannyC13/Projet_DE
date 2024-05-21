import javax.mail.{Session, Message, PasswordAuthentication, Transport}
import javax.mail.internet.{InternetAddress, MimeMessage}
import java.util.Properties

object EmailUtil {

  //Définition des variables
  val username = sys.env("MAIL_ACCOUNT")
  val password = sys.env("MAIL_PASSWORD")

  val props = new Properties()
  props.put("mail.smtp.auth", "true")
  props.put("mail.smtp.starttls.enable", "true")
  props.put("mail.smtp.host", "smtp.gmail.com")
  props.put("mail.smtp.port", "587")

  //Instance de mail

  val session = Session.getInstance(props, new javax.mail.Authenticator() {
    protected override def getPasswordAuthentication: PasswordAuthentication = {
      new PasswordAuthentication(username, password)
    }
  })

  def sendEmail(recipient: String, subject: String, content: String): Unit = {
    try {
      val message = new MimeMessage(session)
      message.setFrom(new InternetAddress(username))

      val addresses: Array[javax.mail.Address] = InternetAddress.parse(recipient).map(_.asInstanceOf[javax.mail.Address])
      message.setRecipients(Message.RecipientType.TO, addresses)
      message.setSubject(subject)
      message.setText(content)
      Transport.send(message)
      println(s"Sent message to $recipient")
    } catch {
      case e: Exception => e.printStackTrace()
    }
  }
}
