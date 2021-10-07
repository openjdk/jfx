package test.javafx.beans.value;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.ref.WeakReference;

public class ReferenceAsserts {

    public static void testIfStronglyReferenced(Object obj, Runnable clearRefs) {
        WeakReference<Object> ref = new WeakReference<>(obj);

        clearRefs.run();
        obj = null;

        System.gc();

        assertNotNull(ref.get());
    }

    public static void testIfNotStronglyReferenced(Object obj, Runnable clearRefs) {
        WeakReference<Object> ref = new WeakReference<>(obj);

        clearRefs.run();
        obj = null;

        System.gc();

        assertNull(ref.get());
    }
}
