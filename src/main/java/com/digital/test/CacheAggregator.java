package com.digital.test;


import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.transform;

public class CacheAggregator implements CacheService {

    ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    private List<CacheService> caches = ImmutableList.of();

    public CacheAggregator(CacheService... caches) {
        this.caches = copyOf(caches);
    }

    public Object get(String key) {
        List<Task<Object>> retrieveTasks = copyOf(transform(caches, cacheRetrievalFunction(key)));
        List<ListenableFuture<Object>> futures = Lists.newArrayList();


        for (Task<Object> task : retrieveTasks) {
            ListenableFuture<Object> futureRetrieve = executor.submit(task);
            Futures.addCallback(futureRetrieve, cacheRetrievalCallback(task.getName()), executor);
        }

        ListenableFuture<List<Object>> listListenableFuture = Futures.successfulAsList(futures);
        try {
            List<Object> objects = listListenableFuture.get();
            warnForInconsistentCacheEntries(objects);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("super unexpected thing");

        }

        return null;

    }


    //todo this should be much more clever, respecting nulls
    private void warnForInconsistentCacheEntries(List<Object> objects) {
        if (Sets.newHashSet(objects).size() != 1) {
            System.out.println("super unexpected thing");
        }
    }


    private FutureCallback<Object> cacheRetrievalCallback(final String cachName) {
        return new FutureCallback<Object>() {
            public void onSuccess(Object object) {
                System.out.println("wohoo got value from cache: " + cachName);
            }

            public void onFailure(Throwable t) {
                System.out.println("holly crap, just failed on getting value from cache: " + cachName);
            }
        };
    }


    private Function<CacheService, Task<Object>> cacheRetrievalFunction(final String key) {
        return new Function<CacheService, Task<Object>>() {
            public Task<Object> apply(final CacheService cache) {
                return new Task<Object>() {
                    public String getName() {
                        return cache.name();
                    }

                    public Object call() throws Exception {
                        System.out.println("trying to get key " + key + "from cache name" + cache);
                        return cache.get(key);
                    }
                };
            }
        };

    }

    interface Task<V> extends Callable<V> {
        String getName();
    }

    public String name() {
        return "joined cache";
    }
}
