package com.digital.test;


import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

public class CacheAggregator  implements CacheService {

    ListeningExecutorService executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(10));
    List<ListenableFuture<Object>> futures = Lists.newArrayList();

    private List<CacheService> caches = Lists.newArrayList();

    public CacheAggregator(CacheService... caches) {
        this.caches = Arrays.asList(caches);
    }

    public Object get(String key) {
        List<Callable<Object>> retrieveTasks = Lists.transform(caches, retrieveFunctionByKey(key));
        List<ListenableFuture<Object>> futures = Lists.newArrayList();

        for (Callable<Object> task : retrieveTasks) {
            futures.add(executor.submit(task));
        }

        final ListenableFuture<List<Object>> resultsFuture = Futures.allAsList(futures);

        return null; //todo some logic here
    }



    private Function<CacheService, Callable<Object>> retrieveFunctionByKey(final String key) {
        return new Function<CacheService, Callable<Object>>() {
            public Callable<Object> apply(final CacheService cache) {
                return new Callable<Object>() {
                    public Object call() throws Exception {
                        return cache.get(key);
                    }
                };
            }
        };

    }
}

