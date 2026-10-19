package assign251_2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StressTest {

    private static final int LOG_COUNT = 10_000;
    private static final String MEMORY_PATTERN = "%d %-5level %logger - %msg";

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream console = new ByteArrayOutputStream();

    private Logger logger;
    private Appender appender;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        MemAppenderTestSupport.resetInstance();
        System.setOut(new PrintStream(console));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        logger.removeAppender(appender);
        appender.stop();
    }

    @ParameterizedTest(name = "MemAppender with LinkedList, maxSize {0}")
    @ValueSource(ints = {1, 10, 100, 1000, 10000, 1000000})
    void memAppenderWithLinkedList(int maxSize) {
        testMemAppender(new LinkedList<>(), maxSize, "LinkedList");
    }

    @ParameterizedTest(name = "MemAppender with ArrayList, maxSize {0}")
    @ValueSource(ints = {1, 10, 100, 1000, 10000, 1000000})
    void memAppenderWithArrayList(int maxSize) {
        testMemAppender(new ArrayList<>(), maxSize, "ArrayList");
    }

    @ParameterizedTest(name = "ConsoleAppender with {0}")
    @ValueSource(strings = {"PatternLayout", "VelocityLayout"})
    void consoleAppender(String layoutName) {
        appender = ConsoleAppender.newBuilder()
                .setName("console")
                .setLayout(createLayout(layoutName))
                .setFollow(true)
                .build();
        appender.start();
        logger = createLogger("stress.console." + layoutName);

        measure("ConsoleAppender + " + layoutName, this::logMessages);

        assertEquals(LOG_COUNT, console.toString().lines().count());
    }

    @ParameterizedTest(name = "FileAppender with {0}")
    @ValueSource(strings = {"PatternLayout", "VelocityLayout"})
    void fileAppender(String layoutName) throws IOException {
        Path logFile = tempDir.resolve("stress.log");
        FileAppender fileAppender = FileAppender.newBuilder()
                .setName("file")
                .withFileName(logFile.toString())
                .setLayout(createLayout(layoutName))
                .build();
        fileAppender.start();
        appender = fileAppender;
        logger = createLogger("stress.file." + layoutName);

        measure("FileAppender + " + layoutName, this::logMessages);
        fileAppender.stop();

        assertEquals(LOG_COUNT, Files.readAllLines(logFile).size());
    }

    private void testMemAppender(List<LogEvent> list, int maxSize, String listName) {
        PatternLayout layout = PatternLayout.newBuilder().withPattern(MEMORY_PATTERN).build();
        MemAppender memAppender = MemAppender.getInstance(list, layout);
        memAppender.setMaxSize(maxSize);
        memAppender.start();
        appender = memAppender;
        logger = createLogger("stress.mem." + listName + "." + maxSize);

        measure("MemAppender " + listName + " maxSize " + maxSize, () -> {
            logMessages();
            memAppender.printLogs();
        });

        assertEquals(Math.min(maxSize, LOG_COUNT), console.toString().lines().count());
    }

    private Logger createLogger(String name) {
        Logger newLogger = (Logger) LogManager.getLogger(name);
        newLogger.addAppender(appender);
        newLogger.setAdditive(false);
        newLogger.setLevel(Level.INFO);
        return newLogger;
    }

    private void logMessages() {
        for (int i = 0; i < LOG_COUNT; i++) {
            logger.info("stress message " + i);
        }
    }

    private static Layout<? extends Serializable> createLayout(String layoutName) {
        if (layoutName.equals("VelocityLayout")) {
            return new VelocityLayout("$d $p $c - $m$n");
        }
        return PatternLayout.newBuilder().withPattern("%d %-5level %logger - %msg%n").build();
    }

    private void measure(String label, Runnable work) {
        Runtime runtime = Runtime.getRuntime();
        System.gc();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        long startTime = System.currentTimeMillis();

        work.run();

        long timeMs = System.currentTimeMillis() - startTime;
        long memoryKb = (runtime.totalMemory() - runtime.freeMemory() - memoryBefore) / 1024;
        originalOut.println(label + ": " + timeMs + " ms, " + memoryKb + " KB");
    }
}