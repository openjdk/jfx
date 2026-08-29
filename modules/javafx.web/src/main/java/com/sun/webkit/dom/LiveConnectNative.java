/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.webkit.dom;

import com.sun.webkit.Utilities;
import com.sun.webkit.WKJLayouts;
import com.sun.webkit.WKJStringCodec;
import com.sun.webkit.WebKitNative;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.StructLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_CHAR;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * {@code WKJLiveConnectHost}: the 26 reflective upcalls that let the library expose an arbitrary
 * Java object to page script, installed by {@code wkj_live_connect_init}.
 * <p>
 * {@link JSObjectNative} is the other half of LiveConnect - the nine {@code wkj_js_*} entry points
 * that take a {@code JSObject} into JavaScript. This class is the way back: everything
 * {@code Source/WebCore/bridge/jni} does to a Java object once JavaScript holds one.
 * {@code JavaClassJSC}, {@code JavaFieldJSC}, {@code JavaMethodJSC} and {@code JavaArrayJSC}
 * enumerate a class, read and write its fields, invoke its methods and index its arrays, and every
 * one of those operations used to be a JNI call. Without this table they have no path back to Java
 * at all, so handing a Java object to script - {@code webEngine.executeScript("window").setMember(
 * "app", myObject)} - produces an object with no members, even though {@code JSObject} itself is
 * fully bound.
 * <p>
 * <b>Ownership.</b> Every slot that returns a {@code wkj_ref} mints a new strong id which the
 * library owns and releases exactly once; every {@code wkj_ref} parameter is borrowed for the
 * duration of the call. {@code invoke} is the one slot that produces two ids, its result and, when
 * the invocation threw, the {@code Throwable}.
 * <p>
 * <b>The allow list.</b> {@code invoke} goes through {@link Utilities#fwkInvokeWithContext}, which
 * is where the class and package rejection lists live; that is unchanged, because the JNI code
 * routed the actual invocation through the same method. {@code field_get}, {@code field_set} and
 * {@code unbox} deliberately do <em>not</em>, because the direct {@code Field.get*} and
 * {@code intValue()} calls of the JNI code did not either. Adding the check here would be a
 * behaviour change rather than a hardening, and it belongs to whoever revisits the security model.
 * <p>
 * <b>Threading.</b> The JavaFX application thread, which owns the JavaScript context, throughout.
 * <p>
 * This class contains no restricted {@code java.lang.foreign} operation.
 *
 * @see com.sun.webkit.dom.JSObjectNative
 */
final class LiveConnectNative {

    /*
     * WKJ_JT_*: the type tag of a WKJJavaValue. WKJ_JT_VOID is deliberately absent - it has no
     * member of its own and never reaches this table, because a void return from invoke is a
     * wkj_ref of 0 like a null one.
     */
    private static final int JT_INVALID = 0;
    private static final int JT_OBJECT = 2;
    private static final int JT_BOOLEAN = 3;
    private static final int JT_BYTE = 4;
    private static final int JT_CHAR = 5;
    private static final int JT_SHORT = 6;
    private static final int JT_INT = 7;
    private static final int JT_LONG = 8;
    private static final int JT_FLOAT = 9;
    private static final int JT_DOUBLE = 10;
    private static final int JT_ARRAY = 11;

    /** {@code WKJ_INIT_OK}. */
    private static final int INIT_OK = 0;

    private static final StructLayout JAVA_VALUE = WKJLayouts.JAVA_VALUE;
    private static final StructLayout HOST_LAYOUT = WKJLayouts.LIVE_CONNECT_HOST;

    private static final long OFFSET_TYPE = offsetOf("type");
    private static final long OFFSET_I = offsetOf("i");
    private static final long OFFSET_J = offsetOf("j");
    private static final long OFFSET_D = offsetOf("d");
    private static final long OFFSET_L = offsetOf("l");

    private static final MethodHandle LIVE_CONNECT_INIT = WebKitNative.downcall(
            "wkj_live_connect_init",
            FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_INT));

    /*
     * The library's own view of the three struct sizes, for the cheapest guard there is against the
     * two halves disagreeing about padding on one of the three platforms.
     */
    private static final MethodHandle SIZEOF_JAVA_VALUE = WebKitNative.downcall(
            "wkj_bridge_sizeof_java_value", FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle SIZEOF_JS_VALUE = WebKitNative.downcall(
            "wkj_bridge_sizeof_js_value", FunctionDescriptor.of(JAVA_INT));
    private static final MethodHandle SIZEOF_LIVE_CONNECT_HOST = WebKitNative.downcall(
            "wkj_bridge_sizeof_live_connect_host", FunctionDescriptor.of(JAVA_INT));

    /** The one {@code WKJLiveConnectHost} of this process, in the process-wide upcall arena. */
    private static final MemorySegment HOST = buildTable();

    /** The code {@code wkj_live_connect_init} returned; read by the binding test. */
    private static final int INIT_RESULT = install();

    private LiveConnectNative() {
    }

    private static long offsetOf(String member) {
        return JAVA_VALUE.byteOffset(PathElement.groupElement(member));
    }

    /**
     * Returns the code {@code wkj_live_connect_init} returned, initializing this class - and so
     * installing the table - if it has not been touched yet. {@link JSObjectNative} calls it from
     * its own initializer, which is what guarantees the table is in place before any
     * {@code wkj_js_*} call, and the binding test reads it.
     *
     * @return {@code WKJ_INIT_OK} on a healthy process
     */
    static int initResult() {
        return INIT_RESULT;
    }

    /**
     * Returns the installed table, for the test that checks its shape.
     *
     * @return the table
     */
    static MemorySegment hostTable() {
        return HOST;
    }

    /*
     * Checks the three sizes against the library, then installs the table.
     *
     * A library that answers 0 is not answering: sizeof is never 0 in C, and it is what the
     * recording stub - which returns whatever the test programmed, or 0 - produces for a function it
     * has no real body for. A real jfxwebkit compiles "return (int32_t) sizeof(X);" and can only
     * return the truth, so skipping the comparison on 0 costs nothing there and keeps the check
     * honest where it can be made.
     */
    private static int install() {
        checkSize("WKJJavaValue", SIZEOF_JAVA_VALUE, JAVA_VALUE.byteSize());
        checkSize("WKJJSValue", SIZEOF_JS_VALUE, JSObjectNative.JS_VALUE_LAYOUT.byteSize());
        checkSize("WKJLiveConnectHost", SIZEOF_LIVE_CONNECT_HOST, HOST_LAYOUT.byteSize());
        int result;
        try {
            result = (int) LIVE_CONNECT_INIT.invokeExact(HOST, (int) HOST_LAYOUT.byteSize(),
                    WebKitNative.WKJ_ABI_VERSION);
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        if (result != INIT_OK) {
            throw new UnsatisfiedLinkError("the jfxwebkit library rejected the javafx.web"
                    + " LiveConnect table with code " + result + "; sizeof(WKJLiveConnectHost) is "
                    + HOST_LAYOUT.byteSize() + " bytes here. Rebuild the library from this source"
                    + " tree.");
        }
        return result;
    }

    private static void checkSize(String name, MethodHandle sizeOf, long expected) {
        int actual;
        try {
            actual = (int) sizeOf.invokeExact();
        } catch (Throwable t) {
            throw new AssertionError(t);
        }
        if (actual != 0 && actual != expected) {
            throw new UnsatisfiedLinkError("sizeof(" + name + ") is " + expected
                    + " bytes in javafx.web and " + actual + " in the loaded library, so the two"
                    + " disagree about padding. Rebuild the library from this source tree.");
        }
    }

    /*
     * Builds the table: its own size, then the 26 stubs in the declaration order of the C struct.
     * The order below is the order of WKJLayouts.LIVE_CONNECT_HOST, and each slot is written by name
     * rather than by index, so a member added to the C header in the middle moves nothing here.
     */
    private static MemorySegment buildTable() {
        MemorySegment host = WebKitNative.allocateTable(HOST_LAYOUT);
        host.set(JAVA_INT, HOST_LAYOUT.byteOffset(
                PathElement.groupElement("size")),
                (int) HOST_LAYOUT.byteSize());
        MethodHandles.Lookup lookup = MethodHandles.lookup();

        slot(host, lookup, "object_get_class", "objectGetClass",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
        slot(host, lookup, "class_get_name", "classGetName",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
        slot(host, lookup, "class_is_array", "classIsArray",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "create_dummy_object", "createDummyObject",
                FunctionDescriptor.of(JAVA_LONG));
        slot(host, lookup, "resolve_method", "resolveMethod",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS, JAVA_INT));
        slot(host, lookup, "invoke", "invoke",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_LONG,
                        ADDRESS));
        slot(host, lookup, "method_get_name", "methodGetName",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
        slot(host, lookup, "method_get_return_type_name", "methodGetReturnTypeName",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
        slot(host, lookup, "method_get_parameter_count", "methodGetParameterCount",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "method_get_parameter_type_name", "methodGetParameterTypeName",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT, ADDRESS));
        slot(host, lookup, "method_get_modifiers", "methodGetModifiers",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "field_get_name", "fieldGetName",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
        slot(host, lookup, "field_get_type_name", "fieldGetTypeName",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
        slot(host, lookup, "field_get", "fieldGet",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS));
        slot(host, lookup, "field_set", "fieldSet",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS));
        slot(host, lookup, "array_length", "arrayLength",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG));
        slot(host, lookup, "array_get", "arrayGet",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT, ADDRESS));
        slot(host, lookup, "array_set", "arraySet",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, JAVA_INT, ADDRESS));
        slot(host, lookup, "box", "box", FunctionDescriptor.of(JAVA_LONG, ADDRESS));
        slot(host, lookup, "unbox", "unbox",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_INT, ADDRESS));
        slot(host, lookup, "box_string", "boxString",
                FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_INT));
        slot(host, lookup, "string_value", "stringValue",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, ADDRESS));
        slot(host, lookup, "describe_object", "describeObject",
                FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS));
        slot(host, lookup, "undefined_object", "undefinedObject",
                FunctionDescriptor.of(JAVA_LONG));
        slot(host, lookup, "jsobject_create", "jsObjectCreate",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT));
        slot(host, lookup, "node_get_cached_impl", "nodeGetCachedImpl",
                FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));
        return host;
    }

    private static void slot(MemorySegment host, MethodHandles.Lookup lookup, String member,
                             String method, FunctionDescriptor descriptor) {
        WebKitNative.installSlot(host, HOST_LAYOUT, member, lookup, method, descriptor);
    }

    // ------------------------------------------------- java.lang.Object and java.lang.Class

    /* obj.getClass(). Was callJNIMethod<Object>(obj, "getClass", "()Ljava/lang/Class;"). */
    private static long objectGetClass(long obj) {
        try {
            Object target = WebKitNative.lookup(obj);
            return target == null ? 0L : WebKitNative.register(target.getClass());
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.object_get_class", t);
            return 0L;
        }
    }

    /*
     * cls.getName(): the reflection name - "int", "[I", "java.lang.String", "[Ljava.lang.String;" -
     * and not a JNI descriptor. WKJ_STR_NULL becomes "<Unknown>" in the C++, exactly as it did when
     * getName returned nothing.
     */
    private static int classGetName(long cls, MemorySegment out, int capacity,
                                    MemorySegment length) {
        try {
            String name = WebKitNative.lookup(cls) instanceof Class<?> target
                    ? target.getName()
                    : null;
            return WebKitNative.emitString(name, out, capacity, length);
        } catch (Throwable t) {
            return failedString("liveconnect.class_get_name", t, length);
        }
    }

    /* cls.isArray(). Default when NULL: 0. */
    private static int classIsArray(long cls) {
        try {
            return WebKitNative.lookup(cls) instanceof Class<?> target && target.isArray() ? 1 : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.class_is_array", t);
            return 0;
        }
    }

    /*
     * new java.lang.Object(): the stand-in JavaClass builds when the object it was asked about has
     * already been collected, so that the rest of its constructor has something to reflect on. Was
     * FindClass("java/lang/Object") + NewObject in JavaClass::createDummyObject.
     */
    private static long createDummyObject() {
        try {
            return WebKitNative.register(new Object());
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.create_dummy_object", t);
            return 0L;
        }
    }

    // ------------------------------------------------------------------------------ methods

    /*
     * The java.lang.reflect.Method that ToReflectedMethod(GetObjectClass(obj), GetMethodID(...))
     * produced, or 0 if there is no such method.
     *
     * The search is over obj.getClass().getMethods(), which is the same set JavaClass enumerates and
     * the only set reachable here, and it matches the name plus the descriptor rebuilt from the
     * parameter types AND the return type. Matching the return type is not optional: a covariant
     * override gives a class two methods with one name and one parameter list, and picking the
     * bridge method rather than the real one changes what invoke() returns.
     *
     * The descriptor has to stay in the ABI rather than being replaced by the Method the C++ already
     * had, because Utilities.fwkInvokeWithContext decides whether the call is permitted from
     * method.getDeclaringClass(), so the search must reach the same Method JNI would have.
     */
    private static long resolveMethod(long obj, MemorySegment name, int nameLength,
                                      MemorySegment signature, int signatureLength) {
        try {
            Object target = WebKitNative.lookup(obj);
            String methodName = WebKitNative.readString(name, nameLength);
            String descriptor = WebKitNative.readString(signature, signatureLength);
            if (target == null || methodName == null || descriptor == null) {
                return 0L;
            }
            for (Method method : target.getClass().getMethods()) {
                if (method.getName().equals(methodName) && descriptor.equals(descriptorOf(method))) {
                    return WebKitNative.register(method);
                }
            }
            return 0L;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.resolve_method", t);
            return 0L;
        }
    }

    /*
     * The JNI method descriptor of a Method, for example "(I)Ljava/lang/String;". MethodType builds
     * it from the same reflective types the C++ built its own from, so the two agree by
     * construction rather than by a hand written table of primitive letters.
     */
    private static String descriptorOf(Method method) {
        return MethodType.methodType(method.getReturnType(), method.getParameterTypes())
                .descriptorString();
    }

    /*
     * Utilities.fwkInvokeWithContext(method, instance, args, acc): the actual invocation, which has
     * always happened in Java. Returns a new id for the result - null and a void return are both 0 -
     * and writes a new id for the Throwable through out_exception if the invocation threw.
     *
     * Java must not let the Throwable escape, and does not: it is caught and reported through
     * out_exception, which is the same swallowing the JNI code did with ExceptionOccurred +
     * ExceptionClear. The caller then wraps the Throwable in a JavaInstance and throws it into
     * JavaScript, which is unchanged. Deliberately NOT routed through upcallFailed: a Java method
     * that throws is a normal LiveConnect outcome, not a failed callback, and reporting it as one
     * would make the next core.check_and_clear_exception lie.
     */
    private static long invoke(long method, long instance, MemorySegment args, int argc, long acc,
                               MemorySegment outException) {
        try {
            if (!(WebKitNative.lookup(method) instanceof Method target)) {
                return 0L;
            }
            long[] refs = WebKitNative.readLongs(args, argc);
            Object[] values = new Object[argc];
            for (int i = 0; i < argc; i++) {
                values[i] = refs == null ? null : WebKitNative.lookup(refs[i]);
            }
            Object result;
            try {
                result = Utilities.fwkInvokeWithContext(target, WebKitNative.lookup(instance),
                        values, WebKitNative.lookup(acc));
            } catch (Throwable thrown) {
                WebKitNative.writeLong(outException, WebKitNative.register(thrown));
                return 0L;
            }
            return WebKitNative.register(result);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.invoke", t);
            return 0L;
        }
    }

    /* method.getName(). Default when NULL: WKJ_STR_NULL, i.e. "<Unknown>". */
    private static int methodGetName(long method, MemorySegment out, int capacity,
                                     MemorySegment length) {
        try {
            String name = WebKitNative.lookup(method) instanceof Method target
                    ? target.getName()
                    : null;
            return WebKitNative.emitString(name, out, capacity, length);
        } catch (Throwable t) {
            return failedString("liveconnect.method_get_name", t, length);
        }
    }

    /*
     * method.getReturnType().getName(). One call instead of the two the JNI code made; the
     * intermediate Class was never used for anything else. Default: WKJ_STR_NULL.
     */
    private static int methodGetReturnTypeName(long method, MemorySegment out, int capacity,
                                               MemorySegment length) {
        try {
            String name = WebKitNative.lookup(method) instanceof Method target
                    ? target.getReturnType().getName()
                    : null;
            return WebKitNative.emitString(name, out, capacity, length);
        } catch (Throwable t) {
            return failedString("liveconnect.method_get_return_type_name", t, length);
        }
    }

    /* method.getParameterTypes().length. Default when NULL: 0. */
    private static int methodGetParameterCount(long method) {
        try {
            return WebKitNative.lookup(method) instanceof Method target
                    ? target.getParameterCount()
                    : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.method_get_parameter_count", t);
            return 0;
        }
    }

    /*
     * method.getParameterTypes()[index].getName(). Default: WKJ_STR_NULL, i.e. "<Unknown>", which is
     * what the JNI code substituted for a parameter whose name it could not read - and which is also
     * the right answer for an index the caller got wrong.
     */
    private static int methodGetParameterTypeName(long method, int index, MemorySegment out,
                                                  int capacity, MemorySegment length) {
        try {
            String name = null;
            if (WebKitNative.lookup(method) instanceof Method target) {
                Class<?>[] parameters = target.getParameterTypes();
                if (index >= 0 && index < parameters.length) {
                    name = parameters[index].getName();
                }
            }
            return WebKitNative.emitString(name, out, capacity, length);
        } catch (Throwable t) {
            return failedString("liveconnect.method_get_parameter_type_name", t, length);
        }
    }

    /* method.getModifiers(). Only bit 0x8, ACC_STATIC, is read. Default when NULL: 0. */
    private static int methodGetModifiers(long method) {
        try {
            return WebKitNative.lookup(method) instanceof Method target
                    ? target.getModifiers()
                    : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.method_get_modifiers", t);
            return 0;
        }
    }

    // ------------------------------------------------------------------------------- fields

    /* field.getName(). Default when NULL: WKJ_STR_NULL, i.e. "<Unknown>". */
    private static int fieldGetName(long field, MemorySegment out, int capacity,
                                    MemorySegment length) {
        try {
            String name = WebKitNative.lookup(field) instanceof Field target
                    ? target.getName()
                    : null;
            return WebKitNative.emitString(name, out, capacity, length);
        } catch (Throwable t) {
            return failedString("liveconnect.field_get_name", t, length);
        }
    }

    /* field.getType().getName(), one call for the two the JNI code made. Default: WKJ_STR_NULL. */
    private static int fieldGetTypeName(long field, MemorySegment out, int capacity,
                                        MemorySegment length) {
        try {
            String name = WebKitNative.lookup(field) instanceof Field target
                    ? target.getType().getName()
                    : null;
            return WebKitNative.emitString(name, out, capacity, length);
        } catch (Throwable t) {
            return failedString("liveconnect.field_get_type_name", t, length);
        }
    }

    /*
     * The typed read of one field of one instance: field.get(instance) for the two reference types
     * and the matching Field.getBoolean / getByte / ... for the primitives. On failure `out` is left
     * WKJ_JT_INVALID, which is what the zeroed jvalue meant. Default when NULL: 0.
     */
    private static int fieldGet(long field, long instance, int type, MemorySegment out) {
        MemorySegment value = sizedValue(out);
        try {
            if (!(WebKitNative.lookup(field) instanceof Field target)) {
                setInvalid(value);
                return 0;
            }
            Object receiver = WebKitNative.lookup(instance);
            switch (type) {
                case JT_OBJECT, JT_ARRAY -> writeObject(value, type, target.get(receiver));
                case JT_BOOLEAN -> writeInt(value, type, target.getBoolean(receiver) ? 1 : 0);
                case JT_BYTE -> writeInt(value, type, target.getByte(receiver));
                case JT_CHAR -> writeInt(value, type, target.getChar(receiver));
                case JT_SHORT -> writeInt(value, type, target.getShort(receiver));
                case JT_INT -> writeInt(value, type, target.getInt(receiver));
                case JT_LONG -> writeLong(value, type, target.getLong(receiver));
                case JT_FLOAT -> writeDouble(value, type, target.getFloat(receiver));
                case JT_DOUBLE -> writeDouble(value, type, target.getDouble(receiver));
                default -> {
                    setInvalid(value);
                    return 0;
                }
            }
            return 1;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.field_get", t);
            setInvalid(value);
            return 0;
        }
    }

    /* The matching typed write: field.set / setBoolean / ... (instance, value). Default: 0. */
    private static int fieldSet(long field, long instance, int type, MemorySegment in) {
        try {
            if (!(WebKitNative.lookup(field) instanceof Field target)) {
                return 0;
            }
            MemorySegment value = sizedValue(in);
            if (value.address() == 0L) {
                return 0;
            }
            Object receiver = WebKitNative.lookup(instance);
            switch (type) {
                case JT_OBJECT, JT_ARRAY ->
                        target.set(receiver, WebKitNative.lookup(value.get(JAVA_LONG, OFFSET_L)));
                case JT_BOOLEAN ->
                        target.setBoolean(receiver, value.get(JAVA_INT, OFFSET_I) != 0);
                case JT_BYTE -> target.setByte(receiver, (byte) value.get(JAVA_INT, OFFSET_I));
                case JT_CHAR -> target.setChar(receiver, (char) value.get(JAVA_INT, OFFSET_I));
                case JT_SHORT -> target.setShort(receiver, (short) value.get(JAVA_INT, OFFSET_I));
                case JT_INT -> target.setInt(receiver, value.get(JAVA_INT, OFFSET_I));
                case JT_LONG -> target.setLong(receiver, value.get(JAVA_LONG, OFFSET_J));
                case JT_FLOAT ->
                        target.setFloat(receiver, (float) value.get(JAVA_DOUBLE, OFFSET_D));
                case JT_DOUBLE -> target.setDouble(receiver, value.get(JAVA_DOUBLE, OFFSET_D));
                default -> {
                    return 0;
                }
            }
            return 1;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.field_set", t);
            return 0;
        }
    }

    // ------------------------------------------------------------------------- Java arrays

    /*
     * GetArrayLength, used both for the Field[] and Method[] that the class enumeration walks and
     * for a Java array exposed to script as a JS array. Default when NULL: 0.
     */
    private static int arrayLength(long array) {
        try {
            Object target = WebKitNative.lookup(array);
            return target != null && target.getClass().isArray() ? Array.getLength(target) : 0;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.array_length", t);
            return 0;
        }
    }

    /*
     * One element, typed as for field_get. Replaces GetObjectArrayElement and the
     * Get<Type>ArrayRegion family. Returns 0 for an out of range index, which the JNI code left to
     * the JVM to report. Default when NULL: 0.
     */
    private static int arrayGet(long array, int index, int type, MemorySegment out) {
        MemorySegment value = sizedValue(out);
        try {
            Object target = WebKitNative.lookup(array);
            if (target == null || !target.getClass().isArray() || index < 0
                    || index >= Array.getLength(target)) {
                setInvalid(value);
                return 0;
            }
            switch (type) {
                case JT_OBJECT, JT_ARRAY -> writeObject(value, type, Array.get(target, index));
                case JT_BOOLEAN -> writeInt(value, type, Array.getBoolean(target, index) ? 1 : 0);
                case JT_BYTE -> writeInt(value, type, Array.getByte(target, index));
                case JT_CHAR -> writeInt(value, type, Array.getChar(target, index));
                case JT_SHORT -> writeInt(value, type, Array.getShort(target, index));
                case JT_INT -> writeInt(value, type, Array.getInt(target, index));
                case JT_LONG -> writeLong(value, type, Array.getLong(target, index));
                case JT_FLOAT -> writeDouble(value, type, Array.getFloat(target, index));
                case JT_DOUBLE -> writeDouble(value, type, Array.getDouble(target, index));
                default -> {
                    setInvalid(value);
                    return 0;
                }
            }
            return 1;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.array_get", t);
            setInvalid(value);
            return 0;
        }
    }

    /* One element written, replacing SetObjectArrayElement and Set<Type>ArrayRegion. Default: 0. */
    private static int arraySet(long array, int index, int type, MemorySegment in) {
        try {
            Object target = WebKitNative.lookup(array);
            MemorySegment value = sizedValue(in);
            if (target == null || !target.getClass().isArray() || value.address() == 0L
                    || index < 0 || index >= Array.getLength(target)) {
                return 0;
            }
            switch (type) {
                case JT_OBJECT, JT_ARRAY ->
                        Array.set(target, index,
                                WebKitNative.lookup(value.get(JAVA_LONG, OFFSET_L)));
                case JT_BOOLEAN ->
                        Array.setBoolean(target, index, value.get(JAVA_INT, OFFSET_I) != 0);
                case JT_BYTE -> Array.setByte(target, index, (byte) value.get(JAVA_INT, OFFSET_I));
                case JT_CHAR -> Array.setChar(target, index, (char) value.get(JAVA_INT, OFFSET_I));
                case JT_SHORT ->
                        Array.setShort(target, index, (short) value.get(JAVA_INT, OFFSET_I));
                case JT_INT -> Array.setInt(target, index, value.get(JAVA_INT, OFFSET_I));
                case JT_LONG -> Array.setLong(target, index, value.get(JAVA_LONG, OFFSET_J));
                case JT_FLOAT ->
                        Array.setFloat(target, index, (float) value.get(JAVA_DOUBLE, OFFSET_D));
                case JT_DOUBLE -> Array.setDouble(target, index, value.get(JAVA_DOUBLE, OFFSET_D));
                default -> {
                    return 0;
                }
            }
            return 1;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.array_set", t);
            return 0;
        }
    }

    // ----------------------------------------------------------- boxing, unboxing, strings

    /*
     * Boolean.valueOf / Byte.valueOf / ... chosen by value->type. WKJ_JT_OBJECT and WKJ_JT_ARRAY are
     * not valid here - an object needs no box - and neither are VOID and INVALID. Default: 0.
     */
    private static long box(MemorySegment in) {
        try {
            MemorySegment value = sizedValue(in);
            if (value.address() == 0L) {
                return 0L;
            }
            Object boxed = switch (value.get(JAVA_INT, OFFSET_TYPE)) {
                case JT_BOOLEAN -> Boolean.valueOf(value.get(JAVA_INT, OFFSET_I) != 0);
                case JT_BYTE -> Byte.valueOf((byte) value.get(JAVA_INT, OFFSET_I));
                case JT_CHAR -> Character.valueOf((char) value.get(JAVA_INT, OFFSET_I));
                case JT_SHORT -> Short.valueOf((short) value.get(JAVA_INT, OFFSET_I));
                case JT_INT -> Integer.valueOf(value.get(JAVA_INT, OFFSET_I));
                case JT_LONG -> Long.valueOf(value.get(JAVA_LONG, OFFSET_J));
                case JT_FLOAT -> Float.valueOf((float) value.get(JAVA_DOUBLE, OFFSET_D));
                case JT_DOUBLE -> Double.valueOf(value.get(JAVA_DOUBLE, OFFSET_D));
                default -> null;
            };
            return WebKitNative.register(boxed);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.box", t);
            return 0L;
        }
    }

    /*
     * booleanValue() / byteValue() / ... on a boxed value, chosen by `type`. The JNI code reached
     * these through callJNIMethod<T>(obj, "intValue", "()I") and friends, so they bypassed the
     * Utilities allow list, and so does this. Default when NULL: 0.
     */
    private static int unbox(long boxed, int type, MemorySegment out) {
        MemorySegment value = sizedValue(out);
        try {
            Object target = WebKitNative.lookup(boxed);
            switch (type) {
                case JT_BOOLEAN -> {
                    if (!(target instanceof Boolean b)) {
                        setInvalid(value);
                        return 0;
                    }
                    writeInt(value, type, b ? 1 : 0);
                }
                case JT_CHAR -> {
                    if (!(target instanceof Character c)) {
                        setInvalid(value);
                        return 0;
                    }
                    writeInt(value, type, c);
                }
                case JT_BYTE, JT_SHORT, JT_INT -> {
                    if (!(target instanceof Number n)) {
                        setInvalid(value);
                        return 0;
                    }
                    writeInt(value, type, n.intValue());
                }
                case JT_LONG -> {
                    if (!(target instanceof Number n)) {
                        setInvalid(value);
                        return 0;
                    }
                    writeLong(value, type, n.longValue());
                }
                case JT_FLOAT, JT_DOUBLE -> {
                    if (!(target instanceof Number n)) {
                        setInvalid(value);
                        return 0;
                    }
                    writeDouble(value, type, type == JT_FLOAT ? n.floatValue() : n.doubleValue());
                }
                default -> {
                    setInvalid(value);
                    return 0;
                }
            }
            return 1;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.unbox", t);
            setInvalid(value);
            return 0;
        }
    }

    /*
     * A java.lang.String holding the given UTF-16 characters; a NULL pointer means null and returns
     * 0. The replacement for String::toJavaString, and how a JS string becomes a Java String without
     * the library naming a Java type. Default when NULL: 0.
     */
    private static long boxString(MemorySegment chars, int length) {
        try {
            return WebKitNative.register(WebKitNative.readString(chars, length));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.box_string", t);
            return 0L;
        }
    }

    /*
     * The characters of a java.lang.String. Replaces GetStringChars and GetStringCritical on a value
     * that reached the library as an object rather than as a parameter - the result of toString(),
     * for instance. A value that is not a String is WKJ_STR_NULL. Default when NULL: WKJ_STR_NULL.
     */
    private static int stringValue(long str, MemorySegment out, int capacity,
                                   MemorySegment length) {
        try {
            String value = WebKitNative.lookup(str) instanceof String s ? s : null;
            return WebKitNative.emitString(value, out, capacity, length);
        } catch (Throwable t) {
            return failedString("liveconnect.string_value", t, length);
        }
    }

    // ------------------------------------------------------------------------ describe

    /*
     * Describes one Java object as a WKJJSValue, using the inbound rules of the C header. The
     * classification is JSObjectNative.kindOf, which is the same one the arguments of
     * wkj_js_set_member, wkj_js_set_slot and wkj_js_call go through - one implementation and two
     * callers, so a value cannot be classified one way as an argument and another way as a field
     * value.
     *
     * out->string / out->string_cap is the caller's buffer. WKJ_STR_OVERFLOW with
     * out->string_length set to the capacity required asks the caller to grow and try again;
     * describing an object twice has no side effects. For a Java object, out->object is a NEW strong
     * id which the library releases when it is done with it - and what it does with it first is wrap
     * it weakly in a JavaInstance, so the object stays exactly as collectable as it was under the
     * weak global reference this replaces. Default when NULL: WKJ_JS_KIND_NULL, WKJ_STR_OK.
     */
    private static int describeObject(long obj, MemorySegment out) {
        MemorySegment value = out.address() == 0L
                ? MemorySegment.NULL
                : WebKitNative.resize(out, JSObjectNative.JS_VALUE_LAYOUT.byteSize());
        try {
            if (value.address() == 0L) {
                return WKJStringCodec.OK;
            }
            Object target = WebKitNative.lookup(obj);
            int kind = JSObjectNative.kindOf(target);
            value.set(JAVA_INT, JSObjectNative.OFFSET_KIND, kind);
            switch (kind) {
                case JSObjectNative.KIND_JS_OBJECT -> {
                    JSObject js = (JSObject) target;
                    value.set(JAVA_LONG, JSObjectNative.OFFSET_PEER, js.getPeer());
                    value.set(JAVA_INT, JSObjectNative.OFFSET_PEER_TYPE, js.getPeerType());
                }
                case JSObjectNative.KIND_STRING -> {
                    return emitDescribedString(value, (String) target);
                }
                case JSObjectNative.KIND_BOOLEAN ->
                        value.set(JAVA_DOUBLE, JSObjectNative.OFFSET_NUMBER,
                                ((Boolean) target) ? 1.0 : 0.0);
                case JSObjectNative.KIND_DOUBLE ->
                        value.set(JAVA_DOUBLE, JSObjectNative.OFFSET_NUMBER,
                                ((Number) target).doubleValue());
                case JSObjectNative.KIND_JAVA_OBJECT ->
                        value.set(JAVA_LONG, JSObjectNative.OFFSET_OBJECT,
                                WebKitNative.register(target));
                default -> {
                    // KIND_NULL: the kind is the whole answer.
                }
            }
            return WKJStringCodec.OK;
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.describe_object", t);
            if (value.address() != 0L) {
                value.set(JAVA_INT, JSObjectNative.OFFSET_KIND, JSObjectNative.KIND_NULL);
            }
            return WKJStringCodec.OK;
        }
    }

    private static int emitDescribedString(MemorySegment value, String text) {
        MemorySegment buffer = value.get(ADDRESS, JSObjectNative.OFFSET_STRING);
        int capacity = value.get(JAVA_INT, JSObjectNative.OFFSET_STRING_CAP);
        value.set(JAVA_INT, JSObjectNative.OFFSET_STRING_LENGTH, text.length());
        if (buffer.address() == 0L || text.length() > capacity) {
            return WKJStringCodec.OVERFLOW;
        }
        if (!text.isEmpty()) {
            MemorySegment.copy(text.toCharArray(), 0,
                    WebKitNative.resize(buffer, (long) capacity * Character.BYTES),
                    JAVA_CHAR, 0L, text.length());
        }
        return WKJStringCodec.OK;
    }

    // ------------------------------------- the three objects the library cannot describe

    /*
     * The JSObject.UNDEFINED singleton: the value LiveConnect hands back for JavaScript undefined.
     * This is where a GetStaticFieldID read of a private static field went, and it cannot become a
     * plain value on this ABI because the identity of the field is the point - JSObject.java creates
     * it as `new String("undefined")` precisely so that it is not equal by reference to any other
     * "undefined" string, and callers compare against it with ==. Default when NULL: 0, which
     * reaches Java as null, the same thing a failed field lookup produced.
     */
    private static long undefinedObject() {
        try {
            return WebKitNative.register(JSObject.UNDEFINED);
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.undefined_object", t);
            return 0L;
        }
    }

    /*
     * new JSObject(peer, peer_type), for a JavaScript object the library has already gc-protected.
     * The protection is dropped by wkj_js_unprotect from the disposer of the Java object, which is
     * unchanged. Default when NULL: 0.
     */
    private static long jsObjectCreate(long peer, int peerType) {
        try {
            return WebKitNative.register(new JSObject(peer, peerType));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.jsobject_create", t);
            return 0L;
        }
    }

    /*
     * NodeImpl.getCachedImpl(peer), for a DOM node the library has already called ref() on; the
     * matching deref() is in the NodeImpl disposer - or getCachedImpl drops it at once on a cache
     * hit - and neither side may add or remove one here. getCachedImpl is private, which is why the
     * JNI code needed GetStaticMethodID; NodeImpl.create is its package private front door, so the
     * privacy is now respected rather than worked around. Default when NULL: 0.
     */
    private static long nodeGetCachedImpl(long nodePeer) {
        try {
            return WebKitNative.register(NodeImpl.create(nodePeer));
        } catch (Throwable t) {
            WebKitNative.upcallFailed("liveconnect.node_get_cached_impl", t);
            return 0L;
        }
    }

    // --------------------------------------------------------------- WKJJavaValue plumbing

    private static MemorySegment sizedValue(MemorySegment pointer) {
        return pointer.address() == 0L
                ? MemorySegment.NULL
                : WebKitNative.resize(pointer, JAVA_VALUE.byteSize());
    }

    private static void setInvalid(MemorySegment value) {
        if (value.address() != 0L) {
            value.set(JAVA_INT, OFFSET_TYPE, JT_INVALID);
        }
    }

    private static void writeInt(MemorySegment value, int type, int i) {
        if (value.address() != 0L) {
            value.set(JAVA_INT, OFFSET_TYPE, type);
            value.set(JAVA_INT, OFFSET_I, i);
        }
    }

    private static void writeLong(MemorySegment value, int type, long j) {
        if (value.address() != 0L) {
            value.set(JAVA_INT, OFFSET_TYPE, type);
            value.set(JAVA_LONG, OFFSET_J, j);
        }
    }

    private static void writeDouble(MemorySegment value, int type, double d) {
        if (value.address() != 0L) {
            value.set(JAVA_INT, OFFSET_TYPE, type);
            value.set(JAVA_DOUBLE, OFFSET_D, d);
        }
    }

    /* An object or array member is a NEW strong id the library owns and releases exactly once. */
    private static void writeObject(MemorySegment value, int type, Object object) {
        if (value.address() != 0L) {
            value.set(JAVA_INT, OFFSET_TYPE, type);
            value.set(JAVA_LONG, OFFSET_L, WebKitNative.register(object));
        }
    }

    private static int failedString(String slot, Throwable t, MemorySegment length) {
        WebKitNative.upcallFailed(slot, t);
        WebKitNative.writeInt(length, 0);
        return WKJStringCodec.NULL;
    }
}
