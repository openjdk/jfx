/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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

package javafx.builder.processor;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

final class AnnotationUtils {

    public static final String inputOption = "javafx.builders.input";
    public static final String outputOption = "javafx.builders.output";
    public static final String verboseOption = "javafx.builders.verbose";

    public Types types;
    public Elements elements; // TODO ? private?

    private NoType voidType;
    private PrimitiveType booleanType;
    private TypeMirror rawCollectionType;
    private TypeMirror rawObservableListType;
    private ProcessingEnvironment processingEnv;
    public boolean verbose = false;
    public Pattern inputPat = Pattern.compile("(javafx\\..*)");
    private String outputRepl = "$1Builder";

    public AnnotationUtils(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;

        types = processingEnv.getTypeUtils();
        elements = processingEnv.getElementUtils();
        voidType = types.getNoType(TypeKind.VOID);
        booleanType = types.getPrimitiveType(TypeKind.BOOLEAN);
        rawCollectionType = types.getDeclaredType(elements.getTypeElement(Collection.class.getName()));
        TypeElement observableArrayListType = elements.getTypeElement("javafx.collections.ObservableList");
        if (observableArrayListType != null) {
            this.rawObservableListType = types.getDeclaredType(observableArrayListType);
        }
        Map<String, String> options = processingEnv.getOptions();
        if (options.containsKey(verboseOption)) {
            verbose = Boolean.parseBoolean(options.get(verboseOption));
        }
        String input = options.get(inputOption);
        if (input != null) {
            try {
                inputPat = Pattern.compile(input);
            } catch (PatternSyntaxException e) {
                error("Value for option " + inputOption + " is not a valid regular expression: " + e.getMessage());
                throw e;
            }
        }
        if (options.containsKey(outputOption)) {
            outputRepl = options.get(outputOption);
        }
    }

    public void note(String msg) {
        if (verbose) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
        }
    }

    public void warn(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, msg);
    }

    public void error(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

    public boolean isCollection(TypeMirror type) {
        return types.isSubtype(types.erasure(type), rawCollectionType);
    }

    public boolean isObservableList(TypeMirror type) {
        return (rawObservableListType != null)
                && types.isSubtype(types.erasure(type), rawObservableListType);
    }

    public boolean isVoidType(TypeMirror type) {
        return types.isSameType(voidType, type);
    }

    public boolean isBooleanType(TypeMirror type) {
        return types.isSameType(booleanType, type);
    }
    
    public boolean isSameType(TypeMirror t1, TypeMirror t2) {
        return types.isSameType(t1, t2);
    }

    public String getBuilderName(String type) {
        Matcher m = inputPat.matcher(type);
        if (m.matches()) {
            String name = m.replaceAll(outputRepl);
            return name;
        } else {
            error("Internal error: pattern does not match " + type);
            return type + "Builder";
        }
    }
    
    
    public TypeElement getBuilderExtension(TypeElement type) {        
        return elements.getTypeElement("com.sun." + type.getQualifiedName() + "BuilderExtension");
    }

    // A string of the type parameters of this type, without the enclosing <>. For List<E>, it will be "E";
    // for Integer, it will be ""; for Map<K, V>, it will be "K, V".
    // If there is an "extends" clause it will be included, so for example
    // ValueAxis<T extends Number> will return "T extends Number"
    public static String typeParameterString(TypeElement type) {
        String s = "";
        for (TypeParameterElement p : type.getTypeParameters()) {
            if (!s.isEmpty()) {
                s += ", ";
            }
            s += p;
            String b = p.getBounds().toString();
            if (b.startsWith("[") && b.endsWith("]")) {
                b = b.substring(1, b.length() - 1).trim();
            }
            if (!b.equals("java.lang.Object")) {
                s += " extends " + b;
            }
        }
        return s;
    }

    // A string of the type arguments of this type. Given ValueAxis<T> extends Axis<T>, if we ask about
    // Axis<T> then the returned string will be "T".
    public static String typeArgumentString(TypeMirror type0) {
        DeclaredType type = (DeclaredType) type0;
        String s = "";
        for (TypeMirror p : type.getTypeArguments()) {
            if (!s.isEmpty()) {
                s += ", ";
            }
            s += p;
        }
        return s;
    }

    // returns String with arguments of a method
    // E.g. for resize(x, y) it returns "x, y"
    public static String methodCallParameterString(ExecutableElement type) {
        StringBuilder sb = new StringBuilder();

        for (VariableElement p : type.getParameters()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(p.getSimpleName());
        }
        return sb.toString();
    }

    // returns String with arguments with types of a method
    // E.g. for resize(int x, int y) it returns "int x, int y"
    public static String methodParameterString(ExecutableElement type) {
        StringBuilder sb = new StringBuilder();
        for (VariableElement p : type.getParameters()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }

            TypeMirror paramType = p.asType();
            if (paramType.getKind() == TypeKind.ARRAY) {
                sb.append(((ArrayType) paramType).getComponentType());
                sb.append(type.isVarArgs() ? "..." : "[]");
            } else {
                sb.append(p.asType());
            }

            sb.append(" ");
            sb.append(p.getSimpleName());
        }

        return sb.toString();
    }

    // Returns parameters of typed create()
    // e.g. for this create:
    // public static <T> ChoiceBoxBuilder<T, ?> create(final Class<T> type1)
    // it returns final Class<T> type1
    public static String typedCreateParamString(TypeElement type) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (TypeParameterElement p : type.getTypeParameters()) {
            if (sb.length() > 0) {
                sb.append("> type");
                sb.append(i);
                sb.append(" , ");
            }
            sb.append("final Class<");
            sb.append(p);
            i++;
        }
        sb.append("> type");
        sb.append(i);

        return sb.toString();
    }

    // Returns a string of the type arguments for non-typed create()
    // e.g. <?, ?> in
    // public static javafx.scene.control.ChoiceBoxBuilder<?, ?> create()
    public static String nonTypedCreateArgumentString(TypeElement type, boolean isFinal) {
        StringBuilder sb = new StringBuilder();
        for (int j = 1; j <= type.getTypeParameters().size(); j++) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append("?");
        }
        if (!isFinal) {
            sb.append(", ?");
        }
        return sb.toString();
    }

    public static boolean hasOneArgument(TypeMirror type) {
        return (type instanceof DeclaredType) 
                && (((DeclaredType) type).getTypeArguments().size() == 1);
    }

    // Returns true, if the type is a "wildcard type" e.g.
    // List<? extend Color>
    public static boolean isWildcardType(TypeMirror type) {
        boolean result = false;
        if (type instanceof DeclaredType) {
            DeclaredType dType = (DeclaredType) type;
            for (TypeMirror typeArg : dType.getTypeArguments()) {
                if (typeArg instanceof WildcardType) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }
}
