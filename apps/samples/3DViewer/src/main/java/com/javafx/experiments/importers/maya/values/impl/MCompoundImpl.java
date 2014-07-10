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
import com.javafx.experiments.importers.maya.types.MCompoundType;
import com.javafx.experiments.importers.maya.types.MDataType;
import com.javafx.experiments.importers.maya.values.MCompound;
import com.javafx.experiments.importers.maya.values.MData;

public class MCompoundImpl extends MDataImpl implements MCompound {
    private MData[] fieldData;

    public MCompoundImpl(MCompoundType type) {
        super(type);
        fieldData = new MData[type.getNumFields()];
        for (int i = 0; i < fieldData.length; i++) {
            MDataType dt = getCompoundType().getField(i).getType();
            if (dt != null) {
                fieldData[i] = dt.createData();
            } else {
                //                System.out.println("field data type is null: " + getCompoundType().getField(i).getName());
            }
        }
    }

    public MCompoundType getCompoundType() {
        return (MCompoundType) getType();
    }

    public MData getFieldData(String fieldName) {
        return getFieldData(getCompoundType().getFieldIndex(fieldName));
    }

    public MData getFieldData(int fieldIndex) {
        if (fieldIndex < 0) {
            return null;
        }
        return fieldData[fieldIndex];
    }

    public void set(int fieldIndex, MData value) {
        fieldData[fieldIndex] = value;
    }

    public void set(String fieldName, MData data) {
        set(getCompoundType().getFieldIndex(fieldName), data);
    }

    public void parse(Iterator<String> data) {
        for (int i = 0; i < getCompoundType().getNumFields(); i++) {
            MData fdata = getFieldData(i);
            if (fdata != null) {
                fdata.parse(data);
            }
        }
    }

    public String toString() {
        String result = "";
        for (int i = 0; i < fieldData.length; i++) {
            result += getCompoundType().getField(i).getName() + ":\t" + fieldData[i] + "\n";
        }
        return result;
    }
}
