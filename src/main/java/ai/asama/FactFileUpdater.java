package ai.asama;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * FactFileUpdater periodically updates machine data files with randomized attributes
 * such as model, OS type, and OS version. It ensures specific constraints on OS versions
 * are met while also introducing variability through common OS versions.
 *
 * The updater operates on a specified directory path, creating or updating JSON files
 * representing machine data. Each file contains attributes such as Model, OSType, OSVersion,
 * and Serial number, serialized as JSON objects using Gson library.
 *
 * Key Features:
 * - Generates machine data files with random attributes using predefined lists for models,
 *   OS types (Windows, MacOS, Linux), and both specific and common OS versions.
 * - Periodically updates files at a fixed interval using a scheduled executor service.
 * - Ensures specific OS version constraints are respected by counting occurrences of
 *   specific versions and choosing from common versions when necessary.
 *
 * Constants:
 * - NUM_FILES: Number of machine data files to generate/update.
 * - UPDATE_INTERVAL: Interval at which files are updated, specified in minutes.
 * - RANDOM: Random number generator used for random selection of attributes.
 * - MODELS: List of available machine models to choose from randomly.
 * - OS_TYPES: List of available operating system types (Windows, MacOS, Linux).
 * - WINDOWS_VERSIONS, MAC_VERSIONS, LINUX_VERSIONS: Lists of specific OS versions for
 *   Windows, MacOS, and Linux respectively.
 * - SPECIFIC_OS_VERSIONS: Map specifying specific OS versions and their maximum allowable
 *   occurrence limits to maintain constraints.
 * - COMMON_WINDOWS_VERSIONS, COMMON_MAC_VERSIONS, COMMON_LINUX_VERSIONS: Lists of common
 *   OS versions for each OS type, providing additional variability.
 *
 * Methods:
 * - main: Entry point of the application, schedules the update task at fixed intervals.
 * - updateFiles: Creates or updates the specified number of machine data files in the
 *   directory path, ensuring directories are created if they do not exist.
 * - updateFile: Generates or updates a single machine data file with randomized attributes,
 *   ensuring specific OS version constraints are observed.
 * - getRandomOSVersion: Retrieves a random OS version based on the provided OS type,
 *   ensuring specific OS versions are prioritized if their occurrence limits are not exceeded.
 *
 * Dependencies:
 * - Gson library for JSON serialization and deserialization.
 * - Java NIO (java.nio.file) for file operations, ensuring efficient file handling.
 * - Java Executors (java.util.concurrent) for managing periodic update tasks with fixed intervals.
 *
 * Note:
 * - This class assumes that the specified directory path (`src/main/resources/input`)
 *   exists or will be created. It also assumes proper handling of IOExceptions during file
 *   operations, printing stack traces if encountered.
 */
public class FactFileUpdater {

    private static final int NUM_FILES = 100;
    private static final int UPDATE_INTERVAL = 1; // in minutes
    private static final Random RANDOM = new Random();
    private static final List<String> MODELS = Arrays.asList("lenovo thinkpad", "dell xps", "macbook pro", "hp spectre");
    private static final List<String> OS_TYPES = Arrays.asList("Windows", "Linux", "MacOS");

    private static final List<Integer> WINDOWS_VERSIONS = Arrays.asList(7, 8, 10, 11);
    private static final List<Integer> MAC_VERSIONS = Arrays.asList(11, 12, 13, 14);
    private static final List<Integer> LINUX_VERSIONS = Arrays.asList(3, 4, 5);

    private static final Map<Integer, Integer> SPECIFIC_OS_VERSIONS = new HashMap() {{
        put(35, 3); // Windows
        put(21, 10); // Linux
    }};
    private static final List<Integer> COMMON_WINDOWS_VERSIONS = Arrays.asList(7, 8, 10, 11);
    private static final List<Integer> COMMON_MAC_VERSIONS = Arrays.asList(11, 12, 13, 14);
    private static final List<Integer> COMMON_LINUX_VERSIONS = Arrays.asList(3, 4, 5);

