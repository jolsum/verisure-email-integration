package house.jolsum.central.verisure;

import com.sun.mail.imap.IMAPFolder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.event.MessageCountAdapter;
import javax.mail.event.MessageCountEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailClient implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(MailClient.class);

  private static final long POLL_INBOX_RATE_MS = 10000;
  private static final String INBOX_FOLDER = "INBOX";

  private final Store store;
  private final Folder inbox;
  private final Collection<MailReceivedListener> receivedListenerCollection = new ArrayList<>();

  public MailClient(String username, String password, String host, String port)
      throws MessagingException {

    Properties properties = getIMAPSProperties(host, port);
    Session emailSession = Session.getDefaultInstance(properties);

    this.store = emailSession.getStore();
    this.store.connect(username, password);
    this.inbox = this.store.getFolder(INBOX_FOLDER);
    this.inbox.open(Folder.READ_ONLY);

    setupInboxMessageReceivedListener();
  }

  private static Properties getIMAPSProperties(String host, String port) {
    Properties properties = new Properties();
    properties.put("mail.store.protocol", "imaps");
    properties.put("mail.imaps.host", host);
    properties.put("mail.imaps.port", port);
    properties.put("mail.imaps.starttls.enable", "true");
    return properties;
  }

  public boolean isConnected() {
    return store.isConnected();
  }

  @Override
  public void close() {
    try {
      inbox.close();
    } catch (MessagingException e) {
      log.warn("Got exception when closing inbox", e);
    }
    try {
      store.close();
    } catch (MessagingException e) {
      log.warn("Got exception when closing store", e);
    }
  }

  private void setupInboxMessageReceivedListener() {

    inbox.addMessageCountListener(
        new MessageCountAdapter() {
          @Override
          public void messagesAdded(MessageCountEvent e) {
            try {
              log.info("{} mails received", e.getMessages().length);
              for (Message msg : e.getMessages()) {
                log.debug(
                    "Received mail from: {}, subject: {}",
                    formatAddresses(msg.getFrom()),
                    msg.getSubject());
                Mail mail = createMail(msg);
                fireMailReceivedListeners(mail);
              }
            } catch (Exception ex) {
              log.error("Failed to process received mails", ex);
            }
          }
        });
  }

  public void addMailReceivedListener(MailReceivedListener listener) {
    receivedListenerCollection.add(listener);
  }

  private void fireMailReceivedListeners(Mail mail) {
    receivedListenerCollection.forEach(listener -> listener.received(mail));
  }

  private Mail createMail(Message message) throws MessagingException, IOException {
    Collection<String> from =
        Arrays.stream(message.getFrom()).map(Address::toString).collect(Collectors.toList());
    Collection<String> to =
        Arrays.stream(message.getRecipients(Message.RecipientType.TO))
            .map(Address::toString)
            .collect(Collectors.toList());
    String subject = message.getSubject();
    Object content = message.getContent();
    return new Mail(from, to, subject, content);
  }

  private String formatAddresses(Address[] addresses) {
    return Arrays.stream(addresses).map(Address::toString).collect(Collectors.joining(", "));
  }

  public void pollInbox() {
    if (!(inbox instanceof IMAPFolder)) {
      throw new UnsupportedOperationException("Cannot poll inbox");
    }

    try {
      while (!Thread.interrupted()) {
        IMAPFolder folder = (IMAPFolder) inbox;
        log.info("idle()");
        folder.idle();
        log.info("idle() returned");
        try {
          Thread.sleep(POLL_INBOX_RATE_MS);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    } catch (MessagingException e) {
      log.error("Got exception inn poll thread", e);
    } finally {
      close();
    }
  }
}
