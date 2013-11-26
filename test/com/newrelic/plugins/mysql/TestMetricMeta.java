package com.newrelic.plugins.mysql;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

/**
 * This class performs test cases on the MySQL class All enabled tests pass on version 5.5 of MySQL.
 * 
 * @author Ronald Bradford me@ronaldbradford.com
 * 
 */

public class TestMetricMeta {

    @Before
    public void setUp() {
    }

    @Test
    public void verifyMetricValue() {
        MetricMeta mm = new MetricMeta(false);
        assertNotNull(mm);
        assertNull(mm.getCounter());
        assertEquals(MetricMeta.DEFAULT_UNIT, mm.getUnit());
    }

    @Test
    public void verifyMetricCounter() {
        MetricMeta mm = new MetricMeta(true);
        assertNotNull(mm);
        assertNotNull(mm.getCounter());
        assertEquals(MetricMeta.DEFAULT_COUNTER_UNIT, mm.getUnit());
    }

    @Test
    public void verifyMetricCounterWithSpecificUnit() {
        String unit = "ops/sec";
        MetricMeta mm = new MetricMeta(true, unit);
        assertNotNull(mm);
        assertEquals(unit, mm.getUnit());
    }

    @Test
    public void testOperator() {
        boolean isCounter = false;
        assertEquals(MetricMeta.DEFAULT_UNIT, isCounter ? MetricMeta.DEFAULT_COUNTER_UNIT : MetricMeta.DEFAULT_UNIT);
        isCounter = true;
        assertEquals(MetricMeta.DEFAULT_COUNTER_UNIT, isCounter ? MetricMeta.DEFAULT_COUNTER_UNIT : MetricMeta.DEFAULT_UNIT);
    }
}