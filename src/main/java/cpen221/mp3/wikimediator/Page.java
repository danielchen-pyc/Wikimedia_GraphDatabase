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

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Page) {
            Page p = (Page) o;
            if (this.id.equals(p.id) && this.pageText.equals(p.pageText)) {
                return true;
            }
        }

        return false;
    }
}
