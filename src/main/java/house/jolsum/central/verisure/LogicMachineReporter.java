package house.jolsum.central.verisure;

import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicMachineReporter implements VerisureEventListener {

  private static final Logger log = LoggerFactory.getLogger(LogicMachineReporter.class);

  private static final String URL =
      "http://%s/scada-remote?m=json&r=grp&fn=checkwrite&alias=%s&value=%d";

  private final HttpClient client;
  private final String host;

  private String alarmStateAddress;
  private String doorStateAddress;

  public LogicMachineReporter(String host, String username, String password) {

    CredentialsProvider provider = new BasicCredentialsProvider();
    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
    provider.setCredentials(AuthScope.ANY, credentials);

    this.client = HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
    this.host = host;
  }

  public LogicMachineReporter setAlarmStateAddress(String alarmStateAddress) {
    this.alarmStateAddress = alarmStateAddress;
    return this;
  }

  public LogicMachineReporter setDoorStateAddress(String doorStateAddress) {
    this.doorStateAddress = doorStateAddress;
    return this;
  }

  @Override
  public void onAlarmArmed() {
    if (alarmStateAddress == null) {
      return;
    }
    try {
      setValue(alarmStateAddress, 1);
    } catch (IOException e) {
      log.error("Failed reporting alarm state", e);
    }
  }

  @Override
  public void onAlarmDisarmed() {
    if (alarmStateAddress == null) {
      return;
    }
    try {
      setValue(alarmStateAddress, 0);
    } catch (IOException e) {
      log.error("Failed reporting alarm state", e);
    }
  }

  @Override
  public void onDoorLocked() {
    if (doorStateAddress == null) {
      return;
    }
    try {
      setValue(doorStateAddress, 1);
    } catch (IOException e) {
      log.error("Failed reporting door state", e);
    }
  }

  @Override
  public void onDoorUnlocked() {
    if (doorStateAddress == null) {
      return;
    }
    try {
      setValue(doorStateAddress, 0);
    } catch (IOException e) {
      log.error("Failed reporting door state", e);
    }
  }

  private void setValue(String address, int value) throws IOException {
    String url = String.format(URL, host, address, value);

    HttpResponse response = client.execute(new HttpGet(url));
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode != 200) {
      throw new IOException("Got reponse code " + statusCode);
    }
  }
}
