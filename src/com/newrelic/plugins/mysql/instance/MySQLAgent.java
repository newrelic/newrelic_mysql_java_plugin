package com.newrelic.plugins.mysql.instance;

import static com.newrelic.plugins.mysql.util.Constants.*;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.util.Logger;
import com.newrelic.plugins.mysql.MetricMeta;
import com.newrelic.plugins.mysql.MySQL;

/**
 * This class creates a specific MySQL agent that is used to obtain a MySQL database connection, 
 * gather requested metrics and report to New Relic
 * 
 * @author Ronald Bradford me@ronaldbradford.com
 * 
 */
public class MySQLAgent extends Agent {
    
    private static final Logger logger = Logger.getLogger(MySQLAgent.class);
    
    private static final String GUID = "com.newrelic.plugins.mysql.instance";
    private static final String version = "2.0.0";

    public static final String AGENT_DEFAULT_HOST = "localhost"; // Default values for MySQL Agent
    public static final String AGENT_DEFAULT_USER = "newrelic";
    public static final String AGENT_DEFAULT_PASSWD = "f63c225f4abe9e13";
    public static final String AGENT_DEFAULT_PROPERTIES = "";
    public static final String AGENT_DEFAULT_METRICS = "status,newrelic";

    private final String name; // Agent Name

    private final String host; // MySQL Connection parameters
    private final String user;
    private final String passwd;
    private final String properties;
    private String agentInfo;

    private final Set<String> metrics;
    // Definition of MySQL meta data (counter, unit, type etc)
    private final Map<String, MetricMeta> metricsMeta = new HashMap<String, MetricMeta>();
    // Definition of categories of metrics
    private Map<String, Object> metricCategories = new HashMap<String, Object>();

    private final MySQL m; // Per agent MySQL Object

    private boolean firstReport = true;

    /**
     * Default constructor to create a new MySQL Agent
     * 
     * @param map
     * 
     * @param String Human name for Agent
     * @param String MySQL Instance host:port
     * @param String MySQL user
     * @param String MySQL user password
     * @param String CSVm List of metrics to be monitored
     */
    public MySQLAgent(String name, String host, String user, String passwd, String properties, Set<String> metrics, Map<String, Object> metricCategories) {
        super(GUID, version);

        this.name = name; // Set local attributes for new class object
        this.host = host;
        this.user = user;
        this.passwd = passwd;
        this.properties = properties;

        this.metrics = metrics;
        this.metricCategories = metricCategories;

        this.m = new MySQL();

        createMetaData(); // Define incremental counters that are value/sec etc

        logger.debug("MySQL Agent initialized: ", formatAgentParams(name, host, user, properties, metrics));
    }

    /**
     * Format Agent parameters for logging
     * 
     * @param name
     * @param host
     * @param user
     * @param properties
     * @param metrics
     * @return A formatted String representing the Agent parameters
     */
    private String formatAgentParams(String name, String host, String user, String properties, Set<String> metrics) {
        StringBuilder builder = new StringBuilder();
        builder.append("name: ").append(name).append(" | ");
        builder.append("host: ").append(host).append(" | ");
        builder.append("user: ").append(user).append(" | ");
        builder.append("properties: ").append(properties).append(" | ");
        builder.append("metrics: ").append(metrics).append(" | ");
        return builder.toString();
    }

    /**
     * This method is run for every poll cycle of the Agent. Get a MySQL Database connection and gather metrics.
     */
    @Override
    public void pollCycle() {
        Connection c = m.getConnection(host, user, passwd, properties); // Get a database connection (which should be cached)
        if (c == null) {
            return; // Unable to continue without a valid database connection
        }

        logger.debug("Gathering MySQL metrics. ", getAgentInfo());

        Map<String, Float> results = gatherMetrics(c); // Gather defined metrics
        reportMetrics(results); // Report Metrics to New Relic
        firstReport = false;
    }

