
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class ConsoleDemoTest {

    @Test
    public void simpleOneliner() throws Exception {
        ConsoleDemo.reset();
        assertEquals("START", ConsoleDemo.getState().toString());
        ConsoleDemo.setNextState("SELECT * FROM X;");
        assertEquals("END", ConsoleDemo.getState().toString());
    }

    @Test
    public void simpleDoubleApos() throws Exception {
        ConsoleDemo.reset();
        assertEquals("START", ConsoleDemo.getState().toString());
        ConsoleDemo.setNextState("SELECT 'I can''t hear' FROM X;");
        assertEquals("END", ConsoleDemo.getState().toString());
    }

    @Test
    public void simpleQuotedStmt() throws Exception {
        ConsoleDemo.reset();
        assertEquals("START", ConsoleDemo.getState().toString());
        ConsoleDemo.setNextState("SELECT \"I can't hear\" FROM X;");
        assertEquals("END", ConsoleDemo.getState().toString());
    }

    @Test
    public void simpleStringWithQuot() throws Exception {
        ConsoleDemo.reset();
        assertEquals("START", ConsoleDemo.getState().toString());
        ConsoleDemo.setNextState("SELECT 'Get \"Real\"' FROM X;");
        assertEquals("END", ConsoleDemo.getState().toString());
    }

    @Test
    public void twoLiner() throws Exception {
        ConsoleDemo.reset();
        assertEquals("START", ConsoleDemo.getState().toString());
        ConsoleDemo.setNextState("SELECT ONE, TWO");
        assertEquals("START", ConsoleDemo.getState().toString());
        ConsoleDemo.setNextState(" FROM X;");
        assertEquals("END", ConsoleDemo.getState().toString());
    }
}
