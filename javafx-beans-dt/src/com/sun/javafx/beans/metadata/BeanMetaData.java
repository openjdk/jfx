/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.javafx.beans.metadata;

import static com.sun.javafx.beans.metadata.Bean.COMPUTE;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javafx.beans.DefaultProperty;
import javafx.scene.image.Image;
import javafx.util.Builder;

/**
 * Represents the meta-data for a JavaFX Bean. A BeanMetaData is created by
 * invoking one of the constructors. These constructors reflect on a Class
 * representing a JavaFX Bean and extracts and/or generates the meta-data for
 * that class. Some information, such as the actual name of the bean, are
 * determined reflectively and cannot be customized. Most information however
 * can be customized either through the use of the {@link Bean} annotation,
 * or through the use of one or more {@link java.util.ResourceBundle}s.
 * <p>
 * During the reflective process, this class will look first for a resource
 * bundle named after the bean, and then for a generic "resources"
 * ResourceBundle located in the same package as the bean.
 * <p>
 * For example, suppose I had a bean named MyWidget. In the same package as
 * MyWidget there may be both a resources.properties, and a
 * MyWidgetResources.properties. When looking up the displayName of MyWidget,
 * BeanMetaData will first:
 *  <ol>
 *      <li>Check for a Bean annotation with a specified displayName.
 *          <ol>
 *              <li>If the displayName does not start with a % character and is
 *                  not {@link Bean.COMPUTE}, then take the value of displayName
 *                  as a literal value.</li>
 *              <li>If the displayName starts with a % character, then take the
 *                  displayName as the key for a corresponding value in the
 *                  resource bundles. First check the MyWidgetResources bundle,
 *                  and if there is no entry then check the resources
 *                  bundle.</li>
 *              <li>If the displayName is {@link Bean.COMPUTE}, then look in
 *                  the resource bundles (first in MyWidgetResources and then
 *                  in resources) for an entry MyWidget-displayName. If it is
 *                  not in either location, then attempt to synthesize a
 *                  displayName based on the class name.</li>
 *          </ol>
 *      </li>
 *      <li>If there is no annotation, then look for MyWidget-displayName first
 *          in MyWidgetResources, and then in resources. If it is not in either
 *          location, then attempt to synthesize a displayName based on the
 *          class name.</li>
 *  </ol>
 * <p>
 * Likewise, for other attributes, we follow the same lookup scheme but instead
 * of MyWidget-displayName we look for MyWidget-shortDescription, and so on.
 * <p>
 * The BeanMetaData also provides access to the PropertyMetaData and
 * EventMetaData and CallbackMetaData for the JavaFX Bean. If you are not
 * interested in the full BeanMetaData but only really want a specific
 * PropertyMetaData, then you can create a PropertyMetaData directly.
 * <p>
 * For a larger discussion on the JavaFX Beans design pattern, see the
 * package documentation in javafx.beans.
 *
 * TODO need to write this larger documentation for JavaFX Beans.
 *
 * @author Richard
 */
public final class BeanMetaData<T> extends MetaData {
    /**
     * This is used in the implementation to find the images of the specified
     * sizes on disk, and in storing them and retrieving them as needed.
     */
    private enum ImageSize {
        Size_16(16),
        Size_32(32),
        Size_64(64),
        Size_128(128),
        Size_256(256),
        Size_512(512),
        Size_Full(Integer.MAX_VALUE);

        private final int size;

        ImageSize(int size) {
            this.size = size;
        }

        public final String getExtension() {
            return size == Integer.MAX_VALUE ? "" : size + "x" + size;
        }

        public final int getSize() {
            return size;
        }
    }

    /**
     * The list of meta-data for properties.
     */
    private List<PropertyMetaData> properties;

    /**
     * The list of meta-data for events.
     */
    private List<EventMetaData> events;

    /**
     * The list of meta-data for callbacks.
     */
    private List<CallbackMetaData> callbacks;

    /**
     * A map containing the images that were discovered for this bean.
     */
    private Map<ImageSize,Image> images;

    /**
     * The class of the Builder, if any, which is associated with this
     * JavaFX Bean. If there is a builder, you should use it when constructing
     * an instance of this bean, rather than using the bean itself.
     */
    private Class<? extends Builder> builderClass;

