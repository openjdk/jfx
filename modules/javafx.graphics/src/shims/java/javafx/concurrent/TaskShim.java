/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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
package javafx.concurrent;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;

public abstract class TaskShim<V> extends Task<V> {

    public static <T extends Event> void setEventHandler(
            Task t,
            final EventType<T> eventType,
            final EventHandler<? super T> eventHandler) {
        t.setEventHandler(eventType, eventHandler);
    }

    //-------------------------------------------------------------

    @Override
    public boolean isFxApplicationThread() {
        return super.isFxApplicationThread();
}

    @Override
    public void runLater(Runnable r) {
        super.runLater(r);
    }

    public void shim_setState(Worker.State value) {
        super.setState(value);
    }


    @Override
    public void updateTitle(String title) {
        super.updateTitle(title);
    }

    @Override
    public void updateMessage(String message) {
        super.updateMessage(message);
    }

    public static void updateProgress(Task t, double workDone, double max) {
        t.updateProgress(workDone, max);
    }

    public static void updateProgress(Task t, long workDone, long max) {
        t.updateProgress(workDone, max);
    }

    @Override
    public void updateProgress(double workDone, double max) {
        super.updateProgress(workDone, max);
    }

}
