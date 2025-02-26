/*
 * Copyright (c) 2011, 2025, Oracle and/or its affiliates. All rights reserved.
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

#include <wtf/Vector.h>
#include <wtf/text/WTFString.h>

namespace WTF {

// String conversions
String::String(JNIEnv* env, const JLString &s)
{
    if (!s) {
        m_impl = StringImpl::empty();
    } else {
        unsigned int len = env->GetStringLength(s);
        if (!len) {
            m_impl = StringImpl::empty();
        } else {
            const jchar* str = env->GetStringCritical(s, NULL);
            if (str) {
                std::span<const UChar> createSpan(reinterpret_cast<const UChar*>(str), len);
                m_impl = StringImpl::create(createSpan);
                env->ReleaseStringCritical(s, str);
            } else {
                std::span<const UChar> createSpan(reinterpret_cast<const UChar*>(str), 3);
                m_impl = StringImpl::create(createSpan);
            }
        }
    }
}

JLString String::toJavaString(JNIEnv *env) const
{
    if (isNull()) {
        return NULL;
    } else {
        const unsigned len = length();
        if (is8Bit()) {
            // Convert latin1 chars to unicode.
            Vector<jchar> jchars(len);
            for (unsigned i = 0; i < len; i++) {
                jchars[i] = characterAt(i);
            }
            return env->NewString(jchars.data(), len);
        } else {
              //return env->NewString((jchar*)characters16(), len);
              std::span<const UChar> span = span16();
              return env->NewString(reinterpret_cast<const jchar*>(span.data()), span.size());
        }
    }
}

} // namespace WTF
