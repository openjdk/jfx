/*
 * Copyright (c) 2010, 2022, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.fxml.builder;

import com.sun.javafx.fxml.BeanAdapter;
import com.sun.javafx.fxml.ModuleHelper;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javafx.beans.NamedArg;
import javafx.util.Builder;
import com.sun.javafx.reflect.ConstructorUtil;
import com.sun.javafx.reflect.ReflectUtil;

/**
 * Using this builder assumes that some of the constructors of desired class
 * with arguments are annotated with NamedArg annotation.
 */
public class ProxyBuilder<T> extends AbstractMap<String, Object> implements Builder<T> {

    private Class<?> type;

    private final Map<Constructor, Map<String, AnnotationValue>> constructorsMap;
    private final Map<String, Property> propertiesMap;
    private final Set<Constructor> constructors;
    private Set<String> propertyNames;

    private boolean hasDefaultConstructor = false;
    private Constructor defaultConstructor;

    private static final String SETTER_PREFIX = "set";
    private static final String GETTER_PREFIX = "get";

    public ProxyBuilder(Class<?> tp) {
        this.type = tp;

        constructorsMap = new HashMap<>();
        Constructor ctors[] = ConstructorUtil.getConstructors(type);

        for (Constructor c : ctors) {
            Map<String, AnnotationValue> args;
            Class<?> paramTypes[] = c.getParameterTypes();
            Annotation[][] paramAnnotations = c.getParameterAnnotations();

            // probably default constructor
            if (paramTypes.length == 0) {
                hasDefaultConstructor = true;
                defaultConstructor = c;
            } else { // constructor with parameters
                int i = 0;
                boolean properlyAnnotated = true;
                args = new LinkedHashMap<>();
                for (Class<?> clazz : paramTypes) {
                    NamedArg argAnnotation = null;
                    for (Annotation annotation : paramAnnotations[i]) {
                        if (annotation instanceof NamedArg) {
                            argAnnotation = (NamedArg) annotation;
                            break;
                        }
                    }

                    if (argAnnotation != null) {
                        AnnotationValue av = new AnnotationValue(
                                argAnnotation.value(),
                                argAnnotation.defaultValue(),
                                clazz);
                        args.put(argAnnotation.value(), av);
                    } else {
                        properlyAnnotated = false;
                        break;
                    }
                    i++;
                }
                if (properlyAnnotated) {
                    constructorsMap.put(c, args);
                }
            }
        }

        if (!hasDefaultConstructor && constructorsMap.isEmpty()) {
            throw new RuntimeException("Cannot create instance of "
                    + type.getCanonicalName()
                    + " the constructor is not properly annotated.");
        }

        constructors = new TreeSet<>(constructorComparator);
        constructors.addAll(constructorsMap.keySet());
        propertiesMap = scanForSetters();
    }

    //make sure int goes before float
    private final Comparator<Constructor> constructorComparator
            = (Constructor o1, Constructor o2) -> {
                int len1 = o1.getParameterCount();
                int len2 = o2.getParameterCount();
                int lim = Math.min(len1, len2);
                for (int i = 0; i < lim; i++) {
                    Class c1 = o1.getParameterTypes()[i];
                    Class c2 = o2.getParameterTypes()[i];
                    if (c1.equals(c2)) {
                        continue;
                    }
                    if (c1.equals(Integer.TYPE) && c2.equals(Double.TYPE)) {
                        return -1;
                    }
                    if (c1.equals(Double.TYPE) && c2.equals(Integer.TYPE)) {
                        return 1;
                    }
                    return c1.getCanonicalName().compareTo(c2.getCanonicalName());
                }
                return len1 - len2;
            };
    private final Map<String, Object> userValues = new HashMap<>();

    @Override
    public Object put(String key, Object value) {
        userValues.put(key, value);
        return null; // to behave the same way as ObjectBuilder does
    }

    private final Map<String, Object> containers = new HashMap<>();

    /**
     * This is used to support read-only collection property. This method must
     * return a Collection of the appropriate type if 1. the property is
     * read-only, and 2. the property is a collection. It must return null
     * otherwise.
     *
     */
    private Object getTemporaryContainer(String propName) {
        Object o = containers.get(propName);
        if (o == null) {
            o = getReadOnlyProperty(propName);
            if (o != null) {
                containers.put(propName, o);
            }
        }
        return o;
    }

    // Wrapper for ArrayList which we use to store read-only collection
    // properties in
    private static class ArrayListWrapper<T> extends ArrayList<T> {

    }