    /**
     * This method runs the various categories of MySQL statements and gathers the metrics that can be reported
     * 
     * @param Connection c MySQL Database Connection
     * @param String List of metrics to be obtained for this agent
     * @return Map of metrics and values
     */
    private Map<String, Float> gatherMetrics(Connection c) {
        Map<String, Float> results = new HashMap<String, Float>(); // Create an empty set of results
        Map<String, Object> categories = getMetricCategories(); // Get current Metric Categories

        Iterator<String> iter = categories.keySet().iterator();
        while (iter.hasNext()) {
            String category = iter.next();
            @SuppressWarnings("unchecked")
            Map<String, String> attributes = (Map<String, String>) categories.get(category);
            if (isReportingForCategory(category)) {
                results.putAll(MySQL.runSQL(c, category, attributes.get(SQL), attributes.get(RESULT)));
            }
        }
        results.putAll(newRelicMetrics(results));
        return results;
    }

    /**
     * This method creates a number of custom New Relic Metrics, that are derived from raw MySQL status metrics
     * 
     * @param Map existing Gathered MySQL metrics
     * @param metrics String of the Metric Categories to capture
     * @return Map Additional derived metrics
     */
    protected Map<String, Float> newRelicMetrics(Map<String, Float> existing) {
        Map<String, Float> derived = new HashMap<String, Float>();

        if (!isReportingForCategory(NEW_RELIC_CATEGORY)) {
            return derived; // Only calculate newrelic category if specified.
        }
        if (!isReportingForCategory(STATUS_CATEGORY)) {
            return derived; // "status" category is a pre-requisite for newrelic metrics
        }

        logger.debug("Adding New Relic derived metrics");

        /* read and write volume */
        if (areRequiredMetricsPresent("Reads", existing, "status/com_select", "status/qcache_hits")) {
            derived.put("newrelic/volume_reads", existing.get("status/com_select") + existing.get("status/qcache_hits"));
        }

        if (areRequiredMetricsPresent("Writes", existing, "status/com_insert", "status/com_update", "status/com_delete", "status/com_replace",
                "status/com_insert_select", "status/com_update_multi", "status/com_delete_multi", "status/com_replace_select")) {
            derived.put("newrelic/volume_writes", existing.get("status/com_insert") + existing.get("status/com_insert_select")
                    + existing.get("status/com_update") + existing.get("status/com_update_multi")
                    + existing.get("status/com_delete") + existing.get("status/com_delete_multi")
                    + existing.get("status/com_replace") + existing.get("status/com_replace_select"));
        }

        /* read and write throughput */
        if (areRequiredMetricsPresent("Read Throughput", existing, "status/bytes_sent")) {
            derived.put("newrelic/bytes_reads", existing.get("status/bytes_sent"));
        }

        if (areRequiredMetricsPresent("Write Throughput", existing, "status/bytes_received")) {
            derived.put("newrelic/bytes_writes", existing.get("status/bytes_received"));
        }

        /* Connection management */
        if (areRequiredMetricsPresent("Connection Management", existing, "status/threads_connected", "status/threads_running", "status/threads_cached")) {
            Float threads_connected = existing.get("status/threads_connected");
            Float threads_running = existing.get("status/threads_running");

            derived.put("newrelic/connections_connected", threads_connected);
            derived.put("newrelic/connections_running", threads_running);
            derived.put("newrelic/connections_cached", existing.get("status/threads_cached"));
            
            Float pct_connection_utilization = 0.0f;
            if (threads_connected > 0) {
                pct_connection_utilization = (threads_running / threads_connected) * 100.0f;
            }
            derived.put("newrelic/pct_connection_utilization", pct_connection_utilization);
        }

        /* InnoDB Metrics */
        if (areRequiredMetricsPresent("InnoDB", existing, "status/innodb_pages_created", "status/innodb_pages_read", "status/innodb_pages_written",
                "status/innodb_buffer_pool_read_requests", "status/innodb_buffer_pool_reads", "status/innodb_data_fsyncs", "status/innodb_os_log_fsyncs")) {
            derived.put("newrelic/innodb_bp_pages_created", existing.get("status/innodb_pages_created"));
            derived.put("newrelic/innodb_bp_pages_read", existing.get("status/innodb_pages_read"));
            derived.put("newrelic/innodb_bp_pages_written", existing.get("status/innodb_pages_written"));

            /* Innodb Specific Metrics */
            Float innodb_read_requests = existing.get("status/innodb_buffer_pool_read_requests");
            Float innodb_reads = existing.get("status/innodb_buffer_pool_reads");
            
            Float pct_innodb_buffer_pool_hit_ratio = 0.0f;
            if (innodb_read_requests + innodb_reads > 0) {
                pct_innodb_buffer_pool_hit_ratio = (innodb_read_requests / (innodb_read_requests + innodb_reads)) * 100.0f;
            }
            
            derived.put("newrelic/pct_innodb_buffer_pool_hit_ratio", pct_innodb_buffer_pool_hit_ratio);
            derived.put("newrelic/innodb_fsyncs_data", existing.get("status/innodb_data_fsyncs"));
            derived.put("newrelic/innodb_fsyncs_os_log", existing.get("status/innodb_os_log_fsyncs"));
        }

        /* InnoDB Buffer Metrics */
        if (areRequiredMetricsPresent("InnoDB Buffers", existing, "status/innodb_buffer_pool_pages_total", "status/innodb_buffer_pool_pages_data",
                "status/innodb_buffer_pool_pages_misc", "status/innodb_buffer_pool_pages_dirty", "status/innodb_buffer_pool_pages_free")) {
            Float pages_total = existing.get("status/innodb_buffer_pool_pages_total");
            Float pages_data = existing.get("status/innodb_buffer_pool_pages_data");
            Float pages_misc = existing.get("status/innodb_buffer_pool_pages_misc");
            Float pages_dirty = existing.get("status/innodb_buffer_pool_pages_dirty");
            Float pages_free = existing.get("status/innodb_buffer_pool_pages_free");

            derived.put("newrelic/innodb_buffer_pool_pages_clean", pages_data - pages_dirty);
            derived.put("newrelic/innodb_buffer_pool_pages_dirty", pages_dirty);
            derived.put("newrelic/innodb_buffer_pool_pages_misc", pages_misc);
            derived.put("newrelic/innodb_buffer_pool_pages_free", pages_free);
            derived.put("newrelic/innodb_buffer_pool_pages_unassigned", pages_total - pages_data - pages_free - pages_misc);
        }

        /* Query Cache */
        if (areRequiredMetricsPresent("Query Cache", existing, "status/qcache_hits", "status/com_select", "status/qcache_free_blocks",
                "status/qcache_total_blocks", "status/qcache_inserts", "status/qcache_not_cached")) {
            Float qc_hits = existing.get("status/qcache_hits");
            Float reads = existing.get("status/com_select");
            Float free = existing.get("status/qcache_free_blocks");
            Float total = existing.get("status/qcache_total_blocks");

            derived.put("newrelic/query_cache_hits", qc_hits);
            derived.put("newrelic/query_cache_misses", existing.get("status/qcache_inserts"));
            derived.put("newrelic/query_cache_not_cached", existing.get("status/qcache_not_cached"));

            Float pct_query_cache_hit_utilization = 0.0f;
            if (qc_hits + reads > 0) {
                pct_query_cache_hit_utilization = (qc_hits / (qc_hits + reads)) * 100.0f;
            }
            
            derived.put("newrelic/pct_query_cache_hit_utilization", pct_query_cache_hit_utilization);
            
            Float pct_query_cache_memory_in_use = 0.0f;
            if (total > 0) {
                pct_query_cache_memory_in_use = 100.0f - ((free / total) * 100.0f);
            }
            derived.put("newrelic/pct_query_cache_memory_in_use", pct_query_cache_memory_in_use);
        }

        /* Temp Table */
        if (areRequiredMetricsPresent("Temp Tables", existing, "status/created_tmp_tables", "status/created_tmp_disk_tables")) {
            Float tmp_tables = existing.get("status/created_tmp_tables");
            Float tmp_tables_disk = existing.get("status/created_tmp_disk_tables");

            Float pct_tmp_tables_written_to_disk = 0.0f;
            if (tmp_tables > 0) {
                pct_tmp_tables_written_to_disk = (tmp_tables_disk / tmp_tables) * 100.0f;
            }
            derived.put("newrelic/pct_tmp_tables_written_to_disk", pct_tmp_tables_written_to_disk);
        }

        /* Replication specifics */
        // "slave" category is a pre-requisite for these metrics
        if (isReportingForCategory("slave")) {
            if (areRequiredMetricsPresent("newrelic/replication_lag", existing, "slave/seconds_behind_master")) {
                derived.put("newrelic/replication_lag", existing.get("slave/seconds_behind_master"));
            }

            if (areRequiredMetricsPresent("newrelic/replication_status", existing, "slave/slave_io_running", "slave/slave_sql_running")) {
                int slave_io_thread_running = existing.get("slave/slave_io_running").intValue();
                int slave_sql_thread_running = existing.get("slave/slave_sql_running").intValue();

                /* both need to be YES, which is 1 */
                Float replication_status = 1.0f; // Default as in ERROR
                if (slave_io_thread_running + slave_sql_thread_running == 2) {
                    replication_status = 0.0f;
                }

                derived.put("newrelic/replication_status", replication_status);
            }

            if (areRequiredMetricsPresent("newrelic/slave_relay_log_bytes", existing, "slave/relay_log_pos")) {
                derived.put("newrelic/slave_relay_log_bytes", existing.get("slave/relay_log_pos"));
            }

            if (areRequiredMetricsPresent("newrelic/master_log_lag_bytes", existing, "slave/read_master_log_pos", "slave/exec_master_log_pos")) {
                derived.put("newrelic/master_log_lag_bytes", existing.get("slave/read_master_log_pos")
                        - existing.get("slave/exec_master_log_pos"));
            }
        } else {// This is a hack because the NR UI can't handle it missing for graphs
            derived.put("newrelic/replication_lag", 0.0f);
            derived.put("newrelic/replication_status", 0.0f);
            derived.put("newrelic/slave_relay_log_bytes", 0.0f);
            derived.put("newrelic/master_log_lag_bytes", 0.0f);
        }

        return derived;
    }

