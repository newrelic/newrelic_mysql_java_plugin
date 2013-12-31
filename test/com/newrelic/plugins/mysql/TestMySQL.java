package com.newrelic.plugins.mysql;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.newrelic.plugins.mysql.instance.MySQLAgent;

/**
 * This class performs test cases on the MySQL class. All enabled tests pass on version 5.5 of MySQL.
 * 
 * @author Ronald Bradford me@ronaldbradford.com
 * 
 */

public class TestMySQL {

    @Before
    public void setUp() {
    }

    @Test
    public void verifyValidConnection() {
        Connection c = new MySQL().getConnection(MySQLAgent.AGENT_DEFAULT_HOST, MySQLAgent.AGENT_DEFAULT_USER, MySQLAgent.AGENT_DEFAULT_PASSWD,
                MySQLAgent.AGENT_DEFAULT_PROPERTIES);
        assertNotNull(c);
    }

    @Test
    public void verifyInvalidConnectionWithBadHost() {
        Connection c = new MySQL().getConnection("bad_host", MySQLAgent.AGENT_DEFAULT_USER, MySQLAgent.AGENT_DEFAULT_PASSWD,
                MySQLAgent.AGENT_DEFAULT_PROPERTIES);
        assertNull(c);
    }

    @Test
    public void verifyInvalidConnectionWithBadUser() {
        Connection c = new MySQL().getConnection(MySQLAgent.AGENT_DEFAULT_HOST, "bad_user", MySQLAgent.AGENT_DEFAULT_PASSWD,
                MySQLAgent.AGENT_DEFAULT_PROPERTIES);
        assertNull(c);
    }

    @Test
    public void verifyInvalidConnectionWithBadPassword() {
        Connection c = new MySQL().getConnection(MySQLAgent.AGENT_DEFAULT_HOST, MySQLAgent.AGENT_DEFAULT_USER, "bad_password",
                MySQLAgent.AGENT_DEFAULT_PROPERTIES);
        assertNull(c);
    }

    @Test
    public void verifyInvalidConnectionWithBadProperties() {
        Connection c = new MySQL().getConnection(MySQLAgent.AGENT_DEFAULT_HOST, MySQLAgent.AGENT_DEFAULT_USER, MySQLAgent.AGENT_DEFAULT_PASSWD,
                "bad_properties");
        assertNull(c);
    }

    @Test
    public void verifyTransformedStringMetrics() {
        assertEquals("FRED", MySQL.transformStringMetric("FRED"));
        assertEquals("1", MySQL.transformStringMetric("ON"));
        assertEquals("1", MySQL.transformStringMetric("TRUE"));
        assertEquals("0", MySQL.transformStringMetric("OFF"));
        assertEquals("0", MySQL.transformStringMetric("NONE"));
        assertEquals("-1", MySQL.transformStringMetric("NULL"));
        assertEquals("FALSE", MySQL.transformStringMetric("FALSE"));
    }

    @Test
    public void verifyValidMetricValues() {
        assertTrue(MySQL.validMetricValue("0"));
        assertTrue(MySQL.validMetricValue("1"));
        assertTrue(MySQL.validMetricValue("-1"));
        assertTrue(MySQL.validMetricValue("100"));
        assertTrue(MySQL.validMetricValue("-100"));
        assertTrue(MySQL.validMetricValue("456.7789"));
        assertTrue(MySQL.validMetricValue("-456.7789"));
        assertTrue(MySQL.validMetricValue(".234"));
        assertTrue(MySQL.validMetricValue("0.234"));
        assertTrue(MySQL.validMetricValue("-.234"));
        assertTrue(MySQL.validMetricValue("-0.234"));
    }

    @Test
    public void verifyInValidMetricValues() {
        assertFalse(MySQL.validMetricValue(""));
        assertFalse(MySQL.validMetricValue("5.25.45a"));
        assertFalse(MySQL.validMetricValue("10.38.78.86"));
        assertFalse(MySQL.validMetricValue("+123"));
        assertFalse(MySQL.validMetricValue("123-34"));
        assertFalse(MySQL.validMetricValue("123."));
        assertFalse(MySQL.validMetricValue("10a"));
        assertFalse(MySQL.validMetricValue("abc"));
    }

