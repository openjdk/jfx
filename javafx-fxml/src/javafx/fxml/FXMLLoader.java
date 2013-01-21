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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;

import javafx.beans.DefaultProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.util.Builder;
import javafx.util.BuilderFactory;
import javafx.util.Callback;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.util.StreamReaderDelegate;

import com.sun.javafx.beans.IDProperty;
import com.sun.javafx.fxml.BeanAdapter;
import com.sun.javafx.fxml.LoadListener;
import com.sun.javafx.fxml.ObservableListChangeEvent;
import com.sun.javafx.fxml.ObservableMapChangeEvent;
import com.sun.javafx.fxml.PropertyChangeEvent;
import com.sun.javafx.fxml.PropertyNotFoundException;
import com.sun.javafx.fxml.expression.Expression;
import com.sun.javafx.fxml.expression.ExpressionValue;
import com.sun.javafx.fxml.expression.KeyPath;
import java.util.Locale;
import sun.reflect.misc.ConstructorUtil;
import sun.reflect.misc.FieldUtil;
import sun.reflect.misc.MethodUtil;
import sun.reflect.misc.ReflectUtil;

/**
 * Loads an object hierarchy from an XML document.
 */
public class FXMLLoader {
    // Abstract base class for elements
    private abstract class Element {
        public final Element parent;
        public final int lineNumber;

        public Object value = null;
        private BeanAdapter valueAdapter = null;

        public final LinkedList<Attribute> eventHandlerAttributes = new LinkedList<Attribute>();
        public final LinkedList<Attribute> instancePropertyAttributes = new LinkedList<Attribute>();
        public final LinkedList<Attribute> staticPropertyAttributes = new LinkedList<Attribute>();
        public final LinkedList<PropertyElement> staticPropertyElements = new LinkedList<PropertyElement>();

        public Element() {
            parent = current;
            lineNumber = getLineNumber();
        }

        public boolean isCollection() {
            // Return true if value is a list, or if the value's type defines
            // a default property that is a list
            boolean collection;
            if (value instanceof List<?>) {
                collection = true;
            } else {
                Class<?> type = value.getClass();
                DefaultProperty defaultProperty = type.getAnnotation(DefaultProperty.class);

                if (defaultProperty != null) {
                    collection = getProperties().get(defaultProperty.value()) instanceof List<?>;
                } else {
                    collection = false;
                }
            }

            return collection;
        }

        @SuppressWarnings("unchecked")
        public void add(Object element) throws LoadException {
            // If value is a list, add element to it; otherwise, get the value
            // of the default property, which is assumed to be a list and add
            // to that (coerce to the appropriate type)
            List<Object> list;
            if (value instanceof List<?>) {
                list = (List<Object>)value;
            } else {
                Class<?> type = value.getClass();
                DefaultProperty defaultProperty = type.getAnnotation(DefaultProperty.class);
                String defaultPropertyName = defaultProperty.value();

                // Get the list value
                list = (List<Object>)getProperties().get(defaultPropertyName);

                // Coerce the element to the list item type
                if (!Map.class.isAssignableFrom(type)) {
                    Type listType = getValueAdapter().getGenericType(defaultPropertyName);
                    element = BeanAdapter.coerce(element, BeanAdapter.getListItemType(listType));
                }
            }

            list.add(element);
        }

        public void set(Object value) throws LoadException {
            if (this.value == null) {
                throw new LoadException("Cannot set value on this element.");
            }

            // Apply value to this element's properties
            Class<?> type = this.value.getClass();
            DefaultProperty defaultProperty = type.getAnnotation(DefaultProperty.class);
            if (defaultProperty == null) {
                throw new LoadException("Element does not define a default property.");
            }

            getProperties().put(defaultProperty.value(), value);
        }

        public void updateValue(Object value) {
            this.value = value;
            valueAdapter = null;
        }

        public boolean isTyped() {
            return !(value instanceof Map<?, ?>);
        }

