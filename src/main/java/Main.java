import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

public class Main {
  public static void main(String[] args) {
    // You can use print statements as follows for debugging, they'll be visible when running tests.
    System.out.println("Logs from your program will appear here!");

    //Uncomment this block to pass the first stage
    
    try {
      ServerSocket serverSocket = new ServerSocket(4221);
    
      // Since the tester restarts your program quite often, setting SO_REUSEADDR
      // ensures that we don't run into 'Address already in use' errors
      serverSocket.setReuseAddress(true);
    
      Socket clientSocket = serverSocket.accept(); // Wait for connection from client.
      System.out.println("accepted new connection");

      InputStream inputStream = clientSocket.getInputStream();
      InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "Utf-8");
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      List<String> requestStringList = Arrays.asList(bufferedReader.readLine().split("\\s+"));
      if(requestStringList.get(1).equals("/")) {
        clientSocket.getOutputStream().write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
      }
      else {
        clientSocket.getOutputStream().write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
      }

    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

}
