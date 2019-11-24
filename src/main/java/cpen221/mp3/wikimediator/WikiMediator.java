package cpen221.mp3.wikimediator;

import fastily.jwiki.core.Wiki;

import java.util.ArrayList;
import java.util.List;

public class WikiMediator {

    /* TODO: Implement this datatype

        You must implement the methods with the exact signatures
        as provided in the statement for this mini-project.

        You must add method signatures even for the methods that you
        do not plan to implement. You should provide skeleton implementation
        for those methods, and the skeleton implementation could return
        values like null.

     */

    private Wiki wiki = new Wiki("en.wikipedia.org");

    /**
     * Given a `query`, return up to `limit` page titles that match the
     * query string (per Wikipedia's search service).
     *
     * @param query
     * @param limit
     * @return
     */
    public List<String> simpleSearch(String query, int limit) {
        ArrayList<String> titles = wiki.search(query, limit);
        ArrayList<String> listOfSearch = new ArrayList<>();

        for (String title: titles) {
            listOfSearch.add(getPage(title));
        }

        return listOfSearch;
    }

    /**
     * Given a `pageTitle`, return the text associated with the Wikipedia
     * page that matches `pageTitle`.
     *
     * @param pageTitle
     * @return
     */
    public String getPage(String pageTitle) {

        return wiki.getPageText(pageTitle);
    }

    /**
     * Return a list of page titles that can be reached by following up to
     * `hops` links starting with the page specified by `pageTitle`.
     *
     * @param pageTitle
     * @param hops
     * @return
     */
    public List<String> getConnectedPages(String pageTitle, int hops) {

        return null;
    }

    /**
     * Return the most common `String`s used in `simpleSearch` and `getPage`
     * requests, with items being sorted in non-increasing count order. When
     * many requests have been made, return only `limit` items.
     *
     * @param limit
     * @return
     */
    public List<String> zeitgeist(int limit) {
        return null;
    }

    /**
     * Similar to `zeitgeist()`, but returns the most frequent requests made
     * in the last 30 seconds.
     *
     * @param limit
     * @return
     */
    public List<String> trending(int limit) {
        return null;
    }

    /**
     * What is the maximum number of requests seen in any 30-second window?
     *
     * @return
     */
    public int peakLoad30s() {
        return 0;
    }

    //TODO - task 3 stuff

}
