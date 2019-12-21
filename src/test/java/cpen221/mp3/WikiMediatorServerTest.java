package cpen221.mp3;

import com.google.gson.Gson;
import cpen221.mp3.server.Request;
import cpen221.mp3.server.WikiMediatorServer;
import cpen221.mp3.wikimediator.WikiMediator;
import fastily.jwiki.core.Wiki;
import org.junit.Test;

import java.io.*;
import java.net.Socket;
import java.util.logging.SocketHandler;

public class WikiMediatorServerTest {

    public final int PORT_NUMBER = 100;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    public WikiMediatorServerTest() throws IOException {
        socket = new Socket("localhost", PORT_NUMBER);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    }

    @Test
    public void testWikiMediatorServerConstructor() throws IllegalArgumentException {
        WikiMediatorServer wms = new WikiMediatorServer(30, 2);
        WikiMediatorServer wms1 = new WikiMediatorServer(0, 100);
        WikiMediatorServer wms2 = new WikiMediatorServer(1000, 0);

        try {
            WikiMediatorServer wms3 = new WikiMediatorServer(100000, 2);
        } catch (IllegalArgumentException ioe) {
            ioe.printStackTrace();
        }
    }

    @Test
    public void testWikiMediatorServer() {
        Gson gson = new Gson();

        try {
            // WikiMediatorServer wms = new WikiMediatorServer(PORT_NUMBER, 5);
            WikiMediatorServerTest wmst = new WikiMediatorServerTest();
            Request request = new Request("1", "simpleSearch", "", "Canada", 10, "", 0);
            out.print(gson.toJson(request));
            out.flush();
            System.out.println(in.readLine());

            in.close();
            out.close();
            socket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
