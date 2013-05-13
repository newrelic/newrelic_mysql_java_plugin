package com.newrelic.plugins.mysql.instance;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.binding.Context;
import com.newrelic.plugins.mysql.MetricMeta;
import com.newrelic.plugins.mysql.MySQL;

/**
 * This class creates a specific MySQL agent that is used to 
 * obtain a MySQL database connection, gather requested metrics
 * and report to New Relic
 * 
 * @author Ronald Bradford me@ronaldbradford.com
 *
 */
public class MySQLAgent extends Agent {
	private static final String GUID = "com.newrelic.plugins.mysql.instance";
	private static final String version = "0.5.0";
	public static final String COMMA = ",";
	
	private String name;												// Agent Name

	private String host;												// MySQL Connection parameters
	private String user;
	private String passwd;
	private String properties;									
	
	private String metrics;												// Metrics to be collected for this agent
 	private Map<String, MetricMeta> metricsMeta = 						// Definition of MySQL meta data (counter, unit, type etc)
 			new HashMap<String, MetricMeta>();							
 	private Map<String, Object> metricCategories = 						// Definition of categories of metrics
 			new HashMap<String, Object>();

 	private MySQL m;													// Per agent MySQL Object

	final Logger logger;												// Local convenience variable

 	/**
 	 * Default constructor to create a new MySQL Agent
 	 * @param map 
 	 * 
 	 * @param String Human name for Agent
 	 * @param String MySQL Instance host:port
 	 * @param String MySQL user
 	 * @param String MySQL user password
 	 * @param String CSVm List of metrics to be monitored
 	 */
 	public MySQLAgent(String name, String host, String user, String passwd, String properties, String metrics, Map<String, Object> metricCategories) {
	   	super(GUID, version);

	   	this.name = name;
	   	this.host = host;
	   	this.user = user;
	   	this.passwd = passwd;
	   	this.properties = properties;
	   	this.metrics = metrics.toLowerCase();
	   	this.metricCategories = metricCategories;

	   	this.m = new MySQL();
	   	
	   	logger = Context.getLogger();									// Set logging to current Context
	   	MySQL.setLogger(logger);										// Push logger to MySQL Object
	   	createMetaData();												// Define incremental counters that are value/sec etc
	}
	
	/**
	 *  This method is run for every poll cycle of the Agent.
	 *  Get a MySQL Database connection and gather metrics. 
	 */
	public void pollCycle() {
		Connection c = m.getConnection(host, user, passwd, properties);	// Get a database connection (which should be cached)
		if (c == null) return;											// Unable to continue without a valid database connection
	 	
		Map<String,Number> results = gatherMetrics(c, metrics);			// Gather defined metrics 
		reportMetrics(results);											// Report Metrics to New Relic
	}

	/**
	 * This method runs the varies categories of MySQL statements
	 * and gathers the metrics that can be reported
	 * 
	 * @param Connection c MySQL Database Connection
	 * @param String List of metrics to be obtained for this agent
	 * @return Map of metrics and values
	 */
	private Map<String, Number> gatherMetrics(Connection c, String metrics) {
	 	Map<String,Number> results = new HashMap<String,Number>();		// Create an empty set of results
	 	Map<String,Object> categories = getMetricCategories(); 			// Get current Metric Categories

	 	Iterator<String> iter = categories.keySet().iterator();	
	 	metrics = metrics + COMMA;
	 	while (iter.hasNext()) {
	 		String category = (String)iter.next();
			@SuppressWarnings("unchecked")
			Map<String, String> attributes = (Map<String,String>)categories.get(category);
	 		if (metrics.contains(category + COMMA)) 
	 			results.putAll(MySQL.runSQL(c, category, attributes.get("SQL"), "row".equals(attributes.get("result"))));
	 	}
	 	results.putAll(newRelicMetrics(results, metrics));
 		return results;
	}