    @Test
    public void verifyValidMetricValuesThatGetTranformed() {
        assertTrue(MySQL.validMetricValue(MySQL.transformStringMetric("ON")));
        assertTrue(MySQL.validMetricValue(MySQL.transformStringMetric("TRUE")));
        assertTrue(MySQL.validMetricValue(MySQL.transformStringMetric("OFF")));
        assertTrue(MySQL.validMetricValue(MySQL.transformStringMetric("NONE")));
        assertTrue(MySQL.validMetricValue(MySQL.transformStringMetric("NULL")));
    }

    @Test
    public void runSQLSingleStatusValid() {
        Connection c = new MySQL().getConnection(MySQLAgent.AGENT_DEFAULT_HOST, MySQLAgent.AGENT_DEFAULT_USER, MySQLAgent.AGENT_DEFAULT_PASSWD,
                MySQLAgent.AGENT_DEFAULT_PROPERTIES);
        assertNotNull(c);
        Map<String, Float> results = MySQL.runSQL(c, "status", "SHOW GLOBAL STATUS LIKE 'Com_xa_rollback'", "set");
        assertEquals(1, results.size());
        assertEquals(0, results.get("status/com_xa_rollback").intValue()); // A status likely to never have a value
    }

    @Test
    public void runSQLSingleStatusValue() {
        Connection c = new MySQL().getConnection(MySQLAgent.AGENT_DEFAULT_HOST, MySQLAgent.AGENT_DEFAULT_USER, MySQLAgent.AGENT_DEFAULT_PASSWD,
                MySQLAgent.AGENT_DEFAULT_PROPERTIES);
        assertNotNull(c);
        Map<String, Float> results = MySQL.runSQL(c, "status", "SHOW GLOBAL STATUS LIKE 'Uptime'", "set");
        assertEquals(1, results.size());
        assertTrue(results.get("status/uptime").intValue() > 0); // A status that will always be > 0
    }

    @Ignore
    @Test
    public void runSQLSingleStatusInvalid() {
        Connection c = new MySQL().getConnection(MySQLAgent.AGENT_DEFAULT_HOST, MySQLAgent.AGENT_DEFAULT_USER, MySQLAgent.AGENT_DEFAULT_PASSWD,
                MySQLAgent.AGENT_DEFAULT_PROPERTIES);
        assertNotNull(c);
        Map<String, Float> results = MySQL.runSQL(c, "status", "SHOW GLOBAL VARIABLES LIKE 'version'", "set");
        assertEquals(0, results.size()); // This is removed because value is a string
    }

    @Test
    public void runSQLSingleStatusTranslated() {
        Connection c = new MySQL().getConnection(MySQLAgent.AGENT_DEFAULT_HOST, MySQLAgent.AGENT_DEFAULT_USER, MySQLAgent.AGENT_DEFAULT_PASSWD,
                MySQLAgent.AGENT_DEFAULT_PROPERTIES);
        assertNotNull(c);
        Map<String, Float> results = MySQL.runSQL(c, "status", "SHOW GLOBAL STATUS LIKE 'Compression'", "set");
        assertEquals(1, results.size());
        assertEquals(0, results.get("status/compression").intValue()); //Translated from OFF
    }

    public void closeConnection(Connection c) {
        try {
            if (c != null) {
                c.close();
            }
        } catch (SQLException e) {
        }
    }

    @Test
    public void runTranslateStringToNumber() {
        assertEquals(5, MySQL.translateStringToNumber("5").intValue());
        assertEquals(java.lang.Float.class, MySQL.translateStringToNumber("5").getClass());
        assertEquals(5.0f, MySQL.translateStringToNumber("5.0"), 0.0001f);
        assertEquals(java.lang.Float.class, MySQL.translateStringToNumber("5.0").getClass());
        Float x = new Float("37107795968");
        assertEquals(x, MySQL.translateStringToNumber("37107795968"));
    }

    @Test
    public void verifyFloat() {
        String val = "0.00";
        assertTrue(val.matches("\\d*\\.\\d*"));
    }

