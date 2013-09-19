# MySQL plugin for New Relic
Find the New Relic MySQL plugin in the [New Relic storefront](http://newrelic.com/plugins/new-relic-inc/52)

Find the New Relic MySQL plugin in [Plugin Central](https://rpm.newrelic.com/extensions/com.newrelic.plugins.mysql.instance)

----
**Your New Relic MySQL plugin can be operational in just a few minutes when following these steps.**

----
## Prerequisites
The MySQL plugin for New Relic requires the following:

- A New Relic account. Signup for a free account at http://newrelic.com
- A server running MySQL Version 5.0 or greater. Download MySQL for free at http://dev.mysql.com/downloads
- A configured Java Runtime (JRE) environment Version 1.6 or better
- Network access to New Relic (authenticated proxies are not currently supported, but see workaround below)

## Download
Download and unpack the [New Relic plugin for MySQL from Plugin Central](https://rpm.newrelic.com/extensions/com.newrelic.plugins.mysql.instance)

Linux example:

    $ mkdir /path/to/newrelic-plugin
    $ cd /path/to/newrelic-plugin
    $ tar zxvf newrelic_mysql_plugin*.tar.gz
    
## Create MySQL user if necessary
The MySQL plugin requires a MySQL user with limited privileges. To use the New Relic default, run the following SQL script located at [/scripts/mysql_user.sql](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/scripts/mysql_user.sql).

`$ mysql -uroot -p < mysql_user.sql`

This script will create the following user:

    username: newrelic
    host: localhost or 127.0.0.1
    password: *B8B274C6AF8165B631B4B517BD0ED2694909F464 (hashed value)

*You can choose to use a different MySQL user name and password. See [MYSQL.TXT](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/MYSQL.TXT) for more info.*

If your MySQL Server is bound to an externally visible IP address, both localhost and 127.0.0.1 will not be accessible via TCP as the host for the MySQL Plugin. In order for the plugin to connect, you will need to create a user for your IP address. Due to security concerns in this case, we strongly recommend **not** using the default password and instead setting it to some other value.

    CREATE USER newrelic@<INSERT_IP_ADDRESS_HERE> IDENTIFIED BY '<INSERT_PASSWORD_HERE>';
    GRANT PROCESS, REPLICATION CLIENT ON *.* TO newrelic@<INSERT_IP_ADDRESS_HERE>;

## Configuring your agent environment
The New Relic plugin for MySQL runs an agent process to collect and report MySQL metrics to New Relic. Configure your New Relic license and MySQL databases.

### Configure your New Relic license
Specify your license key in the necessary properties file.
Your license key can be found under Account Settings at https://rpm.newrelic.com see https://newrelic.com/docs/subscriptions/license-key for more help.

Linux example:

    $ cp config/template_newrelic.properties config/newrelic.properties
    # Edit config/newrelic.properties and paste in your license key

### Configure your MySQL properties
Each running MySQL plugin agent requires a JSON configuration file which defines the access to the monitored MySQL instance(s). An example file is provided in the config directory.

Linux example:

    $ cp config/template_mysql.instance.json config/mysql.instance.json
    # Edit config/mysql.instance.json

If using your localhost MySQL instance, add a meaningful name which will appear in the New Relic user interface for the MySQL instance. Set the "name" attribute to match your MySQL databases purpose, e.g. "Production Master". If you used the provided [/scripts/mysql_user.sql](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/scripts/mysql_user.sql) to generate a default user and password, then you do not need to set the "user" and "passwd" attributes. If you are not using the default user and password, you will need to provide a user and clear text password with the same limited priveleges as shown below.

    [
      {
        "name" : "Production Master",
        "host" : "localhost",
        "metrics" : "status,newrelic",
        "user" : "USER_NAME_HERE",
        "passwd" : "USER_CLEAR_TEXT_PASSWORD_HERE"
      }
    ]

#### Externally Visible IP Address

If your MySQL Server is bound to an externally visible IP address, set the 'host' to your IP address and use the username and password that you created above. See the 'Create MySQL user if necessary' section above.

#### Metrics

The MySQL Plugin is capable of reporting different sets of metrics by configuring the 'metrics' attribute. E.g., add the 'slave' category to report replication metrics. 
*See [CATEGORIES.TXT](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/CATEGORIES.TXT) for more info.*

## Running the agent
To run the plugin from the command line: 
`$ java -jar newrelic_mysql_plugin*.jar`

If your host needs a proxy server to access the Internet, you can specify a proxy server & port: 
`$ java -Dhttps.proxyHost=proxyhost -Dhttps.proxyPort=8080 -jar newrelic_mysql_plugin*.jar`

To run the plugin from the command line and detach the process so it will run in the background:
`$ nohup java -jar newrelic_mysql_plugin*.jar &`

## Keep this process running
You can use services like these to manage this process.

- [Upstart](http://upstart.ubuntu.com/)
- [Systemd](http://www.freedesktop.org/wiki/Software/systemd/)
- [Runit](http://smarden.org/runit/)
- [Monit](http://mmonit.com/monit/)

## For support
Plugin support and troubleshooting assistance can be obtained by visiting [support.newrelic.com](https://support.newrelic.com)

## Fork me!
The MySQL plugin uses an extensible architecture that allows you to define new MySQL metrics beyond the provided defaults. To expose more data about your MySQL servers, fork this repository, create a new GUID, add the metrics you would like to collect to config/metric.category.json and then build summary metrics and dashboards to expose your newly collected metrics.

*See [CATEGORIES.TXT](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/CATEGORIES.TXT) for more info.*

## Credits
The MySQL plugin was originally authored by [Ronald Bradford](http://ronaldbradford.com/) of [EffectiveMySQL](http://effectivemysql.com/). Subsequent updates and support are provided by [New Relic](http://newrelic.com/platform).