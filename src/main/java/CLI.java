import java.io.IOException;
import java.io.PrintStream;
//import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVFormat;

import jline.console.ConsoleReader;
import jline.console.history.History;
import jline.TerminalFactory;
import org.dbunit.util.xml.XmlWriter;

public class CLI {

    private String stmtBuf;
    private StmtState lineState = StmtState.START;
    enum OutputForms { CSV, TSV, FLATXML }
    private OutputForms outputFormat = OutputForms.TSV;

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

    /**
     * Send query to database.
     */
    void executeSQLQuery(String query, PrintStream console) throws Exception {
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
    void outputFlatXML(ResultSet rs, PrintStream console) throws Exception {
        String DATASET = "dataset";
        XmlWriter _xmlWriter;
        _xmlWriter = new XmlWriter(console, "UTF-8");
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

    void outputResult(ResultSet rs, PrintStream console) throws Exception {
        if (outputFormat == OutputForms.CSV) {
            outputCSV(rs, console);
        } else if (outputFormat == OutputForms.TSV) {
            outputTSV(rs, console);
        } else {
            outputFlatXML(rs, console);
        }
    }

    /**
     * Output the result set in CSV format.
     */
    void outputCSV(ResultSet rs, PrintStream console) throws Exception {
        CSVPrinter printer = CSVFormat.DEFAULT.withHeader(rs).print(console);
        printer.printRecords(rs);
        printer.flush();
    }

    void outputTSV(ResultSet rs, PrintStream console) throws Exception {
        CSVPrinter printer = CSVFormat.TDF.withHeader(rs).print(console);
        printer.printRecords(rs);
        printer.flush();
    }

    /**
     * Output the result set in TSV format.
     */
    void OLDoutputTSV(ResultSet rs, PrintStream console) throws Exception {
        ResultSetMetaData rsMd = rs.getMetaData();
        int columnCount = rsMd.getColumnCount();
        while (rs.next()) {
            for (int i = 1; i <= columnCount; i++) {
                String value = rs.getString(i);
                console.print(value == null ? "\\N" : value);
                if (i == columnCount) {
                    console.println();
                } else {
                    console.print("\t");
                }
            }
        }
    }

    /**
     * Get tables from database via metadata query.
     * @param query - unused.
     */
    void listTables(String query, PrintStream console) throws Exception {
        String catalogPattern = null;
        String schemaPattern = null;

        DatabaseMetaData dbMetadata = connection.getMetaData();

        ResultSet rs = null;
        rs = dbMetadata.getTables(catalogPattern, schemaPattern, "%", new String[] {"TABLE", "VIEW"});
        outputResult(rs, console);
    }

    /**
     * Execute the query. The semicolon has been removed.
     *
     * @param query - the full multi-line query.
     */
    private void executeQuery(String query, PrintStream console) throws Exception {
        query = query.substring(0, query.length() - 1);
        query = query.trim();
        //console.println("EXECUTING:" + query);

        if (query.equals("tables")) {
            listTables(query, console);
        } else if (query.equals("xml")) {
            outputFormat = OutputForms.FLATXML;
        } else if (query.equals("csv")) {
            outputFormat = OutputForms.CSV;
        } else if (query.equals("tsv")) {
            outputFormat = OutputForms.TSV;
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
                        executeQuery(stmtBuf, System.out);
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
            //Properties props = Util.getProperties();
            //String extraJars = props.getProperty("classpath");
            //if (extraJars != null) {
            //    URL urls [] = {};
            //    JarFileLoader cl = new JarFileLoader(urls);
            //    cl.addFiles(extraJars);
            //}
            Options options = new Options();
            options.addOption("e", true, "execute SQL statement");
            options.addOption("p", true, "profile to use");
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            String profile = cmd.getOptionValue("p");
            String queryArgument = cmd.getOptionValue("e");

            CLI engine = new CLI();
            engine.openConnection(profile);
            if (queryArgument == null) {
                engine.readLoop();
            } else {
                engine.executeSQLQuery(queryArgument, System.out);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
