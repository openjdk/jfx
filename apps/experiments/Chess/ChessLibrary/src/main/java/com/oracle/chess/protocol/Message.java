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

package com.oracle.chess.protocol;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import javax.json.Json;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonArray;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.JsonArrayBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import com.oracle.chess.model.Color;
import java.lang.reflect.ParameterizedType;

/**
 * Message class.
 *
 */
public abstract class Message {

    // Create JSON writer factory with pretty printing enabled
    private static final Map<String, Boolean> config;
    private static final JsonWriterFactory factory;
    static {
        config = new HashMap<>();
        config.put(JsonGenerator.PRETTY_PRINTING, Boolean.TRUE);
        factory = Json.createWriterFactory(config);
    }

    protected String msg;
    
    protected String gameId;

    protected Color color;
    
    private String username;

    private String password;

    public Message() {
        msg = getClass().getSimpleName();
    }

    public Message(String gameId) {
        msg = getClass().getSimpleName();
        this.gameId = gameId;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public boolean hasGameId() {
        return gameId != null;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean hasColor() {
        return color != null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean hasUsername() {
        return username != null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Message readFrom(JsonObject jobj) {
        readFrom(this, jobj);
        return this;
    }

    private static void readFrom(Object object, JsonObject jobj) {
        try {
            for (PropertyDescriptor pd
                    : Introspector.getBeanInfo(object.getClass(), Object.class).getPropertyDescriptors()) {
                final Method m = pd.getWriteMethod();
                if (m != null) {
                    JsonValue jv = jobj.get(pd.getName());
                    if (jv == null) {
                        continue;
                    }
                    Class<?> clazz = m.getParameterTypes()[0];
                    switch (jv.getValueType()) {
                        case NULL:
                            break;
                        case STRING:
                            final String sv = ((JsonString) jv).getString();
                            if (clazz.isEnum()) {
                                m.invoke(object, Enum.valueOf((Class<? extends Enum>) clazz, sv));
                            } else {
                                m.invoke(object, sv);
                            }
                            break;
                        case NUMBER:
                            m.invoke(object, ((JsonNumber) jv).intValue());
                            break;
                        case TRUE:
                            m.invoke(object, true);
                            break;
                        case FALSE:
                            m.invoke(object, false);
                            break;
                        case OBJECT:
                            Object instance = clazz.newInstance();
                            readFrom(instance, (JsonObject) jv);
                            m.invoke(object, instance);
                            break;
                        case ARRAY:     // only array of strings and objects supported!
                            final JsonArray ja = (JsonArray) jv;
                            final List<Object> list = new ArrayList<>(ja.size());
                            for (JsonValue v : ja) {
                                if (v instanceof JsonString) {
                                    list.add(((JsonString) v).getString());
                                } else {
                                    ParameterizedType pt = (ParameterizedType) m.getGenericParameterTypes()[0];
                                    clazz = (Class<?>) pt.getActualTypeArguments()[0];
                                    instance = clazz.newInstance();
                                    readFrom(instance, (JsonObject) v);
                                    list.add(instance);
                                }
                            }
                            m.invoke(object, list);
                            break;
                        default:
                            throw new UnsupportedOperationException("Unsupported type " + jv.getValueType());
                    }
                }
            }
        } catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | InstantiationException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void writeTo(JsonObjectBuilder jobj) {
        writeTo(this, jobj);
    }

    private static void writeTo(Object object, JsonObjectBuilder jobj) {
        try {
            for (PropertyDescriptor pd
                    : Introspector.getBeanInfo(object.getClass(), Object.class).getPropertyDescriptors()) {
                final Method m = pd.getReadMethod();
                if (m != null) {
                    Object v = m.invoke(object);
                    if (v == null) {
                        continue;
                    } else if (v instanceof String) {
                        jobj.add(pd.getName(), (String) v);
                    } else if (v instanceof Integer) {
                        jobj.add(pd.getName(), (Integer) v);
                    } else if (v instanceof Boolean) {
                        jobj.add(pd.getName(), (Boolean) v);
                    } else if (v instanceof Enum) {
                        jobj.add(pd.getName(), ((Enum) v).toString());
                    } else if (v instanceof List) {     // only list of strings or objects supported!
                        JsonArrayBuilder jab = Json.createArrayBuilder();
                        for (Object o : (List) v) {
                            if (o instanceof String) {
                                jab.add((String) o);
                            } else {
                                JsonObjectBuilder njobj = Json.createObjectBuilder();
                                writeTo(o, njobj);
                                jab.add(njobj);
                            }
                        }
                        jobj.add(pd.getName(), jab.build());
                    } else {
                        JsonObjectBuilder newJobj = Json.createObjectBuilder();
                        writeTo(v, newJobj);
                        jobj.add(pd.getName(), newJobj.build());
                    }
                }
            }
        } catch (IntrospectionException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new RuntimeException(ex);
        }
    }

    public static Message createInstance(String className) {
        try {
            final ClassLoader ccl = Thread.currentThread().getContextClassLoader();
            return (Message) ccl.loadClass("com.oracle.chess.protocol." + className).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    public abstract Message processMe(ServerMessageProcessor processor);

    public MessageRsp newResponse() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public String toString() {
        final JsonObjectBuilder jobj = Json.createObjectBuilder();
        writeTo(jobj);
        final StringWriter sw = new StringWriter();
        try (JsonWriter jw = factory.createWriter(sw)) {
            jw.writeObject(jobj.build());
        }
        return sw.toString();
    }
}
