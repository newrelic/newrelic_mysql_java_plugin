package com.newrelic.plugins.mysql.instance;

import java.util.Map;
import com.newrelic.data.in.Agent;
import com.newrelic.data.in.AgentFactory;
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
	
	/**
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
		String metrics = (String) properties.get("metrics");
		/**
		 * Use pre-defined defaults to simplify configuration
		 */
		if (host == null || "".equals(host)) host = MySQL.AGENT_DEFAULT_HOST;
		if (user == null || "".equals(user)) user = MySQL.AGENT_DEFAULT_USER;
		if (passwd == null || "".equals(passwd)) passwd = MySQL.AGENT_DEFAULT_PASSWD;
		if (metrics == null || "".equals(metrics)) metrics = MySQL.AGENT_DEFAULT_METRICS;
		return new MySQLAgent(name,host,user,passwd, metrics);
	}
}
