/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates.
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

package com.javafx.experiments.importers.maya.values.impl;

import java.util.Iterator;
import com.javafx.experiments.importers.maya.types.MIntArrayType;
import com.javafx.experiments.importers.maya.values.MData;
import com.javafx.experiments.importers.maya.values.MIntArray;

public class MIntArrayImpl extends MDataImpl implements MIntArray {

    private int[] data;

    static class Parser {
        private MIntArray array;

        Parser(MIntArray array) {
            this.array = array;
        }

        public void parse(Iterator<String> elements) {
            int i = 0;
            while (elements.hasNext()) {
                array.set(
                        i++,
                        Integer.parseInt(elements.next()));
            }
        }
    }

    static class MIntArraySlice extends MDataImpl implements MIntArray {
        private MIntArray array;
        private int base;
        private int length;

        MIntArraySlice(
                MIntArray array,
                int base,
                int length) {
            super((MIntArrayType) array.getType());
            this.array = array;
            this.base = base;
            this.length = length;
        }

        public void setSize(int size) {
            array.setSize(base + size);
        }

        public int getSize() {
            return length;
        }

        public void set(int index, int x) {
            if (index >= length) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            array.set(base + index, x);
        }

        public int[] get() {
            // FIXME
            throw new RuntimeException("Probably shouldn't fetch the data behind a slice");
        }

        public int get(int index) {
            return array.get(base + index);
        }

        public void parse(Iterator<String> elements) {
            new Parser(this).parse(elements);
        }
    }

    public MIntArrayImpl(MIntArrayType type) {
        super(type);
    }

    public void setSize(int size) {
        if (data == null || size > data.length) {
            int[] newdata = new int[size];
            if (data != null) {
                System.arraycopy(data, 0, newdata, 0, data.length);
            }
            data = newdata;
        }
    }

    public void set(int index, int x) {
        setSize(index + 1);
        data[index] = x;
    }

    public int[] get() {
        return data;
    }

    public int get(int index) {
        return data[index];
    }

    public MData getData(int index) {
        return getData(index, index + 1);
    }

    public int getSize() {
        return data == null ? 0 : data.length;
    }

    public MData getData(int start, int end) {
        return new MIntArraySlice(this, start, end - start + 1);
    }

    public void parse(Iterator<String> elements) {
        new Parser(this).parse(elements);
    }

    public String toString() {
        String result = getType().getName();
        String sep = " ";
        if (data != null) {
            for (int f : data) {
                result += sep;
                result += f;
            }
        }
        return result;
    }
}
