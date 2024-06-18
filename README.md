# AnomalyDetector

AnomalyDetector is a Java-based application that performs anomaly detection on machine data stored in JSON files. The application reads data from a specified directory, detects anomalies based on predefined thresholds, and reports the detected anomalies to a JSON output file. This project uses Maven for build automation and dependency management, and integrates with JaCoCo for code coverage reporting.

## Features

- Read machine data from JSON files.
- Detect anomalies based on predefined thresholds and standard deviations for certain attributes.
- Report detected anomalies to a JSON output file.
- Scheduled detection tasks using a thread pool.
- High concurrency support for efficient file processing.

## Prerequisites

- Java 8
- Maven 3.2.0 or later

## Getting Started

### Clone the Repository

```bash
git clone [https://github.com/your-username/AnomalyDetector.git](https://github.com/PradeepVPK/AnomalyDetection.git)
cd AnomalyDetector
```
Build the Project
Use Maven to build the project:

```bash
mvn clean install
```
Run the Application
You can run the application using the following Maven command:

```bash
mvn exec:java -Dexec.mainClass="ai.asama.AnomalyDetector"
```
Alternatively, you can run the application from your IDE by executing the main method in the ai.asama.AnomalyDetector class.

Running Tests
To run the tests and generate a code coverage report using JaCoCo, use the following command:

```bash
mvn clean test
Directory Structure
bash
Copy code
AnomalyDetector/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── ai/
│   │   │       └── asama/
│   │   │           ├── AnomalyDetector.java
│   │   │           ├── FactFileUpdater.java
│   │   │           ├── CreateInputFiles.java
│   │   │           └── ... other classes ...
│   │   └── resources/
│   │       ├── input/  # Directory for input JSON files
│   │       └── output/ # Directory for output JSON files
│   └── test/
│       ├── java/
│       │   └── ai/
│       │       └── asama/
│       │           └── AnomalyDetectorTest.java
│       └── resources/
├── pom.xml
└── README.md
```

## Classes
#### AnomalyDetector
This is the main class that initiates the anomaly detection process. It reads data from the specified directory, detects anomalies, and writes the results to a JSON output file.

#### CreateInputFiles
This class generates 100 sample input JSON files with random machine data. It is useful for testing the anomaly detection process with a large dataset.

#### FactFileUpdater
This class reads the input JSON files and updates the facts based on the machine data. It extracts relevant machine data and updates a map with attributes and associated machine names.

## Output Screenshot

![image](https://github.com/PradeepVPK/AnomalyDetection/assets/108991867/cff3d5cb-1184-4687-a63c-538cf0ba51c9)

## Code Coverage Screenshot

![image_720](https://github.com/PradeepVPK/AnomalyDetection/assets/108991867/b08eb141-646d-4776-9b4c-cab6dd942c53)
![image_720](https://github.com/PradeepVPK/AnomalyDetection/assets/108991867/a15465d4-9494-49e2-b429-13692a34d34f)
![image_720](https://github.com/PradeepVPK/AnomalyDetection/assets/108991867/bd23d22b-6151-4b27-ac9b-aa6cbd7f8d09)
![image_720](https://github.com/PradeepVPK/AnomalyDetection/assets/108991867/4a41f1f6-806f-445e-a431-e486d68178a2)


## Dependencies
The project relies on the following main dependencies:

1. Google Gson for JSON parsing.
2. JUnit & Mockito for testing.
3. JaCoCo for code coverage reporting.
