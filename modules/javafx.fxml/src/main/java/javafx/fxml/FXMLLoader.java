/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.javafx.util.Logging;
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
import javafx.beans.InvalidationListener;
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
import javax.script.Compilable;
import javax.script.CompiledScript;
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
import com.sun.javafx.fxml.ParseTraceElement;
import com.sun.javafx.fxml.PropertyNotFoundException;
import com.sun.javafx.fxml.expression.Expression;
import com.sun.javafx.fxml.expression.ExpressionValue;
import com.sun.javafx.fxml.expression.KeyPath;
import static com.sun.javafx.FXPermissions.MODIFY_FXML_CLASS_LOADER_PERMISSION;
import com.sun.javafx.fxml.FXMLLoaderHelper;
import com.sun.javafx.fxml.MethodHelper;
import java.net.MalformedURLException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.EnumMap;
import java.util.Locale;
import java.util.StringTokenizer;
import com.sun.javafx.reflect.ConstructorUtil;
import com.sun.javafx.reflect.MethodUtil;
import com.sun.javafx.reflect.ReflectUtil;

/**
 * Loads an object hierarchy from an XML document.
 * For more information, see the
 * <a href="doc-files/introduction_to_fxml.html">Introduction to FXML</a>
 * document.
 *
 * @since JavaFX 2.0
 */
public class FXMLLoader {

    // Indicates permission to get the ClassLoader
    private static final RuntimePermission GET_CLASSLOADER_PERMISSION =
        new RuntimePermission("getClassLoader");

