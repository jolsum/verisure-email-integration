package house.jolsum.central.verisure;

import java.util.Collection;

public class Mail {

  private final Collection<String> from;
  private final Collection<String> to;
  private final String subject;
  private final Object content;

  public Mail(Collection<String> from, Collection<String> to, String subject, Object content) {
    this.from = from;
    this.to = to;
    this.subject = subject;
    this.content = content;
  }

  public Collection<String> getFrom() {
    return from;
  }

  public Collection<String> getTo() {
    return to;
  }

  public String getSubject() {
    return subject;
  }

  public Object getContent() {
    return content;
  }

  @Override
  public String toString() {
    return "Mail{" +
        "from=" + from +
        ", to=" + to +
        ", subject='" + subject + '\'' +
        ", content=" + content +
        '}';
  }
}
