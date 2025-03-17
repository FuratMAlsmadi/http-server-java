import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

/**
 * A simple HTTP server that responds to GET and POST requests.
 */
public class HttpServer {
    // Constants
    private static final int DEFAULT_PORT = 4221;
    private static final String CRLF = "\r\n";

    // Server configuration
    private final int port;
    private final Path directoryPath;
    private final ExecutorService threadPool;
    private final Map<String, RequestHandler> handlers = new HashMap<>();

    /**
     * Creates a new HTTP server with the specified configuration.
     */
    private HttpServer(Builder builder) {
        this.port = builder.port;
        this.directoryPath = builder.directoryPath;
        this.threadPool = Executors.newCachedThreadPool();

        // Register default handlers
        registerHandlers();
    }

    /**
     * Builder for creating HttpServer instances.
     */
    public static class Builder {
        private int port = DEFAULT_PORT;
        private Path directoryPath = Paths.get(".");

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder directory(String directory) {
            this.directoryPath = Paths.get(directory);
            return this;
        }

        public HttpServer build() {
            return new HttpServer(this);
        }
    }

    /**
     * Registers all request handlers.
     */
    private void registerHandlers() {
        handlers.put("echo", new EchoHandler());
        handlers.put("user-agent", new UserAgentHandler());
        handlers.put("files", new FileHandler(directoryPath));
        // Add more handlers as needed
    }