	/**
	 * This method creates a number of custom New Relic Metrics, that are derived from
	 * raw MySQL status metrics
	 * 
	 * @param Map existing Gathered MySQL metrics
	 * @param metrics  String of the Metric Categories to capture
	 * @return Map  Additional derived metrics
	 */
	protected Map<String, Number> newRelicMetrics(Map<String, Number> existing, String metrics) {
    	Map<String, Number> derived = new HashMap<String,Number>();

 		if (!metrics.contains("newrelic" + COMMA)) return derived;		// Only calculate newrelic category if specified.
 		if (!metrics.contains("status" + COMMA)) return derived;		// "status" category is a pre-requisite

	 	logger.info("Adding NewRelic derived metrics");

	 	try {															// Catch any number conversion problems
		 	/* read and write volume */
		 	derived.put("newrelic/volume_reads", existing.get("status/com_select").intValue() + existing.get("status/qcache_hits").intValue());
	    	derived.put("newrelic/volume_writes", existing.get("status/com_insert").intValue() + existing.get("status/com_insert_select").intValue() +
					                       existing.get("status/com_update").intValue() + existing.get("status/com_update_multi").intValue() +
					                       existing.get("status/com_delete").intValue() + existing.get("status/com_delete_multi").intValue() +
					                       existing.get("status/com_replace").intValue() + existing.get("status/com_replace_select").intValue());
	
	    	/* read and write throughput */
		 	derived.put("newrelic/bytes_reads", existing.get("status/bytes_sent").intValue());
		 	derived.put("newrelic/bytes_writes", existing.get("status/bytes_received").intValue());
	
	    	/* Connection management */
		 	float threads_connected = existing.get("status/threads_running").floatValue();
		 	float threads_running = existing.get("status/threads_running").floatValue();
		 	derived.put("newrelic/connections_connected", (int)threads_connected);
		 	derived.put("newrelic/connections_running", (int)threads_running);
		 	derived.put("newrelic/connections_maximum", existing.get("status/max_used_connections").intValue());
	
		 	derived.put("newrelic/pct_connnection_utilization", (threads_running  / threads_connected) * 100.0); 

	    	/* Query Cache */
		 	float qc_hits = existing.get("status/qcache_hits").floatValue();
		 	float reads = existing.get("status/com_select").floatValue();
		 	
		 	derived.put("newrelic/pct_query_cache_utilization", (qc_hits / (qc_hits + reads))* 100.0); 
		 	
		 	/* Replication specifics */
	 		if (metrics.contains("slave" + COMMA)) {					// "slave" category is a pre-requisite for these metrics
			 	derived.put("newrelic/replication_lag", existing.get("slave/seconds_behind_master").intValue());
			 	int slave_io_thread_running = existing.get("slave/slave_io_running").intValue();
			 	int slave_sql_thread_running = existing.get("slave/slave_sql_running").intValue();
			 	
			 	/* both need to be YES, which is 1 */
			 	int replication_status = 1;								// Default as in ERROR
			 	if (slave_io_thread_running + slave_sql_thread_running == 2) 
			 		replication_status = 0;
			 	derived.put("newrelic/replication_status", replication_status);
	 		} else {													// This is a hack because the NR UI can't handle it missing for graphs
			 	derived.put("newrelic/replication_lag", 0);
			 	derived.put("newrelic/replication_status", 0);
 	 		}
		 	/* Innodb Specific Metrics */
		 	float innodb_read_requests = existing.get("status/innodb_buffer_pool_read_requests").floatValue();
		 	float innodb_reads = existing.get("status/innodb_buffer_pool_reads").floatValue();
	    	derived.put("newrelic/pct_innodb_buffer_pool_hit_ratio", (innodb_read_requests / 
	    			                                                 (innodb_read_requests + innodb_reads)) * 100.0);
	 	} catch (Exception e) {
		 	logger.severe("An error occured calculating New Relic custom metrics. " + e.getMessage());	 		
	 	}

	 	return derived;
	}

	/**
	 * This method does the reporting of metrics to New Relic
	 * 
	 * @param Map results 
	 */
	public void reportMetrics(Map<String,Number> results) { 
	 	logger.info("Reporting " + results.size() + " metrics");
	 	logger.finest(results.toString());

	 	Iterator<String> iter = results.keySet().iterator();			
	 	while (iter.hasNext()) {										// Iterate over current metrics	
	 		String key = (String)iter.next().toLowerCase();
	 		Number val = results.get(key);
	 		MetricMeta md = getMetricMeta(key);
	 		logger.fine("Metric " + " " + key + "(" + md.getUnit() + ")=" + val + " " + (md.isCounter() ? "counter" : ""));

	 		if (md.isCounter()) {										// Metric is a counter
					reportMetric(key , md.getUnit(), md.getCounter().process(val).floatValue());
			} else {													// Metric is a fixed Number
				if (java.lang.Float.class.equals(results.get(key).getClass())) {	
					reportMetric(key, md.getUnit(), val.floatValue()); 	// We are working with a float value
	 			} else {
 					reportMetric(key , md.getUnit(), val.intValue());	// We are working with an int
 				}
	 		}
	 	}
	}

