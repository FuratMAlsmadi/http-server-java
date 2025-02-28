import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class Main {
  private static final int PORT = 4221;
  private static final String HTTP_OK_RESPONSE = "HTTP/1.1 200 OK\r\n";
  private static final String HTTP_NOT_FOUND_RESPONSE = "HTTP/1.1 404 Not Found\r\n";
  private static final String CONTENT_TYPE_HEADER_PLAIN_TEXT = "Content-Type: text/plain\r\n";

  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible
    // when running tests.
    System.out.println("Logs from your program will appear here!");

    // Uncomment this block to pass the first stage

    try (ServerSocket serverSocket = new ServerSocket(4221)) {
      // Since the tester restarts your program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);

      try (Socket clientSocket = serverSocket.accept();
          BufferedReader bufferedReader = new BufferedReader(
              new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8))) {
        // Wait for connection from client.
        System.out.println("accepted new connection");

        // Read the request line and split into parts.
        String requestLine = bufferedReader.readLine();
        if (requestLine == null) {
          System.out.println("Empty request received.");
          return;
        }

        List<String> requestParts = Arrays.asList(requestLine.split(" "));

        System.out.println(requestParts); // [GET, /echo/abc, HTTP/1.1]
        if (requestParts.size() < 2) {
          System.out.println("Invalid request format.");
          return;
        }

        // Expecting the request to be of the form: GET /echo/abc HTTP/1.1
        String[] pathParts = requestParts.get(1).split("/");
        if (pathParts.length < 3) {
          System.out.println("Unexpected request path format.");
          return;
        }

        // The "content" is extracted from the path (e.g., "abc").
        String content = pathParts[2];
        System.out.println("Content: " + content);

        // Build the response.
        String responseBody = content;
        String response = HTTP_OK_RESPONSE +
            CONTENT_TYPE_HEADER_PLAIN_TEXT +
            String.format("Content-Length: %d\r\n\r\n", responseBody.length()) +
            responseBody;
        System.out.println("Response:\n" + response);

        // Write the response using the correct charset.
        clientSocket.getOutputStream().write(response.getBytes(StandardCharsets.UTF_8));

      }
    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

}
