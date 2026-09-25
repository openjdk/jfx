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

package test.com.sun.webkit;

import java.lang.classfile.ClassFile;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.Constructor;

/**
 * Defines classes that have a public method whose parameter type does not exist, so that
 * {@link Class#getMethods()} on them throws {@link NoClassDefFoundError}. That is the state an
 * application class is in when an optional dependency named in one of its signatures is missing at
 * run time. JNI's {@code GetMethodID} never listed a class, so it still found the one method it was
 * asked for; a lookup that lists every public method does not.
 * <p>
 * The classes are generated rather than compiled so that the missing type is missing for certain:
 * this loader refuses every name its parent cannot supply.
 */
public final class MissingTypeClassLoader extends ClassLoader {

    /** The parameter type of {@code use}, which no loader can find. */
    public static final String MISSING_TYPE = "lcprobe.Missing";

    /**
     * Creates a loader whose parent is the loader of the tests.
     */
    public MissingTypeClassLoader() {
        super(MissingTypeClassLoader.class.getClassLoader());
    }

    /**
     * Defines and instantiates a public class with a public no-argument constructor and a public
     * {@code void use(lcprobe.Missing)}.
     *
     * @param binaryName the binary name of the class, for example {@code "lcprobe.Opt"}
     * @param toString what a public {@code toString()} override returns, or {@code null} for a class
     *        that inherits {@code Object.toString()}
     * @return a new instance of the class
     */
    public Object newInstance(String binaryName, String toString) {
        return instantiate(define(binaryName, ConstantDescs.CD_Object, ClassFile.ACC_PUBLIC,
                toString, true));
    }

    /**
     * Defines and instantiates the same class as {@link #newInstance}, except that the class is
     * package private.
     *
     * @param binaryName the binary name of the class
     * @param toString what a public {@code toString()} override returns, or {@code null}
     * @return a new instance of the class
     */
    public Object newPackagePrivateInstance(String binaryName, String toString) {
        return instantiate(define(binaryName, ConstantDescs.CD_Object, 0, toString, true));
    }

    /**
     * Defines a package private class with the {@code use} and {@code toString()} of
     * {@link #newInstance}, then a public subclass of it that declares nothing but its constructor,
     * and instantiates the subclass. Its {@code toString()} is the package private class's.
     *
     * @param binaryName the binary name of the public subclass
     * @param superclassName the binary name of the package private superclass, in the same package
     * @param toString what the superclass's public {@code toString()} override returns
     * @return a new instance of the subclass
     */
    public Object newSubclassOfPackagePrivateInstance(String binaryName, String superclassName,
                                                      String toString) {
        define(superclassName, ConstantDescs.CD_Object, 0, toString, true);
        return instantiate(define(binaryName, ClassDesc.of(superclassName), ClassFile.ACC_PUBLIC,
                null, false));
    }

    /**
     * Defines and instantiates a public class with the {@code use} of {@link #newInstance} and a
     * public {@code double doubleValue()}. With a {@code superclassValue} the class extends a public
     * class named {@code binaryName + "Base"}, whose methods can be listed and whose own public
     * {@code doubleValue()} it overrides; without one it extends {@code Object}.
     *
     * @param binaryName the binary name of the class
     * @param value what the class's {@code doubleValue()} returns
     * @param superclassValue what the superclass's {@code doubleValue()} returns, or {@code null}
     *        for a class that extends {@code Object}
     * @return a new instance of the class
     */
    public Object newDoubleValueInstance(String binaryName, double value, Double superclassValue) {
        ClassDesc superclass = ConstantDescs.CD_Object;
        if (superclassValue != null) {
            String superclassName = binaryName + "Base";
            define(superclassName, superclass, ClassFile.ACC_PUBLIC, null, false, superclassValue);
            superclass = ClassDesc.of(superclassName);
        }
        return instantiate(define(binaryName, superclass, ClassFile.ACC_PUBLIC, null, true, value));
    }

    private Class<?> define(String binaryName, ClassDesc superclass, int access, String toString,
                            boolean withUse) {
        return define(binaryName, superclass, access, toString, withUse, null);
    }

    private Class<?> define(String binaryName, ClassDesc superclass, int access, String toString,
                            boolean withUse, Double doubleValue) {
        byte[] bytes = ClassFile.of().build(ClassDesc.of(binaryName), builder -> {
            builder.withFlags(access | ClassFile.ACC_SUPER);
            builder.withSuperclass(superclass);
            builder.withMethodBody(ConstantDescs.INIT_NAME, ConstantDescs.MTD_void,
                    ClassFile.ACC_PUBLIC,
                    code -> code.aload(0)
                            .invokespecial(superclass, ConstantDescs.INIT_NAME,
                                    ConstantDescs.MTD_void)
                            .return_());
            if (withUse) {
                builder.withMethodBody("use",
                        MethodTypeDesc.of(ConstantDescs.CD_void, ClassDesc.of(MISSING_TYPE)),
                        ClassFile.ACC_PUBLIC, code -> code.return_());
            }
            if (toString != null) {
                builder.withMethodBody("toString", MethodTypeDesc.of(ConstantDescs.CD_String),
                        ClassFile.ACC_PUBLIC, code -> code.ldc(toString).areturn());
            }
            if (doubleValue != null) {
                builder.withMethodBody("doubleValue", MethodTypeDesc.of(ConstantDescs.CD_double),
                        ClassFile.ACC_PUBLIC, code -> code.loadConstant(doubleValue).dreturn());
            }
        });
        return defineClass(binaryName, bytes, 0, bytes.length);
    }

    private static Object instantiate(Class<?> type) {
        try {
            // The unnamed module of this loader opens every package, so this works for a package
            // private class too.
            Constructor<?> constructor = type.getConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("cannot instantiate " + type.getName(), e);
        }
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }
}
