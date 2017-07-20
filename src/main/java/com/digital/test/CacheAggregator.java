package com.digital.test;


import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
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
        List<Callable<Object>> retrieveTasks = copyOf(transform(caches, retrieveFunctionByKey(key)));
        List<ListenableFuture<Object>> futures = Lists.newArrayList();


        for (Callable<Object> task : retrieveTasks) {
            futures.add(executor.submit(task));
        }

        final ListenableFuture<List<Object>> resultsFuture = Futures.allAsList(futures);

        Futures.addCallback(resultsFuture, createCallback(), executor);


        return null;
    }


    private FutureCallback<Object> createCallback() {
        return new FutureCallback<Object>() {
            public void onSuccess(Object object) {
                System.out.println("wohoo got value from cache");
            }

            public void onFailure(Throwable t) {
                System.out.println("holly crap, just failed on getting value from cache");
            }
        };
    }


    private Function<CacheService, Callable<Object>> retrieveFunctionByKey(final String key) {
        return new Function<CacheService, Callable<Object>>() {
            public Callable<Object> apply(final CacheService cache) {
                return new Callable<Object>() {
                    public Object call() throws Exception {
                        System.out.println("trying to get key " + key + "from cache name" + cache);
                        return cache.get(key);
                    }
                };
            }
        };

    }
}

