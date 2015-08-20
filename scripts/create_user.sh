#!/bin/bash

USERNAME=$1
PASSWORD=$2
HOST=$3

if [[ !  -f /usr/bin/mysql ]]; then
    echo "Mysql client tools not installed"
    exit 2
fi

/usr/bin/mysql -u ${USERNAME} -p${PASSWORD} -h ${HOST}  << EOF
GRANT PROCESS,REPLICATION CLIENT ON *.* TO newrelic@'%';
SET PASSWORD FOR newrelic@'%' = PASSWORD('NEWRELIC_PASSWORD');
EOF

if [[ $? -ne 0 ]]; then
    echo "Error unable to creating a mysql user"
    exit 1
fi
