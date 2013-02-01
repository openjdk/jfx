/*
 * Copyright (c) 2010, 2012, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.fxml.BeanAdapter;
import com.sun.javafx.fxml.builder.JavaFXFontBuilder;
import com.sun.javafx.fxml.builder.JavaFXImageBuilder;
import com.sun.javafx.fxml.builder.JavaFXSceneBuilder;
import com.sun.javafx.fxml.builder.URLBuilder;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.scene.web.WebView;
import javafx.util.Builder;
import javafx.util.BuilderFactory;
import sun.reflect.misc.ConstructorUtil;
import sun.reflect.misc.MethodUtil;

/**
 * JavaFX builder factory.
 */
public final class JavaFXBuilderFactory implements BuilderFactory {
    private final JavaFXBuilder NO_BUILDER = new JavaFXBuilder();

    private final Map<Class<?>, JavaFXBuilder> builders = new HashMap<Class<?>, JavaFXBuilder>();

    private final ClassLoader classLoader;
    private final boolean alwaysUseBuilders;

    /**
     * Default constructor.
     */
    public JavaFXBuilderFactory() {
        this(FXMLLoader.getDefaultClassLoader(), false);
    }

    /**
     * @treatAsPrivate
     * This constructor is for internal use only.
     *
     * @deprecated
     */
    public JavaFXBuilderFactory(boolean alwaysUseBuilders) {
        // SB-dependency: RT-21230 has been filed to track this
        this(FXMLLoader.getDefaultClassLoader(), alwaysUseBuilders);
    }

    /**
     * Constructor that takes a class loader.
     *
     * @param classLoader
     */
    public JavaFXBuilderFactory(ClassLoader classLoader) {
        this(classLoader, false);
    }

    /**
     * @treatAsPrivate
     * This constructor is for internal use only.
     *
     * @deprecated
     */
    public JavaFXBuilderFactory(ClassLoader classLoader, boolean alwaysUseBuilders) {
        // SB-dependency: RT-21230 has been filed to track this
        if (classLoader == null) {
            throw new NullPointerException();
        }

        this.classLoader = classLoader;
        this.alwaysUseBuilders = alwaysUseBuilders;
    }

    @Override
    public Builder<?> getBuilder(Class<?> type) {
        Builder<?> builder;

        if (type == Scene.class) {
            builder = new JavaFXSceneBuilder();
        } else if (type == Font.class) {
            builder = new JavaFXFontBuilder();
        } else if (type == Image.class) {
            builder = new JavaFXImageBuilder();
        } else if (type == URL.class) {
            builder = new URLBuilder(classLoader);
        } else {
            Builder<Object> objectBuilder = null;
            JavaFXBuilder typeBuilder = builders.get(type);

            if (typeBuilder != NO_BUILDER) {
                if (typeBuilder == null) {
                    // We want to retun a builder here
                    // only for those classes that reqire it.
                    // For now we assume that an object that has a default
                    // constructor does not require a builder. This is the case
                    // for most platform classes, except those handled above.
                    // We may need to add other exceptions to the rule if the need
                    // arises...
                    //
                    boolean hasDefaultConstructor;
                    try {
                        ConstructorUtil.getConstructor(type, new Class[] {});
                        // found!
                        // forces the factory  to return a builder if there is one.
                        // TODO: delete the line below when we are sure that both
                        //       builders and default constructors are working!
                        if (alwaysUseBuilders) throw new Exception();

                        hasDefaultConstructor = true;
                    } catch (Exception x) {
                        hasDefaultConstructor = false;
                    }

                    // Force the loader to use a builder for WebView even though
                    // it defines a default constructor
                    if (!hasDefaultConstructor || type == WebView.class) {
                        try {
                            typeBuilder = createTypeBuilder(type);
                        } catch (ClassNotFoundException ex) {
                            // no builder... will fail later when the FXMLLoader
                            // will try to instantiate the bean...
                        }
                    }

                    builders.put(type, typeBuilder == null ? NO_BUILDER : typeBuilder);
                }
                if (typeBuilder != null) {
                    objectBuilder = typeBuilder.createBuilder();
                }
            }

            builder = objectBuilder;
        }

        return builder;
    }

