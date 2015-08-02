import java.io.PrintStream;
import java.nio.charset.Charset;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;

enum OutputForms {

    CSV {
        /**
         * Output the result set in CSV format.
         * BLOB and CLOB values are not supported.
         */
        void output(ResultSet rs, PrintStream console) throws Exception {
            CSVPrinter printer = CSVFormat.DEFAULT.withHeader(rs).print(console);
            printer.printRecords(rs);
            printer.flush();
        }

    },
    TSV {
        /**
         * Output the result set in TSV format.
         * BLOB and CLOB values are not supported.
         */
        void output(ResultSet rs, PrintStream console) throws Exception {
            CSVPrinter printer = CSVFormat.TDF.withHeader(rs).print(console);
            printer.printRecords(rs);
            printer.flush();
        }

    },
    OLDTSV {
        /**
         * Output the result set in TSV format with \N for nulls.
         */
        void output(ResultSet rs, PrintStream console) throws Exception {
            ResultSetMetaData rsMd = rs.getMetaData();
            int columnCount = rsMd.getColumnCount();
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    console.print(value == null ? "\\N" : value);
                    if (i == columnCount) {
                        console.println();
                    } else {
                        console.print("\t");
                    }
                }
            }
        }
    },
    EXCEL {
        /**
         * Output the result set in Excel Open XML format.
         */
        void output(ResultSet rs, PrintStream console) throws Exception {
            ExcelOutput eo = new ExcelOutput();
            eo.output(rs, console);
        }
    },
    ACCESSXML {
        /**
         * Output the result in MS-Access compatible XML.
         * FIXME: Convert spaces in column names to _x0020_
         */
        void output(ResultSet rs, PrintStream console) throws Exception {
            String dataSet = "dataroot";
            XmlWriter xmlWriter;
            xmlWriter = new XmlWriter(console, "UTF-8");
            xmlWriter.enablePrettyPrint(true);
            xmlWriter.writeDeclaration();
            xmlWriter.writeElement(dataSet);

            ResultSetMetaData rsMd = rs.getMetaData();
            int columnCount = rsMd.getColumnCount();
            String tableName = rsMd.getTableName(1);
            tableName = (tableName == null || "".equals(tableName)) ? "row" : tableName;

            while (rs.next()) {
                xmlWriter.writeElement(tableName);
                for (int columnNum = 1; columnNum <= columnCount; columnNum++) {
                    String value = getColumnValue(rs, columnNum);
                    if (value == null) {
                        continue;
                    }
                    String columnName = rsMd.getColumnName(columnNum);
                    xmlWriter.writeElementWithText(columnName, value);
                }
                xmlWriter.endElement();
            }
            xmlWriter.endElement();
            xmlWriter.close();
        }
    },
    FLATXML {
        /**
         * Output the result in flat XML.
         */
        void output(ResultSet rs, PrintStream console) throws Exception {
            String dataSet = "dataset";
            XmlWriter xmlWriter;
            xmlWriter = new XmlWriter(console, "UTF-8");
            xmlWriter.enablePrettyPrint(true);
            xmlWriter.writeDeclaration();
            xmlWriter.writeElement(dataSet);

            ResultSetMetaData rsMd = rs.getMetaData();
            int columnCount = rsMd.getColumnCount();
            String tableName = rsMd.getTableName(1);
            tableName = (tableName == null || "".equals(tableName)) ? "row" : tableName;

            while (rs.next()) {
                xmlWriter.writeElement(tableName);
                for (int columnNum = 1; columnNum <= columnCount; columnNum++) {
                    String value = getColumnValue(rs, columnNum);
                    if (value == null) {
                        continue;
                    }
                    String columnName = rsMd.getColumnName(columnNum);
                    xmlWriter.writeAttribute(columnName, value, true);
                }
                xmlWriter.endElement();
            }
            xmlWriter.endElement();
            xmlWriter.close();
        }
    };

    void output(ResultSet rs, PrintStream console) throws Exception {
    }

    /**
     * Convert column values to string as the library's getString() doesn't do the job well enough.
     *
     * @param rs - the result set at the right row.
     * @param columnNum - the number of the column to get the value from.
     */
    private static String getColumnValue(ResultSet rs, int columnNum) throws SQLException {
        Object value = rs.getObject(columnNum);
        if (value == null) {
            return null;
        }
        if (value instanceof Clob) {
            Clob tValue = (Clob) value;
            return tValue.getSubString(1, (int) tValue.length());
        } else if (value instanceof Blob) {
            // There is no guarantee that we'll get text data from a BLOB.
            Blob tValue = (Blob) value;
            return new String(tValue.getBytes(1, (int) tValue.length()), Charset.forName("UTF-8"));
        } else if (value instanceof byte[]) {
            return new String((byte[]) value, Charset.forName("UTF-8"));
        }

        return rs.getString(columnNum); // Fallback
    }
}
