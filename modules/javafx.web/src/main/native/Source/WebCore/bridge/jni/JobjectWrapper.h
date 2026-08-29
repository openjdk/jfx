/*
 * Copyright (C) 2019 Apple Inc. All rights reserved.
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

#include "JNIUtility.h"
#include <wtf/Ref.h>
#include <wtf/RefCounted.h>

namespace JSC {

namespace Bindings {

/*
 * A reference-counted reference to a Java object, held for as long as a JavaInstance,
 * JavaArray or JavaField needs it.
 *
 * The `useGlobalRef` parameter kept its name and its default. It used to choose between
 * NewGlobalRef and NewWeakGlobalRef; it now chooses between WKJRetain and WKJRetainWeak,
 * which is the same choice. Defaulting to false - a WEAK reference - is deliberate and is
 * the reason WKJHostCore has retain_weak at all: an object an application hands to
 * JSObject.setMember must stay collectable, and only the access-control context, which is
 * created once and never collected, is held strongly.
 *
 * Every dereference of a weak id therefore has to start with a liveness check, exactly as
 * the JNI code started every one by making a local reference from the weak one.
 * WKJHandle::retained does the same: it returns a null handle once the object has gone, and
 * keeps the object alive for as long as the handle lives.
 *
 * The destructor no longer asks which kind of reference it holds. GetObjectRefType existed
 * because DeleteGlobalRef and DeleteWeakGlobalRef are different calls; WKJHostCore.release
 * takes either kind, so the branch has nothing left to decide.
 */
class JobjectWrapper : public RefCounted<JobjectWrapper> {
public:
    static Ref<JobjectWrapper> create(wkj_ref object, bool useGlobalRef = false)
    {
        return adoptRef(*new JobjectWrapper(object, useGlobalRef));
    }
    ~JobjectWrapper();

    /* Borrowed: this wrapper keeps the reference. Do not release the returned id. */
    wkj_ref instance() const { return m_instance.get(); }

private:
    JobjectWrapper(wkj_ref, bool useGlobalRef);

    WKJHandle m_instance;
};

} // namespace Bindings

} // namespace JSC

#endif // ENABLE(JAVA_BRIDGE)