        public BeanAdapter getValueAdapter() {
            if (valueAdapter == null) {
                valueAdapter = new BeanAdapter(value);
            }

            return valueAdapter;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> getProperties() {
            return (isTyped()) ? getValueAdapter() : (Map<String, Object>)value;
        }

        public void processStartElement() throws IOException {
            for (int i = 0, n = xmlStreamReader.getAttributeCount(); i < n; i++) {
                String prefix = xmlStreamReader.getAttributePrefix(i);
                String localName = xmlStreamReader.getAttributeLocalName(i);
                String value = xmlStreamReader.getAttributeValue(i);

                if (loadListener != null
                    && prefix != null
                    && prefix.equals(FX_NAMESPACE_PREFIX)) {
                    loadListener.readInternalAttribute(prefix + ":" + localName, value);
                }

                processAttribute(prefix, localName, value);
            }
        }

        public void processEndElement() throws IOException {
            // No-op
        }

        public void processCharacters() throws IOException {
            throw new LoadException("Unexpected characters in input stream.");
        }

        public void processInstancePropertyAttributes() throws IOException {
            if (instancePropertyAttributes.size() > 0) {
                for (Attribute attribute : instancePropertyAttributes) {
                    processPropertyAttribute(attribute);
                }
            }
        }

        public void processAttribute(String prefix, String localName, String value)
            throws IOException{
            if (prefix == null) {
                // Add the attribute to the appropriate list
                if (localName.startsWith(EVENT_HANDLER_PREFIX)) {
                    if (loadListener != null) {
                        loadListener.readEventHandlerAttribute(localName, value);
                    }

                    eventHandlerAttributes.add(new Attribute(localName, null, value));
                } else {
                    int i = localName.lastIndexOf('.');

                    if (i == -1) {
                        // The attribute represents an instance property
                        if (loadListener != null) {
                            loadListener.readPropertyAttribute(localName, null, value);
                        }

                        instancePropertyAttributes.add(new Attribute(localName, null, value));
                    } else {
                        // The attribute represents a static property
                        String name = localName.substring(i + 1);
                        Class<?> sourceType = getType(localName.substring(0, i));

                        if (sourceType != null) {
                            if (loadListener != null) {
                                loadListener.readPropertyAttribute(name, sourceType, value);
                            }

                            staticPropertyAttributes.add(new Attribute(name, sourceType, value));
                        } else if (staticLoad) {
                            if (loadListener != null) {
                                loadListener.readUnknownStaticPropertyAttribute(localName, value);
                            }
                        } else {
                            throw new LoadException(localName + " is not a valid attribute.");
                        }
                    }

                }
            } else {
                throw new LoadException(prefix + ":" + localName
                    + " is not a valid attribute.");
            }
        }

        @SuppressWarnings("unchecked")
        public void processPropertyAttribute(Attribute attribute) throws IOException {
            String value = attribute.value;

            if (value.startsWith(ESCAPE_PREFIX)) {
                value = value.substring(ESCAPE_PREFIX.length());

                if (value.length() == 0
                    || !(value.startsWith(ESCAPE_PREFIX)
                        || value.startsWith(RELATIVE_PATH_PREFIX)
                        || value.startsWith(RESOURCE_KEY_PREFIX)
                        || value.startsWith(EXPRESSION_PREFIX)
                        || value.startsWith(BI_DIRECTIONAL_BINDING_PREFIX))) {
                    throw new LoadException("Invalid escape sequence.");
                }

                applyProperty(attribute.name, attribute.sourceType, value);
            } else if (value.startsWith(RELATIVE_PATH_PREFIX)) {
                value = value.substring(RELATIVE_PATH_PREFIX.length());

                if (value.startsWith(RELATIVE_PATH_PREFIX)) {
                    // The prefix was escaped
                    warnDeprecatedEscapeSequence(RELATIVE_PATH_PREFIX);
                    applyProperty(attribute.name, attribute.sourceType, value);
                } else {
                    if (value.length() == 0) {
                        throw new LoadException("Missing relative path.");
                    }

                    URL location;
                    if (value.charAt(0) == '/') {
                        location = classLoader.getResource(value.substring(1));
                    } else {
                        if (FXMLLoader.this.location == null) {
                            throw new LoadException("Base location is undefined.");
                        }

                        location = new URL(FXMLLoader.this.location, value);
                    }

                    applyProperty(attribute.name, attribute.sourceType, location);
                }
            } else if (value.startsWith(RESOURCE_KEY_PREFIX)) {
                value = value.substring(RESOURCE_KEY_PREFIX.length());

                if (value.startsWith(RESOURCE_KEY_PREFIX)) {
                    // The prefix was escaped
                    warnDeprecatedEscapeSequence(RESOURCE_KEY_PREFIX);
                    applyProperty(attribute.name, attribute.sourceType, value);
                } else {
                    if (value.length() == 0) {
                        throw new LoadException("Missing resource key.");
                    }

                    // Resolve the resource value
                    if (resources == null) {
                        throw new LoadException("No resources specified.");
                    }

                    if (!resources.containsKey(value)) {
                        throw new LoadException("Resource \"" + value + "\" not found.");
                    }

                    applyProperty(attribute.name, attribute.sourceType, resources.getObject(value));
                }
            } else if (value.startsWith(EXPRESSION_PREFIX)) {
                value = value.substring(EXPRESSION_PREFIX.length());

                if (value.startsWith(EXPRESSION_PREFIX)) {
                    // The prefix was escaped
                    warnDeprecatedEscapeSequence(EXPRESSION_PREFIX);
                    applyProperty(attribute.name, attribute.sourceType, value);
                } else if (value.equals(NULL_KEYWORD)) {
                    // The attribute value is null
                    applyProperty(attribute.name, attribute.sourceType, null);
                } else {
                    if (value.length() == 0) {
                        throw new LoadException("Missing expression.");
                    }

                    // Resolve the expression
                    Expression expression;
                    if (value.startsWith(BINDING_EXPRESSION_PREFIX)
                        && value.endsWith(BINDING_EXPRESSION_SUFFIX)) {
                        if (attribute.sourceType != null) {
                            throw new LoadException("Cannot bind to static property.");
                        }

                        if (!isTyped()) {
                            throw new LoadException("Cannot bind to untyped object.");
                        }

                        // TODO We may want to identify binding properties in processAttribute()
                        // and apply them after build() has been called
                        if (this.value instanceof Builder) {
                            throw new LoadException("Cannot bind to builder property.");
                        }

                        value = value.substring(1, value.length() - 1);
                        expression = Expression.valueOf(value);

                        // Create the binding
                        BeanAdapter targetAdapter = new BeanAdapter(this.value);
                        ObservableValue<Object> propertyModel = targetAdapter.getPropertyModel(attribute.name);
                        Class<?> type = targetAdapter.getType(attribute.name);

                        if (propertyModel instanceof Property<?>) {
                            ((Property<Object>)propertyModel).bind(new ExpressionValue(namespace, expression, type));
                        }
                    } else {
                        applyProperty(attribute.name, attribute.sourceType, Expression.get(namespace, KeyPath.parse(value)));
                    }
                }
            } else if (value.startsWith(BI_DIRECTIONAL_BINDING_PREFIX)) {
                throw new UnsupportedOperationException("This feature is not currently enabled.");
            } else {
                Object propertyValue = value;

                if (attribute.sourceType == null && isTyped()) {
                    BeanAdapter valueAdapter = getValueAdapter();
                    Class<?> type = valueAdapter.getType(attribute.name);

                    if (type == null) {
                        throw new PropertyNotFoundException("Property \"" + attribute.name
                            + "\" does not exist" + " or is read-only.");
                    }

                    if (List.class.isAssignableFrom(type)
                        && valueAdapter.isReadOnly(attribute.name)) {
                        // Split the string and add the values to the list
                        List<Object> list = (List<Object>)valueAdapter.get(attribute.name);
                        Type listType = valueAdapter.getGenericType(attribute.name);
                        Type itemType = (Class<?>)BeanAdapter.getGenericListItemType(listType);

                        if (itemType instanceof ParameterizedType) {
                            itemType = ((ParameterizedType)itemType).getRawType();
                        }

                        String stringValue = value.toString();
                        if (stringValue.length() > 0) {
                            String[] values = stringValue.split(ARRAY_COMPONENT_DELIMITER);

                            for (int i = 0; i < values.length; i++) {
                                list.add(BeanAdapter.coerce(values[i].trim(), (Class<?>)itemType));
                            }
                        }

                        propertyValue = null;
                    } else if (type.isArray()) {
                        // Split the string and set the values as an array
                        Class<?> componentType = type.getComponentType();

                        String stringValue = value.toString();
                        if (stringValue.length() > 0) {
                            String[] values = stringValue.split(ARRAY_COMPONENT_DELIMITER);
                            propertyValue = Array.newInstance(componentType, values.length);
                            for (int i = 0; i < values.length; i++) {
                                Array.set(propertyValue, i, BeanAdapter.coerce(values[i].trim(),
                                    type.getComponentType()));
                            }
                        } else {
                            propertyValue = Array.newInstance(componentType, 0);
                        }
                    }
                }

                if (propertyValue != null) {
                    applyProperty(attribute.name, attribute.sourceType, propertyValue);
                }
            }
        }

        public void warnDeprecatedEscapeSequence(String prefix) {
            System.err.println(prefix + prefix + " is a deprecated escape sequence. "
                + "Please use \\" + prefix + " instead.");
        }

        public void applyProperty(String name, Class<?> sourceType, Object value) {
            if (sourceType == null) {
                getProperties().put(name, value);
            } else {
                BeanAdapter.put(this.value, sourceType, name, value);
            }
        }

        public void processEventHandlerAttributes() throws LoadException {
            if (eventHandlerAttributes.size() > 0 && !staticLoad) {
                for (Attribute attribute : eventHandlerAttributes) {
                    EventHandler<? extends Event> eventHandler = null;

                    String value = attribute.value;

                    if (value.startsWith(CONTROLLER_METHOD_PREFIX)) {
                        value = value.substring(CONTROLLER_METHOD_PREFIX.length());

                        if (!value.startsWith(CONTROLLER_METHOD_PREFIX)) {
                            if (value.length() == 0) {
                                throw new LoadException("Missing controller method.");
                            }

                            if (controller == null) {
                                throw new LoadException("No controller specified.");
                            }

                            Method method = getControllerMethods().get(value);

                            if (method == null) {
                                throw new LoadException("Controller method \"" + value + "\" not found.");
                            }

                            eventHandler = new ControllerMethodEventHandler(controller, method);
                        }
                    }

                    if (eventHandler == null) {
                        if (value.length() == 0) {
                            throw new LoadException("Missing handler script.");
                        }

                        if (scriptEngine == null) {
                            throw new LoadException("Page language not specified.");
                        }

                        eventHandler = new ScriptEventHandler(value, scriptEngine);
                    }

                    // Add the handler
                    if (eventHandler != null){
                        addEventHandler(attribute, eventHandler);
                    }
                }
            }
        }

        @SuppressWarnings("unchecked")
        private void addEventHandler(Attribute attribute, EventHandler<? extends Event> eventHandler)
            throws LoadException {
            if (attribute.name.endsWith(CHANGE_EVENT_HANDLER_SUFFIX)) {
                int i = EVENT_HANDLER_PREFIX.length();
                int j = attribute.name.length() - CHANGE_EVENT_HANDLER_SUFFIX.length();

                if (i == j) {
                    if (value instanceof ObservableList<?>) {
                        ObservableList<Object> list = (ObservableList<Object>)value;
                        list.addListener(new ObservableListChangeAdapter(list,
                            (EventHandler<ObservableListChangeEvent<?>>)eventHandler));
                    } else if (value instanceof ObservableMap<?, ?>) {
                        ObservableMap<Object, Object> map = (ObservableMap<Object, Object>)value;
                        map.addListener(new ObservableMapChangeAdapter(map,
                            (EventHandler<ObservableMapChangeEvent<?, ?>>)eventHandler));
                    } else {
                        throw new LoadException("Invalid event source.");
                    }
                } else {
                    String key = Character.toLowerCase(attribute.name.charAt(i))
                        + attribute.name.substring(i + 1, j);

                    ObservableValue<Object> propertyModel = getValueAdapter().getPropertyModel(key);
                    if (propertyModel == null) {
                        throw new LoadException(value.getClass().getName() + " does not define"
                                + " a property model for \"" + key + "\".");
                    }

                    propertyModel.addListener(new PropertyChangeAdapter(value,
                        (EventHandler<PropertyChangeEvent<?>>)eventHandler));
                }
            } else {
                getValueAdapter().put(attribute.name, eventHandler);
            }
        }
    }

    // Element representing a value
    private abstract class ValueElement extends Element {
        public String fx_id = null;

