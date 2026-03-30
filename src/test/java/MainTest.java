import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    @Test
    void testMainPrintsOutput() {
        // Capture System.out
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        // Run main
        Main.main(new String[]{});

        // Restore System.out
        System.setOut(originalOut);

        // Verify output
        String output = outContent.toString().trim();
        assertTrue(output.contains("App started"));
    }
}
