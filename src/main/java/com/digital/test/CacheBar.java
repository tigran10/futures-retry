package com.digital.test;


public class CacheBar implements CacheService {
    public Object get(String key) {
        return "I am Bar";
    }
}
