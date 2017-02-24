-- #####################################################################################
-- ################## MySQL DDL for tests with external DB (not mock)	################
-- #####################################################################################

CREATE TABLE table1 (
	c1_char CHAR(10),
    c2_varchar VARCHAR(20),
    c3_tinytext TINYTEXT,
    c4_text TEXT,
    c5_blob BLOB,
    c6_mediumtext MEDIUMTEXT,
    c7_mediumblob MEDIUMBLOB,
    c8_longtext LONGTEXT,
    c9_longblob LONGBLOB,
    c10_tinynt TINYINT(5),
    c11_smallint SMALLINT(5),
    c12_mediumint MEDIUMINT(5),
    c13_int INT(5),
    c14_bigint BIGINT(5),
    c15_float FLOAT,
    c16_double DOUBLE(4, 2),
    c17_decimal DECIMAL(4, 2),
    c18_date DATE,
    c19_datetime DATETIME,
    c20_timestamp TIMESTAMP,
    c21_time TIME,
    c22_bit BIT,
    c23_bit_5 BIT(10),
    c24_binary BINARY,
    c25_varbinary VARBINARY(10)
);

/* #### SAMPLE COMMANDS ####

		INSERT INTO table1 VALUES  ('test', 'test', 'test', 'test', 0xABC, 
                                    'test', 0xABCD, 'test', 0xABCDE, 5,
                                    25, 35, 55, 500, 5.5, 
                                    25.5, 35.4, curdate(), curdate(), curdate(),
                                    curtime(), 1, 1, 0xA, 0xABCD);
*/
                                    
INSERT INTO `table1` (c1_char,c2_varchar,c3_tinytext,c4_text,c5_blob,c6_mediumtext,c7_mediumblob,c8_longtext,c9_longblob,c10_tinynt,c11_smallint,c12_mediumint,c13_int,c14_bigint,c15_float,c16_double,c17_decimal,c18_date,c19_datetime,c20_timestamp,c21_time,c22_bit,c23_bit_5,c24_binary,c25_varbinary)
	VALUES('test2','test','test','test',0x0ABC,'test',0xABCD,'test',0x0ABCDE,5,25,35,55,500,'5.5',25.50,35.40,'2010-02-01','2010-02-01 00:00:00.0','2010-02-01 00:00:00.0','15:16:42',0x1,0x0001,0x0A,0xABCD);
INSERT INTO `table1` (c1_char,c2_varchar,c3_tinytext,c4_text,c5_blob,c6_mediumtext,c7_mediumblob,c8_longtext,c9_longblob,c10_tinynt,c11_smallint,c12_mediumint,c13_int,c14_bigint,c15_float,c16_double,c17_decimal,c18_date,c19_datetime,c20_timestamp,c21_time,c22_bit,c23_bit_5,c24_binary,c25_varbinary)
	VALUES('test1','test','test','test',0x0ABC,'test',0xABCD,'test',0x0ABCDE,5,25,35,55,500,'5.5',25.50,35.40,'2010-02-01','2010-02-01 00:00:00.0','2010-02-01 00:00:00.0','15:16:47',0x1,0x0001,0x0A,0xABCD);
INSERT INTO `table1` (c1_char,c2_varchar,c3_tinytext,c4_text,c5_blob,c6_mediumtext,c7_mediumblob,c8_longtext,c9_longblob,c10_tinynt,c11_smallint,c12_mediumint,c13_int,c14_bigint,c15_float,c16_double,c17_decimal,c18_date,c19_datetime,c20_timestamp,c21_time,c22_bit,c23_bit_5,c24_binary,c25_varbinary)
	VALUES('test3','test','test','test',0x0ABC,'test',0xABCD,'test',0x0ABCDE,5,25,35,55,500,'5.5',25.50,35.40,'2010-02-01','2010-02-01 00:00:00.0','2010-02-01 00:00:00.0','15:16:51',0x1,0x0001,0x0A,0xABCD);
                                    

-- ###		Table for test reseting auto_increment value (including zero)	###
	
CREATE TABLE test_autoincrement (
	id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
	data VARCHAR(100)
);

SET SESSION sql_mode='NO_AUTO_VALUE_ON_ZERO';	-- allow inserting 0 values to AUTO_INCREMENT column

INSERT INTO test_autoincrement VALUES (0, 'value0');
INSERT INTO test_autoincrement VALUES (1, 'value1');
INSERT INTO test_autoincrement VALUES (2, 'value2');
INSERT INTO test_autoincrement VALUES (3, 'value3');


