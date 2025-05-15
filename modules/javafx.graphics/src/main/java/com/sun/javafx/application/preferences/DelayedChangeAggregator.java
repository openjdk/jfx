/*
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.application.preferences;

import com.sun.javafx.tk.Toolkit;
import javafx.animation.AnimationTimer;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Aggregates multiple subsequent sets of changes into a single changeset, and notifies a consumer.
 * Due to its delayed nature, the consumer may not be notified immediately when a changeset arrives.
 */
public class DelayedChangeAggregator extends AnimationTimer {

    private final Consumer<Map<String, Object>> changeConsumer;
    private final Map<String, Object> currentChangeSet;
    private long elapsedTimeNanos;
    private boolean running;

    public DelayedChangeAggregator(Consumer<Map<String, Object>> changeConsumer) {
        this.changeConsumer = changeConsumer;
        this.currentChangeSet = new HashMap<>();
    }

    /**
     * Integrates the specified changeset into the current changeset, and applies the current changeset
     * after the specified delay period. The delay is added to the current time, but will not elapse
     * before any previous delays are scheduled to elapse.
     *
     * @param changeset the changeset
     * @param delayMillis the delay period, in milliseconds
     */
    public void update(Map<String, Object> changeset, int delayMillis) {
        if (delayMillis > 0 || !currentChangeSet.isEmpty()) {
            long newElapsedTimeNanos = now() + (long)delayMillis * 1000000;
            elapsedTimeNanos = Math.max(elapsedTimeNanos, newElapsedTimeNanos);
            currentChangeSet.putAll(changeset);

            if (!running) {
                running = true;
                start();
            }
        } else {
            changeConsumer.accept(changeset);
        }
    }

    @Override
    public void handle(long now) {
        if (now >= elapsedTimeNanos) {
            stop();
            running = false;
            changeConsumer.accept(currentChangeSet);
            currentChangeSet.clear();
        }
    }

    protected long now() {
        return Toolkit.getToolkit().getPrimaryTimer().nanos();
    }
}
