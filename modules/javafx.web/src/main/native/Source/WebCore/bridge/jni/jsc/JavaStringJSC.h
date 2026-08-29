/*
 * Copyright (C) 2003, 2004, 2005, 2007, 2009, 2010 Apple Inc. All rights reserved.
 * Copyright 2010, The Android Open Source Project
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include "JNIUtility.h"

#include <wtf/text/CString.h>
#include <wtf/text/StringImpl.h>
#include <wtf/text/WTFString.h>

namespace JSC {

namespace Bindings {

/*
 * A Java string held for the life of a JavaField or a JavaMethod: the names and type names
 * that class enumeration produced.
 *
 * It used to be built from a Java string, with GetStringChars and a UTF-16 copy. The characters
 * now arrive as a WTF::String from the host table - see javaClassName, javaFieldName and
 * javaMethodName - so there is nothing left to convert and the class is only a holder that
 * caches the UTF-8 form its callers ask for.
 *
 * A null String becomes the empty string, which is what the JNI constructor produced for the
 * "<Unknown>" substitutions its callers made, and keeps length() and utf8() safe on a value
 * nothing could be read from.
 */
class JavaString {
public:
    JavaString()
    {
        m_impl = StringImpl::empty();
    }

    explicit JavaString(const WTF::String& value)
    {
        if (value.isNull())
            m_impl = StringImpl::empty();
        else
            m_impl = value.impl();
    }

    ~JavaString()
    {
        m_impl = nullptr;
    }

    const char* utf8() const
    {
        if (!m_utf8String.data())
            m_utf8String = String((RefPtr<StringImpl>)m_impl).utf8();
        return m_utf8String.data();
    }
    int length() const { return m_impl->length(); }
    RefPtr<StringImpl> impl() const { return m_impl; }

private:
    RefPtr<StringImpl> m_impl;
    mutable CString m_utf8String;
};

} // namespace Bindings

} // namespace JSC
