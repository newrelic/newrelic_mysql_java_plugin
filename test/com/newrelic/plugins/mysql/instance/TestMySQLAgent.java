package com.newrelic.plugins.mysql.instance;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class TestMySQLAgent {

	@Test
	public void verifyNewRelicMetrics() {
		Map<String, Number> results = new HashMap<String, Number>();

		results.put("status/com_select", 1);
		results.put("status/qcache_hits", 2);
		results.put("status/com_insert", 4);
		results.put("status/com_insert_select", 8);
		results.put("status/com_update", 16);
		results.put("status/com_update_multi", 32);
		results.put("status/com_delete", 64);
		results.put("status/com_delete_multi", 128);
		results.put("status/com_replace",256);
		results.put("status/com_replace_select", 512);

		String metrics="status,newrelic";
		MySQLAgent agent = new MySQLAgent("test",
										  MySQLAgent.AGENT_DEFAULT_HOST, MySQLAgent.AGENT_DEFAULT_USER, MySQLAgent.AGENT_DEFAULT_PASSWD, MySQLAgent.AGENT_DEFAULT_PROPERTIES,
										  metrics, new HashMap<String,Object>());
		
		Map<String, Number> newRelicMetrics = agent.newRelicMetrics(results,metrics);
		assertNotNull(newRelicMetrics);
		assertEquals(2,newRelicMetrics.size());
		assertEquals(3,newRelicMetrics.get("newrelic/reads"));
		assertEquals(1020,newRelicMetrics.get("newrelic/writes"));
	}
	
	@Test
	public void testExpresssions() {
		Map<String, Number> existing = new HashMap<String, Number>();

		existing.put("status/threads_running", 4);
		existing.put("status/threads_connected", 10);

		assertEquals (50.0, (5.0/10.0)*100.0, 0.0001);
	 	float threads_running = existing.get("status/threads_running").floatValue();
		float threads_connected = existing.get("status/threads_connected").floatValue();
		assertEquals(4.0, threads_running, 0.0001);
		assertEquals(10.0, threads_connected, 0.0001);
	 	assertEquals(40.0, (threads_running / threads_connected) * 100.0 , 0.0001);
	 	assertEquals(4, (int)threads_running);
	}
}