    @Test
    public void verifyNotFloat() {
        String val = "70797";
        assertFalse(val.matches("\\d*\\.\\d*"));
    }

    @Ignore
    @Test
    public void runSHOWENGINEINNODBSTATUS() throws SQLException {
        Connection c = new MySQL().getConnection(MySQLAgent.AGENT_DEFAULT_HOST, MySQLAgent.AGENT_DEFAULT_USER, MySQLAgent.AGENT_DEFAULT_PASSWD,
                MySQLAgent.AGENT_DEFAULT_PROPERTIES);
        assertNotNull(c);
        Statement stmt = c.createStatement();
        String SQL = "SHOW ENGINE INNODB STATUS";
        Map<String, Float> results = MySQL.processInnoDBStatus(stmt.executeQuery(SQL), "innodb_status");
        assertEquals(3, results.size());
        assertNotNull(results.get("innodb_status/history_list_length"));

        String s = "0 queries inside innodb, 0 queries in queue";
        assertTrue(s.matches(".*queries inside innodb.*"));

        s = "&dict->stats[i]";
        assertEquals("dict_statsi", s.replaceAll("[&\\[\\]]", "").replaceAll("->", "_"));
    }

    @Test
    public void testProcessInnoDBStatus() {
        Map<String, Float> results = MySQL.processInnoDBStatus(INNODB_STATUS, "test");

        assertEquals(1096f, results.get("test/history_list_length"), 0.0001f);
        assertEquals(11727772890f, results.get("test/log_sequence_number"), 0.0001f);
        assertEquals(11727772890f, results.get("test/last_checkpoint"), 0.0001f);
        assertEquals(0f, results.get("test/queries_inside_innodb"), 0.0001f);
        assertEquals(0f, results.get("test/queries_in_queue"), 0.0001f);
        assertEquals(0, results.get("test/checkpoint_age"), 0.0001f);
    }

    @Test
    public void testBuildString() {
        assertEquals("onetwo ,three", MySQL.buildString("one", "two", " ", ",", "three"));
        assertEquals(" ", MySQL.buildString("", " "));
    }

