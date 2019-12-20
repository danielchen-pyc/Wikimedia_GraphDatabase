package cpen221.mp3.server;

import com.google.gson.*;
import cpen221.mp3.wikimediator.WikiMediator;
import fastily.jwiki.core.Wiki;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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
     * @throws IOException if the port number is invalid
     */
    public WikiMediatorServer(int port, int n) throws IOException{
        this.maxClients = n;

        ServerSocket socket;
        boolean connected = false;
        try {
            socket = new ServerSocket(port);
            connected = true;
        } catch (IOException e) {
            throw new IOException("Could not establish connection to specified port");
        } finally {
            if (!connected) {
                socket = null;
            }
        }

        this.serverSocket = socket;
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
        WikiMediator wm = new WikiMediator();

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

        try {
            JsonElement json = JsonParser.parseReader(in);
            if (json.isJsonArray()) {
                JsonArray requestArray = json.getAsJsonArray();
                for (int i = 0; i < requestArray.size(); i++) {
                    JsonObject request = requestArray.get(i).getAsJsonObject();
                    String id = "";
                    try {
                        id = request.get("id").getAsString();
                    } catch (Exception e) {
                        id = null;
                    }

                    String type = "";
                    try {
                        type = request.get("type").getAsString();
                    } catch (Exception e) { /* ignore it for now */ }

                    String query = "";
                    try {
                        query = request.get("query").getAsString();
                    } catch (Exception e) {
                        query = null;
                    }

                    String timeout = "";
                    try {
                        timeout = request.get("timeout").getAsString();
                    } catch (Exception e) {
                        timeout = null;
                    }

                    String pageTitle = "";
                    try {
                        pageTitle = request.get("pageTitle").getAsString();
                    } catch (Exception e) {
                        pageTitle = null;
                    }

                    int limit = 0;
                    try {
                        limit = Integer.parseInt(request.get("limit").getAsString());
                    } catch (Exception e) { /* ignore it for now */ }

                    int hops = 0;
                    try {
                        hops = Integer.parseInt(request.get("hops").getAsString());
                    } catch (Exception e) { /* ignore it for now */ }

                    Request newRequest = new Request(id, type, timeout, query, limit, pageTitle, hops);
                    execute(newRequest, wm, out);
                }
            }
        } finally {
            in.close();
            out.close();
        }

        // the Request and Response classes are so we can use Gson easily

        out.close();
        in.close();
    }

    private void execute(Request newRequest, WikiMediator wm, PrintWriter out) {
        Gson gson = new Gson();
        String id = newRequest.getId();
        Response response;
        long startTime = System.currentTimeMillis();

        switch (newRequest.getType()) {
            case "simpleSearch": {
                if (newRequest.getQuery() != null) {
                    response = new Response(id, "success",
                            wm.simpleSearch(newRequest.getQuery(), newRequest.getLimit()).toString());
                } else {
                    response = new Response(id, "failed", "Query is null.");
                }
            }

            case "getPage": {
                if (newRequest.getPageTitle() != null) {
                    response = new Response(id, "success", wm.getPage(newRequest.getPageTitle()));
                } else {
                    response = new Response(id, "failed", "pageTitle is null");
                }
            }

            case "getConnectedPages": {
                if (newRequest.getTimeout() != null && !newRequest.getTimeout().equals("")) {
                    String result = wm.getConnectedPages(newRequest.getPageTitle(), newRequest.getHops()).toString();
                    long currentTime = System.currentTimeMillis();
                    if (newRequest.getPageTitle() != null
                            && currentTime - startTime <= Integer.parseInt(newRequest.getTimeout())) {
                        response = new Response(id, "success", result);
                    } else if (newRequest.getPageTitle() == null){
                        response = new Response(id, "failed", "pageTitle is null");
                    } else {
                        response = new Response(id, "failed", "Operation timed out");
                    }
                } else {
                    response = new Response(id, "failed", "Invalid Timeout");
                }
            }

            case "zeitgeist": {
                String result = wm.zeitgeist(newRequest.getLimit()).toString();
                long currentTime = System.currentTimeMillis();
                if (currentTime - startTime <= Integer.parseInt(newRequest.getTimeout())) {
                    response = new Response(id, "success", result);
                } else {
                    response = new Response(id, "failed", "Operation timed out");
                }
            }

            case "trending": {
                String result = wm.trending(newRequest.getLimit()).toString();
                long currentTime = System.currentTimeMillis();
                if (currentTime - startTime <= Integer.parseInt(newRequest.getTimeout())) {
                    response = new Response(id, "success", result);
                } else {
                    response = new Response(id, "failed", "Operation timed out");
                }

            }

            case "peakLoad30s": {
                String result = Integer.toString(wm.peakLoad30s());
                long currentTime = System.currentTimeMillis();
                if (currentTime - startTime <= Integer.parseInt(newRequest.getTimeout())) {
                    response = new Response(id, "success", Integer.toString(wm.peakLoad30s()));
                } else {
                    response = new Response(id, "failed", "Operation timed out");
                }
            }

            default: response = new Response(id, "failed", "Operation Failed");
        }

        gson.toJson(response, out);
    }

    /**
     * Write data to the file specified by dataPath.
     *
     * @param data the data to store locally
     */
    private synchronized void writeToLocal(String data) {
        try(FileOutputStream fileOutputStream = new FileOutputStream(dataPath)) {
            fileOutputStream.write(data.getBytes());
        } catch (IOException e) {
            e.printStackTrace(); // exception handling
        }
    }

    /**
     * Retrieve the data stored in the local file specified by dataPath
     *
     * @param search an optional search term
     * @return the entire file located at dataPath, or the parts of the file
     *          specified by search
     */
    private synchronized String readFromLocal(String... search) throws IOException {

        return Files.readString(Paths.get(dataPath), StandardCharsets.US_ASCII);
    }

}
