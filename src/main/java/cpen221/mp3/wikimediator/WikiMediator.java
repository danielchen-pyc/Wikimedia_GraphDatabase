package cpen221.mp3.wikimediator;

import cpen221.mp3.cache.Cache;
import cpen221.mp3.cache.Cacheable;
import fastily.jwiki.core.Wiki;
import java.util.*;
import java.util.stream.Collectors;

public class WikiMediator {

    /**
     *
     *
     */

    private final static int CACHE_CAPACITY = 256;
    private final static int CACHE_TIMEOUT = 12 * 60 * 60;  // 12 hours

    private Wiki wiki = new Wiki("en.wikipedia.org");
    private LinkedHashMap<String, Long> methodList = new LinkedHashMap<>();
    private List<CacheObject> cacheObjects = new ArrayList<>();
    private Cache<Page> cache;


    public WikiMediator() {
        cache = new Cache<>(CACHE_CAPACITY, CACHE_TIMEOUT);
    }

    /**
     * Given a `query`, return up to `limit` page titles that match the
     * query string (per Wikipedia's search service).
     *
     * @param query
     * @param limit
     * @return
     */
    public List<String> simpleSearch(String query, int limit) {
        ArrayList<String> listOfSearch = new ArrayList<>();

        this.cacheObjects.add(new CacheObject<>(query));
        this.methodList.put("simpleSearch", System.currentTimeMillis());
        this.cache.put(new Page(wiki.getPageText(query), query));

        for (String title: wiki.search(query, limit)) {
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
        Page page = new Page(wiki.getPageText(pageTitle), pageTitle);

        this.cacheObjects.add(new CacheObject<>(pageTitle));
        this.methodList.put("getPage", System.currentTimeMillis());
        this.cache.put(page);

        return page.getPageText();
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

        this.methodList.put("getConnectedPages", System.currentTimeMillis());

        return recursiveGetConnected(new ArrayList<>(), pageTitle, hops);
    }


    private ArrayList<String> recursiveGetConnected(ArrayList<String> listOfAllTitles, String pageTitle, int hops) {
        if (hops <= 0) {
            return listOfAllTitles;
        } else {
            for (int i = 0; i < wiki.getLinksOnPage(true, pageTitle).size(); i++) {
                listOfAllTitles.addAll(wiki.getLinksOnPage(true, pageTitle)
                                .stream()
                                .filter(x -> !listOfAllTitles.contains(x))
                                .collect(Collectors.toList()));
                listOfAllTitles
                        .addAll(recursiveGetConnected(listOfAllTitles, wiki.getLinksOnPage(true, pageTitle).get(i), --hops)
                        .stream()
                        .filter(x -> !listOfAllTitles.contains(x))
                        .collect(Collectors.toList()));
            }
            return listOfAllTitles;
        }
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
        List<CacheObject> list = new ArrayList<>(this.cacheObjects);
        List<String> listOfStrings = new ArrayList<>();

        this.methodList.put("zeitgeist", System.currentTimeMillis());

        for (CacheObject c: this.cacheObjects) {
            if (list.contains(c)) {
                int max = this.cacheObjects.stream()
                        .map(x -> x.numRequests)
                        .max(Integer::compareTo)
                        .get();
                List<CacheObject> stringsCommon = this.cacheObjects.stream()
                        .filter(x -> x.numRequests == max)
                        .collect(Collectors.toList());

                if (stringsCommon.size() != 0) {
                    list.remove(c);
                    listOfStrings.add(c.id());
                } else {
                    if (listOfStrings.size() >= limit) {
                        return listOfStrings.subList(0, limit);
                    } else {
                        return listOfStrings;
                    }
                }
            }
        }

        if (listOfStrings.size() >= limit) {
            return listOfStrings.subList(0, limit);
        } else {
            return listOfStrings;
        }                                               //finished this method but haven't tested, might be buggy
    }

    /**
     * Similar to `zeitgeist()`, but returns the most frequent requests made
     * in the last 30 seconds.
     *
     * @param limit
     * @return
     */
    public List<String> trending(int limit) {
        List<CacheObject> list = new ArrayList<>(this.cacheObjects);
        List<String> listOfStrings = new ArrayList<>();

        for (CacheObject c: this.cacheObjects) {
            if (list.contains(c)) {
                int max = this.cacheObjects.stream()
                        .map(x -> x.numRequests)
                        .max(Integer::compareTo)
                        .get();
                List<CacheObject> stringsCommon = this.cacheObjects.stream()
                        .filter(x -> System.currentTimeMillis() - x.lastAccess <= 30 * 1000)
                        .filter(x -> x.numRequests == max)
                        .collect(Collectors.toList());

                if (stringsCommon.size() != 0) {
                    list.remove(c);
                    listOfStrings.add(c.id());
                } else {
                    if (listOfStrings.size() >= limit) {
                        return listOfStrings.subList(0, limit);
                    } else {
                        return listOfStrings;
                    }
                }
            }
        }

        if (listOfStrings.size() >= limit) {
            return listOfStrings.subList(0, limit);
        } else {
            return listOfStrings;
        }                                               //finished this method but haven't tested, might be buggy
    }

    /**
     * What is the maximum number of requests seen in any 30-second window?
     *
     * @return
     */
    public int peakLoad30s() {
        int max = Integer.MIN_VALUE;

        for (int i = 0; i < this.methodList.size(); i++) {
            if (getCallsInInterval(this.methodList.get(i), this.methodList.get(i) + 30 * 1000) > max) {
                max = getCallsInInterval(this.methodList.get(i), this.methodList.get(i) + 30 * 1000);
            }
        }

        return max;                                     //finished this method but haven't tested, might be buggy
    }

    private int getCallsInInterval(long start, long end) {
        int callsCount = 0;

        for (int count = 0; count < this.methodList.size(); count++) {
            if (this.methodList.get(count) >= start && this.methodList.get(count) <= end) {
                callsCount++;
            }
        }

        return callsCount;
    }


    private static class CacheObject<String> implements Cacheable {
        private String s;
        private long lastAccess;
        private int numRequests;

        private CacheObject(String s) {
            this.s = s;
            this.lastAccess = System.currentTimeMillis();
            this.numRequests = 0;
        }

        public int getNumRequests(String s) {
            return this.numRequests;
        }

        public void setNumRequests(String s) {
            this.numRequests++;
        }

        @Override
        public int hashCode() {
            return s.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CacheObject) {
                CacheObject c = (CacheObject<?>) o;
                if (this.s.equals(c.s)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public java.lang.String id() {
            return (java.lang.String) s;
        }
    }

    //TODO - task 3 stuff

    /**
     * Given two Wikipedia page titles, find a path from the start page to
     * the stop page by following the links between pages.
     *
     * @param startPage title of page to start searching from
     * @param stopPage title of page to search for
     * @return a list containing a path of pages from startPage to
     *         stopPage, including startPage and stopPage
     */
    public List<String> getPath(String startPage, String stopPage) {
        // hunt from MP 2
        return new LinkedList<>();
    }

    /**
     * Execute a provided structured query.
     *
     * @param query the query to execute
     * @return a list of all page titles that match the query
     * @throws InvalidQueryException if query cannot be parsed
     */
    public List<String> excuteQuery(String query) throws InvalidQueryException { //TODO - check campuswire to see if typo
        /*
         * idea for how to do this
         * 1. parse query and pass the CONDITION to executeCondition()
         * 2. executeCondition() will recursively call itself on any subconditions within
         * the condition passed, until all conditions are handled. Handling a condition
         * entails  1. Running the commands for the condition (ex: getPage() for "title is 'Barack Obama'")
         *          2. Depending on whether condition is 'and' or 'or' or a simple condition,
         *              get the result (ex: For the condition "title is 'Barack Obama' AND category is
         *              'Illinois State Senators'", result would be the intersection of the lists of
         *              both sides of the condition)
         *          3. Return the final result
         * 3. look through the list returned from executeCondition() and apply ITEM to it (applying ITEM
         * is a filter & map operation(s))
         * 4. if the output is supposed to be sorted, sort it
         * 5. return the result
         *
         */
        return new LinkedList<>();
    }

    /**
     * Get the result of a given condition of a query.
     *
     * @param condition the condition to execute
     * @return the result of the condition
     */
    private List<String> executeCondition(String condition) { // maybe something is better than a string? idk
        return new LinkedList<>();
    }

}
