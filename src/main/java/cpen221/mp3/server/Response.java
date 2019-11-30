package cpen221.mp3.server;

public class Response {

    /**
     * Rep Invariants
     *
     * id not null
     * status not null
     * response not null
     *
     * Abstraction Functions
     *
     * id       -> the value of the id       property in the json response sent by WikiMediatorServer
     * status   -> the value of the status   property in the json response sent by WikiMediatorServer
     * response -> the value of the response property in the json response sent by WikiMediatorServer
     */

    private String id;
    private String status;
    private String response;

    public Response(String id, String status, String response) {
        this.id = id;
        this.status = status;
        this.response = response;
    }

    public String getId() {
        return id;
    }

    public String getResponse() {
        return response;
    }

    public String getStatus() {
        return status;
    }
}
