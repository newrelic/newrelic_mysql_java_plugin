package com.newrelic.plugins.mysql.instance;

import static com.newrelic.plugins.mysql.util.Constants.COMMA;
import static com.newrelic.plugins.mysql.util.Constants.EMPTY_STRING;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.newrelic.metrics.publish.Agent;
import com.newrelic.metrics.publish.AgentFactory;
import com.newrelic.metrics.publish.configuration.ConfigurationException;

/**
 * This class produces the necessary Agents to perform gathering and reporting
 * metrics for the MySQL plugin
 * 
 * @author Ronald Bradford me@ronaldbradford.com
 * 
 */
public class MySQLAgentFactory extends AgentFactory {

    private static final String CATEGORY_CONFIG_FILE = "metric.category.json";
    
    /**
     * Configure an agent based on an entry in the properties file. There may be
     * multiple agents per plugin
     */
    @Override
    public Agent createConfiguredAgent(Map<String, Object> properties) throws ConfigurationException {
        String name = (String) properties.get("name");
        String host = (String) properties.get("host");
        String user = (String) properties.get("user");
        String passwd = (String) properties.get("passwd");
        String conn_properties = (String) properties.get("properties");
        String metrics = (String) properties.get("metrics");

        if (name == null || EMPTY_STRING.equals(name)) {
            throw new ConfigurationException("The 'name' attribute is required. Have you configured the 'config/plugin.json' file?");
        }
        
        /**
         * Use pre-defined defaults to simplify configuration
         */
        if (host == null || EMPTY_STRING.equals(host)) {
            host = MySQLAgent.AGENT_DEFAULT_HOST;
        }
        if (user == null || EMPTY_STRING.equals(user)) {
            user = MySQLAgent.AGENT_DEFAULT_USER;
        }
        if (passwd == null) {
            passwd = MySQLAgent.AGENT_DEFAULT_PASSWD;
        }
        if (conn_properties == null || EMPTY_STRING.equals(conn_properties)) {
            conn_properties = MySQLAgent.AGENT_DEFAULT_PROPERTIES;
        }
        if (metrics == null || EMPTY_STRING.equals(metrics)) {
            metrics = MySQLAgent.AGENT_DEFAULT_METRICS;
        }

        return new MySQLAgent(name, host, user, passwd, conn_properties,
                processMetricCategories(metrics), readCategoryConfiguration());
    }

    /**
     * Read metric category information that enables the dynamic definition of
     * MySQL metrics that can be collected.
     * 
     * @return Map Categories and the meta data about the categories
     * @throws ConfigurationException
     */
    public Map<String, Object> readCategoryConfiguration() throws ConfigurationException {
        Map<String, Object> metricCategories = new HashMap<String, Object>();
        try {
            JSONArray json = readJSONFile(CATEGORY_CONFIG_FILE);
            for (int i = 0; i < json.size(); i++) {
                JSONObject obj = (JSONObject) json.get(i);
                String category = (String) obj.get("category");
                metricCategories.put(category, obj);
            }
        } catch (ConfigurationException e) {
            throw new ConfigurationException("'metric_categories' could not be found in the 'plugin.json' configuration file");
        }
        return metricCategories;
    }

    Set<String> processMetricCategories(String metrics) {
        String[] categories = metrics.toLowerCase().split(COMMA);
        Set<String> set = new HashSet<String>(Arrays.asList(categories));
        set.remove(EMPTY_STRING); // in case of trailing comma or two consecutive commas
        return set;
    }
}
