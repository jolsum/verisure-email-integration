package house.jolsum.central.verisure;

import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Duration;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogicMachineReporter implements VerisureEventListener {

  private static final Logger log = LoggerFactory.getLogger(LogicMachineReporter.class);

  private static final String GET_URL =
      "http://%s/scada-remote?m=json&r=grp&fn=getvalue&alias=%s&datatype=bool";

  private static final String WRITE_URL =
      "http://%s/scada-remote?m=json&r=grp&fn=write&alias=%s&value=%s&datatype=bool";

  private final CloseableHttpClient client;
  private final String host;

  private String alarmStateAddress;
  private String doorStateAddress;

  public LogicMachineReporter(String host, String username, String password) {

    CredentialsProvider provider = new BasicCredentialsProvider();
    UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username, password);
    provider.setCredentials(AuthScope.ANY, credentials);

    Duration timeout = Duration.ofSeconds(5);

    RequestConfig config =
        RequestConfig.custom()
            .setConnectTimeout((int) timeout.toMillis())
            .setConnectionRequestTimeout((int) timeout.toMillis())
            .setSocketTimeout((int) timeout.toMillis())
            .build();

    this.client =
        HttpClientBuilder.create()
            .setDefaultCredentialsProvider(provider)
            .setDefaultRequestConfig(config)
            .build();
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
      setValue(alarmStateAddress, true);
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
      setValue(alarmStateAddress, false);
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
      setValue(doorStateAddress, true);
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
      setValue(doorStateAddress, false);
    } catch (IOException e) {
      log.error("Failed reporting door state", e);
    }
  }

  private boolean getValue(String address) throws IOException {
    String url = String.format(GET_URL, host, address);

    log.info("Calling {}", url);

    try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {

      log.info("Response: {}", response.getStatusLine());

      try (Reader in = new InputStreamReader(response.getEntity().getContent())) {
        return "true".equals(CharStreams.toString(in));
      }
    }
  }

  private void setValue(String address, boolean value) throws IOException {

    if (getValue(address) == value) {
      log.info("No change. Value of {} is already {}", address, value);
      return;
    }

    String url = String.format(WRITE_URL, host, address, value);
    log.info("Calling {}", url);

    try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
      log.info("Response: {}", response.getStatusLine());
      try (Reader in = new InputStreamReader(response.getEntity().getContent())) {
        String responseBody = CharStreams.toString(in);
        log.info("Response body: {}", responseBody);
      }
    }
  }
}
