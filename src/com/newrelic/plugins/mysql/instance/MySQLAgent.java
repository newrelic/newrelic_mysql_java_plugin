package com.newrelic.plugins.mysql.instance;

import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.newrelic.data.in.Agent;
import com.newrelic.data.in.binding.Context;
import com.newrelic.data.in.processors.EpochCounter;
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
	final static String GUID = "com.newrelic.plugins.mysql.instance";
	final static String version = "0.1.0";
	
	Logger logger;														// Local convenience variable
	private String name;
	private String host;
	private String user;
	private String passwd;
	private String metrics;
 	private Map<String, MetricMeta> metricsMeta;						// Definition of MySQL meta data (counter, unit, type etc)

	public MySQLAgent(String name, String host, String user, String passwd, String metrics) {
	   	super(GUID, version);
	   	this.name = name;
	   	this.host = host;
	   	this.user = user;
	   	this.passwd = passwd;
	   	this.metrics = metrics.toLowerCase();
	   	
	   	logger = Context.getLogger();									// Set logging to current Context
	   	MySQL.setLogger(logger);										// Push logger to MySQL context
	   	setMetricsMeta(createMetaData());								// Define incremental counters that are value/sec etc
	}
	
	/**
	 * 
	 */
	public void pollCycle() {
		Connection c = MySQL.getConnection(host, user, passwd);			// Get a database connection (which should be cached)
		if (c == null) return;											// Unable to continue without a valid database connection
	 	
		Map<String,String> results = gatherMetrics(c, metrics);			// Gather defined metrics 
		reportMetrics(results);											// Report Metrics to New Relic
	}

	/**
	 * 
	 * @param c
	 * @return
	 */
	private Map<String, String> gatherMetrics(Connection c, String metrics) {
	 	Map<String,String> results = new HashMap<String,String>();		// Create an empty set of results
	 	
	 	Map<String,String> SQL = new HashMap<String,String>();
	 	SQL.put("status", "SHOW GLOBAL STATUS LIKE 'bytes%'");
	 	SQL.put("slave",  "SHOW SLAVE STATUS");
	 	SQL.put("master", "SHOW MASTER STATUS");
	 	SQL.put("innodb", "SHOW ENGINE INNODB STATUS");
	 	SQL.put("mutex",  "SHOW ENGINE INNODB MUTEX");
	 	

	 	Iterator<String> iter = SQL.keySet().iterator();			
	 	while (iter.hasNext()) {
	 		String category = (String)iter.next();
	 		if (metrics.contains(category)) 
	 			results.putAll(MySQL.runSQL(c, category, SQL.get(category)));
	 	}

 		return results;
	}

	/**
	 * 
	 * @param results
	 */
	public void reportMetrics(Map<String,String> results) { 
	 	logger.info("Reporting " + results.size() + " metrics");
	 	logger.finest(results.toString());
	 	int i=0;
	 	Iterator<String> iter = results.keySet().iterator();			
	 	while (iter.hasNext()) {										// Iterate over current metrics	
	 		String key = (String)iter.next().toLowerCase();
	 		String val = (String)results.get(key);
	 		MetricMeta md = getMetricMeta(key);
	 		logger.fine("Metric " + ++i + " " + key + ":" + val);
	 		if (MetricMeta.FLOAT_TYPE.equals(md.getType())) {			// We are working with a float value
	 			try {
	 				float floatval = (float)Float.parseFloat(results.get(key));
	 				reportMetric(key, "value", floatval); 
	 			} catch (Exception e) {
	 				logger.warning("Unable to parse float value " + val + " for " + key);
	 			}
	 		} else {													// We are working with an integer value
	 			try {
	 				int intval = (int)Integer.parseInt(results.get(key));
	 				if (md.isCounter()) {
	 					float floatval = md.getCounter().process(intval).floatValue();
	 					reportMetric(key , "value/sec", floatval);
	 				} else {
	 					reportMetric(key , "value", intval);
	 				}
	 			} catch (Exception e) {
	 				logger.warning("Unable to parse int value " + val + " for " + key);
	 			}
	 		}
	 	}
	}

	private Map<String, MetricMeta> createMetaData() {
		Map<String,MetricMeta> c = new HashMap<String,MetricMeta>();
	
		c.put("bytes_received", new MetricMeta(true, "bytes/sec"));
		c.put("bytes_sent", new MetricMeta(true, "bytes/sec"));
		return c;
	}
  
	@Override
	public String getComponentHumanLabel() {
		return name;
	}

	/**
	 * This provides a lazy instantiation of a MySQL metric where no meta data was defined
	 * and means new metrics can be captured automatically.
	 * 
	 * A default metric is a integer value
	 * 
	 * @param key
	 * @return
	 */
	private MetricMeta getMetricMeta(String key) {
 		MetricMeta md = (MetricMeta)metricsMeta.get(key);
 		if (md == null) {
 			metricsMeta.put(key, MetricMeta.defaultMetricMeta());
 	 		md = (MetricMeta)metricsMeta.get(key);
 		}
 		return md;
	}

	private void setMetricsMeta(Map<String, MetricMeta> mm) {
		this.metricsMeta = mm;
		
	}
}
