/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.javafx.property.adapter;

import javafx.beans.property.adapter.ReadOnlyJavaBeanObjectProperty;
import javafx.beans.property.adapter.ReadOnlyJavaBeanObjectPropertyBuilder;

public final class JavaBeanQuickAccessor {

    private JavaBeanQuickAccessor() {
    }
    
    public static <T> ReadOnlyJavaBeanObjectProperty<T> createReadOnlyJavaBeanObjectProperty(Object bean, String name) throws NoSuchMethodException {
        return ReadOnlyJavaBeanObjectPropertyBuilder.<T>create().bean(bean).name(name).build();
    }
    
}
