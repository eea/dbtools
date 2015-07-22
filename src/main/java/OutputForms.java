import java.io.PrintStream;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Date;

import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

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
            ResultSetMetaData rsMd = rs.getMetaData();
            int columnCount = rsMd.getColumnCount();
            Workbook wb = new XSSFWorkbook();
            CreationHelper createHelper = wb.getCreationHelper();
            XSSFSheet sheet = (XSSFSheet) wb.createSheet();
            String tableName = rsMd.getTableName(1);
            tableName = (tableName == null || "".equals(tableName)) ? "Output" : tableName;
            wb.setSheetName(0, tableName);
            CellStyle timeStyle = wb.createCellStyle();
            timeStyle.setDataFormat(createHelper.createDataFormat().getFormat("d/m/yy h:mm"));
            // Make the header line
            short rowNum = 0;
            Row row = sheet.createRow(rowNum++);
            for (int colNum = 1; colNum <= columnCount; colNum++) {
                String columnName = rsMd.getColumnName(colNum);
                Cell cell = row.createCell(colNum - 1);
                cell.setCellValue(columnName);
            }
            // Data
            while (rs.next()) {
                row = sheet.createRow(rowNum++);
                for (int colNum = 1; colNum <= columnCount; colNum++) {
                    Cell cell = row.createCell(colNum - 1);
                    Object rawValue = rs.getObject(colNum);
                    writeCell(cell, rawValue, timeStyle);
                }
            }
            wb.write(console);
        }

        private void writeCell(Cell cell, Object rawValue, CellStyle timeStyle) throws Exception {
            if (rawValue == null) {
                cell.setCellValue((String) null);
            } else if (rawValue instanceof Boolean) {
                cell.setCellValue(((Boolean)rawValue).booleanValue());
            } else if (rawValue instanceof Double) {
                cell.setCellValue(((Double)rawValue).doubleValue());
            } else if (rawValue instanceof Integer) {
                cell.setCellValue(((Integer)rawValue).intValue());
            } else if (rawValue instanceof Date) {
                cell.setCellValue(((Date)rawValue));
                cell.setCellStyle(timeStyle);
            } else if (rawValue instanceof Clob) {
                Clob tValue = (Clob) rawValue;
                cell.setCellValue(tValue.getSubString(1, (int) tValue.length()));
            } else {
                cell.setCellValue(rawValue.toString());
            }
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