    /**
     * Entry point of the application. Schedules the update task to run periodically
     * at fixed intervals specified by UPDATE_INTERVAL.
     *
     * @param args command-line arguments (not used)
     * @throws InterruptedException if the thread executing the task is interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        String directoryPath = "src/main/resources/input"; // specify the directory path
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

        Runnable updateTask = () -> {
            try {
                updateFiles(directoryPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        scheduler.scheduleAtFixedRate(updateTask, 0, UPDATE_INTERVAL, TimeUnit.MINUTES);
    }

    /**
     * Creates or updates machine data files in the specified directory path. It ensures
     * directories are created if they do not exist. Handles constraints on specific OS
     * versions by counting occurrences and updating files accordingly.
     *
     * @param directoryPath the directory path where machine data files are stored or created
     * @throws IOException if an I/O error occurs during file creation or update
     */
    static void updateFiles(String directoryPath) throws IOException {
        Files.createDirectories(Paths.get(directoryPath));

        // Count for specific OS versions to ensure constraints
        Map<Integer, Integer> specificCountMap = new HashMap<>();
        for (Map.Entry<Integer, Integer> entry : SPECIFIC_OS_VERSIONS.entrySet()) {
            specificCountMap.put(entry.getKey(), 0);
        }

        for (int i = 1; i <= NUM_FILES; i++) {
            updateFile(directoryPath, "Machine" + i + ".json", specificCountMap);
        }
    }

    /**
     * Generates or updates a single machine data file with randomized attributes such as
     * model, OS type, OS version, and serial number. Respects constraints on specific OS
     * versions by counting occurrences and selecting from common versions when necessary.
     *
     * @param directoryPath   the directory path where the file is stored or created
     * @param fileName        the name of the machine data file
     * @param specificCountMap map tracking occurrences of specific OS versions
     * @throws IOException if an I/O error occurs during file creation or update
     */
    private static void updateFile(String directoryPath, String fileName, Map<Integer, Integer> specificCountMap) throws IOException {
        Gson gson = new Gson();
        JsonObject jsonObject = new JsonObject();

        String model = MODELS.get(RANDOM.nextInt(MODELS.size()));
        String osType = OS_TYPES.get(RANDOM.nextInt(OS_TYPES.size()));
        int osVersion = getRandomOSVersion(osType, specificCountMap);

        jsonObject.addProperty("Model", model);
        jsonObject.addProperty("OSType", osType);
        jsonObject.addProperty("OSVersion", osVersion);
        jsonObject.addProperty("Serial", "ABCD" + (1000 + RANDOM.nextInt(9000)));

        String jsonString = gson.toJson(jsonObject);

        Path filePath = Paths.get(directoryPath, fileName);
        Files.write(filePath, jsonString.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Retrieves a random OS version based on the specified OS type, ensuring constraints
     * on specific OS versions are respected. If the count of specific versions is within
     * the limit, selects from specific versions; otherwise, selects from common versions.
     *
     * @param osType           the type of operating system (Windows, MacOS, Linux)
     * @param specificCountMap map tracking occurrences of specific OS versions
     * @return a random OS version
     */
    private static int getRandomOSVersion(String osType, Map<Integer, Integer> specificCountMap) {
        List<Integer> osVersions;
        if (osType.equals("Windows")) {
            osVersions = new ArrayList<>(WINDOWS_VERSIONS);
            osVersions.addAll(COMMON_WINDOWS_VERSIONS);
        } else if (osType.equals("MacOS")) {
            osVersions = new ArrayList<>(MAC_VERSIONS);
            osVersions.addAll(COMMON_MAC_VERSIONS);
        } else {
            osVersions = new ArrayList<>(LINUX_VERSIONS);
            osVersions.addAll(COMMON_LINUX_VERSIONS);
        }

        // Shuffle to randomize the order
        Collections.shuffle(osVersions);

        // Try to pick a specific OS version if the count is less than the limit
        for (Map.Entry<Integer, Integer> entry : SPECIFIC_OS_VERSIONS.entrySet()) {
            int version = entry.getKey();
            int limit = entry.getValue();
            if (osVersions.contains(version) && specificCountMap.get(version) < limit) {
                specificCountMap.put(version, specificCountMap.get(version) + 1);
                return version;
            }
        }

        // Otherwise, pick a random common OS version
        return osVersions.get(RANDOM.nextInt(osVersions.size()));
    }
}