    /**
     * This method does the reporting of metrics to New Relic
     * 
     * @param Map results
     */
    public void reportMetrics(Map<String, Float> results) {
        int count = 0;
        logger.debug("Collected ", results.size(), " MySQL metrics. ", getAgentInfo());
        logger.debug(results);

        Iterator<String> iter = results.keySet().iterator();
        while (iter.hasNext()) { // Iterate over current metrics
            String key = iter.next().toLowerCase();
            Float val = results.get(key);
            MetricMeta md = getMetricMeta(key);
            if (md != null) { // Metric Meta data exists (from metric.category.json)
                logger.debug(METRIC_LOG_PREFIX, key, SPACE, md, EQUALS, val);
                count++;

                if (md.isCounter()) { // Metric is a counter
                    reportMetric(key, md.getUnit(), md.getCounter().process(val));
                } else { // Metric is a fixed Number
                    reportMetric(key, md.getUnit(), val);
                }
            } else { // md != null
                if (firstReport) { // Provide some feedback of available metrics for future reporting
                    logger.debug("Not reporting identified metric ", key);
                }
            }
        }
        logger.debug("Reported to New Relic ", count, " metrics. ", getAgentInfo());
    }

    /**
     * Is this agent reporting metrics for a specific category
     * 
     * @param metricCategory
     * @return boolean
     */
    boolean isReportingForCategory(String metricCategory) {
        return metrics.contains(metricCategory);
    }

