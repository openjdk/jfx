package com.sun.javafx.css;

import javafx.beans.property.Property;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.Node;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CssMetaDataCache {

    private static final Map<Class<?>, List<CssMetaData<? extends Styleable, ?>>> cache = new HashMap<>();

    private CssMetaDataCache() {}

    public synchronized static <T extends Node> List<CssMetaData<? extends Styleable, ?>> getCssMetaData(T node) {
        List<CssMetaData<? extends Styleable, ?>> list = cache.get(node.getClass());
        if (list == null) {
            cache.put(node.getClass(), list = CssMetaDataCache.reflectCssMetaData(node));
        }

        return list;
    }

    private static <T extends Node> List<CssMetaData<? extends Styleable, ?>> reflectCssMetaData(T node) {
        List<CssMetaData<? extends Styleable, ?>> metadata = new ArrayList<>();

        for (Method method : node.getClass().getMethods()) {
            if (Modifier.isStatic(method.getModifiers())
                    || method.isBridge()
                    || method.isSynthetic()
                    || method.getParameterCount() > 0) {
                continue;
            }

            Class<?> type = method.getReturnType();
            if (!Property.class.isAssignableFrom(type) && !StyleableProperty.class.isAssignableFrom(type)) {
                continue;
            }

            try {
                if (method.invoke(node) instanceof StyleableProperty<?> property) {
                    metadata.add(property.getCssMetaData());
                }
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        }

        return List.copyOf(metadata);
    }
}
