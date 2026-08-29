/*
 * Copyright (C) 2003, 2008, 2010 Apple Inc. All rights reserved.
 * Copyright 2011, The Android Open Source Project
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

#include "config.h"
#include "JobjectWrapper.h"

#if ENABLE(JAVA_BRIDGE)

using namespace JSC::Bindings;

JobjectWrapper::JobjectWrapper(wkj_ref instance, bool useGlobalRef)
{
    ASSERT(instance);

    /*
     * There is no JNI environment to cache any more. The JNI version kept one so the destructor
     * could delete the reference with the same environment it created it with; a registry id
     * is not tied to a thread, so release works from wherever the last reference dies.
     */
    m_instance = useGlobalRef ? WKJHandle::retained(instance) : WKJHandle::retainedWeak(instance);

    if (!m_instance)
        LOG_ERROR("Could not get GlobalRef for %llu", static_cast<unsigned long long>(instance));
}

JobjectWrapper::~JobjectWrapper()
{
    /*
     * WKJHandle releases whatever it holds, strong or weak alike, so the GetObjectRefType
     * branch this destructor used to carry has nothing left to choose between.
     */
}

#endif // ENABLE(JAVA_BRIDGE)
