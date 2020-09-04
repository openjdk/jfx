/*
 * Copyright (C) 2019 Apple Inc. All rights reserved.
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
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#if ENABLE(WEBGPU)

#include "WHLSLAddressSpace.h"
#include "WHLSLCodeLocation.h"
#include "WHLSLUnnamedType.h"
#include <wtf/FastMalloc.h>
#include <wtf/UniqueRef.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

namespace WHLSL {

namespace AST {

class ReferenceType : public UnnamedType {
    WTF_MAKE_FAST_ALLOCATED;
    WTF_MAKE_NONCOPYABLE(ReferenceType);

protected:
    ~ReferenceType() = default;

protected:
    ReferenceType(CodeLocation location, AddressSpace addressSpace, Ref<UnnamedType> elementType, Kind kind)
        : UnnamedType(location, kind)
        , m_addressSpace(addressSpace)
        , m_elementType(WTFMove(elementType))
    {
    }


public:
    AddressSpace addressSpace() const { return m_addressSpace; }
    const UnnamedType& elementType() const { return m_elementType; }
    UnnamedType& elementType() { return m_elementType; }

    unsigned hash() const
    {
        return ~m_elementType->hash();
    }

private:
    AddressSpace m_addressSpace;
    Ref<UnnamedType> m_elementType;
};

} // namespace AST

}

}

DEFINE_DEFAULT_DELETE(ReferenceType)

SPECIALIZE_TYPE_TRAITS_WHLSL_TYPE(ReferenceType, isReferenceType())

#endif
