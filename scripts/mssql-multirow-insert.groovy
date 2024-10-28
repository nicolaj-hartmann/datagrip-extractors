/**
 * MSSQL Multi-Row Insert
 * =======================
 * Inspired/Redone based on https://gist.github.com/ProjectCleverWeb/d2362b082af1d7054ebfd464f202ec1b
 * See:
 *
 * @author Nicolaj Helmer Hartmann
 * @license MIT
 *
 * Available context bindings:
 *   COLUMNS     List<DataColumn>
 *   ROWS        Iterable<DataRow>
 *   OUT         { append() }
 *   FORMATTER   { format(row, col); formatValue(Object, col) }
 * plus ALL_COLUMNS, TABLE, DIALECT
 *
 * where:
 *   DataRow     { rowNumber(); first(); last(); data(): List<Object>; value(column): Object }
 *   DataColumn  { columnNumber(), name() }
 */

BATCH_SIZE          = 1000
DEFAULT_TABLE_NAME  = "TABLE_NAME"
MINIFY              = false
COLUMNS_ON_ONE_LINE = true
SHOW_CONFIG         = true
INDENT              = "\t"
VALUE_QUOTE         = "\'"
COLUMN_QUOTE_START  = "["
COLUMN_QUOTE_END  = "]"
TABLE_QUOTE         = ""
REAL_EOL            = System.getProperty("line.separator") // End of line delimiter
EOL                 = REAL_EOL
OB                  = "" // Output Buffer Variable
RI                  = 0 // Row Iterator Variable
BI                  = 0 // Batch Iterator Variable
VALUE_DELIMITER     = ","
COLUMN_DELIMITER    = ","
STATEMENT_DELIMITER = ";"
COLUMN_INDENT       = INDENT
ROW_INDENT          = ""
KEYWORDS_LOWERCASE  = com.intellij.database.util.DbSqlUtil.areKeywordsLowerCase(PROJECT)
KW_INSERT_INTO      = KEYWORDS_LOWERCASE ? "insert into " : "INSERT INTO "
KW_VALUES           = KEYWORDS_LOWERCASE ? "values" : "VALUES"
KW_NULL             = KEYWORDS_LOWERCASE ? "null" : "NULL"
KW_TRUE             = KEYWORDS_LOWERCASE ? "true" : "TRUE"
KW_FALSE            = KEYWORDS_LOWERCASE ? "false" : "FALSE"
LAST_INDEX          = COLUMNS.size() - 1

// Main loop function
def mmli() {
	ROWS.each { row ->
		mmliPrintRow(COLUMNS, row)
	}
	OUT.append(
		STATEMENT_DELIMITER + REAL_EOL + REAL_EOL +
		"-- Inserts Created: " + RI + REAL_EOL +
		"-- Batches Created: " + BI + REAL_EOL
	)
}

// Generate table name, with parent if it is available
if (TABLE != null) {
	TABLE_PARENT = TABLE.getParent().getName()
	if (TABLE_PARENT != null && TABLE_PARENT != "") {
		TABLE_NAME += TABLE_QUOTE + TABLE_PARENT + TABLE_QUOTE + "."
	}
	TABLE_NAME += TABLE_QUOTE + TABLE.getName() + TABLE_QUOTE
} else {
	TABLE_NAME += TABLE_QUOTE + DEFAULT_TABLE_NAME + TABLE_QUOTE
}

// Show config in comments
if (SHOW_CONFIG) {
	OB += "-- Batch Size: " + BATCH_SIZE + EOL
	// OB += "-- Table: " + TABLE_NAME + EOL
	OB += "-- Minify: " + (MINIFY ? KW_TRUE : KW_FALSE) + EOL
	OB += "-- Indent: '" + INDENT + "'" + EOL
	OB += EOL
}

// Make extra whitespace empty strings to minify
if (MINIFY) {
	EOL           = ""
	COLUMN_INDENT = ""
	ROW_INDENT    = ""
}

// Handles batching & printing the buffer.
def mmliPrintRow(columns, row) {
	if (RI % BATCH_SIZE == 0) {
		// Handle a beginning row
		BI++ // Count batches
		if (RI != 0) {
			// Separate each new statement
			OB += STATEMENT_DELIMITER + REAL_EOL + REAL_EOL
		}
		mmliHandleBeginStatement(columns)
	} else {
		// Handle a continuation row
		OB += VALUE_DELIMITER + EOL
	}

	// Handle a row's column data
	mmliHandleRow(columns, row)

	// Print, clear buffer, & increment row count
	OUT.append(OB)
	OB = ''
	RI++
}

// Print the beginning of an insert
def mmliHandleBeginStatement(columns) {
	OB += KW_INSERT_INTO + TABLE_NAME + " (" + EOL + COLUMN_INDENT

	// Loop through each column and print its name
	columns.eachWithIndex { column, index ->
		OB += COLUMN_QUOTE_START + column.name() + COLUMN_QUOTE_END
		if (index != LAST_INDEX) {
			OB += COLUMN_DELIMITER
			if (!COLUMNS_ON_ONE_LINE) {
				OB += EOL + COLUMN_INDENT
			}
		}
	}

	OB += EOL + ")" + EOL + KW_VALUES + EOL
}

// Print the values to insert
def mmliHandleRow(columns, row) {
	OB += ROW_INDENT + "(" // open new row

	// Loop through each column and print its value
	columns.eachWithIndex { column, index ->
		def stringValue = FORMATTER.format(row, column)
		def rawValue    = row.value(column)

        if (!DIALECT.getDbms().isMicrosoft()) {
            throw new IllegalStateException("Dialect is not Microsoft - This script only works for Azure Sql Server or Microsoft Sql Server")
        }
		stringValue = stringValue.replace(VALUE_QUOTE, VALUE_QUOTE + VALUE_QUOTE)

		// Determine value type so it can be printed correctly
		if (rawValue instanceof Boolean) {
			OB += rawValue ? KW_TRUE : KW_FALSE
		} else if (rawValue == null) {
			OB += KW_NULL
		} else if (rawValue.toString().matches("[0-9.-]+")) {
			OB += stringValue
		} else {
			OB += VALUE_QUOTE + stringValue + VALUE_QUOTE
		}

		// Delimit each value
		if (index != LAST_INDEX) {
			OB += VALUE_DELIMITER
		}
	}
	OB += ")" // close new row
}

// Run
mmli()