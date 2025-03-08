import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A simple HTTP server that responds to GET requests.
 */
public class Main {
  private static final int PORT = 4221;
  private static final String HTTP_OK = "HTTP/1.1 200 OK";
  private static final String HTTP_NOT_FOUND = "HTTP/1.1 404 Not Found";
  private static final String HTTP_CREATED = "HTTP/1.1 201 Created";
  private static final String CRLF = "\r\n";
  private static String directory;

  private enum RequestType {
    ECHO, USER_AGENT, UNKNOWN, EMPTY, FILE, POST
  }

  private enum ContentType {
    APPOCTSTREAM("Content-Type: application/octet-stream"),
    TXTPLAIN("Content-Type: text/plain");

    private final String headerValue;

    ContentType(String heaaderValue) {
      this.headerValue = heaaderValue;
    }

    public String getHeaderValue() {
      return this.headerValue;
    }
  }

  public static void main(String[] args) {
    if (args.length > 1 && args[0].equals("--directory")) {
      directory = args[1];
    }

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
      Map<String, String> requestHeadersMaped = readHeadersMaped(reader);
      if (requestHeaders.isEmpty()) {
        System.out.println("Empty request received.");
        return;
      }

      int contentLength = Integer.parseInt(requestHeadersMaped.get("content-length").split(":", 2)[1].trim());
      String requestbody = readBody(reader, contentLength);

      HttpRequest request = parseRequest(requestHeaders, requestbody);

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
        case FILE:
          handleFileRequest(clientSocket.getOutputStream(), request.getContent());
          break;
        case POST:
          handlePostRequest(clientSocket.getOutputStream(), request);
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

  private static Map<String, String> readHeadersMaped(BufferedReader reader) throws IOException {
        Map<String, String> headers = new LinkedHashMap<>();
        String line;

        while ((line = reader.readLine()) != null && !line.trim().isEmpty()) {
            String[] parts = line.split(":", 2);
            if (parts.length == 2) {
                headers.put(parts[0].trim().toLowerCase(), parts[1].trim()); // Normalize to lowercase
            }
        }

        return headers;
    }

  private static String readBody(BufferedReader reader, int contentLength) throws IOException {
    char[] body = new char[contentLength];
    int bytesRead = reader.read(body, 0, contentLength);
    return new String(body, 0, bytesRead);
  }

  private static HttpRequest parseRequest(List<String> headers, String body) {
    if (headers.isEmpty()) {
      return new HttpRequest(RequestType.UNKNOWN, "", "");
    }

    String requestLine = headers.get(0);
    String[] requestParts = requestLine.split(" ");
    // "POST /files/file_123 HTTP/1.1"
    if ("POST".equals(requestParts[0].trim())) {
      String path = requestParts[1];
      String[] pathParts = path.split("/");
      String fileName = pathParts[2];
      return new HttpRequest(RequestType.POST, body, fileName);
    } else {
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
      } else if (pathParts.length >= 3 && "files".equals(pathParts[1])) {
        return new HttpRequest(RequestType.FILE, pathParts[2], userAgent);
      } else if (pathParts.length >= 3 && "files".equals(pathParts[1])) {
        return new HttpRequest(RequestType.FILE, pathParts[2], userAgent);
      } else {
        return new HttpRequest(RequestType.UNKNOWN, "", userAgent);
      }
    }
  }

  private static void handleEchoRequest(OutputStream outputStream, String content) throws IOException {
    String response = buildResponse(HTTP_OK, ContentType.TXTPLAIN, content);
    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    System.out.println("Echo response sent with content: " + content);
  }

  private static void handleEmptyRequest(OutputStream outputStream, String content) throws IOException {
    String response = buildResponse(HTTP_OK, ContentType.TXTPLAIN, content);
    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    System.out.println("Empty response sent with content: " + content);
  }

  private static void handleUserAgentRequest(OutputStream outputStream, String userAgent) throws IOException {
    String response = buildResponse(HTTP_OK, ContentType.TXTPLAIN, userAgent);
    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    System.out.println("User-Agent response sent with agent: " + userAgent);
  }

  private static void sendNotFoundResponse(OutputStream outputStream) throws IOException {
    String response = HTTP_NOT_FOUND + CRLF + CRLF;
    outputStream.write(response.getBytes(StandardCharsets.UTF_8));
    System.out.println("404 Not Found response sent");
  }

  private static void handlePostRequest(OutputStream outputStream, HttpRequest httpRequest) throws IOException {
    Path filePath = Paths.get(directory, httpRequest.getUserAgent());
    try {
      Files.createDirectories(filePath.getParent());

      Files.createFile(filePath);
      System.out.println("File created at: " + filePath);

      String content = httpRequest.content;
      Files.write(filePath, content.getBytes(), StandardOpenOption.WRITE);

      String response = HTTP_CREATED + CRLF + CRLF;
      outputStream.write(response.getBytes(StandardCharsets.UTF_8));
      System.out.println("201 Created response sent");

    } catch (FileAlreadyExistsException e) {
      System.out.println("File already exists: " + filePath);
    }
  }

  private static void handleFileRequest(OutputStream outputStream, String fileName) throws IOException {
    Path filePath = Paths.get(directory, fileName);
    System.out.println(filePath);
    StringBuilder fileContent = new StringBuilder();
    try (BufferedReader bufferedReader = Files.newBufferedReader(filePath)) {
      String line;
      while ((line = bufferedReader.readLine()) != null) {
        fileContent.append(line);
      }
      String response = buildResponse(HTTP_OK, ContentType.APPOCTSTREAM, fileContent.toString());
      outputStream.write(response.getBytes(StandardCharsets.UTF_8));
      System.out.println("File content response sent from file: " + fileName);
    } catch (NoSuchFileException e) {
      sendNotFoundResponse(outputStream);
    }
  }

  private static String buildResponse(String status, ContentType contentType, String body) {
    return status + CRLF +
        contentType.getHeaderValue() + CRLF +
        contentType.getHeaderValue() + CRLF +
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