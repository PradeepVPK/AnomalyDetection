package ai.asama;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class FactFileUpdaterTest {

    private static final String TEST_INPUT_DIR = "src/test/resources/input";
    private ScheduledExecutorService scheduler;

    @BeforeAll
    static void setup() throws IOException {
        Files.createDirectories(Paths.get(TEST_INPUT_DIR));
    }

    @AfterEach
    void cleanup() throws IOException {
        Files.walk(Paths.get(TEST_INPUT_DIR))
                .filter(Files::isRegularFile)
                .forEach(file -> file.toFile().delete());
    }



    @Test
    void testUpdateFiles() throws IOException, InterruptedException {
        // Initialize FactFileUpdater
        FactFileUpdater.main(new String[]{});

        // Wait a little to let the scheduled task run
        Thread.sleep(2000);

        // Verify files are created
        List<Path> files = Files.list(Paths.get(TEST_INPUT_DIR)).collect(Collectors.toList());

        // Check the content of a few files
        for (int i = 1; i <= 5; i++) {
            Path filePath = Paths.get(TEST_INPUT_DIR, "Machine" + i + ".json");
            String content = Files.readString(filePath);
            JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();

            assertNotNull(jsonObject.get("Model").getAsString(), "Model should not be null");
            assertNotNull(jsonObject.get("OSType").getAsString(), "OSType should not be null");
            assertNotNull(jsonObject.get("OSVersion").getAsInt(), "OSVersion should not be null");
            assertNotNull(jsonObject.get("Serial").getAsString(), "Serial should not be null");
        }
    }
}
