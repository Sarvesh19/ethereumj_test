package org.ethereum.jsontestsuite.suite;

import org.ethereum.config.SystemProperties;
import org.ethereum.config.net.MainNetConfig;
import org.ethereum.jsontestsuite.GitHubJSONTestSuite;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static org.ethereum.jsontestsuite.suite.JSONReader.listJsonBlobsForTreeSha;
import static org.ethereum.jsontestsuite.suite.JSONReader.loadJSONFromCommit;

/**
 * @author Mikhail Kalinin
 * @since 11.08.2017
 */
public class BlockchainTestSuite {

    private static Logger logger = LoggerFactory.getLogger("TCK-Test");

    private static final String TEST_ROOT = "BlockchainTests/";

    String commitSHA;
    List<String> files;
    GitHubJSONTestSuite.Network[] networks;

    public BlockchainTestSuite(String treeSHA, String commitSHA, GitHubJSONTestSuite.Network[] networks) {
        files = listJsonBlobsForTreeSha(treeSHA);
        this.commitSHA = commitSHA;
        this.networks = networks;
    }

    public BlockchainTestSuite(String treeSHA, String commitSHA) {
        this(treeSHA, commitSHA, GitHubJSONTestSuite.Network.values());
    }

    private static void run(List<String> checkFiles,
                            String commitSHA,
                            GitHubJSONTestSuite.Network[] networks) throws IOException {

        if (checkFiles.isEmpty()) return;

        List<BlockTestSuite> suites = new ArrayList<>();
        for (String file : checkFiles) {
            String json = loadJSONFromCommit(TEST_ROOT + file, commitSHA);
            suites.add(new BlockTestSuite(json));
        }

        Map<String, BlockTestCase> testCases = new HashMap<>();
        for (BlockTestSuite suite : suites) {
            Map<String, BlockTestCase> suiteCases = suite.getTestCases(networks);
            testCases.putAll(suiteCases);
        }

        Map<String, Boolean> summary = new HashMap<>();

        for (String testName : testCases.keySet()) {

            BlockTestCase blockTestCase =  testCases.get(testName);
            TestRunner runner = new TestRunner();

            logger.info("\n\n ***************** Running test: {} ***************************** \n\n", testName);

            try {

                SystemProperties.getDefault().setBlockchainConfig(blockTestCase.getConfig());

                List<String> result = runner.runTestCase(blockTestCase);

                logger.info("--------- POST Validation---------");
                if (!result.isEmpty())
                    for (String single : result)
                        logger.info(single);

                if (!result.isEmpty())
                    summary.put(testName, false);
                else
                    summary.put(testName, true);

            } finally {
                SystemProperties.getDefault().setBlockchainConfig(MainNetConfig.INSTANCE);
            }
        }


        logger.info("");
        logger.info("");
        logger.info("Summary: ");
        logger.info("=========");

        int fails = 0; int pass = 0;
        for (String key : summary.keySet()){

            if (summary.get(key)) ++pass; else ++fails;
            String sumTest = String.format("%-60s:^%s", key, (summary.get(key) ? "OK" : "FAIL")).
                    replace(' ', '.').
                    replace("^", " ");
            logger.info(sumTest);
        }

        logger.info(" - Total: Pass: {}, Failed: {} - ", pass, fails);

        Assert.assertTrue(fails == 0);
    }

    public void runAll(String testCaseRoot, Set<String> excludedCases, GitHubJSONTestSuite.Network[] networks) throws IOException {

        List<String> testCaseFiles = new ArrayList<>();
        for (String file : files) {
            if (file.startsWith(testCaseRoot + "/")) testCaseFiles.add(file);
        }

        Set<String> toExclude = new HashSet<>();
        for (String file : testCaseFiles) {
            String fileName = file.substring(file.lastIndexOf('/') + 1, file.lastIndexOf(".json"));
            if (excludedCases.contains(fileName)) {
                logger.info(" [X] " + file);
                toExclude.add(file);
            } else {
                logger.info("     " + file);
            }
        }

        testCaseFiles.removeAll(toExclude);

        run(testCaseFiles, commitSHA, networks);
    }

    public void runAll(String testCaseRoot) throws IOException {
        runAll(testCaseRoot, Collections.<String>emptySet(), networks);
    }

    public void runAll(String testCaseRoot, Set<String> excludedCases) throws IOException {
        runAll(testCaseRoot, excludedCases, networks);
    }

    public void runAll(String testCaseRoot, GitHubJSONTestSuite.Network network) throws IOException {
        runAll(testCaseRoot, Collections.<String>emptySet(), new GitHubJSONTestSuite.Network[]{network});
    }

    public static void runSingle(String testFile, String commitSHA,
                                 GitHubJSONTestSuite.Network network) throws IOException {
        logger.info("     " + testFile);
        run(Collections.singletonList(testFile), commitSHA, new GitHubJSONTestSuite.Network[] { network });
    }

    public static void runSingle(String testFile, String commitSHA) throws IOException {
        logger.info("     " + testFile);
        run(Collections.singletonList(testFile), commitSHA, GitHubJSONTestSuite.Network.values());
    }
}
