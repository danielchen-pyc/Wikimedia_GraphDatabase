package cpen221.mp3.server;

public class Request {

    /**
     * Rep Invariants
     *
     * None
     *
     * Abstraction Functions
     *
     * id        -> the value of the id        property in the user's json request sent to WikiMediatorServer
     * type      -> the value of the type      property in the user's json request sent to WikiMediatorServer
     * timeout   -> the value of the timeout   property in the user's json request sent to WikiMediatorServer
     * query     -> the value of the query     property in the user's json request sent to WikiMediatorServer
     * limit     -> the value of the limit     property in the user's json request sent to WikiMediatorServer
     * pageTitle -> the value of the pageTitle property in the user's json request sent to WikiMediatorServer
     * hops      -> the value of the hops      property in the user's json request sent to WikiMediatorServer
     */

    private String id;
    private String type;
    private String timeout; // a String so we can distinguish from a
                            // 0 timeout and no timeout when using Gson

    // simpleSearch, executeQuery
    private String query;

    // simpleSearch, zeitgeist, trending
    private int limit;

    // getConnectedPages
    private String pageTitle;
    private int hops;

    public Request(String id, String type, String timeout, String query, int limit, String pageTitle, int hops) {
        this.id = id;
        this.type = type;
        this.timeout = timeout;
        this.query = query;
        this.limit = limit;
        this.pageTitle = pageTitle;
        this.hops = hops;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTimeout() {
        return timeout;
    }

    public String getQuery() {
        return query;
    }

    public int getLimit() {
        return limit;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public int getHops() {
        return hops;
    }
}
