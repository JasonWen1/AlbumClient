import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLOutput;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ApiClient implements Runnable{
  public final CloseableHttpClient httpClient;

  public final Gson gson = new Gson();

  public static final AtomicInteger FAILED_REQ = new AtomicInteger(0);
  public String apiUrl;

  public byte[] getImageData() {
    return imageData;
  }

  public void setImageData(byte[] imageData) {
    this.imageData = imageData;
  }

  public Profile getProfile() {
    return profile;
  }

  public void setProfile(Profile profile) {
    this.profile = profile;
  }

  public byte[] imageData;

  public Profile profile;

  public static final ConcurrentLinkedQueue<Long> latencies1 = new ConcurrentLinkedQueue<>();
  public static final ConcurrentLinkedQueue<Long> latencies2 = new ConcurrentLinkedQueue<>();

  public ApiClient(int maxTotal, int maxPerRoute, byte[] imageData, Profile profile, String apiUrl) {
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    cm.setMaxTotal(maxTotal);
    cm.setDefaultMaxPerRoute(maxPerRoute);
    this.httpClient = HttpClients.custom().setConnectionManager(cm).build();
    this.imageData = imageData;
    this.profile = profile;
    this.apiUrl = apiUrl;
  }

  @Override
  public void run() {
    executeTask(imageData, profile);
  }

  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  public void executeTask(byte[] imageData, Profile profile) {
      try {
        List<Long> latencies11 = new ArrayList<>();
        List<Long> latencies22 = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
          //System.out.println("loop " + i);
          long start = System.currentTimeMillis();

          String albumId = postImage(imageData, profile);
          long latency1 = System.currentTimeMillis() - start;
          latencies11.add(latency1);
          //String albumId = "1";
          getImage(albumId);

          long latency2 = System.currentTimeMillis() - latency1 - start;
          //System.out.println(latency);
          latencies22.add(latency2);
        }
        latencies1.addAll(latencies11);
        latencies2.addAll(latencies22);

      } catch (IOException e) {
        System.out.println("Exception caught!");
        e.printStackTrace();
      }


  }

  public String postImage(byte[] imageData, Profile profile) throws IOException {
    HttpPost post = new HttpPost(this.apiUrl);

    String profileJson = gson.toJson(profile);
    //System.out.println(profileJson);
    MultipartEntityBuilder builder = MultipartEntityBuilder.create();
    builder.addBinaryBody("image", imageData, ContentType.IMAGE_JPEG, "image.jpg");
    builder.addTextBody("profile", profileJson, ContentType.APPLICATION_JSON);

    HttpEntity multipart = builder.build();
    post.setEntity(multipart);

    try (CloseableHttpResponse response = httpClient.execute(post)) {
      //System.out.println("111111");
      int statusCode = response.getStatusLine().getStatusCode();
      //System.out.println("aaa " + statusCode);
      if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_CREATED) {
        String responseString = EntityUtils.toString(response.getEntity());
        //System.out.println(responseString);
        AlbumResponse albumResponse = gson.fromJson(responseString, AlbumResponse.class);
        String albumId = albumResponse.getAlbumID();
        //System.out.println(albumId);

        return albumId;
      } else {
        System.out.println("POST request failed with status code " + statusCode);
        FAILED_REQ.incrementAndGet();
        return "1";
      }
    }
  }

  public void getImage(String albumId) throws IOException {
    HttpGet get = new HttpGet(this.apiUrl + "/" + albumId);

    try (CloseableHttpResponse response = httpClient.execute(get)) {
      int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode == HttpStatus.SC_OK) {
        String responseString = EntityUtils.toString(response.getEntity());
        // Do something with the response if needed
      } else {
        System.out.println("GET request failed with status code " + statusCode);
        FAILED_REQ.incrementAndGet();
      }
    }
  }

  public static void calculateAndDisplayStatistics(ConcurrentLinkedQueue<Long> latenciesQueue) {


    //System.out.println("Wall time: " + (System.currentTimeMillis() - startTime) * 0.001
    List<Long> latencies = new ArrayList<>(latenciesQueue);
    Collections.sort(latencies);
    long min = latencies.get(0);
    long max = latencies.get(latencies.size() - 1);
    double median = latencies.size() % 2 == 0 ?
        (latencies.get(latencies.size() / 2) + latencies.get(latencies.size() / 2 - 1)) / 2.0 :
        latencies.get(latencies.size() / 2);
    double average = latencies.stream().mapToLong(val -> val).average().orElse(0.0);
    long p99 = latencies.get((int) (latencies.size() * 0.99));

    System.out.println("Min Latency: " + min + " ms");
    System.out.println("Max Latency: " + max + " ms");
    System.out.println("Median Latency: " + median + " ms");
    System.out.println("Average Latency: " + average + " ms");
    System.out.println("99th Percentile Latency: " + p99 + " ms");
  }

  public void shutdown() throws IOException {
    httpClient.close();
  }



  public static byte[] convertImageToByteArray(String imagePath) {
    try {

      // Uses java.nio.file.Files to read all bytes from the specified file into a byte array
      return Files.readAllBytes(Paths.get(imagePath));
    } catch (IOException e) {
      // Handle the exception according to your needs
      e.printStackTrace();
      return null;
    }
  }
}

