import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileReader;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

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

    /** Configuration file for database connections. */
    private Properties props = new Properties();

    /** Control console for interactive operation. */
    ConsoleReader console;

    /** If set then all statements are wrapped in one transation. */
    private boolean wrapInTransaction;
    /**
     * Constructor.
     */
    public CLI() {
        this("database.properties");
    }

    /**
     * Constructor.
     *
     * @param propertiesPath - path name of a properties file.
     */
    public CLI(String propertiesPath) {
        try {
            loadProperties(props, propertiesPath);
        } catch (IOException e) {
        }
        String extraJars = props.getProperty("classpath");
        JarFileLoader.addPaths(extraJars);
    }

    /**
     * Constructor.
     *
     * @param properties - populated properties.
     */
    public CLI(Properties properties) {
        props = properties;
        String extraJars = props.getProperty("classpath");
        JarFileLoader.addPaths(extraJars);
    }

    public void setOneTransaction() {
        wrapInTransaction = true;
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
    private void loadProperties(Properties properties, String filePath) throws IOException {
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

    private Connection getConnection() throws Exception {
        return getConnection(null);
    }

    private Connection getConnection(String profile) throws Exception {

        String prefix = (profile == null || "".equals(profile)) ? "" : profile + ".";

        String driver = props.getProperty(prefix + "db.driver");
        Class driverClass = Class.forName(driver);
        String connectionUrl = props.getProperty(prefix + "db.database");
        String username = props.getProperty(prefix + "db.user");
        String password = props.getProperty(prefix + "db.password");
        return DriverManager.getConnection(connectionUrl, username, password);
    }

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

    /**
     * Set the next state for one character.
     * FIXME: Inefficient
     */
    public StmtState setNextState(char c) {
        stmtBuf = stmtBuf == null ? String.valueOf(c) : stmtBuf + String.valueOf(c);
        lineState = lineState.next(lineState, c);
        return lineState;
    }

    public StmtState getState() {
        return lineState;
    }

    /**
     * Reset the statement scanner to the start.
     */
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
     *
     * @param line - the message to output.
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
        if (args[0].equals("\\c")) {
            metaConnect(args);
        } else if (args[0].equals("\\dc")) {
            metaCatalogs(args);
        } else if (args[0].equals("\\dd")) {
            metaColumns(args);
        } else if (args[0].equals("\\df")) {
            getFunctions(args);
        } else if (args[0].equals("\\dn")) {  // a.k.a namespaces
            metaSchemas(args);
        } else if (args[0].equals("\\dp")) {
            metaProcedures(args);
        } else if (args[0].equals("\\dt")) {
            metaTables(args);
        } else if (args[0].equals("\\f")) {
            metaFormat(args);
        } else if (args[0].equals("\\o")) {
            metaOutput(args);
        } else if (args[0].equals("\\h")) {
            metaHelp(args);
        } else {
            controlOutput("Unknown meta command. Type \\h for help");
        }
    }

    /**
     * Show a help text.
     *
     * @param args - In case we want to expand the help to be specific on a topic.
     */
    private void metaHelp(String[] args) throws Exception {
        controlOutput("Lines starting with \\ are meta commands. Anything else is sent as is to the database service");
        controlOutput("Meta commands:");
        controlOutput("\\c = connect");
        controlOutput("\\dc = List catalogs.");
        controlOutput("\\dd = List table columns. Mandatory argument: table name");
        controlOutput("\\df = List functions.");
        controlOutput("\\dn = List schemas a.k.a namespaces.");
        controlOutput("\\dp = List procedures.");
        controlOutput("\\dt = List tables.");
        controlOutput("\\f = Format of output. Available arguments: flatxml, csv, tsv");
        controlOutput("\\o = Redirect output to file. No argument redirects to console");
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
     * Get tables from database via metadata query.
     * @param args - unused.
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
     *
     * @param args - the arguments to the meta command.
     */
    private void metaProcedures(String[] args) throws Exception {
        DatabaseMetaData dbMetadata = connection.getMetaData();

        ResultSet rs = dbMetadata.getProcedures(null, null, "%");
        outputFormat.output(rs, outputStream);
    }

    /**
     * Set the format of the output.
     *
     * @param args - the arguments to the meta command.
     */
    private void metaFormat(String[] args) throws Exception {
        String format = args[1].toLowerCase();
        setOutputFormat(format);
        controlOutput("Output format set to " + outputFormat.toString().toLowerCase());
    }

    void setOutputFormat(String format) {
        if (format.equals("xml") || format.equals("flatxml")) {
            outputFormat = OutputForms.FLATXML;
        } else if (format.equals("csv")) {
            outputFormat = OutputForms.CSV;
        } else if (format.equals("excel")) {
            outputFormat = OutputForms.EXCEL;
        } else if (format.equals("tsv")) {
            outputFormat = OutputForms.TSV;
        } else {
            throw new IllegalArgumentException("Unknown output format: " + format);
        }
    }

    /**
     * Get column information of table
     *
     * @param subQuery - The rest of the query.
     */
    private void metaColumns(String[] args) throws Exception {
        if (args.length < 2) {
            throw new IllegalArgumentException("You must enter a table pattern");
        }
        String table = args[1];
        DatabaseMetaData dbMetadata = connection.getMetaData();

        ResultSet rs = dbMetadata.getColumns(null, null, table, "%");
        outputFormat.output(rs, outputStream);
    }

    /**
     * Set output to a file or stdout.
     *
     * @param args - the arguments to the meta command.
     */
    private void metaOutput(String[] args) throws Exception {
        if (args.length > 1 && !"".equals(args[1])) {
            if ("-".equals(args[1])) {
                setOutputStream(System.out);
            } else {
                setOutputStream(new PrintStream(args[1]));
            }
            controlOutput("Output redirected to " + args[1]);
        } else {
            setOutputStream(System.out);
            controlOutput("Output redirected to standard out");
        }
    }

    /**
     * Execute the 'connect' statement.
     *
     * @param subQuery - The rest of the query.
     */
    private void metaConnect(String[] args) throws Exception {
        closeConnection();
        openConnection(args[1]);
    }

    /**
     * Opens a connection to the database of the default profile.
     */
    void openConnection() throws Exception {
        openConnection(null);
    }

    /**
     * Opens a connection to a database.
     *
     * @param profile - A profile to use.
     */
    void openConnection(String profile) throws Exception {
        connection = getConnection(profile);
        if (wrapInTransaction) {
            connection.setAutoCommit(false);
        }
    }

    void closeConnection() throws Exception {
        if (connection != null) {
            if (wrapInTransaction) {
                connection.commit();
            }
            connection.close();
            connection = null;
        }
    }

    /**
     * Read input from user.
     */
    private void interactive() {
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
     * Read input from source file. The statements are separated by semicolon.
     */
    private void readFromFile(String sourceFile) throws IOException {
        FileReader inputStream = null;
        try {
            inputStream = new FileReader(sourceFile);
            char[] line = new char[1000];
            int size;
            while ((size = inputStream.read(line)) != -1) {
                controlOutput(String.valueOf(line));
                for (int inx = 0; inx < size; inx++) {
                    setNextState(line[inx]);
                    if (lineState == StmtState.END) {
                        evaluateQuery(stmtBuf);
                        reset();
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to load from source file at " + sourceFile);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

    }

    /**
     * Main routine.
     */
    public static void main(String[] args) {
        String outputFile = null;
        String outputFormat = null;
        String profile = null;
        String queryArgument = null;
        String sourceFile = null;
        String program = CLI.class.getName().toLowerCase();
        boolean wrapInTransaction = false;
        PrintStream outputStream = System.out;
        try {
            Options options = new Options();
            options.addOption("e", "execute", true, "Execute SQL statement");
            options.addOption("p", "profile", true, "Profile to use for connecting to database");
            options.addOption("o", "output", true, "File to output to");
            options.addOption("f", "file", true, "Read statements to execute from file");
            options.addOption("F", "format", true, "Format of output");
            options.addOption("h", "help", false, "Print help with options");
            options.addOption("T", "transaction", false, "Wrap statements in one transaction");

            try {
                CommandLineParser parser = new DefaultParser();
                CommandLine cmd = parser.parse(options, args);
                profile = cmd.getOptionValue("p");
                queryArgument = cmd.getOptionValue("e");
                sourceFile = cmd.getOptionValue("f");
                outputFile = cmd.getOptionValue("o");
                outputFormat = cmd.getOptionValue("F");
                if(cmd.hasOption("T")) {
                    wrapInTransaction = true;
                }
                if(cmd.hasOption("h")) {
                    HelpFormatter formatter = new HelpFormatter();
                    formatter.printHelp(program, options);
                    System.exit(0);
                }
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
            if (outputFormat != null) {
                engine.setOutputFormat(outputFormat);
            }
            if (wrapInTransaction) {
                engine.setOneTransaction();
            }

            engine.openConnection(profile);
            if (queryArgument == null && sourceFile == null) {
                engine.interactive();
            } else if (sourceFile != null) {
                engine.readFromFile(sourceFile);
            } else {
                engine.executeSQLQuery(queryArgument);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
