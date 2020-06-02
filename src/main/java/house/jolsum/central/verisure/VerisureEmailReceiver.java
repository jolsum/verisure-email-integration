package house.jolsum.central.verisure;

import java.util.Map;
import javax.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerisureEmailReceiver {

  private static final Logger logger = LoggerFactory.getLogger(VerisureEmailReceiver.class);

  private MailClient mailClient;

  public VerisureEmailReceiver(Map<String, String> properties) throws MessagingException {

    this.mailClient =
        new MailClient(
            properties.get("EMAIL_USER"),
            properties.get("EMAIL_PASSWORD"),
            properties.get("EMAIL_INCOMING_URL"),
            properties.get("EMAIL_INCOMING_PORT"));

    logger.info("Mail receiver initialized");
  }

  public void start() {
    mailClient.addMailReceivedListener(this::handleEmailReceived);
    mailClient.pollInbox();
    logger.info("Receiver stopped");
  }

  private void handleEmailReceived(Mail mail) {

    String subject = mail.getSubject();
    logger.info("Subject: {}", subject);

    if (subject.contains("tilkoblet")) {
      logger.info("Alarm tilkoblet");
    } else if (subject.contains("frakoblet")) {
      logger.info("Alarm frakoblet");
    }
  }

  public static void main(String[] args) {
    try {
      VerisureEmailReceiver crawler = new VerisureEmailReceiver(System.getenv());
      crawler.start(); // blocking call

    } catch (Exception e) {
      logger.error("Got exception in main method; exiting", e);

    } finally {
      System.exit(1);
    }
  }
}
