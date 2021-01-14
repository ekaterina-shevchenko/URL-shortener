package de.tum.services.cache.jobs;

import de.tum.services.ConsistentHashingService;
import de.tum.services.cache.CachingService;

import java.util.Set;

public class CacheCleaner implements Runnable{
    private final CachingService cachingService = CachingService.getInstance();
    private final ConsistentHashingService consistentHashingService = ConsistentHashingService.getInstance();

    @Override
    public void run() {
        Set<String> cachedAliases = cachingService.listElements();
        for (String cachedAlias : cachedAliases) {
            if (!consistentHashingService.isOurs(cachedAlias)) {
                cachingService.removeFromCache(cachedAlias);
            }
        }
    }
}