    private String getAgentInfo() {
        if (agentInfo == null) {
            agentInfo = new StringBuilder().append("Agent Name: ").append(name).append(". Agent Version: ").append(version).toString();
        }
        return agentInfo;
    }

    /**
     * This method creates the metric meta data that is derived from the provided configuration and New Relic specific metrics.
     */
    private void createMetaData() {

        Map<String, Object> categories = getMetricCategories(); // Get current Metric Categories
        Iterator<String> iter = categories.keySet().iterator();
        while (iter.hasNext()) {
            String category = iter.next();
            @SuppressWarnings("unchecked")
            Map<String, String> attributes = (Map<String, String>) categories.get(category);
            String valueMetrics = attributes.get("value_metrics");
            if (valueMetrics != null) {
                Set<String> metrics = new HashSet<String>(Arrays.asList(valueMetrics.toLowerCase().replaceAll(SPACE, EMPTY_STRING).split(COMMA)));
                for (String s : metrics) {
                    addMetricMeta(category + SEPARATOR + s, new MetricMeta(false));
                }

            }
            String counterMetrics = attributes.get("counter_metrics");
            if (counterMetrics != null) {
                Set<String> metrics = new HashSet<String>(Arrays.asList(counterMetrics.toLowerCase().replaceAll(SPACE, EMPTY_STRING).split(COMMA)));
                for (String s : metrics) {
                    addMetricMeta(category + SEPARATOR + s, new MetricMeta(true));
                }
            }
        }

        /* Define New Relic specific metrics used for default dashboards */
        addMetricMeta("newrelic/volume_reads", new MetricMeta(true, "Queries/Second"));
        addMetricMeta("newrelic/volume_writes", new MetricMeta(true, "Queries/Second"));

        addMetricMeta("newrelic/bytes_reads", new MetricMeta(true, "Bytes/Second"));
        addMetricMeta("newrelic/bytes_writes", new MetricMeta(true, "Bytes/Second"));

        addMetricMeta("newrelic/connections_connected", new MetricMeta(false, "Connections"));
        addMetricMeta("newrelic/connections_running", new MetricMeta(false, "Connections"));
        addMetricMeta("newrelic/connections_cached", new MetricMeta(false, "Connections"));

        addMetricMeta("newrelic/innodb_bp_pages_created", new MetricMeta(true, "Pages/Second"));
        addMetricMeta("newrelic/innodb_bp_pages_read", new MetricMeta(true, "Pages/Second"));
        addMetricMeta("newrelic/innodb_bp_pages_written", new MetricMeta(true, "Pages/Second"));

        addMetricMeta("newrelic/query_cache_hits", new MetricMeta(true, "Queries/Seconds"));
        addMetricMeta("newrelic/query_cache_misses", new MetricMeta(true, "Queries/Seconds"));
        addMetricMeta("newrelic/query_cache_not_cached", new MetricMeta(true, "Queries/Seconds"));

        addMetricMeta("newrelic/replication_lag", new MetricMeta(false, "Seconds"));
        addMetricMeta("newrelic/replication_status", new MetricMeta(false, "State"));

        addMetricMeta("newrelic/pct_connection_utilization", new MetricMeta(false, "Percent"));
        addMetricMeta("newrelic/pct_innodb_buffer_pool_hit_ratio", new MetricMeta(false, "Percent"));
        addMetricMeta("newrelic/pct_query_cache_hit_utilization", new MetricMeta(false, "Percent"));
        addMetricMeta("newrelic/pct_query_cache_memory_in_use", new MetricMeta(false, "Percent"));
        addMetricMeta("newrelic/pct_tmp_tables_written_to_disk", new MetricMeta(false, "Percent"));

        addMetricMeta("newrelic/innodb_fsyncs_data", new MetricMeta(true, "Fsyncs/Second"));
        addMetricMeta("newrelic/innodb_fsyncs_os_log", new MetricMeta(true, "Fsyncs/Second"));

        addMetricMeta("newrelic/slave_relay_log_bytes", new MetricMeta(true, "Bytes/Second"));
        addMetricMeta("newrelic/master_log_lag_bytes", new MetricMeta(true, "Bytes/Second"));

        /* Define improved metric values for certain general metrics */
        addMetricMeta("status/aborted_clients", new MetricMeta(true, "Connections/Second"));
        addMetricMeta("status/aborted_connects", new MetricMeta(true, "Connections/Second"));

        addMetricMeta("status/bytes_sent", new MetricMeta(true, "Bytes/Second"));
        addMetricMeta("status/bytes_received", new MetricMeta(true, "Bytes/Second"));

        addMetricMeta("status/com_select", new MetricMeta(true, "Selects/Second"));
        addMetricMeta("status/com_insert", new MetricMeta(true, "Inserts/Second"));
        addMetricMeta("status/com_insert_select", new MetricMeta(true, "Inserts/Second"));
        addMetricMeta("status/com_update", new MetricMeta(true, "Updates/Second"));
        addMetricMeta("status/com_update_multi", new MetricMeta(true, "Updates/Second"));
        addMetricMeta("status/com_delete", new MetricMeta(true, "Deletes/Second"));
        addMetricMeta("status/com_delete_multi", new MetricMeta(true, "Deletes/Second"));
        addMetricMeta("status/com_replace", new MetricMeta(true, "Replaces/Second"));
        addMetricMeta("status/com_replace_select", new MetricMeta(true, "Replaces/Second"));

        addMetricMeta("status/slow_queries", new MetricMeta(true, "Queries/Second"));
        addMetricMeta("status/created_tmp_tables", new MetricMeta(true, "Queries/Second"));
        addMetricMeta("status/created_tmp_disk_tables", new MetricMeta(true, "Queries/Second"));

        addMetricMeta("status/innodb_buffer_pool_pages_flushed", new MetricMeta(true, "Pages/Second"));

        addMetricMeta("newrelic/innodb_buffer_pool_pages_clean", new MetricMeta(false, "Pages"));
        addMetricMeta("newrelic/innodb_buffer_pool_pages_dirty", new MetricMeta(false, "Pages"));
        addMetricMeta("newrelic/innodb_buffer_pool_pages_misc", new MetricMeta(false, "Pages"));
        addMetricMeta("newrelic/innodb_buffer_pool_pages_free", new MetricMeta(false, "Pages"));
        addMetricMeta("newrelic/innodb_buffer_pool_pages_unassigned", new MetricMeta(false, "Pages"));

        addMetricMeta("status/innodb_data_fsyncs", new MetricMeta(true, "Fsyncs/Second"));
        addMetricMeta("status/innodb_os_log_fsyncs", new MetricMeta(true, "Fsyncs/Second"));

        addMetricMeta("status/innodb_os_log_written", new MetricMeta(true, "Bytes/Second"));

        /* Query Cache Units */
        addMetricMeta("status/qcache_free_blocks", new MetricMeta(false, "Blocks"));
        addMetricMeta("status/qcache_free_memory", new MetricMeta(false, "Bytes"));
        addMetricMeta("status/qcache_hits", new MetricMeta(true, "Queries/Second"));
        addMetricMeta("status/qcache_inserts", new MetricMeta(true, "Queries/Second"));
        addMetricMeta("status/qcache_lowmem_prunes", new MetricMeta(true, "Queries/Second"));
        addMetricMeta("status/qcache_not_cached", new MetricMeta(true, "Queries/Second"));
        addMetricMeta("status/qcache_queries_in_cache", new MetricMeta(false, "Queries"));
        addMetricMeta("status/qcache_total_blocks", new MetricMeta(false, "Blocks"));

        addMetricMeta("innodb_status/history_list_length", new MetricMeta(false, "Pages"));
        addMetricMeta("innodb_status/queries_inside_innodb", new MetricMeta(false, "Queries"));
        addMetricMeta("innodb_status/queries_in_queue", new MetricMeta(false, "Queries"));
        addMetricMeta("innodb_status/checkpoint_age", new MetricMeta(false, "Bytes"));

        addMetricMeta("master/position", new MetricMeta(true, "Bytes/Second"));
        addMetricMeta("slave/relay_log_pos", new MetricMeta(true, "Bytes/Second"));
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
     * This provides a lazy instantiation of a MySQL metric where no meta data was defined and means new metrics can be captured automatically.
     * 
     * A default metric is a integer value
     * 
     * @param String Metric to look up
     * @return MetridMeta Structure of information about the metric
     */
    private MetricMeta getMetricMeta(String key) {
        if (key.startsWith(INNODB_MUTEX_CATEGORY) && !metricsMeta.containsKey(key)) { // This is a catch all for dynamic name metrics
            addMetricMeta(key, new MetricMeta(true, "Operations/Second"));
        }

        return metricsMeta.get(key.toLowerCase()); // Look for existing meta data on metric
    }

    /**
     * Private utility function to validate that all required data is present for constructing atomic metrics
     * 
     * @param category - a display name for which metric category will not be included if a given key is not present
     * @param map - the map of available data points
     * @param keys - keys that are expected to be present for this operation
     * @return true if all expected keys are present, otherwise false
     */
    private boolean areRequiredMetricsPresent(String category, Map<String, Float> map, String... keys) {
        for (String key : keys) {
            if (!map.containsKey(key)) {
                if (firstReport) { // Only report missing category data on the first run so as not to clutter the log
                    logger.debug("Not reporting on '", category, "' due to missing data field '", key, "'");
                }

                return false;
            }
        }

        return true;
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