    // Instance of StackWalker used to get caller class (must be private)
    @SuppressWarnings("removal")
    private static final StackWalker walker =
        AccessController.doPrivileged((PrivilegedAction<StackWalker>) () ->
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE));

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
                throw constructLoadException("Cannot set value on this element.");
            }

            // Apply value to this element's properties
            Class<?> type = this.value.getClass();
            DefaultProperty defaultProperty = type.getAnnotation(DefaultProperty.class);
            if (defaultProperty == null) {
                throw constructLoadException("Element does not define a default property.");
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
            throw constructLoadException("Unexpected characters in input stream.");
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
                            throw constructLoadException(localName + " is not a valid attribute.");
                        }
                    }

                }
            } else {
                throw constructLoadException(prefix + ":" + localName
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
                    throw constructLoadException("Cannot bind to static property.");
                }

                if (!isTyped()) {
                    throw constructLoadException("Cannot bind to untyped object.");
                }

                // TODO We may want to identify binding properties in processAttribute()
                // and apply them after build() has been called
                if (this.value instanceof Builder) {
                    throw constructLoadException("Cannot bind to builder property.");
                }

                if (!isStaticLoad()) {
                    value = value.substring(BINDING_EXPRESSION_PREFIX.length(),
                            value.length() - 1);
                    expression = Expression.valueOf(value);

                    // Create the binding
                    BeanAdapter targetAdapter = new BeanAdapter(this.value);
                    ObservableValue<Object> propertyModel = targetAdapter.getPropertyModel(attribute.name);
                    Class<?> type = targetAdapter.getType(attribute.name);

                    if (propertyModel instanceof Property<?>) {
                        ((Property<Object>) propertyModel).bind(new ExpressionValue(namespace, expression, type));
                    }
                }
            } else if (isBidirectionalBindingExpression(value)) {
                throw constructLoadException(new UnsupportedOperationException("This feature is not currently enabled."));
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
                    throw constructLoadException("Invalid escape sequence.");
                }
                return aValue;
            } else if (aValue.startsWith(RELATIVE_PATH_PREFIX)) {
                aValue = aValue.substring(RELATIVE_PATH_PREFIX.length());
                if (aValue.length() == 0) {
                    throw constructLoadException("Missing relative path.");
                }
                if (aValue.startsWith(RELATIVE_PATH_PREFIX)) {
                    // The prefix was escaped
                    warnDeprecatedEscapeSequence(RELATIVE_PATH_PREFIX);
                    return aValue;
                } else {
                        if (aValue.charAt(0) == '/') {
                            // FIXME: JIGSAW -- use Class.getResourceAsStream if resource is in a module
                            final URL res = getClassLoader().getResource(aValue.substring(1));
                            if (res == null) {
                                throw constructLoadException("Invalid resource: " + aValue + " not found on the classpath");
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
                    throw constructLoadException("Missing resource key.");
                }
                if (aValue.startsWith(RESOURCE_KEY_PREFIX)) {
                    // The prefix was escaped
                    warnDeprecatedEscapeSequence(RESOURCE_KEY_PREFIX);
                    return aValue;
                } else {
                    // Resolve the resource value
                    if (resources == null) {
                        throw constructLoadException("No resources specified.");
                    }
                    if (!resources.containsKey(aValue)) {
                        throw constructLoadException("Resource \"" + aValue + "\" not found.");
                    }

                    return resources.getString(aValue);
                }
            } else if (aValue.startsWith(EXPRESSION_PREFIX)) {
                aValue = aValue.substring(EXPRESSION_PREFIX.length());
                if (aValue.length() == 0) {
                    throw constructLoadException("Missing expression.");
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
                    throw constructLoadException("Missing expression reference.");
                }

                Object expression = Expression.get(namespace, KeyPath.parse(handlerValue));
                if (expression == null) {
                    throw constructLoadException("Unable to resolve expression : $" + handlerValue);
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
                throw constructLoadException("Error resolving \"" + handlerValue +"\" expression."
                        + "Does not point to a " + type.getName());
            }
            return null;
        }

        private MethodHandler getControllerMethodHandle(String handlerName, SupportedType... types) throws LoadException {
            if (handlerName.startsWith(CONTROLLER_METHOD_PREFIX)) {
                handlerName = handlerName.substring(CONTROLLER_METHOD_PREFIX.length());

                if (!handlerName.startsWith(CONTROLLER_METHOD_PREFIX)) {
                    if (handlerName.length() == 0) {
                        throw constructLoadException("Missing controller method.");
                    }

                    if (controller == null) {
                        throw constructLoadException("No controller specified.");
                    }

                    for (SupportedType t : types) {
                        Method method = controllerAccessor
                                            .getControllerMethods()
                                            .get(t)
                                            .get(handlerName);
                        if (method != null) {
                            return new MethodHandler(controller, method, t);
                        }
                    }
                    Method method = controllerAccessor
                                        .getControllerMethods()
                                        .get(SupportedType.PARAMETERLESS)
                                        .get(handlerName);
                    if (method != null) {
                        return new MethodHandler(controller, method, SupportedType.PARAMETERLESS);
                    }

                    return null;

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
                                throw constructLoadException("Error resolving " + attribute.name + "='" + attribute.value
                                        + "', either the event handler is not in the Namespace or there is an error in the script.");
                            }
                            eventHandler = new ScriptEventHandler(handlerName, scriptEngine, location.getPath()
                                        + "-" + attribute.name  + "_attribute_in_element_ending_at_line_"  + getLineNumber());
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
                    throw constructLoadException("Controller method \"" + handlerValue + "\" not found.");
                }
            } else if (handlerValue.startsWith(EXPRESSION_PREFIX)) {
                Object listener = getExpressionObject(handlerValue);
                   if (listener instanceof ListChangeListener) {
                    list.addListener((ListChangeListener) listener);
                } else if (listener instanceof InvalidationListener) {
                    list.addListener((InvalidationListener) listener);
                } else {
                    throw constructLoadException("Error resolving \"" + handlerValue + "\" expression."
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
                    throw constructLoadException("Controller method \"" + handlerValue + "\" not found.");
                }
            } else if (handlerValue.startsWith(EXPRESSION_PREFIX)) {
                Object listener = getExpressionObject(handlerValue);
                if (listener instanceof MapChangeListener) {
                    map.addListener((MapChangeListener) listener);
                } else if (listener instanceof InvalidationListener) {
                    map.addListener((InvalidationListener) listener);
                } else {
                    throw constructLoadException("Error resolving \"" + handlerValue + "\" expression."
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
                    throw constructLoadException("Controller method \"" + handlerValue + "\" not found.");
                }
            } else if (handlerValue.startsWith(EXPRESSION_PREFIX)) {
                Object listener = getExpressionObject(handlerValue);
                if (listener instanceof SetChangeListener) {
                    set.addListener((SetChangeListener) listener);
                } else if (listener instanceof InvalidationListener) {
                    set.addListener((InvalidationListener) listener);
                } else {
                    throw constructLoadException("Error resolving \"" + handlerValue + "\" expression."
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
                    throw constructLoadException(value.getClass().getName() + " does not define"
                            + " a property model for \"" + key + "\".");
                }

                if (handlerValue.startsWith(CONTROLLER_METHOD_PREFIX)) {
                    final MethodHandler handler = getControllerMethodHandle(handlerValue, SupportedType.PROPERTY_CHANGE_LISTENER, SupportedType.EVENT);
                    if (handler != null) {
                        if (handler.type == SupportedType.EVENT) {
                            // Note: this part is solely for purpose of 2.2 backward compatibility where an Event object
                            // has been used instead of usual property change parameters
                            propertyModel.addListener(new ChangeListener<Object>() {
                                @Override
                                public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
                                    handler.invoke(new Event(value, null, Event.ANY));
                                }
                            });
                        } else {
                            propertyModel.addListener(new PropertyChangeAdapter(handler));
                        }
                    } else {
                    throw constructLoadException("Controller method \"" + handlerValue + "\" not found.");
                    }
                } else if (handlerValue.startsWith(EXPRESSION_PREFIX)) {
                    Object listener = getExpressionObject(handlerValue);
                    if (listener instanceof ChangeListener) {
                        propertyModel.addListener((ChangeListener) listener);
                    } else if (listener instanceof InvalidationListener) {
                        propertyModel.addListener((InvalidationListener) listener);
                    } else {
                        throw constructLoadException("Error resolving \"" + handlerValue + "\" expression."
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
                        throw constructLoadException("Loading FXML document of version " +
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
                injectFields(fx_id, value);
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
                throw constructLoadException(type.getName() + " does not have a default property.");
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
                        throw constructLoadException("Invalid identifier.");
                    }

                    for (int i = 0, n = value.length(); i < n; i++) {
                        if (!Character.isJavaIdentifierPart(value.charAt(i))) {
                            throw constructLoadException("Invalid identifier.");
                        }
                    }

                    fx_id = value;

                } else if (localName.equals(FX_CONTROLLER_ATTRIBUTE)) {
                    if (current.parent != null) {
                        throw constructLoadException(FX_NAMESPACE_PREFIX + ":" + FX_CONTROLLER_ATTRIBUTE
                            + " can only be applied to root element.");
                    }

                    if (controller != null) {
                        throw constructLoadException("Controller value already specified.");
                    }

                    if (!staticLoad) {
                        Class<?> type;
                        try {
                            type = getClassLoader().loadClass(value);
                        } catch (ClassNotFoundException exception) {
                            throw constructLoadException(exception);
                        }

                        try {
                            if (controllerFactory == null) {
                                ReflectUtil.checkPackageAccess(type);
                                setController(type.getDeclaredConstructor().newInstance());
                            } else {
                                setController(controllerFactory.call(type));
                            }
                        } catch (Exception e) {
                            throw constructLoadException(e);
                        }
                    }
                } else {
                    throw constructLoadException("Invalid attribute.");
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
                    throw constructLoadException(exception);
                }

                try {
                    value = MethodHelper.invoke(factoryMethod, null, new Object [] {});
                } catch (IllegalAccessException exception) {
                    throw constructLoadException(exception);
                } catch (InvocationTargetException exception) {
                    throw constructLoadException(exception);
                }
            } else {
                value = (builderFactory == null) ? null : builderFactory.getBuilder(type);

                if (value == null) {
                    value = DEFAULT_BUILDER_FACTORY.getBuilder(type);
                }

                if (value == null) {
                    try {
                        ReflectUtil.checkPackageAccess(type);
                        value = type.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw constructLoadException(e);
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
                throw constructLoadException(INCLUDE_SOURCE_ATTRIBUTE + " is required.");
            }

            URL location;
            final ClassLoader cl = getClassLoader();
            if (source.charAt(0) == '/') {
            // FIXME: JIGSAW -- use Class.getResourceAsStream if resource is in a module
                location = cl.getResource(source.substring(1));
                if (location == null) {
                    throw constructLoadException("Cannot resolve path: " + source);
                }
            } else {
                if (FXMLLoader.this.location == null) {
                    throw constructLoadException("Base location is undefined.");
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
            fxmlLoader.setClassLoader(cl);
            fxmlLoader.setStaticLoad(staticLoad);

            Object value = fxmlLoader.loadImpl(callerClass);

            if (fx_id != null) {
                String id = this.fx_id + CONTROLLER_SUFFIX;
                Object controller = fxmlLoader.getController();

                namespace.put(id, controller);
                injectFields(id, controller);
            }

            return value;
        }
    }

    private void injectFields(String fieldName, Object value) throws LoadException {
        if (controller != null && fieldName != null) {
            List<Field> fields = controllerAccessor.getControllerFields().get(fieldName);
            if (fields != null) {
                try {
                    for (Field f : fields) {
                        f.set(controller, value);
                    }
                } catch (IllegalAccessException exception) {
                    throw constructLoadException(exception);
                }
            }
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
                throw constructLoadException(REFERENCE_SOURCE_ATTRIBUTE + " is required.");
            }

            KeyPath path = KeyPath.parse(source);
            if (!Expression.isDefined(namespace, path)) {
                throw constructLoadException("Value \"" + source + "\" does not exist.");
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
                throw constructLoadException(COPY_SOURCE_ATTRIBUTE + " is required.");
            }

            KeyPath path = KeyPath.parse(source);
            if (!Expression.isDefined(namespace, path)) {
                throw constructLoadException("Value \"" + source + "\" does not exist.");
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
                    throw constructLoadException(exception);
                } catch (IllegalAccessException exception) {
                    throw constructLoadException(exception);
                } catch (InvocationTargetException exception) {
                    throw constructLoadException(exception);
                }
            } else {
                throw constructLoadException("Can't copy value " + sourceValue + ".");
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
                throw constructLoadException(ROOT_TYPE_ATTRIBUTE + " is required.");
            }

            Class<?> type = getType(this.type);

            if (type == null) {
                throw constructLoadException(this.type + " is not a valid type.");
            }

            Object value;
            if (root == null) {
                if (staticLoad) {
                    value = (builderFactory == null) ? null : builderFactory.getBuilder(type);

                    if (value == null) {
                        value = DEFAULT_BUILDER_FACTORY.getBuilder(type);
                    }

                    if (value == null) {
                        try {
                            ReflectUtil.checkPackageAccess(type);
                            value = type.getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            throw constructLoadException(e);
                        }
                    }
                    root = value;
                } else {
                    throw constructLoadException("Root hasn't been set. Use method setRoot() before load.");
                }
            } else {
                if (!type.isAssignableFrom(root.getClass())) {
                    throw constructLoadException("Root is not an instance of "
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
                throw constructLoadException("Invalid root element.");
            }

            if (parent.value == null) {
                throw constructLoadException("Parent element does not support property elements.");
            }

            this.name = name;
            this.sourceType = sourceType;

            if (sourceType == null) {
                // The element represents an instance property
                if (name.startsWith(EVENT_HANDLER_PREFIX)) {
                    throw constructLoadException("\"" + name + "\" is not a valid element name.");
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
                        throw constructLoadException("Invalid property.");
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
                throw constructLoadException("Attributes are not supported for writable property elements.");
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
            String text = xmlStreamReader.getText();
            text = extraneousWhitespacePattern.matcher(text).replaceAll(" ").trim();

            if (readOnly) {
                if (isCollection()) {
                    add(text);
                } else {
                    super.processCharacters();
                }
            } else {
                set(text);
            }
        }
    }

    // Element representing an unknown static property
    private class UnknownStaticPropertyElement extends Element {
        public UnknownStaticPropertyElement() throws LoadException {
            if (parent == null) {
                throw constructLoadException("Invalid root element.");
            }

            if (parent.value == null) {
                throw constructLoadException("Parent element does not support property elements.");
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
                    throw constructLoadException("Cannot determine type of script \""
                        + source + "\".");
                }

                String extension = source.substring(i + 1);
                ScriptEngine engine;
                final ClassLoader cl = getClassLoader();
                if (scriptEngine != null && scriptEngine.getFactory().getExtensions().contains(extension)) {
                    // If we have a page language and it's engine supports the extension, use the same engine
                    engine = scriptEngine;
                } else {
                    ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();
                    try {
                        Thread.currentThread().setContextClassLoader(cl);
                        ScriptEngineManager scriptEngineManager = getScriptEngineManager();
                        engine = scriptEngineManager.getEngineByExtension(extension);
                    } finally {
                        Thread.currentThread().setContextClassLoader(oldLoader);
                    }
                }

                if (engine == null) {
                    throw constructLoadException("Unable to locate scripting engine for"
                        + " extension " + extension + ".");
                }

                try {
                    URL location;
                    if (source.charAt(0) == '/') {
                        // FIXME: JIGSAW -- use Class.getResourceAsStream if resource is in a module
                        location = cl.getResource(source.substring(1));
                    } else {
                        if (FXMLLoader.this.location == null) {
                            throw constructLoadException("Base location is undefined.");
                        }

                        location = new URL(FXMLLoader.this.location, source);
                    }
                    Bindings engineBindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
                    String filename = location.getPath();
                    engineBindings.put(engine.FILENAME, filename);

                    InputStreamReader scriptReader = null;
                    String script = null;
                    try {
                        scriptReader = new InputStreamReader(location.openStream(), charset);
                        StringBuilder sb = new StringBuilder();
                        final int bufSize = 4096;
                        char[] charBuffer = new char[bufSize];
                        int n;
                        do {
                            n = scriptReader.read(charBuffer,0,bufSize);
                            if (n > 0) {
                                sb.append(new String(charBuffer,0,n));
                          }
                        } while (n > -1);
                        script = sb.toString();
                    } catch (IOException exception) {
                        throw constructLoadException(exception);
                    } finally {
                        if (scriptReader != null) {
                            scriptReader.close();
                        }
                    }
                    try {
                        if (engine instanceof Compilable && compileScript) {
                            CompiledScript compiledScript = null;
                            try {
                                compiledScript = ((Compilable) engine).compile(script);
                            } catch (ScriptException compileExc) {
                                Logging.getJavaFXLogger().warning(filename + ": compiling caused \"" + compileExc +
                                    "\", falling back to evaluating script in uncompiled mode");
                            }
                            if (compiledScript != null) {
                                compiledScript.eval();
                            } else { // fallback to uncompiled mode
                                engine.eval(script);
                            }
                        } else {
                            engine.eval(script);
                        }
                    } catch (ScriptException exception) {
                        System.err.println(filename + ": caused ScriptException");
                        exception.printStackTrace();
                    }
                }
                catch (IOException exception) {
                  throw constructLoadException(exception);
                }
            }
        }

        @Override
        public void processEndElement() throws IOException {
            super.processEndElement();

            if (value != null && !staticLoad) {
                // Evaluate the script
                String filename = null;
                try {
                    Bindings engineBindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
                    String script = (String) value;
                    filename = location.getPath() + "-script_starting_at_line_"
                                       + (getLineNumber() - (int) script.codePoints().filter(c -> c == '\n').count());
                    engineBindings.put(scriptEngine.FILENAME, filename);
                    if (scriptEngine instanceof Compilable && compileScript) {
                        CompiledScript compiledScript = null;
                        try {
                            compiledScript = ((Compilable) scriptEngine).compile(script);
                        } catch (ScriptException compileExc) {
                            Logging.getJavaFXLogger().warning(filename + ": compiling caused \"" + compileExc +
                                "\", falling back to evaluating script in uncompiled mode");
                        }
                        if (compiledScript != null) {
                            compiledScript.eval();
                        } else { // fallback to uncompiled mode
                            scriptEngine.eval(script);
                        }
                    } else {
                        scriptEngine.eval(script);
                    }
                } catch (ScriptException exception) {
                    System.err.println(filename + ": caused ScriptException\n" + exception.getMessage());
                }
            }
        }

        @Override
        public void processCharacters() throws LoadException {
            if (source != null) {
                throw constructLoadException("Script source already specified.");
            }

            if (scriptEngine == null && !staticLoad) {
                throw constructLoadException("Page language not specified.");
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
                throw constructLoadException(prefix == null ? localName : prefix + ":" + localName
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
            throw constructLoadException("Element does not support attributes.");
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
        public final String filename;
        public CompiledScript compiledScript;
        public boolean isCompiled = false;

        public ScriptEventHandler(String script, ScriptEngine scriptEngine, String filename) {
            this.script = script;
            this.scriptEngine = scriptEngine;
            this.filename = filename;
            if (scriptEngine instanceof Compilable  && compileScript) {
                try {
                    // supply the filename to the scriptEngine engine scope Bindings in case it is needed for compilation
                    scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE).put(scriptEngine.FILENAME, filename);
                    this.compiledScript = ((Compilable) scriptEngine).compile(script);
                    this.isCompiled = true;
                } catch (ScriptException compileExc) {
                    Logging.getJavaFXLogger().warning(filename + ": compiling caused \"" + compileExc +
                        "\", falling back to evaluating script in uncompiled mode");
                }
            }
        }

        @Override
        public void handle(Event event) {
            // Don't pollute the page namespace with values defined in the script
            Bindings engineBindings = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
            Bindings localBindings = scriptEngine.createBindings();
            localBindings.putAll(engineBindings);
            localBindings.put(EVENT_KEY, event);
            localBindings.put(scriptEngine.ARGV, new Object[]{event});
            localBindings.put(scriptEngine.FILENAME, filename);
            // Execute the script
            try {
                if (isCompiled) {
                   compiledScript.eval(localBindings);
                } else {
                   scriptEngine.eval(script, localBindings);
                }
            } catch (ScriptException exception) {
                throw new RuntimeException(filename + ": caused ScriptException", exception);
            }
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
        private final SupportedType type;

        private MethodHandler(Object controller, Method method, SupportedType type) {
            this.method = method;
            this.controller = controller;
            this.type = type;
        }

        public void invoke(Object... params) {
            try {
                if (type != SupportedType.PARAMETERLESS) {
                    MethodHelper.invoke(method, controller, params);
                } else {
                    MethodHelper.invoke(method, controller, new Object[] {});
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

    private ClassLoader classLoader = null;
    private boolean staticLoad = false;
    private LoadListener loadListener = null;

    private FXMLLoader parentLoader;

    private XMLStreamReader xmlStreamReader = null;
    private Element current = null;

    private ScriptEngine scriptEngine = null;
    private static boolean compileScript = true;

    private List<String> packages = new LinkedList<String>();
    private Map<String, Class<?>> classes = new HashMap<String, Class<?>>();

    private ScriptEngineManager scriptEngineManager = null;

    private static ClassLoader defaultClassLoader = null;

    private static final Pattern extraneousWhitespacePattern = Pattern.compile("\\s+");

    private static BuilderFactory DEFAULT_BUILDER_FACTORY = new JavaFXBuilderFactory();

    /**
     * The character set used when character set is not explicitly specified.
     */
    public static final String DEFAULT_CHARSET_NAME = "UTF-8";

    /**
     * The tag name of language processing instruction.
     */
    public static final String LANGUAGE_PROCESSING_INSTRUCTION = "language";
    /**
     * The tag name of import processing instruction.
     */
    public static final String IMPORT_PROCESSING_INSTRUCTION = "import";

    /**
     * The tag name of the compile processing instruction.
     * @since 15
     */
    public static final String COMPILE_PROCESSING_INSTRUCTION = "compile";

    /**
     * Prefix of 'fx' namespace.
     */
    public static final String FX_NAMESPACE_PREFIX = "fx";
    /**
     * The name of fx:controller attribute of a root.
     */
    public static final String FX_CONTROLLER_ATTRIBUTE = "controller";
    /**
     * The name of fx:id attribute.
     */
    public static final String FX_ID_ATTRIBUTE = "id";
    /**
     * The name of fx:value attribute.
     */
    public static final String FX_VALUE_ATTRIBUTE = "value";
    /**
     * The tag name of 'fx:constant'.
     * @since JavaFX 2.2
     */
    public static final String FX_CONSTANT_ATTRIBUTE = "constant";
    /**
     * The name of 'fx:factory' attribute.
     */
    public static final String FX_FACTORY_ATTRIBUTE = "factory";

    /**
     * The tag name of {@literal <fx:include>}.
     */
    public static final String INCLUDE_TAG = "include";
    /**
     * The {@literal <fx:include>} 'source' attribute.
     */
    public static final String INCLUDE_SOURCE_ATTRIBUTE = "source";
    /**
     * The {@literal <fx:include>} 'resources' attribute.
     */
    public static final String INCLUDE_RESOURCES_ATTRIBUTE = "resources";
    /**
     * The {@literal <fx:include>} 'charset' attribute.
     */
    public static final String INCLUDE_CHARSET_ATTRIBUTE = "charset";

    /**
     * The tag name of {@literal <fx:script>}.
     */
    public static final String SCRIPT_TAG = "script";
    /**
     * The {@literal <fx:script>} 'source' attribute.
     */
    public static final String SCRIPT_SOURCE_ATTRIBUTE = "source";
    /**
     * The {@literal <fx:script>} 'charset' attribute.
     */
    public static final String SCRIPT_CHARSET_ATTRIBUTE = "charset";

    /**
     * The tag name of {@literal <fx:define>}.
     */
    public static final String DEFINE_TAG = "define";

    /**
     * The tag name of {@literal <fx:reference>}.
     */
    public static final String REFERENCE_TAG = "reference";
    /**
     * The {@literal <fx:reference>} 'source' attribute.
     */
    public static final String REFERENCE_SOURCE_ATTRIBUTE = "source";

    /**
     * The tag name of {@literal <fx:root>}.
     * @since JavaFX 2.2
     */
    public static final String ROOT_TAG = "root";
    /**
     * The {@literal <fx:root>} 'type' attribute.
     * @since JavaFX 2.2
     */
    public static final String ROOT_TYPE_ATTRIBUTE = "type";

    /**
     * The tag name of {@literal <fx:copy>}.
     */
    public static final String COPY_TAG = "copy";
    /**
     * The {@literal <fx:copy>} 'source' attribute.
     */
    public static final String COPY_SOURCE_ATTRIBUTE = "source";

    /**
     * The prefix of event handler attributes.
     */
    public static final String EVENT_HANDLER_PREFIX = "on";
    /**
     * The name of the Event object in event handler scripts.
     */
    public static final String EVENT_KEY = "event";
    /**
     * Suffix for property change/invalidation handlers.
     */
    public static final String CHANGE_EVENT_HANDLER_SUFFIX = "Change";
    private static final String COLLECTION_HANDLER_NAME = EVENT_HANDLER_PREFIX + CHANGE_EVENT_HANDLER_SUFFIX;

    /**
     * Value that represents 'null'.
     */
    public static final String NULL_KEYWORD = "null";

    /**
     * Escape prefix for escaping special characters inside attribute values.
     * Serves as an escape for {@link #ESCAPE_PREFIX}, {@link #RELATIVE_PATH_PREFIX},
     * {@link #RESOURCE_KEY_PREFIX}, {@link #EXPRESSION_PREFIX},
     * {@link #BI_DIRECTIONAL_BINDING_PREFIX}
     * @since JavaFX 2.1
     */
    public static final String ESCAPE_PREFIX = "\\";
    /**
     * Prefix for relative location resolution.
     */
    public static final String RELATIVE_PATH_PREFIX = "@";
    /**
     * Prefix for resource resolution.
     */
    public static final String RESOURCE_KEY_PREFIX = "%";
    /**
     * Prefix for (variable) expression resolution.
     */
    public static final String EXPRESSION_PREFIX = "$";
    /**
     * Prefix for binding expression resolution.
     */
    public static final String BINDING_EXPRESSION_PREFIX = "${";
    /**
     * Suffix for binding expression resolution.
     */
    public static final String BINDING_EXPRESSION_SUFFIX = "}";

    /**
     * Prefix for bidirectional-binding expression resolution.
     * @since JavaFX 2.1
     */
    public static final String BI_DIRECTIONAL_BINDING_PREFIX = "#{";
    /**
     * Suffix for bidirectional-binding expression resolution.
     * @since JavaFX 2.1
     */
    public static final String BI_DIRECTIONAL_BINDING_SUFFIX = "}";

    /**
     * Delimiter for arrays as values.
     * @since JavaFX 2.1
     */
    public static final String ARRAY_COMPONENT_DELIMITER = ",";

    /**
     * A key for location URL in namespace map.
     * @see #getNamespace()
     * @since JavaFX 2.2
     */
    public static final String LOCATION_KEY = "location";
    /**
     * A key for ResourceBundle in namespace map.
     * @see #getNamespace()
     * @since JavaFX 2.2
     */
    public static final String RESOURCES_KEY = "resources";

    /**
     * Prefix for controller method resolution.
     */
    public static final String CONTROLLER_METHOD_PREFIX = "#";
    /**
     * A key for controller in namespace map.
     * @see #getNamespace()
     * @since JavaFX 2.1
     */
    public static final String CONTROLLER_KEYWORD = "controller";
    /**
     * A suffix for controllers of included fxml files.
     * The full key is stored in namespace map.
     * @see #getNamespace()
     * @since JavaFX 2.2
     */
    public static final String CONTROLLER_SUFFIX = "Controller";

    /**
     * The name of initialize method.
     * @since JavaFX 2.2
     */
    public static final String INITIALIZE_METHOD_NAME = "initialize";

    /**
     * Contains the current javafx version.
     * @since JavaFX 8.0
     */
    public static final String JAVAFX_VERSION;

    /**
     * Contains the current fx namepsace version.
     * @since JavaFX 8.0
     */
    public static final String FX_NAMESPACE_VERSION = "1";

    static {
        @SuppressWarnings("removal")
        String tmp = AccessController.doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return System.getProperty("javafx.version");
            }
        });
        JAVAFX_VERSION = tmp;

        FXMLLoaderHelper.setFXMLLoaderAccessor(new FXMLLoaderHelper.FXMLLoaderAccessor() {
            @Override
            public void setStaticLoad(FXMLLoader fxmlLoader, boolean staticLoad) {
                fxmlLoader.setStaticLoad(staticLoad);
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
     * @param location the location used to resolve relative path attribute values
     * @since JavaFX 2.1
     */
    public FXMLLoader(URL location) {
        this(location, null);
    }

    /**
     * Creates a new FXMLLoader instance.
     *
     * @param location the location used to resolve relative path attribute values
     * @param resources the resources used to resolve resource key attribute values
     * @since JavaFX 2.1
     */
    public FXMLLoader(URL location, ResourceBundle resources) {
        this(location, resources, null);
    }

    /**
     * Creates a new FXMLLoader instance.
     *
     * @param location the location used to resolve relative path attribute values
     * @param resources resources used to resolve resource key attribute values
     * @param builderFactory the builder factory used by this loader
     * @since JavaFX 2.1
     */
    public FXMLLoader(URL location, ResourceBundle resources, BuilderFactory builderFactory) {
        this(location, resources, builderFactory, null);
    }

    /**
     * Creates a new FXMLLoader instance.
     *
     * @param location the location used to resolve relative path attribute values
     * @param resources resources used to resolve resource key attribute values
     * @param builderFactory the builder factory used by this loader
     * @param controllerFactory the controller factory used by this loader
     * @since JavaFX 2.1
     */
    public FXMLLoader(URL location, ResourceBundle resources, BuilderFactory builderFactory,
        Callback<Class<?>, Object> controllerFactory) {
        this(location, resources, builderFactory, controllerFactory, Charset.forName(DEFAULT_CHARSET_NAME));
    }

    /**
     * Creates a new FXMLLoader instance.
     *
     * @param charset the character set used by this loader
     */
    public FXMLLoader(Charset charset) {
        this(null, null, null, null, charset);
    }

    /**
     * Creates a new FXMLLoader instance.
     *
     * @param location the location used to resolve relative path attribute values
     * @param resources resources used to resolve resource key attribute values
     * @param builderFactory the builder factory used by this loader
     * @param controllerFactory the controller factory used by this loader
     * @param charset the character set used by this loader
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
     * @param location the location used to resolve relative path attribute values
     * @param resources resources used to resolve resource key attribute values
     * @param builderFactory the builder factory used by this loader
     * @param controllerFactory the controller factory used by this loader
     * @param charset the character set used by this loader
     * @param loaders list of loaders
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
     * @return the location used to resolve relative path attribute values
     */
    public URL getLocation() {
        return location;
    }

    /**
     * Sets the location used to resolve relative path attribute values.
     *
     * @param location the location
     */
    public void setLocation(URL location) {
        this.location = location;
    }

    /**
     * Returns the resources used to resolve resource key attribute values.
     * @return the resources used to resolve resource key attribute values
     */
    public ResourceBundle getResources() {
        return resources;
    }

    /**
     * Sets the resources used to resolve resource key attribute values.
     *
     * @param resources the resources
     */
    public void setResources(ResourceBundle resources) {
        this.resources = resources;
    }

    /**
     * Returns the namespace used by this loader.
     * @return the namespace
     */
    public ObservableMap<String, Object> getNamespace() {
        return namespace;
    }

    /**
     * Returns the root of the object hierarchy.
     * @param <T> the type of the root object
     * @return the root of the object hierarchy
     */
    @SuppressWarnings("unchecked")
    public <T> T getRoot() {
        return (T)root;
    }

    /**
     * Sets the root of the object hierarchy. The value passed to this method
     * is used as the value of the {@code <fx:root>} tag. This method
     * must be called prior to loading the document when using
     * {@code <fx:root>}.
     *
     * @param root the root of the object hierarchy
     *
     * @since JavaFX 2.2
     */
    public void setRoot(Object root) {
        this.root = root;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FXMLLoader) {
            FXMLLoader loader = (FXMLLoader)obj;
            if (location == null || loader.location == null) {
                return loader.location == location;
            }
            return location.toExternalForm().equals(
                    loader.location.toExternalForm());
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
     * @param <T> the type of the controller
     * @return the controller associated with the root object
     */
    @SuppressWarnings("unchecked")
    public <T> T getController() {
        return (T)controller;
    }

    /**
     * Sets the controller associated with the root object. The value passed to
     * this method is used as the value of the {@code fx:controller} attribute.
     * This method must be called prior to loading the document when using
     * controller event handlers when an {@code fx:controller} attribute is not
     * specified in the document.
     *
     * @param controller the controller to associate with the root object
     *
     * @since JavaFX 2.2
     */
    public void setController(Object controller) {
        this.controller = controller;

        if (controller == null) {
            namespace.remove(CONTROLLER_KEYWORD);
        } else {
            namespace.put(CONTROLLER_KEYWORD, controller);
        }

        controllerAccessor.setController(controller);
    }

    /**
     * Returns the builder factory used by this loader.
     * @return the builder factory
     */
    public BuilderFactory getBuilderFactory() {
        return builderFactory;
    }

    /**
     * Sets the builder factory used by this loader.
     *
     * @param builderFactory the builder factory
     */
    public void setBuilderFactory(BuilderFactory builderFactory) {
        this.builderFactory = builderFactory;
    }

    /**
     * Returns the controller factory used by this loader.
     * @return the controller factory
     * @since JavaFX 2.1
     */
    public Callback<Class<?>, Object> getControllerFactory() {
        return controllerFactory;
    }

    /**
     * Sets the controller factory used by this loader.
     *
     * @param controllerFactory the controller factory
     * @since JavaFX 2.1
     */
    public void setControllerFactory(Callback<Class<?>, Object> controllerFactory) {
        this.controllerFactory = controllerFactory;
    }

    /**
     * Returns the character set used by this loader.
     * @return the character set
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * Sets the character set used by this loader.
     *
     * @param charset the character set
     * @since JavaFX 2.1
     */
    public void setCharset(Charset charset) {
        if (charset == null) {
            throw new NullPointerException("charset is null.");
        }

        this.charset = charset;
    }

    /**
     * Returns the classloader used by this loader.
     * @return the classloader
     * @since JavaFX 2.1
     */
    public ClassLoader getClassLoader() {
        if (classLoader == null) {
            @SuppressWarnings("removal")
            final SecurityManager sm = System.getSecurityManager();
            final Class caller = (sm != null) ?
                    walker.getCallerClass() :
                    null;
            return getDefaultClassLoader(caller);
        }
        return classLoader;
    }

    /**
     * Sets the classloader used by this loader and clears any existing
     * imports.
     *
     * @param classLoader the classloader
     * @since JavaFX 2.1
     */
    public void setClassLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalArgumentException();
        }

        this.classLoader = classLoader;

        clearImports();
    }

    /*
     * Returns the static load flag.
     */
    boolean isStaticLoad() {
        // SB-dependency: RT-21226 has been filed to track this
        return staticLoad;
    }

    /*
     * Sets the static load flag.
     *
     * @param staticLoad
     */
    void setStaticLoad(boolean staticLoad) {
        // SB-dependency: RT-21226 has been filed to track this
        this.staticLoad = staticLoad;
    }

    /**
     * Returns this loader's load listener.
     *
     * @return the load listener
     *
     * @since 9
     */
    public LoadListener getLoadListener() {
        // SB-dependency: RT-21228 has been filed to track this
        return loadListener;
    }

    /**
     * Sets this loader's load listener.
     *
     * @param loadListener the load listener
     *
     * @since 9
     */
    public final void setLoadListener(LoadListener loadListener) {
        // SB-dependency: RT-21228 has been filed to track this
        this.loadListener = loadListener;
    }

    /**
     * Loads an object hierarchy from a FXML document. The location from which
     * the document will be loaded must have been set by a prior call to
     * {@link #setLocation(URL)}.
     *
     * @param <T> the type of the root object
     * @throws IOException if an error occurs during loading
     * @return the loaded object hierarchy
     *
     * @since JavaFX 2.1
     */
    @SuppressWarnings("removal")
    public <T> T load() throws IOException {
        return loadImpl((System.getSecurityManager() != null)
                            ? walker.getCallerClass()
                            : null);
    }

    /**
     * Loads an object hierarchy from a FXML document.
     *
     * @param <T> the type of the root object
     * @param inputStream an input stream containing the FXML data to load
     *
     * @throws IOException if an error occurs during loading
     * @return the loaded object hierarchy
     */
    @SuppressWarnings("removal")
    public <T> T load(InputStream inputStream) throws IOException {
        return loadImpl(inputStream, (System.getSecurityManager() != null)
                                         ? walker.getCallerClass()
                                         : null);
    }

    private Class<?> callerClass;

    private <T> T loadImpl(final Class<?> callerClass) throws IOException {
        if (location == null) {
            throw new IllegalStateException("Location is not set.");
        }

        InputStream inputStream = null;
        T value;
        try {
            inputStream = location.openStream();
            value = loadImpl(inputStream, callerClass);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

        return value;
    }

    @SuppressWarnings({ "dep-ann", "unchecked" })
    private <T> T loadImpl(InputStream inputStream,
                           Class<?> callerClass) throws IOException {
        if (inputStream == null) {
            throw new NullPointerException("inputStream is null.");
        }

        this.callerClass = callerClass;
        controllerAccessor.setCallerClass(callerClass);
        try {
            clearImports();

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
                throw constructLoadException(exception);
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
                throw constructLoadException(exception);
            }

            if (controller != null) {
                if (controller instanceof Initializable) {
                    ((Initializable)controller).initialize(location, resources);
                } else {
                    // Inject controller fields
                    Map<String, List<Field>> controllerFields =
                            controllerAccessor.getControllerFields();

                    injectFields(LOCATION_KEY, location);

                    injectFields(RESOURCES_KEY, resources);

                    // Initialize the controller
                    Method initializeMethod = controllerAccessor
                                                  .getControllerMethods()
                                                  .get(SupportedType.PARAMETERLESS)
                                                  .get(INITIALIZE_METHOD_NAME);

                    if (initializeMethod != null) {
                        try {
                            MethodHelper.invoke(initializeMethod, controller, new Object [] {});
                        } catch (IllegalAccessException exception) {
                            throw constructLoadException(exception);
                        } catch (InvocationTargetException exception) {
                            throw constructLoadException(exception);
                        }
                    }
                }
            }
        } catch (final LoadException exception) {
            throw exception;
        } catch (final Exception exception) {
            throw constructLoadException(exception);
        } finally {
            controllerAccessor.setCallerClass(null);
            // Clear controller accessor caches
            controllerAccessor.reset();
            // Clear the parser
            xmlStreamReader = null;
        }

        return (T)root;
    }

    private void clearImports() {
        packages.clear();
        classes.clear();
    }

    private LoadException constructLoadException(String message){
        return new LoadException(message + constructFXMLTrace());
    }

    private LoadException constructLoadException(Throwable cause) {
        return new LoadException(constructFXMLTrace(), cause);
    }

    private LoadException constructLoadException(String message, Throwable cause){
        return new LoadException(message + constructFXMLTrace(), cause);
    }

    private String constructFXMLTrace() {
        StringBuilder messageBuilder = new StringBuilder("\n");

        for (FXMLLoader loader : loaders) {
            messageBuilder.append(loader.location != null ? loader.location.getPath() : "unknown path");

            if (loader.current != null) {
                messageBuilder.append(":");
                messageBuilder.append(loader.getLineNumber());
            }

            messageBuilder.append("\n");
        }
        return messageBuilder.toString();
    }

    /**
     * Returns the current line number.
     */
    int getLineNumber() {
        return xmlStreamReader.getLocation().getLineNumber();
    }

    /**
     * Returns the current parse trace.
     */
    ParseTraceElement[] getParseTrace() {
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
        } else if (piTarget.equals(COMPILE_PROCESSING_INSTRUCTION)) {
            String strCompile = xmlStreamReader.getPIData().trim();
            // if PIData() is empty string then default to true, otherwise use Boolean.parseBoolean(string) to determine the boolean value
            compileScript = (strCompile.length() == 0 ? true : Boolean.parseBoolean(strCompile));
        }
    }

    private void processLanguage() throws LoadException {
        if (scriptEngine != null) {
            throw constructLoadException("Page language already set.");
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
                        throw constructLoadException(localName + " is not a valid property.");
                    }
                }
            } else {
                if (current == null && root != null) {
                    throw constructLoadException("Root value already specified.");
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
                    throw constructLoadException(localName + " is not a valid type.");
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
                throw constructLoadException(prefix + ":" + localName + " is not a valid element.");
            }
        } else {
            throw constructLoadException("Unexpected namespace prefix: " + prefix + ".");
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
            throw constructLoadException(exception);
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
        return getClassLoader().loadClass(packageName + "." + className.replace('.', '$'));
    }

    private static enum SupportedType {
        PARAMETERLESS {

            @Override
            protected boolean methodIsOfType(Method m) {
                return m.getParameterTypes().length == 0;
            }

        },
        EVENT {

            @Override
            protected boolean methodIsOfType(Method m) {
                return m.getParameterTypes().length == 1 &&
                        Event.class.isAssignableFrom(m.getParameterTypes()[0]);
            }

        },
        LIST_CHANGE_LISTENER {

            @Override
            protected boolean methodIsOfType(Method m) {
                return m.getParameterTypes().length == 1 &&
                        m.getParameterTypes()[0].equals(ListChangeListener.Change.class);
            }

        },
        MAP_CHANGE_LISTENER {

            @Override
            protected boolean methodIsOfType(Method m) {
                return m.getParameterTypes().length == 1 &&
                        m.getParameterTypes()[0].equals(MapChangeListener.Change.class);
            }

        },
        SET_CHANGE_LISTENER {

            @Override
            protected boolean methodIsOfType(Method m) {
                return m.getParameterTypes().length == 1 &&
                        m.getParameterTypes()[0].equals(SetChangeListener.Change.class);
            }

        },
        PROPERTY_CHANGE_LISTENER {

            @Override
            protected boolean methodIsOfType(Method m) {
                return m.getParameterTypes().length == 3 &&
                        ObservableValue.class.isAssignableFrom(m.getParameterTypes()[0])
                        && m.getParameterTypes()[1].equals(m.getParameterTypes()[2]);
            }

        };

        protected abstract boolean methodIsOfType(Method m);
    }

    private static SupportedType toSupportedType(Method m) {
        for (SupportedType t : SupportedType.values()) {
            if (t.methodIsOfType(m)) {
                return t;
            }
        }
        return null;
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
     * @param packageName the package name of the class to load
     * @param className the name of the class to load
     *
     * @throws ClassNotFoundException if the specified class cannot be found
     * @return the class
     *
     * @deprecated
     * This method now delegates to {@link #getDefaultClassLoader()}.
     */
    @Deprecated
    public static Class<?> loadType(String packageName, String className) throws ClassNotFoundException {
        return loadType(packageName + "." + className.replace('.', '$'));
    }

    /**
     * Loads a type using the default class loader.
     *
     * @param className the name of the class to load
     * @throws ClassNotFoundException if the specified class cannot be found
     * @return the class
     *
     * @deprecated
     * This method now delegates to {@link #getDefaultClassLoader()}.
     */
    @Deprecated
    public static Class<?> loadType(String className) throws ClassNotFoundException {
        ReflectUtil.checkPackageAccess(className);
        return Class.forName(className, true, getDefaultClassLoader());
    }

    private static boolean needsClassLoaderPermissionCheck(Class caller) {
        if (caller == null) {
            return false;
        }
        return !FXMLLoader.class.getModule().equals(caller.getModule());
    }

    private static ClassLoader getDefaultClassLoader(Class caller) {
        if (defaultClassLoader == null) {
            @SuppressWarnings("removal")
            final SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                if (needsClassLoaderPermissionCheck(caller)) {
                    sm.checkPermission(GET_CLASSLOADER_PERMISSION);
                }
            }
            return Thread.currentThread().getContextClassLoader();
        }
        return defaultClassLoader;
    }

    /**
     * Returns the default class loader.
     * @return the default class loader
     * @since JavaFX 2.1
     */
    public static ClassLoader getDefaultClassLoader() {
        @SuppressWarnings("removal")
        final SecurityManager sm = System.getSecurityManager();
        final Class caller = (sm != null) ?
                walker.getCallerClass() :
                null;
        return getDefaultClassLoader(caller);
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
        @SuppressWarnings("removal")
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(MODIFY_FXML_CLASS_LOADER_PERMISSION);
        }

        FXMLLoader.defaultClassLoader = defaultClassLoader;
    }

    /**
     * Loads an object hierarchy from a FXML document.
     *
     * @param <T> the type of the root object
     * @param location the location used to resolve relative path attribute values
     *
     * @throws IOException if an error occurs during loading
     * @return the loaded object hierarchy
     */
    @SuppressWarnings("removal")
    public static <T> T load(URL location) throws IOException {
        return loadImpl(location, (System.getSecurityManager() != null)
                                      ? walker.getCallerClass()
                                      : null);
    }

    private static <T> T loadImpl(URL location, Class<?> callerClass)
            throws IOException {
        return loadImpl(location, null, callerClass);
    }

    /**
     * Loads an object hierarchy from a FXML document.
     *
     * @param <T> the type of the root object
     * @param location the location used to resolve relative path attribute values
     * @param resources the resources used to resolve resource key attribute values
     *
     * @throws IOException if an error occurs during loading
     * @return the loaded object hierarchy
     */
    @SuppressWarnings("removal")
    public static <T> T load(URL location, ResourceBundle resources)
                                     throws IOException {
        return loadImpl(location, resources,
                        (System.getSecurityManager() != null)
                            ? walker.getCallerClass()
                            : null);
    }

    private static <T> T loadImpl(URL location, ResourceBundle resources,
                                  Class<?> callerClass) throws IOException {
        return loadImpl(location, resources,  null,
                        callerClass);
    }

    /**
     * Loads an object hierarchy from a FXML document.
     *
     * @param <T> the type of the root object
     * @param location the location used to resolve relative path attribute values
     * @param resources the resources used to resolve resource key attribute values
     * @param builderFactory the builder factory used to load the document
     *
     * @throws IOException if an error occurs during loading
     * @return the loaded object hierarchy
     */
    @SuppressWarnings("removal")
    public static <T> T load(URL location, ResourceBundle resources,
                             BuilderFactory builderFactory)
                                     throws IOException {
        return loadImpl(location, resources, builderFactory,
                        (System.getSecurityManager() != null)
                            ? walker.getCallerClass()
                            : null);
    }

    private static <T> T loadImpl(URL location, ResourceBundle resources,
                                  BuilderFactory builderFactory,
                                  Class<?> callerClass) throws IOException {
        return loadImpl(location, resources, builderFactory, null, callerClass);
    }

    /**
     * Loads an object hierarchy from a FXML document.
     *
     * @param <T> the type of the root object
     * @param location the location used to resolve relative path attribute values
     * @param resources the resources used to resolve resource key attribute values
     * @param builderFactory the builder factory used when loading the document
     * @param controllerFactory the controller factory used when loading the document
     *
     * @throws IOException if an error occurs during loading
     * @return the loaded object hierarchy
     *
     * @since JavaFX 2.1
     */
    @SuppressWarnings("removal")
    public static <T> T load(URL location, ResourceBundle resources,
                             BuilderFactory builderFactory,
                             Callback<Class<?>, Object> controllerFactory)
                                     throws IOException {
        return loadImpl(location, resources, builderFactory, controllerFactory,
                        (System.getSecurityManager() != null)
                            ? walker.getCallerClass()
                            : null);
    }

    private static <T> T loadImpl(URL location, ResourceBundle resources,
                                  BuilderFactory builderFactory,
                                  Callback<Class<?>, Object> controllerFactory,
                                  Class<?> callerClass) throws IOException {
        return loadImpl(location, resources, builderFactory, controllerFactory,
                        Charset.forName(DEFAULT_CHARSET_NAME), callerClass);
    }

    /**
     * Loads an object hierarchy from a FXML document.
     *
     * @param <T> the type of the root object
     * @param location the location used to resolve relative path attribute values
     * @param resources the resources used to resolve resource key attribute values
     * @param builderFactory the builder factory used when loading the document
     * @param controllerFactory the controller factory used when loading the document
     * @param charset the character set used when loading the document
     *
     * @throws IOException if an error occurs during loading
     * @return the loaded object hierarchy
     *
     * @since JavaFX 2.1
     */
    @SuppressWarnings("removal")
    public static <T> T load(URL location, ResourceBundle resources,
                             BuilderFactory builderFactory,
                             Callback<Class<?>, Object> controllerFactory,
                             Charset charset) throws IOException {
        return loadImpl(location, resources, builderFactory, controllerFactory,
                        charset,
                        (System.getSecurityManager() != null)
                            ? walker.getCallerClass()
                            : null);
    }

    private static <T> T loadImpl(URL location, ResourceBundle resources,
                                  BuilderFactory builderFactory,
                                  Callback<Class<?>, Object> controllerFactory,
                                  Charset charset, Class<?> callerClass)
                                          throws IOException {
        if (location == null) {
            throw new NullPointerException("Location is required.");
        }

        FXMLLoader fxmlLoader =
                new FXMLLoader(location, resources, builderFactory,
                               controllerFactory, charset);

        return fxmlLoader.<T>loadImpl(callerClass);
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

    private static void checkClassLoaderPermission() {
        @SuppressWarnings("removal")
        final SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(MODIFY_FXML_CLASS_LOADER_PERMISSION);
        }
    }

    private final ControllerAccessor controllerAccessor =
            new ControllerAccessor();

    private static final class ControllerAccessor {
        private static final int PUBLIC = 1;
        private static final int PROTECTED = 2;
        private static final int PACKAGE = 4;
        private static final int PRIVATE = 8;
        private static final int INITIAL_CLASS_ACCESS =
                PUBLIC | PROTECTED | PACKAGE | PRIVATE;
        private static final int INITIAL_MEMBER_ACCESS =
                PUBLIC | PROTECTED | PACKAGE | PRIVATE;

        private static final int METHODS = 0;
        private static final int FIELDS = 1;

        private Object controller;
        private ClassLoader callerClassLoader;

        private Map<String, List<Field>> controllerFields;
        private Map<SupportedType, Map<String, Method>> controllerMethods;

        void setController(final Object controller) {
            if (this.controller != controller) {
                this.controller = controller;
                reset();
            }
        }

        void setCallerClass(final Class<?> callerClass) {
            final ClassLoader newCallerClassLoader =
                    (callerClass != null) ? callerClass.getClassLoader()
                                          : null;
            if (callerClassLoader != newCallerClassLoader) {
                callerClassLoader = newCallerClassLoader;
                reset();
            }
        }

        void reset() {
            controllerFields = null;
            controllerMethods = null;
        }

        Map<String, List<Field>> getControllerFields() {
            if (controllerFields == null) {
                controllerFields = new HashMap<>();

                if (callerClassLoader == null) {
                    // allow null class loader only with permission check
                    checkClassLoaderPermission();
                }

                addAccessibleMembers(controller.getClass(),
                                     INITIAL_CLASS_ACCESS,
                                     INITIAL_MEMBER_ACCESS,
                                     FIELDS);
            }

            return controllerFields;
        }

        Map<SupportedType, Map<String, Method>> getControllerMethods() {
            if (controllerMethods == null) {
                controllerMethods = new EnumMap<>(SupportedType.class);
                for (SupportedType t: SupportedType.values()) {
                    controllerMethods.put(t, new HashMap<String, Method>());
                }

                if (callerClassLoader == null) {
                    // allow null class loader only with permission check
                    checkClassLoaderPermission();
                }

                addAccessibleMembers(controller.getClass(),
                                     INITIAL_CLASS_ACCESS,
                                     INITIAL_MEMBER_ACCESS,
                                     METHODS);
            }

            return controllerMethods;
        }

        private void addAccessibleMembers(final Class<?> type,
                                          final int prevAllowedClassAccess,
                                          final int prevAllowedMemberAccess,
                                          final int membersType) {
            if (type == Object.class) {
                return;
            }

            int allowedClassAccess = prevAllowedClassAccess;
            int allowedMemberAccess = prevAllowedMemberAccess;
            if ((callerClassLoader != null)
                    && (type.getClassLoader() != callerClassLoader)) {
                // restrict further access
                allowedClassAccess &= PUBLIC;
                allowedMemberAccess &= PUBLIC;
            }

            final int classAccess = getAccess(type.getModifiers());
            if ((classAccess & allowedClassAccess) == 0) {
                // we are done
                return;
            }

            ReflectUtil.checkPackageAccess(type);

            addAccessibleMembers(type.getSuperclass(),
                                 allowedClassAccess,
                                 allowedMemberAccess,
                                 membersType);

            final int finalAllowedMemberAccess = allowedMemberAccess;
            @SuppressWarnings("removal")
            var dummy = AccessController.doPrivileged(
                    new PrivilegedAction<Void>() {
                        @Override
                        public Void run() {
                            if (membersType == FIELDS) {
                                addAccessibleFields(type,
                                                    finalAllowedMemberAccess);
                            } else {
                                addAccessibleMethods(type,
                                                     finalAllowedMemberAccess);
                            }

                            return null;
                        }
                    });
        }

        private void addAccessibleFields(final Class<?> type,
                                         final int allowedMemberAccess) {
            final boolean isPublicType = Modifier.isPublic(type.getModifiers());

            final Field[] fields = type.getDeclaredFields();
            for (int i = 0; i < fields.length; ++i) {
                final Field field = fields[i];
                final int memberModifiers = field.getModifiers();

                if (((memberModifiers & (Modifier.STATIC
                                             | Modifier.FINAL)) != 0)
                        || ((getAccess(memberModifiers) & allowedMemberAccess)
                                == 0)) {
                    continue;
                }

                if (!isPublicType || !Modifier.isPublic(memberModifiers)) {
                    if (field.getAnnotation(FXML.class) == null) {
                        // no fxml annotation on a non-public field
                        continue;
                    }

                    // Ensure that the field is accessible
                    field.setAccessible(true);
                }

                List<Field> list = controllerFields.get(field.getName());
                if (list == null) {
                    list = new ArrayList<>(1);
                    controllerFields.put(field.getName(), list);
                }
                list.add(field);

            }
        }

        private void addAccessibleMethods(final Class<?> type,
                                          final int allowedMemberAccess) {
            final boolean isPublicType = Modifier.isPublic(type.getModifiers());

            final Method[] methods = type.getDeclaredMethods();
            for (int i = 0; i < methods.length; ++i) {
                final Method method = methods[i];
                final int memberModifiers = method.getModifiers();

                if (((memberModifiers & (Modifier.STATIC
                                             | Modifier.NATIVE)) != 0)
                        || ((getAccess(memberModifiers) & allowedMemberAccess)
                                == 0)) {
                    continue;
                }

                if (!isPublicType || !Modifier.isPublic(memberModifiers)) {
                    if (method.getAnnotation(FXML.class) == null) {
                        // no fxml annotation on a non-public method
                        continue;
                    }

                    // Ensure that the method is accessible
                    method.setAccessible(true);
                }

                // Add this method to the map if:
                // a) it is the initialize() method, or
                // b) it takes a single event argument, or
                // c) it takes no arguments and a handler with this
                //    name has not already been defined
                final String methodName = method.getName();
                final SupportedType convertedType;

                if ((convertedType = toSupportedType(method)) != null) {
                    controllerMethods.get(convertedType)
                                     .put(methodName, method);
                }
            }
        }

        private static int getAccess(final int fullModifiers) {
            final int untransformedAccess =
                    fullModifiers & (Modifier.PRIVATE | Modifier.PROTECTED
                                                      | Modifier.PUBLIC);

            switch (untransformedAccess) {
                case Modifier.PUBLIC:
                    return PUBLIC;

                case Modifier.PROTECTED:
                    return PROTECTED;

                case Modifier.PRIVATE:
                    return PRIVATE;

                default:
                    return PACKAGE;
            }
        }
    }
}
