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

package jfx.incubator.scene.control.richtext.skin;

import java.util.Locale;
import javafx.scene.text.Font;
import com.sun.jfx.incubator.scene.control.richtext.RichTextAreaSkinHelper;
import com.sun.jfx.incubator.scene.control.richtext.util.ListenerHelper;
import com.sun.jfx.incubator.scene.control.richtext.util.RichUtils;
import jfx.incubator.scene.control.richtext.CodeArea;
import jfx.incubator.scene.control.richtext.model.StyleAttributeMap;

/**
 * The skin for {@link CodeArea}.
 *
 * @since 24
 */
public class CodeAreaSkin extends RichTextAreaSkin {
    /**
     * Constructs the CodeArea skin.
     * @param control the CodeArea instance
     */
    public CodeAreaSkin(CodeArea control) {
        super(control);

        ListenerHelper lh = RichTextAreaSkinHelper.getListenerHelper(this);
        lh.addInvalidationListener(
            this::refreshLayout,
            control.fontProperty(),
            control.lineSpacingProperty(),
            control.tabSizeProperty()
        );
    }

    @Override
    public void applyStyles(CellContext cx, StyleAttributeMap attrs, boolean forParagraph) {
        super.applyStyles(cx, attrs, forParagraph);

        if (forParagraph) {
            CodeArea control = (CodeArea)getSkinnable();
            // font
            Font f = control.getFont();
            if (f != null) {
                double size = f.getSize();
                String family = f.getFamily();
                String name = f.getName();
                if (RichUtils.isLogicalFont(family)) {
                    String lowerCaseName = name.toLowerCase(Locale.ENGLISH);
                    String style = RichUtils.guessFontStyle(lowerCaseName);
                    String weight = RichUtils.guessFontWeight(lowerCaseName);
                    cx.addStyle("-fx-font-family:'" + family + "';");
                    cx.addStyle("-fx-font-style:" + style + ";");
                    cx.addStyle("-fx-font-weight:" + weight + ";");
                } else {
                    cx.addStyle("-fx-font-family:'" + name + "';");
                }
                cx.addStyle("-fx-font-size:" + size + ";");
            }

            // line spacing
            double lineSpacing = control.getLineSpacing();
            cx.addStyle("-fx-line-spacing:" + lineSpacing + ";");

            // tab size
            double tabSize = control.getTabSize();
            cx.addStyle("-fx-tab-size:" + tabSize + ";");
        }
    }
}
