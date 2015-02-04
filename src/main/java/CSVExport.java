
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


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
	try {
	    con = Util.getConnection();
	    Statement st = con.createStatement();

	    //this query gets all the tables in your database(put your db name in the query)
	    ResultSet res = st.executeQuery("SELECT table_name FROM INFORMATION_SCHEMA.TABLES WHERE table_schema = 'db_name' ");

	    String[] tableNameList = Util.getTables();

	    //star iterating on each table to fetch its data and save in a .csv file
	    for (String tableName : tableNameList) {
		System.out.println(tableName);

		List<String> columnsNameList  = new ArrayList<String>();

                String query = Util.getQuery(tableName);
                if (query == null) {
                    query = "SELECT * FROM " + tableName;
                }
		//select all data from table
		res = st.executeQuery(query);

		//column count is necessay as the tables are dynamic and we need to figure out the numbers of columns
		int columnCount = getColumnCount(res);

		try {
		    fw = new FileWriter(tableName + ".csv");

		    //this loop is used to add column names at the top of file , if you do not need it just comment this loop
		    for (int i = 1; i <= columnCount; i++) {
		        if (i > 1) {
			    fw.append(",");
			}
			fw.append(res.getMetaData().getColumnName(i));
		    }

		    fw.append(System.getProperty("line.separator"));

		    while (res.next()) {
			for (int i = 1; i <= columnCount; i++) {
			    if (i > 1) {
				fw.append(",");
			    }

			    if(res.getObject(i) != null) {
				String data = res.getObject(i).toString();
				if (data.contains(",") || data.contains("\"") || data.contains("\n")) {
				    fw.append("\"");
				    fw.append(data.replace("\"", "\"\"")) ;
				    fw.append("\"");
                                } else {
				    fw.append(data) ;
                                }
			    } else {
				fw.append("NULL") ;
			    }

			}
			//new line entered after each row
			fw.append(System.getProperty("line.separator"));
		    }

		    fw.flush();
		    fw.close();

		  } catch (IOException e) {
		      // TODO Auto-generated catch block
		      e.printStackTrace();
		  }

	      }

	      con.close();
	} catch (ClassNotFoundException e) {
            System.err.println("Could not load JDBC driver");
            e.printStackTrace();
        } catch (SQLException ex) {
            System.err.println("SQLException information");
        }
    }

    //to get numbers of rows in a result set
    public static int getRowCount(ResultSet res) throws SQLException {
          res.last();
          int numberOfRows = res.getRow();
          res.beforeFirst();
          return numberOfRows;
    }

    //to get no of columns in result set
    public static int getColumnCount(ResultSet res) throws SQLException {
        return res.getMetaData().getColumnCount();
    }

}
