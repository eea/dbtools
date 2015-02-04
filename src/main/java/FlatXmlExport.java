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

    public static void main(String[] args) throws Exception {

        Connection jdbcConnection = Util.getConnection();
        IDatabaseConnection connection = new DatabaseConnection(jdbcConnection);

        // partial database export
        QueryDataSet partialDataSet = new QueryDataSet(connection);
        String[] tables = new String[0];

        tables = Util.getTables();
        for (String table : tables) {
            String query = Util.getQuery(table);
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

