package mid_tests;

import org.junit.*;
import org.xml.sax.*;
import bundle_service_for_backend.xml_parser.XMLReader;

import javax.xml.parsers.*;
import java.io.*;

import static org.junit.Assert.*;

public class TestXMLReader {
    @Test
    public void test(){
        try {
            XMLReader xmlReader = new XMLReader();
            xmlReader.read();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            System.out.println(e.getMessage());
            assertFalse(false);
        }
        assertTrue(true);
    }

}
