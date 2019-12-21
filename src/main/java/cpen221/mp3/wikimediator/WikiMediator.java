package cpen221.mp3.wikimediator;

import cpen221.mp3.cache.Cache;
import cpen221.mp3.cache.Cacheable;
import cpen221.mp3.cache.NoSuchCacheElementException;
import fastily.jwiki.core.Wiki;
import java.util.*;
import java.util.stream.Collectors;

public class WikiMediator {

    /**
     * WikiMediator Rep Invariant
     *
     * wiki is not null
     * methodList is not null and does not contain null elements
     * cacheObjects is not null and does not contain null elements
     * cache is not null and does not contain null elements
     *
     *
     * CacheObject Rep Invariant
     *
     * s is not null
     * lastAccess >= 0
     * numRequests >= 0
     *
     * ---------------------------------------------------------------------------
     * WikiMediator Abstract Functions
     *
     * wiki -> an instance of Wiki class for WikiMediator to use
     * methodList -> a linked HashMap that takes the function name string as key
     *               and the time that this method is being called
     * cacheObjects -> a list that stores cacheObjects, which is titles, that
     *                 allows methods to call them when needed
     * cache -> a cache that stores Page, the time that such Page is last
     *          accessed, and the number of times such Page has been accessed
     *
     *
     * CacheObject Abstraction Functions
     *
     * s -> a string stored in the cache
     * lastUpdated -> the time the string was most recently updated
     * lastAccessed -> the time the string was most recently accessed
     */

    private final static int CACHE_CAPACITY = 256;
    private final static int CACHE_TIMEOUT = 12 * 60 * 60;  // 12 hours
    private final long startTime = System.nanoTime();

    private Wiki wiki = new Wiki("en.wikipedia.org");
    private LinkedHashMap<String, Long> methodList = new LinkedHashMap<>();
    private List<CacheObject> cacheObjects = new ArrayList<>();
    private Cache<Page> cache;


    /**
     * Create a WikiMediator that has a new cache with cache capacity 256,
     * and timeout value of 12 hours
     */
    public WikiMediator() {
        cache = new Cache<>(CACHE_CAPACITY, CACHE_TIMEOUT);
    }


    /**
     * Get the time since the cache was created.
     *
     * @return the number of nanoseconds since the cache was created.
     */
    private synchronized long currentTime() {
        return System.nanoTime() - startTime;
    }


