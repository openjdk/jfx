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
import java.lang.module.ModuleDescriptor;

/**
 * Annotation that tags a field or method as accessible to markup.
 * If the object being annotated is in a named module then it must
 * be reflectively accessible to the {@code javafx.fxml} module.
 * An object is reflectively accessible if the module containing that
 * object opens (see {@code Module.isOpen}) the containing package to the
 * {@code javafx.fxml} module, either in its {@link ModuleDescriptor}
 * (e.g., in its module-info.class) or by calling {@code Module.addOpens}.
 * An object is also reflectively accessible if it is declared as a public
 * member, is in a public class, and the module containing that class
 * exports (see {@code Module.isExported(String,Module)})
 * the containing package to the {@code javafx.fxml} module.
 * If the object is not reflectively accessible to the {@code javafx.fxml}
 * module, then the {@link FXMLLoader} will fail with an
 * {@code InaccessibleObjectException} when it attempts to modify the
 * annotated element.
 *
 * @since JavaFX 2.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface FXML {
}
