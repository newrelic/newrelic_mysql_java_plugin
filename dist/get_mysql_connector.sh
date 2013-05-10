#!/bin/sh
wget http://cdn.mysql.com/Downloads/Connector-J/mysql-connector-java-5.1.25.tar.gz
tar xvfz mysql-connector-java-5.1.25.tar.gz mysql-connector-java-5.1.25/mysql-connector-java-5.1.25-bin.jar
mv mysql-connector-java-5.1.25/mysql-connector-java-5.1.25-bin.jar mysql-connector-java.jar
rm -rf mysql-connector-java-5.1.25*
exit 0