    /**
     * Given a `query`, return up to `limit` page titles that match the
     * query string (per Wikipedia's search service).
     * Creates a cacheObject that represents 'query' and store it in cacheObjects
     * Store the method name "simpleSearch" in the methodList with the current time stamp
     * For every title that matches the specified query, create a page, which contains its
     * title and the entire text, and store it in the cache
     *
     * @param query the string that is being searched
     * @param limit the maximum number of titles that is required to search
     * @return a list that stores up to 'limit' amount of page title that matches 'query'
     * @throws IllegalArgumentException if page title is an empty string
     */
    public List<String> simpleSearch(String query, int limit) {
        ArrayList<String> listOfSearch = new ArrayList<>();

        this.cacheObjects.add(new CacheObject<>(query));
        this.methodList.put("simpleSearch", currentTime());

        // TODO - check cache implementation
        for (String title: wiki.search(query, limit)) {
            boolean check = false;
            for (CacheObject c : this.cacheObjects) {
                try {
                    if (cache.get(c.id()).id().equals(title)) {
                        cache.touch(c.id());
                        listOfSearch.add(title);
                        check = true;
                    }
                } catch (NoSuchCacheElementException nsee) {
                    nsee.printStackTrace();
                }
            }
            if (!check) {
                listOfSearch.add(title);
                this.cache.put(new Page(wiki.getPageText(title), title));
            }
        }

        for (String search: listOfSearch) {
            boolean exists = false;
            CacheObject c1 = new CacheObject<>(search);
            for (CacheObject c: this.cacheObjects) {
                if (c.equals(c1)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                this.cacheObjects.add(c1);
            }
        }

        return listOfSearch;
    }


    /**
     * Given a `pageTitle`, return the text associated with the Wikipedia
     * page that matches `pageTitle`. If no titles matches, return an empty string.
     * Creates a cacheObject that represents 'pageTitle' and store it in cacheObjects
     * Store the method name "getPage" in the methodList with the current time stamp
     * Create a new Page, which contains the title and the entire content, and store it in cache
     *
     * @param pageTitle the page title that we are getting the page text from
     * @return the entire page text
     * @throws IllegalArgumentException if page title is an empty string
     */
    public String getPage(String pageTitle) {
        if (pageTitle.equals("")) {
            throw new IllegalArgumentException("Page title cannot be an empty string.");
        }

        boolean exist = false;
        for (CacheObject c: this.cacheObjects) {
            if (c.id().equals(pageTitle)) {
                exist = true;
                try {
                    this.methodList.put("getPage", currentTime());
                    return this.cache.get(pageTitle).getPageText();
                } catch (NoSuchCacheElementException e) {
                    e.printStackTrace();
                }
            }
        }

        if (!exist) {
            Page page = new Page(wiki.getPageText(pageTitle), pageTitle);
            this.methodList.put("getPage", currentTime());
            this.cacheObjects.add(new CacheObject<>(pageTitle));
            this.cache.put(page);
            return page.getPageText();
        }

        return null;
    }


    /**
     * Return a list of page titles that can be reached by following up to
     * `hops` links starting with the page specified by `pageTitle`.
     * Stores the method name "getConnectedPage" to methodList with current time stamp
     * Calls recursive function to get all connected pages within specified hops
     *
     * @param pageTitle the page title that this method starts from
     * @param hops the number of hops (range) that needs to be searched
     * @return the list of page titles that are within 'hops' range
     */
    public List<String> getConnectedPages(String pageTitle, int hops) {

        this.methodList.put("getConnectedPages", currentTime());

        return recursiveGetConnected(new ArrayList<>(), pageTitle, hops);
    }


    /**
     * Recursive function that stores every page title that is within specified 'hops' range
     * If the title is already in the list, skip and check the next title.
     *
     * Base case: hops <= 0, no hops left or negative hops
     * Induction step: For every hop, get all available titles on that page. For every available
     *                 titles, call this recursive function with --hops. Eventually, hops will reach
     *                 its base case and terminate the recursive function.
     *
     * @param listOfAllTitles all the page titles that are connected to specified page title within hops range
     * @param pageTitle the specified page title that this method will start from
     * @param hops number of hops left (range) that needs to be searched
     * @return the list of page titles that are within 'hops' range so far
     */
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
                        .addAll(recursiveGetConnected(listOfAllTitles,
                                wiki.getLinksOnPage(true, pageTitle).get(i), --hops)
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
     * Stores the method name "zeitgeist" to methodList with current time stamp
     *
     * @param limit the maximum number of strings that can be returned
     * @return the most common string's used in 'simpleSearch' and 'getPage' requests
     *         with items being sorted in non-increasing order
     */
    public List<String> zeitgeist(int limit) {
        List<CacheObject> list = new ArrayList<>(this.cacheObjects);
        List<String> listOfStrings = new ArrayList<>();

        this.methodList.put("zeitgeist", currentTime());

        // TODO: Check this implementation
        for (CacheObject c: this.cacheObjects) {
            if (list.contains(c)) {
                int max;
                int stringsCommonSize = Integer.MIN_VALUE;
                if (this.cacheObjects.stream().map(x -> x.numRequests).max(Integer::compareTo).isPresent()) {
                    max = this.cacheObjects.stream().map(x -> x.numRequests).max(Integer::compareTo).get();
                    stringsCommonSize = (int) this.cacheObjects.stream()
                            .filter(x -> x.numRequests == max).count();
                }

                if (stringsCommonSize != 0 && listOfStrings.size() < limit) {
                    list.removeAll(Collections.singleton(c));
                    listOfStrings.add(c.id());
                }
            }
        }

        return listOfStrings;
    }

    /**
     * Similar to `zeitgeist()`, but returns the most frequent requests made
     * in the last 30 seconds.
     * Stores the method name "trending" to methodList with current time stamp
     *
     * @param limit the maximum number of strings that can be returned
     * @return the most common string's used in 'simpleSearch' and 'getPage' requests
     *         in the last 30 second with items being sorted in non-increasing order
     */
    public List<String> trending(int limit) {
        List<CacheObject> list = new ArrayList<>(this.cacheObjects);
        List<String> listOfStrings = new ArrayList<>();

        // TODO: Check this implementation
        for (CacheObject c: this.cacheObjects) {
            if (list.contains(c)) {
                int max;
                int stringsCommonSize = Integer.MIN_VALUE;
                if (this.cacheObjects.stream().map(x -> x.numRequests).max(Integer::compareTo).isPresent()) {
                    max = this.cacheObjects.stream().map(x -> x.numRequests).max(Integer::compareTo).get();
                    stringsCommonSize = (int) this.cacheObjects.stream()
                            .filter(x -> currentTime() - x.lastAccess <= 30 * Math.pow(10, 9))
                            .filter(x -> x.numRequests == max).count();
                }


                if (stringsCommonSize != 0 && listOfStrings.size() < limit) {
                    list.remove(c);
                    listOfStrings.add(c.id());
                }
            }
        }

        return listOfStrings;
    }

    /**
     * Returns the maximum number of requests seen in any 30-second window?
     * Stores the method name "peakLoad30s" to methodList with current time stamp
     *
     * @return the maximum number of requests seen in any 30-seconds interval
     */
    public int peakLoad30s() {
        int max = Integer.MIN_VALUE;
        this.methodList.put("peakLoad30s", currentTime());

        for (String methodName: this.methodList.keySet()) {
            int numberOfCalls = getCallsInInterval(this.methodList.get(methodName),
                    this.methodList.get(methodName) + 30 * (long) Math.pow(10, 9));
                if (numberOfCalls > max) {
                max = numberOfCalls;
            }
        }

        return max;
    }


    /**
     * Helper method of peakLoad30s that produces the number of all the requests (method calls)
     * in specified time interval
     *
     * @param start the starting time in nanoseconds
     * @param end the ending time in nanoseconds
     * @return number of all the requests that happened between specified start and end time
     */
    private int getCallsInInterval(long start, long end) {
        int callsCount = 0;

        for (String key: this.methodList.keySet()) {
            if (this.methodList.get(key) >= start && this.methodList.get(key) <= end) {
                callsCount++;
            }
        }

        return callsCount;
    }


    /**
     * An object that holds the values in the cache along with their
     * associated metadata (number of times that was requested and number of requests)
     */
    private class CacheObject<string> implements Cacheable {
        private string s;
        private long lastAccess;
        private int numRequests;


        /**
         * Create a CacheObject with current time as last accessed time
         *
         * @param s the value to store
         */
        private CacheObject(string s) {
            this.s = s;
            this.lastAccess = currentTime();
            this.numRequests = 0;
        }


        /**
         * Make a hashcode for the CacheObject
         *
         * @return the hashcode
         */
        @Override
        public int hashCode() {
            return s.hashCode();
        }


        /**
         * Determine if this and another CacheObject are equal
         *
         * @param o other CacheObject to compare to
         * @return true if this and o are equal, false otherwise
         */
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


        /**
         * Produces the id of the string, which is the string itself
         *
         * @return the id of the string
         */
        @Override
        public java.lang.String id() {
            return (java.lang.String) s;
        }
    }


    /**
     * Given two Wikipedia page titles, find a path from the start page to
     * the stop page by following the links between pages.
     *
     * @param startPage title of page to start searching from
     * @param stopPage title of page to search for
     * @return a list of page titles containing a path of pages from startPage to
     *         stopPage inclusive, or an empty list if no path exists.
     * @throws IllegalArgumentException if either startPage or stopPage is an empty
     *         string or not a Wikipedia article title
     */
    public List<String> getPath(String startPage, String stopPage) {
        LinkedList<String> pagesForward = new LinkedList<>();
        HashSet<String> visitedPagesForward = new HashSet<>();
        Map<String, String> previousPageForward = new HashMap<>();
        LinkedList<String> pagesBackward = new LinkedList<>();
        HashSet<String> visitedPagesBackward = new HashSet<>();
        Map<String, String> previousPageBackward = new HashMap<>();

        if (startPage == null || stopPage == null || startPage.equals("") || stopPage.equals("")) {
            throw new IllegalArgumentException("Invalid page");
        } else if (this.wiki.getLinksOnPage(startPage).size() == 0
                || (this.wiki.whatLinksHere(stopPage).size() == 0
                && this.wiki.whatLinksHere(stopPage, true).size() == 0)) {
            return new LinkedList<>();
        }

        if (this.wiki.getCategoriesOnPage(stopPage).contains("Category:All orphaned articles")) {
            return new LinkedList<>();
        }

        if (startPage.equals(stopPage)) {
            return Arrays.asList(startPage);
        }

        visitedPagesForward.add(startPage);
        pagesForward.offer(startPage);
        visitedPagesBackward.add(stopPage);
        pagesBackward.offer(stopPage);

        String currentPageForward = startPage;
        String currentPageBackward = stopPage;

        while (!pagesForward.isEmpty() && !pagesBackward.isEmpty()) {
            int forwardLinksCount = this.wiki.getLinksOnPage(currentPageForward).size();
            int backwardLinksCount = this.wiki.getLinksOnPage(currentPageBackward).size();
            String currentPage;
            String targetPage;
            HashSet<String> visitedPages;
            Map<String, String> previousPage;
            LinkedList<String> pages;
            List<String> links;

            if (forwardLinksCount > backwardLinksCount) {
                currentPageForward = pagesForward.poll();
                currentPage = currentPageForward;
                targetPage = stopPage;
                visitedPages = visitedPagesForward;
                previousPage = previousPageForward;
                pages = pagesForward;
                links = this.wiki.getLinksOnPage(currentPage);
            } else {
                currentPageBackward = pagesBackward.poll();
                currentPage = currentPageBackward;
                targetPage = startPage;
                visitedPages = visitedPagesBackward;
                previousPage = previousPageBackward;
                pages = pagesBackward;

                links = this.wiki.whatLinksHere(currentPage);
                List<String> redirects = this.wiki.whatLinksHere(currentPage, true);
                for (String redirect : redirects) {
                    links.addAll(this.wiki.whatLinksHere(redirect).stream()
                            .filter(s -> !links.contains(s))
                            .collect(Collectors.toList()));
                }
            }

            for (String link : links) {
                if (!visitedPages.contains(link)) {
                    visitedPages.add(link);
                    previousPage.put(link, currentPage);
                    pages.offer(link);

                    if (link.equals(targetPage)) {
                        LinkedList<String> path = new LinkedList<>();
                        String page = link;
                        while (!page.equals(startPage)) {
                            path.addFirst(page);
                            page = previousPage.get(page);
                        }

                        path.addFirst(startPage);
                        System.out.println("link = targetPage");
                        return path;
                    }

                    for (String page : pagesForward) {
                        if (pagesBackward.contains(page)) {
                            // get path for each half - stick together
                            LinkedList<String> pathForward = new LinkedList<>();
                            LinkedList<String> pathBackward = new LinkedList<>();
                            List<String> path = new LinkedList<>();

                            String pageForward = page;
                            while (!pageForward.equals(startPage)) {
                                pathForward.addFirst(pageForward);
                                pageForward = previousPageForward.get(pageForward);
                            }
                            pathForward.addFirst(startPage);

                            String pageBackward = previousPageBackward.get(page);
                            while (!pageBackward.equals(stopPage)) {
                                pathBackward.addLast(pageBackward);
                                pageBackward = previousPageBackward.get(pageBackward);
                            }
                            pathBackward.addLast(stopPage);

                            path.addAll(pathForward);
                            path.addAll(pathBackward);

                            System.out.printf("forward backward intersection at %s\n", page);
                            return path;
                        }
                    }
                }
            }
        }

        return new LinkedList<>();
    }

    /**
     * Execute a provided structured query.
     *
     * @param query the query to execute
     * @return a list of all page titles that match the query
     * @throws InvalidQueryException if query cannot be parsed
     */
    public List<String> executeQuery(String query) throws InvalidQueryException {
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
