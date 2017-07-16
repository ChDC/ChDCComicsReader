package com.chdc.comicsreader;

import com.chdc.comicsreader.utils.NumberStringComparator;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by Wen on 2017/7/16.
 */

public class NumberStringComparatorUnitTest {
    @Test
    public void testCompare(){
        assertTrue(NumberStringComparator.INSTANCE.compare("1", "002") < 0);
        assertTrue( NumberStringComparator.INSTANCE.compare("0", "002") < 0);
        assertTrue(NumberStringComparator.INSTANCE.compare("2", "11") < 0);
        assertTrue(NumberStringComparator.INSTANCE.compare("Chapter 1", "Chapter 002") < 0);
        assertTrue(NumberStringComparator.INSTANCE.compare("Chapter 1 test", "Chapter 002 test") < 0);

        assertTrue(NumberStringComparator.INSTANCE.compare("Chapter 1 test", "Chapter 001 test") == 0);
        assertTrue(NumberStringComparator.INSTANCE.compare("Chapter 1 test", "Chapter 1 test") == 0);
        assertTrue(NumberStringComparator.INSTANCE.compare("Chapter test", "Chapter test") == 0);
        assertTrue(NumberStringComparator.INSTANCE.compare("Chapter test1", "Chapter test") > 0);
    }
}
