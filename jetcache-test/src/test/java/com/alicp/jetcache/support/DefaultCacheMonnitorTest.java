package com.alicp.jetcache.support;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.MonitoredCache;
import com.alicp.jetcache.MultiLevelCache;
import com.alicp.jetcache.embedded.EmbeddedCacheBuilder;
import com.alicp.jetcache.embedded.EmbeddedCacheConfig;
import com.alicp.jetcache.embedded.LinkedHashMapCache;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2016/11/1.
 *
 * @author <a href="mailto:yeli.hl@taobao.com">huangli</a>
 */
public class DefaultCacheMonnitorTest {

    public Cache createCache() {
        return EmbeddedCacheBuilder
                .createEmbeddedCacheBuilder()
                .limit(10)
                .expireAfterWrite(200, TimeUnit.SECONDS)
                .buildFunc(c -> new LinkedHashMapCache((EmbeddedCacheConfig) c))
                .build();
    }

    private void basetest(Cache cache, DefaultCacheMonitor monitor) {
        cache.get("K1");
        cache.put("K1", "V1");
        cache.get("K1");
        cache.invalidate("K1");
        cache.computeIfAbsent("K1", (k) -> null);
        cache.computeIfAbsent("K1", (k) -> null, true);
        cache.get("K1");
        cache.invalidate("K1");

        cache.computeIfAbsent("K2", (k) -> null, false, 10, TimeUnit.SECONDS);
        cache.computeIfAbsent("K2", (k) -> null, true, 10, TimeUnit.SECONDS);
        cache.get("K2");
        cache.invalidate("K2");

        CacheStat s = monitor.getCacheStat();
        Assert.assertEquals(8, s.getGetCount());
        Assert.assertEquals(3, s.getGetHitCount());
        Assert.assertEquals(5, s.getGetMissCount());
        Assert.assertEquals(3, s.getPutCount());
        Assert.assertEquals(3, s.getInvalidateCount());
    }

    @Test
    public void testWithoutLogger() {
        Cache cache = createCache();
        DefaultCacheMonitor monitor = new DefaultCacheMonitor("Test");
        cache = new MonitoredCache(cache, monitor);
        basetest(cache, monitor);
    }

    @Test
    public void testWithLogger() throws Exception {
        DefaultCacheMonitorStatLogger logger = new DefaultCacheMonitorStatLogger();
        Cache c1 = createCache();
        DefaultCacheMonitor m1 = new DefaultCacheMonitor("cache1", 2 , TimeUnit.SECONDS, logger);
        Cache c2 = createCache();
        DefaultCacheMonitor m2 = new DefaultCacheMonitor("cache2", 2, TimeUnit.SECONDS, logger);

        c1 = new MonitoredCache(c1, m1);
        c2 = new MonitoredCache(c2, m2);
        basetest(c1, m1);
        basetest(c2, m2);

        Cache mc = new MultiLevelCache(c1, c2);
        DefaultCacheMonitor mcm = new DefaultCacheMonitor("cache1", 2 , TimeUnit.SECONDS, logger);
        mc = new MonitoredCache(mc, mcm);
        basetest(mc, mcm);

        Thread.sleep(10000);
    }
}