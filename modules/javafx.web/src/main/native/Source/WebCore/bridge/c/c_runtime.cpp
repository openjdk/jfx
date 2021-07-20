/*
 * Copyright (C) 2004 Apple Inc.  All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "config.h"

#if ENABLE(NETSCAPE_PLUGIN_API)

#include "c_runtime.h"

#include "c_instance.h"
#include "c_utility.h"
#include "npruntime_impl.h"
#include <JavaScriptCore/JSLock.h>
#include <JavaScriptCore/JSObject.h>

namespace JSC {
namespace Bindings {

JSValue CField::valueFromInstance(JSGlobalObject* lexicalGlobalObject, const Instance* inst) const
{
    const CInstance* instance = static_cast<const CInstance*>(inst);
    NPObject* obj = instance->getObject();
    if (obj->_class->getProperty) {
        NPVariant property;
        VOID_TO_NPVARIANT(property);

        bool result;
        {
            JSLock::DropAllLocks dropAllLocks(lexicalGlobalObject);
            result = obj->_class->getProperty(obj, _fieldIdentifier, &property);
            CInstance::moveGlobalExceptionToExecState(lexicalGlobalObject);
        }
        if (result) {
            JSValue result = convertNPVariantToValue(lexicalGlobalObject, &property, instance->rootObject());
            _NPN_ReleaseVariantValue(&property);
            return result;
        }
    }
    return jsUndefined();
}

bool CField::setValueToInstance(JSGlobalObject* lexicalGlobalObject, const Instance *inst, JSValue aValue) const
{
    const CInstance* instance = static_cast<const CInstance*>(inst);
    NPObject* obj = instance->getObject();
    if (obj->_class->setProperty) {
        NPVariant variant;
        convertValueToNPVariant(lexicalGlobalObject, aValue, &variant);

        bool result = false;
        {
            JSLock::DropAllLocks dropAllLocks(lexicalGlobalObject);
            result = obj->_class->setProperty(obj, _fieldIdentifier, &variant);
            CInstance::moveGlobalExceptionToExecState(lexicalGlobalObject);
        }

        _NPN_ReleaseVariantValue(&variant);
        return result;
    }
    return false;
}

} }

#endif // ENABLE(NETSCAPE_PLUGIN_API)
