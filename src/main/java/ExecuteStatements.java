
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Options;


/**
 * Execute a set of statements on a database. Does not print the output.
 */
public class ExecuteStatements {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {

        Options options = new Options();
        options.addOption("t", false, "Execute as one transaction");
        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args);

        Connection con = Util.getConnection();
        if (cmd.hasOption("t")) {
            con.setAutoCommit(false);
        }

        Statement st = con.createStatement();

        Properties props = Util.getProperties();
        TreeSet<String> sortedProps = new TreeSet<String>(props.stringPropertyNames());

        for (String key : sortedProps) {
            if (key.startsWith("execute.")) {
                String query = props.getProperty(key);
                ResultSet res = st.executeQuery(query);
            }
        }
        if (cmd.hasOption("t")) {
            con.commit();
        }

        con.close();
    }
}
