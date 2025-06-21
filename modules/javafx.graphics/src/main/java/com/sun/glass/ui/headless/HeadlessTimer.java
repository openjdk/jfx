package com.sun.glass.ui.headless;

import com.sun.glass.ui.Timer;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class HeadlessTimer extends Timer {

    private static ScheduledThreadPoolExecutor scheduler;
    private ScheduledFuture task;

    HeadlessTimer(final Runnable runnable) {
        super(runnable);
    }

    @Override
    protected long _start(Runnable runnable) {
        throw new RuntimeException("vsync timer not supported");
    }

    @Override
    protected long _start(Runnable runnable, int period) {
        if (scheduler == null) {
            scheduler = new ScheduledThreadPoolExecutor(1, target -> {
                Thread thread = new Thread(target, "Headless Timer");
                thread.setDaemon(true);
                return thread;
            });
        }
        task = scheduler.scheduleAtFixedRate(runnable, 0, period, TimeUnit.MILLISECONDS);
        return 1;

    }

    @Override
    protected void _stop(long timer) {
        if (task != null) {
            task.cancel(false);
            task = null;
        }
    }

    @Override
    protected void _pause(long timer) {
    }

    @Override
    protected void _resume(long timer) {
    }

}
