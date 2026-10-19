package assign251_2;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public final class VelocityLayout extends AbstractStringLayout {

    private final VelocityEngine velocityEngine = new VelocityEngine();
    private String pattern;

    public VelocityLayout(String pattern) {
        super(StandardCharsets.UTF_8);
        this.pattern = pattern;
        velocityEngine.init();
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public String toSerializable(LogEvent event) {
        StringWriter result = new StringWriter();
        velocityEngine.evaluate(createVariables(event), result, "VelocityLayout", pattern);
        return result.toString();
    }

    private static VelocityContext createVariables(LogEvent event) {
        VelocityContext variables = new VelocityContext();
        variables.put("c", event.getLoggerName());
        variables.put("d", new Date(event.getTimeMillis()).toString());
        variables.put("m", event.getMessage().getFormattedMessage());
        variables.put("p", event.getLevel().toString());
        variables.put("t", event.getThreadName());
        variables.put("n", System.lineSeparator());
        return variables;
    }

    @Override
    public String getContentType() {
        return "text/plain";
    }
}