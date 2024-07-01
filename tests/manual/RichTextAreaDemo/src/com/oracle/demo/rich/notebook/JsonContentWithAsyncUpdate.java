/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.demo.rich.notebook;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Random;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import jfx.incubator.scene.control.rich.TextPos;
import jfx.incubator.scene.control.rich.model.PlainTextModel;
import jfx.incubator.scene.control.rich.model.StyleAttrs;

/**
 * Mock content which simulates non-instantaneous retrieval of the underlying data,
 * as in database call or remote file system.
 */
public class JsonContentWithAsyncUpdate implements PlainTextModel.Content {
    private final int size;
    private final HashMap<Integer,String> data;
    private final Random random = new Random();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSS");
    private final HexFormat hex = HexFormat.of();
    private Consumer<Integer> updater;

    public JsonContentWithAsyncUpdate(int size) {
        this.size = size;
        this.data = new HashMap<>(size);
    }

    @Override
    public boolean isUserEditable() {
        return true;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public String getText(int index) {
        String s = data.get(index);
        if (s == null) {
            queue(index);
            return "";
        }
        return s;
    }

    private void queue(int index) {
        Duration simulatedDelay = Duration.millis(200 + random.nextInt(3_000));
        Timeline t = new Timeline();
        t.setCycleCount(1);
        t.getKeyFrames().add(
            new KeyFrame(simulatedDelay, (ev) -> {
                String s = generate(index);
                if(!data.containsKey(index)) {
                    data.put(index, s);
                    update(index);
                }
            })
        );
        t.play();
    }

    private void update(int index) {
        if (updater != null) {
            updater.accept(index);
        }
    }

    public void setUpdater(Consumer<Integer> u) {
        updater = u;
    }

    @Override
    public int insertTextSegment(int index, int offset, String text, StyleAttrs attrs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void insertLineBreak(int index, int offset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeRange(TextPos start, TextPos end) {
        throw new UnsupportedOperationException();
    }

    private String bytes(int count) {
        byte[] b = new byte[count];
        random.nextBytes(b);
        return hex.formatHex(b);
    }

    private String generate(int index) {
        Random r = new Random();
        long time = System.currentTimeMillis() - ((size - 1 - index) * 145_678L);
        String date = dateFormat.format(time);
        String id = bytes(8);
        String message = bytes(1 + random.nextInt(10));
        String payload = bytes(10 + random.nextInt(128));
        int size = payload.length() / 2;

        return
            "{date=\"" + date + "\"" +
            ", timestamp=" + time +
            ", id=\"" + id + "\"" +
            ", message-id=\"" + message + "\"" +
            ", payload=\"" + payload + "\"" +
            ", size=" + size +
            "}";
    }
}
