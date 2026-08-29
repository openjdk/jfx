/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "config.h"

#include "NotImplemented.h"

/*
 * This translation unit defines nothing.
 *
 * Everything it held was commented out: an old PluginWidgetJava constructor and destructor,
 * superseded by the live ones in PluginWidgetJava.cpp, and six PluginView stubs marked
 * "XXX recheck". The bodies named a JNI environment, cached method ids and a Java array
 * built by strVect2JArray, none of which exists any more, so the text was no longer even
 * translatable if it were uncommented - it described an API that has been replaced by
 * WKJHostTheme.plugin_widget_create.
 *
 * Only comments were removed; nothing was compiled before and nothing is compiled now. The
 * file stays in WebCore/SourcesJava.txt so the build list is untouched.
 */
