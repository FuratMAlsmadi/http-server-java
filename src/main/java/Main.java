import java.io.IOException;

public class Main {
    /**
     * Entry point for the HTTP server application.
     */
    public static void main(String[] args) {
        HttpServer.Builder builder = new HttpServer.Builder();

        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if ("--directory".equals(args[i]) && i + 1 < args.length) {
                builder.directory(args[i + 1]);
                i++;
            } else if ("--port".equals(args[i]) && i + 1 < args.length) {
                try {
                    builder.port(Integer.parseInt(args[i + 1]));
                    i++;
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port number: " + args[i + 1]);
                }
            }
        }

        try {
            HttpServer server = builder.build();
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
    }
