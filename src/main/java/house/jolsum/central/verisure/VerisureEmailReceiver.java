package house.jolsum.central.verisure;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VerisureEmailReceiver {

  private static final Logger logger = LoggerFactory.getLogger(VerisureEmailReceiver.class);

  private MailClient mailClient;
  private List<VerisureEventListener> listeners = new CopyOnWriteArrayList<>();

  public VerisureEmailReceiver(Map<String, String> properties) throws MessagingException {

    this.mailClient =
        new MailClient(
            properties.get("EMAIL_USER"),
            properties.get("EMAIL_PASSWORD"),
            properties.get("EMAIL_INCOMING_URL"),
            properties.get("EMAIL_INCOMING_PORT"));

    logger.info("Mail receiver initialized");
  }

  public void addListener(VerisureEventListener listener) {
    listeners.add(listener);
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

  public static void main(String[] args) {
    try {
      Map<String, String> envProps = System.getenv();

      VerisureEmailReceiver receiver = new VerisureEmailReceiver(envProps);
      receiver.addListener(
          new LogicMachineReporter(
                  envProps.get("LOGICMACHINE_HOST"),
                  envProps.get("LOGICMACHINE_USER"),
                  envProps.get("LOGICMACHINE_PASSWORD"))
              .setAlarmStateAddress(envProps.get("LOGICMACHINE_ALARM_STATE_ADDRESS")));
      receiver.start(); // blocking call

    } catch (Exception e) {
      logger.error("Got exception in main method; exiting", e);

    } finally {
      System.exit(1);
    }
  }
}
