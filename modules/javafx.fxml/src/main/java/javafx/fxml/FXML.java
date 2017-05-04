/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that tags a field or method as accessible to markup.
 * <p><b>Deploying an Application as a Module</b></p>
 * <p>
 * If the object being annotated is in a named module then it must
 * be reflectively accessible to the {@code javafx.fxml} module.
 * Otherwise, the {@link FXMLLoader} will fail with an
 * {@code InaccessibleObjectException} when it attempts to modify the
 * annotated element.
 * An object is reflectively accessible if the module containing that
 * object {@link Module#isOpen(String,Module) opens} the containing package to
 * at least the
 * {@code javafx.fxml} module.
 * </p>
 * <p>
 * For example, if the object being annotated is in the {@code com.foo}
 * package in the {@code foo.app} module, the {@code module-info.java} might
 * look like this:
 * </p>
 *
<pre>{@code module foo.app {
    opens com.foo to javafx.fxml;
}}</pre>
 *
 * <p>
 * Alternatively, an object is reflectively accessible if it is declared
 * as a public member, is in a public class, and the module containing that
 * class {@link Module#isExported(String) exports} the containing
 * package unconditionally.
 * </p>
 *
 * @since JavaFX 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface FXML {
}
