package cpen221.mp3;

import cpen221.mp3.cache.Cache;
import cpen221.mp3.wikimediator.Page;
import cpen221.mp3.wikimediator.WikiMediator;
import fastily.jwiki.core.Wiki;
import org.junit.Test;

import java.util.ArrayList;

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
                stressTester.start();
                stressTester.join();
            }
        }

        assertEquals(36, contents.size());
    }


    @Test
    public void testSimpleSearch() {
        WikiMediator wm = new WikiMediator();
        System.out.println(System.nanoTime());
    }


}
