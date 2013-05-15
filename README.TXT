MySQL plugin for New Relic
==========================

Prerequisites
-------------

The MySQL plugin for New Relic requires the following prerequisites:

  1. A New Relic account. Signup for a free account at http://newrelic.com
  2. A server running MySQL Version 5.0 or greater. Download MySQL for free at http://dev.mysql.com/downloads
  3. A configured Java Runtime (JRE) environment Version 1.6 or better

Download
--------

To run the MySQL plugin for New Relic, you require the software and the MySQL Connector/J:

  1. Download and unpack the New Relic plugin for MySQL from Plugin Central
  2. Download the MySQL Connector/J from http://dev.mysql.com/downloads/connector/j/

The following instructions help to automate these steps.

 $ mkdir /path/to/newrelic-plugin
 $ cd /path/to/newrelic-plugin
 $ wget http://s3.amazonaws.com/new_relic_platform_plugin_binary_hosting/mysql_plugin/newrelic_mysql_plugin-beta.tar.gz
 $ tar xfz newrelic_mysql_plugin*.tar.gz
 $ sh ./get_mysql_connector.sh


Configuring your Agent Environment
----------------------------------

The New Relic plugin for MySQL runs an Agent process to collect and report MySQL metrics to New Relic. 
The following steps configure this agent for your specific environment.

  1.  Enter New Relic Account License Key
  2.  Create MySQL user
  3.  Create MySQL properties
  
  
1. New Relic License

Specific your current license key in the necessary properties file.

 $ cp config/newrelic.properties.example config/newrelic.properties
 # Edit config/newrelic.properties and enter your license key found under Account Settings at https://rpm.newrelic.com

2. Create MySQL User

The MySQL plugin requires a MySQL user with limited privileges. To use the New Relic default, run the following SQL file.

 $ mysql -uroot -p < mysql_user.sql
 
You can choose to use a different MySQL user name and password if you desire. See the file MYSQL.TXT for more information.

3. MySQL Properties

Each running MySQL plugin agent requires a JSON configuration file defining the access to the MySQL instance(s).

 $ cp config/mysql.instance.json.sample config/mysql.instance.json
 # Edit config/mysql.instance.json

If you have configured the Agent to run on the MySQL instance, and used the MySQL user privileges in the 
previous step, you only need to give your MySQL instance a meaningful name.  All other options will use defaults.
Set the value for the "name" attribute to match your MySQL databases purpose, e.g. "Production Master"

 [
    {
        "name"   : "Server/Instance Name",
    }
 ]
 
  
Running the Agent
-----------------

 $ chmod +x run_mysql_plugin.sh
 $ ./run_mysql_plugin.sh

To run the plugin in the background.

 $ nohup ./run_mysql_plugin.sh &

At present there are no init.d scripts to start the New Relic MySQL plugin at system startup.


Source Code
-----------

This plugin can be found at https://github.com/newrelic-platform/newrelic_mysql_extension