        @Override
        public void processStartElement() throws IOException {
            super.processStartElement();

            updateValue(constructValue());

            if (value instanceof Builder<?>) {
                processInstancePropertyAttributes();
            } else {
                processValue();
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void processEndElement() throws IOException {
            super.processEndElement();

            // Build the value, if necessary
            if (value instanceof Builder<?>) {
                Builder<Object> builder = (Builder<Object>)value;
                updateValue(builder.build());

                processValue();
            } else {
                processInstancePropertyAttributes();
            }

            processEventHandlerAttributes();

            // Process static property attributes
            if (staticPropertyAttributes.size() > 0) {
                for (Attribute attribute : staticPropertyAttributes) {
                    processPropertyAttribute(attribute);
                }
            }

            // Process static property elements
            if (staticPropertyElements.size() > 0) {
                for (PropertyElement element : staticPropertyElements) {
                    BeanAdapter.put(value, element.sourceType, element.name, element.value);
                }
            }

            if (parent != null) {
                if (parent.isCollection()) {
                    parent.add(value);
                } else {
                    parent.set(value);
                }
            }
        }

        private Object getListValue(Element parent, String listPropertyName, Object value) {
            // If possible, coerce the value to the list item type
            if (parent.isTyped()) {
                Type listType = parent.getValueAdapter().getGenericType(listPropertyName);

                if (listType != null) {
                    Type itemType = BeanAdapter.getGenericListItemType(listType);

                    if (itemType instanceof ParameterizedType) {
                        itemType = ((ParameterizedType)itemType).getRawType();
                    }

                    value = BeanAdapter.coerce(value, (Class<?>)itemType);
                }
            }

            return value;
        }

        private void processValue() throws LoadException {
            // If this is the root element, update the value
            if (parent == null) {
                root = value;
            }

            // Add the value to the namespace
            if (fx_id != null) {
                namespace.put(fx_id, value);

                // If the value defines an ID property, set it
                IDProperty idProperty = value.getClass().getAnnotation(IDProperty.class);

                if (idProperty != null) {
                    Map<String, Object> properties = getProperties();
                    // set fx:id property value to Node.id only if Node.id was not
                    // already set when processing start element attributes
                    if (properties.get(idProperty.value()) == null) {
                        properties.put(idProperty.value(), fx_id);
                    }
                }

                // Set the controller field value
                if (controller != null) {
                    Field field = getControllerFields().get(fx_id);

                    if (field != null) {
                        try {
                            field.set(controller, value);
                        } catch (IllegalAccessException exception) {
                            throw new RuntimeException(exception);
                        }
                    }
                }
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public void processCharacters() throws LoadException {
            Class<?> type = value.getClass();
            DefaultProperty defaultProperty = type.getAnnotation(DefaultProperty.class);

            // If the default property is a read-only list, add the value to it;
            // otherwise, set the value as the default property
            if (defaultProperty != null) {
                String text = xmlStreamReader.getText();
                text = extraneousWhitespacePattern.matcher(text).replaceAll(" ");

                String defaultPropertyName = defaultProperty.value();
                BeanAdapter valueAdapter = getValueAdapter();

                if (valueAdapter.isReadOnly(defaultPropertyName)
                    && List.class.isAssignableFrom(valueAdapter.getType(defaultPropertyName))) {
                    List<Object> list = (List<Object>)valueAdapter.get(defaultPropertyName);
                    list.add(getListValue(this, defaultPropertyName, text));
                } else {
                    valueAdapter.put(defaultPropertyName, text.trim());
                }
            } else {
                throw new LoadException(type.getName() + " does not have a default property.");
            }
        }

        @Override
        public void processAttribute(String prefix, String localName, String value)
            throws IOException{
            if (prefix != null
                && prefix.equals(FX_NAMESPACE_PREFIX)) {
                if (localName.equals(FX_ID_ATTRIBUTE)) {
                    // Verify that ID is a valid identifier
                    if (value.equals(NULL_KEYWORD)) {
                        throw new LoadException("Invalid identifier.");
                    }

                    for (int i = 0, n = value.length(); i < n; i++) {
                        if (!Character.isJavaIdentifierPart(value.charAt(i))) {
                            throw new LoadException("Invalid identifier.");
                        }
                    }

                    fx_id = value;

                } else if (localName.equals(FX_CONTROLLER_ATTRIBUTE)) {
                    if (current.parent != null) {
                        throw new LoadException(FX_NAMESPACE_PREFIX + ":" + FX_CONTROLLER_ATTRIBUTE
                            + " can only be applied to root element.");
                    }

                    if (controller != null) {
                        throw new LoadException("Controller value already specified.");
                    }

                    if (!staticLoad) {
                        Class<?> type;
                        try {
                            type = classLoader.loadClass(value);
                        } catch (ClassNotFoundException exception) {
                            throw new LoadException(exception);
                        }

                        try {
                            if (controllerFactory == null) {
                                setController(ReflectUtil.newInstance(type));
                            } else {
                                setController(controllerFactory.call(type));
                            }
                        } catch (InstantiationException exception) {
                            throw new LoadException(exception);
                        } catch (IllegalAccessException exception) {
                            throw new LoadException(exception);
                        }
                    }
                } else {
                    throw new LoadException("Invalid attribute.");
                }
            } else {
                super.processAttribute(prefix, localName, value);
            }
        }

        public abstract Object constructValue() throws IOException;
    }

    // Element representing a class instance
    private class InstanceDeclarationElement extends ValueElement {
        public Class<?> type;

        public String constant = null;
        public String factory = null;

        public InstanceDeclarationElement(Class<?> type) throws LoadException {
            this.type = type;
        }

        @Override
        public void processAttribute(String prefix, String localName, String value)
            throws IOException {
            if (prefix != null
                && prefix.equals(FX_NAMESPACE_PREFIX)) {
                if (localName.equals(FX_VALUE_ATTRIBUTE)) {
                    this.value = value;
                } else if (localName.equals(FX_CONSTANT_ATTRIBUTE)) {
                    constant = value;
                } else if (localName.equals(FX_FACTORY_ATTRIBUTE)) {
                    factory = value;
                } else {
                    super.processAttribute(prefix, localName, value);
                }
            } else {
                super.processAttribute(prefix, localName, value);
            }
        }

        @Override
        public Object constructValue() throws IOException {
            Object value;
            if (this.value != null) {
                value = BeanAdapter.coerce(this.value, type);
            } else if (constant != null) {
                value = BeanAdapter.getConstantValue(type, constant);
            } else if (factory != null) {
                Method factoryMethod;
                try {
                    factoryMethod = MethodUtil.getMethod(type, factory, new Class[] {});
                } catch (NoSuchMethodException exception) {
                    throw new LoadException(exception);
                }

                try {
                    value = MethodUtil.invoke(factoryMethod, null, new Object [] {});
                } catch (IllegalAccessException exception) {
                    throw new LoadException(exception);
                } catch (InvocationTargetException exception) {
                    throw new LoadException(exception);
                }
            } else {
                value = (builderFactory == null) ? null : builderFactory.getBuilder(type);

                if (value == null) {
                    try {
                        value = ReflectUtil.newInstance(type);
                    } catch (InstantiationException exception) {
                        throw new LoadException(exception);
                    } catch (IllegalAccessException exception) {
                        throw new LoadException(exception);
                    }
                }
            }

            return value;
        }
    }

    // Element representing an unknown type
    private class UnknownTypeElement extends ValueElement {
        // Map type representing an unknown value
        @DefaultProperty("items")
        public class UnknownValueMap extends AbstractMap<String, Object> {
            private ArrayList<?> items = new ArrayList<Object>();
            private HashMap<String, Object> values = new HashMap<String, Object>();

            @Override
            public Object get(Object key) {
                if (key == null) {
                    throw new NullPointerException();
                }

                return (key.equals(getClass().getAnnotation(DefaultProperty.class).value())) ?
                    items : values.get(key);
            }

            @Override
            public Object put(String key, Object value) {
                if (key == null) {
                    throw new NullPointerException();
                }

                if (key.equals(getClass().getAnnotation(DefaultProperty.class).value())) {
                    throw new IllegalArgumentException();
                }

                return values.put(key, value);
            }

            @Override
            public Set<Entry<String, Object>> entrySet() {
                return Collections.emptySet();
            }
        }

        @Override
        public void processEndElement() throws IOException {
            // No-op
        }

        @Override
        public Object constructValue() throws LoadException {
            return new UnknownValueMap();
        }
    }

    // Element representing an include
    private class IncludeElement extends ValueElement {
        public String source = null;
        public ResourceBundle resources = FXMLLoader.this.resources;
        public Charset charset = FXMLLoader.this.charset;

        @Override
        public void processAttribute(String prefix, String localName, String value)
            throws IOException {
            if (prefix == null) {
                if (localName.equals(INCLUDE_SOURCE_ATTRIBUTE)) {
                    if (loadListener != null) {
                        loadListener.readInternalAttribute(localName, value);
                    }

                    source = value;
                } else if (localName.equals(INCLUDE_RESOURCES_ATTRIBUTE)) {
                    if (loadListener != null) {
                        loadListener.readInternalAttribute(localName, value);
                    }

                    resources = ResourceBundle.getBundle(value, Locale.getDefault(), 
                            FXMLLoader.this.resources.getClass().getClassLoader());
                } else if (localName.equals(INCLUDE_CHARSET_ATTRIBUTE)) {
                    if (loadListener != null) {
                        loadListener.readInternalAttribute(localName, value);
                    }

                    charset = Charset.forName(value);
                } else {
                    super.processAttribute(prefix, localName, value);
                }
            } else {
                super.processAttribute(prefix, localName, value);
            }
        }

        @Override
        public Object constructValue() throws IOException {
            if (source == null) {
                throw new LoadException(INCLUDE_SOURCE_ATTRIBUTE + " is required.");
            }

            URL location;
            if (source.charAt(0) == '/') {
                location = classLoader.getResource(source.substring(1));
            } else {
                if (FXMLLoader.this.location == null) {
                    throw new LoadException("Base location is undefined.");
                }

                location = new URL(FXMLLoader.this.location, source);
            }

            FXMLLoader fxmlLoader = new FXMLLoader(location, resources,
                builderFactory, controllerFactory, charset,
                loaders);
            fxmlLoader.parentLoader = FXMLLoader.this;
            
            if (isCyclic(FXMLLoader.this, fxmlLoader)) {
                throw new IOException(
                        String.format(
                        "Including \"%s\" in \"%s\" created cyclic reference.",
                        fxmlLoader.location.toExternalForm(),
                        FXMLLoader.this.location.toExternalForm()));
            }
            fxmlLoader.setClassLoader(classLoader);
            fxmlLoader.setStaticLoad(staticLoad);

            Object value = fxmlLoader.load();

            if (fx_id != null) {
                String id = this.fx_id + CONTROLLER_SUFFIX;
                Object controller = fxmlLoader.getController();

                namespace.put(id, controller);

                if (FXMLLoader.this.controller != null) {
                    Field field = getControllerFields().get(id);

                    if (field != null) {
                        try {
                            field.set(FXMLLoader.this.controller, controller);
                        } catch (IllegalAccessException exception) {
                            throw new LoadException(exception);
                        }
                    }
                }
            }

            return value;
        }
    }

    // Element representing a reference
    private class ReferenceElement extends ValueElement {
        public String source = null;

        @Override
        public void processAttribute(String prefix, String localName, String value)
            throws IOException {
            if (prefix == null) {
                if (localName.equals(REFERENCE_SOURCE_ATTRIBUTE)) {
                    if (loadListener != null) {
                        loadListener.readInternalAttribute(localName, value);
                    }

                    source = value;
                } else {
                    super.processAttribute(prefix, localName, value);
                }
            } else {
                super.processAttribute(prefix, localName, value);
            }
        }

        @Override
        public Object constructValue() throws LoadException {
            if (source == null) {
                throw new LoadException(REFERENCE_SOURCE_ATTRIBUTE + " is required.");
            }

            KeyPath path = KeyPath.parse(source);
            if (!Expression.isDefined(namespace, path)) {
                throw new LoadException("Value \"" + source + "\" does not exist.");
            }

            return Expression.get(namespace, path);
        }
    }

    // Element representing a copy
    private class CopyElement extends ValueElement {
        public String source = null;

        @Override
        public void processAttribute(String prefix, String localName, String value)
            throws IOException {
            if (prefix == null) {
                if (localName.equals(COPY_SOURCE_ATTRIBUTE)) {
                    if (loadListener != null) {
                        loadListener.readInternalAttribute(localName, value);
                    }

                    source = value;
                } else {
                    super.processAttribute(prefix, localName, value);
                }
            } else {
                super.processAttribute(prefix, localName, value);
            }
        }

        @Override
        public Object constructValue() throws LoadException {
            if (source == null) {
                throw new LoadException(COPY_SOURCE_ATTRIBUTE + " is required.");
            }

            KeyPath path = KeyPath.parse(source);
            if (!Expression.isDefined(namespace, path)) {
                throw new LoadException("Value \"" + source + "\" does not exist.");
            }

            Object sourceValue = Expression.get(namespace, path);
            Class<?> sourceValueType = sourceValue.getClass();

            Constructor<?> constructor = null;
            try {
                constructor = ConstructorUtil.getConstructor(sourceValueType, new Class[] { sourceValueType });
            } catch (NoSuchMethodException exception) {
                // No-op
            }

            Object value;
            if (constructor != null) {
                try {
                    ReflectUtil.checkPackageAccess(sourceValueType);
                    value = constructor.newInstance(sourceValue);
                } catch (InstantiationException exception) {
                    throw new LoadException(exception);
                } catch (IllegalAccessException exception) {
                    throw new LoadException(exception);
                } catch (InvocationTargetException exception) {
                    throw new LoadException(exception);
                }
            } else {
                throw new LoadException("Can't copy value " + sourceValue + ".");
            }

            return value;
        }
    }

    // Element representing a predefined root value
    private class RootElement extends ValueElement {
        public String type = null;

        @Override
        public void processAttribute(String prefix, String localName, String value)
            throws IOException {
            if (prefix == null) {
                if (localName.equals(ROOT_TYPE_ATTRIBUTE)) {
                    if (loadListener != null) {
                        loadListener.readInternalAttribute(localName, value);
                    }

                    type = value;
                } else {
                    super.processAttribute(prefix, localName, value);
                }
            } else {
                super.processAttribute(prefix, localName, value);
            }
        }

        @Override
        public Object constructValue() throws LoadException {
            if (type == null) {
                throw new LoadException(ROOT_TYPE_ATTRIBUTE + " is required.");
            }

            Class<?> type = getType(this.type);

            if (type == null) {
                throw new LoadException(this.type + " is not a valid type.");
            }

            Object value;
            if (root == null) {
                value = (builderFactory == null) ? null : builderFactory.getBuilder(type);

                if (value == null) {
                    try {
                        value = ReflectUtil.newInstance(type);
                    } catch (InstantiationException exception) {
                        throw new LoadException(exception);
                    } catch (IllegalAccessException exception) {
                        throw new LoadException(exception);
                    }
                }
            } else {
                if (!type.isAssignableFrom(root.getClass())) {
                    throw new LoadException("Root is not an instance of "
                        + type.getName() + ".");
                }

                value = root;
            }

            return value;
        }
    }

    // Element representing a property
    private class PropertyElement extends Element {
        public final String name;
        public final Class<?> sourceType;
        public final boolean readOnly;

        public PropertyElement(String name, Class<?> sourceType) throws LoadException {
            if (parent == null) {
                throw new LoadException("Invalid root element.");
            }

            if (parent.value == null) {
                throw new LoadException("Parent element does not support property elements.");
            }

            this.name = name;
            this.sourceType = sourceType;

            if (sourceType == null) {
                // The element represents an instance property
                if (name.startsWith(EVENT_HANDLER_PREFIX)) {
                    throw new LoadException("\"" + name + "\" is not a valid element name.");
                }

            Map<String, Object> parentProperties = parent.getProperties();

            if (parent.isTyped()) {
                readOnly = parent.getValueAdapter().isReadOnly(name);
            } else {
                // If the map already defines a value for the property, assume
                // that it is read-only
                readOnly = parentProperties.containsKey(name);
            }

            if (readOnly) {
                Object value = parentProperties.get(name);
                if (value == null) {
                    throw new LoadException("Invalid property.");
                }

                updateValue(value);
            }
            } else {
                // The element represents a static property
                readOnly = false;
            }
        }

        @Override
        public boolean isCollection() {
            return (readOnly) ? super.isCollection() : false;
        }

        @Override
        public void add(Object element) throws LoadException {
            // Coerce the element to the list item type
            if (parent.isTyped()) {
                Type listType = parent.getValueAdapter().getGenericType(name);
                element = BeanAdapter.coerce(element, BeanAdapter.getListItemType(listType));
            }

            // Add the item to the list
            super.add(element);
        }

        @Override
        public void set(Object value) throws LoadException {
            // Update the value
            updateValue(value);

            if (sourceType == null) {
                // Apply value to parent element's properties
                parent.getProperties().put(name, value);
            } else {
                if (parent.value instanceof Builder) {
                    // Defer evaluation of the property
                    parent.staticPropertyElements.add(this);
                } else {
                    // Apply the static property value
                    BeanAdapter.put(parent.value, sourceType, name, value);
                }
            }
        }

        @Override
        public void processAttribute(String prefix, String localName, String value)
            throws IOException {
            if (!readOnly) {
                throw new LoadException("Attributes are not supported for writable property elements.");
            }

            super.processAttribute(prefix, localName, value);
        }

        @Override
        public void processEndElement() throws IOException {
            super.processEndElement();

            if (readOnly) {
                processInstancePropertyAttributes();
                processEventHandlerAttributes();
            }
        }

        @Override
        public void processCharacters() throws IOException {
            if (!readOnly) {
                String text = xmlStreamReader.getText();
                text = extraneousWhitespacePattern.matcher(text).replaceAll(" ");

                set(text.trim());
            } else {
                super.processCharacters();
            }
        }
    }

    // Element representing an unknown static property
    private class UnknownStaticPropertyElement extends Element {
        public UnknownStaticPropertyElement() throws LoadException {
            if (parent == null) {
                throw new LoadException("Invalid root element.");
            }

            if (parent.value == null) {
                throw new LoadException("Parent element does not support property elements.");
            }
        }

        @Override
        public boolean isCollection() {
            return false;
        }

        @Override
        public void set(Object value) {
            updateValue(value);
        }

        @Override
        public void processCharacters() throws IOException {
            String text = xmlStreamReader.getText();
            text = extraneousWhitespacePattern.matcher(text).replaceAll(" ");

            updateValue(text.trim());
        }
    }

    // Element representing a script block
    private class ScriptElement extends Element {
        public String source = null;
        public Charset charset = FXMLLoader.this.charset;

        @Override
        public boolean isCollection() {
            return false;
        }

        @Override
        public void processStartElement() throws IOException {
            super.processStartElement();

            if (source != null && !staticLoad) {
                int i = source.lastIndexOf(".");
                if (i == -1) {
                    throw new LoadException("Cannot determine type of script \""
                        + source + "\".");
                }

                String extension = source.substring(i + 1);
                ScriptEngine scriptEngine;
                ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    ScriptEngineManager scriptEngineManager = getScriptEngineManager();
                    scriptEngine = scriptEngineManager.getEngineByExtension(extension);
                } finally {
                    Thread.currentThread().setContextClassLoader(oldLoader);
                }

                if (scriptEngine == null) {
                    throw new LoadException("Unable to locate scripting engine for"
                        + " extension " + extension + ".");
                }

                scriptEngine.setBindings(scriptEngineManager.getBindings(), ScriptContext.ENGINE_SCOPE);

                try {
                    URL location;
                    if (source.charAt(0) == '/') {
                        location = classLoader.getResource(source.substring(1));
                    } else {
                        if (FXMLLoader.this.location == null) {
                            throw new LoadException("Base location is undefined.");
                        }

                        location = new URL(FXMLLoader.this.location, source);
                    }

                    InputStreamReader scriptReader = null;
                    try {
                        scriptReader = new InputStreamReader(location.openStream(), charset);
                        scriptEngine.eval(scriptReader);
                    } catch(ScriptException exception) {
                        exception.printStackTrace();
                    } finally {
                        if (scriptReader != null) {
                            scriptReader.close();
                        }
                    }
                } catch (IOException exception) {
                    throw new LoadException(exception);
                }
            }
        }

        @Override
        public void processEndElement() throws IOException {
            super.processEndElement();

            if (value != null && !staticLoad) {
                // Evaluate the script
                try {
                    scriptEngine.eval((String)value);
                } catch (ScriptException exception) {
                    System.err.println(exception.getMessage());
                }
            }
        }

        @Override
        public void processCharacters() throws LoadException {
            if (source != null) {
                throw new LoadException("Script source already specified.");
            }

            if (scriptEngine == null && !staticLoad) {
                throw new LoadException("Page language not specified.");
            }

            updateValue(xmlStreamReader.getText());
        }

        @Override
        public void processAttribute(String prefix, String localName, String value)
            throws IOException {
            if (prefix == null
                && localName.equals(SCRIPT_SOURCE_ATTRIBUTE)) {
                if (loadListener != null) {
                    loadListener.readInternalAttribute(localName, value);
                }

                source = value;
            } else if (localName.equals(SCRIPT_CHARSET_ATTRIBUTE)) {
                if (loadListener != null) {
                    loadListener.readInternalAttribute(localName, value);
                }

                charset = Charset.forName(value);
            } else {
                throw new LoadException(prefix == null ? localName : prefix + ":" + localName
                    + " is not a valid attribute.");
            }
        }
    }

    // Element representing a define block
    private class DefineElement extends Element {
        @Override
        public boolean isCollection() {
            return true;
        }

        @Override
        public void add(Object element) {
            // No-op
        }

        @Override
        public void processAttribute(String prefix, String localName, String value)
            throws LoadException{
            throw new LoadException("Element does not support attributes.");
        }
    }

    // Class representing an attribute of an element
    private static class Attribute {
        public final String name;
        public final Class<?> sourceType;
        public final String value;

        public Attribute(String name, Class<?> sourceType, String value) {
            this.name = name;
            this.sourceType = sourceType;
            this.value = value;
        }
    }

    // Event handler that delegates to a method defined by the controller object
    private static class ControllerMethodEventHandler implements EventHandler<Event> {
        public final Object controller;
        public final Method method;
        public final boolean typed;

        public ControllerMethodEventHandler(Object controller, Method method) {
            this.controller = controller;
            this.method = method;
            this.typed = (method.getParameterTypes().length == 1);
        }

        @Override
        public void handle(Event event) {
            try {
                if (typed) {
                    MethodUtil.invoke(method, controller, new Object[] { event });
                } else {
                    MethodUtil.invoke(method, controller, new Object[] {});
                }
            } catch (InvocationTargetException exception) {
                throw new RuntimeException(exception);
            } catch (IllegalAccessException exception) {
                throw new RuntimeException(exception);
            }
        }
    }

    // Event handler implemented in script code
    private static class ScriptEventHandler implements EventHandler<Event> {
        public final String script;
        public final ScriptEngine scriptEngine;

        public ScriptEventHandler(String script, ScriptEngine scriptEngine) {
            this.script = script;
            this.scriptEngine = scriptEngine;
        }

        @Override
        public void handle(Event event) {
            // Don't pollute the page namespace with values defined in the script
            Bindings engineBindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
            Bindings localBindings = scriptEngine.createBindings();
            localBindings.put(EVENT_KEY, event);
            scriptEngine.setBindings(localBindings, ScriptContext.ENGINE_SCOPE);

            // Execute the script
            try {
                scriptEngine.eval(script);
            } catch (ScriptException exception){
                throw new RuntimeException(exception);
            }

            // Restore the original bindings
            scriptEngine.setBindings(engineBindings, ScriptContext.ENGINE_SCOPE);
        }
    }

    // Observable list change listener
    private static class ObservableListChangeAdapter implements ListChangeListener<Object> {
        public final ObservableList<Object> source;
        public final EventHandler<ObservableListChangeEvent<?>> handler;

        public ObservableListChangeAdapter(ObservableList<Object> source,
            EventHandler<ObservableListChangeEvent<?>> handler) {
            this.source = source;
            this.handler = handler;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onChanged(Change<? extends Object> change) {
            while (change.next()) {
                EventType<ObservableListChangeEvent<?>> eventType;
                List<Object> removed = (List<Object>)change.getRemoved();

                if (change.wasPermutated()) {
                    eventType = ObservableListChangeEvent.UPDATE;
                    removed = null;
                } else if (change.wasAdded() && change.wasRemoved()) {
                    eventType = ObservableListChangeEvent.UPDATE;
                } else if (change.wasAdded()) {
                    eventType = ObservableListChangeEvent.ADD;
                } else if (change.wasRemoved()) {
                    eventType = ObservableListChangeEvent.REMOVE;
                } else {
                    throw new UnsupportedOperationException();
                }

                handler.handle(new ObservableListChangeEvent<Object>(source,
                    eventType, change.getFrom(), change.getTo(),
                    removed));
            }
        }
    }

    // Observable map change listener
    private static class ObservableMapChangeAdapter implements MapChangeListener<Object, Object> {
        public final ObservableMap<Object, Object> source;
        public final EventHandler<ObservableMapChangeEvent<?, ?>> handler;

        public ObservableMapChangeAdapter(ObservableMap<Object, Object> source,
            EventHandler<ObservableMapChangeEvent<?, ?>> handler) {
            this.source = source;
            this.handler = handler;
        }

        @Override
        public void onChanged(Change<? extends Object, ? extends Object> change) {
            EventType<ObservableMapChangeEvent<?, ?>> eventType;
            if (change.wasAdded() && change.wasRemoved()) {
                eventType = ObservableMapChangeEvent.UPDATE;
            } else if (change.wasAdded()) {
                eventType = ObservableMapChangeEvent.ADD;
            } else if (change.wasRemoved()) {
                eventType = ObservableMapChangeEvent.REMOVE;
            } else {
                throw new UnsupportedOperationException();
            }

            handler.handle(new ObservableMapChangeEvent<Object, Object>(source,
                eventType, change.getKey(), change.getValueRemoved()));
        }
    }

    // Property model change listener
    private static class PropertyChangeAdapter implements ChangeListener<Object> {
        public final Object source;
        public final EventHandler<PropertyChangeEvent<?>> handler;

        public PropertyChangeAdapter(Object source, EventHandler<PropertyChangeEvent<?>> handler) {
            if (source == null) {
                throw new NullPointerException();
            }

            if (handler == null) {
                throw new NullPointerException();
            }

            this.source = source;
            this.handler = handler;
        }

        @Override
        public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
            handler.handle(new PropertyChangeEvent<Object>(source, oldValue));
        }
    }

    protected URL location;
    protected ResourceBundle resources;

    private ObservableMap<String, Object> namespace = FXCollections.observableHashMap();

    protected Object root = null;
    protected Object controller = null;

    private BuilderFactory builderFactory;
    private Callback<Class<?>, Object> controllerFactory;
    private Charset charset;

    private LinkedList<FXMLLoader> loaders;

    private ClassLoader classLoader = defaultClassLoader;
    private boolean staticLoad = false;
    private LoadListener loadListener = null;
    
    private FXMLLoader parentLoader;

    private XMLStreamReader xmlStreamReader = null;
    private Element current = null;

    private ScriptEngine scriptEngine = null;

    private boolean template = false;

    private LinkedList<String> packages = new LinkedList<String>();
    private HashMap<String, Class<?>> classes = new HashMap<String, Class<?>>();

    private HashMap<String, Field> controllerFields = null;
    private HashMap<String, Method> controllerMethods = null;

    private ScriptEngineManager scriptEngineManager = null;

    private static ClassLoader defaultClassLoader;

    private static final Pattern extraneousWhitespacePattern = Pattern.compile("\\s+");

    public static final String DEFAULT_CHARSET_NAME = "UTF-8";

    public static final String LANGUAGE_PROCESSING_INSTRUCTION = "language";
    public static final String IMPORT_PROCESSING_INSTRUCTION = "import";

    public static final String FX_NAMESPACE_PREFIX = "fx";
    public static final String FX_CONTROLLER_ATTRIBUTE = "controller";
    public static final String FX_ID_ATTRIBUTE = "id";
    public static final String FX_VALUE_ATTRIBUTE = "value";
    public static final String FX_CONSTANT_ATTRIBUTE = "constant";
    public static final String FX_FACTORY_ATTRIBUTE = "factory";

    public static final String INCLUDE_TAG = "include";
    public static final String INCLUDE_SOURCE_ATTRIBUTE = "source";
    public static final String INCLUDE_RESOURCES_ATTRIBUTE = "resources";
    public static final String INCLUDE_CHARSET_ATTRIBUTE = "charset";

    public static final String SCRIPT_TAG = "script";
    public static final String SCRIPT_SOURCE_ATTRIBUTE = "source";
    public static final String SCRIPT_CHARSET_ATTRIBUTE = "charset";

    public static final String DEFINE_TAG = "define";

    public static final String REFERENCE_TAG = "reference";
    public static final String REFERENCE_SOURCE_ATTRIBUTE = "source";

    public static final String ROOT_TAG = "root";
    public static final String ROOT_TYPE_ATTRIBUTE = "type";

    public static final String COPY_TAG = "copy";
    public static final String COPY_SOURCE_ATTRIBUTE = "source";

    public static final String EVENT_HANDLER_PREFIX = "on";
    public static final String EVENT_KEY = "event";
    public static final String CHANGE_EVENT_HANDLER_SUFFIX = "Change";

    public static final String NULL_KEYWORD = "null";

    public static final String ESCAPE_PREFIX = "\\";
    public static final String RELATIVE_PATH_PREFIX = "@";
    public static final String RESOURCE_KEY_PREFIX = "%";
    public static final String EXPRESSION_PREFIX = "$";
    public static final String BINDING_EXPRESSION_PREFIX = "{";
    public static final String BINDING_EXPRESSION_SUFFIX = "}";

    public static final String BI_DIRECTIONAL_BINDING_PREFIX = "#{";
    public static final String BI_DIRECTIONAL_BINDING_SUFFIX = "}";

    public static final String ARRAY_COMPONENT_DELIMITER = ",";

    public static final String LOCATION_KEY = "location";
    public static final String RESOURCES_KEY = "resources";

    public static final String CONTROLLER_METHOD_PREFIX = "#";
    public static final String CONTROLLER_KEYWORD = "controller";
    public static final String CONTROLLER_SUFFIX = "Controller";

    public static final String INITIALIZE_METHOD_NAME = "initialize";

    static {
        defaultClassLoader = Thread.currentThread().getContextClassLoader();

        if (defaultClassLoader == null) {
            throw new NullPointerException();
        }
    }

    /**
     * Creates a new FXMLLoader instance.
     */
    public FXMLLoader() {
        this((URL)null);
    }

    /**
     * Creates a new FXMLLoader instance.
     *
     * @param location
     */
    public FXMLLoader(URL location) {
        this(location, null);
    }

    /**
     * Creates a new FXMLLoader instance.
     *
     * @param location
     * @param resources
     */
    public FXMLLoader(URL location, ResourceBundle resources) {
        this(location, resources, new JavaFXBuilderFactory());
    }

    /**
     * Creates a new FXMLLoader instance.
     *
     * @param location
     * @param resources
     * @param builderFactory
     */
    public FXMLLoader(URL location, ResourceBundle resources, BuilderFactory builderFactory) {
        this(location, resources, builderFactory, null);
    }

    /**
     * Creates a new FXMLLoader instance.
     *
     * @param location
     * @param resources
     * @param builderFactory
     * @param controllerFactory
     */
    public FXMLLoader(URL location, ResourceBundle resources, BuilderFactory builderFactory,
        Callback<Class<?>, Object> controllerFactory) {
        this(location, resources, builderFactory, controllerFactory, Charset.forName(DEFAULT_CHARSET_NAME));
    }

    /**
     * Creates a new FXMLLoader instance.
     *
     * @param charset
     */
    public FXMLLoader(Charset charset) {
        this(null, null, null, null, charset);
    }

    /**
     * Creates a new FXMLLoader instance.
     *
     * @param location
     * @param resources
     * @param builderFactory
     * @param controllerFactory
     * @param charset
     */
    public FXMLLoader(URL location, ResourceBundle resources, BuilderFactory builderFactory,
        Callback<Class<?>, Object> controllerFactory, Charset charset) {
        this(location, resources, builderFactory, controllerFactory, charset,
            new LinkedList<FXMLLoader>());
    }

    /**
     * Creates a new FXMLLoader instance.
     *
     * @param location
     * @param resources
     * @param builderFactory
     * @param controllerFactory
     * @param charset
     * @param loaders
     */
    public FXMLLoader(URL location, ResourceBundle resources, BuilderFactory builderFactory,
        Callback<Class<?>, Object> controllerFactory, Charset charset,
        LinkedList<FXMLLoader> loaders) {
        setLocation(location);
        setResources(resources);
        setBuilderFactory(builderFactory);
        setControllerFactory(controllerFactory);
        setCharset(charset);

        this.loaders = loaders;
    }

    /**
     * Returns the location used to resolve relative path attribute values.
     */
    public URL getLocation() {
        return location;
    }

    /**
     * Sets the location used to resolve relative path attribute values.
     *
     * @param location
     */
    public void setLocation(URL location) {
        this.location = location;
    }

    /**
     * Returns the resources used to resolve resource key attribute values.
     */
    public ResourceBundle getResources() {
        return resources;
    }

    /**
     * Sets the resources used to resolve resource key attribute values.
     *
     * @param resources
     */
    public void setResources(ResourceBundle resources) {
        this.resources = resources;
    }

    /**
     * Returns the namespace used by this loader.
     */
    public ObservableMap<String, Object> getNamespace() {
        return namespace;
    }

    /**
     * Returns the root of the object hierarchy.
     */
    @SuppressWarnings("unchecked")
    public <T> T getRoot() {
        return (T)root;
    }

    /**
     * Sets the root of the object hierarchy. The value passed to this method
     * is used as the value of the <tt>&lt;fx:root&gt;</tt> tag. This method
     * must be called prior to loading the document when using
     * <tt>&lt;fx:root&gt;</tt>.
     *
     * @param root
     * The root of the object hierarchy.
     */
    public void setRoot(Object root) {
        this.root = root;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FXMLLoader) {
            FXMLLoader loader = (FXMLLoader)obj;
            return loader.location.toExternalForm().equals(
                    location.toExternalForm());
        }
        return false;
    }            
    
    private boolean isCyclic(
                            FXMLLoader currentLoader, 
                            FXMLLoader node) {
        if (currentLoader == null) {
            return false;
        }
        if (currentLoader.equals(node)) {
            return true;
        }        
        return isCyclic(currentLoader.parentLoader, node);
    }
        
    /**
     * Returns the controller associated with the root object.
     */
    @SuppressWarnings("unchecked")
    public <T> T getController() {
        return (T)controller;
    }

    /**
     * Sets the controller associated with the root object. The value passed to
     * this method is used as the value of the <tt>fx:controller</tt> attribute.
     * This method must be called prior to loading the document when using
     * controller event handlers when an <tt>fx:controller</tt> attribute is not
     * specified in the document.
     *
     * @param controller
     * The controller to associate with the root object.
     */
    public void setController(Object controller) {
        this.controller = controller;

        if (controller == null) {
            namespace.remove(CONTROLLER_KEYWORD);
        } else {
            namespace.put(CONTROLLER_KEYWORD, controller);
        }

        controllerFields = null;
        controllerMethods = null;
    }

    /**
     * Returns the template flag.
     */
    public boolean isTemplate() {
        return template;
    }

    /**
     * Sets the template flag. Setting this value to <tt>true</tt> can improve
     * performance when using a single loader instance to reload the same FXML
     * document multiple times. See the documentation for the {@link #load()}
     * method for more information.
     *
     * @param template
     * The template flag.
     */
    public void setTemplate(boolean template) {
        this.template = template;
    }

    /**
     * Returns the builder factory used by this loader.
     */
    public BuilderFactory getBuilderFactory() {
        return builderFactory;
    }

    /**
     * Sets the builder factory used by this loader.
     *
     * @param builderFactory
     */
    public void setBuilderFactory(BuilderFactory builderFactory) {
        this.builderFactory = builderFactory;
    }

    /**
     * Returns the controller factory used by this serializer.
     */
    public Callback<Class<?>, Object> getControllerFactory() {
        return controllerFactory;
    }

    /**
     * Sets the controller factory used by this serializer.
     *
     * @param controllerFactory
     */
    public void setControllerFactory(Callback<Class<?>, Object> controllerFactory) {
        this.controllerFactory = controllerFactory;
    }

    /**
     * Returns the character set used by this loader.
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * Sets the charset used by this loader.
     *
     * @param charset
     */
    public void setCharset(Charset charset) {
        if (charset == null) {
            throw new NullPointerException("charset is null.");
        }

        this.charset = charset;
    }

    /**
     * Returns the classloader used by this serializer.
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets the classloader used by this serializer and clears any existing
     * imports (see {@link #setTemplate(boolean)}).
     *
     * @param classLoader
     */
    public void setClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalArgumentException();
        }

