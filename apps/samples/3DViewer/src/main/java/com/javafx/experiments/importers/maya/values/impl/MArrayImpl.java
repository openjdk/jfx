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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.javafx.experiments.importers.maya.MayaImporter;
import com.javafx.experiments.importers.maya.types.MArrayType;
import com.javafx.experiments.importers.maya.values.MArray;
import com.javafx.experiments.importers.maya.values.MData;

public class MArrayImpl extends MDataImpl implements MArray {
    public static final boolean DEBUG = MayaImporter.DEBUG;
    public static final boolean WARN = MayaImporter.WARN;

    List<MData> data = new ArrayList();

    static class Parser {
        private MArray array;

        Parser(MArray array) {
            this.array = array;
        }

        public void parse(Iterator<String> values) {
            int i = 0;
            while (values.hasNext()) {
                array.setSize(i + 1);
                //            System.out.println("get " + i +" of " + array.getSize());
                array.getData(i).parse(values);
                i++;
            }
        }
    }

    class MArraySlice extends MDataImpl implements MArray {
        private MArray array;
        private int base;
        private int length;

        MArraySlice(
                MArray array,
                int base,
                int length) {
            super((MArrayType) array.getType());
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

        public void set(int index, MData data) {
            if (index >= length) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            array.set(base + index, data);
        }

        public MData getData(int index) {
            return array.getData(base + index);
        }

        public MData getData(int start, int end) {
            return new MArraySlice(this, start, end - start);
        }

        public List<MData> get() {
            // FIXME
            throw new RuntimeException("Probably shouldn't fetch the data behind a slice");
        }

        public void parse(Iterator<String> values) {
            new Parser(this).parse(values);
        }
    }

    public MArrayImpl(MArrayType type) {
        super(type);
    }

    public MArrayType getArrayType() {
        return (MArrayType) getType();
    }

    public List<MData> get() {
        return data;
    }

    public MData getData(int index) {
        if (index >= data.size()) {  // TODO huge hack, to prevent out of bounds exception
            int oldIndex = index;
            index = data.size() - 1;
            if (WARN) System.err.println("Changed index from [" + oldIndex + "] to [" + index + "]");
        }
        return data.get(index);
    }

    public MData getData(int start, int end) {
        return new MArraySlice(this, start, end - start);
    }

    public void set(int index, MData data) {
        this.data.set(index, data);
    }

    public void setSize(int size) {
        while (data.size() < size) {
            data.add(getArrayType().getElementType().createData());
        }
        //        System.out.println("SET SIZE: " + size + " data.size="+data.size());
    }

    public int getSize() {
        return data.size();
    }

    public void parse(Iterator<String> values) {
        new Parser(this).parse(values);
    }

    public String toString() {
        return data.toString();
    }
}
