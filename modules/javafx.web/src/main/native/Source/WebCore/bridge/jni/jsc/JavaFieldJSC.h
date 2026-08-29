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

#if ENABLE(JAVA_BRIDGE)

#include "BridgeJSC.h"
#include "JNIUtility.h"
#include "JobjectWrapper.h"
#include "JavaMethodJSC.h"
#include "JavaStringJSC.h"

namespace JSC {

namespace Bindings {

class JavaField : public Field {
public:
    /*
     * `aField` is a java.lang.reflect.Field. As in the JNI version, the reference this keeps
     * is weak (JobjectWrapper defaults to it), so the Field can be collected while the
     * JavaClass that owns this JavaField is still alive; every access checks first and gives
     * up with a log if it has gone. That was true before and is preserved deliberately.
     */
    explicit JavaField(wkj_ref aField);

    JSValue valueFromInstance(JSGlobalObject*, const Instance*) const override;
    bool setValueToInstance(JSGlobalObject*, const Instance*, JSValue) const override;

    const JavaString& name() const { return m_name; }
    virtual RuntimeType typeClassName() const { return m_typeClassName.utf8(); }
    JavaType type() const { return m_type; }

private:
    JavaString m_name;
    JavaString m_typeClassName;
    JavaType m_type;
    RefPtr<JobjectWrapper> m_field;
};

} // namespace Bindings

} // namespace JSC

#endif // ENABLE(JAVA_BRIDGE)
