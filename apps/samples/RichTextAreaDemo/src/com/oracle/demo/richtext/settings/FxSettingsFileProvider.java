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

// This code borrows heavily from the following project, with permission from the author:
// https://github.com/andy-goryachev/FxDock
package com.oracle.demo.richtext.settings;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * Settings provider stores settings as a single file in the specified directory.
 *
 * @author Andy Goryachev
 */
public class FxSettingsFileProvider implements ISettingsProvider {
    private static final char SEP = '=';
    private static final String DIV = ",";
    private final File file;
    private final HashMap<String, Object> data = new HashMap<>();

    public FxSettingsFileProvider(File dir) {
        file = new File(dir, "ui-settings.properties");
    }

    @Override
    public void load() throws IOException {
        if (file.exists() && file.isFile()) {
            Charset cs = Charset.forName("utf-8");
            try (BufferedReader rd = new BufferedReader(new InputStreamReader(new FileInputStream(file), cs))) {
                synchronized (data) {
                    read(rd);
                }
            }
        }
    }

    @Override
    public void save() throws IOException {
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        Charset cs = Charset.forName("utf-8");
        try (Writer wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), cs))) {
            synchronized (data) {
                write(wr);
            }
        }
    }

    private void read(BufferedReader rd) throws IOException {
        String s;
        while ((s = rd.readLine()) != null) {
            int ix = s.indexOf(SEP);
            if (ix <= 0) {
                continue;
            }
            String k = s.substring(0, ix);
            String v = s.substring(ix + 1);
            data.put(k, v);
        }
    }

    private void write(Writer wr) throws IOException {
        ArrayList<String> keys = new ArrayList<>(data.keySet());
        Collections.sort(keys);

        for (String k: keys) {
            Object v = data.get(k);
            wr.write(k);
            wr.write(SEP);
            wr.write(encode(v));
            wr.write("\r\n");
        }
    }

    @Override
    public void set(String key, String value) {
        if (FxSettings.LOG) {
            System.out.println("FxSettingsFileProvider.set key=" + key + " value=" + value);
        }
        synchronized (data) {
            if (value == null) {
                data.remove(key);
            } else {
                data.put(key, value);
            }
        }
    }

    @Override
    public void set(String key, SStream stream) {
        if (FxSettings.LOG) {
            System.out.println("FxSettingsFileProvider.set key=" + key + " stream=" + stream);
        }
        synchronized (data) {
            if (stream == null) {
                data.remove(key);
            } else {
                data.put(key, stream.toArray());
            }
        }
    }

    @Override
    public String get(String key) {
        Object v;
        synchronized (data) {
            v = data.get(key);
        }

        String s;
        if (v instanceof String) {
            s = (String)v;
        } else {
            s = null;
        }

        if (FxSettings.LOG) {
            System.out.println("FxSettingsFileProvider.get key=" + key + " value=" + s);
        }
        return s;
    }

    @Override
    public SStream getSStream(String key) {
        SStream s;
        synchronized (data) {
            Object v = data.get(key);
            if (v instanceof Object[]) {
                s = SStream.reader((Object[])v);
            } else if (v != null) {
                s = parseStream(v.toString());
                data.put(key, s.toArray());
            } else {
                s = null;
            }
        }

        if (FxSettings.LOG) {
            System.out.println("FxSettingsFileProvider.get key=" + key + " stream=" + s);
        }
        return s;
    }

    private static SStream parseStream(String text) {
        String[] ss = text.split(DIV);
        return SStream.reader(ss);
    }

    private static String encode(Object x) {
        if (x == null) {
            return "";
        } else if (x instanceof Object[] items) {
            StringBuilder sb = new StringBuilder();
            boolean sep = false;
            for (Object item: items) {
                if (sep) {
                    sb.append(DIV);
                } else {
                    sep = true;
                }
                sb.append(item);
            }
            return sb.toString();
        } else {
            return x.toString();
        }
    }
}
