package ai.asama;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class CreateInputFilesTest {

    private static final String TEST_DIRECTORY_PATH = "src/test/resources/input";


    @BeforeEach
    void setUp() throws IOException {
        // Ensure the directory is clean before each test
        if (Files.exists(Paths.get(TEST_DIRECTORY_PATH))) {
            Files.walk(Paths.get(TEST_DIRECTORY_PATH))
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }
        Files.createDirectories(Paths.get(TEST_DIRECTORY_PATH));
    }


    @Test
    void testCreateSpecificFiles() throws IOException {
        // Run the method to create specific files
        CreateInputFiles.main(new String[]{});

        // Check the total number of files
        List<Path> files = Files.walk(Paths.get(TEST_DIRECTORY_PATH))
                                   .filter(Files::isRegularFile)
                                   .collect(Collectors.toList());
        assertEquals(100, files.size(), "There should be exactly 100 JSON files.");

        // Check specific OS versions for anomalies
        Map<String, List<JsonObject>> osVersionMap = files.stream().map(this::readFile).collect(Collectors.groupingBy(obj -> obj.get("OSVersion").getAsString()));

        // Verify the specific constraints
        assertEquals(3, osVersionMap.get("35").size(), "There should be exactly 3 Windows OS version 35 files.");
        assertEquals(10, osVersionMap.get("21").size(), "There should be exactly 10 Linux OS version 21 files.");
        assertFalse(osVersionMap.get("7").size() < 5, "There should be less than 5 occurrences of Windows OS version 7.");
    }

    private JsonObject readFile(Path path) {
        try {
            String content = new String(Files.readAllBytes(path));
            return JsonParser.parseString(content).getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + path, e);
        }
    }
}
