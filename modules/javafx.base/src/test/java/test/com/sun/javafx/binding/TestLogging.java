package test.com.sun.javafx.binding;

import com.sun.javafx.binding.Logging;
import org.junit.Test;
import test.util.memory.JMemoryBuddy;

public class TestLogging {

    @Test
    public void testExceptionCollectableAfterLogging() {

        JMemoryBuddy.memoryTest(checker -> {
            Throwable e = new Exception();

            // This is the value that is used in the application
            // other test might set it to true
            Logging.keepException = false;

            Logging.getLogger().warning("test", e);

            checker.assertCollectable(e);
        });
    }
}
