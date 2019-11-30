package cpen221.mp3.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class WikiMediatorServer {

    private int port;
    private int maxClients;
    private ServerSocket serverSocket;

    /*
     * Rep Invariants
     *
     * 0 <= port <= 65535
     * maxClients >= 0
     * serverSocket not null
     *
     * Abstraction Functions
     *
     * port -> the port the server communicates over
     * maxClients -> the maximum number of concurrent requests the server can handle
     * serverSocket -> a
     */

    /**
     * Start a server at a given port number, with the ability to process
     * upto n requests concurrently.
     *
     * @param port the port number to bind the server to
     * @param n the number of concurrent requests the server can handle
     */
    public WikiMediatorServer(int port, int n) {
        this.port = port;
        this.maxClients = n;

        /* TODO: Implement this method */
        // do not need to store cache locally, only statistics (Q # 1670)
        // follow fibonacciServer example

    }

    /**
     * Run the server, listening for connections and handling them.
     *
     * @throws IOException
     *             if the main server socket is broken
     */
    public void serve() throws IOException {
        while (true) {
            // block until a client connects
            final Socket socket = serverSocket.accept();
            // create a new thread to handle that client
            Thread handler = new Thread(new Runnable() {
                public void run() {
                    try {
                        try {
                            handle(socket);
                        } finally {
                            socket.close();
                        }
                    } catch (IOException ioe) {
                        // this exception wouldn't terminate serve(),
                        // since we're now on a different thread, but
                        // we still need to handle it
                        ioe.printStackTrace();
                    }
                }
            });
            // start the thread
            handler.start();
        }
    }

    /**
     * Handle one client connection. Returns when client disconnects.
     *
     * @param socket
     *            socket where client is connected
     * @throws IOException
     *             if connection encounters an error
     */
    private void handle(Socket socket) throws IOException {

        // get the socket's input stream, and wrap converters around it
        // that convert it from a byte stream to a character stream,
        // and that buffer it so that we can read a line at a time
        BufferedReader in = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));

        // similarly, wrap character=>bytestream converter around the
        // socket output stream, and wrap a PrintWriter around that so
        // that we have more convenient ways to write Java primitive
        // types to it.
        PrintWriter out = new PrintWriter(new OutputStreamWriter(
                socket.getOutputStream()), true);

        //TODO - implement

        out.close();
        in.close();
    }

}
