package cpen221.mp3.wikimediator;

import cpen221.mp3.cache.Cacheable;

public class Page implements Cacheable {

    private String pageText;
    private String id;

    public Page(String pageText, String id) {
        this.pageText = pageText;
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public String getPageText() {
        return pageText;
    }
}
