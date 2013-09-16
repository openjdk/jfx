/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.Logging;
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

import com.sun.javafx.fxml.*;
import javafx.beans.DefaultProperty;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.*;
import javafx.event.Event;
import javafx.event.EventHandler;
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
import com.sun.javafx.fxml.expression.Expression;
import com.sun.javafx.fxml.expression.ExpressionValue;
import com.sun.javafx.fxml.expression.KeyPath;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Deque;
import java.util.EnumMap;
import java.util.Locale;
import java.util.StringTokenizer;
import javafx.beans.InvalidationListener;
import sun.reflect.misc.ConstructorUtil;
import sun.reflect.misc.FieldUtil;
import sun.reflect.misc.MethodUtil;
import sun.reflect.misc.ReflectUtil;

/**
 * Loads an object hierarchy from an XML document.
 * @since JavaFX 2.0
 */
public class FXMLLoader {
    // Abstract base class for elements
    private abstract class Element {
        public final Element parent;

        public Object value = null;
        private BeanAdapter valueAdapter = null;

        public final LinkedList<Attribute> eventHandlerAttributes = new LinkedList<Attribute>();
        public final LinkedList<Attribute> instancePropertyAttributes = new LinkedList<Attribute>();
        public final LinkedList<Attribute> staticPropertyAttributes = new LinkedList<Attribute>();
        public final LinkedList<PropertyElement> staticPropertyElements = new LinkedList<PropertyElement>();

        public Element() {
            parent = current;
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
            if (isBindingExpression(value)) {
                // Resolve the expression
                Expression expression;

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

                value = value.substring(BINDING_EXPRESSION_PREFIX.length(),
                        value.length() - 1);
                expression = Expression.valueOf(value);

                // Create the binding
                BeanAdapter targetAdapter = new BeanAdapter(this.value);
                ObservableValue<Object> propertyModel = targetAdapter.getPropertyModel(attribute.name);
                Class<?> type = targetAdapter.getType(attribute.name);

                if (propertyModel instanceof Property<?>) {
                    ((Property<Object>)propertyModel).bind(new ExpressionValue(namespace, expression, type));
                }
            } else if (isBidirectionalBindingExpression(value)) {
                throw new UnsupportedOperationException("This feature is not currently enabled.");
            } else {
                processValue(attribute.sourceType, attribute.name, value);
            }
        }

        private boolean isBindingExpression(String aValue) {
            return aValue.startsWith(BINDING_EXPRESSION_PREFIX)
                   && aValue.endsWith(BINDING_EXPRESSION_SUFFIX);
        }

        private boolean isBidirectionalBindingExpression(String aValue) {
            return aValue.startsWith(BI_DIRECTIONAL_BINDING_PREFIX);
        }

        private boolean processValue(Class sourceType, String propertyName, String aValue)
            throws LoadException {

            boolean processed = false;
                //process list or array first
                if (sourceType == null && isTyped()) {
                    BeanAdapter valueAdapter = getValueAdapter();
                    Class<?> type = valueAdapter.getType(propertyName);

                    if (type == null) {
                        throw new PropertyNotFoundException("Property \"" + propertyName
                            + "\" does not exist" + " or is read-only.");
                    }

                    if (List.class.isAssignableFrom(type)
                        && valueAdapter.isReadOnly(propertyName)) {
                        populateListFromString(valueAdapter, propertyName, aValue);
                        processed = true;
                    } else if (type.isArray()) {
                        applyProperty(propertyName, sourceType,
                                populateArrayFromString(type, aValue));
                        processed = true;
                    }
                }
                if (!processed) {
                    applyProperty(propertyName, sourceType, resolvePrefixedValue(aValue));
                    processed = true;
                }
                return processed;
        }

