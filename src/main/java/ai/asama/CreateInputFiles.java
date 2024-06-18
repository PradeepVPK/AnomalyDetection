package ai.asama;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * CreateInputFiles generates a set of JSON input files representing machine data
 * with randomized attributes such as model, OS type, OS version, and serial number.
 * Specific files are generated with constrained OS versions to simulate anomalies.
 *
 * The generated files are stored in a specified directory path (`src/test/resources/input`).
 *
 * Key Features:
 * - Generates a specified number of JSON files (`NUM_FILES`) with randomized attributes.
 * - Supports multiple OS types (Windows, Linux, MacOS) and specific OS versions.
 * - Ensures specific files are generated with constrained OS versions to simulate anomalies.
 *
 * Constants:
 * - NUM_FILES: Total number of machine data files to generate.
 * - GSON: Gson instance for JSON serialization.
 * - DIRECTORY_PATH: Directory path where generated JSON files are stored.
 * - MODELS: List of available machine models to choose from randomly.
 * - OS_TYPES: List of available operating system types (Windows, Linux, MacOS).
 * - OS_VERSIONS: Map specifying OS types and their respective available OS versions.
 *
 * Methods:
 * - main: Entry point of the application. Creates the directory if it doesn't exist
 *   and invokes the method to create specific input files.
 * - createSpecificFiles: Generates specific input files with constrained OS versions.
 *   Uses methods `createFilesWithSpecificOSVersions` and `createFile` to create the files.
 * - createFilesWithSpecificOSVersions: Generates a specified number of files with a
 *   specific OS type and OS versions. Invokes `createFile` to create each file.
 * - createFile: Creates a single JSON file with randomized attributes (model, OSType,
 *   OSVersion, Serial) using Gson for serialization.
 *
 * Dependencies:
 * - Gson library for JSON serialization and deserialization.
 * - Java NIO (java.nio.file) for file operations, ensuring efficient file handling.
 *
 * Note:
 * - This class assumes that the specified directory path (`src/test/resources/input`)
 *   exists or will be created. It handles IOExceptions during file operations by
 *   throwing them or letting them propagate up the call stack.
 */
public class CreateInputFiles {

    private static final int NUM_FILES = 100;
    private static final Gson GSON = new Gson();
    private static final String DIRECTORY_PATH = "src/main/resources/input";

    private static final List<String> MODELS = Arrays.asList("lenovo thinkpad", "dell xps", "macbook pro", "hp spectre");
    private static final List<String> OS_TYPES = Arrays.asList("Windows", "Linux", "MacOS");

    private static final Map<String, List<Integer>> OS_VERSIONS = new HashMap<String, List<Integer>>() {{
        put("Windows", Arrays.asList(7, 8, 10, 11, 35));
        put("Linux", Arrays.asList(3,4,5));
        put("MacOS", Arrays.asList(11, 12, 13, 14));
    }};

    /**
     * Entry point of the application. Creates the directory for storing input files
     * if it doesn't already exist and invokes the method to generate specific input files.
     *
     * @param args command-line arguments (not used)
     * @throws IOException if an I/O error occurs during directory creation or file writing
     */
    public static void main(String[] args) throws IOException {
        Files.createDirectories(Paths.get(DIRECTORY_PATH));
        createSpecificFiles();
    }

    /**
     * Generates specific input files with constrained OS versions to simulate anomalies.
     * Uses `createFilesWithSpecificOSVersions` to create files with specific OS versions
     * and counts, and creates additional files with common OS versions.
     *
     * @throws IOException if an I/O error occurs during file creation
     */
    private static void createSpecificFiles() throws IOException {
        int fileCount = 1;

        // Create files with specific OSVersion values
        createFilesWithSpecificOSVersions("Windows", Arrays.asList(35), 3, fileCount);
        fileCount += 3;
        createFilesWithSpecificOSVersions("Linux", Arrays.asList(21), 10, fileCount);
        fileCount += 10;

        // Create remaining files with standard OS versions ensuring some values appear less than 5 times
        createFilesWithSpecificOSVersions("Windows", Arrays.asList(7, 8, 10, 11), 35, fileCount);
        fileCount += 35;
        createFilesWithSpecificOSVersions("MacOS", Arrays.asList(11, 12, 13, 14), 52, fileCount);
    }

    /**
     * Generates a specified number of input files with a specific OS type and OS versions.
     * Uses `createFile` to create each file with randomized attributes.
     *
     * @param osType    the type of operating system (Windows, Linux, MacOS)
     * @param osVersions the list of OS versions to use for creating files
     * @param count     the number of files to create with these OS versions
     * @param startIndex the starting index for naming the files
     * @throws IOException if an I/O error occurs during file creation
     */
    private static void createFilesWithSpecificOSVersions(String osType, List<Integer> osVersions, int count, int startIndex) throws IOException {
        for (int i = 0; i < count; i++) {
            int osVersion = osVersions.get(i % osVersions.size());
            createFile(DIRECTORY_PATH, "Machine" + (startIndex + i) + ".json", osType, osVersion);
        }
    }

    /**
     * Creates a single JSON input file with randomized attributes (model, OSType, OSVersion,
     * Serial) using Gson for serialization.
     *
     * @param directoryPath the directory path where the file is stored
     * @param fileName      the name of the JSON file to create
     * @param osType        the type of operating system (Windows, Linux, MacOS)
     * @param osVersion     the OS version to include in the file
     * @throws IOException if an I/O error occurs during file creation
     */
    private static void createFile(String directoryPath, String fileName, String osType, int osVersion) throws IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("Model", MODELS.get(new Random().nextInt(MODELS.size())));
        jsonObject.addProperty("OSType", osType);
        jsonObject.addProperty("OSVersion", osVersion);
        jsonObject.addProperty("Serial", "ABCD" + (1000 + new Random().nextInt(9000)));

        String jsonString = GSON.toJson(jsonObject);

        Path filePath = Paths.get(directoryPath, fileName);
        Files.write(filePath, jsonString.getBytes());
    }
}
