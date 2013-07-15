package com.newrelic.plugins.mysql;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This class provide MySQL specific methods, operations and values for New Relic Agents
 * reporting MySQL Metrics
 * 
 * @author Ronald Bradford  me@ronaldbradford.com
 *
 */
public class MySQL {

	public static final String SEPARATOR = "/";
	private static final String PING = "/* ping */ SELECT 1";
	private static final Pattern VALID_METRIC_PATTERN = Pattern.compile("(-)?(\\.)?\\d+(\\.\\d+)?");  // Only integers and floats are valid metric values
	
	private static Logger logger = Logger.getAnonymousLogger();			// Local convenience variable
	private  Connection conn = null;									// Cached Database Connection
	private boolean connectionInitialized = false;

	public MySQL() {
	}

	/**
     * This method will return a new MySQL database connection  
     *  
     * @param host  String Hostname for MySQL Connection
     * @param user  String Database username for MySQL Connection
     * @param passwd String database password for MySQL Connection
     * @return connection new MySQL Connection
     */
	 private Connection getNewConnection(String host, String user, String passwd, String properties) {
	    Connection newConn = null; 
	    String dbURL="jdbc:mysql://" + host + "/" + properties;
	    String connectionInfo = dbURL + " " + user + "/PASSWORD_FILTERED";
			 
		logger.fine("Getting new MySQL Connection: " + connectionInfo);
		try {
		    if (!connectionInitialized) {
		        // load jdbc driver
		        Class.forName("com.mysql.jdbc.Driver").newInstance();
		        connectionInitialized = true;
		    }
		    newConn = DriverManager.getConnection(dbURL, user, passwd);
		    if (newConn == null) {
		        logger.severe("Unable to obtain a new database connection: " + connectionInfo + ", check your MySQL configuration settings.");
		    }
		} catch (Exception e) {
			logger.severe("Unable to obtain a new database connection: " + connectionInfo + ", check your MySQL configuration settings. " + e.getMessage());
		}
		return newConn;
	}

	/**
	 * This method will return a MySQL database connection for use, either a new connection
	 * or a cached connection
	 * 
     * @param host  String Hostname
     * @param user  String Database username
     * @param passwd String database password
	 * @return  A MySQL Database connection for use
	 */
	public Connection getConnection(String host, String user, String passwd, String properties) {
		if (conn == null) {
			conn = getNewConnection(host, user, passwd, properties);
		}
		// Test Connection, and reconnect if necessary
		else if (!isConnectionValid()) {
		    closeConnection();
		    conn = getNewConnection(host, user, passwd, properties);
		}
		return conn;
	}
	
	/**
	 * Check if connection is valid by pinging MySQL server.
	 * If connection is null or invalid return false, otherwise true.
	 * @return the state of the connection
	 */
	private boolean isConnectionValid() {
	    boolean available = false;
	    if (conn != null) {
    	    Statement stmt = null;
    	    ResultSet rs = null;
    	    try {
    	        logger.fine("Checking connection - pinging MySQL server");
                stmt = conn.createStatement();
                rs = stmt.executeQuery(PING);
                available = true;
            } catch (SQLException e) {
                logger.fine("The MySQL connection is not available.");
                available = false;
            } finally {
                try {
                    if (stmt != null) stmt.close();
                    if (rs != null) rs.close();
                } catch (SQLException e) {
                    logger.fine("Error closing statement/result set: " + e);
                }
                rs = null;
                stmt = null;
            }
	    }
	    return available;
	}
	
	/**
	 * Close current connection
	 */
	private void closeConnection() {
	    if (conn != null) {
	        try {
                conn.close();
                conn = null;
            } catch (SQLException e) {
                logger.fine("Error closing connection: " + e);
            }
	    }
	}

