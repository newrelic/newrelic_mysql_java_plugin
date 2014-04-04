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

**Note:** The MySQL Plugin includes the [Connector/J JDBC Driver](http://dev.mysql.com/usingmysql/java/) and it does not need to be installed separately.

## Installation

The MySQL plugin can be installed in several ways:

* [Chef and Puppet Install Scripts](#chef-and-puppet)
* [Manual Install](#download)

## Chef and Puppet

For [Chef](http://www.getchef.com) and [Puppet](http://puppetlabs.com) support see the New Relic plugin's [Chef Cookbook](http://community.opscode.com/cookbooks/newrelic_plugins) and [Puppet Module](https://forge.puppetlabs.com/newrelic/newrelic_plugins).

Additional information on using Chef and Puppet with New Relic is available in New Relic's [documentation](https://docs.newrelic.com/docs/plugins/plugin-installation-with-chef-and-puppet).

## Download
Download and unpack the [New Relic plugin for MySQL from Plugin Central](https://rpm.newrelic.com/extensions/com.newrelic.plugins.mysql.instance)

Linux example:

    $ mkdir /path/to/newrelic-plugin
    $ cd /path/to/newrelic-plugin
    $ tar zxvf newrelic_mysql_plugin*.tar.gz

Windows example:

    $ mkdir /path/to/newrelic-plugin
    $ cd /path/to/newrelic-plugin
    # untar the archive with a tool of your choice
    
## Create MySQL user if necessary
The MySQL plugin requires a MySQL user with limited privileges. To use the New Relic default, run the following SQL script located at [/scripts/mysql_user.sql](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/scripts/mysql_user.sql).

`$ mysql -uroot -p < mysql_user.sql`

This script will create the following user:

    username: newrelic
    host: localhost or 127.0.0.1
    password: *B8B274C6AF8165B631B4B517BD0ED2694909F464 (hashed value)

*You can choose to use a different MySQL user name and password. See [MYSQL.TXT](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/MYSQL.TXT) for more info.*

If your MySQL Server is bound to an externally visible IP address, both localhost and 127.0.0.1 will not be accessible via TCP as the host for the MySQL Plugin. In order for the plugin to connect, you will need to create a user for your IP address. Due to security concerns in this case, we strongly recommend **not** using the default password and instead setting it to some other value.

    CREATE USER newrelic@<INSERT_IP_ADDRESS_HERE> IDENTIFIED BY '<INSERT_HASHED_PASSWORD_HERE>';
    GRANT PROCESS, REPLICATION CLIENT ON *.* TO newrelic@<INSERT_IP_ADDRESS_HERE>;

## Configuring your agent environment
The New Relic plugin for MySQL runs an agent process to collect and report MySQL metrics to New Relic. Configure your New Relic license and MySQL databases.

### Configure your New Relic license
Specify your license key in the necessary properties file.
Your license key can be found under Account Settings at https://rpm.newrelic.com see https://newrelic.com/docs/subscriptions/license-key for more help.

Linux example:

    $ cp config/newrelic.template.json config/newrelic.json
    # Edit config/newrelic.json and paste in your license key

### Configure your MySQL properties
Each running MySQL plugin agent requires a JSON configuration file which defines the access to the monitored MySQL instance(s). An example file is provided in the config directory.

Linux example:

    $ cp config/plugin.template.json config/plugin.json
    # Edit config/plugin.json

If using your localhost MySQL instance, add a meaningful name which will appear in the New Relic user interface for the MySQL instance. Set the "name" attribute to match your MySQL databases purpose, e.g. "Production Master". If you used the provided [/scripts/mysql_user.sql](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/scripts/mysql_user.sql) to generate a default user and password, then you do not need to set the "user" and "passwd" attributes. If you are not using the default user and password, you will need to provide a user and clear text password with the same limited priveleges as shown below.

    {
      "agents": [
        {
          "name" : "Production Master",
          "host" : "localhost",
          "metrics" : "status,newrelic",
          "user" : "USER_NAME_HERE",
          "passwd" : "USER_CLEAR_TEXT_PASSWORD_HERE"
        }
      ]
    }

#### Externally Visible IP Address

If your MySQL Server is bound to an externally visible IP address, set the 'host' to your IP address and use the username and password that you created above. See the 'Create MySQL user if necessary' section above.

#### Metrics

The MySQL Plugin is capable of reporting different sets of metrics by configuring the 'metrics' attribute. E.g., add the 'slave' category to report replication metrics. 
*See [CATEGORIES.TXT](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/CATEGORIES.TXT) for more info.*

**Note:** The `innodb_mutex` metric category can lead to increased memory usage for the plugin if the monitored database is under a high level of contention (i.e. large numbers of active mutexes).

### Amazon RDS

The MySQL plugin can report metrics for Amazon RDS MySQL instances as well. To do so, configure the `plugin.json` as mentioned above in [Configure your MySQL properties](#configure-your-mysql-properties) and set your `host` attribute to the RDS instance endpoint without the port. The endpoint can be found in the AWS console for your instance. For more information regarding the RDS endpoint see the [Amazon RDS documentation](http://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_ConnectToInstance.html).

Ex. `database1.abcjiasdocdsf.us-west-1.rds.amazonaws.com`

The `user` and `passwd` attributes should be the RDS master user and master password, or a user and password that has correct privileges. See [Create MySQL user if necessary](#create-mysql-user-if-necessary) for more information. 

## Running the agent
To run the plugin from the command line: 
`$ java -jar plugin.jar`

**Note:** When running the plugin on a server class machine, the `java` command will start a JVM that may reserve up to one quarter (25%) of available memory. To reduce the amount of reserved memory use the `-Xmx` JVM argument. Ex. `java -Xmx128m -jar plugin.jar`.

For more information on JVM server class machines and the `-Xmx` JVM argument, see: 

 - [http://docs.oracle.com/javase/6/docs/technotes/guides/vm/server-class.html](http://docs.oracle.com/javase/6/docs/technotes/guides/vm/server-class.html)
 - [http://docs.oracle.com/cd/E22289_01/html/821-1274/configuring-the-default-jvm-and-java-arguments.html](http://docs.oracle.com/cd/E22289_01/html/821-1274/configuring-the-default-jvm-and-java-arguments.html)

If your host needs a proxy server to access the Internet, you can specify a proxy server & port: 
`$ java -Dhttps.proxyHost=proxyhost -Dhttps.proxyPort=8080 -jar plugin.jar`

To run the plugin from the command line and detach the process so it will run in the background:
`$ nohup java -jar plugin.jar &`

## Keep this process running
You can use services like these to manage this process.

- [Upstart](http://upstart.ubuntu.com/)
- [Systemd](http://www.freedesktop.org/wiki/Software/systemd/)
- [Runit](http://smarden.org/runit/)
- [Monit](http://mmonit.com/monit/)

### Debian init script configuration
The debian init script will import environment variables from /etc/default/newrelic-mysql-plugin

Set the DAEMONDIR variable to the directory where you have installed the agent
## For support
Plugin support and troubleshooting assistance can be obtained by visiting [support.newrelic.com](https://support.newrelic.com)

## Fork me!
The MySQL plugin uses an extensible architecture that allows you to define new MySQL metrics beyond the provided defaults. To expose more data about your MySQL servers, fork this repository, create a new GUID, add the metrics you would like to collect to config/metric.category.json and then build summary metrics and dashboards to expose your newly collected metrics.

*See [CATEGORIES.TXT](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/CATEGORIES.TXT) for more info.*

## Credits
The MySQL plugin was originally authored by [Ronald Bradford](http://ronaldbradford.com/) of [EffectiveMySQL](http://effectivemysql.com/). Subsequent updates and support are provided by [New Relic](http://newrelic.com/platform).