        /**
         * Resolves value prefixed with RELATIVE_PATH_PREFIX and RESOURCE_KEY_PREFIX.
         */
        private Object resolvePrefixedValue(String aValue) throws LoadException {
            if (aValue.startsWith(ESCAPE_PREFIX)) {
                aValue = aValue.substring(ESCAPE_PREFIX.length());

                if (aValue.length() == 0
                    || !(aValue.startsWith(ESCAPE_PREFIX)
                        || aValue.startsWith(RELATIVE_PATH_PREFIX)
                        || aValue.startsWith(RESOURCE_KEY_PREFIX)
                        || aValue.startsWith(EXPRESSION_PREFIX)
                        || aValue.startsWith(BI_DIRECTIONAL_BINDING_PREFIX))) {
                    throw new LoadException("Invalid escape sequence.");
                }
                return aValue;
            } else if (aValue.startsWith(RELATIVE_PATH_PREFIX)) {
                aValue = aValue.substring(RELATIVE_PATH_PREFIX.length());
                if (aValue.length() == 0) {
                    throw new LoadException("Missing relative path.");
                }
                if (aValue.startsWith(RELATIVE_PATH_PREFIX)) {
                    // The prefix was escaped
                    warnDeprecatedEscapeSequence(RELATIVE_PATH_PREFIX);
                    return aValue;
                } else {
                        if (aValue.charAt(0) == '/') {
                            final URL res = classLoader.getResource(aValue.substring(1));
                            if (res == null) {
                                throw new LoadException("Invalid resource: " + aValue + " not found on the classpath");
                            }
                            return res.toString();
                        } else {
                            try {
                                return new URL(FXMLLoader.this.location, aValue).toString();
                            } catch (MalformedURLException e) {
                                System.err.println(FXMLLoader.this.location + "/" + aValue);
                            }
                        }
                }
            } else if (aValue.startsWith(RESOURCE_KEY_PREFIX)) {
                aValue = aValue.substring(RESOURCE_KEY_PREFIX.length());
                if (aValue.length() == 0) {
                    throw new LoadException("Missing resource key.");
                }
                if (aValue.startsWith(RESOURCE_KEY_PREFIX)) {
                    // The prefix was escaped
                    warnDeprecatedEscapeSequence(RESOURCE_KEY_PREFIX);
                    return aValue;
                } else {
                    // Resolve the resource value
                    if (resources == null) {
                        throw new LoadException("No resources specified.");
                    }
                    if (!resources.containsKey(aValue)) {
                        throw new LoadException("Resource \"" + aValue + "\" not found.");
                    }

                    return resources.getString(aValue);
                }
            } else if (aValue.startsWith(EXPRESSION_PREFIX)) {
                aValue = aValue.substring(EXPRESSION_PREFIX.length());
                if (aValue.length() == 0) {
                    throw new LoadException("Missing expression.");
                }
                if (aValue.startsWith(EXPRESSION_PREFIX)) {
                    // The prefix was escaped
                    warnDeprecatedEscapeSequence(EXPRESSION_PREFIX);
                    return aValue;
                } else if (aValue.equals(NULL_KEYWORD)) {
                    // The attribute value is null
                    return null;
                }
                return Expression.get(namespace, KeyPath.parse(aValue));
            }
            return aValue;
        }

        /**
         * Creates an array of given type and populates it with values from
         * a string where tokens are separated by ARRAY_COMPONENT_DELIMITER.
         * If token is prefixed with RELATIVE_PATH_PREFIX a value added to
         * the array becomes relative to document location.
         */
        private Object populateArrayFromString(
                Class<?>type,
                String stringValue) throws LoadException {

            Object propertyValue = null;
            // Split the string and set the values as an array
            Class<?> componentType = type.getComponentType();

            if (stringValue.length() > 0) {
                String[] values = stringValue.split(ARRAY_COMPONENT_DELIMITER);
                propertyValue = Array.newInstance(componentType, values.length);
                for (int i = 0; i < values.length; i++) {
                    Array.set(propertyValue, i,
                            BeanAdapter.coerce(resolvePrefixedValue(values[i].trim()),
                            type.getComponentType()));
                }
            } else {
                propertyValue = Array.newInstance(componentType, 0);
            }
            return propertyValue;
        }

