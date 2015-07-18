import java.io.IOException;
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

    /** Control console for interactive operation. */
    ConsoleReader console;

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
     * Output action information to the user. The console doesn't have the same
     * interface as the output stream.
     */
    void controlOutput(String line) throws IOException {
        if (console != null) {
            console.println(line);
        } else {
            outputStream.println(line);
        }
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
           controlOutput(e.getMessage());
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
        String[] args = splitMetaLine(query);
        if (args[0].equals("\\dt")) {
            metaTables(args);
        } else if (args[0].equals("\\dc")) {
            metaCatalogs(args);
        } else if (args[0].equals("\\df")) {
            getFunctions(args);
        } else if (args[0].equals("\\dp")) {
            metaProcedures(args);
        } else if (args[0].equals("\\dn")) {  // a.k.a namespaces
            metaSchemas(args);
        } else if (args[0].equals("\\f")) {
            metaFormat(args);
        } else if (args[0].equals("\\c")) {
            metaConnect(args);
        } else if (args[0].equals("\\o")) {
            metaOutput(args);
        } else if (args[0].equals("\\h")) {
            metaHelp(args);
        } else {
            controlOutput("Unknown meta command. Type \\h for help");
        }
    }

    /**
     * Split the meta query into components. We don't support quotes at
     * the moment, so we can be naive and split on whitespace.
     *
     * @param query The full line to split.
     * @return The split line.
     */
    private String[] splitMetaLine(String query) {
        return query.split("\\s+");
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
    void metaHelp(String[] args) throws Exception {
        controlOutput("Commands: \\dt = list tables, \\df = format, connect and SQL statements");
    }

    /**
     * Get tables from database via metadata query.
     * @param query - unused.
     */
    void metaTables(String[] args) throws Exception {
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
    void metaSchemas(String[] args) throws Exception {
        DatabaseMetaData dbMetadata = connection.getMetaData();

        ResultSet rs = dbMetadata.getSchemas();
        outputFormat.output(rs, outputStream);
    }

    /**
     * Get catalogs from database via metadata query.
     * @param query - unused.
     */
    void metaCatalogs(String[] args) throws Exception {
        DatabaseMetaData dbMetadata = connection.getMetaData();

        ResultSet rs = dbMetadata.getCatalogs();
        outputFormat.output(rs, outputStream);
    }

    /**
     * Get functions from database via metadata query.
     * @param query - unused.
     */
    private void getFunctions(String[] args) throws Exception {
        DatabaseMetaData dbMetadata = connection.getMetaData();

        ResultSet rs = dbMetadata.getFunctions(null, null, "%");
        outputFormat.output(rs, outputStream);
    }

    /**
     * Get procedures from database via metadata query.
     * @param query - unused.
     */
    private void metaProcedures(String[] args) throws Exception {
        DatabaseMetaData dbMetadata = connection.getMetaData();

        ResultSet rs = dbMetadata.getProcedures(null, null, "%");
        outputFormat.output(rs, outputStream);
    }

    /**
     * Connect to a profile.
     *
     * @param subQuery - The rest of the query.
     */
    private void metaFormat(String[] args) throws Exception {
        if (args[1].equals("xml")) {
            outputFormat = OutputForms.FLATXML;
        } else if (args[1].equals("csv")) {
            outputFormat = OutputForms.CSV;
        } else if (args[1].equals("tsv")) {
            outputFormat = OutputForms.TSV;
        }
        controlOutput("Output format set to " + outputFormat.toString());
    }

    /**
     * Connect to a profile.
     *
     * @param subQuery - The rest of the query.
     */
    private void metaOutput(String[] args) throws Exception {
        if (args.length < 1 && !"".equals(args[1])) {
            if ("-".equals(args[1])) {
                setOutputStream(System.out);
            } else {
                setOutputStream(new PrintStream(args[1]));
            }
        } else {
            setOutputStream(System.out);
        }
    }

    /**
     * Execute the 'connect' statement.
     *
     * @param subQuery - The rest of the query.
     */
    private void metaConnect(String[] args) throws Exception {
        if (connection != null) {
            connection.close();
            connection = null;
        }
        openConnection(args[1]);
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
            console = new ConsoleReader();
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
