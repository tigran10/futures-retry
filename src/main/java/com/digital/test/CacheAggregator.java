package com.digital.test;


import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.*;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Lists.transform;
import static com.google.common.util.concurrent.Futures.addCallback;
import static com.google.common.util.concurrent.Futures.successfulAsList;

public class CacheAggregator implements CacheService {

    private ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));

    private List<CacheService> caches = ImmutableList.of();

    public CacheAggregator(CacheService... caches) {
        this.caches = copyOf(caches);
    }

    public Object get(String key) {
        List<Task<Object>> retrieveTasks = copyOf(transform(caches, cacheRetrievalFunction(key)));
        List<ListenableFuture<Object>> futures = submitTasks(retrieveTasks);

        // more info here https://github.com/google/guava/wiki/ListenableFutureExplained
        ListenableFuture<List<Object>> listListenableFuture = successfulAsList(futures);
        Optional<Object> result = collectResultsFromFuture(listListenableFuture);

        return result.orNull();

    }


    private Optional<Object> collectResultsFromFuture(ListenableFuture<List<Object>> listListenableFuture) {

        Optional<Object> result;
        try {
            List<Object> objects = listListenableFuture.get();
            warnForInconsistentCacheEntries(objects);
            result = Iterables.tryFind(objects, Predicates.notNull());
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().log("super unexpected thing");
            throw new RuntimeException(e);
        }

        return result;

    }

    private List<ListenableFuture<Object>> submitTasks(List<Task<Object>> retrieveTasks) {
        List<ListenableFuture<Object>> futures = newArrayList();
        for (Task<Object> task : retrieveTasks) {
            ListenableFuture<Object> futureRetrieve = executor.submit(task);
            addCallback(futureRetrieve, cacheRetrievalCallback(task.getName()), executor);
            futures.add(futureRetrieve);
        }
        return futures;
    }


    //todo this should be much more clever, respecting nulls
    private void warnForInconsistentCacheEntries(List<Object> objects) {

        if (Sets.newHashSet(objects).size() != 1) {
            getLogger().log("world is very inconsistent");
        }

    }


    private FutureCallback<Object> cacheRetrievalCallback(final String cachName) {
        return new FutureCallback<Object>() {
            public void onSuccess(Object object) {
                getLogger().log("wohoo got value from cache: " + cachName);
            }

            public void onFailure(Throwable t) {
                getLogger().log("holly crap, just failed on getting value from cache: " + cachName);
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
                        getLogger().log("trying to get key " + key + " from cache name " + cache);
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

    public Logger getLogger() {
        return new Logger();
    }
}