        this.classLoader = classLoader;

        clearImports();
    }

    /**
     * Returns the static load flag.
     *
     * @treatAsPrivate
     * @deprecated
     */
    public boolean isStaticLoad() {
        // SB-dependency: RT-21226 has been filed to track this
        return staticLoad;
    }

    /**
     * Sets the static load flag.
     *
     * @param staticLoad
     *
     * @treatAsPrivate
     * @deprecated
     */
    public void setStaticLoad(boolean staticLoad) {
        // SB-dependency: RT-21226 has been filed to track this
        this.staticLoad = staticLoad;
    }

    /**
     * Returns this loader's load listener.
     *
     * @treatAsPrivate
     * @deprecated
     */
    public LoadListener getLoadListener() {
        // SB-dependency: RT-21228 has been filed to track this
        return loadListener;
    }

    /**
     * Sets this loader's load listener.
     *
     * @param loadListener
     *
     * @treatAsPrivate
     * @deprecated
     */
    public void setLoadListener(LoadListener loadListener) {
        // SB-dependency: RT-21228 has been filed to track this
        this.loadListener = loadListener;
    }

    /**
     * Loads an object hierarchy from a FXML document. The location from which
     * the document will be loaded must have been set by a prior call to
     * {@link #setLocation(URL)}.
     * <p>
     * When the "template" flag is set to <tt>false</tt> (the default), this
     * method will clear the imports before loading the document's content.
     * When "template" is <tt>true</tt>, the imports will not be cleared, and
     * the root value will be set to <tt>null</tt> before the content is
     * loaded. This helps improve performance on subsequent loads by
     * eliminating the overhead of loading the classes referred to by the
     * document.
     *
     * @return
     * The loaded object hierarchy.
     */
    public Object load() throws IOException {
        if (location == null) {
            throw new IllegalStateException("Location is not set.");
        }

        InputStream inputStream = null;
        Object value;
        try {
            inputStream = location.openStream();
            value = load(inputStream);
        } catch (IOException exception) {
            logException(exception);
            throw exception;
        } catch (RuntimeException exception) {
            logException(exception);
            throw exception;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        return value;
    }

    /**
     * Loads an object hierarchy from a FXML document.
     *
     * @param inputStream
     * An input stream containing the FXML data to load.
     *
     * @return
     * The loaded object hierarchy.
     */
    @SuppressWarnings("dep-ann")
    public Object load(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            throw new NullPointerException("inputStream is null.");
        }

        if (template) {
            setRoot(null);
        } else {
            clearImports();
        }

        // Initialize the namespace
        namespace.put(LOCATION_KEY, location);
        namespace.put(RESOURCES_KEY, resources);

        // Clear the script engine
        scriptEngine = null;

        // Create the parser
        try {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            xmlInputFactory.setProperty("javax.xml.stream.isCoalescing", true);

            // Some stream readers incorrectly report an empty string as the prefix
            // for the default namespace; correct this as needed
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, charset);
            xmlStreamReader = new StreamReaderDelegate(xmlInputFactory.createXMLStreamReader(inputStreamReader)) {
                @Override
                public String getPrefix() {
                    String prefix = super.getPrefix();

                    if (prefix != null
                        && prefix.length() == 0) {
                        prefix = null;
                    }

                    return prefix;
                }

                @Override
                public String getAttributePrefix(int index) {
                    String attributePrefix = super.getAttributePrefix(index);

                    if (attributePrefix != null
                        && attributePrefix.length() == 0) {
                        attributePrefix = null;
                    }

                    return attributePrefix;
                }
            };
        } catch (XMLStreamException exception) {
            throw new LoadException(exception);
        }

