
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class CLITest {

    @Test
    public void simpleOneliner() throws Exception {
        CLI consoleDemo = new CLI();
        consoleDemo.reset();
        assertEquals("START", consoleDemo.getState().toString());
        consoleDemo.setNextState("SELECT * FROM X;");
        assertEquals("END", consoleDemo.getState().toString());
    }

    @Test
    public void simpleDoubleApos() throws Exception {
        CLI consoleDemo = new CLI();
        consoleDemo.reset();
        assertEquals("START", consoleDemo.getState().toString());
        consoleDemo.setNextState("SELECT 'I can''t hear' FROM X;");
        assertEquals("END", consoleDemo.getState().toString());
    }

    @Test
    public void simpleQuotedStmt() throws Exception {
        CLI consoleDemo = new CLI();
        consoleDemo.reset();
        assertEquals("START", consoleDemo.getState().toString());
        consoleDemo.setNextState("SELECT \"I can't hear\" FROM X;");
        assertEquals("END", consoleDemo.getState().toString());
    }

    @Test
    public void simpleStringWithQuot() throws Exception {
        CLI consoleDemo = new CLI();
        consoleDemo.reset();
        assertEquals("START", consoleDemo.getState().toString());
        consoleDemo.setNextState("SELECT 'Get \"Real\"' FROM X;");
        assertEquals("END", consoleDemo.getState().toString());
    }

    @Test
    public void twoLiner() throws Exception {
        CLI consoleDemo = new CLI();
        consoleDemo.reset();
        assertEquals("START", consoleDemo.getState().toString());
        consoleDemo.setNextState("SELECT ONE, TWO");
        assertEquals("START", consoleDemo.getState().toString());
        consoleDemo.setNextState(" FROM X;");
        assertEquals("END", consoleDemo.getState().toString());
    }

    @Test
    public void backslashL() throws Exception {
        CLI consoleDemo = new CLI();
        consoleDemo.reset();
        assertEquals("START", consoleDemo.getState().toString());
        consoleDemo.setNextState("\\l\n");
        assertEquals("END", consoleDemo.getState().toString());
    }
}