    JavaFXBuilder createTypeBuilder(Class<?> type) throws ClassNotFoundException {
        JavaFXBuilder typeBuilder = null;
        Class<?> builderClass = classLoader.loadClass(type.getName() + "Builder");
        try {
            typeBuilder = new JavaFXBuilder(builderClass);
        } catch (Exception ex) {
            //TODO should be reported
            Logger.getLogger(JavaFXBuilderFactory.class.getName()).
                    log(Level.WARNING, "Failed to instantiate JavaFXBuilder for " + builderClass, ex);
        }
        if (!alwaysUseBuilders) {
            Logger.getLogger(JavaFXBuilderFactory.class.getName()).
                    log(Level.FINER, "class {0} requires a builder.", type);
        }
        return typeBuilder;
    }
}

/**
 * JavaFX builder.
 */
final class JavaFXBuilder {
    private static final Object[]   NO_ARGS = {};
    private static final Class<?>[] NO_SIG = {};

    private final Class<?>           builderClass;
    private final Method             createMethod;
    private final Method             buildMethod;
    private final Map<String,Method> methods = new HashMap<String, Method>();
    private final Map<String,Method> getters = new HashMap<String,Method>();
    private final Map<String,Method> setters = new HashMap<String,Method>();

    final class ObjectBuilder extends AbstractMap<String, Object> implements Builder<Object> {
        private final Map<String,Object> containers = new HashMap<String,Object>();
        private Object                   builder = null;
        private Map<Object,Object>       properties;

        private ObjectBuilder() {
            try {
                builder = MethodUtil.invoke(createMethod, null, NO_ARGS);
            } catch (Exception e) {
                //TODO
                throw new RuntimeException("Creation of the builder " + builderClass.getName() + " failed.", e);
            }
        }

        @Override
        public Object build() {
            for (Iterator<Entry<String,Object>> iter = containers.entrySet().iterator(); iter.hasNext(); ) {
                Entry<String, Object> entry = iter.next();

                put(entry.getKey(), entry.getValue());
            }

            Object res;
            try {
                res = MethodUtil.invoke(buildMethod, builder, NO_ARGS);
                // TODO:
                // temporary special case for Node properties until
                // platform builders are fixed
                if (properties != null && res instanceof Node) {
                    ((Map<Object, Object>)((Node)res).getProperties()).putAll(properties);
                }
            } catch (InvocationTargetException exception) {
                throw new RuntimeException(exception);
            } catch (IllegalAccessException exception) {
                throw new RuntimeException(exception);
            } finally {
                builder = null;
            }

            return res;
        }

