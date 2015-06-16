import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;

import jline.console.history.History;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
 
public class CLI {
 
    private String stmtBuf;
    private StmtState lineState = StmtState.START;

    /** Active database connection. */
    private Connection connection;

    enum StmtState {
        START {
            StmtState next(StmtState state, char c) {
                switch (c) {
                    case '\'': state = APOS; break;
                    case '"': state = QUOT; break;
                    case ';': state = END; break;
                    default:
                }
                return state;
            }
        },
        APOS {
            StmtState next(StmtState state, char c) {
                switch (c) {
                    case '\'': state = APOS2; break;
                    default:
                }
                return state;
            }
        },
        APOS2 {
            StmtState next(StmtState state, char c) {
                switch (c) {
                    case '\'': state = APOS; break;
                    case ';': state = END; break;
                    default: state = START;
                }
                return state;
            }
        },
        QUOT {
            StmtState next(StmtState state, char c) {
                switch (c) {
                    case '"': state = QUOT2; break;
                    default:
                }
                return state;
            }
        },
        QUOT2 {
            StmtState next(StmtState state, char c) {
                switch (c) {
                    case '"': state = APOS; break;
                    case ';': state = END; break;
                    default: state = START;
                }
                return state;
            }
        },
        END {
            StmtState next(StmtState state, char c) {
                return state;
            }
        };

        /**
         * Constructor.
         */
        private StmtState() {}

        StmtState next(StmtState state, char c) {
            throw new RuntimeException("what");
        }
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
        ResultSet rs = st.executeQuery(query);
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
        rs.close();

    }

    void listTables(String query, ConsoleReader console) throws Exception {
        String catalogPattern = null;
        String schemaPattern = null;

        DatabaseMetaData dbMetadata = connection.getMetaData();
        
        ResultSet rs = null;
        rs = dbMetadata.getTables(catalogPattern, schemaPattern, "%", new String[] {"TABLE", "VIEW"});
        while (rs.next()) {
            console.print(rs.getString(1));
            console.print(" ");
            console.print(rs.getString(2));
            console.print(" ");
            console.print(rs.getString(3));
            console.print(" ");
            console.print(rs.getString(4));
            console.println();
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
        } else if (query.equals("connect") || query.startsWith("connect ")) {
            String profile = query.substring(7).trim();
            if (connection != null) {
                connection.close();
                connection = null;
            }
            connection = Util.getConnection(profile);
        } else {
            executeSQLQuery(query, console);
        }
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
        CLI engine = new CLI();
        engine.readLoop();
    }
 
}
