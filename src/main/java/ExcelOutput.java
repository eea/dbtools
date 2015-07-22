import java.io.PrintStream;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

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

class ExcelOutput {

    private CellStyle dateStyle;

    private CellStyle timeStyle;

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
        dateStyle = wb.createCellStyle();
        dateStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy"));
        timeStyle = wb.createCellStyle();
        timeStyle.setDataFormat(createHelper.createDataFormat().getFormat("dd/MM/yyyy HH:mm:ss"));
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
                writeCell(cell, rawValue);
            }
        }
        wb.write(console);
    }

    private void writeCell(Cell cell, Object rawValue) throws Exception {
        if (rawValue == null) {
            cell.setCellValue((String) null);
        } else if (rawValue instanceof Boolean) {
            cell.setCellValue(((Boolean) rawValue).booleanValue());
        } else if (rawValue instanceof Double) {
            cell.setCellValue(((Double) rawValue).doubleValue());
        } else if (rawValue instanceof Integer) {
            cell.setCellValue(((Integer) rawValue).intValue());
        } else if (rawValue instanceof Timestamp) {
            cell.setCellValue(((Timestamp) rawValue));
            cell.setCellStyle(timeStyle);
        } else if (rawValue instanceof Date) {
            cell.setCellValue(((Date) rawValue));
            cell.setCellStyle(dateStyle);
        } else if (rawValue instanceof Clob) {
            Clob tValue = (Clob) rawValue;
            cell.setCellValue(tValue.getSubString(1, (int) tValue.length()));
        } else {
            cell.setCellValue(rawValue.toString());
        }
    }
}
