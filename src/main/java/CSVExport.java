
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;


/**
 * A CSV export fit for liquibase.
 */
public class CSVExport {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        Connection con = null;
        FileWriter fw ;

        con = Util.getConnection();
        Statement st = con.createStatement();

        String[] tableNameList = Util.getTables();
        String lineSeparator = System.getProperty("line.separator");

        //star iterating on each table to fetch its data and save in a .csv file
        for (String tableName : tableNameList) {
            System.out.println(tableName);

            List<String> columnsNameList  = new ArrayList<String>();

            String query = Util.getQuery(tableName);
            if (query == null) {
                query = "SELECT * FROM " + tableName;
            }
            //select all data from table
            ResultSet res = st.executeQuery(query);


            fw = new FileWriter(tableName + ".csv");
            CSVPrinter printer = CSVFormat.DEFAULT.withHeader(res).print(fw);
            printer.printRecords(res);
            printer.flush();
            fw.flush();
            fw.close();
        }
        con.close();
    }
}
