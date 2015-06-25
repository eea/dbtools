import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;

import jline.console.history.History;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import org.dbunit.util.xml.XmlWriter;
 
public class CLI {
 
    private String stmtBuf;
    private StmtState lineState = StmtState.START;
    enum OutputForms { CSV, FLATXML }
    private OutputForms outputFormat = OutputForms.CSV;

    /** Active database connection. */
    private Connection connection;

    /**
     * Go through the input line one character at a time to set the next state.
     * A scanner.
     *
     * @param line - input string
     * @return next state.
     */
    public StmtState setNextState(String line) {
        stmtBuf = stmtBuf == null ? line : stmtBuf + "\n" + line;
        char[] charBuf = line.toCharArray();
        for (int i = 0; i < charBuf.length; i++) {
            lineState = lineState.next(lineState, charBuf[i]);
            //console.println(lineState.toString());
        }
        return lineState;
    }

    public StmtState getState() {
        return lineState;
    }

    public void reset() {
        lineState = StmtState.START;
        stmtBuf = null;
    }

    void executeSQLQuery(String query, ConsoleReader console) throws Exception {
        Statement st = connection.createStatement();
        ResultSet rs = null;
        try {
            rs = st.executeQuery(query);
            outputResult(rs, console);
        } catch(SQLException e) {
           console.println(e.getMessage()); 
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Output the result in flat xml.
     */
    void outputFlatXML(ResultSet rs, ConsoleReader console) throws Exception {
        String DATASET = "dataset";
        XmlWriter _xmlWriter;
        _xmlWriter = new XmlWriter(System.out, "UTF-8");
        _xmlWriter.enablePrettyPrint(true);
        _xmlWriter.writeDeclaration();
        _xmlWriter.writeElement(DATASET);

        ResultSetMetaData rsMd = rs.getMetaData();
        int columnCount = rsMd.getColumnCount();
        String tableName = rsMd.getTableName(1) != "" ? rsMd.getTableName(1) : "row";

        while (rs.next()) {
            _xmlWriter.writeElement(tableName);
            for (int i = 1; i <= columnCount; i++) {
                String value = rs.getString(i);
                if (value == null) {
                    continue;
                }
                String columnName = rsMd.getColumnName(i);
                _xmlWriter.writeAttribute(columnName, value, true);
            }
            _xmlWriter.endElement();
        }
        _xmlWriter.endElement();
        _xmlWriter.close();
    }

    void outputResult(ResultSet rs, ConsoleReader console) throws Exception {
        if (outputFormat == OutputForms.CSV) {
            outputCSV(rs, console);
        } else {
            outputFlatXML(rs, console);
        }
    }

    /**
     * Output the result set in CSV format.
     */
    void outputCSV(ResultSet rs, ConsoleReader console) throws Exception {
        ResultSetMetaData rsMd = rs.getMetaData();
        int columnCount = rsMd.getColumnCount();
        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                String value = rs.getString(i);
                System.out.print(value == null ? "\\N" : value);
                if (i == columnCount) {
                    System.out.println();
                } else {
                    System.out.print("\t");
                }
            }
        }
    }

    void listTables(String query, ConsoleReader console) throws Exception {
        String catalogPattern = null;
        String schemaPattern = null;

        DatabaseMetaData dbMetadata = connection.getMetaData();
        
        ResultSet rs = null;
        rs = dbMetadata.getTables(catalogPattern, schemaPattern, "%", new String[] {"TABLE", "VIEW"});
        outputResult(rs, console);
        /*
        while (rs.next()) {
            safePrint(console, rs.getString(1));
            console.print(" ");
            safePrint(console, rs.getString(2));
            console.print(" ");
            safePrint(console, rs.getString(3));
            console.print(" ");
            safePrint(console, rs.getString(4));
            console.println();
        }   
        */
    }

    private void safePrint(ConsoleReader console, String value) throws Exception {
        if (value != null) {
            console.print(value);
        }
    }

    /**
     * Execute the query. The semicolon has been removed.
     *
     * @param query - the full multi-line query.
     */
    private void executeQuery(String query, ConsoleReader console) throws Exception {
        query = query.substring(0, query.length() - 1);
        query = query.trim();
        //console.println("EXECUTING:" + query);

        if (query.equals("tables")) {
            listTables(query, console);
        } else if (query.equals("xml")) {
            outputFormat = OutputForms.FLATXML;
        } else if (query.equals("csv")) {
            outputFormat = OutputForms.CSV;
        } else if (query.equals("connect") || query.startsWith("connect ")) {
            String profile = query.substring(7).trim();
            if (connection != null) {
                connection.close();
                connection = null;
            }
            openConnection(profile);
        } else {
            executeSQLQuery(query, console);
        }
    }

    /**
     * Opens a connection to a database.
     *
     * @param profile - A profile to use.
     */
    private void openConnection(String profile) throws Exception {
        connection = Util.getConnection(profile);
    }

    /**
     * Read input from user.
     */
    private void readLoop() {
        try {
            ConsoleReader console = new ConsoleReader();
            console.setPrompt("SQL> ");
            console.setHistoryEnabled(false);
            reset();
            String line = null;
            while ((line = console.readLine()) != null) {
                //console.println(line);
                setNextState(line);
                switch (lineState) {
                    case APOS:
                        console.setPrompt("'> ");
                        break;
                    case END:
                        executeQuery(stmtBuf, console);
                        History h = console.getHistory();
                        h.add(stmtBuf);
                        reset();
                    default:
                        console.setPrompt("SQL> ");
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            try {
                TerminalFactory.get().restore();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Main routine.
     */
    public static void main(String[] args) {
        try {
            Options options = new Options();
            options.addOption("e", true, "execute SQL statement");
            options.addOption("p", true, "profile to use");
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            String profile = cmd.getOptionValue("p");

            CLI engine = new CLI();
            engine.openConnection(profile);
            engine.readLoop();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
 
}
