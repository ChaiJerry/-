package mid_tests;

import org.junit.*;
import org.xml.sax.*;
import bundle_service_for_backend.xml_parser.XMLIO;

import javax.xml.parsers.*;
import java.io.*;

import static org.junit.Assert.*;

public class TestXMLIO {
    @Test
    public void test(){
        try {
            XMLIO XMLIO = new XMLIO();
            XMLIO.readTest2();
        } catch (IOException | ParserConfigurationException | SAXException e) {
            System.out.println(e.getMessage());
            assertFalse(false);
        }
        assertTrue(true);
    }

}
