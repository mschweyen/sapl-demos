/*******************************************************************************
 * Copyright 2017-2018 Dominic Heutelbeck (dheutelbeck@ftk.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package io.sapl.benchmark;

import com.google.common.base.Strings;
import com.google.gson.Gson;
import io.sapl.api.functions.FunctionException;
import io.sapl.api.interpreter.PolicyEvaluationException;
import io.sapl.api.pdp.AuthorizationDecision;
import io.sapl.api.pdp.AuthorizationSubscription;
import io.sapl.api.pdp.PDPConfigurationException;
import io.sapl.api.pdp.PolicyDecisionPoint;
import io.sapl.api.pip.AttributeException;
import io.sapl.db.BenchmarkResult;
import io.sapl.db.BenchmarkResultRepository;
import io.sapl.pdp.embedded.EmbeddedPolicyDecisionPoint;
import io.sapl.pdp.embedded.EmbeddedPolicyDecisionPoint.Builder.IndexType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jxls.template.SimpleExporter;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.BitmapEncoder.BitmapFormat;
import org.knowm.xchart.CategoryChart;
import org.knowm.xchart.XYChart;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.sapl.pdp.embedded.EmbeddedPolicyDecisionPoint.Builder.IndexType.FAST;
import static io.sapl.pdp.embedded.EmbeddedPolicyDecisionPoint.Builder.IndexType.IMPROVED;
import static io.sapl.pdp.embedded.EmbeddedPolicyDecisionPoint.Builder.IndexType.SIMPLE;

@Slf4j
@Component
@RequiredArgsConstructor
public class Benchmark implements CommandLineRunner {

    private static final int DEFAULT_HEIGHT = 1080;

    private static final int DEFAULT_WIDTH = 1920;

    private static final String ERROR_READING_TEST_CONFIGURATION = "Error reading test configuration";

    private static final String ERROR_WRITING_BITMAP = "Error writing bitmap";

    private static final String EXPORT_PROPERTIES = "number, name, preparation, duration, request, response";

    private static final String EXPORT_PROPERTIES_AGGREGATES = "name, min, max, avg, mdn";

    private static final String DEFAULT_PATH = "/Users/marclucasbaur/sapl/";

    private static final String HELP_DOC = "print this message";

    private static final String HELP = "help";

    private static final String REUSE = "reuse";

    private static final String REUSE_DOC = "reuse existing policies (true, false)";

    private static final String INDEX = "index";

    private static final String INDEX_DOC = "index type used (SIMPLE, FAST, IMPROVED)";

    private static final String TEST = "test";

    private static final String TEST_DOC = "JSON file containing test definition";

    private static final String USAGE = "java -jar sapl-benchmark-1.0.0-SNAPSHOT-jar-with-dependencies.jar";

    private static final String PATH = "path";

    private static final String PATH_DOC = "path for output files";

    private static final int ITERATIONS = 10;

    private static final int RUNS = 30;

    private static final double MILLION = 1000000.0D;

    private String path = DEFAULT_PATH;

    private IndexType indexType = FAST;

    private boolean reuseExistingPolicies = true;

    private String testFilePath = DEFAULT_PATH + "tests/tests.json";

    private String filePrefix;

    private static final Gson GSON = new Gson();

    private final BenchmarkResultRepository repository;

    @Override
    public void run(String... args) throws Exception {
        LOGGER.info("command line runner started");

        parseCommandLineArguments(args);
        filePrefix = String.format("%s_%s/", LocalDateTime.now(), indexType);

        LOGGER.info("index={}, reuse={}, testfile={}, filePrefix={}", indexType, reuseExistingPolicies,
                testFilePath, filePrefix);

        runBenchmark(path);

        System.exit(0);
    }

    private void parseCommandLineArguments(String... args) {
        Options options = new Options();

        options.addOption(PATH, true, PATH_DOC);
        options.addOption(HELP, false, HELP_DOC);
        options.addOption(REUSE, true, REUSE_DOC);
        options.addOption(INDEX, true, INDEX_DOC);
        options.addOption(TEST, true, TEST_DOC);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption(HELP)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(USAGE, options);
                return;
            }

            String pathOption = cmd.getOptionValue(PATH);
            if (!Strings.isNullOrEmpty(pathOption)) {
                if (!Files.exists(Paths.get(pathOption))) {
                    throw new IllegalArgumentException("path provided does not exists");
                }
                path = pathOption;
            }

            String reuseOption = cmd.getOptionValue(REUSE);
            if (!Strings.isNullOrEmpty(reuseOption)) {
                switch (reuseOption.toUpperCase()) {
                    case "TRUE":
                        reuseExistingPolicies = true;
                        break;
                    case "FALSE":
                        reuseExistingPolicies = false;
                        break;
                    default:
                        HelpFormatter formatter = new HelpFormatter();
                        formatter.printHelp(USAGE, options);
                        throw new IllegalArgumentException("invalid policy reuse option provided");
                }
            }

            String indexOption = cmd.getOptionValue(INDEX);

            if (!Strings.isNullOrEmpty(indexOption)) {
                LOGGER.info("using index {}", indexOption);
                switch (indexOption.toUpperCase()) {
                    case "FAST":
                        indexType = FAST;
                        break;
                    case "IMPROVED":
                        indexType = IMPROVED;
                        break;
                    case "SIMPLE":
                        indexType = SIMPLE;
                        break;
                    default:
                        HelpFormatter formatter = new HelpFormatter();
                        formatter.printHelp(USAGE, options);
                        throw new IllegalArgumentException("invalid index option provided");
                }
            }

            String testOption = cmd.getOptionValue(TEST);
            if (!Strings.isNullOrEmpty(testOption)) {
                if (!Files.exists(Paths.get(testOption))) {
                    throw new IllegalArgumentException("test file provided does not exists");
                }
                testFilePath = testOption;
            }


        } catch (ParseException e) {
            LOGGER.info("encountered an error running the demo: {}", e.getMessage(), e);
            System.exit(1);
        }

    }

    public void runBenchmark(String path) throws URISyntaxException {
        String resultPath = path + filePrefix;

        try {
            final Path dir = Paths.get(path, filePrefix);
            Files.createDirectories(dir);
        } catch (IOException e) {
            LOGGER.error(ERROR_READING_TEST_CONFIGURATION, e);
        }

        XYChart chart = new XYChart(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        BenchmarkDataContainer benchmarkDataContainer = new BenchmarkDataContainer(indexType.toString(),
                reuseExistingPolicies, ITERATIONS, RUNS);

        TestSuite suite = generateTestSuite();
        List<PolicyGeneratorConfiguration> configs = suite.getCases();

        for (PolicyGeneratorConfiguration config : configs) {
            benchmarkConfiguration(path, chart, benchmarkDataContainer, config);
        }

        writeOverviewChart(resultPath, chart);

        writeHistogram(resultPath, benchmarkDataContainer);

        writeExcelFile(resultPath, benchmarkDataContainer.getData());

        buildAggregateData(benchmarkDataContainer);
        writeExcelFileAggregates(resultPath, benchmarkDataContainer.getAggregateData());
        persistAggregates(benchmarkDataContainer);
    }

    private TestSuite generateTestSuite() {
//            File testFile = new File(testFilePath);
//            LOGGER.info("using testfile: {}", testFile);
//            List<String> allLines = Files.readAllLines(Paths.get(testFile.toURI()));
//            String allLinesAsString = StringUtils.join(allLines, "");
//            suite = GSON.fromJson(allLinesAsString, TestSuite.class);
        TestSuite suite = TestSuiteGenerator.generate();

        Objects.requireNonNull(suite, "test suite is null");
        Objects.requireNonNull(suite.getCases(), "test cases are null");
        LOGGER.info("suite contains {} test cases", suite.getCases().size());
        assert !suite.getCases().isEmpty() : "at least one test case must be present";

        return suite;
    }

    private void persistAggregates(BenchmarkDataContainer dataContainer) {
        dataContainer.getAggregateData().stream()
                .map(aggregateRecord -> new BenchmarkResult(dataContainer, aggregateRecord))
                .forEach(repository::save);
    }

    private void buildAggregateData(BenchmarkDataContainer benchmarkDataContainer) {
        for (int i = 0; i < benchmarkDataContainer.getIdentifier().size(); i++) {
            benchmarkDataContainer.getAggregateData().add(new XlsAggregateRecord(benchmarkDataContainer.getIdentifier().get(i),
                    benchmarkDataContainer.getMinValues().get(i), benchmarkDataContainer.getMaxValues().get(i),
                    benchmarkDataContainer.getAvgValues().get(i), benchmarkDataContainer.getMdnValues().get(i)));
        }
    }

    private void benchmarkConfiguration(String path, XYChart chart, BenchmarkDataContainer benchmarkDataContainer,
                                        PolicyGeneratorConfiguration config) throws URISyntaxException {
        benchmarkDataContainer.getIdentifier().add(config.getName());
        List<XlsRecord> results = null;
        try {
            results = runTest(config, path);
        } catch (IOException | PolicyEvaluationException e) {
            LOGGER.error("Error running test", e);
            System.exit(1);
        }
        double[] times = new double[results.size()];
        int i = 0;
        for (XlsRecord item : results) {
            times[i] = item.getDuration();
            i++;
        }

        XYChart details = new XYChart(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        details.setTitle("Evaluation Time");
        details.setXAxisTitle("Run");
        details.setYAxisTitle("ms");
        details.addSeries(config.getName(), times);

        String resultPath = path + filePrefix;

        try {
            BitmapEncoder.saveBitmap(details, resultPath + config.getName()
                    .replaceAll("[^a-zA-Z0-9]", ""), BitmapFormat.PNG);
        } catch (IOException e) {
            LOGGER.error(ERROR_WRITING_BITMAP, e);
            System.exit(1);
        }

        benchmarkDataContainer.getMinValues().add(extractMin(results));
        benchmarkDataContainer.getMaxValues().add(extractMax(results));
        benchmarkDataContainer.getAvgValues().add(extractAvg(results));
        benchmarkDataContainer.getMdnValues().add(extractMdn(results));

        chart.addSeries(config.getName(), times);
        benchmarkDataContainer.getData().addAll(results);
    }

    private void writeOverviewChart(String path, XYChart chart) {
        chart.setTitle("Evaluation Time");
        chart.setXAxisTitle("Run");
        chart.setYAxisTitle("ms");
        try {
            BitmapEncoder.saveBitmap(chart, path + "overview", BitmapFormat.PNG);
        } catch (IOException e) {
            LOGGER.error(ERROR_WRITING_BITMAP, e);
            System.exit(1);
        }
    }

    private void writeHistogram(String path, BenchmarkDataContainer benchmarkDataContainer) {

        CategoryChart histogram = new CategoryChart(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        histogram.setTitle("Aggregates");
        histogram.setXAxisTitle("Run");
        histogram.setYAxisTitle("ms");
        histogram.addSeries("min", benchmarkDataContainer.getIdentifier(), benchmarkDataContainer.getMinValues());
        histogram.addSeries("max", benchmarkDataContainer.getIdentifier(), benchmarkDataContainer.getMaxValues());
        histogram.addSeries("avg", benchmarkDataContainer.getIdentifier(), benchmarkDataContainer.getAvgValues());
        histogram.addSeries("mdn", benchmarkDataContainer.getIdentifier(), benchmarkDataContainer.getMdnValues());

        try {
            BitmapEncoder.saveBitmap(histogram, path + "histogram", BitmapFormat.PNG);
        } catch (IOException e) {
            LOGGER.error(ERROR_WRITING_BITMAP, e);
            System.exit(1);
        }
    }

    private void writeExcelFile(String path, List<XlsRecord> data) {
        try (OutputStream os = Files.newOutputStream(Paths.get(path, "overview.xls"))) {
            SimpleExporter exp = new SimpleExporter();
            exp.gridExport(getExportHeader(), data, EXPORT_PROPERTIES, os);
        } catch (IOException e) {
            LOGGER.error("Error writing XLS", e);
            System.exit(1);
        }
    }

    private void writeExcelFileAggregates(String path, List<XlsAggregateRecord> data) {
        try (OutputStream os = Files.newOutputStream(Paths.get(path, "histogram.xls"))) {
            SimpleExporter exp = new SimpleExporter();
            exp.gridExport(getExportHeaderAggregates(), data, EXPORT_PROPERTIES_AGGREGATES, os);
        } catch (IOException e) {
            LOGGER.error("Error writing XLS", e);
            System.exit(1);
        }
    }

    private List<XlsRecord> runTest(PolicyGeneratorConfiguration config, String path)
            throws IOException, URISyntaxException, PolicyEvaluationException {


        PolicyGenerator generator = new PolicyGenerator(config);

        String subfolder = config.getName().replaceAll("[^a-zA-Z0-9]", "");
        if (!reuseExistingPolicies) {
            generator.generatePolicies(subfolder);

            final Path dir = Paths.get(path, subfolder);
            Files.createDirectories(dir);
            Files.copy(Paths.get(path, "pdp.json"), Paths.get(path, subfolder, "pdp.json"),
                    StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        }

        List<XlsRecord> results = new LinkedList<>();

        try {
            for (int i = 0; i < ITERATIONS; i++) {
                long begin = System.nanoTime();
                PolicyDecisionPoint pdp = EmbeddedPolicyDecisionPoint.builder()
                        .withFilesystemPolicyRetrievalPoint(path + subfolder, indexType)
                        .withFilesystemPDPConfigurationProvider(path + subfolder).build();
                double prep = nanoToMs(System.nanoTime() - begin);

                for (int j = 0; j < RUNS; j++) {
                    AuthorizationSubscription request = generator.createAuthorizationSubscriptionObject();

                    long start = System.nanoTime();
                    AuthorizationDecision authzDecision = pdp.decide(request).blockFirst();
                    long end = System.nanoTime();

                    double diff = nanoToMs(end - start);

                    if (authzDecision == null) {
                        throw new IOException("PDP returned null authzDecision");
                    }
                    results.add(new XlsRecord(j + (i * RUNS), config.getName(), prep, diff, request.toString(),
                            authzDecision.toString()));

                    LOGGER.debug("Total : {}ms", diff);
                }
            }
        } catch (IOException | AttributeException | FunctionException | PDPConfigurationException e) {
            LOGGER.error("Error running test", e);
        }

        return results;
    }

    private double nanoToMs(long nanoseconds) {
        return nanoseconds / MILLION;
    }

    private List<String> getExportHeader() {
        return Arrays.asList("Iteration", "Test Case", "Preparation Time (ms)", "Execution Time (ms)", "Request String",
                "Response String (ms)");
    }

    private List<String> getExportHeaderAggregates() {
        return Arrays.asList("Test Case", "Minimum Time (ms)", "Maximum Time (ms)", "Average Time (ms)",
                "Median Time (ms)");
    }

    private double extractMin(List<XlsRecord> data) {
        double min = Double.MAX_VALUE;
        for (XlsRecord item : data) {
            if (item.getDuration() < min) {
                min = item.getDuration();
            }
        }
        return min;
    }

    private double extractMax(List<XlsRecord> data) {
        double max = Double.MIN_VALUE;
        for (XlsRecord item : data) {
            if (item.getDuration() > max) {
                max = item.getDuration();
            }
        }
        return max;
    }

    private double extractAvg(List<XlsRecord> data) {
        double sum = 0;
        for (XlsRecord item : data) {
            sum += item.getDuration();
        }
        return sum / data.size();
    }

    private double extractMdn(List<XlsRecord> data) {
        List<Double> list = data.stream().map(XlsRecord::getDuration).sorted().collect(Collectors.toList());
        int index = list.size() / 2;
        if (list.size() % 2 == 0) {
            return (list.get(index) + list.get(index - 1)) / 2;
        } else {
            return list.get(index);
        }
    }


}
