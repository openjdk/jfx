/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.javafx.fxml.annotation;

import java.util.Set;
import javafx.fxml.FXML;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.SimpleElementVisitor8;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("javafx.fxml.FXML")
public class FXMLAnnotationProcessor extends AbstractProcessor{

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(FXML.class);
        for (Element e : annotatedElements) {
            e.accept(new SimpleElementVisitor8<Void, Void>() {

                @Override
                public Void visitExecutable(ExecutableElement e, Void p) {
                    checkModifiers(e, e.getModifiers());
                    return null;
                }

                @Override
                public Void visitVariable(VariableElement e, Void p) {
                    checkModifiers(e, e.getModifiers());
                    return null;
                }

                private void checkModifiers(Element e, Set<Modifier> modifiers) {
                    for (Modifier m : modifiers) {
                        final String mod = m.name().toLowerCase();
                        final String type = e.getKind().name().toLowerCase();
                        switch(m) {
                            case NATIVE:
                            case STATIC:
                            case FINAL:
                                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                        "@FXML annotation cannot be used with "
                                        + mod + " " + type, e);
                                break;
                        }
                    }

                }

            }, null);
        }
        return true;
    }
    
}
