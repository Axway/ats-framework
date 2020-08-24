SELECT 
                c.conname AS constraint_name,
		CASE c.condeferrable WHEN true THEN 'DEFERRABLE' ELSE 'NOT DEFERRABLE' END AS deferrable_type,
                (SELECT n.nspname FROM pg_namespace AS n WHERE n.oid=c.connamespace) AS constraint_schema,

                tf.name AS from_table,
                (
                    SELECT STRING_AGG(QUOTE_IDENT(a.attname), ', ' ORDER BY t.seq)
                    FROM
                        (
                            SELECT
                                ROW_NUMBER() OVER (ROWS UNBOUNDED PRECEDING) AS seq,
                                attnum
                            FROM
                                UNNEST(c.conkey) AS t(attnum)
                        ) AS t
                        INNER JOIN pg_attribute AS a ON a.attrelid=c.conrelid AND a.attnum=t.attnum
                ) AS from_cols,

                tt.name AS to_table,
                (
                    SELECT STRING_AGG(QUOTE_IDENT(a.attname), ', ' ORDER BY t.seq)
                    FROM
                        (
                            SELECT
                                ROW_NUMBER() OVER (ROWS UNBOUNDED PRECEDING) AS seq,
                                attnum
                            FROM
                                UNNEST(c.confkey) AS t(attnum)
                        ) AS t
                        INNER JOIN pg_attribute AS a ON a.attrelid=c.confrelid AND a.attnum=t.attnum
                ) AS to_cols,

                CASE confupdtype WHEN 'r' THEN 'restrict' WHEN 'c' THEN 'cascade' WHEN 'n' THEN 'set null' WHEN 'd' THEN 'set default' WHEN 'a' THEN 'no action' ELSE NULL END AS on_update,
                CASE confdeltype WHEN 'r' THEN 'restrict' WHEN 'c' THEN 'cascade' WHEN 'n' THEN 'set null' WHEN 'd' THEN 'set default' WHEN 'a' THEN 'no action' ELSE NULL END AS on_delete,
                CASE confmatchtype::text WHEN 'f' THEN 'full' WHEN 'p' THEN 'partial' WHEN 'u' THEN 'simple' WHEN 's' THEN 'simple' ELSE NULL END AS match_type,  -- In earlier postgres docs, simple was 'u'nspecified, but current versions use 's'imple.  text cast is required.

                pg_catalog.pg_get_constraintdef(c.oid, true) as condef
            FROM
                pg_catalog.pg_constraint AS c
                INNER JOIN (
                    SELECT pg_class.oid, QUOTE_IDENT(pg_namespace.nspname) || '.' || QUOTE_IDENT(pg_class.relname) AS name
                    FROM pg_class INNER JOIN pg_namespace ON pg_class.relnamespace=pg_namespace.oid
                ) AS tf ON tf.oid=c.conrelid
                INNER JOIN (
                    SELECT pg_class.oid, QUOTE_IDENT(pg_namespace.nspname) || '.' || QUOTE_IDENT(pg_class.relname) AS name
                    FROM pg_class INNER JOIN pg_namespace ON pg_class.relnamespace=pg_namespace.oid
                ) AS tt ON tt.oid=c.confrelid
            WHERE c.contype = 'f' AND tf.name = '<SCHEMA_NAME>.<TABLE_NAME>' AND c.conname = '<FK_NAME>' ORDER BY 1;