        /**
         * Populates list with values from a string where tokens are separated
         * by ARRAY_COMPONENT_DELIMITER. If token is prefixed with RELATIVE_PATH_PREFIX
         * a value added to the list becomes relative to document location.
         */
        private void populateListFromString(
                BeanAdapter valueAdapter,
                String listPropertyName,
                String stringValue) throws LoadException {
            // Split the string and add the values to the list
            List<Object> list = (List<Object>)valueAdapter.get(listPropertyName);
            Type listType = valueAdapter.getGenericType(listPropertyName);
            Type itemType = (Class<?>)BeanAdapter.getGenericListItemType(listType);

            if (itemType instanceof ParameterizedType) {
                itemType = ((ParameterizedType)itemType).getRawType();
            }

            if (stringValue.length() > 0) {
                String[] values = stringValue.split(ARRAY_COMPONENT_DELIMITER);

                for (String aValue: values) {
                    aValue = aValue.trim();
                    list.add(
                            BeanAdapter.coerce(resolvePrefixedValue(aValue),
                                               (Class<?>)itemType));
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
        
        private Object getExpressionObject(String handlerValue) throws LoadException{
            if (handlerValue.startsWith(EXPRESSION_PREFIX)) {
                handlerValue = handlerValue.substring(EXPRESSION_PREFIX.length());

                if (handlerValue.length() == 0) {
                    throw new LoadException("Missing expression reference.");
                }

                Object expression = Expression.get(namespace, KeyPath.parse(handlerValue));
                if (expression == null) {
                    throw new LoadException("Unable to resolve expression : $" + handlerValue);
                }
                return expression;
            }
            return null;
        }

        private <T> T getExpressionObjectOfType(String handlerValue, Class<T> type) throws LoadException{
            Object expression = getExpressionObject(handlerValue);
            if (expression != null) {
                if (type.isInstance(expression)) {
                    return (T) expression;
                }
                throw new LoadException("Error resolving \"" + handlerValue +"\" expression."
                        + "Does not point to a " + type.getName());
            }
            return null;
        }

        private MethodHandler getControllerMethodHandle(String handlerName, SupportedType type) throws LoadException{
            if (handlerName.startsWith(CONTROLLER_METHOD_PREFIX)) {
                handlerName = handlerName.substring(CONTROLLER_METHOD_PREFIX.length());

                if (!handlerName.startsWith(CONTROLLER_METHOD_PREFIX)) {
                    if (handlerName.length() == 0) {
                        throw new LoadException("Missing controller method.");
                    }

                    if (controller == null) {
                        throw new LoadException("No controller specified.");
                    }

                    Method method = getControllerMethods().get(type).get(handlerName);
                    if (method == null) {
                        method = getControllerMethods().get(SupportedType.PARAMETERLESS).get(handlerName);
                    }

                    if (method == null) {
                        return null;
                    }

                    return new MethodHandler(controller, method);
                }

            }
            return null;
        }

        public void processEventHandlerAttributes() throws LoadException {
            if (eventHandlerAttributes.size() > 0 && !staticLoad) {
                for (Attribute attribute : eventHandlerAttributes) {
                    String handlerName = attribute.value;
                    if (value instanceof ObservableList && attribute.name.equals(COLLECTION_HANDLER_NAME)) {
                        processObservableListHandler(handlerName);
                    } else if (value instanceof ObservableMap && attribute.name.equals(COLLECTION_HANDLER_NAME)) {
                        processObservableMapHandler(handlerName);
                    } else if (value instanceof ObservableSet && attribute.name.equals(COLLECTION_HANDLER_NAME)) {
                        processObservableSetHandler(handlerName);
                    } else if (attribute.name.endsWith(CHANGE_EVENT_HANDLER_SUFFIX)) {
                        processPropertyHandler(attribute.name, handlerName);
                    } else {
                        EventHandler<? extends Event> eventHandler = null;
                        MethodHandler handler = getControllerMethodHandle(handlerName, SupportedType.EVENT);
                        if (handler != null) {
                            eventHandler = new ControllerMethodEventHandler<>(handler);
                        }

                        if (eventHandler == null) {
                            eventHandler = getExpressionObjectOfType(handlerName, EventHandler.class);
                        }

                        if (eventHandler == null) {
                            if (handlerName.length() == 0 || scriptEngine == null) {
                                throw new LoadException("Error resolving " + attribute.name + "='" + attribute.value
                                        + "', either the event handler is not in the Namespace or there is an error in the script.");
                            }

                            eventHandler = new ScriptEventHandler(handlerName, scriptEngine);
                        }

                        // Add the handler
                        getValueAdapter().put(attribute.name, eventHandler);
                    }
                }
            }
        }

        private void processObservableListHandler(String handlerValue) throws LoadException {
            ObservableList list = (ObservableList)value;
            if (handlerValue.startsWith(CONTROLLER_METHOD_PREFIX)) {
                MethodHandler handler = getControllerMethodHandle(handlerValue, SupportedType.LIST_CHANGE_LISTENER);
                if (handler != null) {
                    list.addListener(new ObservableListChangeAdapter(handler));
                } else {
                    throw new LoadException("Controller method \"" + handlerValue + "\" not found.");
                }
            } else if (handlerValue.startsWith(EXPRESSION_PREFIX)) {
                Object listener = getExpressionObject(handlerValue);
                   if (listener instanceof ListChangeListener) {
                    list.addListener((ListChangeListener) listener);
                } else if (listener instanceof InvalidationListener) {
                    list.addListener((InvalidationListener) listener);
                } else {
                    throw new LoadException("Error resolving \"" + handlerValue + "\" expression."
                            + "Must be either ListChangeListener or InvalidationListener");
                }
            }
        }

        private void processObservableMapHandler(String handlerValue) throws LoadException {
            ObservableMap map = (ObservableMap)value;
            if (handlerValue.startsWith(CONTROLLER_METHOD_PREFIX)) {
                MethodHandler handler = getControllerMethodHandle(handlerValue, SupportedType.MAP_CHANGE_LISTENER);
                if (handler != null) {
                    map.addListener(new ObservableMapChangeAdapter(handler));
                } else {
                    throw new LoadException("Controller method \"" + handlerValue + "\" not found.");
                }
            } else if (handlerValue.startsWith(EXPRESSION_PREFIX)) {
                Object listener = getExpressionObject(handlerValue);
                if (listener instanceof MapChangeListener) {
                    map.addListener((MapChangeListener) listener);
                } else if (listener instanceof InvalidationListener) {
                    map.addListener((InvalidationListener) listener);
                } else {
                    throw new LoadException("Error resolving \"" + handlerValue + "\" expression."
                            + "Must be either MapChangeListener or InvalidationListener");
                }
            }
        }

        private void processObservableSetHandler(String handlerValue) throws LoadException {
            ObservableSet set = (ObservableSet)value;
            if (handlerValue.startsWith(CONTROLLER_METHOD_PREFIX)) {
                MethodHandler handler = getControllerMethodHandle(handlerValue, SupportedType.SET_CHANGE_LISTENER);
                if (handler != null) {
                    set.addListener(new ObservableSetChangeAdapter(handler));
                } else {
                    throw new LoadException("Controller method \"" + handlerValue + "\" not found.");
                }
            } else if (handlerValue.startsWith(EXPRESSION_PREFIX)) {
                Object listener = getExpressionObject(handlerValue);
                if (listener instanceof SetChangeListener) {
                    set.addListener((SetChangeListener) listener);
                } else if (listener instanceof InvalidationListener) {
                    set.addListener((InvalidationListener) listener);
                } else {
                    throw new LoadException("Error resolving \"" + handlerValue + "\" expression."
                            + "Must be either SetChangeListener or InvalidationListener");
                }
            }
        }

        private void processPropertyHandler(String attributeName, String handlerValue) throws LoadException {
            int i = EVENT_HANDLER_PREFIX.length();
            int j = attributeName.length() - CHANGE_EVENT_HANDLER_SUFFIX.length();
            
            if (i != j) {
                String key = Character.toLowerCase(attributeName.charAt(i))
                        + attributeName.substring(i + 1, j);
                
                ObservableValue<Object> propertyModel = getValueAdapter().getPropertyModel(key);
                if (propertyModel == null) {
                    throw new LoadException(value.getClass().getName() + " does not define"
                            + " a property model for \"" + key + "\".");
                }

                if (handlerValue.startsWith(CONTROLLER_METHOD_PREFIX)) {
                    MethodHandler handler = getControllerMethodHandle(handlerValue, SupportedType.PROPERTY_CHANGE_LISTENER);
                    if (handler != null) {
                        propertyModel.addListener(new PropertyChangeAdapter(handler));
                    } else {
                        // Note: this part is solely for purpose of 2.2 backward compatibility where an Event object
                        // has been used instead of usual property change parameters
                        MethodHandler evHandler = getControllerMethodHandle(handlerValue, SupportedType.EVENT);
                        if (evHandler != null) {
                            propertyModel.addListener(new ChangeListener<Object>() {
                                @Override
                                public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
                                    evHandler.invoke(new Event(value, null, Event.ANY));
                                }
                            });
                        } else {
                            throw new LoadException("Controller method \"" + handlerValue + "\" not found.");
                        }
                    }
                } else if (handlerValue.startsWith(EXPRESSION_PREFIX)) {
                    Object listener = getExpressionObject(handlerValue);
                    if (listener instanceof ChangeListener) {
                        propertyModel.addListener((ChangeListener) listener);
                    } else if (listener instanceof InvalidationListener) {
                        propertyModel.addListener((InvalidationListener) listener);
                    } else {
                        throw new LoadException("Error resolving \"" + handlerValue + "\" expression."
                                + "Must be either ChangeListener or InvalidationListener");
                    }
                }
                
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

                // checking version of fx namespace - throw exception if not supported
                String fxNSURI = xmlStreamReader.getNamespaceContext().getNamespaceURI("fx");
                if (fxNSURI != null) {
                    String fxVersion = fxNSURI.substring(fxNSURI.lastIndexOf("/") + 1);
                    if (compareJFXVersions(FX_NAMESPACE_VERSION, fxVersion) < 0) {
                        throw new LoadException("Loading FXML document of version " +
                                fxVersion + " by JavaFX runtime supporting version " + FX_NAMESPACE_VERSION);
                    }
                }

                // checking the version JavaFX API - print warning if not supported
                String defaultNSURI = xmlStreamReader.getNamespaceContext().getNamespaceURI("");
                if (defaultNSURI != null) {
                    String nsVersion = defaultNSURI.substring(defaultNSURI.lastIndexOf("/") + 1);
                    if (compareJFXVersions(JAVAFX_VERSION, nsVersion) < 0) {
                        Logging.getJavaFXLogger().warning("Loading FXML document with JavaFX API of version " +
                                nsVersion + " by JavaFX runtime of version " + JAVAFX_VERSION);
                    }
                }
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
                throw new LoadException("Root hasn't been set. Use method setRoot() before load.");
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
    private static class ControllerMethodEventHandler<T extends Event> implements EventHandler<T> {
        private final MethodHandler handler;

        public ControllerMethodEventHandler(MethodHandler handler) {
            this.handler = handler;
        }

        @Override
        public void handle(T event) {
            handler.invoke(event);
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
            localBindings.putAll(engineBindings);
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
    private static class ObservableListChangeAdapter implements ListChangeListener {
        private final MethodHandler handler;

        public ObservableListChangeAdapter(MethodHandler handler) {
            this.handler = handler;
        }

        @Override
        @SuppressWarnings("unchecked")
        public void onChanged(Change change) {
            if (handler != null) {
                handler.invoke(change);
            }
        }
    }

    // Observable map change listener
    private static class ObservableMapChangeAdapter implements MapChangeListener {
        public final MethodHandler handler;

        public ObservableMapChangeAdapter(MethodHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onChanged(Change change) {
            if (handler != null) {
                handler.invoke(change);
            }
        }
    }

    // Observable set change listener
    private static class ObservableSetChangeAdapter implements SetChangeListener {
        public final MethodHandler handler;

        public ObservableSetChangeAdapter(MethodHandler handler) {
            this.handler = handler;
        }

        @Override
        public void onChanged(Change change) {
            if (handler != null) {
                handler.invoke(change);
            }
        }
    }

    // Property model change listener
    private static class PropertyChangeAdapter implements ChangeListener<Object> {
        public final MethodHandler handler;

        public PropertyChangeAdapter(MethodHandler handler) {
            this.handler = handler;
        }

        @Override
        public void changed(ObservableValue<? extends Object> observable, Object oldValue, Object newValue) {
            handler.invoke(observable, oldValue, newValue);
        }
    }

    private static class MethodHandler {
        private final Object controller;
        private final Method method;
        private final boolean typed;

        private MethodHandler(Object controller, Method method) {
            this.method = method;
            this.controller = controller;
            this.typed = (method.getParameterTypes().length > 0);
        }

        public void invoke(Object... params) {
            try {
                if (typed) {
                    MethodUtil.invoke(method, controller, params);
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

    private URL location;
    private ResourceBundle resources;

    private ObservableMap<String, Object> namespace = FXCollections.observableHashMap();

    private Object root = null;
    private Object controller = null;

    private BuilderFactory builderFactory;
    private Callback<Class<?>, Object> controllerFactory;
    private Charset charset;

    private final LinkedList<FXMLLoader> loaders;

    private ClassLoader classLoader = defaultClassLoader;
    private boolean staticLoad = false;
    private LoadListener loadListener = null;

    private FXMLLoader parentLoader;

    private XMLStreamReader xmlStreamReader = null;
    private Element current = null;

    private ScriptEngine scriptEngine = null;

    private boolean template = false;

    private List<String> packages = new LinkedList<String>();
    private Map<String, Class<?>> classes = new HashMap<String, Class<?>>();

    private Map<String, Field> controllerFields = null;
    private Map<SupportedType, Map<String, Method>> controllerMethods = null;

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
    /**
     * @since JavaFX 2.2
     */
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

    /**
     * @since JavaFX 2.2
     */
    public static final String ROOT_TAG = "root";
    /**
     * @since JavaFX 2.2
     */
    public static final String ROOT_TYPE_ATTRIBUTE = "type";

    public static final String COPY_TAG = "copy";
    public static final String COPY_SOURCE_ATTRIBUTE = "source";

    public static final String EVENT_HANDLER_PREFIX = "on";
    public static final String EVENT_KEY = "event";
    public static final String CHANGE_EVENT_HANDLER_SUFFIX = "Change";
    private static final String COLLECTION_HANDLER_NAME = EVENT_HANDLER_PREFIX + CHANGE_EVENT_HANDLER_SUFFIX;

    public static final String NULL_KEYWORD = "null";

    /**
     * @since JavaFX 2.1
     */
    public static final String ESCAPE_PREFIX = "\\";
    public static final String RELATIVE_PATH_PREFIX = "@";
    public static final String RESOURCE_KEY_PREFIX = "%";
    public static final String EXPRESSION_PREFIX = "$";
    public static final String BINDING_EXPRESSION_PREFIX = "${";
    public static final String BINDING_EXPRESSION_SUFFIX = "}";

    /**
     * @since JavaFX 2.1
     */
    public static final String BI_DIRECTIONAL_BINDING_PREFIX = "#{";
    /**
     * @since JavaFX 2.1
     */
    public static final String BI_DIRECTIONAL_BINDING_SUFFIX = "}";

    /**
     * @since JavaFX 2.1
     */
    public static final String ARRAY_COMPONENT_DELIMITER = ",";

    /**
     * @since JavaFX 2.2
     */
    public static final String LOCATION_KEY = "location";
    /**
     * @since JavaFX 2.2
     */
    public static final String RESOURCES_KEY = "resources";

    public static final String CONTROLLER_METHOD_PREFIX = "#";
    /**
     * @since JavaFX 2.1
     */
    public static final String CONTROLLER_KEYWORD = "controller";
    /**
     * @since JavaFX 2.2
     */
    public static final String CONTROLLER_SUFFIX = "Controller";

    /**
     * @since JavaFX 2.2
     */
    public static final String INITIALIZE_METHOD_NAME = "initialize";

    /**
     * @since JavaFX 8.0
     */
    public static final String JAVAFX_VERSION;

    /**
     * @since JavaFX 8.0
     */
    public static final String FX_NAMESPACE_VERSION = "1";

    static {
        defaultClassLoader = Thread.currentThread().getContextClassLoader();

        if (defaultClassLoader == null) {
            throw new NullPointerException();
        }

        JAVAFX_VERSION = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("javafx.version");
            }
        });
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
     * @since JavaFX 2.1
     */
    public FXMLLoader(URL location) {
        this(location, null);
    }

    /**
     * Creates a new FXMLLoader instance.
     *
     * @param location
     * @param resources
     * @since JavaFX 2.1
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
     * @since JavaFX 2.1
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
     * @since JavaFX 2.1
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
     * @since JavaFX 2.1
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
     * @since JavaFX 2.1
     */
    public FXMLLoader(URL location, ResourceBundle resources, BuilderFactory builderFactory,
        Callback<Class<?>, Object> controllerFactory, Charset charset,
        LinkedList<FXMLLoader> loaders) {
        setLocation(location);
        setResources(resources);
        setBuilderFactory(builderFactory);
        setControllerFactory(controllerFactory);
        setCharset(charset);

        this.loaders = new LinkedList(loaders);
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
     * @since JavaFX 2.2
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
     * @since JavaFX 2.2
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
     * @since JavaFX 8.0
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
     * @since JavaFX 8.0
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
     * @since JavaFX 2.1
     */
    public Callback<Class<?>, Object> getControllerFactory() {
        return controllerFactory;
    }

    /**
     * Sets the controller factory used by this serializer.
     *
     * @param controllerFactory
     * @since JavaFX 2.1
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
     * @since JavaFX 2.1
     */
    public void setCharset(Charset charset) {
        if (charset == null) {
            throw new NullPointerException("charset is null.");
        }

        this.charset = charset;
    }

    /**
     * Returns the classloader used by this serializer.
     * @since JavaFX 2.1
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Sets the classloader used by this serializer and clears any existing
     * imports (see {@link #setTemplate(boolean)}).
     *
     * @param classLoader
     * @since JavaFX 2.1
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
     * @since JavaFX 2.1
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
                Map<String, Field> controllerFields = getControllerFields();

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
                Method initializeMethod = getControllerMethods().get(SupportedType.PARAMETERLESS).
                        get(INITIALIZE_METHOD_NAME);

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
                messageBuilder.append(loader.getLineNumber());
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
     * @since JavaFX 2.2
     */
    public int getLineNumber() {
        return xmlStreamReader.getLocation().getLineNumber();
    }

    /**
     * Returns the current parse trace.
     *
     * @treatAsPrivate
     * @deprecated
     * @since JavaFX 2.1
     */
    public ParseTraceElement[] getParseTrace() {
        ParseTraceElement[] parseTrace = new ParseTraceElement[loaders.size()];

        int i = 0;
        for (FXMLLoader loader : loaders) {
            parseTrace[i++] = new ParseTraceElement(loader.location, (loader.current != null) ?
                loader.getLineNumber() : -1);
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

    private Map<String, Field> getControllerFields() throws LoadException {
        if (controllerFields == null) {
            controllerFields = new HashMap<>();

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
    
    private static enum SupportedType {
        PARAMETERLESS {

            @Override
            protected boolean methodIsOfType(Method m) {
                return m.getParameterCount() == 0;
            }
          
        },
        EVENT {

            @Override
            protected boolean methodIsOfType(Method m) {
                return m.getParameterCount() == 1 &&
                        m.getParameterTypes()[0].isAssignableFrom(Event.class);
            }
            
        },
        LIST_CHANGE_LISTENER {

            @Override
            protected boolean methodIsOfType(Method m) {
                return m.getParameterCount() == 1 &&
                        m.getParameterTypes()[0].equals(ListChangeListener.Change.class);
            }
            
        },
        MAP_CHANGE_LISTENER {

            @Override
            protected boolean methodIsOfType(Method m) {
                return m.getParameterCount() == 1 &&
                        m.getParameterTypes()[0].equals(MapChangeListener.Change.class);
            }
            
        },
        SET_CHANGE_LISTENER {

            @Override
            protected boolean methodIsOfType(Method m) {
                return m.getParameterCount() == 1 &&
                        m.getParameterTypes()[0].equals(SetChangeListener.Change.class);
            }
            
        },
        PROPERTY_CHANGE_LISTENER {

            @Override
            protected boolean methodIsOfType(Method m) {
                return m.getParameterCount() == 3 &&
                        ObservableValue.class.isAssignableFrom(m.getParameterTypes()[0])
                        && m.getParameterTypes()[1].equals(m.getParameterTypes()[2]);
            }
            
        };
        
        protected abstract boolean methodIsOfType(Method m);
    }

    private SupportedType toSupportedType(Method m) {
        for (SupportedType t : SupportedType.values()) {
            if (t.methodIsOfType(m)) {
                return t;
            }
        }
        return null;
    }

    private Map<SupportedType, Map<String, Method>> getControllerMethods() throws LoadException {
        if (controllerMethods == null) {
            controllerMethods = new EnumMap<>(SupportedType.class);
            for (SupportedType t: SupportedType.values()) {
                controllerMethods.put(t, new HashMap<String, Method>());
            }

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
                        SupportedType convertedType;

                        if ((convertedType = toSupportedType(method)) != null) {
                            controllerMethods.get(convertedType).put(methodName, method);
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
     * @since JavaFX 2.1
     */
    public static ClassLoader getDefaultClassLoader() {
        return defaultClassLoader;
    }

    /**
     * Sets the default class loader.
     *
     * @param defaultClassLoader
     * The default class loader to use when loading classes.
     * @since JavaFX 2.1
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
     * @since JavaFX 2.1
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
     * @since JavaFX 2.1
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

    /**
     * Utility method for comparing two JavaFX version strings (such as 2.2.5, 8.0.0-ea)
     * @param rtVer String representation of JavaFX runtime version, including - or _ appendix
     * @param nsVer String representation of JavaFX version to compare against runtime version
     * @return number &lt; 0 if runtime version is lower, 0 when both versions are the same,
     *          number &gt; 0 if runtime is higher version
     */
    static int compareJFXVersions(String rtVer, String nsVer) {

        int retVal = 0;

        if (rtVer == null || "".equals(rtVer) ||
            nsVer == null || "".equals(nsVer)) {
            return retVal;
        }

        if (rtVer.equals(nsVer)) {
            return retVal;
        }

        // version string can contain '-'
        int dashIndex = rtVer.indexOf("-");
        if (dashIndex > 0) {
            rtVer = rtVer.substring(0, dashIndex);
        }

        // or "_"
        int underIndex = rtVer.indexOf("_");
        if (underIndex > 0) {
            rtVer = rtVer.substring(0, underIndex);
        }

        // do not try to compare if the string is not valid version format
        if (!Pattern.matches("^(\\d+)(\\.\\d+)*$", rtVer) ||
            !Pattern.matches("^(\\d+)(\\.\\d+)*$", nsVer)) {
            return retVal;
        }

        StringTokenizer nsVerTokenizer = new StringTokenizer(nsVer, ".");
        StringTokenizer rtVerTokenizer = new StringTokenizer(rtVer, ".");
        int nsDigit = 0, rtDigit = 0;
        boolean rtVerEnd = false;

        while (nsVerTokenizer.hasMoreTokens() && retVal == 0) {
            nsDigit = Integer.parseInt(nsVerTokenizer.nextToken());
            if (rtVerTokenizer.hasMoreTokens()) {
                rtDigit = Integer.parseInt(rtVerTokenizer.nextToken());
                retVal = rtDigit - nsDigit;
            } else {
                rtVerEnd = true;
                break;
            }
        }

        if (rtVerTokenizer.hasMoreTokens() && retVal == 0) {
            rtDigit = Integer.parseInt(rtVerTokenizer.nextToken());
            if (rtDigit > 0) {
                retVal = 1;
            }
        }

        if (rtVerEnd) {
            if (nsDigit > 0) {
                retVal = -1;
            } else {
                while (nsVerTokenizer.hasMoreTokens()) {
                    nsDigit = Integer.parseInt(nsVerTokenizer.nextToken());
                    if (nsDigit > 0) {
                        retVal = -1;
                        break;
                    }
                }
            }
        }

        return retVal;
    }

}
