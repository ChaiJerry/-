package mid_tests;

import org.junit.*;

import java.io.*;

import static io.CSVFileIO.*;
import static io.SharedAttributes.*;
import static org.junit.Assert.*;

public class TestCsvFileIO {
    @Test
    public void test() throws IOException {
        assertEquals(fileIO.read(TICKET).size(), getTestMap().size()+getTrainingMap().size());
    }
}
