package com.digital.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CacheAggregatorTest {


    @Mock
    private CacheFoo fooCache;

    @Mock
    private CacheBar barCache;

    @Mock
    private Logger logger;


    private CacheAggregator cache ;

    @Before
    public void init() {
        given(fooCache.name()).willReturn("fooCache");
        given(barCache.name()).willReturn("barCache");
        cache = new CacheAggregator(fooCache, barCache) {
            @Override
            public Logger getLogger() {
                return logger;
            }
        };
    }


    @Test
    public void testCacheAggregatorGetReturnsFirstAvailableValue() {
        //given
        given(fooCache.get(anyString())).willReturn("apple");
        given(barCache.get(anyString())).willReturn("carrot");


        //when
        cache.get("random");

        //then
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(fooCache, atLeast(1)).get(argumentCaptor.capture());
        verify(barCache, atLeast(1)).get(argumentCaptor.capture());
    }

    @Test
    public void testCacheAggregatorTouchesEachCacheOnce() {

        //given
        given(fooCache.get(anyString())).willReturn("carrot");
        given(barCache.get(anyString())).willReturn("apple");

        //then
        assertThat(cache.get("random")).isEqualTo("carrot");
    }


    @Test
    public void testCacheAggregatorGetReturnsFirstNotNullValue() {
        //given
        given(fooCache.get(anyString())).willReturn(null);
        given(barCache.get(anyString())).willReturn("carrot");

        //then
        assertThat(cache.get("random")).isEqualTo("carrot");
    }

    @Test
    public void testCacheAggregatorGetReturnsFirstNotNullValueEvenIfOneOfThemDies() {
        //given
        given(fooCache.get(anyString())).willThrow(new RuntimeException("i am dead"));
        given(barCache.get(anyString())).willReturn("carrot");

        //then
        assertThat(cache.get("random")).isEqualTo("carrot");
    }

    @Test
    public void testCacheAggregatorCallsSuccessCallbackOnEachCache() {

        //given
        given(fooCache.get(anyString())).willReturn("carrot");
        given(barCache.get(anyString())).willReturn("apple");
        

        //when
        cache.get("random");

        //then
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, atLeast(2)).log(argumentCaptor.capture());

        //and then
        List<String> values = argumentCaptor.getAllValues();

        assertThat(values).contains("trying to get key random from cache name fooCache");
        assertThat(values).contains("trying to get key random from cache name barCache");

        assertThat(values).contains("wohoo got value from cache: fooCache");
        assertThat(values).contains("wohoo got value from cache: barCache");

    }

    @Test
    public void testCacheAggregatorCallsFailureCallbackOnEachCache() {

        //given
        given(fooCache.get(anyString())).willThrow(new RuntimeException("i am dead"));
        given(barCache.get(anyString())).willThrow(new RuntimeException("i am dead"));
        

        //when
        cache.get("random");

        //then
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, atLeast(2)).log(argumentCaptor.capture());


        //and then
        List<String> values = argumentCaptor.getAllValues();

        assertThat(values).contains("trying to get key random from cache name fooCache");
        assertThat(values).contains("trying to get key random from cache name barCache");
        assertThat(values).contains("holly crap, just failed on getting value from cache: fooCache");
        assertThat(values).contains("holly crap, just failed on getting value from cache: barCache");

    }

    @Test
    public void testCacheAggregatorCallsFailureAndSuccessCallbacks() {

        //given
        given(fooCache.get(anyString())).willThrow(new RuntimeException("i am dead"));
        given(barCache.get(anyString())).willReturn("I am not");
        


        //when
        cache.get("random");

        //then
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, atLeast(2)).log(argumentCaptor.capture());


        //and then
        List<String> values = argumentCaptor.getAllValues();

        assertThat(values).contains("trying to get key random from cache name fooCache");
        assertThat(values).contains("trying to get key random from cache name barCache");
        assertThat(values).contains("holly crap, just failed on getting value from cache: fooCache");
        assertThat(values).contains("wohoo got value from cache: barCache");

    }

    @Test
    public void testCacheAggregatorCallsWarningLogsWhenInconsistentResultsAreRecorded() {

        //given
        given(fooCache.get(anyString())).willReturn("apple");
        given(barCache.get(anyString())).willReturn("carrot");
        


        //when
        cache.get("random");

        //then
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(logger, atLeast(1)).log(argumentCaptor.capture());


        //and then
        List<String> values = argumentCaptor.getAllValues();
        assertThat(values).contains("world is very inconsistent");

    }

}