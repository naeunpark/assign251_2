package assign251_2;

import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MemAppender extends AbstractAppender {

    private static final String NAME = "MemAppender";
    private static final int DEFAULT_MAX_SIZE = 1000;

    private static MemAppender instance;

    private final List<LogEvent> events;
    private int maxSize = DEFAULT_MAX_SIZE;
    private long discardedLogCount;

    private MemAppender(List<LogEvent> events, Layout<? extends Serializable> layout) {
        super(NAME, null, layout, true, Property.EMPTY_ARRAY);   // [일반] AbstractAppender 생성자
        this.events = Objects.requireNonNull(events, "events list must not be null");
    }

    public static synchronized MemAppender getInstance(List<LogEvent> events,
                                                       Layout<? extends Serializable> layout) {
        if (instance == null) {
            instance = new MemAppender(events, layout);
        }
        return instance;
    }

    public static synchronized MemAppender getInstance(Layout<? extends Serializable> layout) {
        return getInstance(new ArrayList<>(), layout);
    }

    @Override
    public synchronized void append(LogEvent event) {
        discardOldestBeyond(maxSize - 1);
        events.add(event.toImmutable());
    }

    public synchronized List<LogEvent> getCurrentLogs() {
        return List.copyOf(events);
    }

    public synchronized List<String> getEventStrings() {
        Layout<? extends Serializable> layout = requireLayout();
        return events.stream()
                .map(event -> layout.toSerializable(event).toString())
                .toList();
    }

    public synchronized void printLogs() {
        getEventStrings().forEach(System.out::println);
        events.clear();
    }

    public synchronized long getDiscardedLogCount() {
        return discardedLogCount;
    }

    public synchronized void setMaxSize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be positive: " + maxSize);
        }
        this.maxSize = maxSize;
        discardOldestBeyond(maxSize);
    }

    public synchronized int getMaxSize() {
        return maxSize;
    }

    private void discardOldestBeyond(int limit) {
        while (events.size() > limit) {
            events.remove(0);
            discardedLogCount++;
        }
    }

    private Layout<? extends Serializable> requireLayout() {
        Layout<? extends Serializable> layout = getLayout();
        if (layout == null) {
            throw new IllegalStateException("A layout is required for this operation");
        }
        return layout;
    }

}
