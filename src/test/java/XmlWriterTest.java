
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import static org.junit.Assert.assertEquals;
import org.junit.Test;


/**
 * Two example methods. They should output the same XML.
 */
public class XmlWriterTest {

    @Test
    public void test1() throws IOException {
        Writer writer = new StringWriter();
        XmlWriter xmlwriter = new XmlWriter(writer);
        xmlwriter.writeElement("person").writeAttribute("name", "fred").writeAttribute("age", "12").writeElement("phone").writeText("4254343").endElement().writeElement("friends").writeElement("bob").endElement().writeElement("jim").endElement().endElement().endElement();
        xmlwriter.close();
        String expected = "<person name=\"fred\" age=\"12\">\n"
            + "  <phone>4254343</phone>\n"
            + "  <friends>\n"
            + "    <bob/>\n"
            + "    <jim/>\n"
            + "  </friends>\n"
            + "</person>\n";
        assertEquals(expected, writer.toString());
    }

    @Test
    public void test2() throws IOException {
        Writer writer = new StringWriter();
        XmlWriter xmlwriter = new XmlWriter(writer);
        xmlwriter.writeComment("Example of XmlWriter running");
        xmlwriter.writeElement("person");
        xmlwriter.writeAttribute("name", "fred");
        xmlwriter.writeAttribute("age", "12");
        xmlwriter.writeElement("phone");
        xmlwriter.writeText("4254343");
        xmlwriter.endElement();
        xmlwriter.writeComment("Examples of empty tags");
//        xmlwriter.setDefaultNamespace("test");
        xmlwriter.writeElement("friends");
        xmlwriter.writeEmptyElement("bob");
        xmlwriter.writeEmptyElement("jim");
        xmlwriter.endElement();
        xmlwriter.writeElementWithText("foo", "This is an example.");
        xmlwriter.endElement();
        xmlwriter.close();
        String expected = "<!-- Example of XmlWriter running -->\n"
            + "<person name=\"fred\" age=\"12\">\n"
            + "  <phone>4254343</phone>\n"
            + "  <!-- Examples of empty tags -->\n"
            + "  <friends>\n"
            + "    <bob/>\n"
            + "    <jim/>\n"
            + "  </friends>\n"
            + "  <foo>This is an example.</foo>\n"
            + "</person>\n";
        assertEquals(expected, writer.toString());
    }

}
