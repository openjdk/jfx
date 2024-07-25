package com.sun.jfx.incubator.scene.control.richtext.util;

import java.text.DecimalFormat;

/** debugging aid */
public class D {
    private static final DecimalFormat DOUBLE_FORMAT = new DecimalFormat("0.###");

    public static void p(Object... a) {
        StringBuilder sb = new StringBuilder();
        for (Object x : a) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(x);
        }
        withCaller(2, sb.toString());
    }

    private static void withCaller(int level, String msg) {
        StackTraceElement t = new Throwable().getStackTrace()[level];
        String className = t.getClassName();
        int ix = className.lastIndexOf('.');
        if (ix >= 0) {
            className = className.substring(ix + 1);
        }
        System.err.println(className + "." + t.getMethodName() + ":" + t.getLineNumber() + " " + msg);
    }

    public static void trace() {
        new Error("Stack Trace:").printStackTrace();
    }

    public static String f(double v) {
        return DOUBLE_FORMAT.format(v);
    }

    public static SW sw() {
        return new SW();
    }

    /** stop watch */
    public static class SW {
        private final long start = System.nanoTime();

        public SW() {
        }

        @Override
        public String toString() {
            double ms = (System.nanoTime() - start) / 1_000_000_000.0;
            return f(ms);
        }
    }
}
