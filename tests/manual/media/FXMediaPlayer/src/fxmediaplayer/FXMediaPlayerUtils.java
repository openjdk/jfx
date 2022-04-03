/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package fxmediaplayer;

import java.io.File;
import java.util.List;
import javafx.scene.input.Dragboard;

public class FXMediaPlayerUtils {

    public static String secondsToString(long seconds) {
        long elapsedHours = seconds / (60 * 60);
        long elapsedMinutes = (seconds - elapsedHours * 60 * 60) / 60;
        long elapsedSeconds = seconds - elapsedHours * 60 * 60 - elapsedMinutes * 60;

        if (elapsedHours > 0) {
            return String.format("%d:%02d:%02d",
                    elapsedHours, elapsedMinutes, elapsedSeconds);
        } else {
            return String.format("%02d:%02d", elapsedMinutes, elapsedSeconds);
        }
    }

    public static String secondsToString(double seconds) {
        if (seconds == Double.POSITIVE_INFINITY) {
            return "Inf";
        } else {
            return FXMediaPlayerUtils.secondsToString((long) seconds);
        }
    }

    public static String millisToString(long millis) {
        long seconds = millis / 1000;
        long elapsedHours = seconds / (60 * 60);
        long elapsedMinutes = (seconds - elapsedHours * 60 * 60) / 60;
        long elapsedSeconds = seconds - elapsedHours * 60 * 60 - elapsedMinutes * 60;
        long elapsedMillis = millis - (seconds * 1000);

        if (elapsedHours > 0) {
            return String.format("%d:%02d:%02d:%03d",
                    elapsedHours, elapsedMinutes, elapsedSeconds, elapsedMillis);
        } else {
            return String.format("%02d:%02d:%03d",
                    elapsedMinutes, elapsedSeconds, elapsedMillis);
        }
    }

    public static String millisToString(double millis) {
        if (millis == Double.POSITIVE_INFINITY) {
            return "Inf";
        } else {
            return FXMediaPlayerUtils.millisToString((long) millis);
        }
    }

    public static String getSourceFromDragboard(Dragboard db) {
        if (db.hasString()) {
            String source = db.getString();
            if (source.startsWith("http://") || source.startsWith("https://")) {
                return source;
            }
        } else if (db.hasFiles()) {
            List<File> files = db.getFiles();
            if (!files.isEmpty()) {
                String source = files.get(0).getPath();
                source = source.replace("\\", "/");
                return "file:///" + source;
            }
        }

        return null;
    }
}
