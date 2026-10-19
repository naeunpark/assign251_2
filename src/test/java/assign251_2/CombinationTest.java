package assign251_2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CombinationTest {

    private static final String VELOCITY_PATTERN = "[$p] $c: $m";
    private static final String NEW_LINE = System.lineSeparator();

    private final List<Runnable> cleanups = new ArrayList<>();

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        MemAppenderTestSupport.resetInstance();
    }

    @AfterEach
    void tearDown() {
        cleanups.forEach(Runnable::run);
    }

    private <T extends Appender> T started(T appender) {
        appender.start();
        cleanups.add(appender::stop);
        return appender;
    }

    private MemAppender startedMemAppender(Layout<? extends Serializable> layout) {
        return started(MemAppender.getInstance(new ArrayList<>(), layout));
    }

    private Logger attach(String loggerName, Level level, Appender appender) {
        Logger logger = (Logger) LogManager.getLogger(loggerName);
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(level);
        cleanups.add(() -> logger.removeAppender(appender));
        return logger;
    }

    @Test
    void memAppenderWorksWithVelocityLayout() {
        MemAppender appender = startedMemAppender(new VelocityLayout(VELOCITY_PATTERN));
        attach("combo.mem.velocity", Level.ALL, appender).info("hello");

        assertEquals(List.of("[INFO] combo.mem.velocity: hello"), appender.getEventStrings());
    }

    @Test
    void consoleAppenderWorksWithVelocityLayout() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured, true));
        try {
            ConsoleAppender console = started(ConsoleAppender.newBuilder()
                    .setName("console")
                    .setLayout(new VelocityLayout(VELOCITY_PATTERN + "$n"))
                    .setFollow(true)
                    .build());
            attach("combo.console.velocity", Level.ALL, console).warn("careful");
        } finally {
            System.setOut(originalOut);
        }

        assertEquals("[WARN] combo.console.velocity: careful" + NEW_LINE, captured.toString());
    }

    @Test
    void fileAppenderWorksWithVelocityLayout() throws Exception {
        Path logFile = tempDir.resolve("velocity.log");
        FileAppender file = started(FileAppender.newBuilder()
                .setName("file")
                .withFileName(logFile.toString())
                .setLayout(new VelocityLayout(VELOCITY_PATTERN + "$n"))
                .build());
        attach("combo.file.velocity", Level.ALL, file).error("broken");
        file.stop();

        assertEquals("[ERROR] combo.file.velocity: broken" + NEW_LINE, Files.readString(logFile));
    }

    @Test
    void memAppenderWorksWithPatternLayoutAcrossLoggers() {
        MemAppender appender = startedMemAppender(
                PatternLayout.newBuilder().withPattern("%c %p %m").build());
        attach("combo.first", Level.ALL, appender).info("one");
        attach("combo.second", Level.ALL, appender).error("two");

        assertEquals(List.of("combo.first INFO one", "combo.second ERROR two"),
                appender.getEventStrings());
    }

    @Test
    void eachLoggerFiltersByItsOwnLevel() {
        MemAppender appender = startedMemAppender(new VelocityLayout(VELOCITY_PATTERN));
        Logger strict = attach("combo.strict", Level.ERROR, appender);
        Logger verbose = attach("combo.verbose", Level.DEBUG, appender);

        strict.warn("hidden");
        strict.error("shown");
        verbose.debug("also shown");

        assertEquals(List.of("[ERROR] combo.strict: shown", "[DEBUG] combo.verbose: also shown"),
                appender.getEventStrings());
    }
}