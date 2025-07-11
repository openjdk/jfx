package com.sun.glass.ui.headless;

import com.sun.glass.ui.Application;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class NestedRunnableProcessor implements Runnable {

    private final LinkedList<RunLoopEntry> activeRunLoops = new LinkedList<>();

    private final BlockingQueue<Runnable> runnableQueue = new LinkedBlockingQueue<>();

    @Override
    public void run() {
        newRunLoop();
    }

    void invokeLater(final Runnable r) {
        runnableQueue.add(r);
    }

    public Object newRunLoop() {
        RunLoopEntry entry = new RunLoopEntry();

        activeRunLoops.push(entry);

        entry.active = true;
        while (entry.active) {
            try {
                runnableQueue.take().run();
            } catch (Throwable e) {
                Application.reportException(e);
            }
        }
        return entry.returnValue;
    }

    public void leaveCurrentLoop(Object returnValue) {
        RunLoopEntry entry = activeRunLoops.pop();
        entry.active = false;
        entry.returnValue = returnValue;
    }

    private static class RunLoopEntry {

        boolean active;
        Object returnValue;
    }

}
