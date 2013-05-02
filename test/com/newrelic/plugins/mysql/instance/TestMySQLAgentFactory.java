package com.newrelic.plugins.mysql.instance;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.newrelic.data.in.configuration.ConfigurationException;
import com.newrelic.plugins.mysql.MetricMeta;
import com.newrelic.plugins.mysql.MySQL;

public class TestMySQLAgentFactory {

	@SuppressWarnings("unchecked")
	@Test
	public void verifyMetricCategoryConfiguration() {

		MySQLAgentFactory factory = new MySQLAgentFactory();
		Map<String, Object> categories = null;
		try {
			categories = factory.readCategoryConfiguration();
		} catch (ConfigurationException e) {
			fail(e.getMessage());
		}
		assertEquals(6, categories.size());
		Object status = categories.get("status");
		assertNotNull(status);
		Map<String, String> map = (Map<String,String>)status;
		assertEquals("SHOW GLOBAL STATUS", map.get("SQL"));
		assertNull(map.get("result"));

		Object slave = categories.get("slave");
		assertNotNull(slave);
		map = (Map<String,String>)slave;
		assertEquals("SHOW SLAVE STATUS", map.get("SQL"));
		assertEquals("row",map.get("result"));
		
		Object doesnotexist = categories.get("doesnotexist");
		assertNull(doesnotexist);
		
	}
	
	@Test
	public void verifyMetricCategoryValueAttribute() {
		MySQLAgentFactory factory = new MySQLAgentFactory();
		Map<String, Object> categories = null;
		try {
			categories = factory.readCategoryConfiguration();
		} catch (ConfigurationException e) {
			fail(e.getMessage());
		}
		assertEquals(6, categories.size());

		Object status = categories.get("status");
		assertNotNull(status);
		@SuppressWarnings("unchecked")
		Map<String, String> map = (Map<String,String>)status;
		String valueMetrics = map.get("value_metrics"); 
		assertNotNull(valueMetrics);
		Set<String> metrics = new HashSet<String>(Arrays.asList(valueMetrics.toLowerCase().split(MySQLAgent.COMMA)));
		assertEquals(30,metrics.size());
		assertTrue(metrics.contains("uptime"));
		assertFalse(metrics.contains("com_select"));

	}
}
