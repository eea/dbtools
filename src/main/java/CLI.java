import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;

import jline.console.ConsoleReader;
import jline.console.history.History;
import jline.TerminalFactory;

public class CLI {

    /** Buffer for current statement. */
    private String stmtBuf;

    /** Statekeeper for the line scanner. */
    private StmtState lineState = StmtState.START;

    /** Formatter of the output. */
    private OutputForms outputFormat = OutputForms.TSV;

    /** Active database connection. */
    private Connection connection;

    /** File to send the output to. */
    private PrintStream outputStream;

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

    public void setOutputStream(PrintStream stream) {
        outputStream = stream;
    }

    /**
     * Send query to database.
     */
    void executeSQLQuery(String query) throws Exception {
        Statement st = connection.createStatement();
        ResultSet rs = null;
        try {
            rs = st.executeQuery(query);
            outputFormat.output(rs, outputStream);
        } catch (SQLException e) {
           outputStream.println(e.getMessage());
        } finally {
            if (rs != null) {
                rs.close();
            }
        }
    }

    /**
     * Send query to database.
     */
    void executeMetaQuery(String query) throws Exception {
        if (isThisCommand(query, "\\dt")) {
            executeTables(query, outputStream);
        } else if (isThisCommand(query, "\\dc")) {
            executeCatalogs(query, outputStream);
        } else if (isThisCommand(query, "\\df")) {
            getFunctions(query);
        } else if (isThisCommand(query, "\\dp")) {
            getProcedures(query);
        } else if (isThisCommand(query, "\\dn")) {  // a.k.a namespaces
            executeSchemas(query, outputStream);
        } else if (isThisCommand(query, "\\f")) {
            executeFormat(query.substring(1), outputStream);
        } else if (isThisCommand(query, "\\c")) {
            executeConnect(query.substring(1), outputStream);
        } else if (isThisCommand(query, "\\o")) {
            executeOutput(query.substring(1), outputStream);
        } else if (isThisCommand(query, "\\h")) {
            executeHelp(query.substring(1), outputStream);
        } else {
            outputStream.println("Unknown meta command. Type \\h for help");
        }
    }

    /**
     * Execute the query. The semicolon has been removed.
     *
     * @param query - the full multi-line query.
     */
    private void evaluateQuery(String query) throws Exception {
        query = query.trim();
        if (query.startsWith("\\")) {
            executeMetaQuery(query);
        } else {
            query = query.substring(0, query.length() - 1); // Remove semicolon
            executeSQLQuery(query);
        }
    }

    /**
     * Check which command the user wants to run.
     * FIXME: Make case insensitive.
     */
    private boolean isThisCommand(String query, String command) {
        return query.equals(command) || query.startsWith(command + " ");
    }


    /**
     * Show a help text.
     */
    void executeHelp(String query, PrintStream outputStream) throws Exception {
        outputStream.println("Commands: tables, format, connect and SQL statements");
    }

    /**
     * Get tables from database via metadata query.
     * @param query - unused.
     */
    void executeTables(String query, PrintStream outputStream) throws Exception {
        String catalogPattern = null;
        String schemaPattern = null;

        DatabaseMetaData dbMetadata = connection.getMetaData();

        ResultSet rs = null;
        rs = dbMetadata.getTables(catalogPattern, schemaPattern, "%", new String[] {"TABLE", "VIEW"});
        outputFormat.output(rs, outputStream);
    }

    /**
     * Get schemas from database via metadata query.
     * @param query - unused.
     */
    void executeSchemas(String query, PrintStream outputStream) throws Exception {
        DatabaseMetaData dbMetadata = connection.getMetaData();

        ResultSet rs = dbMetadata.getSchemas();
        outputFormat.output(rs, outputStream);
    }

    /**
     * Get catalogs from database via metadata query.
     * @param query - unused.
     */
    void executeCatalogs(String query, PrintStream outputStream) throws Exception {
        DatabaseMetaData dbMetadata = connection.getMetaData();

        ResultSet rs = dbMetadata.getCatalogs();
        outputFormat.output(rs, outputStream);
    }

    /**
     * Get functions from database via metadata query.
     * @param query - unused.
     */
    private void getFunctions(String query) throws Exception {
        DatabaseMetaData dbMetadata = connection.getMetaData();

        ResultSet rs = dbMetadata.getFunctions(null, null, "%");
        outputFormat.output(rs, outputStream);
    }

    /**
     * Get procedures from database via metadata query.
     * @param query - unused.
     */
    private void getProcedures(String query) throws Exception {
        DatabaseMetaData dbMetadata = connection.getMetaData();

        ResultSet rs = dbMetadata.getProcedures(null, null, "%");
        outputFormat.output(rs, outputStream);
    }

    /**
     * Connect to a profile.
     *
     * @param subQuery - The rest of the query.
     */
    private void executeFormat(String subQuery, PrintStream outputStream) throws Exception {
        String format = subQuery.substring(1).trim();
        if (format.equals("xml")) {
            outputFormat = OutputForms.FLATXML;
        } else if (format.equals("csv")) {
            outputFormat = OutputForms.CSV;
        } else if (format.equals("tsv")) {
            outputFormat = OutputForms.TSV;
        }
    }

    /**
     * Connect to a profile.
     *
     * @param subQuery - The rest of the query.
     */
    private void executeOutput(String subQuery, PrintStream outputStream) throws Exception {
        String outputFile = subQuery.substring(1).trim();
        if (outputFile != null && !"".equals(outputFile)) {
            if ("-".equals(outputFile)) {
                setOutputStream(System.out);
            } else {
                setOutputStream(new PrintStream(outputFile));
            }
        }
    }

    /**
     * Execute the 'connect' statement.
     *
     * @param subQuery - The rest of the query.
     */
    private void executeConnect(String subQuery, PrintStream outputStream) throws Exception {
        String profile = subQuery.substring(1).trim();
        if (connection != null) {
            connection.close();
            connection = null;
        }
        openConnection(profile);
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
                        evaluateQuery(stmtBuf);
                        History h = console.getHistory();
                        h.add(stmtBuf);
                        reset();
                    default:
                        console.setPrompt("SQL> ");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                TerminalFactory.get().restore();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Main routine.
     */
    public static void main(String[] args) {
        String outputFile = null;
        String profile = null;
        String queryArgument = null;
        String program = CLI.class.getName();
        PrintStream outputStream = System.out;
        try {
            Options options = new Options();
            options.addOption("e", "execute", true, "Execute SQL statement");
            options.addOption("p", "profile", true, "Profile to use");
            options.addOption("o", "output", true, "File to output to");

            try {
                CommandLineParser parser = new DefaultParser();
                CommandLine cmd = parser.parse(options, args);
                profile = cmd.getOptionValue("p");
                queryArgument = cmd.getOptionValue("e");
                outputFile = cmd.getOptionValue("o");
            } catch (ParseException exp) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(program, options);
                System.exit(2);
            }

            CLI engine = new CLI();

            engine.setOutputStream(System.out);
            if (outputFile != null) {
                if (!"-".equals(outputFile)) {
                    engine.setOutputStream(new PrintStream(outputFile));
                }
            }

            engine.openConnection(profile);
            if (queryArgument == null) {
                engine.readLoop();
            } else {
                engine.executeSQLQuery(queryArgument);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
