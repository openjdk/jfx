/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.io.File;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class VMPerformance {

    private static long TEST_TIME = Long.getLong("testTime", 1000l);
    private static long TIMEOUT = Long.getLong("timeout", TEST_TIME * 30);
    private static boolean VERBOSE = Boolean.getBoolean("verbose");

    private static void showProperty(String key) {
        System.out.println(key + ": " + System.getProperty(key));
    }

    public static void main(String[] args) throws Exception {
        showProperty("java.version");
        showProperty("os.arch");
        try {
            String userDir = System.getProperty("user.dir") + File.separator;
            String libPrefix = "lib";
            String libSuffix = ".so";
            String libFileName = libPrefix
                    + "VMPerformance-" + System.getProperty("os.arch")
                    + libSuffix;
            try {
                System.load(userDir + "build" + File.separator + libFileName);
            } catch (UnsatisfiedLinkError e1) {
                try {
                    System.load(userDir + "dist" + File.separator + libFileName);
                } catch (UnsatisfiedLinkError e2) {
                    System.loadLibrary("VMPerformance");
                }
            }
            jniInitIDs();
        } catch (UnsatisfiedLinkError e3) {
            e3.printStackTrace();
            System.err.println("VMPerformance native library not available");
        }
        Thread.sleep(500);
        for (Test t : tests) {
            t.run();
        }
        System.exit(0);
    }

    static void reportTime(
            String description,
            long startTime,
            long endTime,
            long delta,
            int iterations) {
        reportTime(description, endTime - startTime - delta, iterations);
    }

    static void reportTime(
            String description,
            long time,
            int iterations) {
        long ns = time / iterations;
        String t = String.valueOf(ns);
        StringBuffer sb = new StringBuffer(description);
        while (sb.length() < 70 - t.length()) {
            sb.append(".");
        }
        sb.append(t);
        if (t.length() == 1) {
            sb.append(".");
            long ps = ((time * 1000l) / iterations) - (ns * 1000l);
            String t2 = String.valueOf(ps);
            if (t.equals("0")) {
                switch (t2.length()) {
                    case 3:
                        sb.append(t2.substring(0, 2));
                        break;
                    case 2:
                        sb.append("0");
                        sb.append(t2.substring(0, 2));
                        break;
                    default:
                        sb.append("00");
                        sb.append(t2);
                }
            } else {
                sb.append(t2.substring(0, 1));
            }
        }
        sb.append("ns");
        if (VERBOSE) {
            sb.append(" (n=" + iterations + ", t=" + time + "ns)");
        }
        System.out.println(sb);
    }

    static Test[] tests = {
        new StaticMethodCall(),
        new InstanceMethodCall(),
        new FinalInstanceMethodCall(),
        new InterfaceWithSingleImplementorMethodCall(),
        new InterfaceWithMultipleImplementorsMethodCall(),
        new JNICallNoParameters(),
        new JNICallFourParameters(),
        new JNIUpCallNoParameters(),
        new IntrinsicMethodCall(),
        new ClassForName(),
        new ReflectedMethodCallLookup(),
        new ReflectedInstanceMethodCall(),
        new GetClass(),
        new InstanceOfResultTrue(),
        new InstanceOfResultFalse(),
        new GrabReleaseLock(),
        new GrabNotifyReleaseLock(),
        new CreateObjectWithNoFinalizer(),
        new CreateObjectWithNonEmptyFinalizer(),
        new CreateAndCollectObjectWithNoFinalizer(),
        new CreateDisposeAndCollectObjectWithNoFinalizer(),
        new CreateAndCollectObjectWithEmptyFinalizer(),
        new CreateAndCollectWithNonEmptyFinalizer(),
        new CreateWeakReference(),
        new CreateSoftReference(),
        new CreateAndCollectWeakReference(),
        new CreateAndCollectWeakReferenceWithQueue(),
        new CreateCatchAndCollectNullPointerException(),
        new TrapCatchAndCollectNullPointerException(),
        new ArrayLookupInteger(),
        new ArrayLookupObject(),
        new ArrayAssignInteger(),
        new ArrayAssignByte(),
        new VectorLookup(),
        new ArrayListLookup(),
        new ThreadLocalLookup(),
        new DirectByteBufferPut(),
        new DirectFloatBufferPut(),
        new StaticFieldRead(),
        new LocalVariableRead(),
    };

    static abstract class Test {
        static List<GarbageCollectorMXBean> gcBeans =
                ManagementFactory.getGarbageCollectorMXBeans();
        void control(int iterations) {
            for (int i = 0; i < iterations; i++) {
            }
        }
        abstract void test(int iterations);
        abstract String getName();
        void run() {
            for (int i = 0; i < 100; i++) {
                try {
                    runTest(1);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                } catch (UnsatisfiedLinkError ule) {
                    return;
                }
            }
            int iterations = 1;
            long testTime = 0;
            long startTime = System.nanoTime();
            long elapsedTime = 0l;
            while ((testTime < TEST_TIME * 1000000l || iterations < 10000)
                    && iterations <= 0x20000000
                    && elapsedTime < TIMEOUT * 1000000l) {
                iterations *= 2;
                System.gc();
                System.runFinalization();
                long gcBaseTime = 0l;
                if (!includeGCTime()) {
                    gcBaseTime = gcTime();
                }
                testTime = runTest(iterations);
                if (!includeGCTime()) {
                    long gcTime = gcTime() - gcBaseTime;
                    testTime -= gcTime;
                }
                elapsedTime = System.nanoTime() - startTime;
            }
            reportTime(getName(), testTime, iterations);
        }
        long runTest(int iterations) {
            long startTime = System.nanoTime();
            test(iterations);
            long endTime = System.nanoTime();
            control(iterations);
            long controlTime = System.nanoTime() - endTime;
            long testTime = endTime - startTime;
            return testTime - controlTime;
        }
        boolean includeGCTime() {
            return true;
        }
        static long gcTime() {
            long gcTime = 0l;
            for (GarbageCollectorMXBean g : gcBeans) {
                gcTime += g.getCollectionTime() * 1000000l;
            }
            return gcTime;
        }

    }

    static abstract class ObjectCreationTest extends Test {
        @Override void test(int n) {
            Object[] refs = new Object[1024];
            for (int i = 0; i < n >> 10; i++) {
                fillArray(refs);
            }
            refs = null;
            System.gc();
            System.runFinalization();
        }
        @Override void control(int n) {
            Object[] refs = new Object[1024];
            for (int i = 0; i < n >> 10; i++) {
                for (int j = 0; j < refs.length; j++) {
                    refs[j] = null;
                }
            }
            refs = null;
        }
        abstract void fillArray(Object[] refs);
    }

    static class StaticMethodCall extends Test {
        static int counter;

        @Override void test(int n) {
            for (int i = 0; i < n; i++) {
                StaticMethodCall.f();
            }
        }
        @Override String getName() {
            return "Static method call";

        }
        static void f() {
            counter ++;
        }
    }

    static class InstanceMethodCall extends Test {
        int counter;
        @Override void test(int n) {
            for (int i = 0; i < n; i++) {
                this.f();
            }
        }
        @Override String getName() {
            return "Instance method call";

        }
        void f() {
            counter ++;
        }
    }

    static class FinalInstanceMethodCall extends Test {
        int counter;
        @Override void test(int n) {
            for (int i = 0; i < n; i++) {
                this.f();
            }
        }
        @Override String getName() {
            return "Final instance method call";

        }
        final void f() {
            counter ++;
        }
    }

    static class InterfaceWithSingleImplementorMethodCall extends Test {
        InterfaceWithSingleImplementor iwsi = new Implementor1();
        @Override void test(int n) {
            for (int i = 0; i < n; i++) {
                iwsi.f();
            }
        }
        @Override String getName() {
            return "Interface method call (single implementor)";
        }
    }

    static class InterfaceWithMultipleImplementorsMethodCall extends Test {
        InterfaceWithMultipleImplementors iwmiA = new Implementor2a();
        InterfaceWithMultipleImplementors iwmiB = new Implementor2b();
        InterfaceWithMultipleImplementors iwmi = new Implementor2a();
        @Override void test(int n) {
            for (int i = 0; i < n; i++) {
                iwmi.f();
            }
        }
        @Override String getName() {
            return "Interface method call (two implementors, one instantiated)";

        }
    }

    static class JNICallNoParameters extends Test {
        @Override void test(int n) {
            for (int i = 0; i < n; i++) {
                fn0();
            }
        }
        @Override String getName() {
            return "JNI method call with no parameters";
        }
    }

    static class JNICallFourParameters extends Test {
        Object o1 = "a";
        Object o2 = "b";
        Object o3 = "c";
        Object o4 = "d";
        @Override void test(int n) {
            for (int i = 0; i < n; i++) {
                fn4(o1, o2, o3, o4);
            }
        }
        @Override String getName() {
            return "JNI method call with 4 parameters";
        }
    }

    static class JNIUpCallNoParameters extends Test {
        @Override void test(int n) {
            jniTestUpCallNoParameters(n);
        }
        @Override String getName() {
            return "JNI up call with no parameters";
        }
    }

    static native void jniInitIDs();
    static native void jniTestUpCallNoParameters(int n);

    static class IntrinsicMethodCall extends Test {
        @Override void test(int n) {
            for (int i = 0; i < n; i++) {
                Float.floatToRawIntBits(0f);
            }
        }
        @Override String getName() {
            return "Intrinsic method call";
        }
    }

    static class ClassForName extends Test {
        @Override void test(int n) {
            try {
                for (int i = 0; i < n; i++) {
                    Class.forName("java.lang.String");
                }
            } catch (Exception e) { }
        }
        @Override String getName() {
            return "Class.forName() for system class";
        }
    }

    static class ReflectedMethodCallLookup extends Test {
        Class stringClass = String.class;
        Class[] signature = { };
        @Override void test(int n) {
            try {
                for (int i = 0; i < n; i++) {
                    stringClass.getMethod("length", signature);
                }
            } catch (Exception e) { }
        }
        @Override String getName() {
            return "Reflected method call lookup";
        }
    }

    static class ReflectedInstanceMethodCall extends Test {
        Class stringClass = String.class;
        Object s = "";
        Class[] signature = { };
        Object[] args = { };
        Method mLength;
        {
            try {
                mLength = stringClass.getMethod("length", signature);
            } catch (Exception e) { }
        }
        @Override void test(int n) {
            try {
                for (int i = 0; i < n; i++) {
                    mLength.invoke(s, args);
                }
            } catch (Exception e) { }
        }
        @Override String getName() {
            return "Reflected instance method call";
        }
    }

    static class GetClass extends Test {
        Object s = "";
        @Override void test(int n) {
            for (int i = 0; i < n; i++) {
                s.getClass();
            }
        }
        @Override String getName() {
            return "getClass()";
        }
    }

    static class InstanceOfResultTrue extends Test {
        Object s = "";
        @Override void test(int n) {
            for (int i = 0; i < n && s instanceof Object; i++) { }
        }
        @Override String getName() {
            return "instanceof (where the result is true)";
        }
    }

    static class InstanceOfResultFalse extends Test {
        Object s = "";
        @Override void test(int n) {
            for (int i = 0; i < n && !(s instanceof ThreadLocal); i++) { }
        }
        @Override String getName() {
            return "instanceof (where the result is false)";
        }
    }

    static class GrabReleaseLock extends Test {
        Object lock = new Object();
        @Override void test(int n) {
            for (int i = 0; i < n; i++) {
                synchronized (lock) { }
            }
        }
        @Override String getName() {
            return "Grab and release uncontested lock";
        }
    }

    static class GrabNotifyReleaseLock extends Test {
        Object lock = new Object();
        @Override void test(int n) {
            for (int i = 0; i < n; i++) {
                synchronized (lock) {
                    lock.notify();
                }
            }
        }
        @Override String getName() {
            return "Grab, notify and release uncontested lock";
        }
    }

    static class CreateObjectWithNoFinalizer extends ObjectCreationTest {
        @Override void fillArray(Object[] refs) {
            for (int i = 0; i < refs.length; i++) {
                refs[i] = new ObjectWithNoFinalizer();
            }
        }
        @Override boolean includeGCTime() {
            return false;
        }
        @Override String getName() {
            return "Create object with no finalizer";
        }
    }

    static class CreateObjectWithNonEmptyFinalizer extends ObjectCreationTest {
        @Override void fillArray(Object[] refs) {
            for (int i = 0; i < refs.length; i++) {
                refs[i] = new ObjectWithNonEmptyFinalizer();
            }
        }
        @Override boolean includeGCTime() {
            return false;
        }
        @Override String getName() {
            return "Create object with non-empty finalizer";
        }
    }

    static class CreateAndCollectObjectWithNoFinalizer extends ObjectCreationTest {
        @Override void fillArray(Object[] refs) {
            for (int i = 0; i < refs.length; i++) {
                refs[i] = new ObjectWithNoFinalizer();
            }
        }
        @Override String getName() {
            return "Create and collect object with no finalizer";
        }
    }

    static class CreateDisposeAndCollectObjectWithNoFinalizer
            extends ObjectCreationTest {
        @Override void fillArray(Object[] refs) {
            for (int i = 0; i < refs.length; i++) {
                ObjectWithNoFinalizer o = new ObjectWithNoFinalizer();
                refs[i] = o;
                o.dispose();
            }
        }
        @Override String getName() {
            return "Create, dispose and collect object with disposer and no finalizer";
        }
    }

    static class CreateAndCollectObjectWithEmptyFinalizer
            extends ObjectCreationTest {
        @Override void fillArray(Object[] refs) {
            for (int i = 0; i < refs.length; i++) {
                refs[i] = new ObjectWithEmptyFinalizer();
            }
        }
        @Override String getName() {
            return "Create and collect object with empty finalizer";
        }
    }

    static class CreateAndCollectWithNonEmptyFinalizer
            extends ObjectCreationTest {
        @Override void fillArray(Object[] refs) {
            for (int i = 0; i < refs.length; i++) {
                refs[i] = new ObjectWithNonEmptyFinalizer();
            }
        }
        @Override String getName() {
            return "Create and collect object with non-empty finalizer";
        }
    }

    static class CreateWeakReference extends ObjectCreationTest {
        @Override void fillArray(Object[] refs) {
            for (int i = 0; i < refs.length; i++) {
                refs[i] = new WeakReference(this);
            }
        }
        @Override boolean includeGCTime() {
            return false;
        }
        @Override String getName() {
            return "Create weak reference";
        }
    }

    static class CreateSoftReference extends ObjectCreationTest {
        @Override void fillArray(Object[] refs) {
            for (int i = 0; i < refs.length; i++) {
                refs[i] = new SoftReference(this);
            }
        }
        @Override boolean includeGCTime() {
            return false;
        }
        @Override String getName() {
            return "Create soft reference";
        }
    }

    static class CreateAndCollectWeakReference
            extends ObjectCreationTest {
        @Override void fillArray(Object[] refs) {
            for (int i = 0; i < refs.length; i++) {
                refs[i] = new WeakReference(this);
            }
        }
        @Override String getName() {
            return "Create and collect weak reference";
        }
    }

    static class CreateAndCollectWeakReferenceWithQueue extends ObjectCreationTest {
        final ReferenceQueue queue = new ReferenceQueue();
        Thread queueThread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        Reference r = queue.remove();
                    }
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        {
            queueThread.start();
        }
        @Override void fillArray(Object[] refs) {
            for (int i = 0; i < refs.length; i++) {
                refs[i] = new WeakReference(this, queue);
            }
        }
        @Override String getName() {
            return "Create and collect weak reference with queue";
        }
    }

    static class CreateAndCollectPhantomReferenceWithQueue
            extends ObjectCreationTest {
        final ReferenceQueue queue = new ReferenceQueue();
        Thread queueThread = new Thread(new Runnable() {
            public void run() {
                try {
                    while (true) {
                        Reference r = queue.remove();
                    }
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        {
            queueThread.start();
        }
        @Override void fillArray(Object[] refs) {
            for (int i = 0; i < refs.length; i++) {
                refs[i] = new PhantomReference(this, queue);
            }
        }
        @Override String getName() {
            return "Create and collect phantom reference with queue";
        }
    }

    static class CreateCatchAndCollectNullPointerException
            extends ObjectCreationTest {
        @Override void fillArray(Object[] refs) {
            for (int i = 0; i < refs.length; i++) {
                try {
                    throw new NullPointerException();
                } catch (Exception e) {
                    refs[i] = e;
                }
            }
        }
        @Override String getName() {
            return "Create, catch and collect a NullPointerException";
        }
    }

    static class TrapCatchAndCollectNullPointerException
            extends ObjectCreationTest {
        @Override void fillArray(Object[] refs) {
            Object o = null;
            for (int i = 0; i < refs.length; i++) {
                try {
                    o.toString();
                } catch (Exception e) {
                    refs[i] = e;
                }
            }
        }
        @Override String getName() {
            return "Trap, catch and collect a NullPointerException";
        }
    }

    static class ArrayLookupInteger extends Test {
        @Override void test(int n) {
            int[] xs = new int[1024];
            int x = 0;
            for (int i = 0; i < n >> 10; i++) {
                for (int j = 0; j < 1024; j++) {
                    x += xs[j];
                }
            }
        }
        @Override String getName() {
            return "Array lookup in an integer array";
        }
    }

    static class ArrayAssignInteger extends Test {
        @Override void test(int n) {
            int[] xs = new int[1024];
            for (int i = 0; i < n >> 10; i++) {
                for (int j = 0; j < 1024; j++) {
                    xs[j] = j;
                }
            }
        }
        @Override String getName() {
            return "Array assignation in an integer array";
        }
    }

    static class ArrayAssignByte extends Test {
        @Override void test(int n) {
            byte[] xs = new byte[1024];
            for (int i = 0; i < n >> 10; i++) {
                for (int j = 0; j < 1024; j++) {
                    xs[j] = (byte) j;
                }
            }
        }
        @Override String getName() {
            return "Array assignation in a byte array";
        }
    }

    static class ArrayLookupObject extends Test {
        @Override void test(int n) {
            Object[] a = new Object[1024];
            int x = 0;
            for (int i = 0; i < n >> 10; i++) {
                for (int j = 0; j < 1024; j++) {
                    if (a[j] != null) {
                        x++;
                    }
                }
            }
        }
        @Override String getName() {
            return "Array lookup in an object array";
        }
    }

    static class VectorLookup extends Test {
        @Override void test(int n) {
            Vector v = new Vector();
            for (int i = 0; i < 1024; i++) {
                v.addElement(v);
            }
            int x = 0;
            for (int i = 0; i < n >> 10; i++) {
                for (int j = 0; j < 1024; j++) {
                    if (v.get(j) == null) {
                        x++;
                    }
                }
            }
        }
        @Override String getName() {
            return "Vector.get()";
        }
    }

    static class ArrayListLookup extends Test {
        @Override void test(int n) {
            ArrayList a = new ArrayList();
            for (int i = 0; i < 1024; i++) {
                a.add(a);
            }
            int x = 0;
            for (int i = 0; i < n >> 10; i++) {
                for (int j = 0; j < 1024; j++) {
                    if (a.get(j) == null) {
                        x++;
                    }
                }
            }
        }
        @Override String getName() {
            return "ArrayList.get()";
        }
    }

    static class ThreadLocalLookup extends Test {
        @Override void test(int n) {
            ThreadLocal threadLocal = new ThreadLocal();
            threadLocal.set(new int[4]);
            int x = 0;
            for (int i = 0; i < n; i++) {
                int[] a = (int[]) threadLocal.get();
                if (a == null) {
                    x++;
                }
            }
        }
        @Override String getName() {
            return "ThreadLocal.get()";
        }
    }

    static class StaticFieldRead extends Test {
        static final int TEST_FIELD = 30;
        @Override void test(int n) {
            for (int i = 0; i < n; i++) {
                int x = TEST_FIELD;
            }
        }
        @Override String getName() {
            return "Read static int field";
        }
    }

    static class LocalVariableRead extends Test {
        static final int TEST_FIELD = 30;
        @Override void test(int n) {
            int x0 = 30;
            for (int i = 0; i < n; i++) {
                int x = x0;
            }
        }
        @Override String getName() {
            return "Read local int variable";
        }
    }

    static class DirectByteBufferPut extends Test {
        @Override void test(int n) {
            ByteBuffer bb = ByteBuffer.allocateDirect(1 << 20);
            int m = n >> 20;
            int size = bb.limit();
            byte b = (byte) 0;
            for (int i = 0; i < m; i++) {
                bb.rewind();
                for (int j = 0; j < size; j++) {
                    bb.put(b);
                }
            }
        }
        @Override void control(int n) {
            ByteBuffer bb = ByteBuffer.allocateDirect(1 << 20);
            int m = n >> 20;
            int size = bb.limit();
            byte b = (byte) 0;
            for (int i = 0; i < m; i++) {
                bb.rewind();
                for (int j = 0; j < size; j++) {
                }
            }
        }
        @Override String getName() {
            return "Direct ByteBuffer.put(byte)";
        }
    }

    static class DirectFloatBufferPut extends Test {
        @Override void test(int n) {
            ByteBuffer bb = ByteBuffer.allocateDirect(4 << 20);
            FloatBuffer fb = bb.asFloatBuffer();
            int m = n >> 20;
            int size = fb.limit();
            float f = 0f;
            for (int i = 0; i < m; i++) {
                fb.rewind();
                for (int j = 0; j < size; j++) {
                    fb.put(f);
                }
            }
        }
        @Override void control(int n) {
            ByteBuffer bb = ByteBuffer.allocateDirect(4 << 20);
            FloatBuffer fb = bb.asFloatBuffer();
            int m = n >> 20;
            int size = bb.limit();
            byte b = (byte) 0;
            for (int i = 0; i < m; i++) {
                fb.rewind();
                for (int j = 0; j < size; j++) {
                }
            }
        }
        @Override String getName() {
            return "Direct FloatBuffer.put(float)";
        }
    }

    static native void fn0();
    static native void fn4(Object o1, Object o2, Object o3, Object o4);

    static void f() { }

    static class ObjectWithNoFinalizer {
        public final void dispose() {
        }
    }

    static class ObjectWithEmptyFinalizer {
        protected final void finalize() { }
    }

    static class ObjectWithNonEmptyFinalizer {
        int i;
        protected final void finalize() {
            i = 1;
        }
    }

    static interface InterfaceWithSingleImplementor {
        public void f();
    }

    static class Implementor1 implements InterfaceWithSingleImplementor {
        int i;
        public void f() {
            i++;
        }
    }

    static interface InterfaceWithMultipleImplementors {
        public void f();
    }

    static class Implementor2a implements InterfaceWithMultipleImplementors {
        int i;
        public void f() {
            i++;
        }
    }

        static class Implementor2b implements InterfaceWithMultipleImplementors {
        int i;
        public void f() {
            i--;
        }
    }

}
