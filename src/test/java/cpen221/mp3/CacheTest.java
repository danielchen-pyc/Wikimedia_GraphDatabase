package cpen221.mp3;

import cpen221.mp3.cache.Cache;
import cpen221.mp3.cache.NoSuchCacheElementException;
import cpen221.mp3.wikimediator.Page;
import org.junit.Test;

import static org.junit.Assert.*;

public class CacheTest {

    // sequential tests

    @Test
    public void testCreateCache() {
        Cache<Page> c1 = new Cache<>();
        Page p1 = new Page("test1", "1");
        c1.put(p1);

        Page result1 = null;

        try {
            result1 = c1.get("1");
        } catch (NoSuchCacheElementException e) {
            fail("Should not have an exception");
        }

        assertEquals(p1, result1);

        Cache<Page> c0 = new Cache<>(0, 1);
        c1.put(p1);

        boolean exceptionThrown1 = false;
        try {
            Cache<Page> c2 = new Cache<>(-5, 9);
        } catch (IllegalArgumentException e) {
            exceptionThrown1 = true;
        } finally {
            if (!exceptionThrown1) {
                fail("Expected an exception");
            }
        }

        boolean exceptionThrown2 = false;
        try {
            Cache<Page> c2 = new Cache<>(5, -9);
        } catch (IllegalArgumentException e) {
            exceptionThrown2 = true;
        } finally {
            if (!exceptionThrown2) {
                fail("Expected an exception");
            }
        }

        boolean exceptionThrown3 = false;
        try {
            Cache<Page> c2 = new Cache<>(-5, -9);
        } catch (IllegalArgumentException e) {
            exceptionThrown3 = true;
        } finally {
            if (!exceptionThrown3) {
                fail("Expected an exception");
            }
        }
    }

    @Test
    public void testPut() {
        Cache<Page> c = new Cache<>(2, 1);
        Page p1 = new Page("test1", "1");
        Page p2 = new Page("test2", "2");
        Page p1again = new Page("test1again", "1");

        assertTrue(c.put(p1));
        assertTrue(c.put(p1));
        assertTrue(c.put(p1again));
        assertTrue(c.put(p2));

        Page result1 = null;
        Page result2 = null;

        try {
            result1 = c.get(p1.id());
        } catch (NoSuchCacheElementException e) {
            fail("Should not have an exception");
        }

        try {
            result2 = c.get(p2.id());
        } catch (NoSuchCacheElementException e) {
            fail("Should not have an exception");
        }

        assertEquals(p1again, result1);
        assertEquals(p2, result2);

        try {
            Page p = c.get(p1.id());
        } catch (NoSuchCacheElementException e) {
            fail("Should not have an exception");
        }

        Cache<Page> c2 = new Cache<>(0, 1);
        assertFalse(c2.put(p1));

        Page result3 = null;

        try {
            result3 = c2.get(p1.id());
        } catch (NoSuchCacheElementException e) {
            // do nothing - this is expected to happen
        } finally {
            if (result3 != null) {
                fail("Capacity = 0 but had an element anyways");
            }
        }
    }

    @Test
    public void testGet() {
        Cache<Page> c = new Cache<>();
        Page p1 = new Page("test1", "1");
        Page p2 = new Page("test2", "2");

        Page result1 = null;
        boolean exceptionThrown1 = false;
        try {
            result1 = c.get("1");
        } catch (NoSuchCacheElementException e) {
            exceptionThrown1 = true;
        } finally {
            if (!exceptionThrown1) {
                fail("Should have an exception");
            }
        }

        c.put(p1);

        Page result2 = null;
        try {
            result2 = c.get("1");
        } catch (NoSuchCacheElementException e) {
            fail("Should not have an exception");
        }

        assertEquals(p1, result2);

        Page result3 = null;
        boolean exceptionThrown2 = false;
        try {
            result3 = c.get("2");
        } catch (NoSuchCacheElementException e) {
            exceptionThrown2 = true;
        } finally {
            if (!exceptionThrown2) {
                fail("Should have an exception");
            }
        }
    }

    @Test
    public void testTouch() {
        Cache<Page> c = new Cache<>(1, 1);
        Page p1 = new Page("test1", "1");
        c.put(p1);

        Page result1 = null;

        try {
            result1 = c.get("1");
        } catch (NoSuchCacheElementException e) {
            fail("Should not have an exception");
        }

        assertEquals(p1, result1);

        assertTrue(c.touch("1"));
        assertFalse(c.touch("2"));

        try {
            Thread.sleep(1010);
        } catch (InterruptedException e) {
            // do nothing
        }

        boolean exceptionThrown = false;
        try {
            Page p = c.get("1");
        } catch (NoSuchCacheElementException e) {
            exceptionThrown = true;
        } finally {
            if (!exceptionThrown) {
                fail("Should have an exception");
            }
        }
    }

