import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class Util {

    private static Properties props = new Properties();

    static {
        props = new Properties();
        try {
            loadProperties(props, "database.properties");
        } catch (IOException e) {
        }
    }

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

    public static Connection getConnection() throws Exception {

        Class driverClass = Class.forName(props.getProperty("db.driver"));
        String connectionUrl = props.getProperty("db.database");
        String username = props.getProperty("db.user");
        String password = props.getProperty("db.password");
        return DriverManager.getConnection(connectionUrl, username, password);
    }


    public static String[] getTables() {
        String tablesProperty = props.getProperty("tables");
        String[] tables = new String[0];

        if (tablesProperty != null && !tablesProperty.isEmpty()) {
            tables = tablesProperty.split("\\s+");
        }
        return tables;
    }

    public static String getQuery(String table) {
        return props.getProperty(table + ".query");
    }
}

