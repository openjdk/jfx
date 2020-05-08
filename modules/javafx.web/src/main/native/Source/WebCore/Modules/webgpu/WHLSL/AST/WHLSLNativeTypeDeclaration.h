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
#include "WHLSLNamedType.h"
#include "WHLSLTypeArgument.h"
#include "WHLSLTypeReference.h"
#include <wtf/FastMalloc.h>
#include <wtf/text/WTFString.h>

namespace WebCore {

namespace WHLSL {

namespace AST {

class NativeTypeDeclaration final : public NamedType {
    WTF_MAKE_FAST_ALLOCATED;
public:
    NativeTypeDeclaration(CodeLocation location, String&& name, TypeArguments&& typeArguments)
        : NamedType(Kind::NativeTypeDeclaration, location, WTFMove(name))
        , m_typeArguments(WTFMove(typeArguments))
    {
    }

    ~NativeTypeDeclaration() = default;

    NativeTypeDeclaration(const NativeTypeDeclaration&) = delete;
    NativeTypeDeclaration(NativeTypeDeclaration&&) = default;

    TypeArguments& typeArguments() { return m_typeArguments; }

    bool isInt() const { return m_isInt; }
    bool isNumber() const { return m_isNumber; }
    bool isFloating() const { return m_isFloating; }
    bool isAtomic() const { return m_isAtomic; }
    bool isVector() const { return m_isVector; }
    bool isMatrix() const { return m_isMatrix; }
    bool isOpaqueType() const { return m_isOpaqueType; }
    bool isTexture() const { return m_isTexture; }
    bool isTextureArray() const { return m_isTextureArray; }
    bool isDepthTexture() const { return m_isDepthTexture; }
    bool isWritableTexture() const { return m_isWritableTexture; }
    uint textureDimension() const { return m_textureDimension; }
    bool isSigned() const { return m_isSigned; }
    const std::function<bool(int)>& canRepresentInteger() const { return m_canRepresentInteger; }
    const std::function<bool(unsigned)>& canRepresentUnsignedInteger() const { return m_canRepresentUnsignedInteger; }
    const std::function<bool(float)>& canRepresentFloat() const { return m_canRepresentFloat; }
    const std::function<int64_t(int64_t)>& successor() const { return m_successor; }
    const std::function<int64_t(int)>& formatValueFromInteger() const { return m_formatValueFromInteger; }
    const std::function<int64_t(unsigned)>& formatValueFromUnsignedInteger() const { return m_formatValueFromUnsignedInteger; }
    void iterateAllValues(const std::function<bool(int64_t)>& callback) { m_iterateAllValues(callback); }
private:
    unsigned matrixDimension(unsigned typeArgumentIndex)
    {
        ASSERT(isMatrix());
        ASSERT(typeArguments().size() == 3);
        return WTF::get<AST::ConstantExpression>(typeArguments()[typeArgumentIndex]).integerLiteral().value();
    }
public:
    unsigned numberOfMatrixRows()
    {
        return matrixDimension(1);
    }
    unsigned numberOfMatrixColumns()
    {
        return matrixDimension(2);
    }

    void setIsInt() { m_isInt = true; }
    void setIsNumber() { m_isNumber = true; }
    void setIsFloating() { m_isFloating = true; }
    void setIsAtomic() { m_isAtomic = true; }
    void setIsVector() { m_isVector = true; }
    void setIsMatrix() { m_isMatrix = true; }
    void setIsOpaqueType() { m_isOpaqueType = true; }
    void setIsTexture() { m_isTexture = true; }
    void setIsTextureArray() { m_isTextureArray = true; }
    void setIsDepthTexture() { m_isDepthTexture = true; }
    void setIsWritableTexture() { m_isWritableTexture = true; }
    void setTextureDimension(uint textureDimension) { m_textureDimension = textureDimension; }
    void setIsSigned() { m_isSigned = true; }
    void setCanRepresentInteger(std::function<bool(int)>&& canRepresent) { m_canRepresentInteger = WTFMove(canRepresent); }
    void setCanRepresentUnsignedInteger(std::function<bool(unsigned)>&& canRepresent) { m_canRepresentUnsignedInteger = WTFMove(canRepresent); }
    void setCanRepresentFloat(std::function<bool(float)>&& canRepresent) { m_canRepresentFloat = WTFMove(canRepresent); }
    void setSuccessor(std::function<int64_t(int64_t)>&& successor) { m_successor = WTFMove(successor); }
    void setFormatValueFromInteger(std::function<int64_t(int)>&& formatValue) { m_formatValueFromInteger = WTFMove(formatValue); }
    void setFormatValueFromUnsignedInteger(std::function<int64_t(unsigned)>&& formatValue) { m_formatValueFromUnsignedInteger = WTFMove(formatValue); }
    void setIterateAllValues(std::function<void(const std::function<bool(int64_t)>&)>&& iterateAllValues) { m_iterateAllValues = WTFMove(iterateAllValues); }

private:
    TypeArguments m_typeArguments;
    std::function<bool(int)> m_canRepresentInteger;
    std::function<bool(unsigned)> m_canRepresentUnsignedInteger;
    std::function<bool(float)> m_canRepresentFloat;
    std::function<int64_t(int64_t)> m_successor;
    std::function<int64_t(int)> m_formatValueFromInteger;
    std::function<int64_t(unsigned)> m_formatValueFromUnsignedInteger;
    std::function<void(const std::function<bool(int64_t)>&)> m_iterateAllValues;
    uint m_textureDimension { 0 };
    bool m_isInt { false };
    bool m_isNumber { false };
    bool m_isFloating { false };
    bool m_isAtomic { false };
    bool m_isVector { false };
    bool m_isMatrix { false };
    bool m_isOpaqueType { false };
    bool m_isTexture { false };
    bool m_isTextureArray { false };
    bool m_isDepthTexture { false };
    bool m_isWritableTexture { false };
    bool m_isSigned { false };
};

} // namespace AST

}

}

SPECIALIZE_TYPE_TRAITS_WHLSL_TYPE(NativeTypeDeclaration, isNativeTypeDeclaration())

#endif
