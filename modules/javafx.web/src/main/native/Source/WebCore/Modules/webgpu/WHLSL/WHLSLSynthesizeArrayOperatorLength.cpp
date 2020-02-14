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

#include "config.h"
#include "WHLSLSynthesizeArrayOperatorLength.h"

#if ENABLE(WEBGPU)

#include "WHLSLArrayType.h"
#include "WHLSLProgram.h"
#include "WHLSLTypeReference.h"
#include "WHLSLVisitor.h"

namespace WebCore {

namespace WHLSL {

class FindArrayTypes : public Visitor {
public:
    virtual ~FindArrayTypes() = default;

    void visit(AST::ArrayType& arrayType) override
    {
        m_arrayTypes.append(arrayType);
        Visitor::visit(arrayType);
    }

    Vector<std::reference_wrapper<AST::ArrayType>> takeArrayTypes()
    {
        return WTFMove(m_arrayTypes);
    }

private:
    Vector<std::reference_wrapper<AST::ArrayType>> m_arrayTypes;
};

Expected<void, Error> synthesizeArrayOperatorLength(Program& program)
{
    FindArrayTypes findArrayTypes;
    findArrayTypes.checkErrorAndVisit(program);
    auto arrayTypes = findArrayTypes.takeArrayTypes();

    bool isOperator = true;

    for (auto& arrayType : arrayTypes) {
        auto location = arrayType.get().codeLocation();
        auto variableDeclaration = makeUniqueRef<AST::VariableDeclaration>(location, AST::Qualifiers(), &arrayType.get(), String(), nullptr, nullptr);
        AST::VariableDeclarations parameters;
        parameters.append(WTFMove(variableDeclaration));
        AST::NativeFunctionDeclaration nativeFunctionDeclaration(AST::FunctionDeclaration(location, AST::AttributeBlock(), WTF::nullopt, AST::TypeReference::wrap(location, program.intrinsics().uintType()), "operator.length"_str, WTFMove(parameters), nullptr, isOperator, ParsingMode::StandardLibrary));
        if (!program.append(WTFMove(nativeFunctionDeclaration)))
            return makeUnexpected(Error("Cannot synthesize operator.length for array type."));
    }
    return { };
}

} // namespace WHLSL

} // namespace WebCore

#endif // ENABLE(WEBGPU)
