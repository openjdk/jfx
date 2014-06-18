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

package com.javafx.experiments.importers.maya.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.javafx.experiments.importers.maya.MEnv;
import com.javafx.experiments.importers.maya.values.MData;
import com.javafx.experiments.importers.maya.values.impl.MCompoundImpl;

public class MCompoundType extends MDataType {

    Map<String, Field> fields = new HashMap();
    List<Field> fieldArray = new ArrayList();

    public MCompoundType(MEnv env, String name) {
        super(env, name);
    }

    public Map<String, Field> getFields() {
        return fields;
    }

    public int getNumFields() {
        return fieldArray.size();
    }

    public int getFieldIndex(String name) {
        Field field = getField(name);
        if (field == null) {
            //            System.out.println("No such field in type " + getName() + ": " + name);
            return -1;
        }
        return getField(name).getIndex();
    }

    public Field getField(String name) {
        return fields.get(name);
    }

    public Field getField(int index) {
        return fieldArray.get(index);
    }

    public Field addField(String name, MDataType type, MData defaultValue) {
        Field field;
        fields.put(name, field = new Field(name, type, defaultValue, fieldArray.size()));
        fieldArray.add(field);
        return field;
    }

    public static class Field {
        String name;
        MDataType type;
        MData defaultValue;
        int index;

        public Field(String name, MDataType type, MData defaultValue, int index) {
            this.name = name;
            this.type = type;
            this.defaultValue = defaultValue;
            this.index = index;
        }

        public String getName() {
            return name;
        }

        public MDataType getType() {
            return type;
        }

        public MData getDefault() {
            //return defaultValue;
            return type.createData();
        }

        public int getIndex() {
            return index;
        }
    }

    public MData createData() {
        return new MCompoundImpl(this);
    }
}
