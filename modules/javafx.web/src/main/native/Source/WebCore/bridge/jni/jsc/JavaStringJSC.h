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
#include "JSDOMWindowBase.h"
#include "DOMWindow.h"

#include <JavaScriptCore/JSLock.h>
#include <wtf/text/WTFString.h>
#include <wtf/text/StringImpl.h>

#include "JavaInstanceJSC.h"

namespace JSC {

namespace Bindings {

class JavaString {
public:
    JavaString(JNIEnv* e, jstring s)
    {
        init(e, s);
    }

    JavaString(jstring s)
    {
        init(getJNIEnv(), s);
    }

    JavaString()
    {
        //JSLockHolder lock(WebCore::JSDOMWindowBase::commonVM());
        m_impl = StringImpl::empty();
    }

    ~JavaString()
    {
        //JSLockHolder lock(WebCore::JSDOMWindowBase::commonVM());
        m_impl = nullptr;
    }

    const char* utf8() const
    {
        if (!m_utf8String.data()) {
            //JSLockHolder lock(WebCore::JSDOMWindowBase::commonVM());
            m_utf8String = String((RefPtr<StringImpl>)m_impl).utf8();
        }
        return m_utf8String.data();
    }
    int length() const { return m_impl->length(); }
    RefPtr<StringImpl> impl() const { return m_impl; }

private:
    void init(JNIEnv* e, jstring s)
    {
        int size = e->GetStringLength(s);
        const jchar* uc = getUCharactersFromJStringInEnv(e, s);
        {
            //JSLockHolder lock(WebCore::JSDOMWindowBase::commonVM());
            std::span<const UChar> createSpan(reinterpret_cast<const UChar*>(uc), size);
            m_impl = StringImpl::create(createSpan);
        }
        releaseUCharactersForJStringInEnv(e, s, uc);
    }

    RefPtr<StringImpl> m_impl;
    mutable CString m_utf8String;
};

} // namespace Bindings

} // namespace JSC
