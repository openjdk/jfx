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

package com.oracle.demo.richtext.notebook.data;

import com.oracle.demo.richtext.notebook.CellType;
import com.oracle.demo.richtext.notebook.CodeCellTextModel;
import com.oracle.demo.richtext.notebook.TextCellTextModel;
import jfx.incubator.scene.control.richtext.model.StyledTextModel;

/**
 * This data structure represents a cell in the notebook.
 */
public class CellInfo {
    private CellType type;
    private String source;
    private CodeCellTextModel codeModel;
    private TextCellTextModel textModel;

    public CellInfo(CellType t) {
        this.type = t;
    }

    public final CellType getCellType() {
        return type;
    }

    public final void setCellType(CellType t) {
        type = t;
    }

    public boolean isCode() {
        return getCellType() == CellType.CODE;
    }

    public boolean isText() {
        return getCellType() == CellType.TEXT;
    }

    public final StyledTextModel getModel() {
        switch (type) {
        case CODE:
            if (textModel != null) {
                if (textModel.isModified()) {
                    source = textModel.getPlainText();
                    codeModel = null;
                }
            }
            if (codeModel == null) {
                codeModel = new CodeCellTextModel();
                codeModel.setText(source);
            }
            return codeModel;
        case TEXT:
        default:
            if (codeModel != null) {
                if (codeModel.isModified()) {
                    source = codeModel.getText();
                    textModel = null;
                }
            }
            if (textModel == null) {
                textModel = new TextCellTextModel();
                textModel.setText(source);
            }
            return textModel;
        }
    }

    private void handleTypeChange(CellType old, CellType type) {
        switch (type) {
        case CODE:
            // TODO
        case TEXT:
        default:
            break;
        }
    }

    public String getSource() {
        switch (type) {
        case CODE:
            if (codeModel != null) {
                if (codeModel.isModified()) {
                    source = codeModel.getText();
                    codeModel.setModified(false);
                }
            }
            break;
        case TEXT:
        default:
            if (textModel != null) {
                if (textModel.isModified()) {
                    source = textModel.getPlainText();
                    textModel.setModified(false);
                }
            }
            break;
        }
        return source;
    }

    public void setSource(String text) {
        this.source = text;
    }
}