        // Push this loader onto the stack
        loaders.push(this);

        // Parse the XML stream
        try {
            while (xmlStreamReader.hasNext()) {
                int event = xmlStreamReader.next();

                switch (event) {
                    case XMLStreamConstants.PROCESSING_INSTRUCTION: {
                        processProcessingInstruction();
                        break;
                    }

                    case XMLStreamConstants.COMMENT: {
                        processComment();
                        break;
                    }

                    case XMLStreamConstants.START_ELEMENT: {
                        processStartElement();
                        break;
                    }

                    case XMLStreamConstants.END_ELEMENT: {
                        processEndElement();
                        break;
                    }

                    case XMLStreamConstants.CHARACTERS: {
                        processCharacters();
                        break;
                    }
                }
            }
        } catch (XMLStreamException exception) {
            throw new LoadException(exception);
        }

        if (controller != null) {
            if (controller instanceof Initializable) {
                ((Initializable)controller).initialize(location, resources);
            } else {
                // Inject controller fields
                HashMap<String, Field> controllerFields = getControllerFields();

                Field locationField = controllerFields.get(LOCATION_KEY);
                if (locationField != null) {
                    try {
                        locationField.set(controller, location);
                    } catch (IllegalAccessException exception) {
                        // TODO Throw when Initializable is deprecated/removed
                        // throw new LoadException(exception);
                    }
                }

                Field resourcesField = controllerFields.get(RESOURCES_KEY);
                if (resourcesField != null) {
                    try {
                        resourcesField.set(controller, resources);
                    } catch (IllegalAccessException exception) {
                        // TODO Throw when Initializable is deprecated/removed
                        // throw new LoadException(exception);
                    }
                }

                // Initialize the controller
                Method initializeMethod = getControllerMethods().get(INITIALIZE_METHOD_NAME);

                if (initializeMethod != null) {
                    try {
                        MethodUtil.invoke(initializeMethod, controller, new Object [] {});
                    } catch (IllegalAccessException exception) {
                        // TODO Throw when Initializable is deprecated/removed
                        // throw new LoadException(exception);
                    } catch (InvocationTargetException exception) {
                        throw new LoadException(exception);
                    }
                }
            }
        }

