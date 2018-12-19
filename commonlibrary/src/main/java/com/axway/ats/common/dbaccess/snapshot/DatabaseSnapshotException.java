/*
 * Copyright 2017 Axway Software
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.axway.ats.common.dbaccess.snapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.dbaccess.snapshot.equality.DatabaseEqualityState;

/**
 * Error while working with Database Snapshot
 */
@PublicAtsApi
public class DatabaseSnapshotException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private static final String DEFAULT_DIFF_STRING_OUTPUT_FORMAT = "\n\t\t[E]\n\t\t[A]";

	private DatabaseEqualityState equality;

	public static final String DEFAULT_DB_DELIMITER = ", ";

	public DatabaseSnapshotException(String arg0) {

		super(arg0);
	}

	public DatabaseSnapshotException(Throwable arg0) {

		super(arg0);
	}

	public DatabaseSnapshotException(String arg0, Throwable arg1) {

		super(arg0, arg1);
	}

	public DatabaseSnapshotException(DatabaseEqualityState equality) {

		this.equality = equality;
	}

	/**
	 * This can be used to retrieve the compare result and then make some custom
	 * compare report.
	 * 
	 * @return the result of compare
	 */
	@PublicAtsApi
	public DatabaseEqualityState getEqualityState() {

		return this.equality;
	}

	@Override
	@PublicAtsApi
	public String getMessage() {

		if (equality == null) {
			// got a generic exception, not directly concerning the comparision
			return super.getMessage();
		} else {
			String firstSnapshot = equality.getFirstSnapshotName();
			String secondSnapshot = equality.getSecondSnapshotName();

			StringBuilder msg = new StringBuilder();
			msg.append("Comparing [");
			msg.append(firstSnapshot);
			msg.append("] and [");
			msg.append(secondSnapshot);
			msg.append("] produced the following unexpected differences:");

			addInfoAboutTablesInOneSnapshotOnly(msg, equality, firstSnapshot);
			addInfoAboutTablesInOneSnapshotOnly(msg, equality, secondSnapshot);

			addInfoAboutDifferentPrimaryKeys(msg, equality, firstSnapshot, secondSnapshot);

			addInfoAboutDifferentNumberOfRows(msg, equality, firstSnapshot, secondSnapshot);

			addInfoAboutColumnsInOneSnapshotOnly(msg, equality, firstSnapshot, secondSnapshot);

			addInfoAboutIndexesInOneSnapshotOnly(msg, equality, firstSnapshot, secondSnapshot);

			addInfoAboutRowsInOneSnapshotOnly(msg, equality, firstSnapshot, secondSnapshot);

			msg.append("\n");
			return msg.toString();
		}
	}

	private void addInfoAboutTablesInOneSnapshotOnly(StringBuilder msg, DatabaseEqualityState equality,
			String snapshot) {

		StringBuilder tablesInOneSnapshotOnly = new StringBuilder();
		for (String table : equality.getTablesPresentInOneSnapshotOnly(snapshot)) {
			tablesInOneSnapshotOnly.append("\n\t");
			tablesInOneSnapshotOnly.append(table);
		}

		if (tablesInOneSnapshotOnly.length() > 0) {
			msg.append("\nTables present in [" + snapshot + "] only:");
			msg.append(tablesInOneSnapshotOnly);
		}
	}

	private void addInfoAboutDifferentPrimaryKeys(StringBuilder msg, DatabaseEqualityState equality,
			String firstSnapshot, String secondSnapshot) {

		Set<String> tables = new HashSet<>();
		tables.addAll(equality.getTablesWithDifferentPrimaryKeys(firstSnapshot));
		tables.addAll(equality.getTablesWithDifferentPrimaryKeys(secondSnapshot));

		if (tables.size() > 0) {
			msg.append("\nDifferent primary keys:");

			for (String table : tables) {
				String firstPrimaryKey = equality.getDifferentPrimaryKeys(firstSnapshot, table);
				String secondPrimaryKey = equality.getDifferentPrimaryKeys(secondSnapshot, table);
				msg.append("\n\ttable '" + table + "', primary key column in [" + firstSnapshot + "] is '"
						+ firstPrimaryKey + "', while in [" + secondSnapshot + "] is '" + secondPrimaryKey + "'");

				msg.append("\n\tDifferences: " + StringDiff.getDifferences(firstPrimaryKey, secondPrimaryKey,
						DEFAULT_DB_DELIMITER, DEFAULT_DIFF_STRING_OUTPUT_FORMAT));

			}
		}
	}

	private void addInfoAboutDifferentNumberOfRows(StringBuilder msg, DatabaseEqualityState equality,
			String firstSnapshot, String secondSnapshot) {

		Set<String> tables = new HashSet<>();
		tables.addAll(equality.getTablesWithDifferentNumberOfRows(firstSnapshot));
		tables.addAll(equality.getTablesWithDifferentNumberOfRows(secondSnapshot));

		if (tables.size() > 0) {
			msg.append("\nDifferent number of rows" + ":");

			for (String table : tables) {
				msg.append("\n\ttable '" + table + "', " + equality.getDifferentNumberOfRows(firstSnapshot, table)
						+ " rows in [" + firstSnapshot + "] and "
						+ equality.getDifferentNumberOfRows(secondSnapshot, table) + " in [" + secondSnapshot + "]");
			}
		}
	}

	private void addInfoAboutColumnsInOneSnapshotOnly(StringBuilder msg, DatabaseEqualityState equality,
			String firstSnapshot, String secondSnapshot) {

		Set<String> tables = new HashSet<>();
		tables.addAll(equality.getTablesWithColumnsPresentInOneSnapshotOnly(firstSnapshot));
		tables.addAll(equality.getTablesWithColumnsPresentInOneSnapshotOnly(secondSnapshot));

		if (tables.size() > 0) {
			for (String table : tables) {
				// add different columns for one table at a time
				msg.append("\nTable columns for '" + table + "' table:");

				List<String> firstColumns = equality.getColumnsPresentInOneSnapshotOnlyAsStrings(firstSnapshot, table);
				if (firstColumns != null && firstColumns.size() > 0) {
					msg.append("\n\t[" + firstSnapshot + "]:");
					for (String column : firstColumns) {
						msg.append("\n\t\t" + column);
					}
				}

				List<String> secondColumns = equality.getColumnsPresentInOneSnapshotOnlyAsStrings(secondSnapshot,
						table);
				if (secondColumns != null && secondColumns.size() > 0) {
					msg.append("\n\t[" + secondSnapshot + "]:");
					for (String column : secondColumns) {
						msg.append("\n\t\t" + column);
					}
				}

				// check if columns from both snapshots for the current table are the same
				// number
				if (firstColumns.size() > secondColumns.size()) {

					int sizeDiff = firstColumns.size() - secondColumns.size();
					for (int i = 0; i < sizeDiff; i++) {
						secondColumns.add("");
					}

				} else if (firstColumns.size() < secondColumns.size()) {

					int sizeDiff = secondColumns.size() - firstColumns.size();
					for (int i = 0; i < sizeDiff; i++) {
						firstColumns.add("");
					}

				}

				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < firstColumns.size(); i++) {
					String firstColumn = firstColumns.get(i);
					String secondColumn = secondColumns.get(i);
					if (!sb.toString().startsWith("\n\tDifferences:")) {
						sb.append("\n\tDifferences: " + StringDiff.getDifferences(firstColumn, secondColumn,
								DEFAULT_DB_DELIMITER, DEFAULT_DIFF_STRING_OUTPUT_FORMAT));
					} else {
						sb.append(StringDiff.getDifferences(firstColumn, secondColumn, DEFAULT_DB_DELIMITER,
								DEFAULT_DIFF_STRING_OUTPUT_FORMAT));
					}
				}
				msg.append(sb.toString());
			}
		}
	}

	private void addInfoAboutIndexesInOneSnapshotOnly(StringBuilder msg, DatabaseEqualityState equality,
			String firstSnapshot, String secondSnapshot) {

		Set<String> tables = new HashSet<>();
		tables.addAll(equality.getTablesWithIndexesPresentInOneSnapshotOnly(firstSnapshot));
		tables.addAll(equality.getTablesWithIndexesPresentInOneSnapshotOnly(secondSnapshot));

		if (tables.size() > 0) {
			for (String table : tables) {
				// add different indexes for one table at a time
				msg.append("\nTable indexes for '" + table + "' table:");

				List<String> firstIndexes = equality.getIndexesPresentInOneSnapshotOnlyAsStrings(firstSnapshot, table);
				msg.append("\n\t[" + firstSnapshot + "]:");
				for (String index : firstIndexes) {
					msg.append("\n\t\t" + index);
				}

				List<String> secondIndexes = equality.getIndexesPresentInOneSnapshotOnlyAsStrings(secondSnapshot,
						table);
				msg.append("\n\t[" + secondSnapshot + "]:");
				for (String index : secondIndexes) {
					msg.append("\n\t\t" + index);
				}

				// check if indexes from both snapshots for the current table are the same
				// number
				if (firstIndexes.size() > secondIndexes.size()) {

					int sizeDiff = firstIndexes.size() - secondIndexes.size();
					for (int i = 0; i < sizeDiff; i++) {
						secondIndexes.add("");
					}

				} else if (firstIndexes.size() < secondIndexes.size()) {

					int sizeDiff = secondIndexes.size() - firstIndexes.size();
					for (int i = 0; i < sizeDiff; i++) {
						firstIndexes.add("");
					}

				}

				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < firstIndexes.size(); i++) {
					String firstIndex = firstIndexes.get(i);
					String secondIndex = secondIndexes.get(i);
					if (!sb.toString().startsWith("\n\tDifferences:")) {
						sb.append("\n\tDifferences: " + StringDiff.getDifferences(firstIndex, secondIndex,
								DEFAULT_DB_DELIMITER, DEFAULT_DIFF_STRING_OUTPUT_FORMAT));
					} else {
						sb.append(StringDiff.getDifferences(firstIndex, secondIndex, DEFAULT_DB_DELIMITER,
								DEFAULT_DIFF_STRING_OUTPUT_FORMAT));
					}
				}
				msg.append(sb.toString());
			}
		}
	}

	private void addInfoAboutRowsInOneSnapshotOnly(StringBuilder msg, DatabaseEqualityState equality,
			String firstSnapshot, String secondSnapshot) {

		Set<String> tables = new HashSet<>();
		tables.addAll(equality.getTablesWithRowsPresentInOneSnapshotOnly(firstSnapshot));
		tables.addAll(equality.getTablesWithRowsPresentInOneSnapshotOnly(secondSnapshot));

		if (tables.size() > 0) {
			for (String table : tables) {
				// add different rows for one table at a time
				msg.append("\nTable rows for '" + table + "' table:");

				List<String> firstRows = equality.getRowsPresentInOneSnapshotOnlyAsStrings(firstSnapshot, table);
				if (firstRows != null && firstRows.size() > 0) {
					msg.append("\n\t[" + firstSnapshot + "]:");
					for (String row : firstRows) {
						msg.append("\n\t\t" + row);
					}
				}

				List<String> secondRows = equality.getRowsPresentInOneSnapshotOnlyAsStrings(secondSnapshot, table);
				if (secondRows != null && secondRows.size() > 0) {
					msg.append("\n\t[" + secondSnapshot + "]:");
					for (String row : secondRows) {
						msg.append("\n\t\t" + row);
					}
				}

				// check if indexes from both snapshots for the current table are the same
				// number
				if (firstRows.size() > secondRows.size()) {

					int sizeDiff = firstRows.size() - secondRows.size();
					for (int i = 0; i < sizeDiff; i++) {
						secondRows.add("");
					}

				} else if (firstRows.size() < secondRows.size()) {

					int sizeDiff = secondRows.size() - firstRows.size();
					for (int i = 0; i < sizeDiff; i++) {
						firstRows.add("");
					}

				}

				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < firstRows.size(); i++) {
					String firstRow = firstRows.get(i);
					String secondRow = secondRows.get(i);
					if (!sb.toString().startsWith("\n\tDifferences:")) {
						sb.append("\n\tDifferences: " + StringDiff.getDifferences(firstRow, secondRow,
								DEFAULT_DB_DELIMITER, DEFAULT_DIFF_STRING_OUTPUT_FORMAT));
					} else {
						sb.append(StringDiff.getDifferences(firstRow, secondRow, DEFAULT_DB_DELIMITER,
								DEFAULT_DIFF_STRING_OUTPUT_FORMAT));
					}
				}
				msg.append(sb.toString());
			}
		}
	}

	static class StringDiff {

		/**
		 * Compare and return String differences in the format EXPECTED < STRING_A > BUT
		 * WAS < STRING_B >
		 * 
		 * @param expected
		 *            the expected String
		 * @param actual
		 *            the actual String
		 * @param delimiter
		 *            use this parameter to specify a delimiter for tokenizing the
		 *            Strings. This produces better visibility of the differences. Pass
		 *            empty string ("") to disable this option
		 * @param outputFormat
		 *            specify additional spaces,tabs, etc to be included in the final
		 *            diff String. Example:</br>
		 *            outputFormat = <strong>'\t[E]\t\n[A]'</strong> will return the
		 *            following String: tab then EXPECTED VALUE then tab then new line
		 *            then ACTUAL VALUE
		 * 
		 */
		public static String getDifferences(String expected, String actual, String delimiter, String outputFormat) {

			final String EXPECTED_PLACEHOLDER = "__ATS_EXPECTED_PLACEHOLDER__";
			final String ACTUAL_PLACEHOLDER = "__ATS_ACTUAL_PLACEHOLDER__";

			StringBuilder finalSb = null;
			if (outputFormat != null && !outputFormat.equals("")) {

				finalSb = new StringBuilder(outputFormat.replace("[E]", "EXPECTED < " + EXPECTED_PLACEHOLDER + " >")
						.replace("[A]", "BUT WAS < " + ACTUAL_PLACEHOLDER + " >"));

			} else {
				finalSb = new StringBuilder(
						"EXPECTED < " + EXPECTED_PLACEHOLDER + " > \nBUT WAS  < " + ACTUAL_PLACEHOLDER + " >");
			}

			StringBuilder expectedSb = new StringBuilder();
			StringBuilder actualSb = new StringBuilder();

			List<String> tokensExpected = new ArrayList<>();
			tokensExpected.addAll(Arrays.asList(expected.split(delimiter)));
			List<String> tokensActual = new ArrayList<>();
			tokensActual.addAll(Arrays.asList(actual.split(delimiter)));

			if (tokensExpected.size() < tokensActual.size()) {
				for (int k = 0; k < (tokensActual.size() - tokensExpected.size()); k++) {
					tokensExpected.add("");
				}
			} else if (tokensExpected.size() > tokensActual.size()) {
				for (int k = 0; k < (tokensExpected.size() - tokensActual.size()); k++) {
					tokensActual.add("");
				}
			}

			for (int j = 0; j < tokensExpected.size(); j++) {
				String[] diffs = compareStrings(tokensExpected.get(j), tokensActual.get(j));
				if (diffs == null) {
					expectedSb.append(tokensExpected.get(j));
					actualSb.append(tokensActual.get(j));
				} else {
					expectedSb.append(diffs[0]);
					actualSb.append(diffs[1]);
				}
				if (tokensExpected.size() > j + 1) {
					expectedSb.append(delimiter);
					actualSb.append(delimiter);

					if (expectedSb.length() > actualSb.length()) {
						int sizeDiff = expectedSb.length() - actualSb.length();
						for (int k = 0; k < sizeDiff; k++) {
							actualSb.append(" ");
						}
					} else if (expectedSb.length() < actualSb.length()) {
						int sizeDiff = actualSb.length() - expectedSb.length();
						for (int k = 0; k < sizeDiff; k++) {
							expectedSb.append(" ");
						}
					}
				}
			}

			String finalDiff = finalSb.toString().replace(EXPECTED_PLACEHOLDER, expectedSb.toString())
					.replace(ACTUAL_PLACEHOLDER, actualSb.toString());

			return finalDiff;

		}

		private static String[] compareStrings(String a, String b) {

			final String paddingSym = "д";

			if (a == null) {
				return new String[] { "[" + a + "]", "[" + b + "]" };
			} else if (b == null) {
				return new String[] { "[" + a + "]", "[" + b + "]" };
			} else {

				if (a.length() > b.length()) {

					StringBuffer tmpB = new StringBuffer(b);

					for (int i = 0; i < (a.length() - b.length()); i++) {
						tmpB.append(paddingSym);
					}

					b = tmpB.toString();

				} else if (a.length() < b.length()) {

					StringBuffer tmpA = new StringBuffer(a);

					for (int i = 0; i < (b.length() - a.length()); i++) {
						tmpA.append(paddingSym);
					}

					a = tmpA.toString();

				}

				if (a.length() == 1 && b.length() == 1) {
					a = a + paddingSym;
					b = b + paddingSym;
				}

				int diffStartIdx = -1;
				boolean atleastOneDiff = false;
				for (int i = 0; i < a.length(); i++) {
					char chA = a.charAt(i);
					char chB = b.charAt(i);

					if (chA != chB) {
						atleastOneDiff = true;
						if (diffStartIdx == -1) {
							diffStartIdx = i;
						}
					} else {
						if (diffStartIdx != -1) {

							a = a.substring(0, diffStartIdx) + "[" + a.substring(diffStartIdx, i) + "]"
									+ a.substring(i);

							b = b.substring(0, diffStartIdx) + "[" + b.substring(diffStartIdx, i) + "]"
									+ b.substring(i);

							diffStartIdx = -1;
						}
					}
				}

				if (diffStartIdx != -1) {

					a = a.substring(0, diffStartIdx) + "[" + a.substring(diffStartIdx, a.length()) + "]"
							+ a.substring(a.length());

					b = b.substring(0, diffStartIdx) + "[" + b.substring(diffStartIdx, b.length()) + "]"
							+ b.substring(b.length());
				}

				if (atleastOneDiff) {
					return new String[] { a = a.replace(paddingSym, ""), b.replace(paddingSym, "") };
				} else {
					return null;
				}

			}
		}

	}
}
