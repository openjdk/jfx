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

package test.javafx.beans.property;

import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ReadOnlyPropertyDeclaringClassTest {

    @Test
    void returnsNullWhenBeanIsNull() {
        var p = new SimpleStringProperty(null, "foo");
        assertNull(p.getDeclaringClass());
    }

    @Test
    void returnsNullWhenNameIsNull() {
        var bean = new Object();
        var p = new SimpleStringProperty(bean, null);
        assertNull(p.getDeclaringClass());
    }

    @Test
    void returnsNullWhenNameIsEmpty() {
        var bean = new Object();
        var p = new SimpleStringProperty(bean, "");
        assertNull(p.getDeclaringClass());
    }

    @Test
    void returnsNullWhenPropertyHasNoAccessor() {
        class FooBean {
            final StringProperty foo = new SimpleStringProperty(this, "foo");
        }

        var bean = new FooBean();
        assertNull(bean.foo.getDeclaringClass());
    }

    @Test
    void returnsDeclaringClassWhenAccessorExistsOnBeanClass() {
        class FooBean {
            private final StringProperty foo = new SimpleStringProperty(this, "foo");

            public final StringProperty fooProperty() {
                return foo;
            }
        }

        var bean = new FooBean();
        var p = bean.fooProperty();
        assertEquals(FooBean.class, p.getDeclaringClass());
    }

    @Test
    void returnsDeclaringClassFromSuperclassWhenAccessorDeclaredInSuperclass() {
        class BaseBean {
            private final StringProperty bar = new SimpleStringProperty(this, "bar");

            public final StringProperty barProperty() {
                return bar;
            }
        }

        class SubBean extends BaseBean {
            // no barProperty() here
        }

        var bean = new SubBean();
        var p = bean.barProperty();
        assertEquals(BaseBean.class, p.getDeclaringClass());
    }

    @Test
    @SuppressWarnings("unused")
    void ignoresStaticAccessorAndReturnsNull() {
        class StaticAccessorOnlyBean {
            public static ReadOnlyStringProperty bazProperty() {
                return null;
            }
        }

        var bean = new StaticAccessorOnlyBean();
        var p = new SimpleStringProperty(bean, "baz");
        assertNull(p.getDeclaringClass());
    }

    @Test
    @SuppressWarnings("unused")
    void ignoresAccessorWithNonReadOnlyPropertyReturnType() {
        class WrongReturnTypeBean {
            public String badProperty() {
                return "not a property";
            }
        }

        var bean = new WrongReturnTypeBean();
        var p = new SimpleStringProperty(bean, "bad");
        assertNull(p.getDeclaringClass());
    }

    @Test
    void worksWithPrivateAccessorMethod() {
        class PrivateAccessorBean {
            private final StringProperty secret = new SimpleStringProperty(this, "secret");

            private StringProperty secretProperty() {
                return secret;
            }
        }

        var bean = new PrivateAccessorBean();
        var p = bean.secretProperty();
        assertEquals(PrivateAccessorBean.class, p.getDeclaringClass());
    }

    @Test
    @SuppressWarnings("unused")
    void returnsNullWhenAccessorHasParameters() {
        class ParamAccessorBean {
            public StringProperty withParamProperty(int ignored) {
                return new SimpleStringProperty(this, "withParam");
            }
        }

        var bean = new ParamAccessorBean();
        var p = new SimpleStringProperty(bean, "withParam");
        assertNull(p.getDeclaringClass());
    }
}
