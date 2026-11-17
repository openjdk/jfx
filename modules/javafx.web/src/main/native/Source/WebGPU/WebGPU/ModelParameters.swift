// Copyright (C) 2025 Apple Inc. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS''
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
// THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS
// BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
// THE POSSIBILITY OF SUCH DAMAGE.

#if canImport(RealityCoreRenderer, _version: 9999)

@_spi(RealityCoreRendererAPI) import RealityCoreRenderer

nonisolated func makeParameters(
    for function: _Proto_LowLevelMaterialResource_v1.Function,
    resourceContext: _Proto_LowLevelResourceContext_v1,
    renderer: _Proto_LowLevelRenderer_v1,
    buffers: [_Proto_LowLevelBufferSpan_v1] = [],
    textures: [_Proto_LowLevelTextureResource_v1] = []
) throws -> _Proto_LowLevelArgumentBuffer_v1? {
    guard let argumentTableDescriptor = function.argumentBufferDescriptor?.table else {
        return nil
    }
    let parametersTable = try resourceContext.makeArgumentTable(
        descriptor: argumentTableDescriptor,
        buffers: buffers,
        textures: textures
    )
    return try renderer.makeArgumentBuffer(for: parametersTable, function: function)
}

nonisolated func makeParameters(
    for material: _Proto_LowLevelMaterialResource_v1,
    resourceContext: _Proto_LowLevelResourceContext_v1,
    renderer: _Proto_LowLevelRenderer_v1,
    geometryBuffers: [_Proto_LowLevelBufferSpan_v1] = [],
    geometryTextures: [_Proto_LowLevelTextureResource_v1] = [],
    surfaceBuffers: [_Proto_LowLevelBufferSpan_v1] = [],
    surfaceTextures: [_Proto_LowLevelTextureResource_v1] = [],
    lightingBuffers: [_Proto_LowLevelBufferSpan_v1] = [],
    lightingTextures: [_Proto_LowLevelTextureResource_v1] = []
) throws
    -> (
        geometryArguments: _Proto_LowLevelArgumentBuffer_v1?,
        surfaceArguments: _Proto_LowLevelArgumentBuffer_v1?,
        lightingArguments: _Proto_LowLevelArgumentBuffer_v1?
    )
{
    let geometryArguments = try makeParameters(
        for: material.geometry,
        resourceContext: resourceContext,
        renderer: renderer,
        buffers: geometryBuffers,
        textures: geometryTextures
    )
    let surfaceArguments = try makeParameters(
        for: material.surface,
        resourceContext: resourceContext,
        renderer: renderer,
        buffers: surfaceBuffers,
        textures: surfaceTextures
    )
    let lightingArguments = try makeParameters(
        for: material.lighting,
        resourceContext: resourceContext,
        renderer: renderer,
        buffers: lightingBuffers,
        textures: lightingTextures
    )

    return (geometryArguments, surfaceArguments, lightingArguments)
}

#endif