        // Pop this loader off of the stack
        loaders.pop();

        // Clear the parser
        xmlStreamReader = null;

        return root;
    }

    private void clearImports() {
        packages.clear();
        classes.clear();
    }

    private void logException(Exception exception) {
        String message = exception.getMessage();
        if (message == null) {
            message = exception.getClass().getName();
        }

        StringBuilder messageBuilder = new StringBuilder(message);
        messageBuilder.append("\n");

        for (FXMLLoader loader : loaders) {
            messageBuilder.append(loader.location.getPath());

            if (loader.current != null) {
                messageBuilder.append(":");
                messageBuilder.append(loader.current.lineNumber);
            }

            messageBuilder.append("\n");
        }

        StackTraceElement[] stackTrace = exception.getStackTrace();
        if (stackTrace != null) {
            for (int i = 0; i < stackTrace.length; i++) {
                messageBuilder.append("  at ");
                messageBuilder.append(stackTrace[i].toString());
                messageBuilder.append("\n");
            }
        }

        System.err.println(messageBuilder.toString());
    }

    /**
     * Returns the current line number.
     *
     * @treatAsPrivate
     * @deprecated
     */
    public int getLineNumber() {
        return xmlStreamReader.getLocation().getLineNumber();
    }

    /**
     * Returns the current parse trace.
     *
     * @treatAsPrivate
     * @deprecated
     */
    public ParseTraceElement[] getParseTrace() {
        ParseTraceElement[] parseTrace = new ParseTraceElement[loaders.size()];

        int i = 0;
        for (FXMLLoader loader : loaders) {
            parseTrace[i++] = new ParseTraceElement(loader.location, (loader.current != null) ?
                loader.current.lineNumber : -1);
        }

        return parseTrace;
    }

