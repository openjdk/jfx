/*
 * Copyright (C) 2003 Apple Computer, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE COMPUTER, INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE COMPUTER, INC. OR
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
#include "runtime_root.h"

namespace JSC {

namespace Bindings {

class JavaClass;

class JavaInstance : public Instance {
public:
    static RefPtr<JavaInstance> create(jobject instance, RefPtr<RootObject>&& rootObject, jobject accessControlContext)
    {
        return adoptRef(new JavaInstance(instance, WTFMove(rootObject), accessControlContext));
    }

    ~JavaInstance();

    virtual Class* getClass() const;

    virtual JSValue valueOf(JSGlobalObject*) const;
    virtual JSValue defaultValue(JSGlobalObject*, PreferredPrimitiveType) const;

    virtual JSValue getMethod(JSGlobalObject*, PropertyName);
    virtual JSValue invokeMethod(JSGlobalObject*, CallFrame*, RuntimeMethod*);

    jobject javaInstance() const { return m_instance->instance(); }

    JSValue stringValue(JSGlobalObject*) const;
    JSValue numberValue(JSGlobalObject*) const;
    JSValue booleanValue() const;
    jobject accessControlContext() const { return m_accessControlContext->instance(); }

protected:
    JavaInstance(jobject instance, RefPtr<RootObject>&&, jobject accessControlContext);

    virtual RuntimeObject* newRuntimeObject(JSGlobalObject*);

    virtual void virtualBegin();
    virtual void virtualEnd();

    RefPtr<JobjectWrapper> m_instance;
    mutable JavaClass* m_class;
    RefPtr<JobjectWrapper> m_accessControlContext;
};

} // namespace Bindings

} // namespace JSC

#endif // ENABLE(JAVA_BRIDGE)
