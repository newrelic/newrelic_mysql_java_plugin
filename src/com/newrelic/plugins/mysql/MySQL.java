package com.newrelic.plugins.mysql;

import static com.newrelic.plugins.mysql.util.Constants.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.metrics.publish.binding.Context;

/**
 * This class provide MySQL specific methods, operations and values for New Relic Agents reporting MySQL Metrics
 * 
 * @author Ronald Bradford me@ronaldbradford.com
 * 
 */
public class MySQL {

    private Connection conn = null; // Cached Database Connection
    private boolean connectionInitialized = false;

    public MySQL() {
    }

    /**
     * This method will return a new MySQL database connection
     * 
     * @param host String Hostname for MySQL Connection
     * @param user String Database username for MySQL Connection
     * @param passwd String database password for MySQL Connection
     * @return connection new MySQL Connection
     */
    private Connection getNewConnection(String host, String user, String passwd, String properties) {
        Connection newConn = null;
        String dbURL = buildString(JDBC_URL, host, SLASH, properties);
        String connectionInfo = buildString(dbURL, SPACE, user, PASSWORD_FILTERED);

        Context.log(Level.FINE, "Getting new MySQL Connection: ", connectionInfo);

        try {
            if (!connectionInitialized) {
                // load jdbc driver
                Class.forName("com.mysql.jdbc.Driver").newInstance();
                connectionInitialized = true;
            }
            newConn = DriverManager.getConnection(dbURL, user, passwd);
            if (newConn == null) {
                Context.log(Level.SEVERE, "Unable to obtain a new database connection: ", connectionInfo, ", check your MySQL configuration settings.");
            }
        } catch (Exception e) {
            Context.log(Level.SEVERE, "Unable to obtain a new database connection: ", connectionInfo, ", check your MySQL configuration settings. ",
                    e.getMessage());
        }
        return newConn;
    }

