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
import java.util.LinkedList;
import java.util.List;

import static java.lang.Thread.interrupted;

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
    public WikiMediatorServer(int port, int n) throws IllegalArgumentException {
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

            // TODO: IOException
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

                    String startPage = "";
                    try {
                        startPage = request.get("startPage").getAsString();
                    } catch (Exception e) {
                        startPage = null;
                    }

                    String stopPage = "";
                    try {
                        stopPage = request.get("stopPage").getAsString();
                    } catch (Exception e) {
                        stopPage = null;
                    }

                    Request newRequest = new Request(id, type, timeout, query, limit, pageTitle, hops, startPage, stopPage);
                    execute(newRequest, wm);
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

    private void execute2(Request newRequest, WikiMediator wm) {
        Gson gson = new Gson();
        String id = newRequest.getId();
        Response response = null;
        ListResponse listResponse = null;
        long startTime = System.currentTimeMillis();

        switch (newRequest.getType()) {
            case "simpleSearch": {
                if (newRequest.getQuery() != null) {
                    listResponse = new ListResponse(id, "success", wm.simpleSearch(newRequest.getQuery(), newRequest.getLimit()));
                    // response = new Response(id, "success", wm.simpleSearch(newRequest.getQuery(), newRequest.getLimit()).toString());
                } else {
                    response = new Response(id, "failed", "Query is null.");
                }
                break;
            }

            case "getPage": {
                if (newRequest.getPageTitle() != null) {
                    response = new Response(id, "success", wm.getPage(newRequest.getPageTitle()));
                } else {
                    response = new Response(id, "failed", "pageTitle is null");
                }
                break;
            }

            case "getConnectedPages": {
                if (newRequest.getTimeout() != null && !newRequest.getTimeout().equals("")) {
                    List<String> result = wm.getConnectedPages(newRequest.getPageTitle(), newRequest.getHops());
                    long currentTime = System.currentTimeMillis();
                    if (newRequest.getPageTitle() != null
                            && currentTime - startTime <= Integer.parseInt(newRequest.getTimeout())) {
                        listResponse = new ListResponse(id, "success", result);
                    } else if (newRequest.getPageTitle() == null){
                        response = new Response(id, "failed", "pageTitle is null");
                    } else {
                        response = new Response(id, "failed", "Operation timed out");
                    }
                } else {
                    response = new Response(id, "failed", "Invalid Timeout");
                }
                break;
            }

            case "zeitgeist": {
                List<String> result = wm.zeitgeist(newRequest.getLimit());
                long currentTime = System.currentTimeMillis();
                if (currentTime - startTime <= Integer.parseInt(newRequest.getTimeout())) {
                    listResponse = new ListResponse(id, "success", result);
                } else {
                    response = new Response(id, "failed", "Operation timed out");
                }
                break;
            }

            case "trending": {
                List<String> result = wm.trending(newRequest.getLimit());
                long currentTime = System.currentTimeMillis();
                if (currentTime - startTime <= Integer.parseInt(newRequest.getTimeout())) {
                    listResponse = new ListResponse(id, "success", result);
                } else {
                    response = new Response(id, "failed", "Operation timed out");
                }
                break;
            }

            case "peakLoad30s": {
                String result = Integer.toString(wm.peakLoad30s());
                long currentTime = System.currentTimeMillis();
                if (currentTime - startTime <= Integer.parseInt(newRequest.getTimeout())) {
                    response = new Response(id, "success", Integer.toString(wm.peakLoad30s()));
                } else {
                    response = new Response(id, "failed", "Operation timed out");
                }
                break;
            }

            case "getPath": {
                if (newRequest.getTimeout() != null) {

                } else {
                    try {
                        listResponse = new ListResponse(id, "success", wm.getPath(newRequest.getStartPage(), newRequest.getStopPage()));
                    } catch (IllegalArgumentException e) {
                        response = new Response(id, "failed", e.getMessage());
                    }
                }
                break;
            }

            default: {
                response = new Response(id, "failed", "Operation Failed");
                break;
            }
        }

        if (response == null) {
            writeToLocal(gson.toJson(listResponse));
        } else {
            writeToLocal(gson.toJson(response));
        }
    }

    private void execute(Request newRequest, WikiMediator wm) {
        Gson gson = new Gson();
        String JSONResponse = null;

        if (newRequest.getTimeout() != null) {
            wm.timeout = Integer.parseInt(newRequest.getTimeout());
        }

        JSONResponse = executeRequest(newRequest, wm);

        if (JSONResponse == null) {
            JSONResponse = gson.toJson(new Response(newRequest.getId(), "failed", "Operation timed out"));
        }

        writeToLocal(JSONResponse);
    }

    private String executeRequest(Request newRequest, WikiMediator wm) {
        Gson gson = new Gson();
        String id = newRequest.getId();
        Response response = null;
        ListResponse listResponse = null;
        long startTime = System.currentTimeMillis();

        switch (newRequest.getType()) {
            case "simpleSearch": {
                if (newRequest.getQuery() != null) {
                    listResponse = new ListResponse(id, "success", wm.simpleSearch(newRequest.getQuery(), newRequest.getLimit()));
                } else {
                    response = new Response(id, "failed", "Query is null.");
                }
                break;
            }

            case "getPage": {
                if (newRequest.getPageTitle() != null) {
                    response = new Response(id, "success", wm.getPage(newRequest.getPageTitle()));
                } else {
                    response = new Response(id, "failed", "pageTitle is null");
                }
                break;
            }

            case "getConnectedPages": {
                if (newRequest.getTimeout() != null && !newRequest.getTimeout().equals("")) {
                    List<String> result = wm.getConnectedPages(newRequest.getPageTitle(), newRequest.getHops());
                    long currentTime = System.currentTimeMillis();
                    if (newRequest.getPageTitle() != null
                            && currentTime - startTime <= Integer.parseInt(newRequest.getTimeout())) {
                        listResponse = new ListResponse(id, "success", result);
                    } else if (newRequest.getPageTitle() == null){
                        response = new Response(id, "failed", "pageTitle is null");
                    } else {
                        response = new Response(id, "failed", "Operation timed out");
                    }
                } else {
                    response = new Response(id, "failed", "Invalid Timeout");
                }
                break;
            }

            case "zeitgeist": {
                List<String> result = wm.zeitgeist(newRequest.getLimit());
                long currentTime = System.currentTimeMillis();
                if (currentTime - startTime <= Integer.parseInt(newRequest.getTimeout())) {
                    listResponse = new ListResponse(id, "success", result);
                } else {
                    response = new Response(id, "failed", "Operation timed out");
                }
                break;
            }

            case "trending": {
                List<String> result = wm.trending(newRequest.getLimit());
                long currentTime = System.currentTimeMillis();
                if (currentTime - startTime <= Integer.parseInt(newRequest.getTimeout())) {
                    listResponse = new ListResponse(id, "success", result);
                } else {
                    response = new Response(id, "failed", "Operation timed out");
                }
                break;
            }

            case "peakLoad30s": {
                String result = Integer.toString(wm.peakLoad30s());
                long currentTime = System.currentTimeMillis();
                if (currentTime - startTime <= Integer.parseInt(newRequest.getTimeout())) {
                    response = new Response(id, "success", Integer.toString(wm.peakLoad30s()));
                } else {
                    response = new Response(id, "failed", "Operation timed out");
                }
                break;
            }

            case "getPath": {
                try {
                    listResponse = new ListResponse(id, "success", wm.getPath(newRequest.getStartPage(), newRequest.getStopPage()));
                } catch (IllegalArgumentException e) {
                    response = new Response(id, "failed", e.getMessage());
                }
                break;
            }

            default: {
                response = new Response(id, "failed", "Operation Failed");
                break;
            }
        }

        if (response == null) {
            return gson.toJson(listResponse);
        } else {
            return gson.toJson(response);
        }
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


    public static void main(String[] args) {
        try {
            WikiMediatorServer server = new WikiMediatorServer(100, 2);
            server.serve();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
