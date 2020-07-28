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

#include "WHLSLCodeLocation.h"
#include <wtf/FastMalloc.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

namespace WHLSL {

namespace AST {

class EnumerationDefinition;
class EnumerationMember;

class EnumerationMemberLiteral final : public Expression {
    WTF_MAKE_FAST_ALLOCATED;
public:
    EnumerationMemberLiteral(CodeLocation location, String&& left, String&& right)
        : Expression(location, Kind::EnumerationMemberLiteral)
        , m_left(WTFMove(left))
        , m_right(WTFMove(right))
    {
    }

    ~EnumerationMemberLiteral() = default;

    explicit EnumerationMemberLiteral(const EnumerationMemberLiteral&) = delete;
    EnumerationMemberLiteral(EnumerationMemberLiteral&&) = default;

    EnumerationMemberLiteral& operator=(const EnumerationMemberLiteral&) = delete;
    EnumerationMemberLiteral& operator=(EnumerationMemberLiteral&&) = default;

    static EnumerationMemberLiteral wrap(CodeLocation location, String&& left, String&& right, EnumerationDefinition& enumerationDefinition, EnumerationMember& enumerationMember)
    {
        EnumerationMemberLiteral result(location, WTFMove(left), WTFMove(right));
        result.m_enumerationDefinition = &enumerationDefinition;
        result.m_enumerationMember = &enumerationMember;
        return result;
    }

    const String& left() const { return m_left; }
    const String& right() const { return m_right; }

    EnumerationMemberLiteral clone() const
    {
        auto result = EnumerationMemberLiteral(codeLocation(), String(m_left), String(m_right));
        result.m_enumerationMember = m_enumerationMember;
        copyTypeTo(result);
        return result;
    }

    EnumerationDefinition* enumerationDefinition()
    {
        return m_enumerationDefinition;
    }

    EnumerationDefinition* enumerationDefinition() const
    {
        return m_enumerationDefinition;
    }

    EnumerationMember* enumerationMember()
    {
        return m_enumerationMember;
    }

    EnumerationMember* enumerationMember() const
    {
        return m_enumerationMember;
    }

    void setEnumerationMember(EnumerationDefinition& enumerationDefinition, EnumerationMember& enumerationMember)
    {
        m_enumerationDefinition = &enumerationDefinition;
        m_enumerationMember = &enumerationMember;
    }

private:
    String m_left;
    String m_right;
    EnumerationDefinition* m_enumerationDefinition { nullptr };
    EnumerationMember* m_enumerationMember { nullptr };
};

} // namespace AST

}

}

DEFINE_DEFAULT_DELETE(EnumerationMemberLiteral)

SPECIALIZE_TYPE_TRAITS_WHLSL_EXPRESSION(EnumerationMemberLiteral, isEnumerationMemberLiteral())

#endif
