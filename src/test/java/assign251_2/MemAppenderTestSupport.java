package assign251_2;

import java.lang.reflect.Field;

final class MemAppenderTestSupport {

    private MemAppenderTestSupport() {
    }

    static void resetInstance() throws ReflectiveOperationException {
        Field instanceField = MemAppender.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
}