[![Archived header](https://github.com/newrelic/open-source-office/raw/master/examples/categories/images/Archived.png)](https://github.com/newrelic/open-source-office/blob/master/examples/categories/index.md#archived)

*Development of this plugin has discontinued and you are encouraged to migrate your MySQL monitoring solution to the [MySQL integration](https://docs.newrelic.com/docs/integrations/host-integrations/host-integrations-list/mysql-monitoring-integration) for [New Relic Infrastructure](https://docs.newrelic.com/docs/infrastructure).*

# New Relic Platform MySQL Plugin - Java

Find the New Relic MySQL plugin in the [New Relic storefront](http://newrelic.com/plugins/new-relic-inc/52)

Find the New Relic MySQL plugin in [Plugin Central](https://rpm.newrelic.com/extensions/com.newrelic.plugins.mysql.instance)

The MySQL plugin monitors a variety of metrics provided by MySQL server through query results. Instead of having to type these queries to get performance and functional metrics on your database, the plugin polls the database server on a 1-minute harvest cycle interval and displays the information in a dashboard.

----

## General functionality

In order to poll certain types of metrics the plugin expects to find configuration values pointing out which categories of metrics should be collected. Some of these categories depend on each other, whereas others are only useful in certain contexts. These metric categories are listed as part of the "metrics" key in the plugin.json configuration file for the plugin. The following categories are available:

Metric Category|Depends On|Default Database Query|Description|
----------------|------------|------------------------|-------------|
status||SHOW GLOBAL STATUS | General status metrics about the running database server|
master||SHOW MASTER STATUS|Status metrics specific to the "master" server of a master-slave configuration|
slave||SHOW SLAVE STATUS|Status metrics specific to the "slave" server of a master-slave configuration|
newrelic|status||General metrics derived from other (more directly available) ones.|

----

## What's new in V2?

This plugin has been upgraded to V2 of the New Relic Platform Java SDK.  For version 2 of the Java SDK, we have made several changes to help make the installation experience more uniform for plugins.  The changes include:

* 'newrelic.properties' file is now 'newrelic.json'
* Plugin configuration is now done through the 'plugin.json' file
* Logging has been made more robust and easier to use.
* Jar distributables now have a well-defined name (i.e. plugin.jar)
* Configuration files are now located in a well-defined location (i.e. './config' off the root)

More information on these changes (including how to configure logging, license keys, and the plugin itself) can be found [here](https://github.com/newrelic-platform/metrics_publish_java#configuration-options).  If you have any feedback, please don't hesitate to reach out to us through our forums [here](https://discuss.newrelic.com/category/platform-plugins/platform-sdk).

----

## Requirements

The requirements for running this plugin are:

- A New Relic account. Sign up for a free account [here](http://newrelic.com)
- Java Runtime (JRE) environment Version 1.6 or later
- A server running MySQL Version 5.0 or greater
- Network access to New Relic (authenticated proxies are not currently supported, but see workaround below)

**Note:** The MySQL Plugin includes the [Connector/J JDBC Driver](http://dev.mysql.com/usingmysql/java/) and it does not need to be installed separately.

----

## Installation

This plugin can be installed one of the following ways:

* [Option 1 - New Relic Platform Installer](#option-1--install-with-the-new-relic-platform-installer)
* [Option 2 - Chef and Puppet Install Scripts](#option-2--install-via-chef-or-puppet)
* [Option 3 - Manual Install](#option-3--install-manually)

### Option 1 - Install with the New Relic Platform Installer

The New Relic Platform Installer (NPI) is a simple, lightweight command line tool that helps you easily download, configure and manage New Relic Platform Plugins.  To learn more simply go to [our forum category](https://discuss.newrelic.com/category/platform-plugins/platform-installer) and checkout the ['Getting Started' section](https://discuss.newrelic.com/t/getting-started-for-the-platform-installer/842).  If you have any questions, concerns or feedback, please do not hesitate to reach out through the forums as we greatly appreciate your feedback!

Once you've installed the NPI tool, run the following command:

```
	./npi install com.newrelic.plugins.mysql.instance
```	

This command will take care of the creation of `newrelic.json` and `plugin.json` configuration files.  See the [configuration information](#configuration-information) section for more information.

### Option 2 - Install via Chef or Puppet

For [Chef](http://www.getchef.com) and [Puppet](http://puppetlabs.com) support see the New Relic plugin's [Chef Cookbook](http://community.opscode.com/cookbooks/newrelic_plugins) and [Puppet Module](https://forge.puppetlabs.com/newrelic/newrelic_plugins).

Additional information on using Chef and Puppet with New Relic is available in New Relic's [documentation](https://docs.newrelic.com/docs/plugins/plugin-installation-with-chef-and-puppet).

### Option 3 - Install Manually (Non-standard)

#### Step 1 - Downloading and Extracting the Plugin

The latest version of the plugin can be downloaded [here](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/tree/master/dist).  Once the plugin is on your box, extract it to a location of your choosing.

**note** - This plugin is distributed in tar.gz format and can be extracted with the following command on Unix-based systems (Windows users will need to download a third-party extraction tool or use the [New Relic Platform Installer](https://discuss.newrelic.com/t/getting-started-with-the-platform-installer/842)):

```
	tar -xvzf newrelic_mysql_plugin-X.Y.Z.tar.gz
```

#### Step 2 - Configuring the Plugin

Check out the [configuration information](#configuration-information) section for details on configuring your plugin. 

#### Step 3 - Running the Plugin

To run the plugin, execute the following command from a terminal or command window (assuming Java is installed and on your path):

```
	java -Xmx128m -jar plugin.jar
```

**Note:** Though it is not necessary, the '-Xmx128m' flag is highly recommended due to the fact that when running the plugin on a server class machine, the `java` command will start a JVM that may reserve up to one quarter (25%) of available memory, but the '-Xmx128m' flag will limit heap allocation to a more reasonable 128MBs.  

For more information on JVM server class machines and the `-Xmx` JVM argument, see: 

 - [http://docs.oracle.com/javase/6/docs/technotes/guides/vm/server-class.html](http://docs.oracle.com/javase/6/docs/technotes/guides/vm/server-class.html)
 - [http://docs.oracle.com/cd/E22289_01/html/821-1274/configuring-the-default-jvm-and-java-arguments.html](http://docs.oracle.com/cd/E22289_01/html/821-1274/configuring-the-default-jvm-and-java-arguments.html)
 
#### Step 4 - Keeping the Plugin Running

Step 3 showed you how to run the plugin; however, there are several problems with running the process directly in the foreground (For example, when the machine reboots the process will not be started again).  That said, there are several common ways to keep a plugin running, but they do require more advanced knowledge or additional tooling.  We highly recommend considering using the [New Relic Platform Installer](https://discuss.newrelic.com/t/getting-started-with-the-platform-installer/842) or Chef/Puppet scripts for installing plugins as they will take care of most of the heavy lifting for you.  

If you prefer to be more involved in the maintaince of the process, consider one of these tools for managing your plugin process (bear in mind that some of these are OS-specific):

- [Upstart](http://upstart.ubuntu.com/)
- [Systemd](http://www.freedesktop.org/wiki/Software/systemd/)
- [Runit](http://smarden.org/runit/)
- [Monit](http://mmonit.com/monit/)

----
    
## Configuration Information

### Configuration Files

You will need to modify two configuration files in order to set this plugin up to run.  The first (`newrelic.json`) contains configurations used by all Platform plugins (e.g. license key, logging information, proxy settings) and can be shared across your plugins.  The second (`plugin.json`) contains data specific to each plugin such as a list of hosts and port combination for what you are monitoring.  Templates for both of these files should be located in the '`config`' directory in your extracted plugin folder. 

#### Configuring the `plugin.json` file: 

The `plugin.json` file has a provided template in the `config` directory named `plugin.template.json`.  If you are installing manually, make a copy of this template file and rename it to `plugin.json` (the New Relic Platform Installer will automatically handle creation of configuration files for you).  

Below is an example of the `plugin.json` file's contents, you can add multiple objects to the "agents" array to monitor different instances:

```
    {
      "agents": [
        {
          "name" : "Production Master",
          "host" : "localhost:port",
          "metrics" : "status,newrelic",
          "user" : "USER_NAME_HERE",
          "passwd" : "USER_CLEAR_TEXT_PASSWORD_HERE"
        }
      ]
    }
```

#### Tips:

* Set the "name" attribute to match your MySQL databases purpose, e.g. "Production Master" as this will be used to identify that instance in the New Relic UI. 

* If you used the provided [/scripts/mysql_user.sql](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/scripts/mysql_user.sql) to generate a default user and password, then you do not need to set the "user" and "passwd" attributes.

* If using an externally visible IP address, the username and password fields are no longer optional. See [Create a MySQL user (optional)](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/README.md#create-a-mysql-user-optional).

#### Configuring the `newrelic.json` file: 

The `newrelic.json` file also has a provided template in the `config` directory named `newrelic.template.json`.  If you are installing manually, make a copy of this template file and rename it to `newrelic.json` (again, the New Relic Platform Installer will automatically handle this for you).  

The `newrelic.json` is a standardized file containing configuration information that applies to any plugin (e.g. license key, logging, proxy settings), so going forward you will be able to copy a single `newrelic.json` file from one plugin to another.  Below is a list of the configuration fields that can be managed through this file:

##### Configuring your New Relic License Key

Your New Relic license key is the only required field in the `newrelic.json` file as it is used to determine what account you are reporting to.  If you do not know what your license key is, you can learn about it [here](https://newrelic.com/docs/subscriptions/license-key).

Example: 

```
{
  "license_key": "YOUR_LICENSE_KEY_HERE"
}
```

##### Logging configuration

By default Platform plugins will have their logging turned on; however, you can manage these settings with the following configurations:

`log_level` - The log level. Valid values: [`debug`, `info`, `warn`, `error`, `fatal`]. Defaults to `info`.

`log_file_name` - The log file name. Defaults to `newrelic_plugin.log`.

`log_file_path` - The log file path. Defaults to `logs`.

`log_limit_in_kbytes` - The log file limit in kilobytes. Defaults to `25600` (25 MB). If limit is set to `0`, the log file size would not be limited.

Example:

```
{
  "license_key": "YOUR_LICENSE_KEY_HERE"
  "log_level": "debug",
  "log_file_path": "/var/logs/newrelic"
}
```

##### Proxy configuration

If you are running your plugin from a machine that runs outbound traffic through a proxy, you can use the following optional configurations in your `newrelic.json` file:

`proxy_host` - The proxy host (e.g. `webcache.example.com`)

`proxy_port` - The proxy port (e.g. `8080`).  Defaults to `80` if a `proxy_host` is set

`proxy_username` - The proxy username

`proxy_password` - The proxy password

Example:

```
{
  "license_key": "YOUR_LICENSE_KEY_HERE",
  "proxy_host": "proxy.mycompany.com",
  "proxy_port": 9000
}
```

### Additional Configuration

#### Create a MySQL user (optional)

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

#### Selecting metrics

The MySQL Plugin is capable of reporting different sets of metrics by configuring the 'metrics' attribute. E.g., add the 'slave' category to report replication metrics. 
*See [CATEGORIES.TXT](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/CATEGORIES.TXT) for more info.*

**Note:** The `innodb_mutex` metric category can lead to increased memory usage for the plugin if the monitored database is under a high level of contention (i.e. large numbers of active mutexes).

## Troubleshooting

### If you don’t see plugin metrics report to your account:

When the component simply doesn't report to your account, it's usually indicative of a permissions issue. When this issue occurs, ensure that you have a user with the permissions listed in the repository's additional configuration:

* [Additional Configuration](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/README.md#additional-configuration)

Other than permissions, verifying that the license key is correct and that the plugin can connect to our collectors through any firewalls are good sanity checks:

* [Confuguring the `newrelic.json` file](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/README.md#configuring-the-pluginjson-file)

### If you don't see any data for the 'X' metric:

Most of the time, a missing metric is a result of a missing category in the plugin.json file. Confirm your setup (i.e. are you using a master->slave architecture?) and verify that the correct categories for that component (slave, master, etc.) are entered into the "metrics" field of the plugin.json file.

If the correct categories are listed, the issue is almost always a problem with the MySQL server not reporting certain numbers, or not running in a setup where the numbers will ever have anything meaningful to report.

You may be curious about "Replication Lag" on a slave database. Assuming configuration is correct, replication lag will only be present in a small set of circumstances. Details on this metric and all other metrics the plugin draws from can be found in the official MySQL documentation for the underlying queries.

* [SHOW STATUS Syntax](https://dev.mysql.com/doc/refman/5.7/en/show-status.html)

* [SHOW MASTER STATUS Syntax](https://dev.mysql.com/doc/refman/5.7/en/show-master-status.html)

* [SHOW SLAVE STATUS Syntax](https://dev.mysql.com/doc/refman/5.7/en/show-slave-status.html)

For further confirmation of the numbers reported for various metrics try collecting detailed logs from your plugin instance and examining them.

### Collecting Logs
#### When using NPI:

1. Browse to your plugin install directory and edit the `newrelic.json` file, from the NPI folder `plugins/com.newrelic.plugins.mysql.instance/newrelic_mysql_plugin-{version}/config/newrelic.json`
2. Change the line `log_level": "info"` to  `log_level": "debug"`
3. Save and close the file
4. Restart the plugin with the commands; `./npi stop com.newrelic.plugins.mysql.instance` then `./npi start com.newrelic.plugins.mysql.instance`
5. If applicable, the generated logs from `plugins/com.newrelic.plugins.mysql.instance/newrelic_mysql_plugin-{version}/logs/newrelic_plugin.log` can be sent to support
6. Set the log level back to `info` and restart the plugin again


---

Once permissions and configuration are taken care of, most problems you encounter will be the result of the underlying database server. Certain metrics always reporting as "0", missing query counts, etc. can be influenced by various aspects of how MySQL is expected to run.

Additionally, even though Amazon Aurora is built around MySQL, the MySQL plugin will not instrument IOPS due to how IOPS are billed in Amazon RDS.

## Support

Plugin support and troubleshooting assistance can be obtained by visiting [support.newrelic.com](https://support.newrelic.com)

### Frequently Asked Questions

**Q: Does this plugin support Amazon RDS?**

**A:** The MySQL plugin can report metrics for Amazon RDS MySQL instances as well. To do so, configure the `plugin.json` as mentioned above in [Configure your MySQL properties](#configure-your-mysql-properties) and set your `host` attribute to the RDS instance endpoint without the port. The endpoint can be found in the AWS console for your instance. For more information regarding the RDS endpoint see the [Amazon RDS documentation](http://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/USER_ConnectToInstance.html).

E.g. `database1.abcjiasdocdsf.us-west-1.rds.amazonaws.com`

The `user` and `passwd` attributes should be the RDS master user and master password, or a user and password that has correct privileges. See [Create MySQL user if necessary](#create-mysql-user-if-necessary) for more information. 

**Q: Do you support MariaDB?**

**A:** The short answer is 'no'.  While we understand that MariaDB is supposed to be functionally equivalent to MySQL, we have received feedback from some users that the MySQL plugin does not work against their MariaDB instances.  We may address this at some point in the future, but currently do not actively support this scenario.

----

## Fork me!

This plugin uses an extensible architecture that allows you to define new MySQL metrics beyond the provided defaults. To expose more data about your MySQL servers, fork this repository, create a new GUID, add the metrics you would like to collect to config/metric.category.json and then build summary metrics and dashboards to expose your newly collected metrics.

*See [CATEGORIES.TXT](https://github.com/newrelic-platform/newrelic_mysql_java_plugin/blob/master/CATEGORIES.TXT) for more info.*

----

## Credits

The MySQL plugin was originally authored by [Ronald Bradford](http://ronaldbradford.com/) of [EffectiveMySQL](http://effectivemysql.com/). Subsequent updates and support are provided by [New Relic](http://newrelic.com/platform).

## Contributing

You are welcome to send pull requests to us - however, by doing so you agree that you are granting New Relic a non-exclusive, non-revokable, no-cost license to use the code, algorithms, patents, and ideas in that code in our products if we so choose. You also agree the code is provided as-is and you provide no warranties as to its fitness or correctness for any purpose.
