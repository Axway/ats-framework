-- #####################################################################################
-- ################## ORACLE DDL for tests with external DB (not mock)	################
-- #####################################################################################

CREATE TABLE table1 (
	c1_number NUMBER NOT NULL PRIMARY KEY,
	c2_varchar VARCHAR(30),
        c3_char char(10),
        c4_nchar nchar(10),
        c5_nvarchar2 nvarchar2(10),
        c6_varchar2 varchar2(10),
--        c7_long long, -- There is a problem with the other 'long raw' column ORA-01754: a table may contain only one column of type LONG
        c8_raw raw(100),
--        c9_long_raw long raw, --deprecated
        c10_float float,
        c11_dec dec(4, 2),
        c12_decimal decimal(4, 2),
        c13_integer integer,
        c15_int int,
        c16_smallint smallint,
        c17_real real,
        c18_double_precision double precision,
        c19_date date,
        c20_timestamp_6 timestamp,
        c21_timestamp_3 timestamp(3),
        c22_char_1_byte char(1),
        c23_clob clob,
        c24_blob blob,
        c25_nclob nclob
--        c26_bfile BFILE --Contains a locator to a large binary file stored outside the database. 
    
        );

/* #### SAMPLE COMMANDS ####

INSERT INTO table1 VALUES (   5, 'test', 'test', 'test', 'test', 
                                  'test', 'ABC', 5.2, 23.4, 21.6,
                                  5, 4, 3, 54.32, 32.12, 
                                  SYSDATE, SYSDATE, SYSDATE, 1, 'ABC', 
                                  'DEF', 'FED');

INSERT INTO table1(C25_NCLOB,C24_BLOB,C23_CLOB,C22_CHAR_1_BYTE,C21_TIMESTAMP_3,C20_TIMESTAMP_6,C19_DATE,C18_DOUBLE_PRECISION,C17_REAL,C16_SMALLINT,C15_INT,C13_INTEGER,C12_DECIMAL,C11_DEC,C10_FLOAT,C8_RAW,C6_VARCHAR2,C5_NVARCHAR2,C4_NCHAR,C3_CHAR,C2_VARCHAR,C1_NUMBER) VALUES ('FED','0DEF','ABC','1',to_timestamp('2010-01-20 17:44:07.123','YYYY-MM-DD hh24:mi:ss.FF'),to_date('2010-01-20 17:44:07','YYYY-MM-DD hh24:mi:ss'),to_date('2010-01-20 17:44:07','YYYY-MM-DD hh24:mi:ss'),'32.12','54.32','3','4','5','21.6','23.4','5.2','0ABC','test','test','test      ','test      ','test','5');

*/
        
        
/* ######## RESETING ORACLE SEQUENCE TO SOME VALUE ######### 
 
 	For reseting some sequence to given value (in this case 100), we need to execute something like this:
      
    DECLARE
        currVal NUMBER;
    BEGIN
        SELECT test_seq.NEXTVAL INTO currVal FROM dual;
        EXECUTE IMMEDIATE 'ALTER SEQUENCE test_seq INCREMENT BY -' || TO_CHAR(currVal - 100);
        SELECT test_seq.NEXTVAL INTO currVal FROM dual;
        EXECUTE IMMEDIATE 'ALTER SEQUENCE test_seq INCREMENT BY 1';
        COMMIT;
    END; 
 */
      