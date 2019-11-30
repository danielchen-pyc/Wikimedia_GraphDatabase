package cpen221.mp3.server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class WikiMediatorServer {

    private final int maxClients;
    private final ServerSocket serverSocket;

    private final String dataPath = "local/data.json";

    /**
     * Rep Invariants
     *
     * maxClients >= 0
     * serverSocket not null
     * dataPath = the string "local/data.json"
     *
     * ---------------------------------------------------------------------------
     *
     * Abstraction Functions
     *
     * maxClients -> the maximum number of concurrent requests the server can handle
     * serverSocket -> a socket bound to the specified port
     * dataPath -> the relative path to the file where statistics data is stored
     *
     * ---------------------------------------------------------------------------
     *
     * Thread Safety Arguments
     *
     * This class is Thread-safe because
     * - Except for statistics data, all data is confined to each thread
     * - The statistics data is read from and written to through synchronized
     *   functions, eliminating data races.
     */

    /**
     * Start a server at a given port number, with the ability to process
     * upto n requests concurrently.
     *
     * @param port the port number to bind the server to
     * @param n the number of concurrent requests the server can handle
     * @throws IllegalArgumentException if the port number is invalid
     */
    public WikiMediatorServer(int port, int n) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Invalid port");
        }

        this.maxClients = n;

        ServerSocket socket;
        boolean connected = false;
        try {
            socket = new ServerSocket(port);
            connected = true;
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not establish connection to specified port");
        } finally {
            if (!connected) {
                socket = null;
            }
        }

        this.serverSocket = socket;

        /* TODO: Implement this method */
        // TODO - figure out IOException thing
        // do not need to store cache locally, only statistics (campuswire #1670, #1688)
        // follow fibonacciServer example
    }

    /**
     * Run the server, listening for connections and handling them.
     *
     * @throws IOException if the main server socket is broken
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
     * @param socket socket where client is connected
     * @throws IOException if connection encounters an IO error
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

        // the Request and Response classes are so we can use Gson easily

        out.close();
        in.close();
    }

    /**
     * Write data to the file specified by dataPath.
     *
     * @param data the data to store locally
     */
    private synchronized void writeToLocal(String data) {
        //TODO
    }

    /**
     * Retrieve the data stored in the local file specified by dataPath
     *
     * @param search an optional search term
     * @return the entire file located at dataPath, or the parts of the file
     *          specified by search
     */
    private synchronized String readFromLocal(String search) {
        //TODO
        return null;
    }

}
