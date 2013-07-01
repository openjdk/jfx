/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.webkit;

/**
 * The class reflects the native webkit module.
 */
final class MainThread {

    private static void fwkScheduleDispatchFunctions() {
        Invoker.getInvoker().postOnEventThread(new Runnable() {
            @Override
            public void run() {
                twkScheduleDispatchFunctions();
            }
        });
    }

    private static native void twkScheduleDispatchFunctions();
}
