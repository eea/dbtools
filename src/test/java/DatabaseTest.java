
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
//import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.PropertyConfigurator;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.operation.DatabaseOperation;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;

public class DatabaseTest {

    private static final String JDBC_DRIVER = org.h2.Driver.class.getName();
    private static final String JDBC_URL = "jdbc:h2:mem:test";
    private static final String USER = "sa";
    private static final String PASSWORD = "";
    private static final String UTF8_ENCODING = "UTF-8";

    private ByteArrayOutputStream testOutput;
    private Properties props;
    private Connection dbConn;
    private CLI engine;

    private void createSchema() throws Exception {
        Statement statement = dbConn.createStatement();
        statement.executeUpdate("create table if not exists PERSON ("
            + "ID int identity primary key,"
            + "NAME varchar(100),"
            + "LAST_NAME varchar(100),"
            + "BORN DATETIME,"
            + "ORG varchar(30))");
        statement.close();
    }

    /**
     * Initialize the logging system. It is used by dbunit.
     */
    @BeforeClass
    public static void setupLogger() throws Exception {
        Properties logProperties = new Properties();
        logProperties.setProperty("log4j.rootCategory", "DEBUG, CONSOLE");
        logProperties.setProperty("log4j.appender.CONSOLE", "org.apache.log4j.ConsoleAppender");
        logProperties.setProperty("log4j.appender.CONSOLE.Threshold", "ERROR");
        logProperties.setProperty("log4j.appender.CONSOLE.layout", "org.apache.log4j.PatternLayout");
        logProperties.setProperty("log4j.appender.CONSOLE.layout.ConversionPattern", "- %m%n");
        PropertyConfigurator.configure(logProperties);
    }

    @Before
    public void importDataSet() throws Exception {
        dbConn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
        createSchema();
        IDataSet dataSet = readDataSet();
        cleanlyInsert(dataSet);
        //dbConn.setAutoCommit(false); // Emulate command line use

        testOutput = new ByteArrayOutputStream();
        props = new Properties();
        props.setProperty("db.driver", JDBC_DRIVER);
        props.setProperty("db.database", JDBC_URL);
        props.setProperty("db.user", USER);
        props.setProperty("db.password", PASSWORD);
        engine = new CLI(props);
        engine.setOutputStream(new PrintStream(testOutput, false, UTF8_ENCODING));
        engine.openConnection();
    }

    @After
    public void closeAll() throws Exception {
        testOutput.close();
        engine.closeConnection();
        engine = null;
        dbConn.close();
        dbConn = null;
    }

    private IDataSet readDataSet() throws Exception {
        InputStream is = DatabaseTest.class.getClassLoader().getResourceAsStream("seed-person.xml");
        return new FlatXmlDataSetBuilder().build(is);
    }

    private void cleanlyInsert(IDataSet dataSet) throws Exception {
        IDatabaseTester databaseTester = new JdbcDatabaseTester(JDBC_DRIVER, JDBC_URL, USER, PASSWORD);
        databaseTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
        databaseTester.setDataSet(dataSet);
        databaseTester.onSetup();
    }

    private String loadFile(String fileName) throws Exception {
        InputStream is = DatabaseTest.class.getClassLoader().getResourceAsStream(fileName);
        return IOUtils.toString(is, UTF8_ENCODING);
    }

    @Test
    public void simplePersonExport() throws Exception {
        engine.executeSQLQuery("SELECT ID, NAME, LAST_NAME, BORN, ORG FROM PERSON ORDER BY ID");
        String actual = testOutput.toString(UTF8_ENCODING);
        //String expected = loadFile("rdf-person.xml");
        String expected = "ID\tNAME\tLAST_NAME\tBORN\tORG\r\n"
            + "182208\tΗλέκτρα\tel Greco\t1984-03-18 20:55:31.0\tspectre\r\n"
            + "533922\tAlice\tFoo\t1980-11-02 00:00:00.0\tyakuza\r\n"
            + "882911\tCharlie\tBrown\t1970-04-30 00:00:00.0\tmafia +\r\n";
        assertEquals(expected, actual);
    }

