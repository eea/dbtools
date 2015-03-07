
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;


/**
 * Investigate a database.
 */
public class ListTables {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        Connection con = null;

        con = Util.getConnection();

        DatabaseMetaData dbMetadata = con.getMetaData();

        ResultSet rs = null;
        rs = dbMetadata.getTables(null, null, "%", new String[] {"TABLE"});
        while (rs.next()) {
            String tableCatalog = rs.getString(1);
            String tableSchema = rs.getString(2);
            String tableName = rs.getString(3);
            System.out.printf("%s.%s.%s\n", tableCatalog, tableSchema, tableName);
        }
        con.close();
    }
}
