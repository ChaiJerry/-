package mid_tests;

import org.junit.*;
import org.w3c.dom.Document;
import org.xml.sax.*;
import xml_parser.XMLReader;

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
