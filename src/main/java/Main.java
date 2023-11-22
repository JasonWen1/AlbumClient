import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
  public static void main(String[] args) throws IOException, InterruptedException {
    ExecutorService executorService = Executors.newFixedThreadPool(300);
    if (args.length < 4) {
      System.out.println("Usage: java ApiClient <threadGroupSize> <numThreadGroups> <delay> <apiUrl>");
      return;
    }

    int threadGroupSize = Integer.parseInt(args[0]);
    int numThreadGroups = Integer.parseInt(args[1]);
    int delay = Integer.parseInt(args[2]);
    String apiUrl = args[3];


    ApiClient client = new ApiClient(2000, 1000);
    client.setApiUrl(apiUrl);
    byte[] imageData = ApiClient.convertImageToByteArray(System.getProperty("user.dir") + "/" + "nmtb.png");
    //System.out.println(imageData.length);
    Profile profile = new Profile("John Doe", "abc", "2023");
    long startTime = System.currentTimeMillis();
    for (int group = 0; group < numThreadGroups; group++) {
      for (int thread = 0; thread < threadGroupSize; thread++) {
        System.out.println("Start of executeTask");
        executorService.submit(() -> {
          System.out.println("Start of submit");
          client.executeTask(imageData, profile);
          System.out.println("End of submit");
        });
        System.out.println("End of executeTask");
      }
      TimeUnit.SECONDS.sleep(delay);
    }
    executorService.shutdown();
    executorService.awaitTermination(120, TimeUnit.MINUTES);
    System.out.println("Wall time: " + (System.currentTimeMillis() - startTime) * 0.001);
    System.out.println("Successful requests: " + (2000 * threadGroupSize * numThreadGroups - client.FAILED_REQ.get()));
    System.out.println("Failed requests: " + client.FAILED_REQ.get());
    System.out.println("Throughput: " + (2000 * threadGroupSize * numThreadGroups - client.FAILED_REQ.get()) / ((System.currentTimeMillis() - startTime) * 0.001) + " req/s");
    System.out.println("Statistics for POST request: ");
    client.calculateAndDisplayStatistics(client.latencies1);
    System.out.println();
    System.out.println("Statistics for GET request: ");
    client.calculateAndDisplayStatistics(client.latencies2);
    client.shutdown();
  }
}
