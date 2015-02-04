import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.database.search.TablesDependencyHelper;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;

public class FlatXmlExport {

    /**
     * Utility method that populates the given properties from the given file path.
     *
     * @param properties
     *         - The properties object
     * @param filePath
     *         - The file path to load the properties from
     * @throws IOException
     *             - if the properties file is missing
     */
    private static void loadProperties(Properties properties, String filePath) throws IOException {

        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            FileInputStream inputStream = null;
            try {
                inputStream = new FileInputStream(file);
                properties.load(inputStream);
            } finally {
                inputStream.close();
            }
        } else {
            throw new IllegalArgumentException("Failed to find input properties at " + filePath);
        }
    }

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        loadProperties(props, "database.properties");

        // database connection
        Class driverClass = Class.forName(props.getProperty("db.driver"));
        String connectionUrl = props.getProperty("db.database");
        String username = props.getProperty("db.user");
        String password = props.getProperty("db.password");
        Connection jdbcConnection = DriverManager.getConnection(connectionUrl, username, password);
        IDatabaseConnection connection = new DatabaseConnection(jdbcConnection);

        // partial database export
        QueryDataSet partialDataSet = new QueryDataSet(connection);
        String tablesProperty = props.getProperty("tables");
        String[] tables = new String[0];

        if (tablesProperty != null && !tablesProperty.isEmpty()) {
            tables = tablesProperty.split("\\s+");
        }

        for (String table : tables) {
            String query = props.getProperty(table + ".query");
            if (query == null) {
                partialDataSet.addTable(table);
            } else {
                partialDataSet.addTable(table, query);
            }
        }
        FlatXmlDataSet.write(partialDataSet, new FileOutputStream("dataset.xml"));

        // full database export
/*
        IDataSet fullDataSet = connection.createDataSet();
        FlatXmlDataSet.write(fullDataSet, new FileOutputStream("full.xml"));
        
        // dependent tables database export: export table X and all tables that
        // have a PK which is a FK on X, in the right order for insertion
        String[] depTableNames = TablesDependencyHelper.getAllDependentTables( connection, "X" );
        IDataSet depDataset = connection.createDataSet( depTableNames );
        FlatXmlDataSet.write(depDataset, new FileOutputStream("dependents.xml"));          
*/
    }
}

