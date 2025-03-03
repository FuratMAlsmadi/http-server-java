import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A simple HTTP server that responds to GET requests.
 */
public class Main {
  private static final int PORT = 4221;
  private static final String HTTP_OK = "HTTP/1.1 200 OK";
  private static final String HTTP_NOT_FOUND = "HTTP/1.1 404 Not Found";
  private static final String CONTENT_TYPE_PLAIN = "Content-Type: text/plain";
  private static final String CRLF = "\r\n";

  private enum RequestType {
    ECHO, USER_AGENT, UNKNOWN, EMPTY
  }

  public static void main(String[] args) {
    System.out.println("Starting HTTP server on port " + PORT);
    System.out.println("Starting HTTP server on port " + PORT);
    try (ServerSocket serverSocket = new ServerSocket(PORT)) {
      serverSocket.setReuseAddress(true);
      while (true) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Accepted new connection");
        Thread handleRequestThread = new Thread(() -> {
          try {
            handleClientRequest(clientSocket);
            clientSocket.close();
          } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
          }
        });
        handleRequestThread.start();
      }
    } catch (IOException e) {
      System.err.println("Server socket error: " + e.getMessage());
    }

  }

  private static void handleClientRequest(Socket clientSocket) throws IOException {
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8))) {

      List<String> requestHeaders = readHeaders(reader);
      if (requestHeaders.isEmpty()) {
        System.out.println("Empty request received.");
        return;
      }

      HttpRequest request = parseRequest(requestHeaders);

      // Handle the request based on its type
      switch (request.getType()) {
        case ECHO:
          handleEchoRequest(clientSocket.getOutputStream(), request.getContent());
          break;
        case USER_AGENT:
          handleUserAgentRequest(clientSocket.getOutputStream(), request.getUserAgent());
          break;
        case EMPTY:
          handleEmptyRequest(clientSocket.getOutputStream(), request.getContent());
          break;
        case UNKNOWN:
        default:
          sendNotFoundResponse(clientSocket.getOutputStream());
          break;
      }
    }
  }

  private static List<String> readHeaders(BufferedReader reader) throws IOException {
    List<String> headers = new ArrayList<>();
    String line;

    // Read headers until we reach an empty line
    while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
      headers.add(line);
    }

    return headers;
  }

  private static HttpRequest parseRequest(List<String> headers) {
    if (headers.isEmpty()) {
      return new HttpRequest(RequestType.UNKNOWN, "", "");
    }

    String requestLine = headers.get(0);
    String[] requestParts = requestLine.split(" ");

    if (requestParts.length < 2) {
      System.out.println("Invalid request format: " + requestLine);
      return new HttpRequest(RequestType.UNKNOWN, "", "");
    }

    String path = requestParts[1];
    String[] pathParts = path.split("/");
    System.out.println("Path parts: " + Arrays.toString(pathParts));

    // Extract User-Agent if present
    String userAgent = "";
    for (String header : headers) {
      if (header.startsWith("User-Agent: ")) {
        userAgent = header.substring("User-Agent: ".length());
        break;
      }
    }
    if (pathParts.length == 0) {
      return new HttpRequest(RequestType.EMPTY, "", userAgent);
    }
    if (pathParts.length >= 3 && "echo".equals(pathParts[1])) {
      return new HttpRequest(RequestType.ECHO, pathParts[2], userAgent);
    } else if (pathParts.length >= 2 && "user-agent".equals(pathParts[1])) {
      return new HttpRequest(RequestType.USER_AGENT, "", userAgent);
    } else {
      return new HttpRequest(RequestType.UNKNOWN, "", userAgent);
    }
  }

  private static void handleEchoRequest(OutputStream outputStream, String content) throws IOException {
    String response = buildResponse(HTTP_OK, CONTENT_TYPE_PLAIN, content);
    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    System.out.println("Echo response sent with content: " + content);
  }

  private static void handleEmptyRequest(OutputStream outputStream, String content) throws IOException {
    String response = buildResponse(HTTP_OK, CONTENT_TYPE_PLAIN, content);
    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    System.out.println("Empty response sent with content: " + content);
  }

  private static void handleUserAgentRequest(OutputStream outputStream, String userAgent) throws IOException {
    String response = buildResponse(HTTP_OK, CONTENT_TYPE_PLAIN, userAgent);
    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    System.out.println("User-Agent response sent with agent: " + userAgent);
  }

  private static void sendNotFoundResponse(OutputStream outputStream) throws IOException {
    String response = HTTP_NOT_FOUND + CRLF + CRLF;
    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    System.out.println("404 Not Found response sent");
  }

  private static String buildResponse(String status, String contentType, String body) {
    return status + CRLF +
        contentType + CRLF +
        "Content-Length: " + body.length() + CRLF +
        CRLF +
        body;
  }

  private static final class HttpRequest {
    private final RequestType type;
    private final String content;
    private final String userAgent;

    HttpRequest(RequestType type, String content, String userAgent) {
      this.type = Objects.requireNonNull(type);
      this.content = Objects.requireNonNull(content);
      this.userAgent = Objects.requireNonNull(userAgent);
    }

    RequestType getType() {
      return type;
    }

    String getContent() {
      return content;
    }

    String getUserAgent() {
      return userAgent;
    }
  }
}