/*
 * Copyright (c) 2010, 2024, Oracle and/or its affiliates. All rights reserved.
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

#ifndef _LOCATOR_H_
#define _LOCATOR_H_

#include <string>
#include <stdint.h>

#include <jni/JniUtils.h>

using namespace std;

class CLocator
{
public:
    enum LocatorType
    {
        kStreamLocatorType      = 1,
        kInvalidLocator         = 0
    };

public:
    CLocator(LocatorType type, const char* contentType, const char* location);
    CLocator(LocatorType type, const char* contentType, const char* location, int64_t llSizeHint);

    LocatorType GetType();

    static jstring LocatorGetStringLocation(JNIEnv *env, jobject locator);

    static jobject CreateConnectionHolder(JNIEnv *env, jobject locator);
    static jobject GetAudioStreamConnectionHolder(JNIEnv *env, jobject locator, jobject connectionHolder);

    inline const string& GetContentType() { return m_contentType; }

    inline const string GetLocation() { return m_location; }

    int64_t    GetSizeHint();

protected:
    LocatorType m_type;
    string      m_contentType;
    string      m_location;
    int64_t     m_llSizeHint;
};

#endif  //_LOCATOR_H_
