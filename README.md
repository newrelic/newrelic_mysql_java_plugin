# MySQL plugin for New Relic
Find the New Relic MySQL plugin in the [New Relic storefront](https://rpm.newrelic.com/plugins/new_relic_mysql/52)

Find the New Relic MySQL plugin in [Plugin Central](https://rpm.newrelic.com/extensions/com.newrelic.plugins.mysql.instance)

----
**Your New Relic MySQL plugin can be operational in 2 minutes when following these steps.**

----
## Prerequisites
The MySQL plugin for New Relic requires the following:

- A New Relic account. Signup for a free account at http://newrelic.com
- A server running MySQL Version 5.0 or greater. Download MySQL for free at http://dev.mysql.com/downloads
- A configured Java Runtime (JRE) environment Version 1.6 or better

## Download
Download and unpack the New Relic plugin for MySQL from Plugin Central: https://rpm.newrelic.com/plugins/

Linux example:

    $ mkdir /path/to/newrelic-plugin
    $ cd /path/to/newrelic-plugin
    $ tar xfz newrelic_mysql_plugin*.tar.gz
    
## Create MySQL user if necessary
The MySQL plugin requires a MySQL user with limited privileges. To use the New Relic default, run the following SQL script located at [/scripts/mysql_user.sql](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/scripts/mysql_user.sql).

`$ mysql -uroot -p < mysql_user.sql`

This script will create the following user:

    username: newrelic
    host: localhost or 127.0.0.1
    password: *B8B274C6AF8165B631B4B517BD0ED2694909F464

*You can choose to use a different MySQL user name and password. see [MYSQL.TXT](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/MYSQL.TXT) for more info.*

## Configuring your agent environment
The New Relic plugin for MySQL runs an agent process to collect and report MySQL metrics to New Relic. Configure your New Relic license and MySQL databases.

### Configure your New Relic license
Specify your license key in the necessary properties file.
Your license key can be found under Account Settings at https://rpm.newrelic.com see https://newrelic.com/docs/subscriptions/license-key for more help.

Linux example:

    $ cp config/template_newrelic.properties config/newrelic.properties
    # Edit config/newrelic.properties and paste in your license key

### Configure your MySQL properties
Each running MySQL plugin agent requires a JSON configuration file defining the access to the monitored MySQL instance(s). An example file is provided in the config directory.

Linux example:

    $ cp config/template_mysql.instance.json config/mysql.instance.json
    # Edit config/mysql.instance.json

If using your localhost MySQL instance, add your user name and password as well as a meaningful name which will appear in the New Relic user interface for the MySQL instance. Set the value for the "name" attribute to match your MySQL databases purpose, e.g. "Production Master". 

    [
      {
        "name" : "Localhost",
        "host" : "localhost",
        "metrics" : "status,newrelic",
        "user" : "USER_NAME_HERE",
        "passwd" : "USER_PASSWD_HERE"
       },
    ]

## Running the agent
To run the plugin in from the command line: 
`$ java -jar newrelic_mysql_plugin*.jar`

To run the plugin in from the command line and detach the process so it will run in the background:
`$ nohup java -jar newrelic_mysql_plugin*.jar &`

*Note: At present there are no [init.d](http://en.wikipedia.org/wiki/Init) scripts to start the New Relic MySQL plugin at system startup.*

## Keep this process running
You can use services like these to manage this process.

- [Upstart](http://upstart.ubuntu.com/)
- [Systemd](http://www.freedesktop.org/wiki/Software/systemd/)
- [Runit](http://smarden.org/runit/)
- [Monit](http://mmonit.com/monit/)

## For support
Plugin support for troubleshooting assistance can be obtained by visiting [support.newrelic.com](https://support.newrelic.com)

## Fork me!
The MySQL plugin uses an extensible architecture that allows you to define new MySQL metrics beyond the provided defaults. To expose more data about your MySQL servers, fork this repository, create a new GUID, add the metrics you would like to collect to config/metric.category.json and then build summary metrics and dashboards to expose your newly collected metrics.

*See [CATEGORIES.TXT](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/CATEGORIES.TXT) for more info.*
