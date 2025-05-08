import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.QuizApp;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;

public class QuizAppTest {

    private static final File RESULT_FILE = new File("test_quiz_results.json");

    @Before
    public void setUp() throws Exception {
        if (RESULT_FILE.exists()) {
            Files.delete(RESULT_FILE.toPath());
            new FileWriter(RESULT_FILE, false).close();
        }
    }

    @Test
    public void testLaunch_doesNotThrowException() {
        try {
            QuizApp.launch();
        } catch (Exception e) {
            fail("QuizApp.launch() should not throw an exception: " + e.getMessage());
        }
    }

    @Test
    public void testSaveResult_createsResultFileWithCorrectStructure() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        QuizApp app = new QuizApp(mapper);
        app.questions = Collections.emptyList(); // avoid UI-related behavior
        app.correct = 3;
        app.incorrect = 2;

        app.saveResult("00:01:05", "test_quiz_results.json"); // simulate 65 seconds

        assertTrue("test_quiz_results.json should exist", RESULT_FILE.exists());
        assertTrue("File should not be empty", RESULT_FILE.length() > 0);

        JsonNode[] results = mapper.readValue(RESULT_FILE, JsonNode[].class);
        assertEquals("There should be one result entry", 1, results.length);

        JsonNode result = results[0];
        assertEquals(3, result.get("correct").asInt());
        assertEquals(2, result.get("wrong").asInt());
        assertNotNull(result.get("date_time").asText());
        assertEquals("00:01:05", result.get("duration_ms").asText());
    }
}