package cpen221.mp3.cache;

import java.util.HashSet;

public class Cache <T extends Cacheable> {

    //TODO - specs, comments, RI, AF, thread safety conditions

    // assume each item has a unique id

    /* the default cache size is 32 objects */
    private static final int DSIZE = 32;

    /* the default timeout value is 3600s */
    private static final int DTIMEOUT = 3600;

    private HashSet<CacheObject<T>> data;
    private int capacity;
    private int timeout;

    /**
     * Create a cache with a fixed capacity and a timeout value.
     * Objects in the cache that have not been refreshed within the timeout period
     * are removed from the cache.
     *
     * @param capacity the number of objects the cache can hold
     * @param timeout  the duration (in seconds) an object should be in the cache
     *                 before it times out
     */
    public Cache(int capacity, int timeout) {
        this.capacity = capacity;
        this.timeout = timeout;
        this.data = new HashSet<>(this.capacity);
    }

    /**
     * Create a cache with default capacity and timeout values.
     */
    public Cache() {
        this(DSIZE, DTIMEOUT);
    }

    /**
     * Add a value to the cache.
     * If the cache is full then remove the least recently accessed object to
     * make room for the new object.
     */
    public boolean put(T t) {
        CacheObject<T> val = new CacheObject<>(t);

        expire();
        if (data.contains(val)) {
            touch(t.id());
        } else if (data.size() >= capacity) {
            removeLeastRequested();
            data.add(val);
        } else {
            data.add(val);
            return true;
        }

        return false;
    }

    /**
     * @param id the identifier of the object to be retrieved
     * @return the object that matches the identifier from the cache
     */
    public T get(String id) throws NoSuchCacheElementException {
        expire();
        for (CacheObject c : this.data) {
            if (c.t.id().equals(id)) {
                touch(c.t.id());
                c.numRequests++;

                return (T) c.t;
            }
        }

        throw new NoSuchCacheElementException();
    }

    /**
     * Update the last refresh time for the object with the provided id.
     * This method is used to mark an object as "not stale" so that its timeout
     * is delayed.
     *
     * @param id the identifier of the object to "touch"
     * @return true if successful and false otherwise
     */
    public boolean touch(String id) {
        expire();
        for (CacheObject c : this.data) {
            if (c.t.id().equals(id)) {
                c.lastAccess = System.currentTimeMillis();
                return true;
            }
        }

        return false;
    }

    /**
     * Update an object in the cache.
     * This method updates an object and acts like a "touch" to renew the
     * object in the cache.
     *
     * @param t the object to update
     * @return true if successful and false otherwise
     */
    public boolean update(T t) {
        return touch(t.id());
    }

    // assumes data has at least 1 element
    private void removeLeastRequested() {
        CacheObject<T> least = null;
        int leastRequests = Integer.MAX_VALUE;
        for (CacheObject<T> c : this.data) {
            if (c.numRequests < leastRequests) {
                least = c;
                leastRequests = c.numRequests;
            }
        }

        this.data.remove(least);
    }

    private void expire() {
        for (CacheObject<T> c : this.data) {
            if (c.lastAccess + this.timeout * 1000 < System.currentTimeMillis()) {
                this.data.remove(c);
            }
        }
    }


    private class CacheObject<S extends T> {
        private T t;
        private long lastAccess;
        private int numRequests;

        private CacheObject(T t) {
            this.t = t;
            this.lastAccess = System.currentTimeMillis();
            numRequests = 0;
        }

        @Override
        public int hashCode() {
            return t.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CacheObject) {
                CacheObject c = (CacheObject<?>) o;
                if (this.t.equals(c.t)) {
                    return true;
                }
            }
            return false;
        }
    }

}