    /**
     * This method will return a MySQL database connection for use, either a new connection or a cached connection
     * 
     * @param host String Hostname
     * @param user String Database username
     * @param passwd String database password
     * @return A MySQL Database connection for use
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
     * Check if connection is valid by pinging MySQL server. If connection is null or invalid return false, otherwise true.
     * 
     * @return the state of the connection
     */
    private boolean isConnectionValid() {
        boolean available = false;
        if (conn != null) {
            Statement stmt = null;
            ResultSet rs = null;
            try {
                Context.log(Level.FINE, "Checking connection - pinging MySQL server");
                stmt = conn.createStatement();
                rs = stmt.executeQuery(PING);
                available = true;
            } catch (SQLException e) {
                Context.log(Level.FINE, "The MySQL connection is not available.");
                available = false;
            } finally {
                try {
                    if (stmt != null) {
                        stmt.close();
                    }
                    if (rs != null) {
                        rs.close();
                    }
                } catch (SQLException e) {
                    Context.log(Level.FINE, "Error closing statement/result set: ", e);
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
                Context.log(Level.FINE, "Error closing connection: ", e);
            }
        }
    }

    /**
     * 
     * This method will execute the given SQL Statement and produce a set of key/value pairs that are used for reporting metrics. This method is optimized for
     * queries designed to produce New Relic compatible type results
     * 
     * @param c Connection
     * @param SQL String of SQL Statement to execute
     * @return Map of key/value pairs
     */
    public static Map<String, Float> runSQL(Connection c, String category, String SQL, String type) {
        Statement stmt = null;
        ResultSet rs = null;
        Map<String, Float> results = new HashMap<String, Float>();

        try {
            Context.log(Level.FINE, "Running SQL Statement ", SQL);
            stmt = c.createStatement();
            rs = stmt.executeQuery(SQL); // Execute the given SQL statement
            ResultSetMetaData md = rs.getMetaData(); // Obtain Meta data about the SQL query (column names etc)

            if (ROW.equals(type)) { // If we expect a single row of results
                if (rs.next()) {
                    for (int i = 1; i <= md.getColumnCount(); i++) { // use column names as the "key"
                        String value = transformStringMetric(rs.getString(i));
                        String columnName = md.getColumnName(i).toLowerCase();
                        if (validMetricValue(value)) {
                            String key = buildString(category, SEPARATOR, columnName);
                            results.put(key, translateStringToNumber(value));
                        }
                        // rs.getString converts the string NULL into null
                        if (SECONDS_BEHIND_MASTER.equals(columnName)) {
                            if (value == null) {
                                String key = buildString(category, SEPARATOR, columnName);
                                results.put(key, -1.0f);
                            }
                        }
                    }
                }
            } else if (SET.equals(type)) { // This SQL statement return a key/value pair set of rows
                if (md.getColumnCount() < 2) {
                    return results; // If there are less than 2 columns, the resultset is incomplete
                }
                while (rs.next()) {
                    String value = transformStringMetric(rs.getString(2));
                    if (validMetricValue(value)) {
                        String key = buildString(category, SEPARATOR, rs.getString(1).toLowerCase());
                        results.put(key, translateStringToNumber(value));
                    }
                } // If there are more than 2 columns, disregard additional columns

            } else if (SPECIAL.equals(type)) { // These are per case bases SQL type commands with special needs
                if (SHOW_ENGINE_INNODB_MUTEX.equals(SQL)) {
                    results.putAll(processInnodbMutex(rs, category));
                } else if (SHOW_ENGINE_INNODB_STATUS.equals(SQL)) {
                    results.putAll(processInnoDBStatus(rs, category));
                }
            }
            return results;
        } catch (SQLException e) {
            Context.log(Level.SEVERE, "An SQL error occured running '", SQL, "' ", e.getMessage());
        } finally {
            try {
                if (rs != null) {
                    rs.close(); // Release objects
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                ;
            }
            rs = null;
            stmt = null;
        }
        return results;
    }

    /**
     * This method is special processing for the the SHOW ENGINE INNODB MUTEX output including - Discard first column - String conversion of names - extraction
     * of value from column - Aggregation of repeating name rows
     * 
     * 
     * @param ResultSet rs
     * @param String category
     * @return Map of metrics collated
     * @throws SQLException
     */
    private static Map<String, Float> processInnodbMutex(ResultSet rs, String category) throws SQLException {
        String mutex;
        Float value;
        Map<String, Float> mutexes = new HashMap<String, Float>();

        while (rs.next()) {
            mutex = buildString(category, SEPARATOR, rs.getString(2).replaceAll(INNODB_MUTEX_REGEX, EMPTY_STRING).replaceAll(ARROW, UNDERSCORE));
            value = translateStringToNumber(rs.getString(3).substring(rs.getString(3).indexOf(EQUALS) + 1));
            if (mutexes.containsKey(mutex)) {
                Context.log(Level.FINE, "appending ", value);
                value = value + mutexes.get(mutex);
            }
            mutexes.put(mutex, value);
        }
        Context.log(Level.FINE, "Mutexes: ", mutexes);

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
    public static Map<String, Float> processInnoDBStatus(ResultSet rs, String category) throws SQLException {
        if (!rs.next()) {
            return Collections.emptyMap();
        } else {
            return processInnoDBStatus(rs.getString(3), category);
        }
    }

    static Map<String, Float> processInnoDBStatus(String status, String category) {

        Set<String> lines = new HashSet<String>(Arrays.asList(status.split(NEW_LINE)));

        Map<String, Float> results = new HashMap<String, Float>();
        Float log_sequence_number = 0.0f, last_checkpoint = 0.0f;

        Context.log(Level.FINE, "Processing ", lines.size(), " of SHOW ENGINE INNODB STATUS");

        for (String s : lines) {
            if (s.startsWith(HISTORY_LIST_LENGTH)) {
                results.put(buildString(category, SEPARATOR, HISTORY_LIST_LENGTH_METRIC),
                        translateStringToNumber(s.substring(HISTORY_LIST_LENGTH.length() + 1)));
            } else if (s.startsWith(LOG_SEQUENCE_NUMBER)) {
                log_sequence_number = translateStringToNumber(s.substring(LOG_SEQUENCE_NUMBER.length() + 1));
                results.put(buildString(category, SEPARATOR, LOG_SEQUENCE_NUMBER_METRIC), log_sequence_number);
            } else if (s.startsWith(LAST_CHECKPOINT_AT)) {
                last_checkpoint = translateStringToNumber(s.substring(LAST_CHECKPOINT_AT.length() + 1));
                results.put(buildString(category, SEPARATOR, LAST_CHECKPOINT_METRIC), last_checkpoint);
            } else if (QUERIES_INSIDE_INNODB_REGEX_PATTERN.matcher(s).matches()) {
                results.put(buildString(category, SEPARATOR, QUERIES_INSIDE_INNODB_METRIC),
                        translateStringToNumber(s.replaceAll(QUERIES_INSIDE_INNODB_REGEX2, EMPTY_STRING)));
                results.put(buildString(category, SEPARATOR, QUERIES_IN_QUEUE), translateStringToNumber(s.replaceAll(QUERIES_IN_QUEUE_REGEX, EMPTY_STRING)
                        .replaceAll(QUERIES_IN_QUEUE_REGEX2, EMPTY_STRING)));
            }
        }
        results.put(buildString(category, SEPARATOR, CHECKPOINT_AGE_METRIC), log_sequence_number - last_checkpoint);

        Context.log(Level.FINE, results);

        return results;
    }

    /**
     * This method will convert the provided string into a Number (either int or float)
     * 
     * @param String value to convert
     * @return Number A int or float representation of the provided string
     */
    public static Float translateStringToNumber(String val) {
        try {
            if (val.contains(SPACE)) {
                val = SPACE_PATTERN.matcher(val).replaceAll(EMPTY_STRING); // Strip any spaces
            }
            return Float.parseFloat(val);
        } catch (Exception e) {
            Context.log(Level.SEVERE, "Unable to parse int/float number from value ", val);
        }
        return 0.0f;
    }

    /**
     * Perform some preliminary transformation of string values that can be represented in integer values for monitoring
     * 
     * @param val String value to evaluate
     * @return String value that best represents and integer
     */
    static String transformStringMetric(String val) {
        if (ON.equalsIgnoreCase(val) || TRUE.equalsIgnoreCase(val)) return ONE; // Convert some TEXT metrics into numerics
        if (OFF.equalsIgnoreCase(val) || NONE.equalsIgnoreCase(val)) return ZERO;
        if (YES.equalsIgnoreCase(val)) return ONE; // For slave/slave_*_running
        if (NO.equalsIgnoreCase(val)) return ZERO; // For slave/slave_*_running
        if (NULL.equalsIgnoreCase(val)) return NEG_ONE; // For slave/seconds_behind_master
        return val;
    }

    /**
     * Check if the value is a valid New Relic Metric value
     * 
     * @param val String to validate
     * @return TRUE if string is a numeric supported by New Relic
     */
    static boolean validMetricValue(String val) {
        if (val == null || EMPTY_STRING.equals(val)) {
            return false;
        }
        if (VALID_METRIC_PATTERN.matcher(val).matches()) {
            return true;
        }
        return false;
    }

    static String buildString(String... strings) {
        StringBuilder builder = new StringBuilder(50);
        for (String string : strings) {
            builder.append(string);
        }
        return builder.toString();
    }
}
