package house.jolsum.central.verisure;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerisureEmailReceiver {

  private static final Logger logger = LoggerFactory.getLogger(VerisureEmailReceiver.class);

  private MailClient mailClient;
  private List<VerisureEventListener> listeners = new CopyOnWriteArrayList<>();

  public VerisureEmailReceiver() throws MessagingException {

    this.mailClient =
        new MailClient(
            getEnvOrThrow("EMAIL_USER"),
            getEnvOrThrow("EMAIL_PASSWORD"),
            getEnvOrThrow("EMAIL_HOST"),
            getEnvOrThrow("EMAIL_PORT"));

    logger.info("Mail receiver initialized");
  }

  public VerisureEmailReceiver addListener(VerisureEventListener listener) {
    listeners.add(listener);
    return this;
  }

  public void start() {
    mailClient.addMailReceivedListener(this::handleEmailReceived);
    mailClient.pollInbox();
    logger.info("Receiver stopped");
  }

  private void handleEmailReceived(Mail mail) {
    String subject = mail.getSubject();

    if (subject.contains("tilkoblet")) {
      logger.info("Alarm tilkoblet");
      listeners.forEach(VerisureEventListener::onAlarmArmed);

    } else if (subject.contains("frakoblet")) {
      logger.info("Alarm frakoblet");
      listeners.forEach(VerisureEventListener::onAlarmDisarmed);
    }
  }

  private static String getEnvOrThrow(String key) {
    String value = System.getenv(key);
    if (value == null) {
      throw new RuntimeException("Missing data in environment variable " + key);
    }
    return value;
  }

  public static void main(String[] args) {
    try {
      new VerisureEmailReceiver()
          .addListener(
              new LogicMachineReporter(
                      getEnvOrThrow("LOGICMACHINE_HOST"),
                      getEnvOrThrow("LOGICMACHINE_USER"),
                      getEnvOrThrow("LOGICMACHINE_PASSWORD"))
                  .setAlarmStateAddress(getEnvOrThrow("LOGICMACHINE_ALARM_STATE_ADDRESS")))
          .start(); // blocking call

    } catch (Exception e) {
      logger.error("Got exception in main method; exiting", e);

    } finally {
      System.exit(1);
    }
  }
}