    /**
     * Starts the HTTP server.
     */
    public void start() throws IOException {
        System.out.println("Starting HTTP server on port " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(() -> handleConnection(clientSocket));
            }
        }
    }

    /**
     * Handles a client connection in a separate thread.
     */
    private void handleConnection(Socket clientSocket) {
        try (clientSocket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = clientSocket.getOutputStream()) {

            HttpRequest request = HttpRequest.parse(reader);
            if (request == null) {
                System.out.println("Empty or invalid request received.");
                return;
            }

            processRequest(request, out);

        } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
        }
    }

    /**
     * Processes an HTTP request and sends a response.
     */
    private void processRequest(HttpRequest request, OutputStream out) throws IOException {
        String path = request.getPath();

        // Handle root path or empty path
        if (path.isEmpty() || path.equals("/")) {
            sendResponse(ResponseFactory.createOk(""), out);
            return;
        }

        String[] pathParts = path.substring(1).split("/");
        if (pathParts.length == 0) {
            sendResponse(ResponseFactory.createNotFound(), out);
            return;
        }

        RequestHandler handler = handlers.get(pathParts[0]);
        if (handler != null) {
            HttpResponse response = handler.handle(request, pathParts);
            sendResponse(response, out);
        } else {
            sendResponse(ResponseFactory.createNotFound(), out);
        }
    }

    /**
     * Sends an HTTP response to the client.
     */
    private void sendResponse(HttpResponse response, OutputStream out) throws IOException {
        response.writeTo(out);
    }

    /**
     * Interface for handling different types of HTTP requests.
     */
    interface RequestHandler {
        HttpResponse handle(HttpRequest request, String[] pathParts);
    }

    /**
     * Handler for echo requests.
     */
    private static class EchoHandler implements RequestHandler {
        @Override
        public HttpResponse handle(HttpRequest request, String[] pathParts) {
            if (pathParts.length < 2) {
                return ResponseFactory.createNotFound();
            }

            String content = pathParts[1];
            if (request.acceptsEncoding("gzip")) {
                return ResponseFactory.createGzipped(content);
            }
            return ResponseFactory.createOk(content);
        }
    }

    /**
     * Handler for user-agent requests.
     */
    private static class UserAgentHandler implements RequestHandler {
        @Override
        public HttpResponse handle(HttpRequest request, String[] pathParts) {
            return ResponseFactory.createOk(request.getHeader("User-Agent"));
        }
    }

    /**
     * Handler for file requests.
     */
    private static class FileHandler implements RequestHandler {
        private final Path directoryPath;

        FileHandler(Path directoryPath) {
            this.directoryPath = directoryPath;
        }

        @Override
        public HttpResponse handle(HttpRequest request, String[] pathParts) {
            if (pathParts.length < 2) {
                return ResponseFactory.createNotFound();
            }

            String fileName = pathParts[1];
            Path filePath = directoryPath.resolve(fileName);

            if ("POST".equals(request.getMethod())) {
                return handleFileUpload(filePath, request.getBody());
            } else {
                return handleFileDownload(filePath);
            }
        }

        private HttpResponse handleFileUpload(Path filePath, String content) {
            try {
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
                return ResponseFactory.createCreated();
            } catch (IOException e) {
                System.err.println("Error creating file: " + e.getMessage());
                return ResponseFactory.createNotFound();
            }
        }

        private HttpResponse handleFileDownload(Path filePath) {
            try {
                String content = Files.readString(filePath);
                return ResponseFactory.createOk(ContentType.OCTET_STREAM, content);
            } catch (IOException e) {
                return ResponseFactory.createNotFound();
            }
        }
    }

    /**
     * Immutable representation of an HTTP request.
     */
    static class HttpRequest {
        private final String method;
        private final String path;
        private final Map<String, String> headers;
        private final String body;

        private HttpRequest(String method, String path, Map<String, String> headers, String body) {
            this.method = method;
            this.path = path;
            this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
            this.body = body;
        }

        public String getMethod() {
            return method;
        }

        public String getPath() {
            return path;
        }

        public String getBody() {
            return body;
        }

        public String getHeader(String name) {
            return headers.getOrDefault(name, "");
        }

        public boolean acceptsEncoding(String encoding) {
            String acceptEncoding = getHeader("Accept-Encoding");
            return acceptEncoding.contains(encoding);
        }

        /**
         * Parses an HTTP request from a reader.
         */
        public static HttpRequest parse(BufferedReader reader) throws IOException {
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return null;
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 3) {
                return null;
            }

            String method = requestParts[0];
            String path = requestParts[1];

            // Parse headers
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                int colonIndex = headerLine.indexOf(':');
                if (colonIndex > 0) {
                    String name = headerLine.substring(0, colonIndex).trim();
                    String value = headerLine.substring(colonIndex + 1).trim();
                    headers.put(name, value);
                }
            }

            // Read body if Content-Length is present
            String body = "";
            if (headers.containsKey("Content-Length")) {
                int contentLength = Integer.parseInt(headers.get("Content-Length"));
                if (contentLength > 0) {
                    char[] bodyChars = new char[contentLength];
                    reader.read(bodyChars, 0, contentLength);
                    body = new String(bodyChars);
                }
            }

            return new HttpRequest(method, path, headers, body);
        }
    }

    /**
     * Content types for HTTP responses.
     */
    enum ContentType {
        TEXT_PLAIN("text/plain"),
        OCTET_STREAM("application/octet-stream");

        private final String value;

        ContentType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Immutable representation of an HTTP response.
     */
    static class HttpResponse {
        private final String statusLine;
        private final Map<String, String> headers;
        private final String body;
        private final byte[] binaryBody;

        private HttpResponse(String statusLine, Map<String, String> headers, String body, byte[] binaryBody) {
            this.statusLine = statusLine;
            this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
            this.body = body;
            this.binaryBody = binaryBody;
        }

        /**
         * Writes the HTTP response to the output stream.
         */
        public void writeTo(OutputStream out) throws IOException {
            StringBuilder headerBuilder = new StringBuilder();
            headerBuilder.append(statusLine).append(CRLF);

            for (Map.Entry<String, String> header : headers.entrySet()) {
                headerBuilder.append(header.getKey())
                        .append(": ")
                        .append(header.getValue())
                        .append(CRLF);
            }

            headerBuilder.append(CRLF);
            out.write(headerBuilder.toString().getBytes(StandardCharsets.UTF_8));

            // Write the appropriate body
            if (binaryBody != null) {
                out.write(binaryBody);
            } else if (body != null) {
                out.write(body.getBytes(StandardCharsets.UTF_8));
            }
            out.flush();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(statusLine).append(CRLF);

            for (Map.Entry<String, String> header : headers.entrySet()) {
                builder.append(header.getKey())
                        .append(": ")
                        .append(header.getValue())
                        .append(CRLF);
            }

            builder.append(CRLF);
            if (body != null) {
                builder.append(body);
            } else if (binaryBody != null) {
                builder.append("[binary data]");
            }
            return builder.toString();
        }

        /**
         * Builder for creating HttpResponse instances.
         */
        static class Builder {
            private final String statusLine;
            private final Map<String, String> headers = new HashMap<>();
            private String body = null;
            private byte[] binaryBody = null;

            Builder(String statusLine) {
                this.statusLine = statusLine;
            }

            Builder header(String name, String value) {
                headers.put(name, value);
                return this;
            }

            Builder contentType(ContentType type) {
                return header("Content-Type", type.getValue());
            }

            Builder contentLength(int length) {
                return header("Content-Length", String.valueOf(length));
            }

            Builder body(String body) {
                this.body = body;
                this.binaryBody = null;
                return contentLength(body.length());
            }

            Builder binaryBody(byte[] binaryBody) {
                this.binaryBody = binaryBody;
                this.body = null;
                return contentLength(binaryBody.length);
            }

            HttpResponse build() {
                return new HttpResponse(statusLine, headers, body, binaryBody);
            }
        }
    }

    /**
     * Factory for creating common HTTP responses.
     */
    static class ResponseFactory {
        private static final String HTTP_OK = "HTTP/1.1 200 OK";
        private static final String HTTP_CREATED = "HTTP/1.1 201 Created";
        private static final String HTTP_NOT_FOUND = "HTTP/1.1 404 Not Found";

        static HttpResponse createOk(String body) {
            return createOk(ContentType.TEXT_PLAIN, body);
        }

        static HttpResponse createOk(ContentType contentType, String body) {
            return new HttpResponse.Builder(HTTP_OK)
                    .contentType(contentType)
                    .body(body)
                    .build();
        }

        static HttpResponse createGzipped(String body) {
            byte[] compressed = compressGzip(body);

            return new HttpResponse.Builder(HTTP_OK)
                    .contentType(ContentType.TEXT_PLAIN)
                    .header("Content-Encoding", "gzip")
                    .binaryBody(compressed)
                    .build();
        }

        static HttpResponse createCreated() {
            return new HttpResponse.Builder(HTTP_CREATED).build();
        }

        static HttpResponse createNotFound() {
            return new HttpResponse.Builder(HTTP_NOT_FOUND).build();
        }

        private static byte[] compressGzip(String data) {
            try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                 GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {

                gzipStream.write(data.getBytes(StandardCharsets.UTF_8));
                gzipStream.close(); // Ensure all data is flushed
                return byteStream.toByteArray();

            } catch (IOException e) {
                throw new UncheckedIOException("Failed to compress data", e);
            }
        }
    }
}