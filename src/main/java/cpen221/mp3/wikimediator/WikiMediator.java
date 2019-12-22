package cpen221.mp3.wikimediator;

import cpen221.mp3.cache.Cache;
import cpen221.mp3.cache.Cacheable;
import cpen221.mp3.cache.NoSuchCacheElementException;
import fastily.jwiki.core.NS;
import fastily.jwiki.core.Wiki;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

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
                    if (cache.get(c.id()).id().equals(title)
                            && !listOfSearch.contains(title)) {
                        cache.touch(c.id());
                        c.update();
                        listOfSearch.add(title);
                        check = true;
                    }
                } catch (NoSuchCacheElementException nsee) {
                    // do nothing
                }
            }
            if (!check && !listOfSearch.contains(title)) {
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
                c.update();
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
            ArrayList<String> newTitle = new ArrayList<>();
            if (!listOfAllTitles.contains(pageTitle)) {
                newTitle.add(pageTitle);
                return newTitle;
            }
            return new ArrayList<>();
        } else {
            int currentHops = hops - 1;
            ArrayList<String> linksOnPage = wiki.getLinksOnPage(true, pageTitle);
            listOfAllTitles.addAll(linksOnPage);
            for (String link: linksOnPage) {
                listOfAllTitles.addAll(recursiveGetConnected(listOfAllTitles, link, currentHops));
            }
            return (ArrayList<String>) listOfAllTitles.stream().distinct().collect(Collectors.toList());
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

        while (listOfStrings.size() < limit) {
            int max;
            CacheObject maxCacheObject = list.get(0);
            max = list.stream().map(x -> x.numRequests).max(Integer::compareTo).get();
            for (CacheObject c : list) {
                if (c.numRequests == max) {
                    maxCacheObject = c;
                    break;
                }
            }

            list.removeAll(Collections.singleton(maxCacheObject));
            listOfStrings.add(maxCacheObject.id());
        }

        return listOfStrings.subList(0, limit);
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
        List<String> listOfStrings = new ArrayList<>();
        List<CacheObject> list = this.cacheObjects.stream()
                .filter(x -> Math.abs(currentTime() - x.lastAccess) <= 30 * Math.pow(10, 9))
                .distinct()
                .collect(Collectors.toList());
        this.methodList.put("trending", currentTime());

        while (listOfStrings.size() < limit && !list.isEmpty()) {
            CacheObject maxCacheObject = list.get(0);
            int max = list.stream().map(x -> x.numRequests).max(Integer::compareTo).get();

            for (CacheObject c : list) {
                if (c.numRequests == max) {
                    maxCacheObject = c;
                    break;
                }
            }

            list.remove(maxCacheObject);
            listOfStrings.add(maxCacheObject.id());
        }

        if (listOfStrings.size() >= 5) {
            return listOfStrings.subList(0, limit);
        } else {
            return listOfStrings;
        }
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
            this.numRequests = 1;
        }


        /**
         * Update the number of requests of the cacheObject
         */
        private void update() {
            this.numRequests++;
            this.lastAccess = currentTime();
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
                return this.s.equals(c.s);
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
     *         string
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
                && this.wiki.whatLinksHere(stopPage, true).size() == 0)
                || !this.wiki.exists(startPage)
                || !this.wiki.exists(stopPage)) {

            return new LinkedList<>();
        }

        if (this.wiki.getCategoriesOnPage(stopPage).contains("Category:All orphaned articles")) {
            return new LinkedList<>();
        }

        if (startPage.equals(stopPage)) {
            return Arrays.asList(startPage);
        }

        // Perform a breadth-first search from start and stop pages
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
            String sourcePage;
            HashSet<String> visitedPages;
            Map<String, String> previousPage;
            LinkedList<String> pages;
            List<String> links;

            // Setup the current iteration of the BFS based on which page has
            // the least amount of links
            if (forwardLinksCount > backwardLinksCount) {
                currentPageForward = pagesForward.poll();
                currentPage = currentPageForward;
                targetPage = stopPage;
                sourcePage = startPage;
                visitedPages = visitedPagesForward;
                previousPage = previousPageForward;
                pages = pagesForward;
                links = this.wiki.getLinksOnPage(currentPage);
            } else {
                currentPageBackward = pagesBackward.poll();
                currentPage = currentPageBackward;
                targetPage = startPage;
                sourcePage = stopPage;
                visitedPages = visitedPagesBackward;
                previousPage = previousPageBackward;
                pages = pagesBackward;

                // Get all pages that link (directly or through redirects) to currentPage
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

                        if (sourcePage.equals(startPage)) {
                            String page = link;
                            while (!page.equals(sourcePage)) {
                                path.addFirst(page);
                                page = previousPage.get(page);
                            }

                            path.addFirst(sourcePage);
                        } else {
                            LinkedList<String> backwardsPath = new LinkedList<>();
                            String page = previousPage.get(startPage);

                            while (!page.equals(stopPage)) {
                                backwardsPath.addLast(page);
                                page = previousPage.get(page);
                            }
                            backwardsPath.addLast(stopPage);

                            path.addFirst(startPage);
                            updateForwardPathFromBackwards(path, backwardsPath);
                        }

                        return path;
                    }

                    // If the BFS from both pages intersect, find the path
                    for (String page : pagesForward) {
                        if (pagesBackward.contains(page)) {

                            // Get path for each half - stick together
                            LinkedList<String> pathForward = new LinkedList<>();
                            LinkedList<String> pathBackward = new LinkedList<>();
                            List<String> path = new LinkedList<>();

                            String pageForward = page;
                            while (!pageForward.equals(startPage)) {
                                pathForward.addFirst(pageForward);
                                pageForward = previousPageForward.get(pageForward);
                            }
                            pathForward.addFirst(startPage);

                            // to eliminate any issues with redirects, do a modified BFS
                            // from page to stopPage, following the path given by pathBackward

                            String pageBackward = previousPageBackward.get(page);
                            while (!pageBackward.equals(stopPage)) {
                                pathBackward.addLast(pageBackward);
                                pageBackward = previousPageBackward.get(pageBackward);
                            }
                            pathBackward.addLast(stopPage);

                            updateForwardPathFromBackwards(pathForward, pathBackward);

                            path.addAll(pathForward);
                            return path;
                        }
                    }
                }
            }
        }

        return new LinkedList<>();
    }

    /**
     * Update pathForward with a path through Wikipedia pages to the last page in pathBackward.
     *
     * @param pathForward the path list from the startPage to an intermediate page
     * @param pathBackward the path list from the intermediate page to the startPage
     */
    private void updateForwardPathFromBackwards(LinkedList<String> pathForward, List<String> pathBackward) {
        for (String currentBackwardsPage : pathBackward) {
            List<String> linksOnPage = this.wiki.getLinksOnPage(pathForward.peekLast());
            if (linksOnPage.contains(currentBackwardsPage)) {
                pathForward.addLast(currentBackwardsPage);
            } else {
                for (String backLink : linksOnPage) {
                    List<String> linkLinks = this.wiki.getLinksOnPage(backLink);
                    if (linkLinks.contains(currentBackwardsPage)) {
                        pathForward.addLast(backLink);
                        pathForward.addLast(currentBackwardsPage);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Execute a provided structured query.
     *
     * @param query the query to execute
     * @return a list of all page titles that match the query
     * @throws InvalidQueryException if query is formatted incorrectly or cannot be parsed
     */
    public List<String> executeQuery(String query) throws InvalidQueryException {
        if (query == null) {
            throw new InvalidQueryException();
        }

        // Create a stream of tokens using the lexer
        CharStream stream = new ANTLRInputStream(query);
        WikiMediatorLexer lexer = new WikiMediatorLexer(stream);
        lexer.reportErrorsAsExceptions();
        TokenStream tokens = new CommonTokenStream(lexer);

        // Feed the tokens into the parser
        WikiMediatorParser parser = new WikiMediatorParser(tokens);
        parser.reportErrorsAsExceptions();

        // Generate the parse tree using the starter rule
        ParseTree tree;
        try {
            tree = parser.query();
        } catch (Exception e) {
            throw new InvalidQueryException();
        }

        // Walk over the parse tree with a listener
        ParseTreeWalker walker = new ParseTreeWalker();
        WikiMediatorListener_WikiMediatorCreator listener = new WikiMediatorListener_WikiMediatorCreator();
        walker.walk(listener, tree);

        return listener.getResult();
    }

    /**
     * A parseTree listener that evaluates a query as it walks through a parse tree.
     */
    private class WikiMediatorListener_WikiMediatorCreator extends WikiMediatorBaseListener {
        private Stack<List<String>> conditionResults = new Stack<>();

        @Override
        public void exitQuery(WikiMediatorParser.QueryContext ctx) {
            List<String> result = conditionResults.pop();

            String item = ctx.ITEM().getText();
            switch (item) {
                case("page"):
                    // result is already page titles
                    break;
                case("author"):
                    result = result.stream().map(x -> wiki.getLastEditor(x)).collect(Collectors.toList());
                    break;
                case("category"):
                    Set<String> categories = new HashSet<>();
                    for (String page : result) {
                        categories.addAll(wiki.getCategoriesOnPage(page));
                    }

                    result = new LinkedList<>(categories);
                    break;
                default:
                    result.clear();
                    break;
            }

            if (ctx.SORTED() != null) {
                switch (ctx.SORTED().getText()) {
                    case("asc"):
                        result = result.stream().sorted().collect(Collectors.toList());
                        break;
                    case("desc"):
                        result = result.stream().sorted(Collections.reverseOrder()).collect(Collectors.toList());
                        break;
                }
            }

            conditionResults.push(result);
        }

        @Override
        public void exitCondition(WikiMediatorParser.ConditionContext ctx) {
            if (ctx.simple_condition() == null) {
                List<String> result1 = conditionResults.pop();
                List<String> result2 = conditionResults.pop();

                if (ctx.AND() != null) {
                    result1 = result1.stream()
                            .filter(result2::contains)
                            .collect(Collectors.toList());
                } else {
                    result1.addAll(result2);
                    result1 = result1.stream()
                            .distinct()
                            .collect(Collectors.toList());
                }

                conditionResults.push(result1);
            }
        }

        @Override
        public void exitSimple_condition(WikiMediatorParser.Simple_conditionContext ctx) {
            List<String> simpleCondition;
            String item = ctx.STRING().toString().substring(1, ctx.STRING().toString().length() - 1);

            if (ctx.TITLE() != null) {
                simpleCondition = wiki.allPages(item, false, false, -1, null);
            } else if (ctx.AUTHOR() != null) {
                simpleCondition = wiki.getContribs(item, -1, false)
                        .stream()
                        .map(x -> x.title)
                        .distinct()
                        .filter(x -> item.equals(wiki.getLastEditor(x)))
                        .collect(Collectors.toList());
            } else {
                simpleCondition = wiki.getCategoryMembers("Category:" + item);
            }

            conditionResults.push(simpleCondition);
        }

        /**
         * Get the result of the most recently parsed query.
         *
         * @return the result of the query
         */
        private List<String> getResult() {
            return conditionResults.pop();
        }
    }

}
