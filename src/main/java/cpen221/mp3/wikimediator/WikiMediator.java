package cpen221.mp3.wikimediator;

import cpen221.mp3.cache.Cache;
import cpen221.mp3.cache.Cacheable;
import fastily.jwiki.core.Wiki;

import java.util.*;
import java.util.stream.Collectors;

public class WikiMediator {

    /* TODO: Implement this datatype

        You must implement the methods with the exact signatures
        as provided in the statement for this mini-project.

        You must add method signatures even for the methods that you
        do not plan to implement. You should provide skeleton implementation
        for those methods, and the skeleton implementation could return
        values like null.
     */

    private Cache<Cacheable> c = new Cache<>();
    private HashSet<CacheObject> data = new HashSet<>();
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
        CacheObject<String> s = new CacheObject<>(query);

        this.c.put(s);
        this.data.add(s);

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
        CacheObject<String> page = new CacheObject<>(pageTitle);
        this.c.put(page);
        this.data.add(page);

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
        List<CacheObject> list = new ArrayList<>(this.data);
        List<String> listOfStrings = new ArrayList<>();

        for (CacheObject c: this.data) {
            if (list.contains(c)) {
                int max = this.data.stream()
                        .map(x -> x.numRequests)
                        .max(Integer::compareTo)
                        .get();
                List<CacheObject> stringsCommon = this.data.stream()
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
        List<CacheObject> list = new ArrayList<>(this.data);
        List<String> listOfStrings = new ArrayList<>();

        for (CacheObject c: this.data) {
            if (list.contains(c)) {
                int max = this.data.stream()
                        .map(x -> x.numRequests)
                        .max(Integer::compareTo)
                        .get();
                List<CacheObject> stringsCommon = this.data.stream()
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

        for (CacheObject c: this.data) {
            if (getCacheInInterval(c.lastAccess, c.lastAccess + 30 * 1000).size() > max) {
                max = getCacheInInterval(c.lastAccess, c.lastAccess + 30 * 1000).size();
            }
        }

        return max;                                     //finished this method but haven't tested, might be buggy
    }

    private List<CacheObject> getCacheInInterval(long start, long end) {
        List<CacheObject> cacheInInterval = new ArrayList<>();

        for (CacheObject c: this.data) {
            if (c.lastAccess >= start && c.lastAccess <= end) {
                cacheInInterval.add(c);
            }
        }

        return cacheInInterval;
    }


    private static class CacheObject<String> implements Cacheable {
        private String s;
        private long lastAccess;
        private int numRequests;

        private CacheObject(String s) {
            this.s = s;
            this.lastAccess = System.currentTimeMillis();
            numRequests = 0;
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


}