    // This is used to support read-only collection property.
    private Object getReadOnlyProperty(String propName) {
        // return ArrayListWrapper now and convert it to proper type later
        // during the build - once we know which constructor we will use
        // and what types it accepts
        return new ArrayListWrapper<>();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        return (getTemporaryContainer(key.toString()) != null);
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object get(Object key) {
        return getTemporaryContainer(key.toString());
    }

    @Override
    public T build() {
        Object retObj = null;
        // adding collection properties to userValues
        for (Entry<String, Object> entry : containers.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }

        propertyNames = userValues.keySet();

        for (Constructor c : constructors) {
            Set<String> argumentNames = getArgumentNames(c);

            // the object is created only if attributes from fxml exactly match constructor arguments
            if (propertyNames.equals(argumentNames)) {
                retObj = createObjectWithExactArguments(c, argumentNames);
                if (retObj != null) {
                    return (T) retObj;
                }
            }
        }

        // constructor with exact match doesn't exist
        Set<String> settersArgs = propertiesMap.keySet();

        // check if all properties can be set by setters and class has default constructor
        if (settersArgs.containsAll(propertyNames) && hasDefaultConstructor) {
            retObj = createObjectFromDefaultConstructor();
            if (retObj != null) {
                return (T) retObj;
            }
        }

        // set of mutable properties which are given by the user in fxml
        Set<String> propertiesToSet = new HashSet<>(propertyNames);
        propertiesToSet.retainAll(settersArgs);

        // will search for combination of constructor and setters
        Set<Constructor> chosenConstructors = chooseBestConstructors(settersArgs);

        // we have chosen the best constructors, let's try to find one we can use
        for (Constructor constructor : chosenConstructors) {
            retObj = createObjectFromConstructor(constructor, propertiesToSet);
            if (retObj != null) {
                return (T) retObj;
            }
        }

        if (retObj == null) {
            throw new RuntimeException("Cannot create instance of "
                    + type.getCanonicalName() + " with given set of properties: "
                    + userValues.keySet().toString());
        }

        return (T) retObj;
    }

    private Set<Constructor> chooseBestConstructors(Set<String> settersArgs) {
        // set of immutable properties which are given by the user in fxml
        Set<String> immutablesToSet = new HashSet<>(propertyNames);
        immutablesToSet.removeAll(settersArgs);

        // set of mutable properties which are given by the user in fxml
        Set<String> propertiesToSet = new HashSet<>(propertyNames);
        propertiesToSet.retainAll(settersArgs);

        int propertiesToSetCount = Integer.MAX_VALUE;
        int mutablesToSetCount = Integer.MAX_VALUE;

        // there may be more constructor with the same argument names
        // (this often happens in case of List<T> and T... etc.
        Set<Constructor> chosenConstructors = new TreeSet<>(constructorComparator);
        Set<String> argsNotSet = null;
        for (Constructor c : constructors) {
            Set<String> argumentNames = getArgumentNames(c);

            // check whether this constructor takes all immutable properties
            // given by the user; if not, skip it
            if (!argumentNames.containsAll(immutablesToSet)) {
                continue;
            }

            // all properties of this constructor which the user didn't
            // specify in FXML
            // we try to minimize this set
            Set<String> propertiesToSetInConstructor = new HashSet<>(argumentNames);
            propertiesToSetInConstructor.removeAll(propertyNames);

            // all mutable properties which the user did specify in FXML
            // but are not settable with this constructor
            // we try to minimize this too (but only if we have more constructors with
            // the same propertiesToSetCount)
            Set<String> mutablesNotSet = new HashSet<>(propertiesToSet);
            mutablesNotSet.removeAll(argumentNames);

            int currentPropSize = propertiesToSetInConstructor.size();
            if (propertiesToSetCount == currentPropSize
                    && mutablesToSetCount == mutablesNotSet.size()) {
                // we found constructor which is as good as the ones we already have
                chosenConstructors.add(c);
            }

            if (propertiesToSetCount > currentPropSize
                    || (propertiesToSetCount == currentPropSize && mutablesToSetCount > mutablesNotSet.size())) {
                propertiesToSetCount = currentPropSize;
                mutablesToSetCount = mutablesNotSet.size();
                chosenConstructors.clear();
                chosenConstructors.add(c);
            }
        }

        if (argsNotSet != null && !argsNotSet.isEmpty()) {
            throw new RuntimeException("Cannot create instance of "
                    + type.getCanonicalName()
                    + " no constructor contains all properties specified in FXML.");
        }

        return chosenConstructors;
    }

    // Returns argument names for given constructor
    private Set<String> getArgumentNames(Constructor c) {
        Map<String, AnnotationValue> constructorArgsMap = constructorsMap.get(c);
        Set<String> argumentNames = null;
        if (constructorArgsMap != null) {
            argumentNames = constructorArgsMap.keySet();
        }
        return argumentNames;
    }

    private Object createObjectFromDefaultConstructor() throws RuntimeException {
        Object retObj = null;

        // create class with default constructor and iterate over all required setters
        try {
            retObj = createInstance(defaultConstructor, new Object[]{});
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        for (String propName : propertyNames) {
            try {
                Property property = propertiesMap.get(propName);
                property.invoke(retObj, getUserValue(propName, property.getType()));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }

        return retObj;
    }

    private Object createObjectFromConstructor(Constructor constructor, Set<String> propertiesToSet) {
        Object retObj = null;
        Map<String, AnnotationValue> constructorArgsMap = constructorsMap.get(constructor);
        Object argsForConstruction[] = new Object[constructorArgsMap.size()];
        int i = 0;

        // set of properties which need to be set by setters if we use current
        // constructor
        Set<String> currentPropertiesToSet = new HashSet<>(propertiesToSet);
        for (AnnotationValue value : constructorArgsMap.values()) {
            // first try to coerce user give value
            Object userValue = getUserValue(value.getName(), value.getType());
            if (userValue != null) {
                try {
                    argsForConstruction[i] = BeanAdapter.coerce(userValue, value.getType());
                } catch (Exception ex) {
                    return null;
                }
            } else {
                // trying to coerce default value
                if (!value.getDefaultValue().isEmpty()) {
                    try {
                        argsForConstruction[i] = BeanAdapter.coerce(value.getDefaultValue(), value.getType());
                    } catch (Exception ex) {
                        return null;
                    }
                } else {
                    argsForConstruction[i] = getDefaultValue(value.getType());
                }
            }
            currentPropertiesToSet.remove(value.getName());
            i++;
        }

        try {
            retObj = createInstance(constructor, argsForConstruction);
        } catch (Exception ex) {
            // try next constructor
        }

        if (retObj != null) {
            for (String propName : currentPropertiesToSet) {
                try {
                    Property property = propertiesMap.get(propName);
                    property.invoke(retObj, getUserValue(propName, property.getType()));
                } catch (Exception ex) {
                    // try next constructor
                    return null;
                }
            }
        }

        return retObj;
    }

    private Object getUserValue(String key, Class<?> type) {
        Object val = userValues.get(key);
        if (val == null) {
            return null;
        }

        if (type.isAssignableFrom(val.getClass())) {
            return val;
        }

        // we currently don't have proper support support for arrays
        // in FXML so we use lists instead
        // the user provides us with a list and here we convert it to
        // array to pass to the constructor
        if (type.isArray()) {
            try {
                return convertListToArray(val, type);
            } catch (RuntimeException ex) {
                // conversion failed, maybe the ArrayListWrapper is
                // used for storing single value
            }
        }

        if (ArrayListWrapper.class.equals(val.getClass())) {
            // user given value is an ArrayList but the constructor doesn't
            // accept an ArrayList so the ArrayList comes from
            // the getTemporaryContainer method
            // we take the first argument
            List l = (List) val;
            return l.get(0);
        }

        return val;
    }

    private Object createObjectWithExactArguments(Constructor c, Set<String> argumentNames) {
        Object retObj = null;
        Object argsForConstruction[] = new Object[argumentNames.size()];
        Map<String, AnnotationValue> constructorArgsMap = constructorsMap.get(c);

        int i = 0;

        for (String arg : argumentNames) {
            Class<?> tp = constructorArgsMap.get(arg).getType();
            Object value = getUserValue(arg, tp);
            try {
                argsForConstruction[i++] = BeanAdapter.coerce(value, tp);
            } catch (Exception ex) {
                return null;
            }
        }

        try {
            retObj = createInstance(c, argsForConstruction);
        } catch (Exception ex) {
            // will try to fall back to different constructor
        }

        return retObj;
    }

    private Object createInstance(Constructor c, Object args[]) throws Exception {
        Object retObj = null;

        ReflectUtil.checkPackageAccess(type);
        retObj = c.newInstance(args);

        return retObj;
    }

    private Map<String, Property> scanForSetters() {
        Map<String, Property> strsMap = new HashMap<>();
        Map<String, LinkedList<Method>> methods = getClassMethodCache(type);

        for (String methodName : methods.keySet()) {
            if (methodName.startsWith(SETTER_PREFIX) && methodName.length() > SETTER_PREFIX.length()) {
                String propName = methodName.substring(SETTER_PREFIX.length());
                propName = Character.toLowerCase(propName.charAt(0)) + propName.substring(1);
                List<Method> methodsList = methods.get(methodName);
                for (Method m : methodsList) {
                    Class<?> retType = m.getReturnType();
                    Class<?> argType[] = m.getParameterTypes();
                    if (retType.equals(Void.TYPE) && argType.length == 1) {
                        strsMap.put(propName, new Setter(m, argType[0]));
                    }
                }
            }
            if (methodName.startsWith(GETTER_PREFIX) && methodName.length() > GETTER_PREFIX.length()) {
                String propName = methodName.substring(GETTER_PREFIX.length());
                propName = Character.toLowerCase(propName.charAt(0)) + propName.substring(1);
                List<Method> methodsList = methods.get(methodName);
                for (Method m : methodsList) {
                    Class<?> retType = m.getReturnType();
                    Class<?> argType[] = m.getParameterTypes();
                    if (Collection.class.isAssignableFrom(retType) && argType.length == 0) {
                        strsMap.put(propName, new Getter(m, retType));
                    }
                }
            }
        }

        return strsMap;
    }

    private static abstract class Property {
        protected final Method method;
        protected final Class<?> type;

        public Property(Method m, Class<?> t) {
            method = m;
            type = t;
        }

        public Class<?> getType() {
            return type;
        }

        public abstract void invoke(Object obj, Object argStr) throws Exception;
    }

    private static class Setter extends Property {

        public Setter(Method m, Class<?> t) {
            super(m, t);
        }

        @Override
        public void invoke(Object obj, Object argStr) throws Exception {
            Object arg[] = new Object[]{BeanAdapter.coerce(argStr, type)};
            ModuleHelper.invoke(method, obj, arg);
        }
    }

    private static class Getter extends Property {

        public Getter(Method m, Class<?> t) {
            super(m, t);
        }

        @Override
        public void invoke(Object obj, Object argStr) throws Exception {
            // we know that this.method returns collection otherwise it wouldn't be here
            Collection to = (Collection) ModuleHelper.invoke(method, obj, new Object[]{});
            if (argStr instanceof Collection) {
                Collection from = (Collection) argStr;
                to.addAll(from);
            } else {
                to.add(argStr);
            }
        }
    }

    // This class holds information for one argument of the constructor
    // which we got from the NamedArg annotation
    private static class AnnotationValue {

        private final String name;
        private final String defaultValue;
        private final Class<?> type;

        public AnnotationValue(String name, String defaultValue, Class<?> type) {
            this.name = name;
            this.defaultValue = defaultValue;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public Class<?> getType() {
            return type;
        }
    }

    private static HashMap<String, LinkedList<Method>> getClassMethodCache(Class<?> type) {
        HashMap<String, LinkedList<Method>> classMethodCache = new HashMap<>();

        ReflectUtil.checkPackageAccess(type);

        Method[] declaredMethods = type.getMethods();
        for (Method method : declaredMethods) {
            int modifiers = method.getModifiers();

            if (Modifier.isPublic(modifiers) && !Modifier.isStatic(modifiers)) {
                String name = method.getName();
                LinkedList<Method> namedMethods = classMethodCache.get(name);

                if (namedMethods == null) {
                    namedMethods = new LinkedList<>();
                    classMethodCache.put(name, namedMethods);
                }

                namedMethods.add(method);
            }
        }

        return classMethodCache;
    }

    // Utility method for converting list to array via reflection
    // it assumes that localType is array
    private static Object[] convertListToArray(Object userValue, Class<?> localType) {
        Class<?> arrayType = localType.getComponentType();
        List l = BeanAdapter.coerce(userValue, List.class);

        return l.toArray((Object[]) Array.newInstance(arrayType, 0));
    }

    private static Object getDefaultValue(Class clazz) {
        return DEFAULTS_MAP.get(clazz);
    }

    private static final Map<Class<?>, Object> DEFAULTS_MAP = new HashMap<>(9);
    static {
        DEFAULTS_MAP.put(byte.class,    (byte) 0);
        DEFAULTS_MAP.put(short.class,   (short) 0);
        DEFAULTS_MAP.put(int.class,     0);
        DEFAULTS_MAP.put(long.class,    0L);
        DEFAULTS_MAP.put(float.class,   0.0f);
        DEFAULTS_MAP.put(double.class,  0.0d);
        DEFAULTS_MAP.put(char.class,   '\u0000');
        DEFAULTS_MAP.put(boolean.class, false);
        DEFAULTS_MAP.put(Object.class,  null);
    }
}