    private static final String INNODB_STATUS = "=====================================\n"
            + "131118 19:46:19 INNODB MONITOR OUTPUT\n"
            + "=====================================\n"
            + "Per second averages calculated from the last 31 seconds\n"
            + "-----------------\n"
            + "BACKGROUND THREAD\n"
            + "-----------------\n"
            + "srv_master_thread loops: 105382 1_second, 105381 sleeps, 9446 10_second, 12076 background, 12075 flush\n"
            + "srv_master_thread log flush and writes: 105427\n"
            + "----------\n"
            + "SEMAPHORES\n"
            + "----------\n"
            + "OS WAIT ARRAY INFO: reservation count 238575, signal count 83540\n"
            + "Mutex spin waits 1890366, rounds 3580179, OS waits 54697\n"
            + "RW-shared spins 68558, rounds 3445412, OS waits 98364\n"
            + "RW-excl spins 349350, rounds 3954294, OS waits 69291\n"
            + "Spin rounds per wait: 1.89 mutex, 50.26 RW-shared, 11.32 RW-excl\n"
            + "------------------------\n"
            + "LATEST FOREIGN KEY ERROR\n"
            + "------------------------\n"
            + "131031 12:02:00 Transaction:\n"
            + "TRANSACTION 987CE1, ACTIVE 0 sec inserting\n"
            + "mysql tables in use 1, locked 1\n"
            + "3 lock struct(s), heap size 376, 1 row lock(s), undo log entries 1\n"
            + "MySQL thread id 703, OS thread handle 0x10e547000, query id 1170599 localhost 127.0.0.1 root update\n"
            + "INSERT IGNORE INTO table_infos (table_name, account_id, begin_time, end_time, shard_id, frequency) VALUES('ts_477_16009_1d',477,'2013-10-31 00:00:00','2013-11-01 00:00:00',2,60)\n"
            + "Foreign key constraint fails for table `test`.`table_infos`:\n" + ",\n"
            + "  CONSTRAINT `table_infos_ibfk_1` FOREIGN KEY (`account_id`) REFERENCES `accounts` (`id`)\n"
            + "Trying to add in child table, in index `index_table_infos_on_account_id_and_begin_time` tuple:\n" + "DATA TUPLE: 3 fields;\n"
            + " 0: len 4; hex 800001dd; asc     ;;\n" + " 1: len 8; hex 8000124f1ef3a3c0; asc    O    ;;\n" + " 2: len 4; hex 8000006a; asc    j;;\n" + "\n"
            + "But in parent table `test`.`accounts`, in index `PRIMARY`,\n" + "the closest match we can find is record:\n"
            + "PHYSICAL RECORD: n_fields 64; compact format; info bits 32\n" + " 0: len 4; hex 80000361; asc    a;;\n"
            + " 1: len 6; hex 0000009872ba; asc     r ;;\n" + " 2: len 7; hex 49000006881cc2; asc I      ;;\n"
            + " 3: len 19; hex 54657374204163636f756e7420383232323033; asc Test Account 822203;;\n" + " 4: SQL NULL;\n"
            + " 5: len 6; hex 616374697665; asc active;;\n"
            + " 6: len 30; hex 67656e6572617465645f746573773655f6b65795f383232; asc generated_test_key_822; (total 33 bytes);\n" + " 7: SQL NULL;\n"
            + " 8: len 4; hex 80000001; asc     ;;\n" + " 9: len 1; hex 80; asc  ;;\n" + " 10: SQL NULL;\n" + " 11: SQL NULL;\n" + " 12: SQL NULL;\n"
            + " 13: SQL NULL;\n" + " 14: SQL NULL;\n" + " 15: SQL NULL;\n" + " 16: SQL NULL;\n" + " 17: SQL NULL;\n" + " 18: len 1; hex 80; asc  ;;\n"
            + " 19: SQL NULL;\n" + " 20: len 1; hex 80; asc  ;;\n" + " 21: SQL NULL;\n" + " 22: SQL NULL;\n" + " 23: len 4; hex 80000002; asc     ;;\n"
            + " 24: SQL NULL;\n" + " 25: SQL NULL;\n" + " 26: SQL NULL;\n" + " 27: len 11; hex 6372656417264; asc id;;\n" + " 28: SQL NULL;\n"
            + " 29: len 4; hex 80000001; asc     ;;\n" + " 30: SQL NULL;\n" + " 31: SQL NULL;\n" + " 32: SQL NULL;\n" + " 33: len 1; hex 80; asc  ;;\n"
            + " 34: SQL NULL;\n" + " 35: SQL NULL;\n" + " 36: SQL NULL;\n" + " 37: SQL NULL;\n" + " 38: SQL NULL;\n" + " 39: SQL NULL;\n" + " 40: SQL NULL;\n"
            + " 41: SQL NULL;\n" + " 42: len 4; hex 80000000; asc     ;;\n" + " 43: SQL NULL;\n" + " 44: len 10; hex 6b65736313531; asc key_686151;;\n"
            + " 45: SQL NULL;\n" + " 46: len 4; hex 80000001; asc     ;;\n" + " 47: SQL NULL;\n" + " 48: len 1; hex 81; asc  ;;\n" + " 49: SQL NULL;\n"
            + " 50: SQL NULL;\n" + " 51: SQL NULL;\n" + " 52: SQL NULL;\n" + " 53: SQL NULL;\n" + " 54: SQL NULL;\n" + " 55: SQL NULL;\n"
            + " 56: len 1; hex 80; asc  ;;\n" + " 57: SQL NULL;\n" + " 58: len 1; hex 81; asc  ;;\n" + " 59: SQL NULL;\n" + " 60: SQL NULL;\n"
            + " 61: len 4; hex 80000001; asc     ;;\n" + " 62: SQL NULL;\n" + " 63: len 4; hex 80000001; asc     ;;\n" + "\n" + "------------\n"
            + "TRANSACTIONS\n" + "------------\n" + "Trx id counter A36FB5\n" + "Purge done for trx's n:o < A36FAF undo n:o < 0\n"
            + "History list length 1096\n" + "LIST OF TRANSACTIONS FOR EACH SESSION:\n" + "---TRANSACTION 0, not started\n"
            + "MySQL thread id 29121, OS thread handle 0x10bcb5000, query id 1934861 localhost root\n" + "SHOW ENGINE INNODB STATUS\n" + "--------\n"
            + "FILE I/O\n" + "--------\n" + "I/O thread 0 state: waiting for i/o request (insert buffer thread)\n"
            + "I/O thread 1 state: waiting for i/o request (log thread)\n" + "I/O thread 2 state: waiting for i/o request (read thread)\n"
            + "I/O thread 3 state: waiting for i/o request (read thread)\n" + "I/O thread 4 state: waiting for i/o request (read thread)\n"
            + "I/O thread 5 state: waiting for i/o request (read thread)\n" + "I/O thread 6 state: waiting for i/o request (write thread)\n"
            + "I/O thread 7 state: waiting for i/o request (write thread)\n" + "I/O thread 8 state: waiting for i/o request (write thread)\n"
            + "I/O thread 9 state: waiting for i/o request (write thread)\n" + "Pending normal aio reads: 0 [0, 0, 0, 0] , aio writes: 0 [0, 0, 0, 0] ,\n"
            + " ibuf aio reads: 0, log i/o's: 0, sync i/o's: 0\n" + "Pending flushes (fsync) log: 0; buffer pool: 0\n"
            + "4731 OS file reads, 1354071 OS file writes, 941695 OS fsyncs\n" + "0.00 reads/s, 0 avg bytes/read, 0.00 writes/s, 0.00 fsyncs/s\n"
            + "-------------------------------------\n" + "INSERT BUFFER AND ADAPTIVE HASH INDEX\n" + "-------------------------------------\n"
            + "Ibuf: size 1, free list len 5, seg size 7, 64 merges\n" + "merged operations:\n" + " insert 196, delete mark 1, delete 0\n"
            + "discarded operations:\n" + " insert 0, delete mark 0, delete 0\n" + "Hash table size 276707, node heap has 197 buffer(s)\n"
            + "0.00 hash searches/s, 0.00 non-hash searches/s\n" + "---\n" + "LOG\n" + "---\n" + "Log sequence number 11727772890\n"
            + "Log flushed up to   11727772890\n" + "Last checkpoint at  11727772890\n" + "0 pending log writes, 0 pending chkp writes\n"
            + "908045 log i/o's done, 0.00 log i/o's/second\n" + "----------------------\n" + "BUFFER POOL AND MEMORY\n" + "----------------------\n"
            + "Total memory allocated 137363456; in additional pool allocated 0\n" + "Dictionary memory allocated 3811224\n" + "Buffer pool size   8192\n"
            + "Free buffers       0\n" + "Database pages     7995\n" + "Old database pages 2931\n" + "Modified db pages  0\n" + "Pending reads 0\n"
            + "Pending writes: LRU 0, flush list 0, single page 0\n" + "Pages made young 5672, not young 0\n" + "0.00 youngs/s, 0.00 non-youngs/s\n"
            + "Pages read 4716, created 25552, written 699907\n" + "0.00 reads/s, 0.00 creates/s, 0.00 writes/s\n"
            + "No buffer pool page gets since the last printout\n" + "Pages read ahead 0.00/s, evicted without access 0.00/s, Random read ahead 0.00/s\n"
            + "LRU len: 7995, unzip_LRU len: 0\n" + "I/O sum[0]:cur[0], unzip sum[0]:cur[0]\n" + "--------------\n" + "ROW OPERATIONS\n" + "--------------\n"
            + "0 queries inside InnoDB, 0 queries in queue\n" + "1 read views open inside InnoDB\n"
            + "Main thread id 4495892480, state: waiting for server activity\n"
            + "Number of rows inserted 6229092, updated 1570966, deleted 199402, read 138785654\n"
            + "0.00 inserts/s, 0.00 updates/s, 0.00 deletes/s, 0.00 reads/s\n" + "----------------------------\n" + "END OF INNODB MONITOR OUTPUT\n"
            + "============================";
}