	/**
	 * 
	 * This method will execute the given SQL Statement and produce a set of
	 * key/value pairs that are used for reporting metrics.
	 * This method is optimized for queries designed to produce New Relic
	 * compatible type results
	 * 
	 * @param c Connection
	 * @param SQL String of SQL Statement to execute
	 * @return Map of key/value pairs
	 */
    public static Map<String,Number> runSQL(Connection c, String category, String SQL, String type) {
    	Statement stmt = null;
    	ResultSet rs = null;
    	Map<String, Number> results = new HashMap<String,Number>();


    	try {
	    	logger.fine("Running SQL Statement " + SQL);
			stmt = c.createStatement();
            rs = stmt.executeQuery(SQL);								// Execute the given SQL statement
            ResultSetMetaData md = rs.getMetaData();					// Obtain Meta data about the SQL query (column names etc)

            if ("row".equals(type)) {									// If we expect a single row of results
            	if (rs.next()) {
            		for (int i=1; i <= md.getColumnCount();i++) {		// use column names as the "key"
            			if (validMetricValue(rs.getString(i)))
            				results.put(category + SEPARATOR + md.getColumnName(i).toLowerCase(),
            							translateStringToNumber(transformStringMetric(rs.getString(i))));
            			// rs.getString converts the string NULL into null
            			if ("seconds_behind_master".equals(md.getColumnName(i).toLowerCase())) {
            				if (rs.getString(i) == null) {
            				results.put(category + SEPARATOR + md.getColumnName(i).toLowerCase(),
        							translateStringToNumber(transformStringMetric("NULL")));
            				}
            			}
            		}
            	}
            } else if ("set".equals(type)) {				            // This SQL statement return a key/value pair set of rows
	            if (md.getColumnCount() < 2) return results;			// If there are less than 2 columns, the resultset is incomplete
	            while (rs.next()) {
	            	if (validMetricValue(rs.getString(2)))
	            		results.put(category + SEPARATOR + rs.getString(1).toLowerCase(),
	            					translateStringToNumber(transformStringMetric(rs.getString(2))));
	            } 														// If there are more than 2 columns, disregard additional columns
		           
            } else if ("special".equals(type)) {						// These are per case bases SQL type commands with special needs
             	if ("SHOW ENGINE INNODB MUTEX".equals(SQL)) {
             		results.putAll(processInnodbMutex(rs, category)); 														
            	} else if ("SHOW ENGINE INNODB STATUS".equals(SQL)) {
            		results.putAll(processInnoDBStatus(rs, category));
            	}
            }
            return results;
		} catch (SQLException e) {
			logger.severe("An SQL error occured running '" + SQL + "' " + e.getMessage());
		} finally {
			try {
				if (rs != null) rs.close();								// Release objects
				if (stmt != null) stmt.close();	
			} catch (SQLException e) {
				;
			}
			rs = null;
			stmt = null;
        }
    	return results;
    }

    /**
     * This method is special processing for the the SHOW ENGINE INNODB MUTEX output including
     * - Discard first column
     * - String conversion of names
     * - extraction of value from column
     * - Aggregation of repeating name rows
     * 
     * 
     * @param ResultSet rs 
     * @param String category
     * @return Map of metrics collated
     * @throws SQLException
     */
	private static Map<String, Number> processInnodbMutex(ResultSet rs, String category) throws SQLException {
		String mutex;
		Number value;
    	Map<String, Number> mutexes = new HashMap<String,Number>();

		while (rs.next()) {
			mutex=category + SEPARATOR + rs.getString(2).replaceAll("[&\\[\\]]", "").replaceAll("->", "_");
			value=translateStringToNumber(rs.getString(3).substring(rs.getString(3).indexOf("=") +1));
			if (mutexes.containsKey(mutex)) {
				logger.fine("appending " + value);
				value = value.intValue() + mutexes.get(mutex).intValue();
			}
			mutexes.put(mutex, value);
		}
		logger.fine(mutexes.toString());
		 
		return mutexes;
	}

