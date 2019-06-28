CREATE PROCEDURE [generateTableKeyConstraintScript]
	
	@schemaName SYSNAME,
	@tableName SYSNAME,
	@idxName SYSNAME
AS

	DECLARE
		@TableSchema sysname,
		@IndexName sysname,
		@IndexType nvarchar(60),
		@IndexIsUnique bit,
		@IndexIgnoreDupKey bit,
		@IndexIsPrimaryKey bit,
		@IndexIsUniqueConstraint bit,
		@IndexFillFactor tinyint,
		@IndexIsPadded bit,
		@IndexIsDisabled bit,
		@IndexIsHypothetical bit,
		@IndexAllowRowLocks bit,
		@IndexAllowPageLocks bit,
		@IndexHasFilter bit,
		@IndexFilterDefinition nvarchar(max),
		@DataSpaceName sysname,
		@DataSpaceType char(2),
		@ColumnName sysname,
		@ColumnIsDesc bit,
		@ColumnIsIncluded bit,
		@ColumnKeyOrdinal tinyint,
		@ColumnPartitionOrdinal tinyint,
		@IndexStatisticsNoRecompute bit,
		@IndexStatisticsIncremental bit,
		@IndexDataCompression nvarchar(60),
		@ConstraintType char(2),
		@IsSystemNamed bit;
		
		DECLARE aCursor CURSOR FOR 
								SELECT
									TableSchema = sch.name,
									IndexName = ind.name,
									IndexType = ind.type_desc,
									IndexIsUnique = ind.is_unique,
									IndexIgnoreDupKey = ind.ignore_dup_key,
									IndexIsPrimaryKey = ind.is_primary_key,
									IndexIsUniqueConstraint = ind.is_unique_constraint,
									IndexFillFactor = ind.fill_factor,
									IndexIsPadded = ind.is_padded,
									IndexIsDisabled = ind.is_disabled,
									IndexIsHypothetical = ind.is_hypothetical,
									IndexAllowRowLocks = ind.allow_row_locks,
									IndexAllowPageLocks = ind.allow_page_locks,
									IndexHasFilter = ind.has_filter,
									IndexFilterDefinition = ind.filter_definition,
									IndexStatisticsNoRecompute = st.no_recompute,
									IndexStatisticsIncremental = st.is_incremental,
									IndexDataCompression = p.data_compression_desc,
									DataSpaceName = ds.name,
									DataSpaceType = ds.type,
									ColumnName = col.name,
									ColumnIsDesc = ic.is_descending_key,
									ColumnIsIncluded = ic.is_included_column,
									ColumnKeyOrdinal = ic.key_ordinal,
									ColumnPartitionOrdinal = ic.partition_ordinal,
									ConstraintType = kc.type,
									IsSystemNamed = kc.is_system_named
								FROM 
									sys.indexes ind 
								INNER JOIN 
									sys.index_columns ic ON  ind.object_id = ic.object_id and ind.index_id = ic.index_id 
								INNER JOIN
									sys.columns col ON ic.object_id = col.object_id and ic.column_id = col.column_id 
								INNER JOIN 
									sys.tables t ON ind.object_id = t.object_id
								INNER JOIN
									sys.data_spaces ds ON ind.data_space_id = ds.data_space_id
								INNER JOIN
									sys.schemas sch ON t.schema_id = sch.schema_id
								INNER JOIN
									sys.stats st ON st.object_id = ind.object_id AND st.stats_id = ind.index_id
								INNER JOIN
									sys.partitions p ON p.object_id = ind.object_id
								INNER JOIN
									sys.key_constraints kc ON kc.name = ind.name
								WHERE 
									t.name = @tableName
								AND
									ind.name = @idxName
								AND
									ind.name IS NOT NULL
								AND
									sch.name = @schemaName
								ORDER BY ic.key_ordinal
								
		DECLARE @keyColumns VARCHAR(MAX) = '';
		DECLARE @partitionColumns VARCHAR(MAX) = '';
		DECLARE @withOptions VARCHAR(MAX) = '';
		DECLARE @fetchStatus int = 0;
		DECLARE @createIndexStatement VARCHAR(MAX) = '';
		
		OPEN aCursor
		WHILE 0 = @fetchStatus
		BEGIN
			FETCH NEXT FROM aCursor INTO
							@TableSchema,
							@IndexName,
							@IndexType,
							@IndexIsUnique,
							@IndexIgnoreDupKey,
							@IndexIsPrimaryKey,
							@IndexIsUniqueConstraint,
							@IndexFillFactor,
							@IndexIsPadded,
							@IndexIsDisabled,
							@IndexIsHypothetical,
							@IndexAllowRowLocks,
							@IndexAllowPageLocks,
							@IndexHasFilter,
							@IndexFilterDefinition,
							@IndexStatisticsNoRecompute,
							@IndexStatisticsIncremental,
							@IndexDataCompression,
							@DataSpaceName,
							@DataSpaceType,
							@ColumnName,
							@ColumnIsDesc,
							@ColumnIsIncluded,
							@ColumnKeyOrdinal,
							@ColumnPartitionOrdinal,
							@ConstraintType,
							@IsSystemNamed
							
			SET @fetchStatus = @@FETCH_STATUS
			IF 0 = @fetchStatus
			BEGIN
				-- KEY COLUMNS PROCESSING START
				IF @keyColumns <> ''
				BEGIN
					IF @ColumnKeyOrdinal <> 0 AND CHARINDEX('[' + @ColumnName + ']',@keyColumns) = 0
					BEGIN
						SET @keyColumns += ', [' + @ColumnName + ']' + CASE WHEN @ColumnIsDesc = 1 THEN ' DESC' ELSE ' ASC' END;
					END
				END
				ELSE
				BEGIN
					IF @ColumnKeyOrdinal <> 0
					BEGIN
						SET @keyColumns = '[' + @ColumnName + ']' + CASE WHEN @ColumnIsDesc = 1 THEN ' DESC' ELSE ' ASC' END;
					END
				END
				-- KEY COLUMNS PROCESSING END
				-- PARTITION COLUMNS PROCESSING START
				IF @partitionColumns <> ''
				BEGIN
					IF @ColumnPartitionOrdinal <> 0 AND CHARINDEX('[' + @ColumnName + ']',@partitionColumns) = 0
					BEGIN
						SET @partitionColumns += ', [' + @ColumnName + ']';
					END
				END
				ELSE
				BEGIN
					IF @ColumnPartitionOrdinal <> 0
					BEGIN
						SET @partitionColumns = '[' + @ColumnName + ']';
					END
				END
				-- PARTITION COLUMNS PROCESSING END
				-- WITH OPTIONS PROCESSING START
				IF @withOptions = ''
				BEGIN
					SET @withOptions = '' + CASE WHEN @IndexIsPadded = 1 THEN 'PAD_INDEX = ON' ELSE 'PAD_INDEX = OFF' END
										  + CASE WHEN @IndexFillFactor <> 0 THEN ', FILLFACTOR = ' + CONVERT(CHAR(3), @IndexFillFactor) ELSE '' END
										  + CASE WHEN @IndexIgnoreDupKey = 1 THEN ', IGNORE_DUP_KEY = ON ' ELSE ', IGNORE_DUP_KEY = OFF' END
										  + CASE WHEN @IndexStatisticsNoRecompute = 1 THEN ', STATISTICS_NORECOMPUTE = ON ' ELSE ', STATISTICS_NORECOMPUTE = OFF' END
										  + CASE WHEN @IndexStatisticsIncremental = 1 THEN ', STATISTICS_INCREMENTAL = ON ' ELSE ', STATISTICS_INCREMENTAL = OFF' END
										  + CASE WHEN @IndexAllowRowLocks = 1 THEN ', ALLOW_ROW_LOCKS = ON' ELSE ', ALLOW_ROW_LOCKS = OFF' END
										  + CASE WHEN @IndexAllowPageLocks = 1 THEN ', ALLOW_PAGE_LOCKS = ON' ELSE ', ALLOW_PAGE_LOCKS = OFF' END
										  + ', DATA_COMPRESSION = ' + @IndexDataCompression;
				END
				-- WITH OPTIONS PROCESSING END
			END
		END
		SET @createIndexStatement = CASE WHEN @IsSystemNamed = 1 THEN '' ELSE 'CONSTRAINT [' + @IndexName + ']' END +
									CASE WHEN @ConstraintType = 'PK' THEN ' PRIMARY KEY ' ELSE CASE WHEN @IndexIsUnique = 1 THEN ' UNIQUE ' ELSE ' ' END END +
									+ @IndexType
									+ char(13) + '(' + char(13)
									+ @keyColumns + char(13)
									+ ')'
									+ CASE WHEN @withOptions <> '' THEN  'WITH ( ' + @withOptions + ' ) ' ELSE '' END
									+ CASE WHEN @DataSpaceName IS NOT NULL THEN ' ON [' + @DataSpaceName + ']' + CASE WHEN @partitionColumns <> '' THEN '('+@partitionColumns+')' ELSE '' END ELSE '' END;
									/*CASE WHEN @ConstraintType = 'PK' THEN 'PRIMARY KEY ' 
									ELSE 'CONSTRAINT ' + @IndexName + CASE WHEN @IndexIsUnique = 1 THEN ' UNIQUE ' 
																	  ELSE ' ' END
									END 
									+ @IndexType
									+ char(13) + '(' + char(13)
									+ @keyColumns + char(13)
									+ ')'
									+ CASE WHEN @withOptions <> ''   THEN  'WITH ( ' + @withOptions + ' ) ' ELSE '' END
									+ CASE WHEN @DataSpaceName IS NOT NULL THEN ' ON [' + @DataSpaceName + ']' + CASE WHEN @partitionColumns <> '' THEN '('+@partitionColumns+')' ELSE '' END ELSE '' END;*/
		CLOSE aCursor
		DEALLOCATE aCursor
		SELECT @createIndexStatement
GO 
