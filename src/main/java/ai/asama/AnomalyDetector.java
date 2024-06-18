package ai.asama;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * AnomalyDetector class performs anomaly detection based on machine data stored in JSON files.
 * It includes methods to read machine data, detect anomalies, and report them to a JSON output file.
 * Anomalies are detected based on predefined thresholds and standard deviations for certain attributes.
 */
public class AnomalyDetector {

    // Constants for anomaly detection thresholds
    private static final int FILE_BATCH_SIZE = 10;  // Number of files to process in each batch
    private static final int ANOMALY_THRESHOLD = 5; // Minimum number of occurrences to flag as anomaly
    private static final double STANDARD_DEVIATION_THRESHOLD = 2.0; // Threshold for standard deviation
    private static final int DETECTION_INTERVAL = 1; // Interval in minutes for running detection tasks

    // Predefined sets of OS versions for different operating systems
    private static final Set<String> LINUX_VERSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("3", "4", "5")));
    private static final Set<String> WINDOWS_VERSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("7", "8", "10", "11")));
    private static final Set<String> MAC_VERSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("11", "12", "13", "14")));

    /**
     * Main method to initiate anomaly detection process.
     * Configures the directory path for input data and output path for anomalies JSON file.
     * Schedules the detection task to run at fixed intervals using a thread pool.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        String directoryPath = "src/main/resources/input"; // specify the directory path
        String outputPath = "src/main/resources/output/anomalies.json"; // specify the output path
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        // Task to run anomaly detection periodically
        Runnable detectionTask = () -> {
            try {
                detectAndUpdateAnomalies(directoryPath, outputPath);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        };

        // Schedule the detection task to run at fixed intervals
        scheduler.scheduleAtFixedRate(detectionTask, 0, DETECTION_INTERVAL, TimeUnit.MINUTES);
    }

    /**
     * Perform anomaly detection on the data in the specified directory.
     * Reads machine data from JSON files, detects anomalies, and writes results to the output JSON file.
     *
     * @param directoryPath the path to the directory containing input JSON files
     * @param outputPath    the path to the output JSON file where anomalies will be reported
     * @throws InterruptedException if the execution is interrupted while waiting for a task to complete
     * @throws ExecutionException   if an error occurs during the execution of a task
     */
    public static void detectAndUpdateAnomalies(String directoryPath, String outputPath) throws InterruptedException, ExecutionException {
        // Get list of files in the directory
        List<Path> files = getFiles(directoryPath);

        // Concurrently read files and populate facts
        Map<String, Map<String, Set<String>>> allFacts = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(10); // Thread pool for concurrent file reading

        List<Future<Void>> futures = new ArrayList<>();
        for (int i = 0; i < files.size(); i += FILE_BATCH_SIZE) {
            List<Path> batch = files.subList(i, Math.min(i + FILE_BATCH_SIZE, files.size()));
            futures.add(executor.submit(() -> {
                readFiles(batch, allFacts);
                return null;
            }));
        }

        // Wait for all file reading tasks to complete
        for (Future<Void> future : futures) {
            future.get();
        }
        executor.shutdown();

        // Analyze collected facts and detect anomalies
        Map<String, List<String>> anomalies = detectAnomalies(allFacts);

        // Report detected anomalies to the output JSON file
        reportAnomalies(anomalies, outputPath);
    }

    /**
     * Retrieve a list of files from the specified directory path.
     *
     * @param directoryPath the path to the directory containing files to be processed
     * @return a list of Paths representing files in the directory
     */
    public static List<Path> getFiles(String directoryPath) {
        try (Stream<Path> paths = Files.walk(Paths.get(directoryPath))) {
            return paths.filter(Files::isRegularFile)
                           .collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Read content from files in the specified batch and update the allFacts map.
     * Extracts relevant machine data and updates the allFacts map with attributes and associated machine names.
     *
     * @param files    the list of file paths to read from
     * @param allFacts the map storing all extracted facts from the files
     */
    public static void readFiles(List<Path> files, Map<String, Map<String, Set<String>>> allFacts) {
        Gson gson = new Gson();
        for (Path file : files) {
            try (Reader reader = Files.newBufferedReader(file)) {
                JsonObject jsonObject = gson.fromJson(reader, JsonObject.class);
                String machineName = file.getFileName().toString();

                // Process each attribute in the JSON object
                jsonObject.entrySet().forEach(entry -> {
                    String key = entry.getKey();
                    // Exclude 'Serial' attribute from processing
                    if (!key.equals("Serial")) {
                        String value = entry.getValue().getAsString();
                        allFacts.computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                                .computeIfAbsent(value, v -> new HashSet<>())
                                .add(machineName);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Detect anomalies in the collected facts.
     * Analyzes extracted facts and identifies anomalies based on predefined thresholds and criteria.
     *
     * @param allFacts the map containing all extracted facts from machine data
     * @return a map of detected anomalies where each entry represents an anomaly and associated machine names
     */
    public static Map<String, List<String>> detectAnomalies(Map<String, Map<String, Set<String>>> allFacts) {
        Map<String, List<String>> anomalies = new LinkedHashMap<>();

        // Iterate through each fact and check for anomalies
        for (Map.Entry<String, Map<String, Set<String>>> entry : allFacts.entrySet()) {
            String factName = entry.getKey();
            Map<String, Set<String>> valueToMachinesMap = entry.getValue();

            // Detect OS version anomalies
            if (factName.equals("OSVersion")) {
                detectOSVersionAnomalies(factName, valueToMachinesMap, anomalies);
            }

            // Check for threshold anomalies for all other attributes
            for (Map.Entry<String, Set<String>> valueEntry : valueToMachinesMap.entrySet()) {
                String value = valueEntry.getKey();
                Set<String> machines = valueEntry.getValue();

                // Flag as anomaly if number of occurrences is below threshold
                if (machines.size() < ANOMALY_THRESHOLD) {
                    addAnomaly(factName, value, machines, anomalies);
                }
            }
        }

        return anomalies;
    }

    /**
     * Detect anomalies in OS versions based on predefined sets of expected versions and calculate the standard deviation.
     * Identifies OS versions that do not fall within the expected ranges for Linux, Windows, or Mac.
     *
     * @param factName         the name of the attribute being analyzed (e.g., OSVersion)
     * @param valueToMachinesMap the map of attribute values to associated machine names
     * @param anomalies        the map to store detected anomalies
     */
    public static void detectOSVersionAnomalies(String factName, Map<String, Set<String>> valueToMachinesMap, Map<String, List<String>> anomalies) {
        List<Double> versionNumbers = new ArrayList<>();

        // Collect version numbers based on the OS type
        for (String version : valueToMachinesMap.keySet()) {
            if (isNumeric(version)) {
                versionNumbers.add(Double.parseDouble(version));
            }
        }

        // Calculate mean and standard deviation
        double mean = calculateMean(versionNumbers);
        double standardDeviation = calculateStandardDeviation(versionNumbers, mean);

        // Determine anomalies based on standard deviation threshold
        for (String version : valueToMachinesMap.keySet()) {
            if (isNumeric(version)) {
                double versionNumber = Double.parseDouble(version);
                if (Math.abs(versionNumber - mean) > STANDARD_DEVIATION_THRESHOLD * standardDeviation) {
                    addAnomaly(factName, version, valueToMachinesMap.get(version), anomalies);
                }
            } else {
                addAnomaly(factName, version, valueToMachinesMap.get(version), anomalies);
            }
        }
    }
    /**
     * Add an anomaly to the anomalies map.
     *
     * @param factName the name of the attribute associated with the anomaly
     * @param value    the attribute value considered an anomaly
     * @param machines the set of machine names associated with the anomaly
     * @param anomalies the map storing detected anomalies
     */
    private static void addAnomaly(String factName, String value, Set<String> machines, Map<String, List<String>> anomalies) {
        String anomalyKey = factName + ": " + value;
        anomalies.computeIfAbsent(anomalyKey, k -> new ArrayList<>()).addAll(machines);
    }

    /**
     * Report the detected anomalies to a JSON file.
     *
     * @param anomalies  the map of detected anomalies, where keys represent anomalies and values represent associated machine names
     * @param outputPath the path to the output JSON file where anomalies will be reported
     */
    public static void reportAnomalies(Map<String, List<String>> anomalies, String outputPath) {
        Gson gson = new Gson();
        Map<String, Set<String>> anomaliesSet = new LinkedHashMap<>();

        // Convert List<String> to Set<String> for each anomaly
        for (Map.Entry<String, List<String>> entry : anomalies.entrySet()) {
            String key = entry.getKey();
            List<String> machinesList = entry.getValue();
            Set<String> machinesSet = new HashSet<>(machinesList); // Convert list to set to remove duplicates
            anomaliesSet.put(key, machinesSet);
        }

        // Write anomalies to the output JSON file
        try (Writer writer = Files.newBufferedWriter(Paths.get(outputPath))) {
            gson.toJson(anomaliesSet, writer);
            System.out.println("Anomalies reported to: " + outputPath + " at " + new Date());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if a string represents a numeric value.
     *
     * @param str the string to check
     * @return true if the string is numeric, false otherwise
     */
    public static boolean isNumeric(String str) {
        return str != null && str.matches("-?\\d+(\\.\\d+)?");
    }

    /**
     * Calculate the mean of a list of numbers.
     *
     * @param numbers the list of numbers
     * @return the mean of the numbers
     */
    public static double calculateMean(List<Double> numbers) {
        double sum = 0.0;
        for (double num : numbers) {
            sum += num;
        }
        return sum / numbers.size();
    }

    /**
     * Calculate the standard deviation of a list of numbers.
     *
     * @param numbers the list of numbers
     * @param mean    the mean of the numbers
     * @return the standard deviation of the numbers
     */
    public static double calculateStandardDeviation(List<Double> numbers, double mean) {
        double sum = 0.0;
        for (double num : numbers) {
            sum += Math.pow(num - mean, 2);
        }
        return Math.sqrt(sum / numbers.size());
    }
}
