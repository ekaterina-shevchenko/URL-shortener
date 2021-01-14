package de.tum.services.cache;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CachingService {
    private ConcurrentMap<String,String> cache = new ConcurrentHashMap<>();
    private static final CachingService INSTANCE = new CachingService();
    protected CachingService() {}

    public static CachingService getInstance() {
        return INSTANCE;
    }

    public void cache(String key, String value) {
        cache.putIfAbsent(key, value);
    }

    public String getFromCache(String key) {
        return cache.get(key);
    }

    public void removeFromCache(String key) {
        cache.remove(key);
    }

    public Set<String> listElements() {
        return new HashSet<>(cache.keySet());
    }
}