	/**
	 * This method is special processing for the SHOW ENGINE INNODB STATUS output as this is one blob of text
	 * 
	 * @param ResultSet rs
	 * @param String category
	 * @return Map of metrics collected
	 * @throws SQLException
	 */
	public static Map<String, Number> processInnoDBStatus(ResultSet rs, String category) throws SQLException {
	   	Map<String, Number> results = new HashMap<String,Number>();
	   	String history="history list length";
	   	String LSN = "log sequence number";
	   	String checkpoint = "last checkpoint at ";
	   	Number log_sequence_number = 0, last_checkpoint = 0;

	   	if (!rs.next()) return results;
		String status=rs.getString(3);
		Set<String> lines = new HashSet<String>(Arrays.asList(status.toLowerCase().split("\n")));
		logger.fine("Processing " + lines.size() + " of SHOW ENGINE INNODB STATUS");

		for (String s: lines) {
			if (s.startsWith(history)) {
				results.put(category + SEPARATOR + "history_list_length", translateStringToNumber(s.substring(history.length()+1)));
			}
			if (s.startsWith(LSN)) {
				log_sequence_number=translateStringToNumber(s.substring(LSN.length()+1));
				results.put(category + SEPARATOR + "log_sequence_number", log_sequence_number);
			}
			if (s.startsWith(checkpoint)) {
				last_checkpoint = translateStringToNumber(s.substring(checkpoint.length()+1));
				results.put(category + SEPARATOR + "last_checkpoint", last_checkpoint);
			}
			if (s.matches(".* queries inside innodb.*")) {
				results.put(category + SEPARATOR + "queries_inside_innodb", translateStringToNumber(s.replaceAll(" queries inside innodb.*", "")));
				results.put(category + SEPARATOR + "queries_in_queue", translateStringToNumber(s.replaceAll(".* queries inside innodb, ", "").replaceAll(" queries in queue","")));
			}
		}
		results.put(category + SEPARATOR + "checkpoint_age", log_sequence_number.intValue() - last_checkpoint.intValue());
		
		logger.fine(results.toString());
		return results;
	}

 	/**
	 * This method will convert the provided string into a Number (either int or float)
	 * 
	 * @param String value to convert
	 * @return Number A int or float representation of the provided string
	 */
	public static Number translateStringToNumber(String val) {
		try {
			val = val.replaceAll(" ", "");									// Strip any spaces
			if (val.matches("\\d*\\.\\d*")) {							// We are working with a float value
				return (float)Float.parseFloat(val);
			 } else {
				return new BigInteger(val);
			 }
		} catch (Exception e) {
 			logger.severe("Unable to parse int/float number from value " + val);
 		}
		return 0;
	}

 	/**
     * Perform some preliminary transformation of string values that can be 
     * represented in integer values for monitoring
     * 
     * @param val String value to evaluate
     * @return String value that best represents and integer 
     */
    static String transformStringMetric(String val) {
    	val = val.toUpperCase(); 
		if ("ON".equals(val)  || "TRUE".equals(val)) return "1";		// Convert some TEXT metrics into numerics
		if ("OFF".equals(val) || "NONE".equals(val)) return "0";
		if ("YES".equals(val))  return "1";								// For slave/slave_*_running
		if ("NO".equals(val))   return "0";								// For slave/slave_*_running
		if ("NULL".equals(val)) return "-1";							// For slave/seconds_behind_master
		return val;
     }

 	/**
 	 * Check if the value is a valid New Relic Metric value
 	 * 
 	 * @param val String to validate
 	 * @return TRUE if string is a numeric supported by New Relic
 	 */
     static boolean validMetricValue(String val) {
     	if (val == null || "".equals(val)) 								//  Empty string values are invalid
     		return false;
        if (VALID_METRIC_PATTERN.matcher(transformStringMetric(val)).matches()) 			//  We can only process numerical metrics
        	return true;	
   		return false;
 	}

    /**
     * Set the System Logger for this class
     * 
     * @param Logger 
     */
	public static void setLogger(Logger _logger) {
		logger = _logger;
	}
}
