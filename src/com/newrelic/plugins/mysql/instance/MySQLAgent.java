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
import com.newrelic.plugins.mysql.MySQL;


public class MySQLAgent extends Agent {
	final static String GUID = "com.newrelic.plugins.mysql.instance";
	final static String version = "0.1.0";
	
	Logger logger;														// Local convenience variable
	private String name;
	private String host;
	private String user;
	private String passwd;
	private String metrics;
 	private Map<String,EpochCounter> counters = 						// Definition of MySQL metrics that
 						new HashMap<String,EpochCounter>();				// are incremental counters

	public MySQLAgent(String name, String host, String user, String passwd, String metrics) {
	   	super(GUID, version);
	   	this.name = name;
	   	this.host = host;
	   	this.user = user;
	   	this.passwd = passwd;
	   	this.metrics = metrics;
	   	
	   	logger = Context.getLogger();									// Set logging to current Context
	   	MySQL.setLogger(logger);										// Push logger to MySQL context
	   	setCounters(createCounters());									// Define incremental counters that are value/sec
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

	 	// TODO: This is where we introduce additional commands based on values of metrics
	 	results.putAll(MySQL.runSQL(c, "status", "SHOW GLOBAL STATUS LIKE 'bytes%'")); 

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
	 	Iterator<String> iter = results.keySet().iterator();			// Iterate over current metrics
	 	while (iter.hasNext()) {
	 		String key = (String)iter.next();
	 		String val = (String)results.get(key);
	 		logger.fine("Metric " + ++i + " " + key + ":" + val);
	 		if (val.matches("\\.")) {
	 			try {
	 				float floatval = (float)Float.parseFloat(results.get(key));
	 				reportMetric(key.toLowerCase(), "value", floatval); 
	 			} catch (Exception e) {
	 				logger.warning("Unable to parse float value " + val + " for " + key);
	 			}
	 		} else {
	 			try {
	 				int intval = (int)Integer.parseInt(results.get(key));
	 				if (getCounters().containsKey(key)) {
	 					float floatval = getCounters().get(key).process(intval).floatValue();
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


 
  	private Map<String, EpochCounter> createCounters() {
		Map<String,EpochCounter> c = new HashMap<String,EpochCounter>();
		Set<String> s = new HashSet<String>();

		// Quick Hack
		s.add("bytes_received");
		s.add("bytes_sent");
		
	 	Iterator<String> iter = s.iterator();
	 	while (iter.hasNext()) {
	 		c.put((String)iter.next(), new EpochCounter());
	 	}
		
		return c;
	}


	@Override
	public String getComponentHumanLabel() {
		return name;
	}
	public Map<String,EpochCounter> getCounters() {
		return counters;
	}
	
	public void setCounters(Map<String,EpochCounter> counters) {
		this.counters = counters;
	}
}
