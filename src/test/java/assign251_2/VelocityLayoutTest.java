package assign251_2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VelocityLayoutTest {

    private static final long EVENT_TIME = 1_000_000_000_000L;

    private static final LogEvent EVENT = Log4jLogEvent.newBuilder()
            .setLoggerName("my.logger")
            .setLevel(Level.INFO)
            .setMessage(new SimpleMessage("hello"))
            .setThreadName("main")
            .setTimeMillis(EVENT_TIME)
            .build();

    private static String format(String pattern) {
        return new VelocityLayout(pattern).toSerializable(EVENT);
    }

    @Test
    void loggerNameIsFormatted() {
        assertEquals("my.logger", format("$c"));
    }

    @Test
    void dateIsFormattedWithDefaultToString() {
        assertEquals(new Date(EVENT_TIME).toString(), format("$d"));
    }

    @Test
    void messageIsFormatted() {
        assertEquals("hello", format("$m"));
    }

    @Test
    void levelIsFormatted() {
        assertEquals("INFO", format("$p"));
    }

    @Test
    void threadNameIsFormatted() {
        assertEquals("main", format("$t"));
    }

    @Test
    void lineSeparatorIsFormatted() {
        assertEquals(System.lineSeparator(), format("$n"));
    }

    @Test
    void examplePatternIsFormatted() {
        String expected = "[INFO] my.logger " + new Date(EVENT_TIME) + ": hello";
        assertEquals(expected, format("[$p] $c $d: $m"));
    }

    @Test
    void setterChangesPattern() {
        VelocityLayout layout = new VelocityLayout("$m");
        layout.setPattern("$p");
        assertEquals("INFO", layout.toSerializable(EVENT));
    }
}