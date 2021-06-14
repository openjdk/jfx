/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

#pragma once

#include <jni.h>

namespace ABI { namespace Windows { namespace UI { struct Color; } } }

class ThemeSupport final
{
public:
    ThemeSupport(JNIEnv*);
    ~ThemeSupport();
    ThemeSupport(ThemeSupport const&) = delete;
    ThemeSupport& operator=(ThemeSupport const&) = delete;

    void querySystemColors(jobject properties);
    void queryHighContrastScheme(jobject properties);
    void queryWindows10ThemeColors(jobject properties);

private:
    JNIEnv* env_;
    jclass mapClass_;
    jmethodID putMethod_;

    jobject newJavaColorString(int r, int g, int b, int a);
    void putValue(jobject properties, const char* key, const char* value);
    void putValue(jobject properties, const char* key, const wchar_t* value);
    void putColorValue(jobject properties, const char* colorName, int colorValue);
    void putColorValue(jobject properties, const char* colorName, ABI::Windows::UI::Color colorValue);
};
