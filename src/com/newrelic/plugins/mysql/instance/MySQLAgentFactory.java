package com.newrelic.plugins.mysql.instance;

import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.newrelic.data.in.Agent;
import com.newrelic.data.in.AgentFactory;
import com.newrelic.data.in.configuration.ConfigurationException;
import com.newrelic.plugins.mysql.MySQL;

/**
 * This class produces the necessary Agents to perform
 * the MySQL plugin work
 * 
 * @author Ronald Bradford me@ronaldbradford.com
 *
 */
public class MySQLAgentFactory extends AgentFactory {
	/**
	 * Construct an Agent Factory based on the default properties file
	 */
	public MySQLAgentFactory() {
		super("mysql.instance.json");
	}
	
	/**	private JSONArray readJSONFile(String string) {
		super()
	}

	 * Configure an agent based on an entry in the properties file.
	 * There may be multiple agents per Plugin
	 * 
	 */
	@Override
	public Agent createConfiguredAgent(Map<String, Object> properties) {
		String name = (String) properties.get("name");
		String host = (String) properties.get("host");
		String user = (String) properties.get("user");
		String passwd = (String) properties.get("passwd");
		String conn_properties = (String) properties.get("properties");
		String metrics = (String) properties.get("metrics");
		/**
		 * Use pre-defined defaults to simplify configuration
		 */
		if (host == null || "".equals(host)) host = MySQL.AGENT_DEFAULT_HOST;
		if (user == null || "".equals(user)) user = MySQL.AGENT_DEFAULT_USER;
		if (passwd == null || "".equals(passwd)) passwd = MySQL.AGENT_DEFAULT_PASSWD;
		if (conn_properties == null || "".equals(conn_properties)) conn_properties = MySQL.AGENT_DEFAULT_PROPERTIES;
		if (metrics == null || "".equals(metrics)) metrics = MySQL.AGENT_DEFAULT_METRICS;
       
		return new MySQLAgent(name,host,user,passwd, conn_properties, metrics, readCategoryConfiguration());
	}
	
	public Map<String,Object> readCategoryConfiguration() {
		Map<String, Object> metricCategories = new HashMap<String, Object>();

		JSONArray json;
		try {
			json = this.readJSONFile("metric.category.json");
			for (int i = 0; i < json.size(); i++) {
		    	JSONObject obj = (JSONObject) json.get(i);
		    	String category = (String)obj.get("category");
		    	metricCategories.put(category, obj);
			}

		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return metricCategories; 
	}
}