   /**
     * Test correctness when the output type is binary.
     * The values are seen as sequences of hex.
     */
    @Test
    public void castToVarBinary() throws Exception {
        engine.setOutputFormat("xml");
        engine.executeSQLQuery("SELECT CAST('63' AS VARBINARY) AS ID"
            + ", CAST('70' AS VARBINARY) AS NAME"
            + ", CAST('3456' AS VARBINARY) AS NUMBER");
        String actual = testOutput.toString(UTF8_ENCODING);
        String expected = "<?xml version='1.0' encoding='UTF-8'?>\n"
            + "<dataset>\n"
            + "  <row ID=\"c\" NAME=\"p\" NUMBER=\"4V\"/>\n"
            + "</dataset>\n";
        assertEquals(expected, actual);
    }

   /**
     * Test correctnes when the output type is CLOB.
     */
    @Ignore @Test
    public void castToClob() throws Exception {
        engine.executeSQLQuery("SELECT CAST('c' AS CLOB) AS ID"
            + ", CAST('plain string' AS CLOB) AS NAME"
            + ", CAST('3456/view' AS CLOB) AS LINK");
        String actual = testOutput.toString(UTF8_ENCODING);
        String expected = "";
        assertEquals(expected, actual);
    }

    @Test
    public void basePersonExport() throws Exception {
        engine.setOutputFormat("xml");
        engine.executeSQLQuery("SELECT ID, NAME, LAST_NAME, BORN, ORG FROM PERSON ORDER BY ID");
        String actual = testOutput.toString(UTF8_ENCODING);
        //String expected = loadFile("rdf-person-base.xml");
        String expected = "<?xml version='1.0' encoding='UTF-8'?>\n"
            + "<dataset>\n"
            + "  <PERSON ID=\"182208\" NAME=\"&#919;&#955;&#941;&#954;&#964;&#961;&#945;\" "
            + "LAST_NAME=\"el Greco\" BORN=\"1984-03-18 20:55:31.0\" ORG=\"spectre\"/>\n"
            + "  <PERSON ID=\"533922\" NAME=\"Alice\" LAST_NAME=\"Foo\" BORN=\"1980-11-02 00:00:00.0\" ORG=\"yakuza\"/>\n"
            + "  <PERSON ID=\"882911\" NAME=\"Charlie\" LAST_NAME=\"Brown\" BORN=\"1970-04-30 00:00:00.0\" ORG=\"mafia +\"/>\n"
            + "</dataset>\n";
        assertEquals(expected, actual);
    }

    @Ignore @Test
    public void personAtQuery() throws Exception {
        engine.executeSQLQuery("SELECT '@' AS ID, NAME, LAST_NAME, BORN, ORG FROM PERSON ORDER BY BORN");
        String actual = testOutput.toString(UTF8_ENCODING);
        String expected = loadFile("rdf-person-atsign.xml");
        assertEquals(expected, actual);
    }

    @Ignore @Test
    public void fullDocumentInformation() throws Exception {
        engine.executeSQLQuery("SELECT NULL AS ID, 'Ηλέκτρα' AS \"dcterms:creator@\", 'http://license.eu' AS \"cc:licence->\"");
        String actual = testOutput.toString(UTF8_ENCODING);
        String expected = "";
        assertEquals(expected, actual);
    }

    /*
     * If the 'class' property is spelled 'CLASS' then it has no effect.
     */
    @Ignore @Test
    public void documentInformationWithCLASS() throws Exception {
        engine.executeSQLQuery("SELECT NULL AS ID, 'Ηλέκτρα' AS \"dcterms:creator@\", 'http://license.eu' AS \"cc:licence->\"");
        String actual = testOutput.toString(UTF8_ENCODING);
        String expected = "";
        assertEquals(expected, actual);
    }

    @Ignore @Test
    public void prtrElektra() throws Exception {
        engine.executeSQLQuery("SELECT 'Elektra' AS ID, 'Ηλέκτρα' AS name");
        String actual = testOutput.toString(UTF8_ENCODING);
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"
            + " xmlns=\"http://prtr/\">\n"
            + "\n"
            + "<prtr:TMX rdf:about=\"#greek/Elektra\">\n"
            + " <NAME>Ηλέκτρα</NAME>\n"
            + "</prtr:TMX>\n"
            + "</rdf:RDF>\n";
        assertEquals(expected, actual);
    }

}
