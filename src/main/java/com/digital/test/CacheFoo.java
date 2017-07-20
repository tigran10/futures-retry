package com.digital.test;


public class CacheFoo implements CacheService {

    public Object get(String key) {
        return "I am FOO";
    }
}