	/**
	 * This method creates the metric meta data that is derived from the provided configuration
	 * and New Relic specific metrics.
	 */
	private void createMetaData() {
	 	Map<String,Object> categories = getMetricCategories(); 			// GetcreateMetaData current Metric Categories
	 	Iterator<String> iter = categories.keySet().iterator();	
	 	while (iter.hasNext()) {
	 		String category = (String)iter.next();
			@SuppressWarnings("unchecked")
			Map<String, String> attributes = (Map<String,String>)categories.get(category);
			String valueMetrics = attributes.get("value_metrics");
			if (valueMetrics != null) {
				Set<String> metrics = new HashSet<String>(Arrays.asList(valueMetrics.toLowerCase().split(MySQLAgent.COMMA)));
				for (String s: metrics) {
					addMetricMeta(category + MySQL.SEPARATOR + s, new MetricMeta(false));
				}
			}
	 	}

	 	/* Define New Relic specific metrics used for default dashboards */
		addMetricMeta("newrelic/volume_reads", new MetricMeta(true, "queries/sec"));
		addMetricMeta("newrelic/volume_writes", new MetricMeta(true, "queries/sec"));

		addMetricMeta("newrelic/bytes_reads", new MetricMeta(true, "bytes/sec"));
		addMetricMeta("newrelic/bytes_writes", new MetricMeta(true, "bytes/sec"));

		addMetricMeta("newrelic/connections_connected", new MetricMeta(false, "value"));
		addMetricMeta("newrelic/connections_running", new MetricMeta(false, "value"));
		addMetricMeta("newrelic/connections_maximum", new MetricMeta(false, "value"));

		addMetricMeta("newrelic/pct_connnection_utilization", new MetricMeta(false, "pct"));
		addMetricMeta("newrelic/pct_innodb_buffer_pool_hit_ratio", new MetricMeta(false, "pct"));
		addMetricMeta("newrelic/pct_query_cache_utilization", new MetricMeta(false, "pct"));

		addMetricMeta("newrelic/replication_lag", new MetricMeta(false, "value"));
		addMetricMeta("newrelic/replication_status", new MetricMeta(false, "value"));

	 	/* Define improved metric values for certain general metrics */
		addMetricMeta("status/bytes_received", new MetricMeta(true, "bytes/sec"));
		addMetricMeta("status/bytes_sent", new MetricMeta(true, "bytes/sec"));
		addMetricMeta("status/com_select", new MetricMeta(true, "selects/sec"));
		addMetricMeta("status/com_insert", new MetricMeta(true, "ops/sec"));
		addMetricMeta("status/com_insert_select", new MetricMeta(true, "ops/sec"));
		addMetricMeta("status/com_update", new MetricMeta(true, "ops/sec"));
		addMetricMeta("status/com_delete", new MetricMeta(true, "ops/sec"));
		addMetricMeta("status/com_replace", new MetricMeta(true, "ops/sec"));

	}

	/**
	 * Add the given metric meta information to the Map of all metric meta information for this agent
	 * 
	 * @param String key
	 * @param Metric mm
	 */
	private void addMetricMeta(String key, MetricMeta mm) {
		metricsMeta.put(key, mm); 		
	}
  
	/**
	 * This provides a lazy instantiation of a MySQL metric where no meta data was defined
	 * and means new metrics can be captured automatically.
	 * 
	 * A default metric is a integer value
	 * 
	 * @param String Metric to look up
	 * @return MetridMeta  Structure of information about the metric
	 */
	private MetricMeta getMetricMeta(String key) {
 		MetricMeta md = (MetricMeta)metricsMeta.get(key);				// Look for existing meta data on metric
 		if (md == null) {												// If not found
			logger.info("Adding default metric for " + key);
			addMetricMeta(key, MetricMeta.defaultMetricMeta());			// create a default representation
 	 		md = (MetricMeta)metricsMeta.get(key);
 		}
 		return md;
	}

	/**
	 * Return the human readable name for this agent.
	 * 
	 * @return String
	 */
	@Override
	public String getComponentHumanLabel() {
		return name;
	}

	/**
	 * Return the map of metric categories
	 * 
	 * @return Map
	 */
	public Map<String, Object> getMetricCategories() {
		return metricCategories;
	}
}
