package cpen221.mp3.server;

import java.util.List;

public class ListResponse {

    private String id;
    private String status;
    private List<String> response;

    public ListResponse(String id, String status, List<String> response) {
        this.id = id;
        this.status = status;
        this.response = response;
    }

    public String getId() {
        return id;
    }

    public List<String> getResponse() {
        return response;
    }

    public String getStatus() {
        return status;
    }
}
