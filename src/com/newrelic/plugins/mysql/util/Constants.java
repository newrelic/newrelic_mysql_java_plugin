package com.newrelic.plugins.mysql.util;

import java.util.regex.Pattern;

public class Constants {

    public static final String COMMA = ",";
    public static final String SLASH = "/";
    public static final String SPACE = " ";
    public static final String EMPTY_STRING = "";
    public static final String UNDERSCORE = "_";
    public static final String LEFT_PAREN = "(";
    public static final String RIGHT_PAREN = ")";
    public static final String ARROW = "->";
    public static final String EQUALS = "=";
    public static final String NEW_LINE = "\n"; 
    public static final String SQL = "SQL";
    public static final String RESULT = "result";
    public static final String COUNTER = "[counter]";
    public static final String METRIC_LOG_PREFIX = "Metric ";

    public static final String SEPARATOR = "/";
    public static final String PING = "/* ping */ SELECT 1";
    public static final Pattern VALID_METRIC_PATTERN = Pattern.compile("(-)?(\\.)?\\d+(\\.\\d+)?");  // Only integers and floats are valid metric values
    public static final Pattern SPACE_PATTERN = Pattern.compile(" ");

    public static final String JDBC_URL = "jdbc:mysql://";
    public static final String PASSWORD_FILTERED = "/PASSWORD_FILTERED";

    public static final String ROW = "row";
    public static final String SET = "set";
    public static final String SPECIAL = "special";

    public static final String ON = "ON";
    public static final String OFF = "OFF";
    public static final String TRUE = "TRUE";
    public static final String NONE = "NONE";
    public static final String YES = "YES";
    public static final String NO = "NO";
    public static final String NULL = "NULL";

    public static final String ONE = "1";
    public static final String NEG_ONE = "-1";
    public static final String ZERO = "0";

    public static final String NEW_RELIC_CATEGORY = "newrelic";
    public static final String STATUS_CATEGORY = "status";

    public static final String INNODB_MUTEX_REGEX = "[&\\[\\]]";
    public static final String INNODB_MUTEX_CATEGORY = "innodb_mutex/";

    public static final String SHOW_ENGINE_INNODB_MUTEX = "SHOW ENGINE INNODB MUTEX";
    public static final String SHOW_ENGINE_INNODB_STATUS = "SHOW ENGINE INNODB STATUS";

    public static final String HISTORY_LIST_LENGTH = "History list length";
    public static final String LOG_SEQUENCE_NUMBER = "Log sequence number";
    public static final String LAST_CHECKPOINT_AT = "Last checkpoint at ";
    public static final Pattern QUERIES_INSIDE_INNODB_REGEX_PATTERN = Pattern.compile(".* queries inside InnoDB.*");
    public static final String QUERIES_INSIDE_INNODB_REGEX2 = " queries inside InnoDB.*";
    public static final String QUERIES_IN_QUEUE_REGEX = ".* queries inside InnoDB, ";
    public static final String QUERIES_IN_QUEUE_REGEX2 = " queries in queue";

    public static final String SECONDS_BEHIND_MASTER = "seconds_behind_master";
    public static final String HISTORY_LIST_LENGTH_METRIC = "history_list_length";
    public static final String LOG_SEQUENCE_NUMBER_METRIC = "log_sequence_number";
    public static final String LAST_CHECKPOINT_METRIC = "last_checkpoint";
    public static final String QUERIES_INSIDE_INNODB_METRIC = "queries_inside_innodb";
    public static final String QUERIES_IN_QUEUE = "queries_in_queue";
    public static final String CHECKPOINT_AGE_METRIC = "checkpoint_age";
}
