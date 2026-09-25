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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * How LiveConnect finds a Java method that the library names by string, where the JNI code asked
 * {@code GetMethodID}: the {@link Method} behind {@code resolve_method}, and the
 * {@code doubleValue()} that {@code unbox} calls to turn an object into a JavaScript number.
 * <p>
 * {@code GetMethodID} looked up one name and descriptor in the runtime class, its superclasses and
 * its interfaces. It never listed a class's methods and it ignored access checks. The two lookups
 * here keep as much of that as Java reflection allows. Each caches its answer per runtime class in a
 * {@link ClassValue}, so a class is searched once rather than on every call from script, and the
 * answer, no answer included, then stands for the life of the class where {@code GetMethodID}
 * resolved again each time (see {@code resolveMethod}).
 * <p>
 * Neither lookup lets page script reach more than the JNI code let it reach. {@link #resolveMethod}
 * answers the {@code Method} the JNI code produced, which {@link Utilities#fwkInvokeWithContext}
 * then checks as it always did. Its one substitution is taken only when that check, made on the
 * class the JNI code would have reported, passes (see {@code objectDeclarationStandingFor}).
 * {@link #doubleValueAccessor} reaches a subset of what JNI reached, because JNI also ignored module
 * encapsulation. For a class whose own methods cannot be listed it can call a different method
 * than the one JNI called for that object; {@code declaredAccessor} says when.
 * <p>
 * Threading: the JavaFX application thread in practice; both caches are safe on any thread.
 */
final class LiveConnectLookup {

    /*
     * resolve_method's answers per runtime class, keyed by name and JNI descriptor. An entry holds
     * what the first search found: the first match in getMethods() order, bridge methods included,
     * which is the choice resolve_method has always made. Later calls get that same Method without
     * copying the class's public Method[] again.
     *
     * Handing out one Method object many times is safe because nothing on the invocation path
     * changes it: Utilities.fwkInvokeWithContext, MethodHelper and MethodUtil read it and call
     * invoke(), and DOUBLE_VALUE below never uses these objects.
     */
    private static final ClassValue<Map<Signature, Resolution>> RESOLVED = new ClassValue<>() {
        @Override
        protected Map<Signature, Resolution> computeValue(Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    /*
     * unbox's conversion of an object that is not a java.lang.Number: a handle of type
     * (Object)double on the instance method double doubleValue() of the runtime class, or null when
     * the class has none that can be reached.
     *
     * The handles may be built from Methods made accessible with trySetAccessible, so they stay in
     * this class and are never registered or passed to Utilities.fwkInvokeWithContext.
     */
    private static final ClassValue<MethodHandle> DOUBLE_VALUE = new ClassValue<>() {
        @Override
        protected MethodHandle computeValue(Class<?> type) {
            return accessor(type, "doubleValue", double.class);
        }
    };

    private LiveConnectLookup() {
    }

    /* A method name and JNI descriptor, for example "greet" and "(I)Ljava/lang/String;". */
    private record Signature(String name, String descriptor) {
    }

    /* A cached answer. The method is null when the class has no such public method. */
    private record Resolution(Method method) {
    }

    /**
     * Returns the public method of {@code type} with this name and JNI descriptor, the one
     * {@code ToReflectedMethod(GetObjectClass(obj), GetMethodID(...))} produced for an object of
     * that class, or {@code null} if there is none.
     *
     * @param type the runtime class of the target object
     * @param name the method name
     * @param descriptor the JNI method descriptor, return type included
     * @return the method, or {@code null}
     */
    static Method resolveMethod(Class<?> type, String name, String descriptor) {
        // The answer is kept for as long as the class lives, and that includes no answer and one
        // found through the LinkageError fallback below. GetMethodID resolved again on every call.
        // Its answer never depended on anything that changes later, but this search's can: once a
        // class loader supplies the missing type, getMethods() stops throwing, and a class the
        // fallback could not answer for would be answered. The cached answer does not follow.
        // DOUBLE_VALUE keeps its answers, null included, the same way, so a package opened to
        // javafx.web later does not change them either. FFM-ABI-CONTRACT.md section 13.3.
        Map<Signature, Resolution> cache = RESOLVED.get(type);
        Signature signature = new Signature(name, descriptor);
        Resolution resolution = cache.get(signature);
        if (resolution == null) {
            // Not computeIfAbsent: the search may resolve on a superclass, and it is simpler not to
            // run reflection while holding a lock inside the map.
            Resolution found = new Resolution(search(type, signature));
            Resolution raced = cache.putIfAbsent(signature, found);
            resolution = raced == null ? found : raced;
        }
        return resolution.method();
    }

    /**
     * Returns a handle that computes {@code obj.doubleValue()} for an object of class {@code type}
     * the way {@code GetMethodID(GetObjectClass(obj), "doubleValue", "()D")} followed by
     * {@code CallDoubleMethod} computed it, or {@code null} when the class has no instance
     * {@code double doubleValue()} that can be reached.
     *
     * @param type the runtime class of the object, which must not be a {@link Number}; a Number
     *        converts through {@link Number#doubleValue()}, which is the same method
     * @return a handle of type {@code (Object)double}, or {@code null}
     */
    static MethodHandle doubleValueAccessor(Class<?> type) {
        return DOUBLE_VALUE.get(type);
    }

    // ------------------------------------------------------------------------ resolve_method

    /*
     * The first public method whose name and descriptor both match. Matching the return type is not
     * optional: a covariant override gives a class two methods with one name and one parameter
     * list, the real one and the compiler's bridge, and picking the bridge changes what invoke
     * returns.
     */
    private static Method search(Class<?> type, Signature signature) {
        Method[] methods;
        try {
            methods = type.getMethods();
        } catch (LinkageError e) {
            return searchWithoutListing(type, signature);
        }
        for (Method method : methods) {
            if (method.getName().equals(signature.name())
                    && signature.descriptor().equals(descriptorOf(method))) {
                return method;
            }
        }
        return null;
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
     * getMethods() creates a Method for every public method of the class and its supertypes, so it
     * throws a LinkageError, typically NoClassDefFoundError, when any one of their signatures names
     * a class that is missing at run time. That is common for an optional dependency. GetMethodID
     * matched one name and descriptor and never failed that way. The method asked for most often is
     * toString(), behind every string conversion of a Java object, and it is almost never the one
     * with the missing type.
     *
     * So this asks the VM to resolve the one method, as GetMethodID did, through a public lookup.
     * findVirtual matches the name and the exact descriptor without listing anything, and the member
     * it resolves names the class that declares it. That class's own getMethods() then yields the
     * very Method the JNI code produced. When it cannot, because the declaring class is the one that
     * cannot be listed, objectDeclarationStandingFor decides whether Object's declaration may stand
     * in.
     *
     * A public lookup sees only public classes in packages exported to everyone. An object whose
     * class is not one of those, or whose method is declared in a class that is not, gets no answer
     * here where JNI had one. That is a narrowing, and FFM-ABI-CONTRACT.md section 13.3 records it.
     */
    private static Method searchWithoutListing(Class<?> type, Signature signature) {
        MethodType methodType;
        Class<?> declaring;
        try {
            methodType = MethodType.fromMethodDescriptorString(signature.descriptor(),
                    type.getClassLoader());
            MethodHandles.Lookup lookup = MethodHandles.publicLookup();
            declaring = lookup.revealDirect(lookup.findVirtual(type, signature.name(), methodType))
                    .getDeclaringClass();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // No such public method, a class the public lookup cannot see, or a descriptor that
            // names a missing type as well: no answer, as for a class without the method.
            return null;
        }
        if (declaring != type) {
            Method exact = resolveMethod(declaring, signature.name(), signature.descriptor());
            if (exact != null && exact.getDeclaringClass() == declaring) {
                return exact;
            }
        }
        return objectDeclarationStandingFor(declaring, signature.name(), methodType);
    }

    /*
     * The one substitution this class makes. When the class that declares the method is the one
     * whose methods cannot be listed, Java has no way to obtain a Method for that declaration: JNI's
     * ToReflectedMethod built one from the resolved method alone, while getMethods,
     * getDeclaredMethod(s) and MethodHandles.reflectAs all list the class first. For a public
     * method that java.lang.Object declares and `declaring` overrides (toString, hashCode or equals)
     * Object's declaration, invoked on the object, dispatches to the override, so the same code runs
     * and returns the same value.
     *
     * What would differ is the check. Utilities.fwkInvokeWithContext and MethodHelper judge a call
     * by method.getDeclaringClass(), which would be Object instead of `declaring`. So the stand-in
     * is returned only when every check that they, and Method.invoke after them, would have made on
     * the Method the JNI code produced passes too: the allow list for `declaring`, a public class in
     * a package exported to everyone, and a public method. The public lookup has already required
     * the last two, since findVirtual answers only a public method and revealDirect refuses a
     * declaring class it cannot see; the class check is repeated here because the substitution is
     * sound only with it. When any of them fails there is no answer. The JNI build threw into the
     * script in that case, and no answer is the narrower of the two outcomes.
     */
    private static Method objectDeclarationStandingFor(Class<?> declaring, String name,
                                                       MethodType methodType) {
        Method objectMethod;
        try {
            objectMethod = Object.class.getMethod(name, methodType.parameterArray());
        } catch (NoSuchMethodException e) {
            return null;
        }
        if (objectMethod.getReturnType() != methodType.returnType()
                || Modifier.isFinal(objectMethod.getModifiers())
                || declaring.isInterface()
                || !Utilities.isInvocationPermitted(declaring, name)
                || !isPublicEverywhere(declaring)) {
            return null;
        }
        return objectMethod;
    }

    /* A public class in a package its module exports to everyone. */
    private static boolean isPublicEverywhere(Class<?> type) {
        try {
            MethodHandles.publicLookup().accessClass(type);
            return true;
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------------- doubleValue

    /*
     * GetMethodID's search for an instance method `returnType name()`, then the nearest thing to
     * CallDoubleMethod's disregard for access that reflection offers.
     *
     * The search is GetMethodID's. It takes the first declaration of that name and descriptor in the
     * class and then its superclasses, whatever its access; a static one there is a failure, as it
     * was for GetMethodID. Only when the class chain has none does it look at the interfaces, where
     * a default method may supply it.
     *
     * The call goes through the first of these declarations that trySetAccessible admits: the one
     * found, then, when that one is public and so overrides every public declaration of the method
     * above it, those public declarations in its superclasses and interfaces. Invoking any of them
     * on the object dispatches to the implementation CallDoubleMethod reached, so the result is the
     * same. trySetAccessible admits a public method of a public class in a package exported to
     * javafx.web, and any method in a package open to it, which includes every class on the class
     * path.
     *
     * What JNI reached and this does not: a non-public doubleValue() in a package that is not open
     * to javafx.web, and a public one declared only in classes and interfaces that are neither
     * exported nor open to it. Such an object converts to 0, as an object without the method
     * always did.
     */
    private static MethodHandle accessor(Class<?> type, String name, Class<?> returnType) {
        Method found = null;
        for (Class<?> c = type; c != null && found == null; c = c.getSuperclass()) {
            found = declaredAccessor(c, name, returnType);
        }
        List<Method> candidates = new ArrayList<>();
        if (found != null) {
            if (Modifier.isStatic(found.getModifiers())) {
                return null;
            }
            candidates.add(found);
        }
        if (found == null || Modifier.isPublic(found.getModifiers())) {
            Class<?> above = found == null ? null : found.getDeclaringClass().getSuperclass();
            for (Class<?> c = above; c != null; c = c.getSuperclass()) {
                addPublicInstance(candidates, declaredAccessor(c, name, returnType));
            }
            for (Class<?> anInterface : interfacesOf(type)) {
                addPublicInstance(candidates, declaredAccessor(anInterface, name, returnType));
            }
        }
        MethodType handleType = MethodType.methodType(returnType, Object.class);
        for (Method candidate : candidates) {
            if (candidate.trySetAccessible()) {
                try {
                    return MethodHandles.lookup().unreflect(candidate).asType(handleType);
                } catch (IllegalAccessException e) {
                    // Not expected for an accessible Method; the next declaration may still do.
                }
            }
        }
        return null;
    }

    /*
     * The no-argument method `name` with exactly this return type that `c` itself declares, of any
     * access, or null. A class whose declared methods cannot be listed, because one of them names a
     * missing type, is passed over, where GetMethodID resolved the one method in it. That keeps
     * parity only in part. A public or package-private override such a class declares still runs,
     * because the declaration it overrides further up, or in an interface, dispatches to it; but
     * only when there is one and trySetAccessible admits it, and otherwise there is no answer. A
     * private or static declaration in such a class is not seen at all. GetMethodID took it, and
     * called the private one or failed on the static one, while this search goes on up and may
     * call a declaration JNI did not: a private one, or a package-private one in another package.
     * Reflection cannot resolve one method without listing its class and still ignore access, so
     * FFM-ABI-CONTRACT.md section 13.3 records the difference rather than closing it.
     */
    private static Method declaredAccessor(Class<?> c, String name, Class<?> returnType) {
        Method[] methods;
        try {
            methods = c.getDeclaredMethods();
        } catch (LinkageError e) {
            return null;
        }
        for (Method method : methods) {
            if (method.getName().equals(name) && method.getParameterCount() == 0
                    && method.getReturnType() == returnType) {
                return method;
            }
        }
        return null;
    }

    private static void addPublicInstance(List<Method> candidates, Method method) {
        if (method != null && Modifier.isPublic(method.getModifiers())
                && !Modifier.isStatic(method.getModifiers())) {
            candidates.add(method);
        }
    }

    /* Every interface `type` implements, directly or through a superclass or superinterface. */
    private static Set<Class<?>> interfacesOf(Class<?> type) {
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        Deque<Class<?>> pending = new ArrayDeque<>();
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            pending.addAll(List.of(c.getInterfaces()));
        }
        while (!pending.isEmpty()) {
            Class<?> anInterface = pending.removeFirst();
            if (interfaces.add(anInterface)) {
                pending.addAll(List.of(anInterface.getInterfaces()));
            }
        }
        return interfaces;
    }
}