        @Override
        public int size() {
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
        @SuppressWarnings("unchecked")
        public Object put(String key, Object value) {
            // TODO:
            // temporary hack: builders don't have a method for properties...
            if (Node.class.isAssignableFrom(getTargetClass()) && "properties".equals(key)) {
                properties = (Map<Object,Object>) value;
                return null;
            }
            try {
                Method m = methods.get(key);
                if (m == null) {
                    m = findMethod(key);
                    methods.put(key, m);
                }
                try {
                    final Class<?> type = m.getParameterTypes()[0];

                    // If the type is an Array, and our value is a list,
                    // we simply convert the list into an array. Otherwise,
                    // we treat the value as a string and split it into a
                    // list using the array component delimiter.
                    if (type.isArray()) {
                        final List<?> list;
                        if (value instanceof List) {
                            list = (List<?>)value;
                        } else {
                            list = Arrays.asList(value.toString().split(FXMLLoader.ARRAY_COMPONENT_DELIMITER));
                        }

                        final Class<?> componentType = type.getComponentType();
                        Object array = Array.newInstance(componentType, list.size());
                        for (int i=0; i<list.size(); i++) {
                            Array.set(array, i, BeanAdapter.coerce(list.get(i), componentType));
                        }
                        value = array;
                    }

                    MethodUtil.invoke(m, builder, new Object[] { BeanAdapter.coerce(value, type) });
                } catch (Exception e) {
                    Logger.getLogger(JavaFXBuilder.class.getName()).log(Level.WARNING,
                        "Method " + m.getName() + " failed", e);
                }
                //TODO Is it OK to return null here?
                return null;
            } catch (Exception e) {
                //TODO Should be reported
                Logger.getLogger(JavaFXBuilder.class.getName()).log(Level.WARNING,
                        "Failed to set "+getTargetClass()+"."+key+" using "+builderClass, e);
                return null;
            }
        }

        // Should do this in BeanAdapter?
        // This is used to support read-only collection property.
        // This method must return a Collection of the appropriate type
        // if 1. the property is read-only, and 2. the property is a collection.
        // It must return null otherwise.
        Object getReadOnlyProperty(String propName) {
            if (setters.get(propName) != null) return null;
            Method getter = getters.get(propName);
            if (getter == null) {
                Method setter = null;
                Class<?> target = getTargetClass();
                String suffix = Character.toUpperCase(propName.charAt(0)) + propName.substring(1);
                try {
                    getter = MethodUtil.getMethod(target, "get"+ suffix, NO_SIG);
                    setter = MethodUtil.getMethod(target, "set"+ suffix, new Class[] { getter.getReturnType() });
                } catch (Exception x) {
                }
                if (getter != null) {
                    getters.put(propName, getter);
                    setters.put(propName, setter);
                }
                if (setter != null) return null;
            }

            Class<?> type;
            if (getter == null) {
                // if we have found no getter it might be a constructor property
                // try to get the type from the builder method.
                final Method m = findMethod(propName);
                if (m == null) {
                    return null;
                }
                type = m.getParameterTypes()[0];
                if (type.isArray()) type = List.class;
            } else {
                type = getter.getReturnType();
            }

            if (ObservableMap.class.isAssignableFrom(type)) {
                return FXCollections.observableMap(new HashMap<Object, Object>());
            } else if (Map.class.isAssignableFrom(type)) {
                return new HashMap<Object, Object>();
            } else if (ObservableList.class.isAssignableFrom(type)) {
                return FXCollections.observableArrayList();
            } else if (List.class.isAssignableFrom(type)) {
                return new ArrayList<Object>();
            } else if (Set.class.isAssignableFrom(type)) {
                return new HashSet<Object>();
            }
            return null;
        }

        /**
         * This is used to support read-only collection property.
         * This method must return a Collection of the appropriate type
         * if 1. the property is read-only, and 2. the property is a collection.
         * It must return null otherwise.
         **/
        public Object getTemporaryContainer(String propName) {
            Object o = containers.get(propName);
            if (o == null) {
                o = getReadOnlyProperty(propName);
                if (o != null) {
                    containers.put(propName, o);
                }
            }

            return o;
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends String, ? extends Object> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> keySet() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Collection<Object> values() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            throw new UnsupportedOperationException();
        }
    }

    JavaFXBuilder() {
        builderClass = null;
        createMethod = null;
        buildMethod = null;
    }

    JavaFXBuilder(Class<?> builderClass) throws NoSuchMethodException, InstantiationException, IllegalAccessException {
        this.builderClass = builderClass;
        createMethod = MethodUtil.getMethod(builderClass, "create", NO_SIG);
        buildMethod = MethodUtil.getMethod(builderClass, "build", NO_SIG);
        assert Modifier.isStatic(createMethod.getModifiers());
        assert !Modifier.isStatic(buildMethod.getModifiers());
    }

    Builder<Object> createBuilder() {
        return new ObjectBuilder();
    }

    private Method findMethod(String name) {
        if (name.length() > 1
            && Character.isUpperCase(name.charAt(1))) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        for (Method m : MethodUtil.getMethods(builderClass)) {
            if (m.getName().equals(name)) {
                return m;
            }
        }
        throw new IllegalArgumentException("Method " + name + " could not be found at class " + builderClass.getName());
    }

    /**
     * The type constructed by this builder.
     * @return The type constructed by this builder.
     */
    public Class<?> getTargetClass() {
        return buildMethod.getReturnType();
    }
}
