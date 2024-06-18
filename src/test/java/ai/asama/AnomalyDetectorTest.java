package ai.asama;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class AnomalyDetectorTest {

    private static final String TEST_INPUT_DIR = "src/test/resources/input";
    private static final String TEST_OUTPUT_PATH = "src/test/resources/output/anomalies.json";

    @BeforeAll
    static void setup() throws IOException {
        Files.createDirectories(Paths.get(TEST_INPUT_DIR));
        Files.createDirectories(Paths.get(TEST_OUTPUT_PATH).getParent());
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.walk(Paths.get(TEST_INPUT_DIR))
                .filter(Files::isRegularFile)
                .forEach(file -> file.toFile().delete());
        Files.deleteIfExists(Paths.get(TEST_OUTPUT_PATH));
    }

    @Test
    void testGetFiles() throws IOException {
        // Create test files
        Path file1 = Files.createFile(Paths.get(TEST_INPUT_DIR, "machine1.json"));
        Path file2 = Files.createFile(Paths.get(TEST_INPUT_DIR, "machine2.json"));

        // Call the method
        List<Path> files = AnomalyDetector.getFiles(TEST_INPUT_DIR);

        // Verify
        assertEquals(2, files.size());
        assertTrue(files.contains(file1));
        assertTrue(files.contains(file2));
    }

    @Test
    void testReadFiles() throws IOException {
        // Create test files with sample JSON content
        Path file1 = Files.createFile(Paths.get(TEST_INPUT_DIR, "machine1.json"));
        Path file2 = Files.createFile(Paths.get(TEST_INPUT_DIR, "machine2.json"));
        String content1 = "{\"OSVersion\": \"10\", \"Attribute1\": \"Value1\"}";
        String content2 = "{\"OSVersion\": \"11\", \"Attribute1\": \"Value2\"}";
        Files.writeString(file1, content1);
        Files.writeString(file2, content2);

        // Call the method
        Map<String, Map<String, Set<String>>> allFacts = new ConcurrentHashMap<>();
        AnomalyDetector.readFiles(Arrays.asList(file1, file2), allFacts);

        // Verify
        assertEquals(2, allFacts.size());
        assertTrue(allFacts.containsKey("OSVersion"));
        assertTrue(allFacts.containsKey("Attribute1"));
        assertEquals(2, allFacts.get("OSVersion").size());
        assertEquals(2, allFacts.get("Attribute1").size());
    }

    @Test
    void testDetectAnomalies() {
        // Create sample facts
        Map<String, Map<String, Set<String>>> allFacts = new ConcurrentHashMap<>();
        allFacts.put("OSVersion", new ConcurrentHashMap<>());
        allFacts.put("Attribute1", new ConcurrentHashMap<>());
        allFacts.get("OSVersion").put("10", new HashSet<>(Arrays.asList("machine1")));
        allFacts.get("OSVersion").put("11", new HashSet<>(Arrays.asList("machine2")));
        allFacts.get("Attribute1").put("Value1", new HashSet<>(Arrays.asList("machine1")));
        allFacts.get("Attribute1").put("Value2", new HashSet<>(Arrays.asList("machine2", "machine3")));

        // Call the method
        Map<String, List<String>> anomalies = AnomalyDetector.detectAnomalies(allFacts);

        // Verify
        assertTrue(anomalies.containsKey("OSVersion: 10"));
        assertTrue(anomalies.containsKey("OSVersion: 11"));
        assertTrue(anomalies.containsKey("Attribute1: Value1"));
    }

    @Test
    void testReportAnomalies() throws IOException {
        // Create sample anomalies
        Map<String, List<String>> anomalies = new LinkedHashMap<>();
        anomalies.put("OSVersion: 10", Arrays.asList("machine1"));
        anomalies.put("OSVersion: 11", Arrays.asList("machine2"));
        anomalies.put("Attribute1: Value1", Arrays.asList("machine1"));

        // Call the method
        AnomalyDetector.reportAnomalies(anomalies, TEST_OUTPUT_PATH);

        // Verify the output file
        assertTrue(Files.exists(Paths.get(TEST_OUTPUT_PATH)));
        String content = Files.readString(Paths.get(TEST_OUTPUT_PATH));
        assertTrue(content.contains("OSVersion: 10"));
        assertTrue(content.contains("machine1"));
        assertTrue(content.contains("OSVersion: 11"));
        assertTrue(content.contains("machine2"));
        assertTrue(content.contains("Attribute1: Value1"));
    }

    @Test
    void testIsNumeric() {
        assertTrue(AnomalyDetector.isNumeric("123"));
        assertTrue(AnomalyDetector.isNumeric("123.45"));
        assertFalse(AnomalyDetector.isNumeric("abc"));
    }

    @Test
    void testCalculateMean() {
        List<Double> numbers = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        double mean = AnomalyDetector.calculateMean(numbers);
        assertEquals(3.0, mean);
    }

    @Test
    void testCalculateStandardDeviation() {
        List<Double> numbers = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
        double mean = AnomalyDetector.calculateMean(numbers);
        double stdDev = AnomalyDetector.calculateStandardDeviation(numbers, mean);
        assertEquals(Math.sqrt(2.0), stdDev);
    }
}
