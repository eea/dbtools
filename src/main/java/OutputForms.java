import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;

enum OutputForms {
    CSV {
        /**
         * Output the result set in CSV format.
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
    FLATXML {
        /**
         * Output the result in flat xml.
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
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    if (value == null) {
                        continue;
                    }
                    String columnName = rsMd.getColumnName(i);
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
}
