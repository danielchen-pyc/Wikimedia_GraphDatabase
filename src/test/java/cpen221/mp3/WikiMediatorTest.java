package cpen221.mp3;

import cpen221.mp3.cache.Cache;
import cpen221.mp3.wikimediator.InvalidQueryException;
import cpen221.mp3.wikimediator.Page;
import cpen221.mp3.wikimediator.WikiMediator;
import fastily.jwiki.core.NS;
import fastily.jwiki.core.Wiki;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class WikiMediatorTest {

    @Test
    public void testWikiMediatorConstructor() {
        WikiMediator wm = new WikiMediator();
        assertFalse(wm.getPage("The Dark Knight (film)").isEmpty());
    }

    @Test
    public void testGetPage_emptyTitle() {
        WikiMediator wm = new WikiMediator();
        boolean exceptionThrown = false;

        try {
            String pageContent = wm.getPage("");
        } catch (IllegalArgumentException iae) {
            exceptionThrown = true;
        } finally {
            if (!exceptionThrown) {
                fail("Invalid page title.");
            }
        }
    }

    @Test
    public void testGetPage_nonexistingTitle() {
        WikiMediator wm = new WikiMediator();
        String pageContent = "";

        try {
            pageContent = wm.getPage("ashifew");
        } catch (IllegalArgumentException iae) {
            fail("Shouldn't throw an exception");
        }

        assertTrue(pageContent.isEmpty());
    }

    @Test
    public void testGetPage_normalPages() {
        WikiMediator wm = new WikiMediator();
        ArrayList<String> contents = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            try {
                contents.add(wm.getPage(Integer.toString(i)));
            } catch (IllegalArgumentException iae) {
                fail("Shouldn't throw an exception.");
            }
        }

        assertFalse(contents.isEmpty());
    }

    @Test
    public void testGetPage_multithreadedRequests() throws InterruptedException {
        WikiMediator wm = new WikiMediator();
        String[] titles = {"The Dark Knight (film)", "Titanic (1997 film)", "The Shawshank Redemption", "The Godfather"};
        ArrayList<String> contents = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            Thread stressTester = new Thread(() -> {
                for (String title : titles) {
                    try {
                        contents.add(wm.getPage(title));
                        System.out.println(Thread.currentThread().getId() + " will obtain this page: " + title);
                    } catch (Exception e) {
                        fail("Shouldn't throw an exception");
                    }
                }
            });

            for (int j = 0; j < 3; j++) {
                stressTester.run();
                stressTester.join();
            }
        }

        assertEquals(36, contents.size());
    }


    @Test
    public void testSimpleSearch() {
        WikiMediator wm = new WikiMediator();
        String answer = "[Barack Obama, Barack Obama in comics, Barack Obama Sr., " +
                "List of things named after Barack Obama]";

        assertEquals(answer, wm.simpleSearch("Barack Obama", 4).toString());
    }

    @Test
    public void testSimpleSearchInvolvingCache() {
        WikiMediator wm = new WikiMediator();
        String answer1 = "[Barack Obama]";
        String answer5 = "[Barack Obama, Barack Obama in comics, Barack Obama Sr., " +
                "List of things named after Barack Obama]";

        assertEquals(answer1, wm.simpleSearch("Barack Obama", 1).toString());
        assertEquals(answer5, wm.simpleSearch("Barack Obama", 4).toString());
    }

    @Test
    public void testGetConnectedPages_0hops() {
        WikiMediator wm = new WikiMediator();
        ArrayList<String> noHop = new ArrayList<>();
        noHop.add("Canada");
        String oneHop = "[29th Infantry Regiment (United States), Bundeswehr, 29th Infantry Division (United States), " +
                "Luftwaffe, Battle of Kesternich, Medal of Honor, Bremerhaven, 18th Infantry Regiment (United States), " +
                "Jonah Edward Kelley, Jacobs University, Cold War, Bremen, 1st Infantry Division (United States), " +
                "Geographic coordinate system, Displaced person, 78th Infantry Division (United States), " +
                "Germany, World War II]";

        assertEquals(noHop, wm.getConnectedPages("Canada", 0));
        assertEquals(oneHop, wm.getConnectedPages("Camp Grohn", 1).toString());
        assertEquals(7868, wm.getConnectedPages("Camp Grohn", 2).size());
    }

    @Test
    public void testZeitgeist() {
        WikiMediator wm = new WikiMediator();
        wm.getPage("Taiwan");
        wm.getPage("Canada");
        wm.simpleSearch("Canada", 5);
        ArrayList<String> commonStrings = new ArrayList<>();
        commonStrings.add("Canada"); commonStrings.add("Taiwan");
        commonStrings.add("Cañada"); commonStrings.add("Monarchy of Canada");
        commonStrings.add("Province of Canada");

        assertEquals(commonStrings, wm.zeitgeist(5));
    }

    @Test
    public void testTrending() throws InterruptedException {
        WikiMediator wm = new WikiMediator();
        wm.simpleSearch("Canada", 5);

        TimeUnit.MINUTES.sleep(1);

        wm.getPage("Taiwan");
        wm.getPage("Canada");
        ArrayList<String> commonStrings = new ArrayList<>();
        commonStrings.add("Canada"); commonStrings.add("Taiwan");

        assertEquals(commonStrings, wm.trending(5));
    }

    @Test
    public void testPeakLoad30() throws InterruptedException {
        WikiMediator wm = new WikiMediator();
        wm.getPage("Taiwan");
        wm.getPage("Canada");
        wm.simpleSearch("Canada", 3);

        assertEquals(3, wm.peakLoad30s());

        TimeUnit.SECONDS.sleep(30);

        wm.simpleSearch("Taiwan", 5);
        assertEquals(2, wm.peakLoad30s());
    }

    @Test
    public void testGetPath_shortPaths() {
        WikiMediator wm = new WikiMediator();

        List<String> result1 = wm.getPath("Carrot", "6-hydroxymellein");
        List<String> expected1 = new ArrayList<>();
        expected1.add("Carrot");
        expected1.add("6-hydroxymellein");
        assertEquals(expected1, result1);

        List<String> result2 = wm.getPath("Carrot", "6-Hydroxymellein");
        List<String> expected2 = new ArrayList<>();
        expected2.add("Carrot");
        expected2.add("6-hydroxymellein");
        expected2.add("6-Hydroxymellein");
        assertEquals(expected2, result2);
    }

    // passes, but takes 2.5 min
    @Test
    public void testGetPath_longPath() {
        WikiMediator wm = new WikiMediator();
        Wiki wiki = new Wiki("en.wikipedia.org");
        String startPage = "List_of_individual_dogs";
        String stopPage = "Butter";
        List<String> result = wm.getPath(startPage, stopPage);

        // assertion of result
        assertEquals(startPage, result.get(0));
        assertEquals(stopPage, result.get(result.size() - 1));

        result.stream().forEach(System.out::println);

        for (int page = 0; page < result.size() - 1; page++) {
            List<String> links = wiki.getLinksOnPage(result.get(page));
            if (!links.contains(result.get(page + 1))) {
                fail("Path does not exist");
            }
        }
    }

    @Test
    public void testGetPath_orphanPage() {
        WikiMediator wm = new WikiMediator();

        List<String> result1 = wm.getPath("Juice", "3-6-3_Rule");
        List<String> expectedEmpty = new ArrayList<>();
        assertEquals(expectedEmpty, result1);
    }

    @Test
    public void testGetPath_samePage() {
        WikiMediator wm = new WikiMediator();

        List<String> result = wm.getPath("TRIUMF", "TRIUMF");
        List<String> expected = new ArrayList<>();
        expected.add("TRIUMF");
        assertEquals(expected, result);
    }

    @Test
    public void testGetPath_invalidTitles() {
        WikiMediator wm = new WikiMediator();
        List<String> expectedEmpty = new ArrayList<>();

        // null page
        boolean exception = false;
        try {
            List<String> result = wm.getPath(null, "Star_Wars");
        } catch (IllegalArgumentException e) {
            exception = true;
        } finally {
            if (!exception) {
                fail("Expected a Invalid Page exception");
            }
        }

        // non-existent page
        List<String> result2 = wm.getPath("LiOp--g", "Danny_DeVito");
        assertEquals(expectedEmpty, result2);

        // invalid page
        List<String> result3 = wm.getPath("<>{notvalid}>", "Skin");
        assertEquals(expectedEmpty, result3);
    }

    @Test
    public void testGetPath_redirects() {
        WikiMediator wm = new WikiMediator();
        String startPage = "Hualien City";
        String redirectPage = "Spaniard";
        String stopPage = "Spaniards";

        List<String> result = wm.getPath(startPage, stopPage);
        List<String> expected = new ArrayList<>();
        expected.add(startPage);
        expected.add(redirectPage);
        expected.add(stopPage);

        assertEquals(expected, result);
    }

    // this test passed as of 2019.12.20 15:33
    @Test
    public void testExecuteQuery_normalInputs() {
        WikiMediator wm = new WikiMediator();
        String query1 = "get category where title is 'Nimble Giant Entertainment'";
        String query2 = "get page where (author is 'Soumya-8974' and category is 'English languages')";
        String query3 = "get author where (title is 'Otis F. Glenn' or (title is 'William H. Dieterich' and category is 'United States senators from Illinois')) asc";
        String query4 = "get author where (title is 'Otis F. Glenn' or (title is 'William H. Dieterich' and category is 'United States senators from Illinois')) desc";
        String query5 = "get category where (author is 'Sylas' and author is 'Henry')";

        List<String> result1 = new LinkedList<>();
        List<String> expected1 = new LinkedList<>(Arrays.asList("Companies based in Buenos Aires", "Video game development companies", "Video game companies of Argentina"));
        try {
            result1 = wm.executeQuery(query1);
        } catch (InvalidQueryException e) {
            fail("InvalidQueryException");
        } finally {
            for (String c : expected1) {
                assertTrue(result1.contains("Category:" + c));
            }
        }

        List<String> result2 = new LinkedList<>();
        List<String> expected2 = new LinkedList<>(Arrays.asList("Old English"));
        try {
            result2 = wm.executeQuery(query2);
        } catch (InvalidQueryException e) {
            fail("InvalidQueryException");
        } finally {
            assertEquals(expected2, result2);
        }

        List<String> result3 = new LinkedList<>();
        List<String> expected3 = new LinkedList<>(Arrays.asList("JJMC89 bot III", "Rich Farmbrough"));
        expected3 = expected3.stream().sorted().collect(Collectors.toList());
        try {
            result3 = wm.executeQuery(query3);
        } catch (InvalidQueryException e) {
            fail("InvalidQueryException");
        } finally {
            assertEquals(expected3, result3);
        }

        List<String> result4 = new LinkedList<>();
        Collections.reverse(expected3);
        try {
            result4 = wm.executeQuery(query4);
        } catch (InvalidQueryException e) {
            fail("InvalidQueryException");
        } finally {
            assertEquals(expected3, result4);
        }

        List<String> result5 = new LinkedList<>();
        List<String> expected5 = new LinkedList<>();
        try {
            result5 = wm.executeQuery(query5);
        } catch (InvalidQueryException e) {
            fail("InvalidQueryException");
        } finally {
            assertEquals(expected5, result5);
        }

    }

    @Test
    public void testExecuteQuery_validQueryButInvalidInputs() {
        WikiMediator wm = new WikiMediator();
        String query1 = "\n \t\t get \t\t\t\t \r\ncategory where \rtitle is 'sdfsdfg_P'\t";
        String query2 = "get author where author is 'sdfsdfg_P'";
        String query3 = "get category where category is 'sdfsdfg_P'";
        String query4 = "getpagewhere author is 'sdfsdfg_P'";
        String query5 = "get author where category is 'sdfsdfg_P'";
        String query6 = "get page where title is 'sdfsdfg_P'";

        List<String> queries = new LinkedList<>();
        queries.add(query1);
        queries.add(query2);
        queries.add(query3);
        queries.add(query4);
        queries.add(query5);
        queries.add(query6);

        List<String> expected = new LinkedList<>();
        for (String query : queries) {
            List<String> result = new LinkedList<>();

            try {
                result = wm.executeQuery(query);
            } catch (InvalidQueryException e) {
                fail("InvalidQueryException");
            } finally {
                assertEquals(expected, result);
            }
        }
    }

    @Test
    public void testExecuteQuery_InvalidQuery() {
        WikiMediator wm = new WikiMediator();
        String query1 = "get Category where title is 'sdfsdfg_P'";
        String query2 = "get author where (author is 'sdfsdfg_P')";
        String query3 = "get category where author is 'Sylas";
        String query4 = "get category where page is 'sdfsdfg_P'";
        String query5 = "author where category is 'sdfsdfg_P'";
        String query6 = "";
        String query7 = null;

        List<String> queries = new LinkedList<>();
        queries.add(query1);
        queries.add(query2);
        queries.add(query3);
        queries.add(query4);
        queries.add(query5);
        queries.add(query6);
        queries.add(query7);

        for (String query : queries) {
            boolean exception = false;

            try {
                List<String> result = wm.executeQuery(query);
            } catch (InvalidQueryException e) {
                exception = true;
            } finally {
                if (!exception) {
                    fail("Expected an InvalidQueryException");
                }
            }
        }
    }

}
