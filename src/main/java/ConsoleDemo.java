import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

import jline.console.history.History;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
 
public class ConsoleDemo {
 
    private static String stmtBuf;
    private static StmtState state = StmtState.START;

    /** Active database connection. */
    private static Connection connection;

    enum StmtState {
        START {
            void next(char c) {
                switch (c) {
                    case '\'': state = APOS; break;
                    case '"': state = QUOT; break;
                    case ';': state = END; break;
                    default:
                }
            }
        },
        APOS {
            void next(char c) {
                switch (c) {
                    case '\'': state = APOS2; break;
                    default:
                }
            }
        },
        APOS2 {
            void next(char c) {
                switch (c) {
                    case '\'': state = APOS; break;
                    case ';': state = END; break;
                    default: state = START;
                }
            }
        },
        QUOT {
            void next(char c) {
                switch (c) {
                    case '"': state = QUOT2; break;
                    default:
                }
            }
        },
        QUOT2 {
            void next(char c) {
                switch (c) {
                    case '"': state = APOS; break;
                    case ';': state = END; break;
                    default: state = START;
                }
            }
        },
        END {
            void next(char c) {
            }
        };

        /**
         * Constructor.
         */
        private StmtState() {}

        void next(char c) {
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
    public static StmtState setNextState(String line) {
        stmtBuf = stmtBuf == null ? line : stmtBuf + "\n" + line;
        char[] charBuf = line.toCharArray();
        for (int i = 0; i < charBuf.length; i++) {
            state.next(charBuf[i]);
            //console.println(state.toString());
        }
        return state;
    }

    public static StmtState getState() {
        return state;
    }

    public static void reset() {
        state = StmtState.START;
        stmtBuf = null;
    }

    public static void executeSQLQuery(String query, ConsoleReader console) throws SQLException {
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(query);
        while (rs.next()) {
        }

    }

    /**
     * Execute the query.
     *
     * @param query - the full multi-line query.
     */
    private static void executeQuery(String rawQuery, ConsoleReader console) throws Exception {
        String query = rawQuery.trim();
        console.println("EXECUTING:" + query);
        if (query.startsWith("\\c")) {
            String profile = query.substring(2).trim();
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
    private static void readLoop() {
        try {
            ConsoleReader console = new ConsoleReader();
            console.setPrompt("SQL> ");
            console.setHistoryEnabled(false);
            reset();
            String line = null;
            while ((line = console.readLine()) != null) {
                //console.println(line);
                setNextState(line);
                switch (state) {
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
        readLoop();
    }
 
}
