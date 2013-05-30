MySQL plugin for New Relic
==========================

PLEASE READ ALL OF THESE INSTRUCTIONS. Your New Relic MySQL plugin can be operational in 2 minutes
when following all steps in this readme file.

Prerequisites
-------------

The MySQL plugin for New Relic requires the following prerequisites:

  1. A New Relic account. Signup for a free account at http://newrelic.com
  2. A server running MySQL Version 5.0 or greater. Download MySQL for free at http://dev.mysql.com/downloads
  3. A configured Java Runtime (JRE) environment Version 1.6 or better

Download
--------

Download and unpack the New Relic plugin for MySQL from Plugin Central.

The following instructions can be used on Linux systems.

 $ mkdir /path/to/newrelic-plugin
 $ cd /path/to/newrelic-plugin
 $ tar xfz newrelic_mysql_plugin*.tar.gz


Getting Started -- Configuring your Agent Environment
----------------------------------

The New Relic plugin for MySQL runs an Agent process to collect and report MySQL metrics to 
New Relic. The following steps configure this agent for your specific environment.

  1.  Enter New Relic Account License Key
  2.  Create MySQL properties
  
  
1. New Relic License

Specify your license key in the necessary properties file.
Your license key can be found under Account Settings at https://rpm.newrelic.com

 # Edit config/newrelic.properties and enter your license key.


2. MySQL Properties

Each running MySQL plugin agent requires a JSON configuration file defining the access to 
the MySQL instance(s). An example file is provided in the config directory.

 # Edit config/mysql.instance.json

If using your localhost MySQL instance, add your user name and passwd as well as a meaningful name for the MySQL instance. 
Set the value for the "name" attribute to match your MySQL databases purpose, e.g. "Production Master". 
More extensive instructions are available below in the Production Setup section.

[
    {
        "name" : "Localhost",
        "host" : "localhost",
        "metrics" : "status,newrelic",
        "user" : "USER_NAME_HERE",
        "passwd" : "USER_PASSWD_HERE"
    },
]
 
  
Running the Agent
-----------------

 $ chmod +x run_mysql_plugin.sh
 $ ./run_mysql_plugin.sh

To run the plugin in the background.

 $ nohup ./run_mysql_plugin.sh &

At present there are no init.d scripts to start the New Relic MySQL plugin at system startup.


Production Setup
-----------------

1. Create MySQL User

The MySQL plugin requires a MySQL user with limited privileges. To use the New Relic default, 
run the following SQL file.

 $ mysql -uroot -p < mysql_user.sql
 
You can choose to use a different MySQL user name and password if you desire. See the file 
MYSQL.TXT for more information.


2. MySQL Properties

 # Edit config/mysql.instance.json
 
Adjust your instance information with name, host, and username/passwd. 


Source Code
-----------

This plugin can be found at https://github.com/newrelic-platform/newrelic_mysql_extension