    @Test
    public void testUpdate() {
        Cache<Page> c = new Cache<>(1, 1);
        Page p1 = new Page("test1", "1");
        Page p2 = new Page("test1 but not really", "1");
        Page p3 = new Page("test3", "3");
        c.put(p1);

        assertTrue(c.update(p2));

        Page result1 = null;

        try {
            result1 = c.get("1");
        } catch (NoSuchCacheElementException e) {
            fail("Should not have an exception");
        }

        assertNotEquals(p1, result1);
        assertEquals(p2, result1);
        assertFalse(c.update(p3));
    }

    @Test
    public void testRemoveLeastRequested() {
        Cache<Page> c = new Cache<>(2, 1);
        Page p1 = new Page("test1", "1");
        Page p2 = new Page("test2", "2");
        Page p3 = new Page("test3", "3");

        c.put(p1);
        c.put(p2);

        // get p1 more than p2

        try {
            Page p = c.get(p1.id());
        } catch (NoSuchCacheElementException e) {
            fail("Should not have an exception");
        }

        try {
            Page p = c.get(p1.id());
        } catch (NoSuchCacheElementException e) {
            fail("Should not have an exception");
        }

        try {
            Page p = c.get(p2.id());
        } catch (NoSuchCacheElementException e) {
            fail("Should not have an exception");
        }

        // should get p2 removed from cache
        c.put(p3);

        Page result1 = null;
        Page result2 = null;

        try {
            result1 = c.get(p1.id());
        } catch (NoSuchCacheElementException e) {
            fail("Should not have an exception");
        }

        boolean exceptionThrown1 = false;
        try {
            Page p = c.get(p2.id());
        } catch (NoSuchCacheElementException e) {
            exceptionThrown1 = true;
        } finally {
            if (!exceptionThrown1) {
                fail("Expected an exception");
            }
        }

        try {
            result2 = c.get(p3.id());
        } catch (NoSuchCacheElementException e) {
            fail("Should not have an exception");
        }

        assertEquals(p1, result1);
        assertEquals(p3, result2);
    }

    @Test
    public void testTimeout() {
        Cache<Page> c = new Cache<>(2, 1);
        Page p1 = new Page("test1", "1");
        Page p2 = new Page("test2", "2");
        Page p3 = new Page("test3", "3");

        c.put(p1);
        c.put(p2);

        try {
            Thread.sleep(1010);
        } catch (InterruptedException e) {
            // do nothing
        }

        boolean exceptionThrown1 = false;
        try {
            Page p = c.get(p1.id());
        } catch (NoSuchCacheElementException e) {
            exceptionThrown1 = true;
        } finally {
            if (!exceptionThrown1) {
                fail("Expected an exception");
            }
        }

        boolean exceptionThrown2 = false;
        try {
            Page p = c.get(p3.id());
        } catch (NoSuchCacheElementException e) {
            exceptionThrown2 = true;
        } finally {
            if (!exceptionThrown2) {
                fail("Expected an exception");
            }
        }
    }

    // concurrent tests

    @Test
    public void testManyThreadsPutGet() {
        Cache<Page> c = new Cache<>(10, 1);

        for (int i = 0; i < 10; i++) {
            int id = i;
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    Page p = new Page("test", Integer.toString(id));
                    for (int j = 0; j < 100; j++) {
                        c.put(p);

                        System.out.println("Hello");

                        Page result = null;
                        try {
                            //System.out.println(Thread.currentThread().getName() + " got: "
                        // + ", j = " + Integer.toString(j));
                            result = c.get(p.id());
                        System.out.println("Hello2");
                            System.out.println(Thread.currentThread().getName() + " got: "
                                    + result.toString() + ", j = " + Integer.toString(j));
                        } catch (NoSuchCacheElementException e) {
                            System.out.println("ERROR!! " + e.getMessage());
                            //fail("Should not be an exception");
                        } finally {
                            if (result == null) {
                                //fail("Should not be null");
                                System.out.println("ERROR!! 44 ");
                            }
                        }



                        if (result == null) {
                            //fail("Should not be null");
                            System.out.println("ERROR!! 44 ");
                        }

                        //System.out.println(Thread.currentThread().getName() + " got: "
                        // + result.toString() + ", j = " + Integer.toString(j));
                    }
                }
            });

            t.start();
            System.out.println(t.getName());
        }
    }

}
