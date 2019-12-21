package cpen221.mp3;

import cpen221.mp3.cache.Cache;
import cpen221.mp3.wikimediator.Page;
import cpen221.mp3.wikimediator.WikiMediator;
import fastily.jwiki.core.Wiki;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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

    // passes, but takes 3 min
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

        List<String> expected = new ArrayList<>();
        expected.add("List_of_individual_dogs");
        expected.add("Philippines");
        expected.add("American cuisine");
        expected.add("Butter");
        assertEquals(expected, result);

        /*
        for (int page = 0; page < result.size() - 1; page++) {
            List<String> links = wiki.getLinksOnPage(result.get(page));
            if (!links.contains(result.get(page + 1))) {
                fail("Path does not exist");
            }
        }
         */
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

}
