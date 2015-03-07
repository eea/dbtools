
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;


/**
 * Investigate a database.
 */
public class ListTables {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("c", true, "name of catalog to search for");
        options.addOption("s", true, "name of schema to search for");
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args);
        // getOptionValue returns null if option is not used
        String catalogPattern = cmd.getOptionValue("c");
        String schemaPattern = cmd.getOptionValue("s");
        
        Connection con = Util.getConnection();

        DatabaseMetaData dbMetadata = con.getMetaData();

        ResultSet rs = null;
        rs = dbMetadata.getTables(catalogPattern, schemaPattern, "%", new String[] {"TABLE", "VIEW"});
        while (rs.next()) {
            String tableCatalog = rs.getString(1);
            String tableSchema = rs.getString(2);
            String tableName = rs.getString(3);
            String tableType = rs.getString(4);
            System.out.printf("%s %s.%s.%s\n", tableType, tableCatalog, tableSchema, tableName);
        }
        con.close();
    }
}
