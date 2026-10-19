package assign251_2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MemAppenderTest {

    private List<LogEvent>  injectedList;
    private MemAppender appender;
    private Logger logger;

    @BeforeEach
    void setUp() throws Exception {
        resetForTesting();
        injectedList = new ArrayList<>();
        PatternLayout layout = PatternLayout.newBuilder().withPattern("%p %m").build(); // [일반] 내장 Layout 생성법
        appender = MemAppender.getInstance(injectedList, layout);
        appender.start();

        logger = (Logger) LogManager.getLogger("MemAppenderTest");
        logger.addAppender(appender);
        logger.setAdditive(false);
        logger.setLevel(Level.ALL);
    }

    @AfterEach
    void tearDown() {
        logger.removeAppender(appender);
        appender.stop();
    }

    private static void resetForTesting() throws ReflectiveOperationException {
        Field instanceField = MemAppender.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }

    @Test
    void getInstanceReturnsSameInstance() {
        assertSame(appender, MemAppender.getInstance(new ArrayList<>(), null));
    }

    @Test
    void storesEventsInInjectedList() {
        logger.info("hello");
        assertEquals(1, injectedList.size());
        assertEquals("hello", injectedList.get(0).getMessage().getFormattedMessage());
    }

    @Test
    void getInstanceWithDefaultListWorks() throws Exception {
        resetForTesting();
        MemAppender defaultListAppender =
                MemAppender.getInstance(PatternLayout.newBuilder().withPattern("%m").build());
        assertTrue(defaultListAppender.getCurrentLogs().isEmpty());
    }

    @Test
    void getEventStringsWithoutLayoutThrows() throws Exception {
        resetForTesting();
        MemAppender noLayout = MemAppender.getInstance(new ArrayList<>(), null);
        assertThrows(IllegalStateException.class, noLayout::getEventStrings);
        assertThrows(IllegalStateException.class, noLayout::printLogs);
    }

    @Test
    void getCurrentLogsIsUnmodifiable() {
        logger.info("x");
        List<LogEvent> logs = appender.getCurrentLogs();
        assertThrows(UnsupportedOperationException.class, logs::clear);
    }

    @Test
    void getEventStringsUsesLayoutAndIsUnmodifiable() {
        logger.warn("hi");
        List<String> strings = appender.getEventStrings();
        assertEquals(List.of("WARN hi"), strings);
        assertThrows(UnsupportedOperationException.class, strings::clear);
    }

    @Test
    void printLogsPrintsWithLayoutThenClears() {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        System.setOut(new PrintStream(captured));
        try {
            logger.error("boom");
            appender.printLogs();
        } finally {
            System.setOut(originalOut);
        }
        assertEquals("ERROR boom" + System.lineSeparator(), captured.toString());
        assertTrue(appender.getCurrentLogs().isEmpty());
    }

    @Test
    void oldestLogRemovedWhenMaxSizeReached() {
        appender.setMaxSize(2);
        logger.info("1");
        logger.info("2");
        logger.info("3");
        assertEquals(List.of("INFO 2", "INFO 3"), appender.getEventStrings());
        assertEquals(1, appender.getDiscardedLogCount());
    }

    @Test
    void clearedLogsAreNotCountedAsDiscarded() {
        logger.info("a");
        logger.info("b");
        appender.printLogs();
        assertEquals(0, appender.getDiscardedLogCount());
    }

    @Test
    void setMaxSizeRejectsNonPositive() {
        assertThrows(IllegalArgumentException.class, () -> appender.setMaxSize(0));
        assertThrows(IllegalArgumentException.class, () -> appender.setMaxSize(-1));
    }

    @Test
    void shrinkingMaxSizeDiscardsOldest() {
        logger.info("1");
        logger.info("2");
        logger.info("3");
        appender.setMaxSize(1);
        assertEquals(List.of("INFO 3"), appender.getEventStrings());
        assertEquals(2, appender.getDiscardedLogCount());
    }

    @Test
    void eventsBelowLoggerLevelAreNotStored() {
        logger.setLevel(Level.WARN);
        logger.debug("ignored");
        logger.info("ignored");
        logger.warn("kept");
        assertEquals(List.of("WARN kept"), appender.getEventStrings());
    }
}
