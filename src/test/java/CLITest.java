
import java.util.Properties;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Before;

public class CLITest {

    private CLI consoleDemo;

    @Before
    public void createCLI() {
        Properties props = new Properties();
        consoleDemo = new CLI(props);
        consoleDemo.reset();
        assertEquals("START", consoleDemo.getState().toString());
    }

    @Test
    public void simpleOneliner() throws Exception {
        consoleDemo.setNextState("SELECT * FROM X;");
        assertEquals("END", consoleDemo.getState().toString());
    }

    @Test
    public void simpleDoubleApos() throws Exception {
        consoleDemo.setNextState("SELECT 'I can''t hear' FROM X;");
        assertEquals("END", consoleDemo.getState().toString());
    }

    @Test
    public void simpleQuotedStmt() throws Exception {
        consoleDemo.setNextState("SELECT \"I can't hear\" FROM X;");
        assertEquals("END", consoleDemo.getState().toString());
    }

    @Test
    public void simpleStringWithQuot() throws Exception {
        consoleDemo.setNextState("SELECT 'Get \"Real\"' FROM X;");
        assertEquals("END", consoleDemo.getState().toString());
    }

    @Test
    public void twoLiner() throws Exception {
        consoleDemo.setNextState("SELECT ONE, TWO");
        assertEquals("START", consoleDemo.getState().toString());
        consoleDemo.setNextState(" FROM X;");
        assertEquals("END", consoleDemo.getState().toString());
    }

    @Test
    public void backslashL() throws Exception {
        consoleDemo.setNextState("\\l\n");
        assertEquals("END", consoleDemo.getState().toString());
    }
}
