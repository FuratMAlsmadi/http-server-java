# Build Your Own HTTP Server in Java

This repository contains a starting point for Java solutions to the [CodeCrafters "Build Your Own HTTP Server" challenge](https://app.codecrafters.io/courses/http-server/overview). The challenge guides you through building an HTTP/1.1 server capable of handling multiple clients, processing basic HTTP requests, and serving static files—all implemented in Java without relying on any external build tool.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Compilation & Running](#compilation--running)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)
- [Acknowledgements](#acknowledgements)

## Overview

In this challenge, you’ll learn about the fundamentals of TCP/IP networking and the HTTP protocol by building a simple HTTP/1.1 server in Java. The server is designed to:
- Listen on a configurable port.
- Handle multiple concurrent connections.
- Process basic HTTP requests.
- Serve static content from a designated directory.

This project is a great way to deepen your understanding of Java networking and server design.

## Features

- **HTTP/1.1 Compliance:** Handles basic HTTP methods (GET/POST).
- **Concurrent Connections:** Supports multiple simultaneous clients using Java multithreading.
- **Static File Serving:** Maps URL paths to local files.

## Prerequisites

- **Java Development Kit (JDK):** Version 8 or higher.
- **Git:** For version control and submitting your solution.
- **Command-Line Tools:** Basic command-line knowledge to compile and run Java files manually.

## Compilation & Running

Since no build tool is used, you can compile and run the project directly from the command line.

1. **Clone the Repository:**

    ```bash
    git clone https://github.com/FuratMAlsmadi/http-server-java.git
    cd http-server-java
    ```

2. **Compile the Code:**

    Create a directory for compiled classes (if not already present):

    ```bash
    mkdir bin
    ```

    Then compile the Java source files (assuming they’re located in `src/main/java/`):

    ```bash
    javac -d bin src/main/java/*.java
    ```

3. **Run the Server:**

    Once compiled, start the server by running the `Main` class from the `bin` directory:

    ```bash
    java -cp bin Main
    ```

    The server will start on the default port (for example, 8080). Open your browser and navigate to:

    ```
    http://localhost:8080
    ```

*(If your `Main.java` file is in a package, adjust the compilation and run commands accordingly.)*

## Project Structure

```plaintext
http-server-java/
├── .codecrafters/         # Challenge-specific configuration files
├── src/
│   └── main/
│       └── java/
│           └── Main.java  # Entry point for the HTTP server implementation
├── your_program.sh        # Optional script to run your program
├── README.md              # This file
```

## Contributing

Contributions are welcome! If you have ideas for improvements or additional features, please follow these steps:

1. Fork the repository.
2. Create a new branch:
    ```bash
    git checkout -b feature/your-feature
    ```
3. Make your changes and commit them:
    ```bash
    git commit -am "Add new feature: description"
    ```
4. Push your branch:
    ```bash
    git push origin feature/your-feature
    ```
5. Open a pull request describing your changes.

## License

This project is provided as-is for educational purposes. See the [LICENSE](LICENSE) file for details.

## Acknowledgements

- **CodeCrafters:** For the original challenge and excellent learning resources.
- **HTTP/1.1 Specification:** For laying out the foundation of web communications.
- **Java Community:** For tools and resources that make projects like this possible.

---
