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

#include "WHLSLExpression.h"
#include "WHLSLStatement.h"
#include <wtf/FastMalloc.h>
#include <wtf/UniqueRef.h>

namespace WebCore {

namespace WHLSL {

namespace AST {

class DoWhileLoop final : public Statement {
    WTF_MAKE_FAST_ALLOCATED;
public:
    DoWhileLoop(CodeLocation location, UniqueRef<Statement>&& body, UniqueRef<Expression>&& conditional)
        : Statement(location, Kind::DoWhileLoop)
        , m_body(WTFMove(body))
        , m_conditional(WTFMove(conditional))
    {
    }

    ~DoWhileLoop() = default;

    DoWhileLoop(const DoWhileLoop&) = delete;
    DoWhileLoop(DoWhileLoop&&) = default;

    Statement& body() { return m_body; }
    Expression& conditional() { return m_conditional; }

private:
    UniqueRef<Statement> m_body;
    UniqueRef<Expression> m_conditional;
};

} // namespace AST

}

}

DEFINE_DEFAULT_DELETE(DoWhileLoop)

SPECIALIZE_TYPE_TRAITS_WHLSL_STATEMENT(DoWhileLoop, isDoWhileLoop())

#endif
