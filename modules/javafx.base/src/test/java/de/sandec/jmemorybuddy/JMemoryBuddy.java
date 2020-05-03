package de.sandec.jmemorybuddy;

import com.sun.management.HotSpotDiagnosticMXBean;
import javax.management.MBeanServer;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.function.Consumer;
import java.util.function.Function;

public class JMemoryBuddy {

    static int steps = 10;
    static int overallTime = 1000;
    static int sleepTime = overallTime / steps;
    private static String MX_BEAN_PROXY_TYPE = "com.sun.management:type=HotSpotDiagnostic";

    static String outputFolderString = ".";

    static {
        outputFolderString = System.getProperty("jmemorybuddy.output",".");
        overallTime = Integer.parseInt(System.getProperty("jmemorybuddy.checktime","1000"));
        steps = Integer.parseInt(System.getProperty("jmemorybuddy.steps", "10"));
    }

    public static void createGarbage() {
        LinkedList list = new LinkedList<Integer>();
        int counter = 0;
        while(counter < 999999) {
            counter += 1;
            list.add(1);
        }
    }

    public static void assertCollectable(WeakReference weakReference) {
        if(!checkCollectable(weakReference)) {
            AssertCollectable assertCollectable = new AssertCollectable(weakReference);
            createHeapDump();
            throw new AssertionError("Content of WeakReference was not collected. content: " + weakReference.get());
        }
    }

    public static boolean checkCollectable(WeakReference weakReference) {
        return checkCollectable(steps, weakReference) > 0;
    }

    private static int checkCollectable(int stepsLeft, WeakReference weakReference) {
        int counter = stepsLeft;

        if(weakReference.get() != null) {
            createGarbage();
            System.gc();
            System.runFinalization();
        }

        while(counter > 0 && weakReference.get() != null) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {}
            counter = counter - 1;
            createGarbage();
            System.gc();
            System.runFinalization();
        }

        if(weakReference.get() == null && counter < steps / 3) {
            int percentageUsed = (int) ((steps - counter) / steps * 100);
            System.out.println("Warning test seems to be unstable. time used: " + percentageUsed + "%");
        }

        return counter;
    }

    public static void assertNotCollectable(WeakReference weakReference) {
        if(!checkNotCollectable(weakReference)) {
            throw new AssertionError("Content of WeakReference was collected!");
        }
    }
    public static boolean checkNotCollectable(WeakReference weakReference) {
        System.gc();
        return weakReference.get() != null;
    }

    public static void memoryTest(Consumer<MemoryTestAPI> f) {
        LinkedList<WeakReference> toBeCollected = new LinkedList<WeakReference>();
        LinkedList<WeakReference> toBeNotCollected = new LinkedList<WeakReference>();
        LinkedList<SetAsReferenced> toBeReferenced = new LinkedList<SetAsReferenced>();

        f.accept(new MemoryTestAPI() {
            public void assertCollectable(Object ref) {
                if(ref == null) throw new NullPointerException();
                toBeCollected.add(new WeakReference<Object>(ref));
            }
            public void assertNotCollectable(Object ref) {
                if(ref == null) throw new NullPointerException();
                toBeNotCollected.add(new WeakReference<Object>(ref));
            }
            public void setAsReferenced(Object ref) {
                if(ref == null) throw new NullPointerException();
                toBeReferenced.add(new SetAsReferenced(ref));
            }
        });

        int stepsLeft = steps;
        boolean failed = false;

        for(WeakReference wRef: toBeCollected) {
            stepsLeft = checkCollectable(stepsLeft, wRef);
        }
        if(stepsLeft == 0) {
            failed = true;
        }
        for(WeakReference wRef: toBeNotCollected) {
            if(!checkNotCollectable(wRef)) {
                failed = true;
            };
        }

        if(failed) {
            LinkedList<AssertCollectable> toBeCollectedMarked = new LinkedList<AssertCollectable>();
            LinkedList<AssertNotCollectable> toBeNotCollectedMarked = new LinkedList<AssertNotCollectable>();

            for(WeakReference wRef: toBeCollected) {
                toBeCollectedMarked.add(new AssertCollectable(wRef));
            }
            for(WeakReference wRef: toBeNotCollected) {
                toBeNotCollectedMarked.add(new AssertNotCollectable(wRef));
            }
            createHeapDump();
            if(toBeNotCollectedMarked.isEmpty()) {
                throw new AssertionError("The following references should be collected: " + toBeCollectedMarked);
            } else {
                throw new AssertionError("The following references should be collected: " + toBeCollectedMarked + " and " + toBeNotCollectedMarked.size() + " should not be collected: ");
            }
        }


    }


    public static void createHeapDump() {
        try {
            String dateString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String fileName = "heapdump_jmemb_" + dateString + ".hprof";
            File outputFolder = new File(outputFolderString);
            String heapdumpFile = new java.io.File(outputFolder,fileName).getAbsolutePath();
            System.out.println("Creating Heapdump at: " + heapdumpFile);
            getHotspotMBean().dumpHeap(heapdumpFile, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void setMxBeanProxyName(String mxBeanName) {
        MX_BEAN_PROXY_TYPE = mxBeanName;
    }

    private static HotSpotDiagnosticMXBean getHotspotMBean() throws IOException {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        HotSpotDiagnosticMXBean bean =
                ManagementFactory.newPlatformMXBeanProxy(server,
                        MX_BEAN_PROXY_TYPE, HotSpotDiagnosticMXBean.class);
        return bean;
    }

    public static interface MemoryTestAPI {
        public void assertCollectable(Object ref);
        public void assertNotCollectable(Object ref);
        public void setAsReferenced(Object ref);
    }

    static class AssertCollectable {
        WeakReference<Object> assertCollectable;

        AssertCollectable(WeakReference<Object> ref) {
            this.assertCollectable = ref;
        }

        WeakReference<Object> getWeakReference() {
            return assertCollectable;
        }

        @Override
        public String toString() {
            Object el = assertCollectable.get();
            return el != null ? el.toString() : "null";
        }
    }

    static class AssertNotCollectable {
        WeakReference<Object> assertNotCollectable;

        AssertNotCollectable(WeakReference<Object> ref) {
            this.assertNotCollectable = ref;
        }

        WeakReference<Object> getWeakReference() {
            return assertNotCollectable;
        }
    }

    static class SetAsReferenced {
        Object setAsReferenced;

        SetAsReferenced(Object ref) {
            this.setAsReferenced = ref;
        }
    }

}