    /**
     * The property which is designated with the DefaultProperty annotation
     * on the bean. This might be null.
     */
    private PropertyMetaData defaultProperty;

    /**
     * The type of the Bean that this BeanMetaData is for.
     */
    private Class<T> type;

    /**
     * Creates a new BeanMetaData instance based on the supplied class. This
     * constructor will introspect the supplied bean to discover (a) whether
     * it is a bean, and if not will throw an IllegalArgumentException; (b)
     * whether the bean has public access, and if not throw an
     * IllegalArgumentException; (c) the name, displayName, and so forth
     * associated with this bean based on annotations, resource bundles,
     * and direct computation where appropriate ; and (d) what the properties,
     * callbacks, and events are.
     *
     * @param beanClass The class to use
     */
    public BeanMetaData(final Class<T> beanClass) {
        if (beanClass == null) throw new NullPointerException(
                "beanClass cannot be null");

        this.type = beanClass;

        // Step 0: Verify that this is a JavaBean. It must have public access.
        //         It must either have a public no-arg constructor, or a
        //         Builder with a public static create() method and a public
        //         instance build() method.

        if (!Modifier.isPublic(beanClass.getModifiers())) {
            throw new IllegalArgumentException("The supplied bean '" +
                    beanClass + "' does not have public access");
        }

        // Look for a Builder class.
        String builderClassName = beanClass.getName() + "Builder";
        try {
            // TODO does this bail in an unsigned context?
            builderClass = (Class<? extends Builder>)Class.forName(builderClassName);
        } catch (ClassNotFoundException ex) {
            // There is no builder, this is an OK condition.
        } catch (ClassCastException ex) {
            // The builder wasn't of the expected type
        }

        // Verify that there is a public no-arg constructor, or that there
        // is a builder with both a public static create() method and a
        // public instance build() method.
        try {
            Constructor<T> noArgConstructor = beanClass.getConstructor();
            if (!Modifier.isPublic(noArgConstructor.getModifiers())) {
                throw new NoSuchMethodException();
            }
        } catch (NoSuchMethodException ex) {
            if (builderClass == null) {
                throw new IllegalArgumentException("The supplied bean '" +
                        beanClass + "' does not have a no-arg constructor");
            } else {
                // There is a builder, and it may be able to create the bean,
                // so we need to do a quick check to make sure it has a no-arg
                // static create() method and a no-arg instance method build().
                try {
                    Method method = builderClass.getMethod("create");
                    if (!Modifier.isStatic(method.getModifiers()) ||
                            !Modifier.isPublic(method.getModifiers())) {
                        throw new NoSuchMethodException();
                    }
                } catch (NoSuchMethodException ex2) {
//                    throw new IllegalArgumentException("The supplied bean '" +
//                            beanClass + "' does not have a builder with a "
//                            + "public no-arg static create() method");
                }

//                try {
//                    Method method = builderClass.getMethod("build");
//                    if (Modifier.isStatic(method.getModifiers()) ||
//                            !Modifier.isPublic(method.getModifiers())) {
//                        throw new NoSuchMethodException();
//                    }
//                } catch (NoSuchMethodException ex2) {
//                    throw new IllegalArgumentException("The supplied bean '" +
//                            beanClass + "' does not have a builder with a "
//                            + "no-arg instance build() method");
//                }
            }
        }

        // Step 0a: Look for and load the resource bundles associated with
        //          this bean. Look for a "resources" bundle in the same
        //          package as the class, and a "FooResources" bundle also
        //          in the same package as the class. The FooResources takes
        //          precedence over "resources" in the case of lookup
        Resources bundle = new Resources(beanClass);

        // Step 1: Discover the name of the bean. Assign this name to the
        //         name property. Create a proper display name based on it.
        //         Let the short description be an empty string. Default the
        //         category to an empty string.

        String _name = beanClass.getSimpleName();
        String _displayName = bundle.get("displayName", toDisplayName(_name));
        String _shortDescription = bundle.get("shortDescription", "");
        String _category = bundle.get("category", "");
        String _imageName = bundle.get("image", _name);

        // Step 2: Look for the @Bean annotation on the JavaBean. If it exists,
        //         then use the information defined there to override the
        //         information previously computed.

        Bean beanAnnotation = beanClass.getAnnotation(Bean.class);
        if (beanAnnotation != null) {
            String s = beanAnnotation.displayName();
            if (s != null && !COMPUTE.equals(s)) _displayName = s;
            s = beanAnnotation.shortDescription();
            if (s != null && !COMPUTE.equals(s)) _shortDescription = s;
            s = beanAnnotation.category();
            if (s != null && !COMPUTE.equals(s)) _category = s;
            s = beanAnnotation.image();
            if (s != null && !COMPUTE.equals(s)) _imageName = s;
        }

        // Step 2a: If the _displayName, _shortDescription, or _category is
        //          prefixed by % then lookup the new value from bundle
        if (_displayName.startsWith("%")) _displayName = bundle.get(_displayName, _displayName);
        if (_shortDescription.startsWith("%")) _shortDescription = bundle.get(_shortDescription, _shortDescription);
        if (_category.startsWith("%")) _category = bundle.get(_category, _category);
        if (_imageName.startsWith("%")) _imageName = bundle.get(_imageName, _imageName);

        // Step 2b: Update the BeanMetaData with these final values
        configure(_name, _displayName, _shortDescription, _category);

        // Step 2c: Lookup the images
        if (!"".equals(_imageName)) {
            for (ImageSize size : ImageSize.values()) {
                String fileName = _imageName + size.getExtension();
                InputStream jpg = beanClass.getResourceAsStream(fileName + ".jpg");
                InputStream png = beanClass.getResourceAsStream(fileName + ".png");

                if (png != null) {
                    images.put(size, new Image(png));
                } else if (jpg != null) {
                    images.put(size, new Image(jpg));
                }
            }
        }

        // Step 3: Find all properties, callbacks, and events. Because immutable
        //         properties only have a "getter" and no property method, and
        //         because we don't support properties which have a setter but
        //         no getter, we use the getter as the authoritative way to
        //         identify a property. If a property has a return type of
        //         Callback, then we have a Callback. Otherwise If the property
        //         name (as derived from the getter) starts with "on", then we
        //         have an event and create an EventMetaData. Otherwise we
        //         create a PropertyMetaData. While iterating, locate the
        //         property which matches the DefaultProperty annotation
        // Step 2d: Lookup the DefaultProperty
        final DefaultProperty defaultPropertyAnnotation =
                beanClass.getAnnotation(DefaultProperty.class);
        final String defaultPropertyName = defaultPropertyAnnotation == null ?
                "" : defaultPropertyAnnotation.value();
        Method[] methods = beanClass.getMethods();
        List<PropertyMetaData> p = new ArrayList<PropertyMetaData>();
        List<EventMetaData> e = new ArrayList<EventMetaData>();
        List<CallbackMetaData> c = new ArrayList<CallbackMetaData>();
        for (Method m : methods) {
            if (Modifier.isPublic(m.getModifiers()) &&
                    !Modifier.isStatic(m.getModifiers())) {
                final String mname = m.getName();
                final int paramCount = m.getParameterTypes().length;
                final Class<?> ret = m.getReturnType();
                if (mname.startsWith("get") && paramCount == 0) {
                    if (javafx.util.Callback.class.isAssignableFrom(ret)) {
                        c.add(new CallbackMetaData(beanClass, m, bundle));
                    } else if (mname.startsWith("getOn")) {
                        e.add(new EventMetaData(beanClass, m, bundle));
                    } else {
                        PropertyMetaData prop = new PropertyMetaData(beanClass, m, bundle);
                        p.add(prop);
                        if (defaultProperty == null &&
                                prop.getName().equals(defaultPropertyName)) {
                            defaultProperty = prop;
                        }
                    }
                } else if (mname.startsWith("is") && paramCount == 0 &&
                        (ret == boolean.class || ret == Boolean.class)) {
                    PropertyMetaData prop = new PropertyMetaData(beanClass, m, bundle);
                    p.add(prop);
                    if (defaultProperty == null &&
                            prop.getName().equals(defaultPropertyName)) {
                        defaultProperty = prop;
                    }
                }
            }
        }

        properties = Collections.unmodifiableList(p);
        events = Collections.unmodifiableList(e);
        callbacks = Collections.unmodifiableList(c);

        // Step 4: Find all other methods. These methods may be useful for
        //         various event handlers. As such it is useful to locate these
        //         methods. In all cases (for properties, events, and methods)
        //         we only find those API members which are public, such that
        //         we are not circumventing any security protocol.

        // TODO If we find this is really necessary
    }

