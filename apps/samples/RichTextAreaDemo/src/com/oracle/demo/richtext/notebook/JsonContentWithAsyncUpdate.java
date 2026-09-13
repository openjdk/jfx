/*
 * Copyright (c) 2023, 2024, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.oracle.demo.richtext.notebook;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Random;
import java.util.function.Consumer;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import jfx.incubator.scene.control.richtext.TextPos;
import jfx.incubator.scene.control.richtext.model.BasicTextModel;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * Mock content which simulates non-instantaneous retrieval of the underlying data,
 * as in database call or remote file system.
 *
 * @author Andy Goryachev
 */
public class JsonContentWithAsyncUpdate implements BasicTextModel.Content {
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
    public boolean isWritable() {
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
    public int insertTextSegment(int index, int offset, String text, StyleAttributeMap attrs) {
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
