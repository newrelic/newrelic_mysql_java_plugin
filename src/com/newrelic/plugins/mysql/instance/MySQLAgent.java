package com.newrelic.plugins.mysql.instance;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


import com.newrelic.data.in.Agent;
import com.newrelic.data.in.binding.Context;
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
	private static final String version = "0.2.2";
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

    	derived.put("newrelic/reads", existing.get("status/com_select").intValue() + existing.get("status/qcache_hits").intValue());
    	derived.put("newrelic/writes", existing.get("status/com_insert").intValue() + existing.get("status/com_insert_select").intValue() +
				                       existing.get("status/com_update").intValue() + existing.get("status/com_update_multi").intValue() +
				                       existing.get("status/com_delete").intValue() + existing.get("status/com_delete_multi").intValue() +
				                       existing.get("status/com_replace").intValue() + existing.get("status/com_replace_select").intValue());
    	derived.put("newrelic/innodb_buffer_pool_hit_ratio", (existing.get("status/innodb_buffer_pool_read_requests").intValue() / 
    			                                             (existing.get("status/innodb_buffer_pool_read_requests").intValue() + existing.get("status/innodb_buffer_pool_reads").intValue()) * 100.0)).floatValue();
    			
		return derived;
	}

	/**
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
	 		logger.info("Metric " + " " + key + "(" + md.getUnit() + ")=" + val + " " + (md.isCounter() ? "counter" : ""));

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

	 	/* TODO: Parameterize Hardcoded examples */
		addMetricMeta("status/bytes_received", new MetricMeta(true, "bytes/sec"));
		addMetricMeta("status/bytes_sent", new MetricMeta(true, "bytes/sec"));
		addMetricMeta("status/com_select", new MetricMeta(true, "ops/sec"));
		addMetricMeta("status/com_insert", new MetricMeta(true, "ops/sec"));
		addMetricMeta("status/com_insert_select", new MetricMeta(true, "ops/sec"));
		addMetricMeta("status/com_delete", new MetricMeta(true, "ops/sec"));
		addMetricMeta("status/com_call_procedure", new MetricMeta(true, "ops/sec"));
		addMetricMeta("network/bytes_received", new MetricMeta(true, "bytes/sec"));
		addMetricMeta("network/bytes_sent", new MetricMeta(true, "bytes/sec"));

		addMetricMeta("newrelic/reads", new MetricMeta(true, "queries/sec"));
		addMetricMeta("newrelic/writes", new MetricMeta(true, "queries/sec"));
		addMetricMeta("newrelic/innodb_buffer_pool_hit_ratio", new MetricMeta(false, "pct"));
	}

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

	public Map<String, Object> getMetricCategories() {
		return metricCategories;
	}
}