    /**
     * Gets a reference to an unmodifiable list of PropertyMetaData, one for
     * each public property defined on the bean.
     *
     * @return an unmodifiable List of properties
     */
    public final List<PropertyMetaData> getProperties() {
        return properties;
    }

    /**
     * Gets a reference to an unmodifiable list of EventMetaData, one for
     * each public event defined on the bean.
     *
     * @return an unmodifiable List of events
     */
    public final List<EventMetaData> getEvents() {
        return events;
    }

    /**
     * Gets a reference to an unmodifiable list of CallbackMetaData, one for
     * each public callback defined on the bean.
     *
     * @return an unmodifiable List of callback
     */
    public final List<CallbackMetaData> getCallbacks() {
        return callbacks;
    }

    /**
     * Gets the class of the Builder, if any, which is associated with this
     * JavaFX Bean. If there is a builder, you should use it when constructing
     * an instance of this bean, rather than using the bean itself.
     *
     * @return The class of the Builder for this bean, or null if there isn't
     *         one
     */
    public final Class<? extends Builder> getBuilder() {
        return builderClass;
    }

    /**
     * Looks for the next-closest image to the one requested in this
     * BeanMetaData. The returned image may not match the given dimensions,
     * so the caller may want to ensure the requested dimensions are met
     * by specifying them on the ImageView which will display the returned
     * Image. If there is no Image equal to or larger than the requested
     * dimensions, then the next closest <em>smaller</em> sized image will
     * be returned. If there simply is no image available, then null is
     * returned.
     *
     * @param width The requested width of the image to look up
     * @param height The requested height of the image to look up
     * @return An image that most correctly matches the given width and
     *         height. First any larger image is found if an exact match
     *         cannot be, and then any smaller image is found. Null is
     *         ultimately returned if no image exists.
     */
    public final Image findImage(int width, int height) {
        // We might as well just get right to it. If there are no images,
        // then null is always returned.
        if (images.isEmpty()) return null;

        // Look for the image associated with a specific size which
        // is greater than or equal to the requested width and height.
        // We simply iterate over all ImageSize values and check the
        // map for any value that is greater than or equal to the
        // requested width and height.
        final ImageSize[] imageSizes = ImageSize.values();
        for (ImageSize imageSize : imageSizes) {
            final int size = imageSize.getSize();
            if (size >= width && size >= height) {
                // We found the best match, so just return it
                Image image = images.get(imageSize);
                if (image != null) return image;
            }
        }

        // Well, we didn't find an image bigger than the requested size, so we
        // now have to find the next closest smaller one.
        for (int i=imageSizes.length-1; i>=0; i--) {
            final ImageSize imageSize = imageSizes[i];
            final int size = imageSize.getSize();
            if (size <= width && size <= height) {
                // We found the best match, so just return it
                Image image = images.get(imageSize);
                if (image != null) return image;
            }
        }

        throw new AssertionError("This code should be unreachable");
    }

    /**
     * Finds the PropertyMetaData matching the property with the given name.
     *
     * @param name The name of the property to find. Cannot be null.
     * @return The property of the given name, or null if there isn't one.
     */
    public final PropertyMetaData findProperty(String name) {
        if (name == null) throw new NullPointerException("name cannot be null");
        for (PropertyMetaData md : getProperties()) {
            if (md.getName().equals(name)) {
                return md;
            }
        }
        return null;
    }

    /**
     * Gets the PropertyMetaData corresponding to the property which was
     * identified via the DefaultProperty annotation used on the JavaFX Bean.
     * If no DefaultProperty annotation was used or the value identified
     * a non-existent property, then null is returned.
     *
     * @return The PropertyMetaData of the default property, or null if there
     *         isn't one.
     */
    public final PropertyMetaData getDefaultProperty() {
        return defaultProperty;
    }

    /**
     * Gets the class type for which this BeanMetaData represents.
     *
     * @return A non-null reference to the bean type
     */
    public final Class<T> getType() {
        return type;
    }
}
