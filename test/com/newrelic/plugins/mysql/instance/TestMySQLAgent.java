package com.newrelic.plugins.mysql.instance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

/**
 * All enabled tests pass on version 5.5 of MySQL.
 * 
 * @author Ronald Bradford me@ronaldbradford.com
 * 
 */
public class TestMySQLAgent {

    @Ignore
    @Test
    public void verifyNewRelicMetrics() {
        Map<String, Float> results = new HashMap<String, Float>();

        results.put("status/com_select", 1.0f);
        results.put("status/qcache_hits", 2.0f);
        results.put("status/com_insert", 4.0f);
        results.put("status/com_insert_select", 8.0f);
        results.put("status/com_update", 16.0f);
        results.put("status/com_update_multi", 32.0f);
        results.put("status/com_delete", 64.0f);
        results.put("status/com_delete_multi", 128.0f);
        results.put("status/com_replace", 256.0f);
        results.put("status/com_replace_select", 512.0f);

        Set<String> metrics = new HashSet<String>();
        metrics.add("status");
        metrics.add("newrelic");

        MySQLAgent agent = new MySQLAgent("test", MySQLAgent.AGENT_DEFAULT_HOST, MySQLAgent.AGENT_DEFAULT_USER, MySQLAgent.AGENT_DEFAULT_PASSWD,
                MySQLAgent.AGENT_DEFAULT_PROPERTIES, metrics, new HashMap<String, Object>());

        Map<String, Float> newRelicMetrics = agent.newRelicMetrics(results);
        assertNotNull(newRelicMetrics);
        assertEquals(2, newRelicMetrics.size());
        assertEquals(3.0f, newRelicMetrics.get("newrelic/reads"), 0.0001f);
        assertEquals(1020.0f, newRelicMetrics.get("newrelic/writes"), 0.0001f);
    }

    @Test
    public void testExpresssions() {
        Map<String, Number> existing = new HashMap<String, Number>();

        existing.put("status/threads_running", 4);
        existing.put("status/threads_connected", 10);

        assertEquals(50.0, (5.0 / 10.0) * 100.0, 0.0001);
        float threads_running = existing.get("status/threads_running").floatValue();
        float threads_connected = existing.get("status/threads_connected").floatValue();
        assertEquals(4.0, threads_running, 0.0001);
        assertEquals(10.0, threads_connected, 0.0001);
        assertEquals(40.0, (threads_running / threads_connected) * 100.0, 0.0001);
        assertEquals(4, (int) threads_running);
    }

    @Test
    public void testMutexMetricReporting() throws InterruptedException {

        Map<String, Float> results = new HashMap<String, Float>();
        results.put("status/qcache_free_memory", 10.0f);
        results.put("innodb_mutex/log0log.c:832", 460.0f);
        results.put("innodb_mutex/combined buf0buf.c:916", 471.0f);

        Set<String> metrics = new HashSet<String>();
        metrics.add("status");
        metrics.add("newrelic");
        metrics.add("innodb_mutex");

        MetricTrackerMySQLAgent agent = new MetricTrackerMySQLAgent("mutex_test", MySQLAgent.AGENT_DEFAULT_HOST, MySQLAgent.AGENT_DEFAULT_USER,
                MySQLAgent.AGENT_DEFAULT_PASSWD, MySQLAgent.AGENT_DEFAULT_PROPERTIES, metrics, new HashMap<String, Object>());

        agent.reportMetrics(results);

        // assert reported metrics only contain status/com_select
        assertTrue(agent.reportedMetrics.size() == 3);
        assertNotNull(agent.reportedMetrics.get("status/qcache_free_memory"));
        assertNull(agent.reportedMetrics.get("innodb_mutex/log0log.c:832"));
        assertNull(agent.reportedMetrics.get("innodb_mutex/combined buf0buf.c:916"));

        // create new results with one mutex updated value
        Map<String, Float> newResults = new HashMap<String, Float>();
        newResults.put("status/qcache_free_memory", 15.0f);
        newResults.put("innodb_mutex/log0log.c:832", 460.0f);
        newResults.put("innodb_mutex/combined buf0buf.c:916", 480.0f);

        TimeUnit.SECONDS.sleep(1);

        // report new results
        agent.reportMetrics(newResults);

        // assert mutex metrics are not null -- the exact metric value is variable and determined by epoch counter timing
        assertTrue(agent.reportedMetrics.size() == 3);
        assertNotNull(agent.reportedMetrics.get("status/qcache_free_memory"));
        assertNotNull(agent.reportedMetrics.get("innodb_mutex/log0log.c:832"));
        assertNotNull(agent.reportedMetrics.get("innodb_mutex/combined buf0buf.c:916"));
    }

    @Test
    public void testIsReportingForCategory() {
        Set<String> metrics = new HashSet<String>();
        metrics.add("status");
        metrics.add("newrelic");
        metrics.add("innodb_mutex");

        MySQLAgent agent = new MySQLAgent("test", MySQLAgent.AGENT_DEFAULT_HOST, MySQLAgent.AGENT_DEFAULT_USER, MySQLAgent.AGENT_DEFAULT_PASSWD,
                MySQLAgent.AGENT_DEFAULT_PROPERTIES, metrics, new HashMap<String, Object>());

        assertTrue(agent.isReportingForCategory("status"));
        assertTrue(agent.isReportingForCategory("newrelic"));
        assertTrue(agent.isReportingForCategory("innodb_mutex"));
        assertFalse(agent.isReportingForCategory("master"));
    }

    /**
     * mysql agent for tracking reported metrics to aid testing
     */
    private static class MetricTrackerMySQLAgent extends MySQLAgent {

        Map<String, Number> reportedMetrics = new HashMap<String, Number>();

        public MetricTrackerMySQLAgent(String name, String host, String user, String passwd, String properties, Set<String> metrics,
                Map<String, Object> metricCategories) {
            super(name, host, user, passwd, properties, metrics, metricCategories);
        }

        /**
         * capture reported metrics for testing
         */
        @Override
        public void reportMetric(String metricName, String units, Number value) {
            reportedMetrics.put(metricName, value);
        }

    }
}
