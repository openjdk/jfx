/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package attenuation;

import javafx.animation.AnimationTimer;

final class FPSCounter extends AnimationTimer {

    private int skipFrames = 100;
    private long lastTime = -1;
    private long elapsedTime;
    private int elapsedFrames;
    private long totalElapsedTime;
    private int totalElapsedFrames;

    @Override
    public void handle(long now) {
        if (skipFrames > 0) {
            --skipFrames;
            return;
        }

        if (lastTime < 0) {
            lastTime = System.nanoTime();
            elapsedTime = 0;
            elapsedFrames = 0;
            totalElapsedTime = 0;
            totalElapsedFrames = 0;
            return;
        }

        long currTime = System.nanoTime();
        elapsedTime += currTime - lastTime;
        elapsedFrames += 1;
        totalElapsedTime += currTime - lastTime;
        totalElapsedFrames += 1;

        double elapsedSeconds = (double) elapsedTime / 1e9;
        double totalElapsedSeconds = (double) totalElapsedTime / 1e9;
        if (elapsedSeconds >= 5.0) {
            double fps = elapsedFrames / elapsedSeconds;
            System.out.println();
            System.out.println("instant fps: " + fps);
            double avgFps = totalElapsedFrames / totalElapsedSeconds;
            System.out.println("average fps: " + avgFps);
            System.out.flush();
            elapsedTime = 0;
            elapsedFrames = 0;
        }

        lastTime = currTime;
    }

    void reset() {
        skipFrames = 100;
        lastTime = -1;
        elapsedTime = 0;
        elapsedFrames = 0;
        totalElapsedTime = 0;
        totalElapsedFrames = 0;
        System.out.println();
        System.out.println(" --------------------- ");
    }
}
