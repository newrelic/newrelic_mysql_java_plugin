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
	private static final String version = "1.0.5";

	public static final String AGENT_DEFAULT_HOST = "localhost";		// Default values for MySQL Agent
	public static final String AGENT_DEFAULT_USER = "newrelic";
	public static final String AGENT_DEFAULT_PASSWD = "f63c225f4abe9e13";
	public static final String AGENT_DEFAULT_PROPERTIES = "";
	public static final String AGENT_DEFAULT_METRICS = "status,newrelic";

	public static final String AGENT_CONFIG_FILE = "mysql.instance.json";
	public static final String CATEGORY_CONFIG_FILE = "metric.category.json";
	
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

	private boolean firstReport = true;
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

	   	this.name = name;												// Set local attributes for new class object
	   	this.host = host;
	   	this.user = user;
	   	this.passwd = passwd;
	   	this.properties = properties;
	   	this.metrics = metrics.toLowerCase();
	   	this.metricCategories = metricCategories;

	   	this.m = new MySQL();
	   	
	   	logger = Context.getLogger();				    				// Set logging to current Context
	   	MySQL.setLogger(logger);										// Push logger to MySQL Object
	   	createMetaData();												// Define incremental counters that are value/sec etc
	   	
	   	logger.fine("MySQL Agent initialized: " + formatAgentParams(name, host, user, properties, metrics));
	}
 	
 	/**
 	 * Format Agent parameters for logging
 	 * @param name
 	 * @param host
 	 * @param user
 	 * @param properties
 	 * @param metrics
 	 * @return A formatted String representing the Agent parameters
 	 */
 	private String formatAgentParams(String name, String host, String user, String properties, String metrics) {
 	    StringBuilder builder = new StringBuilder();
 	    builder.append("name: ").append(name).append(" | ");
 	    builder.append("host: ").append(host).append(" | ");
 	    builder.append("user: ").append(user).append(" | ");
 	    builder.append("properties: ").append(properties).append(" | ");
 	    builder.append("metrics: ").append(metrics).append(" | ");
 	    return builder.toString();
 	}
	
	/**
	 *  This method is run for every poll cycle of the Agent.
	 *  Get a MySQL Database connection and gather metrics. 
	 */
	public void pollCycle() {
		Connection c = m.getConnection(host, user, passwd, properties);	// Get a database connection (which should be cached)
		if (c == null) return;											// Unable to continue without a valid database connection

	 	logger.fine("Gathering MySQL metrics. " + getAgentInfo());
		Map<String,Number> results = gatherMetrics(c, metrics);			// Gather defined metrics 
		reportMetrics(results);											// Report Metrics to New Relic
		firstReport = false;
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
	 	metrics = metrics + COMMA;										// Add trailing comma for search criteria
	 	while (iter.hasNext()) {
	 		String category = (String)iter.next();
			@SuppressWarnings("unchecked")
			Map<String, String> attributes = (Map<String,String>)categories.get(category);
	 		if (metrics.contains(category + COMMA)) {					// Use a dumb search, including comma to handle overlapping categories
	 			results.putAll(MySQL.runSQL(c, category, attributes.get("SQL"), attributes.get("result")));
	 		}
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
 		if (!metrics.contains("status" + COMMA))   return derived;		// "status" category is a pre-requisite for newrelic metrics

	 	logger.fine("Adding New Relic derived metrics");

	 	try {															// Catch any number conversion problems
		 	/* read and write volume */
		 	derived.put("newrelic/volume_reads", 
		 				existing.get("status/com_select").intValue()  + existing.get("status/qcache_hits").intValue());
	    	derived.put("newrelic/volume_writes", 
	    				existing.get("status/com_insert").intValue()  + existing.get("status/com_insert_select").intValue() +
	    				existing.get("status/com_update").intValue()  + existing.get("status/com_update_multi").intValue() +
	    				existing.get("status/com_delete").intValue()  + existing.get("status/com_delete_multi").intValue() +
	    				existing.get("status/com_replace").intValue() + existing.get("status/com_replace_select").intValue());

	 	} catch (Exception e) {
		 	logger.severe("An error occured calculating read/write volume " + e.getMessage());	 		
	 	}

	 	try {															// Catch any number conversion problems
	    	/* read and write throughput */
		 	derived.put("newrelic/bytes_reads",  existing.get("status/bytes_sent").intValue());
		 	derived.put("newrelic/bytes_writes", existing.get("status/bytes_received").intValue());
	 	} catch (Exception e) {
		 	logger.severe("An error occured calculating read/write throughput " + e.getMessage());	 		
	 	}

	 	try {															// Catch any number conversion problems
	    	/* Connection management */
		 	float threads_connected = existing.get("status/threads_connected").floatValue();
		 	float threads_running   = existing.get("status/threads_running").floatValue();
		 	derived.put("newrelic/connections_connected", (int)threads_connected);
		 	derived.put("newrelic/connections_running", (int)threads_running);
		 	derived.put("newrelic/connections_cached", existing.get("status/threads_cached").intValue());
		 	//derived.put("newrelic/connections_maximum", existing.get("status/max_used_connections").intValue());
		 	derived.put("newrelic/pct_connection_utilization", (threads_running  / threads_connected) * 100.0); 

	 	} catch (Exception e) {
		 	logger.severe("An error occured calculating connection " + e.getMessage());	 		
	 	}

	 	try {															// Catch any number conversion problems
		 	/* InnoDB Metrics */
		 	derived.put("newrelic/innodb_bp_pages_created", existing.get("status/innodb_pages_created").intValue());
		 	derived.put("newrelic/innodb_bp_pages_read",    existing.get("status/innodb_pages_read").intValue());
		 	derived.put("newrelic/innodb_bp_pages_written", existing.get("status/innodb_pages_written").intValue());	

		 	/* Innodb Specific Metrics */
		 	float innodb_read_requests = existing.get("status/innodb_buffer_pool_read_requests").floatValue();
		 	float innodb_reads = existing.get("status/innodb_buffer_pool_reads").floatValue();
	    	derived.put("newrelic/pct_innodb_buffer_pool_hit_ratio", 
	    				(innodb_read_requests / (innodb_read_requests + innodb_reads)) * 100.0);

		 	derived.put("newrelic/innodb_fsyncs_data",   existing.get("status/innodb_data_fsyncs").intValue());
		 	derived.put("newrelic/innodb_fsyncs_os_log", existing.get("status/innodb_os_log_fsyncs").intValue());

	 	} catch (Exception e) {
		 	logger.severe("An error occured calculating InnoDB metrics " + e.getMessage());	 		
	 	}
	
	 	try {															// Catch any number conversion problems
		 	/* InnoDB Metrics */

		 	int pages_total = existing.get("status/innodb_buffer_pool_pages_total").intValue();
		 	int pages_data = existing.get("status/innodb_buffer_pool_pages_data").intValue();
		 	int pages_misc = existing.get("status/innodb_buffer_pool_pages_misc").intValue();
		 	int pages_dirty = existing.get("status/innodb_buffer_pool_pages_dirty").intValue();
		 	int pages_free = existing.get("status/innodb_buffer_pool_pages_free").intValue();

		 	derived.put("newrelic/innodb_buffer_pool_pages_clean",  pages_data - pages_dirty);
		 	derived.put("newrelic/innodb_buffer_pool_pages_dirty",  pages_dirty);
		 	derived.put("newrelic/innodb_buffer_pool_pages_misc",   pages_misc);
		 	derived.put("newrelic/innodb_buffer_pool_pages_free",   pages_free);
		 	derived.put("newrelic/innodb_buffer_pool_pages_unassigned",   pages_total - pages_data - pages_free - pages_misc);

	 	} catch (Exception e) {
		 	logger.severe("An error occured calculating InnoDB metrics " + e.getMessage());	 		
	 	}
	 	
	 	try {															// Catch any number conversion problems

	    	/* Query Cache */
		 	float qc_hits = existing.get("status/qcache_hits").floatValue();
		 	float reads   = existing.get("status/com_select").floatValue();
		 	float free    = existing.get("status/qcache_free_blocks").floatValue();
		 	float total   = existing.get("status/qcache_total_blocks").floatValue();

		 	derived.put("newrelic/query_cache_hits", (int)qc_hits);
		 	derived.put("newrelic/query_cache_misses", existing.get("status/qcache_inserts").intValue());
		 	derived.put("newrelic/query_cache_not_cached", existing.get("status/qcache_not_cached").intValue());	

		 	derived.put("newrelic/pct_query_cache_hit_utilization", (qc_hits / (qc_hits + reads))* 100.0); 
		 	derived.put("newrelic/pct_query_cache_memory_in_use",   100 - ((free/total)* 100.0)); 

	 	} catch (Exception e) {
		 	logger.severe("An error occured calculating Query Cache Metrics " + e.getMessage());	 		
	 	}

	 	try {															// Catch any number conversion problems
		 	float tmp_tables = existing.get("status/created_tmp_tables").floatValue();
		 	float tmp_tables_disk = existing.get("status/created_tmp_disk_tables").floatValue();

		 	derived.put("newrelic/pct_tmp_tables_written_to_disk", (tmp_tables_disk/tmp_tables)* 100.0); 

	 	} catch (Exception e) {
		 	logger.severe("An error occured calculating Temporary Table metrics " + e.getMessage());	 		
	 	}
	 	try {															// Catch any number conversion problems
		 	/* Replication specifics */
	 		if (metrics.contains("slave" + COMMA)) {					// "slave" category is a pre-requisite for these metrics
			 	derived.put("newrelic/replication_lag", existing.get("slave/seconds_behind_master").intValue());
			 	int slave_io_thread_running  = existing.get("slave/slave_io_running").intValue();
			 	int slave_sql_thread_running = existing.get("slave/slave_sql_running").intValue();
			 	
			 	/* both need to be YES, which is 1 */
			 	int replication_status = 1;								// Default as in ERROR
			 	if (slave_io_thread_running + slave_sql_thread_running == 2) 
			 		replication_status = 0;
			 	derived.put("newrelic/replication_status", replication_status);
			 	derived.put("newrelic/slave_relay_log_bytes", existing.get("slave/relay_log_pos").intValue());
			 	derived.put("newrelic/master_log_lag_bytes", existing.get("slave/read_master_log_pos").intValue() -  existing.get("slave/exec_master_log_pos").intValue());
	 		} else {													// This is a hack because the NR UI can't handle it missing for graphs
			 	derived.put("newrelic/replication_lag",    0);
			 	derived.put("newrelic/replication_status", 0);
			 	derived.put("newrelic/slave_relay_log_bytes", 0);
			 	derived.put("newrelic/master_log_lag_bytes", 0);
 	 		}
	 	} catch (Exception e) {
		 	logger.severe("An error occured calculating Replication Metrics " + e.getMessage());	 		
	 	}

	 	return derived;
	}

	/**
	 * This method does the reporting of metrics to New Relic
	 * 
	 * @param Map results 
	 */
	public void reportMetrics(Map<String,Number> results) { 
		int count = 0;
	 	logger.fine("Collected " + results.size() + " MySQL metrics. " + getAgentInfo());
	 	logger.finest(results.toString());

	 	Iterator<String> iter = results.keySet().iterator();			
	 	while (iter.hasNext()) {										// Iterate over current metrics	
	 		String key = (String)iter.next().toLowerCase();
	 		Number val = results.get(key);
	 		MetricMeta md = getMetricMeta(key);
	 		if (md != null) {											// Metric Meta data exists (from metric.category.json)
		 		logger.fine("Metric " + " " + key + "(" + md.getUnit() + ")=" + val + " " + (md.isCounter() ? "counter" : ""));
		 		count++;
	
		 		if (md.isCounter()) {										// Metric is a counter
						reportMetric(key , md.getUnit(), md.getCounter().process(val));
				} else {													// Metric is a fixed Number
					if (java.lang.Float.class.equals(results.get(key).getClass())) {	
						reportMetric(key, md.getUnit(), val.floatValue()); 	// We are working with a float value
		 			} else {
	 					reportMetric(key , md.getUnit(), val.intValue());	// We are working with an int
	 				}
		 		}
	 		} else { // md != null
	 			if (firstReport)											// Provide some feedback of available metrics for future reporting 
	 				logger.fine("Not reporting identified metric " + key); 			
	 		}
	 	}
	 	logger.fine("Reported to New Relic " + count + " metrics. " + getAgentInfo());
	}

	private String getAgentInfo() {
		return "Agent Name: " + name + ". Agent Version: " + version;
	}

	/**
	 * This method creates the metric meta data that is derived from the provided configuration
	 * and New Relic specific metrics.
	 */
	private void createMetaData() {
		
		Map<String,Object> categories = getMetricCategories(); 			// Get current Metric Categories
	 	Iterator<String> iter = categories.keySet().iterator();	
	 	while (iter.hasNext()) {
	 		String category = (String)iter.next();
			@SuppressWarnings("unchecked")
			Map<String, String> attributes = (Map<String,String>)categories.get(category);
			String valueMetrics = attributes.get("value_metrics");
			if (valueMetrics != null) {
				Set<String> metrics = new HashSet<String>(Arrays.asList(valueMetrics.toLowerCase().replaceAll(" ", "").split(MySQLAgent.COMMA)));
				for (String s: metrics) {
					addMetricMeta(category + MySQL.SEPARATOR + s, new MetricMeta(false));
				}

			}
			String counterMetrics = attributes.get("counter_metrics");
			if (counterMetrics != null) {
				Set<String> metrics = new HashSet<String>(Arrays.asList(counterMetrics.toLowerCase().replaceAll(" ", "").split(MySQLAgent.COMMA)));
				for (String s: metrics) {
					addMetricMeta(category + MySQL.SEPARATOR + s, new MetricMeta(true));
				}
			}
	 	}		
		
	 	/* Define New Relic specific metrics used for default dashboards */
		addMetricMeta("newrelic/volume_reads", 		new MetricMeta(true, "Queries/Second"));
		addMetricMeta("newrelic/volume_writes", 	new MetricMeta(true, "Queries/Second"));

		addMetricMeta("newrelic/bytes_reads", 		new MetricMeta(true, "Bytes/Second"));
		addMetricMeta("newrelic/bytes_writes", 		new MetricMeta(true, "Bytes/Second"));

		addMetricMeta("newrelic/connections_connected", new MetricMeta(false, "Connections"));
		addMetricMeta("newrelic/connections_running", 	new MetricMeta(false, "Connections"));
		addMetricMeta("newrelic/connections_cached", 	new MetricMeta(false, "Connections"));
	
		addMetricMeta("newrelic/innodb_bp_pages_created", 	new MetricMeta(true, "Pages/Second"));
		addMetricMeta("newrelic/innodb_bp_pages_read", 		new MetricMeta(true, "Pages/Second"));
		addMetricMeta("newrelic/innodb_bp_pages_written", 	new MetricMeta(true, "Pages/Second"));

		addMetricMeta("newrelic/query_cache_hits", 			new MetricMeta(true, "Queries/Seconds"));
		addMetricMeta("newrelic/query_cache_misses", 		new MetricMeta(true, "Queries/Seconds"));
		addMetricMeta("newrelic/query_cache_not_cached", 	new MetricMeta(true, "Queries/Seconds"));
	
		addMetricMeta("newrelic/replication_lag", 		new MetricMeta(false, "Seconds"));
		addMetricMeta("newrelic/replication_status", 	new MetricMeta(false, "State"));

		addMetricMeta("newrelic/pct_connection_utilization", 		new MetricMeta(false, "Percent"));
		addMetricMeta("newrelic/pct_innodb_buffer_pool_hit_ratio",	new MetricMeta(false, "Percent"));
		addMetricMeta("newrelic/pct_query_cache_hit_utilization", 	new MetricMeta(false, "Percent"));
		addMetricMeta("newrelic/pct_query_cache_memory_in_use", 	new MetricMeta(false, "Percent"));
		addMetricMeta("newrelic/pct_tmp_tables_written_to_disk", 	new MetricMeta(false, "Percent"));

		addMetricMeta("newrelic/innodb_fsyncs_data",   new MetricMeta(true, "Fsyncs/Second"));
		addMetricMeta("newrelic/innodb_fsyncs_os_log", new MetricMeta(true, "Fsyncs/Second"));

		addMetricMeta("newrelic/slave_relay_log_bytes",   new MetricMeta(true, "Bytes/Second"));
		addMetricMeta("newrelic/master_log_lag_bytes", new MetricMeta(true, "Bytes/Second"));

	 	/* Define improved metric values for certain general metrics */
		addMetricMeta("status/aborted_clients", 	new MetricMeta(true, "Connections/Second"));
		addMetricMeta("status/aborted_connects", 	new MetricMeta(true, "Connections/Second"));

		addMetricMeta("status/bytes_sent", 		new MetricMeta(true, "Bytes/Second"));
		addMetricMeta("status/bytes_received", 	new MetricMeta(true, "Bytes/Second"));
		
		addMetricMeta("status/com_select", 			new MetricMeta(true, "Selects/Second"));
		addMetricMeta("status/com_insert", 			new MetricMeta(true, "Inserts/Second"));
		addMetricMeta("status/com_insert_select", 	new MetricMeta(true, "Inserts/Second"));
		addMetricMeta("status/com_update", 			new MetricMeta(true, "Updates/Second"));
		addMetricMeta("status/com_update_multi",	new MetricMeta(true, "Updates/Second"));
		addMetricMeta("status/com_delete", 			new MetricMeta(true, "Deletes/Second"));
		addMetricMeta("status/com_delete_multi", 	new MetricMeta(true, "Deletes/Second"));
		addMetricMeta("status/com_replace", 		new MetricMeta(true, "Replaces/Second"));
		addMetricMeta("status/com_replace_select", 	new MetricMeta(true, "Replaces/Second"));

		addMetricMeta("status/slow_queries", 			new MetricMeta(true, "Queries/Second"));
		addMetricMeta("status/created_tmp_tables", 		new MetricMeta(true, "Queries/Second"));
		addMetricMeta("status/created_tmp_disk_tables", new MetricMeta(true, "Queries/Second"));

		addMetricMeta("status/innodb_buffer_pool_pages_flushed",new MetricMeta(true, "Pages/Second"));


		addMetricMeta("newrelic/innodb_buffer_pool_pages_clean",		new MetricMeta(false, "Pages"));
		addMetricMeta("newrelic/innodb_buffer_pool_pages_dirty",		new MetricMeta(false, "Pages"));
		addMetricMeta("newrelic/innodb_buffer_pool_pages_misc",			new MetricMeta(false, "Pages"));
		addMetricMeta("newrelic/innodb_buffer_pool_pages_free",			new MetricMeta(false, "Pages"));
		addMetricMeta("newrelic/innodb_buffer_pool_pages_unassigned",	new MetricMeta(false, "Pages"));

		addMetricMeta("status/innodb_data_fsyncs", 		new MetricMeta(true, "Fsyncs/Second"));
		addMetricMeta("status/innodb_os_log_fsyncs", 	new MetricMeta(true, "Fsyncs/Second"));

		addMetricMeta("status/innodb_os_log_written", 	new MetricMeta(true, "Bytes/Second"));

		/* Query Cache Units */
		addMetricMeta("status/qcache_free_blocks",      new MetricMeta(false, "Blocks"));
		addMetricMeta("status/qcache_free_memory",      new MetricMeta(false, "Bytes"));
		addMetricMeta("status/qcache_hits",             new MetricMeta(true,  "Queries/Second"));
		addMetricMeta("status/qcache_inserts",          new MetricMeta(true,  "Queries/Second"));
		addMetricMeta("status/qcache_lowmem_prunes",    new MetricMeta(true,  "Queries/Second"));
		addMetricMeta("status/qcache_not_cached",       new MetricMeta(true,  "Queries/Second"));
		addMetricMeta("status/qcache_queries_in_cache", new MetricMeta(false, "Queries"));
		addMetricMeta("status/qcache_total_blocks",     new MetricMeta(false, "Blocks"));
		
		addMetricMeta("innodb_status/history_list_length",  new MetricMeta(false, "Pages"));
		addMetricMeta("innodb_status/queries_inside_innodb",new MetricMeta(false, "Queries"));
		addMetricMeta("innodb_status/queries_in_queue",     new MetricMeta(false, "Queries"));
		addMetricMeta("innodb_status/checkpoint_age",		new MetricMeta(false, "Bytes"));

		
		addMetricMeta("master/position",    			 new MetricMeta(true, "Bytes/Second"));
		addMetricMeta("slave/relay_log_pos",    		 new MetricMeta(true, "Bytes/Second"));
	}

	/**
	 * Add the given metric meta information to the Map of all metric meta information for this agent
	 * 
	 * @param String key
	 * @param Metric mm
	 */
	private void addMetricMeta(String key, MetricMeta mm) {
		metricsMeta.put(key.toLowerCase(), mm); 		
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
		if (key.startsWith("innodb_mutex/") && !metricsMeta.containsKey(key)) {								// This is a catch all for dynamic name metrics
			addMetricMeta(key, new MetricMeta(true, "Operations/Second"));
		}
 		return (MetricMeta)metricsMeta.get(key.toLowerCase());				// Look for existing meta data on metric
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