    private void processProcessingInstruction() throws LoadException {
        String piTarget = xmlStreamReader.getPITarget().trim();

        if (piTarget.equals(LANGUAGE_PROCESSING_INSTRUCTION)) {
            processLanguage();
        } else if (piTarget.equals(IMPORT_PROCESSING_INSTRUCTION)) {
            processImport();
        }
    }

    private void processLanguage() throws LoadException {
        if (scriptEngine != null) {
            throw new LoadException("Page language already set.");
        }

        String language = xmlStreamReader.getPIData();

        if (loadListener != null) {
            loadListener.readLanguageProcessingInstruction(language);
        }

        if (!staticLoad) {
            ScriptEngineManager scriptEngineManager = getScriptEngineManager();
            scriptEngine = scriptEngineManager.getEngineByName(language);
            scriptEngine.setBindings(scriptEngineManager.getBindings(), ScriptContext.ENGINE_SCOPE);
        }
    }

    private void processImport() throws LoadException {
        String target = xmlStreamReader.getPIData().trim();

        if (loadListener != null) {
            loadListener.readImportProcessingInstruction(target);
        }

        if (target.endsWith(".*")) {
            importPackage(target.substring(0, target.length() - 2));
        } else {
            importClass(target);
        }
    }

    private void processComment() throws LoadException {
        if (loadListener != null) {
            loadListener.readComment(xmlStreamReader.getText());
        }
    }

    private void processStartElement() throws IOException {
        // Create the element
        createElement();

        // Process the start tag
        current.processStartElement();

        // Set the root value
        if (root == null) {
            root = current.value;
        }
    }

    private void createElement() throws IOException {
        String prefix = xmlStreamReader.getPrefix();
        String localName = xmlStreamReader.getLocalName();

        if (prefix == null) {
            int i = localName.lastIndexOf('.');

            if (Character.isLowerCase(localName.charAt(i + 1))) {
                String name = localName.substring(i + 1);

                if (i == -1) {
                    // This is an instance property
                    if (loadListener != null) {
                        loadListener.beginPropertyElement(name, null);
                    }

                    current = new PropertyElement(name, null);
                } else {
                    // This is a static property
                    Class<?> sourceType = getType(localName.substring(0, i));

                    if (sourceType != null) {
                        if (loadListener != null) {
                            loadListener.beginPropertyElement(name, sourceType);
                        }

                        current = new PropertyElement(name, sourceType);
                    } else if (staticLoad) {
                        // The source type was not recognized
                        if (loadListener != null) {
                            loadListener.beginUnknownStaticPropertyElement(localName);
                        }

                        current = new UnknownStaticPropertyElement();
                    } else {
                        throw new LoadException(localName + " is not a valid property.");
                    }
                }
            } else {
                if (current == null && root != null) {
                    throw new LoadException("Root value already specified.");
                }

                Class<?> type = getType(localName);

                if (type != null) {
                    if (loadListener != null) {
                        loadListener.beginInstanceDeclarationElement(type);
                    }

                    current = new InstanceDeclarationElement(type);
                } else if (staticLoad) {
                    // The type was not recognized
                    if (loadListener != null) {
                        loadListener.beginUnknownTypeElement(localName);
                    }

                    current = new UnknownTypeElement();
                } else {
                    throw new LoadException(localName + " is not a valid type.");
                }
            }
        } else if (prefix.equals(FX_NAMESPACE_PREFIX)) {
            if (localName.equals(INCLUDE_TAG)) {
                if (loadListener != null) {
                    loadListener.beginIncludeElement();
                }

                current = new IncludeElement();
            } else if (localName.equals(REFERENCE_TAG)) {
                if (loadListener != null) {
                    loadListener.beginReferenceElement();
                }

                current = new ReferenceElement();
            } else if (localName.equals(COPY_TAG)) {
                if (loadListener != null) {
                    loadListener.beginCopyElement();
                }

                current = new CopyElement();
            } else if (localName.equals(ROOT_TAG)) {
                if (loadListener != null) {
                    loadListener.beginRootElement();
                }

                current = new RootElement();
            } else if (localName.equals(SCRIPT_TAG)) {
                if (loadListener != null) {
                    loadListener.beginScriptElement();
                }

                current = new ScriptElement();
            } else if (localName.equals(DEFINE_TAG)) {
                if (loadListener != null) {
                    loadListener.beginDefineElement();
                }

                current = new DefineElement();
            } else {
                throw new LoadException(prefix + ":" + localName + " is not a valid element.");
            }
        } else {
            throw new LoadException("Unexpected namespace prefix: " + prefix + ".");
        }
    }

