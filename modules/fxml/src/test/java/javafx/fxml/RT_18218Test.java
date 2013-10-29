/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.fxml;

import com.sun.javafx.fxml.LoadListener;
import java.io.IOException;
import java.util.Map;
import org.junit.Test;

import static org.junit.Assert.*;

public class RT_18218Test {
    @Test
    @SuppressWarnings({"unchecked", "deprecation"})
    public void testStaticScriptLoad() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("rt_18218.fxml"));
        fxmlLoader.impl_setStaticLoad(true);
        fxmlLoader.impl_setLoadListener(new LoadListener() {
            private String unknownStaticPropertyElementName = null;

            @Override
            public void readImportProcessingInstruction(String target) {
            }

            @Override
            public void readLanguageProcessingInstruction(String language) {
            }

            @Override
            public void readComment(String comment) {
            }

            @Override
            public void beginInstanceDeclarationElement(Class<?> type) {
            }

            @Override
            public void beginUnknownTypeElement(String name) {
            }

            @Override
            public void beginIncludeElement() {
            }

            @Override
            public void beginReferenceElement() {
            }

            @Override
            public void beginCopyElement() {
            }

            @Override
            public void beginRootElement() {
            }

            @Override
            public void beginPropertyElement(String name, Class<?> sourceType) {
            }

            @Override
            public void beginUnknownStaticPropertyElement(String name) {
                unknownStaticPropertyElementName = name;
            }

            @Override
            public void beginScriptElement() {
            }

            @Override
            public void beginDefineElement() {
            }

            @Override
            public void readInternalAttribute(String name, String value) {
            }

            @Override
            public void readPropertyAttribute(String name, Class<?> sourceType, String value) {
            }

            @Override
            public void readUnknownStaticPropertyAttribute(String name, String value) {
                assertEquals(name, "Gadget.bar");
                assertEquals(value, "123456");
            }

            @Override
            public void readEventHandlerAttribute(String name, String value) {
            }

            @Override
            public void endElement(Object value) {
                if (unknownStaticPropertyElementName != null) {
                    if (unknownStaticPropertyElementName.equals("Gadget.bar")) {
                        assertEquals(value, "abcdef");
                    } else if (unknownStaticPropertyElementName.equals("Gadget.baz")) {
                        assertEquals(value.getClass(), Widget.class);
                    } else {
                        throw new RuntimeException();
                    }

                    unknownStaticPropertyElementName = null;
                }
            }
        });

        fxmlLoader.load();

        Map<String, Object> gadget = (Map<String, Object>)fxmlLoader.getNamespace().get("gadget");
        assertNotNull(gadget);

        Widget widget2 = (Widget)fxmlLoader.getNamespace().get("widget2");
        assertNotNull(widget2);
        assertEquals(widget2.getName(), "Widget 2");
    }
}
