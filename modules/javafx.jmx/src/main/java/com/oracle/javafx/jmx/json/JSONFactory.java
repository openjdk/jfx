/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.javafx.jmx.json;

import com.oracle.javafx.jmx.json.impl.JSONStreamReaderImpl;
import java.io.Reader;
import java.io.Writer;

/**
 * A factory class for creating a {@link JSONReader} or {@link JSONWriter}.
 *
 */
public class JSONFactory {

    private static final JSONFactory INSTANCE = new JSONFactory();

    private JSONFactory() {
    }

    /**
     * Gets an instance of JSONFactory.
     *
     * @return the JSONFactory
     */
    public static JSONFactory instance() {
        return INSTANCE;
    }

    /**
     * Creates a new {@link JSONReader} from a reader.
     *
     * @param reader - the reader to read from
     * @return a {@link JSONReader}
     * @throws JSONException if reader cannot be read
     */
    public JSONReader makeReader(final Reader reader) throws JSONException {
        return new JSONStreamReaderImpl(reader);
    }

    /**
     * Creates a new {@link JSONWriter} from a writer
     *
     * @param writer - the writer to write to
     * @return a {@link JSONWriter}
     */
    public JSONWriter makeWriter(final Writer writer) {
        return new JSONWriter(writer);
    }
}