    private void processEndElement() throws IOException {
        current.processEndElement();

        if (loadListener != null) {
            loadListener.endElement(current.value);
        }

        // Move up the stack
        current = current.parent;
    }

    private void processCharacters() throws IOException {
        // Process the characters
        if (!xmlStreamReader.isWhiteSpace()) {
            current.processCharacters();
        }
    }

    private void importPackage(String name) throws LoadException {
        packages.add(name);
    }

    private void importClass(String name) throws LoadException {
        try {
            loadType(name, true);
        } catch (ClassNotFoundException exception) {
            throw new LoadException(exception);
        }
    }

    private Class<?> getType(String name) throws LoadException {
        Class<?> type = null;

        if (Character.isLowerCase(name.charAt(0))) {
            // This is a fully-qualified class name
            try {
                type = loadType(name, false);
            } catch (ClassNotFoundException exception) {
                // No-op
            }
        } else {
            // This is an unqualified class name
            type = classes.get(name);

            if (type == null) {
                // The class has not been loaded yet; look it up
                for (String packageName : packages) {
                    try {
                        type = loadTypeForPackage(packageName, name);
                    } catch (ClassNotFoundException exception) {
                        // No-op
                    }

                    if (type != null) {
                        break;
                    }
                }

                if (type != null) {
                    classes.put(name, type);
                }
            }
        }

        return type;
    }

    private Class<?> loadType(String name, boolean cache) throws ClassNotFoundException {
        int i = name.indexOf('.');
        int n = name.length();
        while (i != -1
            && i < n
            && Character.isLowerCase(name.charAt(i + 1))) {
            i = name.indexOf('.', i + 1);
        }

        if (i == -1 || i == n) {
            throw new ClassNotFoundException();
        }

        String packageName = name.substring(0, i);
        String className = name.substring(i + 1);

        Class<?> type = loadTypeForPackage(packageName, className);

        if (cache) {
            classes.put(className, type);
        }

        return type;
    }

    // TODO Rename to loadType() when deprecated static version is removed
    private Class<?> loadTypeForPackage(String packageName, String className) throws ClassNotFoundException {
        return classLoader.loadClass(packageName + "." + className.replace('.', '$'));
    }

    protected HashMap<String, Field> getControllerFields() throws LoadException {
        if (controllerFields == null) {
            controllerFields = new HashMap<String, Field>();

            Class<?> controllerType = controller.getClass();
            Class<?> type = controllerType;

            while (type != Object.class) {
                Field[] fields = FieldUtil.getDeclaredFields(type);

                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];
                    int modifiers = field.getModifiers();

                    // Only add fields that are visible to this controller type
                    if (type == controllerType
                        || (modifiers & Modifier.PRIVATE) == 0) {
                        // Ensure that the field is accessible
                        if ((modifiers & Modifier.PUBLIC) == 0
                            && field.getAnnotation(FXML.class) != null) {
                            try {
                                field.setAccessible(true);
                            } catch (SecurityException exception) {
                                throw new LoadException(exception);
                            }
                        }

                        controllerFields.put(field.getName(), field);
                    }
                }

                type = type.getSuperclass();
            }
        }

        return controllerFields;
    }

    protected HashMap<String, Method> getControllerMethods() throws LoadException {
        if (controllerMethods == null) {
            controllerMethods = new HashMap<String, Method>();

            Class<?> controllerType = controller.getClass();
            Class<?> type = controllerType;

            while (type != Object.class) {
                ReflectUtil.checkPackageAccess(type);
                Method[] methods = type.getDeclaredMethods();

                for (int i = 0; i < methods.length; i++) {
                    Method method = methods[i];
                    int modifiers = method.getModifiers();

                    // Only add methods that are visible to this controller type
                    if (type == controllerType
                        || (modifiers & Modifier.PRIVATE) == 0) {
                        // Ensure that the method is accessible
                        if ((modifiers & Modifier.PUBLIC) == 0
                            && method.getAnnotation(FXML.class) != null) {
                            try {
                                method.setAccessible(true);
                            } catch (SecurityException exception) {
                                throw new LoadException(exception);
                            }
                        }

                        // Add this method to the map if:
                        // a) it is the initialize() method, or
                        // b) it takes a single event argument, or
                        // c) it takes no arguments and a handler with this
                        //    name has not already been defined
                        String methodName = method.getName();
                        Class<?>[] parameterTypes = method.getParameterTypes();

                        if (methodName.equals(INITIALIZE_METHOD_NAME)) {
                            if (parameterTypes.length == 0) {
                                controllerMethods.put(method.getName(), method);
                            }
                        } else if ((parameterTypes.length == 1 && Event.class.isAssignableFrom(parameterTypes[0]))
                            || (parameterTypes.length == 0 && !controllerMethods.containsKey(methodName))) {
                            controllerMethods.put(method.getName(), method);
                        }
                    }
                }

                type = type.getSuperclass();
            }
        }

        return controllerMethods;
    }

    private ScriptEngineManager getScriptEngineManager() {
        if (scriptEngineManager == null) {
            scriptEngineManager = new javax.script.ScriptEngineManager();
            scriptEngineManager.setBindings(new SimpleBindings(namespace));
        }

        return scriptEngineManager;
    }

    /**
     * Loads a type using the default class loader.
     *
     * @param packageName
     * @param className
     *
     * @deprecated
     * This method now delegates to {@link #getDefaultClassLoader()}.
     */
    public static Class<?> loadType(String packageName, String className) throws ClassNotFoundException {
        return loadType(packageName + "." + className.replace('.', '$'));
    }

    /**
     * Loads a type using the default class loader.
     *
     * @param className
     *
     * @deprecated
     * This method now delegates to {@link #getDefaultClassLoader()}.
     */
    public static Class<?> loadType(String className) throws ClassNotFoundException {
        ReflectUtil.checkPackageAccess(className);
        return Class.forName(className, true, defaultClassLoader);
    }

    /**
     * Returns the default class loader.
     */
    public static ClassLoader getDefaultClassLoader() {
        return defaultClassLoader;
    }

    /**
     * Sets the default class loader.
     *
     * @param defaultClassLoader
     * The default class loader to use when loading classes.
     */
    public static void setDefaultClassLoader(ClassLoader defaultClassLoader) {
        if (defaultClassLoader == null) {
            throw new NullPointerException();
        }

        FXMLLoader.defaultClassLoader = defaultClassLoader;
    }

    /**
     * Loads an object hierarchy from a FXML document.
     *
     * @param location
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(URL location) throws IOException {
        return (T)load(location, null);
    }

    /**
     * Loads an object hierarchy from a FXML document.
     *
     * @param location
     * @param resources
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(URL location, ResourceBundle resources) throws IOException {
        return (T)load(location, resources, new JavaFXBuilderFactory());
    }

    /**
     * Loads an object hierarchy from a FXML document.
     *
     * @param location
     * @param resources
     * @param builderFactory
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(URL location, ResourceBundle resources, BuilderFactory builderFactory)
        throws IOException {
        return (T)load(location, resources, builderFactory, null);
    }

    /**
     * Loads an object hierarchy from a FXML document.
     *
     * @param location
     * @param resources
     * @param builderFactory
     * @param controllerFactory
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(URL location, ResourceBundle resources, BuilderFactory builderFactory,
        Callback<Class<?>, Object> controllerFactory) throws IOException {
        return (T)load(location, resources, builderFactory, controllerFactory, Charset.forName(DEFAULT_CHARSET_NAME));
    }

    /**
     * Loads an object hierarchy from a FXML document.
     *
     * @param location
     * @param resources
     * @param builderFactory
     * @param controllerFactory
     * @param charset
     */
    @SuppressWarnings("unchecked")
    public static <T> T load(URL location, ResourceBundle resources, BuilderFactory builderFactory,
        Callback<Class<?>, Object> controllerFactory, Charset charset) throws IOException {
        if (location == null) {
            throw new NullPointerException("Location is required.");
        }

        FXMLLoader fxmlLoader = new FXMLLoader(location, resources, builderFactory, controllerFactory, charset);

        return (T)fxmlLoader.load();
    }
}
