package test.javafx.fxml;
/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.fxml.FXMLLoaderHelper;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.fxml.FXMLLoader;
import javafx.fxml.LoadListener;
import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

public class FXMLLoader_ScriptTest {
    @Test
    @SuppressWarnings("deprecation")
    public void testStaticScriptLoad() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("static_script_load.fxml"));
        FXMLLoaderHelper.setStaticLoad(fxmlLoader, true);
        AtomicBoolean scriptCalled = new AtomicBoolean();
        AtomicBoolean scriptEndCalled = new AtomicBoolean();
        fxmlLoader.setLoadListener(new LoadListener() {

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
            }

            @Override
            public void beginScriptElement() {
                assertFalse(scriptCalled.getAndSet(true));
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
            }

            @Override
            public void readEventHandlerAttribute(String name, String value) {
            }

            @Override
            public void endElement(Object value) {
                if (value instanceof String && ((String) value).contains("doSomething")) {
                    assertTrue(scriptCalled.get());
                    assertFalse(scriptEndCalled.getAndSet(true));
                }
            }
        });

        fxmlLoader.load();
        assertTrue(scriptCalled.get());
        assertTrue(scriptEndCalled.get());
    }

    @Test
    public void testScriptHandler() throws IOException {

        // This test needs Nashorn script engine.
        // Test will be rewritten under - JDK-8245568
        assumeTrue(isNashornEngineAvailable());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("script_handler.fxml"));
        loader.load();

        Widget w = (Widget) loader.getNamespace().get("w");
        assertNotNull(w);
        loader.getNamespace().put("actionDone", new AtomicBoolean(false));
        w.fire();
        assertTrue(((AtomicBoolean) loader.getNamespace().get("actionDone")).get());
    }

    @Test
    public void testExternalScriptHandler() throws IOException {

        // This test needs Nashorn script engine.
        // Test will be rewritten under - JDK-8245568
        assumeTrue(isNashornEngineAvailable());

        FXMLLoader loader = new FXMLLoader(getClass().getResource("script_handler_external.fxml"));
        loader.load();

        Widget w = (Widget) loader.getNamespace().get("w");
        assertNotNull(w);
        loader.getNamespace().put("actionDone", new AtomicBoolean(false));
        w.fire();
        assertTrue(((AtomicBoolean)loader.getNamespace().get("actionDone")).get());
    }

    private boolean isNashornEngineAvailable() {
        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("nashorn");

        return (engine != null);
    }
}
