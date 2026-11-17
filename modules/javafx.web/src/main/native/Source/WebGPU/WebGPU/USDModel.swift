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

internal import Metal
internal import OSLog
internal import WebGPU_Private.DDModelTypes
internal import simd

#if canImport(RealityCoreRenderer, _version: 9999)
@_spi(RealityCoreRendererAPI) @_spi(ShaderGraph) import RealityCoreRenderer
@_spi(RealityCoreRendererAPI) @_spi(ShaderGraph) import RealityKit
@_spi(UsdLoaderAPI) import _USDStageKit_SwiftUI
@_spi(SwiftAPI) import DirectResource
import USDStageKit
import _USDStageKit_SwiftUI
import ShaderGraph
import RealityCoreDeformation

extension _USDStageKit_SwiftUI._Proto_MeshDataUpdate_v1 {
    @_silgen_name("$s20_USDStageKit_SwiftUI24_Proto_MeshDataUpdate_v1V18instanceTransformsSaySo13simd_float4x4aGvg")
    internal func instanceTransformsCompat() -> [simd_float4x4]
}

extension _USDStageKit_SwiftUI._Proto_DeformationData_v1.SkinningData {
    @_silgen_name("$s20_USDStageKit_SwiftUI25_Proto_DeformationData_v1V08SkinningG0V21geometryBindTransformSo13simd_float4x4avg")
    internal func geometryBindTransformCompat() -> simd_float4x4
}

extension _USDStageKit_SwiftUI._Proto_DeformationData_v1.SkinningData {
    @_silgen_name("$s20_USDStageKit_SwiftUI25_Proto_DeformationData_v1V08SkinningG0V15jointTransformsSaySo13simd_float4x4aGvg")
    internal func jointTransformsCompat() -> [simd_float4x4]
}

extension _USDStageKit_SwiftUI._Proto_DeformationData_v1.SkinningData {
    @_silgen_name("$s20_USDStageKit_SwiftUI25_Proto_DeformationData_v1V08SkinningG0V16inverseBindPosesSaySo13simd_float4x4aGvg")
    internal func inverseBindPosesCompat() -> [simd_float4x4]
}

extension MTLCaptureDescriptor {
    fileprivate convenience init(from device: MTLDevice?) {
        self.init()

        captureObject = device
        destination = .gpuTraceDocument
        let now = Date()
        let dateFormatter = DateFormatter()
        dateFormatter.timeZone = .current
        dateFormatter.dateFormat = "yyyy-MM-dd-HH-mm-ss-SSSS"
        let dateString = dateFormatter.string(from: now)

        outputURL = URL.temporaryDirectory.appending(path: "capture_\(dateString).gputrace").standardizedFileURL
    }
}

private func mapSemantic(_ semantic: Int) -> _Proto_LowLevelMeshResource_v1.VertexSemantic {
    switch semantic {
    case 0: .position
    case 1: .color
    case 2: .normal
    case 3: .tangent
    case 4: .bitangent
    case 5: .uv0
    case 6: .uv1
    case 7: .uv2
    case 8: .uv3
    case 9: .uv4
    case 10: .uv5
    case 11: .uv6
    case 12: .uv7
    case 13: .unspecified
    default: .unspecified
    }
}

extension _Proto_LowLevelMeshResource_v1.Descriptor {
    nonisolated static func fromLlmDescriptor(_ llmDescriptor: DDBridgeMeshDescriptor) -> Self {
        var descriptor = Self.init()
        descriptor.vertexCapacity = Int(llmDescriptor.vertexCapacity)
        descriptor.vertexAttributes = llmDescriptor.vertexAttributes.map { attribute in
            .init(
                semantic: mapSemantic(attribute.semantic),
                format: MTLVertexFormat(rawValue: UInt(attribute.format)) ?? .invalid,
                layoutIndex: attribute.layoutIndex,
                offset: attribute.offset
            )
        }
        descriptor.vertexLayouts = llmDescriptor.vertexLayouts.map { layout in
            .init(bufferIndex: layout.bufferIndex, bufferOffset: layout.bufferOffset, bufferStride: layout.bufferStride)
        }
        descriptor.indexCapacity = llmDescriptor.indexCapacity
        descriptor.indexType = llmDescriptor.indexType

        return descriptor
    }
}

private func isNonZero(value: Float) -> Bool {
    abs(value) > Float.ulpOfOne
}

private func isNonZero(_ vector: simd_float4) -> Bool {
    isNonZero(value: vector[0]) || isNonZero(value: vector[1]) || isNonZero(value: vector[2]) || isNonZero(value: vector[3])
}

private func isNonZero(matrix: simd_float4x4) -> Bool {
    isNonZero(_: matrix.columns.0) || isNonZero(_: matrix.columns.1) || isNonZero(_: matrix.columns.2) || isNonZero(_: matrix.columns.3)
}

private func makeTextureFromImageAsset(
    _ imageAsset: DDBridgeImageAsset,
    device: MTLDevice,
    resourceContext: _Proto_LowLevelResourceContext_v1,
    commandQueue: MTLCommandQueue,
    generateMips: Bool,
    swizzle: MTLTextureSwizzleChannels = .init(red: .red, green: .green, blue: .blue, alpha: .alpha)
) -> _Proto_LowLevelTextureResource_v1? {
    guard let imageAssetData = imageAsset.data else {
        logError("no image data")
        return nil
    }
    logError(
        "imageAssetData = \(imageAssetData)  -  width = \(imageAsset.width)  -  height = \(imageAsset.height)  -  bytesPerPixel = \(imageAsset.bytesPerPixel) imageAsset.pixelFormat:  \(imageAsset.pixelFormat)"
    )

    var pixelFormat = imageAsset.pixelFormat
    if imageAsset.textureType != .typeCube {
        switch imageAsset.bytesPerPixel {
        case 1:
            pixelFormat = .r8Unorm
        case 2:
            pixelFormat = .rg8Unorm
        case 4:
            pixelFormat = .rgba8Unorm
        default:
            pixelFormat = .rgba8Unorm
        }
    }

    let (textureDescriptor, sliceCount) =
        switch imageAsset.textureType {
        case .typeCube:
            (
                MTLTextureDescriptor.textureCubeDescriptor(
                    pixelFormat: pixelFormat,
                    size: imageAsset.width,
                    mipmapped: generateMips
                ), 6
            )
        default:
            (
                MTLTextureDescriptor.texture2DDescriptor(
                    pixelFormat: pixelFormat,
                    width: imageAsset.width,
                    height: imageAsset.height,
                    mipmapped: generateMips
                ), 1
            )
        }

    textureDescriptor.usage = .shaderRead
    textureDescriptor.storageMode = .shared

    guard let mtlTexture = device.makeTexture(descriptor: textureDescriptor) else {
        logError("failed to device.makeTexture")
        return nil
    }

    let bytesPerRow = imageAsset.width * imageAsset.bytesPerPixel
    let bytesPerImage = bytesPerRow * imageAsset.height

    unsafe imageAssetData.bytes.withUnsafeBytes { textureBytes in
        guard let textureBytesBaseAddress = textureBytes.baseAddress else {
            return
        }
        if imageAsset.bytesPerPixel == 0 {
            logError("bytesPerPixel == 0")
            fatalError()
        }
        for face in 0..<sliceCount {
            let offset = face * bytesPerImage
            let facePointer = unsafe textureBytesBaseAddress.advanced(by: offset)

            unsafe mtlTexture.replace(
                region: MTLRegionMake2D(0, 0, imageAsset.width, imageAsset.height),
                mipmapLevel: 0,
                slice: face,
                withBytes: facePointer,
                bytesPerRow: bytesPerRow,
                bytesPerImage: bytesPerImage
            )
        }
    }

    let descriptor = _Proto_LowLevelTextureResource_v1.Descriptor.from(mtlTexture, swizzle: swizzle)
    if let textureResource = try? resourceContext.makeTextureResource(descriptor: descriptor) {
        guard let commandBuffer = commandQueue.makeCommandBuffer() else {
            fatalError("Could not create command buffer")
        }
        guard let blitEncoder = commandBuffer.makeBlitCommandEncoder() else {
            fatalError("Could not create blit encoder")
        }
        if generateMips {
            blitEncoder.generateMipmaps(for: mtlTexture)
        }

        let outTexture = textureResource.replace(using: commandBuffer)
        blitEncoder.copy(from: mtlTexture, to: outTexture)

        blitEncoder.endEncoding()
        commandBuffer.commit()
        commandBuffer.waitUntilCompleted()
        return textureResource
    }

    return nil
}

private func makeParameters(
    for function: _Proto_LowLevelMaterialResource_v1.Function?,
    resourceContext: _Proto_LowLevelResourceContext_v1,
    renderer: _Proto_LowLevelRenderer_v1,
    textureResources: [String: _Proto_LowLevelTextureResource_v1]
) throws -> _Proto_LowLevelArgumentBuffer_v1? {
    guard let function else { return nil }
    guard let argumentTableDescriptor = function.argumentBufferDescriptor?.table else { return nil }
    let parameterMapping = function.parameterMapping

    var optTextures: [_Proto_LowLevelTextureResource_v1?] = argumentTableDescriptor.textures.map({ _ in nil })
    for parameter in parameterMapping?.textures ?? [] {
        guard let textureResource = textureResources[parameter.name] else {
            fatalError("Failed to find texture resource \(parameter.name)")
        }
        optTextures[parameter.textureIndex] = textureResource
    }
    let textures = optTextures.map({ $0! })

    let buffers: [_Proto_LowLevelBufferSpan_v1] = try argumentTableDescriptor.buffers.map { bufferRequirements in
        let capacity = (bufferRequirements.size + 16 - 1) / 16 * 16
        let buffer = try resourceContext.makeBufferResource(descriptor: .init(capacity: capacity))
        buffer.replace { span in
            for byteOffset in span.byteOffsets {
                span.storeBytes(of: 0, toByteOffset: byteOffset, as: UInt8.self)
            }
        }
        return try _Proto_LowLevelBufferSpan_v1(buffer: buffer, offset: 0, size: bufferRequirements.size)
    }

    let parametersTable = try resourceContext.makeArgumentTable(
        descriptor: argumentTableDescriptor,
        buffers: buffers,
        textures: textures
    )
    let parameters = try renderer.makeArgumentBuffer(for: parametersTable, function: function)
    return parameters
}

extension Logger {
    fileprivate static let modelGPU = Logger(subsystem: "com.apple.WebKit", category: "model")
}

private func logError(_ error: String) {
    Logger.modelGPU.error("\(error)")
}

private func logInfo(_ info: String) {
    Logger.modelGPU.info("\(info)")
}

extension simd_float4x4 {
    fileprivate var minor: simd_float3x3 {
        .init(
            [self.columns.0.x, self.columns.0.y, self.columns.0.z],
            [self.columns.1.x, self.columns.1.y, self.columns.1.z],
            [self.columns.2.x, self.columns.2.y, self.columns.2.z]
        )
    }
}

private class RenderTargetWrapper {
    var descriptor: _Proto_LowLevelRenderTarget_v1.Descriptor?
}

@objc
@implementation
extension DDUSDConfiguration {
    @nonobjc
    fileprivate let device: MTLDevice
    @nonobjc
    fileprivate let appRenderer: Renderer
    @nonobjc
    fileprivate final var commandQueue: MTLCommandQueue {
        get { appRenderer.commandQueue }
    }
    @nonobjc
    fileprivate final var renderer: _Proto_LowLevelRenderer_v1 {
        get { appRenderer.renderer }
    }
    @nonobjc
    fileprivate final var resourceContext: _Proto_LowLevelResourceContext_v1 {
        get { appRenderer.resourceContext }
    }
    @nonobjc
    fileprivate final var materialCompiler: _Proto_LowLevelMaterialCompiler_v1 {
        get {
            // swift-format-ignore: NeverForceUnwrap
            appRenderer.materialCompiler!
        }
    }
    @nonobjc
    fileprivate let renderTargetWrapper = RenderTargetWrapper()
    @nonobjc
    fileprivate final var renderTarget: _Proto_LowLevelRenderTarget_v1.Descriptor {
        get {
            // swift-format-ignore: NeverForceUnwrap
            renderTargetWrapper.descriptor!
        }
        set { renderTargetWrapper.descriptor = newValue }
    }

    @objc(initWithDevice:)
    init(device: MTLDevice) {
        let renderTarget = _Proto_LowLevelRenderTarget_v1.Descriptor.texture(color: .bgra8Unorm, sampleCount: 4)
        self.renderTargetWrapper.descriptor = renderTarget
        self.device = device
        do {
            self.appRenderer = try Renderer(device: device, renderTargetDescriptor: .texture(color: .bgra8Unorm, sampleCount: 4))
        } catch {
            fatalError("Exception creating renderer \(error)")
        }
    }

    @objc(createMaterialCompiler:)
    func createMaterialCompiler() async {
        do {
            try await self.appRenderer.createMaterialCompiler()
        } catch {
            fatalError("Exception creating renderer \(error)")
        }
    }
}

extension DDBridgeReceiver {
    fileprivate func configureDeformation(
        identifier: _Proto_ResourceId,
        deformationData: DDBridgeDeformationData,
        commandBuffer: MTLCommandBuffer
    ) {
        var deformers: [_Proto_LowLevelDeformerDescription_v1] = []

        if let skinningData = deformationData.skinningData {
            let skinningDeformer = skinningData.makeDeformerDescription(device: self.device)
            deformers.append(skinningDeformer)
        }

        // swift-format-ignore: NeverForceUnwrap
        let meshResource = meshResources[identifier]!

        var inputMeshDescription: _Proto_LowLevelDeformationDescription_v1.MeshDescription?
        if self.meshResourceToDeformationContext[identifier] == nil {
            // swift-format-ignore: NeverForceUnwrap
            let vertexPositionsBuffer = meshResource.readVertices(at: 1, using: commandBuffer)!
            // swift-format-ignore: NeverForceUnwrap
            let inputPositionsBuffer = device.makeBuffer(length: vertexPositionsBuffer.length, options: .storageModeShared)!

            // Copy data from vertexPositionsBuffer to inputPositionsBuffer
            // swift-format-ignore: NeverForceUnwrap
            let blitEncoder = commandBuffer.makeBlitCommandEncoder()!
            blitEncoder.copy(
                from: vertexPositionsBuffer,
                sourceOffset: 0,
                to: inputPositionsBuffer,
                destinationOffset: 0,
                size: vertexPositionsBuffer.length
            )
            blitEncoder.endEncoding()

            // swift-format-ignore: NeverForceUnwrap
            let inputPositions = _Proto_LowLevelDeformationDescription_v1.Buffer.make(
                inputPositionsBuffer,
                offset: 0,
                occupiedLength: inputPositionsBuffer.length,
                elementType: .float3
            )!
            inputMeshDescription = _Proto_LowLevelDeformationDescription_v1.MeshDescription(descriptions: [
                _Proto_LowLevelDeformationDescription_v1.SemanticBuffer(.position, inputPositions)
            ])
        } else {
            // swift-format-ignore: NeverForceUnwrap
            inputMeshDescription = self.meshResourceToDeformationContext[identifier]!.description.input
        }

        guard let inputMeshDescription else {
            logError("inputMeshDescription is unexpectedly nil")
            return
        }

        // swift-format-ignore: NeverForceUnwrap
        let outputPositionsBuffer = meshResource.replaceVertices(at: 1, using: commandBuffer)!
        // swift-format-ignore: NeverForceUnwrap
        let outputPositions = _Proto_LowLevelDeformationDescription_v1.Buffer.make(
            outputPositionsBuffer,
            offset: 0,
            occupiedLength: outputPositionsBuffer.length,
            elementType: .float3
        )!

        let outputMeshDescription = _Proto_LowLevelDeformationDescription_v1.MeshDescription(descriptions: [
            _Proto_LowLevelDeformationDescription_v1.SemanticBuffer(.position, outputPositions)
        ])

        guard
            let deformationDescription =
                try? _Proto_LowLevelDeformationDescription_v1.make(
                    input: inputMeshDescription,
                    deformers: deformers,
                    output: outputMeshDescription
                )
                .get()
        else {
            logError("_Proto_LowLevelDeformationDescription_v1.make failed unexpectedly")
            return
        }

        // swift-format-ignore: NeverForceUnwrap
        guard let deformation = try? self.deformationSystem.make(description: deformationDescription).get() else {
            logError("deformationSystem.make failed unexpectedly")
            return
        }

        self.meshResourceToDeformationContext[identifier] = DeformationContext.init(
            deformation: deformation,
            description: deformationDescription,
            dirty: true
        )
    }
}

func modelTransformToCameraTransform(_ modelTransform: simd_float4x4, _ distance: Float) -> CameraTransform {
    let inverted = modelTransform.inverse

    // Extract the upper-left 3x3 (rotation + scale)
    let col0 = simd_float3(inverted.columns.0.x, inverted.columns.0.y, inverted.columns.0.z)
    let col1 = simd_float3(inverted.columns.1.x, inverted.columns.1.y, inverted.columns.1.z)
    let col2 = simd_float3(inverted.columns.2.x, inverted.columns.2.y, inverted.columns.2.z)

    // Extract uniform scale
    let scale = length(col0)

    // Normalize rotation matrix (remove scale)
    let rotationMatrix = simd_float3x3(
        col0 / scale,
        col1 / scale,
        col2 / scale
    )

    // Convert to quaternion
    let rotation = simd_quatf(rotationMatrix)

    // Extract model center position
    let modelCenter = simd_float3(
        modelTransform.columns.3.x,
        modelTransform.columns.3.y,
        modelTransform.columns.3.z
    )

    // Calculate camera position: move back from model center along camera's forward direction
    // Camera forward is typically -Z in camera space, which is the negative of the third column
    let cameraForward = -normalize(col2 / scale)
    let translation = modelCenter + cameraForward * distance * scale

    return CameraTransform(
        rotation: rotation,
        translation: translation,
        scale: simd_make_float3(1, 1, 1)
    )
}

@objc
@implementation
extension DDBridgeReceiver {
    @nonobjc
    fileprivate let device: MTLDevice
    @nonobjc
    fileprivate let commandQueue: MTLCommandQueue

    @nonobjc
    fileprivate let resourceContext: _Proto_LowLevelResourceContext_v1
    @nonobjc
    fileprivate let renderer: _Proto_LowLevelRenderer_v1
    @nonobjc
    fileprivate let appRenderer: Renderer
    @nonobjc
    fileprivate let materialCompiler: _Proto_LowLevelMaterialCompiler_v1
    @nonobjc
    fileprivate let lightingFunction: _Proto_LowLevelMaterialResource_v1.LightingFunction
    @nonobjc
    fileprivate var lightingArgumentBuffer: _Proto_LowLevelArgumentBuffer_v1?

    @nonobjc
    private let renderTargetWrapper = RenderTargetWrapper()
    @nonobjc
    private final var renderTarget: _Proto_LowLevelRenderTarget_v1.Descriptor {
        get {
            // swift-format-ignore: NeverForceUnwrap
            renderTargetWrapper.descriptor!
        }
        set { renderTargetWrapper.descriptor = newValue }
    }
    @nonobjc
    fileprivate var meshInstancePlainArray: [_Proto_LowLevelMeshInstance_v1?]
    @nonobjc
    fileprivate var meshInstances: _Proto_LowLevelMeshInstanceArray_v1

    @nonobjc
    fileprivate var meshResources: [_Proto_ResourceId: _Proto_LowLevelMeshResource_v1] = [:]
    @nonobjc
    fileprivate var meshResourceToMaterials: [_Proto_ResourceId: [_Proto_ResourceId]] = [:]
    @nonobjc
    fileprivate var meshToMeshInstances: [_Proto_ResourceId: [_Proto_LowLevelMeshInstance_v1]] = [:]
    @nonobjc
    fileprivate var meshTransforms: [_Proto_ResourceId: [simd_float4x4]] = [:]
    @nonobjc
    fileprivate var rotationAngle: Float = 0

    @nonobjc
    fileprivate let deformationSystem: _Proto_LowLevelDeformationSystem_v1

    struct DeformationContext {
        let deformation: _Proto_Deformation_v1
        var description: _Proto_LowLevelDeformationDescription_v1
        var dirty: Bool
    }
    @nonobjc
    fileprivate var meshResourceToDeformationContext: [_Proto_ResourceId: DeformationContext] = [:]

    struct Material {
        let resource: _Proto_LowLevelMaterialResource_v1
        let geometryArguments: _Proto_LowLevelArgumentBuffer_v1?
        let surfaceArguments: _Proto_LowLevelArgumentBuffer_v1?
    }
    @nonobjc
    fileprivate var materialsAndParams: [_Proto_ResourceId: Material] = [:]

    @nonobjc
    fileprivate var textureResources: [String: _Proto_LowLevelTextureResource_v1] = [:]
    @nonobjc
    fileprivate var textureData: [_Proto_ResourceId: (MTLTexture, String)] = [:]

    @nonobjc
    fileprivate var modelTransform: simd_float4x4
    @nonobjc
    fileprivate var modelDistance: Float

    @nonobjc
    fileprivate var dontCaptureAgain: Bool = false

    init(
        configuration: DDUSDConfiguration,
        diffuseAsset: DDBridgeImageAsset,
        specularAsset: DDBridgeImageAsset
    ) throws {
        self.materialCompiler = configuration.materialCompiler
        self.resourceContext = configuration.resourceContext
        self.renderer = configuration.renderer
        self.appRenderer = configuration.appRenderer
        self.device = configuration.device
        self.commandQueue = configuration.commandQueue
        self.deformationSystem = try _Proto_LowLevelDeformationSystem_v1.make(configuration.device, configuration.commandQueue).get()
        modelTransform = matrix_identity_float4x4
        modelDistance = 1.0
        self.meshInstancePlainArray = []
        self.meshInstances = _Proto_LowLevelMeshInstanceArray_v1(
            renderTarget: configuration.renderTarget,
            resourceContext: configuration.resourceContext,
            count: 16
        )
        let lightingFunction = materialCompiler.makePhysicallyBasedLightingFunction()
        guard
            let diffuseTexture = makeTextureFromImageAsset(
                diffuseAsset,
                device: device,
                resourceContext: resourceContext,
                commandQueue: configuration.commandQueue,
                generateMips: true,
                swizzle: .init(red: .red, green: .red, blue: .red, alpha: .one)
            )
        else {
            fatalError("Could not create diffuseTexture")
        }
        guard
            let specularTexture = makeTextureFromImageAsset(
                specularAsset,
                device: device,
                resourceContext: resourceContext,
                commandQueue: configuration.commandQueue,
                generateMips: true,
                swizzle: .init(red: .red, green: .red, blue: .red, alpha: .one)
            )
        else {
            fatalError("Could not create specularTexture")
        }
        self.lightingFunction = lightingFunction
        self.renderTargetWrapper.descriptor = configuration.renderTarget
        do {
            // swift-format-ignore: NeverForceUnwrap
            let lightingFunctionDescriptor = lightingFunction.argumentBufferDescriptor!
            // swift-format-ignore: NeverForceUnwrap
            let lightingFunctionTable = lightingFunctionDescriptor.table!
            let lightingArgumentTable = try self.resourceContext.makeArgumentTable(
                descriptor: lightingFunctionTable,
                buffers: [],
                textures: [
                    diffuseTexture, specularTexture,
                ]
            )
            let lightingArgumentBuffer = try self.renderer.makeArgumentBuffer(for: lightingArgumentTable, function: lightingFunction)

            self.lightingArgumentBuffer = lightingArgumentBuffer
        } catch {
            fatalError("EXCEPTION \(error)")
        }
    }

    @objc(renderWithTexture:)
    func render(with texture: MTLTexture) {
        for (identifier, meshes) in meshToMeshInstances {
            let originalTransforms = meshTransforms[identifier]
            // swift-format-ignore: NeverForceUnwrap
            let angle: Float = 0.707
            let rotationY90 = simd_float4x4(
                simd_float4(angle, 0, angle, 0), // column 0
                simd_float4(0, 1, 0, 0), // column 1
                simd_float4(-angle, 0, angle, 0), // column 2
                simd_float4(0, 0, 0, 1) // column 3
            )

            for (index, meshInstance) in meshes.enumerated() {
                // swift-format-ignore: NeverForceUnwrap
                let computedTransform = modelTransform * rotationY90 * originalTransforms![index]
                meshInstance.setTransform(.single(computedTransform))
            }
        }

        // animate
        if !meshResourceToDeformationContext.isEmpty {
            // swift-format-ignore: NeverForceUnwrap
            let commandBuffer = self.commandQueue.makeCommandBuffer()!

            for (identifier, deformationContext) in meshResourceToDeformationContext where deformationContext.dirty {
                deformationContext.deformation.execute(deformation: deformationContext.description, commandBuffer: commandBuffer) {
                    (commandBuffer: any MTLCommandBuffer) in
                }
                // swift-format-ignore: NeverForceUnwrap
                meshResourceToDeformationContext[identifier]!.dirty = false
            }

            commandBuffer.enqueue()
            commandBuffer.commit()
        }

        // render
        if dontCaptureAgain == false {
            let captureDescriptor = MTLCaptureDescriptor(from: device)
            let captureManager = MTLCaptureManager.shared()
            do {
                try captureManager.startCapture(with: captureDescriptor)
                print("Capture started at \(captureDescriptor.outputURL?.absoluteString ?? "")")
            } catch {
                logError("failed to start gpu capture \(error)")
                dontCaptureAgain = true
            }
        }

        do {
            try appRenderer.render(meshInstances: meshInstances, texture: texture)
        } catch {
            logError("failed to start gpu capture \(error)")
        }

        let captureManager = MTLCaptureManager.shared()
        if captureManager.isCapturing {
            captureManager.stopCapture()
        }
    }

    @objc(updateTexture:)
    func updateTexture(_ data: DDBridgeUpdateTexture) {
        guard let asset = data.imageAsset else {
            logError("Image asset was nil")
            return
        }

        let textureHash = data.hashString
        if textureResources[textureHash] != nil {
            logError("Texture already exists")
            return
        }

        let commandQueue = appRenderer.commandQueue
        if let textureResource = makeTextureFromImageAsset(
            asset,
            device: device,
            resourceContext: resourceContext,
            commandQueue: commandQueue,
            generateMips: true
        ) {
            textureResources[textureHash] = textureResource
        }
    }

    @objc(updateMaterial:completionHandler:)
    func updateMaterial(_ data: consuming sending DDBridgeUpdateMaterial) async {
        logInfo("updateMaterial (pre-dispatch) \(data.identifier)")
        do {
            let identifier = data.identifier
            logInfo("updateMaterial \(identifier)")
            let materialSourceArchive = data.materialGraph
            let shaderGraphFunctions = try await materialCompiler.makeShaderGraphFunctions(materialSourceArchive)

            let geometryArguments = try makeParameters(
                for: shaderGraphFunctions.geometryModifier,
                resourceContext: resourceContext,
                renderer: renderer,
                textureResources: textureResources
            )
            let surfaceArguments = try makeParameters(
                for: shaderGraphFunctions.surfaceShader,
                resourceContext: resourceContext,
                renderer: renderer,
                textureResources: textureResources
            )

            let geometryModifier = shaderGraphFunctions.geometryModifier ?? materialCompiler.makeDefaultGeometryModifier()
            let surfaceShader = shaderGraphFunctions.surfaceShader
            let materialResource = try await materialCompiler.makeMaterialResource(
                descriptor: .init(
                    geometryModifier: geometryModifier,
                    surfaceShader: surfaceShader,
                    lightingFunction: lightingFunction
                )
            )

            logInfo("inserting \(identifier) into materialsAndParams")
            materialsAndParams[identifier] = .init(
                resource: materialResource,
                geometryArguments: geometryArguments,
                surfaceArguments: surfaceArguments
            )
        } catch {
            logError("updateMaterial failed \(error)")
        }
    }

    @objc(updateMesh:completionHandler:)
    func updateMesh(_ data: consuming sending DDBridgeUpdateMesh) async {
        let identifier = data.identifier
        logInfo("(update mesh) \(identifier) Material ids \(data.materialPrims)")

        do {
            let identifier = data.identifier

            let meshResource: _Proto_LowLevelMeshResource_v1
            if data.updateType == .initial || data.descriptor != nil {
                let meshDescriptor = data.descriptor!
                let descriptor = _Proto_LowLevelMeshResource_v1.Descriptor.fromLlmDescriptor(meshDescriptor)
                meshResource = try resourceContext.makeMeshResource(descriptor: descriptor)
                meshResource.replaceData(indexData: data.indexData, vertexData: data.vertexData)
                meshResources[identifier] = meshResource
            } else {
                guard let cachedMeshResource = meshResources[identifier] else {
                    fatalError("Mesh resource should already be created from previous update")
                }

                if data.indexData != nil || !data.vertexData.isEmpty {
                    cachedMeshResource.replaceData(indexData: data.indexData, vertexData: data.vertexData)
                }
                meshResource = cachedMeshResource
            }

            if let deformationData = data.deformationData {
                // swift-format-ignore: NeverForceUnwrap
                let commandBuffer = self.commandQueue.makeCommandBuffer()!
                // TODO: delta update
                configureDeformation(identifier: identifier, deformationData: deformationData, commandBuffer: commandBuffer)
                commandBuffer.enqueue()
                commandBuffer.commit()
            }

            if data.instanceTransformsCount > 0 {
                // Make new instances
                if meshToMeshInstances[identifier] == nil {
                    meshToMeshInstances[identifier] = []
                    meshTransforms[identifier] = []

                    for (partIndex, _) in data.parts.enumerated() {
                        let materialIdentifier = data.materialPrims[partIndex]
                        guard let material = materialsAndParams[materialIdentifier] else {
                            fatalError("Material \(materialIdentifier) could not be found")
                        }

                        let pipeline = try await materialCompiler.makeRenderPipelineState(
                            descriptor: .descriptor(mesh: meshResource.descriptor, material: material.resource, renderTarget: renderTarget)
                        )

                        let meshPart = try resourceContext.makeMeshPart(
                            resource: meshResource,
                            indexOffset: data.parts[partIndex].indexOffset,
                            indexCount: data.parts[partIndex].indexCount,
                            primitive: data.parts[partIndex].topology,
                            windingOrder: .counterClockwise,
                            boundsMin: -.one,
                            boundsMax: .one
                        )

                        for instanceTransform in data.instanceTransforms {
                            let meshInstance = try _Proto_LowLevelMeshInstance_v1(
                                meshPart: meshPart,
                                pipeline: pipeline,
                                geometryArguments: material.geometryArguments,
                                surfaceArguments: material.surfaceArguments,
                                lightingArguments: lightingArgumentBuffer,
                                transform: .single(instanceTransform),
                                category: .opaque
                            )

                            // swift-format-ignore: NeverForceUnwrap
                            meshToMeshInstances[identifier]!.append(meshInstance)
                            // swift-format-ignore: NeverForceUnwrap
                            meshTransforms[identifier]!.append(instanceTransform)

                            let meshInstanceIndex = meshInstancePlainArray.count
                            meshInstancePlainArray.append(meshInstance)
                            if meshInstances.count < meshInstancePlainArray.count {
                                let meshInstances = _Proto_LowLevelMeshInstanceArray_v1(
                                    renderTarget: renderTarget,
                                    resourceContext: resourceContext,
                                    count: meshInstances.count * 2
                                )
                                for index in meshInstancePlainArray.indices {
                                    try meshInstances.setMeshInstance(meshInstancePlainArray[index], index: index)
                                }
                                self.meshInstances = meshInstances
                            } else {
                                try meshInstances.setMeshInstance(meshInstance, index: meshInstanceIndex)
                            }
                        }
                    }
                } else {
                    // Update transforms otherwise

                    // swift-format-ignore: NeverForceUnwrap
                    let partCount = meshToMeshInstances[identifier]!.count / data.instanceTransformsCount
                    for (instanceIndex, instanceTransform) in data.instanceTransforms.enumerated() {
                        for partIndex in 0..<partCount {
                            // swift-format-ignore: NeverForceUnwrap
                            let meshInstance = meshToMeshInstances[identifier]![instanceIndex * data.parts.count + partIndex]
                            meshInstance.setTransform(.single(instanceTransform))
                            // swift-format-ignore: NeverForceUnwrap
                            meshTransforms[identifier]![instanceIndex * data.parts.count + partIndex] = instanceTransform
                        }
                    }
                }
            }

            if !data.materialPrims.isEmpty {
                meshResourceToMaterials[identifier] = data.materialPrims
            }
        } catch {
            logError(error.localizedDescription)
        }
    }

    @objc(setTransform:)
    func setTransform(_ transform: simd_float4x4) {
        modelTransform = transform
    }

    @objc
    func setCameraDistance(_ distance: Float) {
        modelDistance = distance
        appRenderer.setCameraDistance(modelDistance)
    }

    @objc
    func setPlaying(_ play: Bool) {
        // resourceContext.setEnableModelRotation(play)
    }
}

private func webPartsFromParts(_ parts: [LowLevelMesh.Part]) -> [DDBridgeMeshPart] {
    parts.map({ a in
        DDBridgeMeshPart(
            indexOffset: a.indexOffset,
            indexCount: a.indexCount,
            topology: a.topology,
            materialIndex: a.materialIndex,
            boundsMin: a.bounds.min,
            boundsMax: a.bounds.max
        )
    })
}

private func convert(_ m: _Proto_DataUpdateType_v1) -> DDBridgeDataUpdateType {
    if m == .initial {
        return .initial
    }
    return .delta
}

private func webUpdateTextureRequestFromUpdateTextureRequest(_ request: _Proto_TextureDataUpdate_v1) -> DDBridgeUpdateTexture {
    // FIXME: remove placeholder code
    // swift-format-ignore: NeverForceUnwrap
    let descriptor = request.descriptor!
    let data = request.data
    return DDBridgeUpdateTexture(
        imageAsset: .init(descriptor, data: data),
        identifier: request.identifier,
        hashString: request.hashString
    )
}

private func webUpdateMeshRequestFromUpdateMeshRequest(
    _ request: _Proto_MeshDataUpdate_v1
) -> DDBridgeUpdateMesh {
    var descriptor: DDBridgeMeshDescriptor?
    if let requestDescriptor = request.descriptor {
        descriptor = .init(requestDescriptor)
    }

    return DDBridgeUpdateMesh(
        identifier: request.identifier,
        updateType: convert(request.updateType),
        descriptor: descriptor,
        parts: webPartsFromParts(request.parts),
        indexData: request.indexData,
        vertexData: request.vertexData,
        instanceTransforms: toData(request.instanceTransformsCompat()),
        instanceTransformsCount: request.instanceTransformsCompat().count,
        materialPrims: request.materialPrims,
        deformationData: .init(request.deformationData)
    )
}

nonisolated func webUpdateMaterialRequestFromUpdateMaterialRequest(
    _ request: _Proto_MaterialDataUpdate_v1
) -> DDBridgeUpdateMaterial {
    DDBridgeUpdateMaterial(materialGraph: request.materialSourceArchive, identifier: request.identifier)
}

final class USDModelLoader: _Proto_UsdStageSession_v1.Delegate {
    fileprivate let usdLoader: _Proto_UsdStageSession_v1
    private let objcLoader: DDBridgeModelLoader

    @nonobjc
    private let dispatchSerialQueue: DispatchSerialQueue

    @nonobjc
    fileprivate var time: TimeInterval = 0

    @nonobjc
    fileprivate var startTime: TimeInterval = 0
    @nonobjc
    fileprivate var endTime: TimeInterval = 1
    @nonobjc
    fileprivate var timeCodePerSecond: TimeInterval = 1

    init(objcInstance: DDBridgeModelLoader) {
        objcLoader = objcInstance
        usdLoader = .init()
        dispatchSerialQueue = DispatchSerialQueue(label: "USDModelWebProcess", qos: .userInteractive)
        usdLoader.delegate = self
    }

    func iblTextureUpdated(data: consuming sending _Proto_TextureDataUpdate_v1) {
        // FIXME: https://bugs.webkit.org/show_bug.cgi?id=299480 - [Model element] Support `environmentMap` attribute in GPU process model element
    }

    func iblTextureDestroyed(identifier: _Proto_ResourceId) {
        // FIXME: https://bugs.webkit.org/show_bug.cgi?id=303906
    }

    func meshUpdated(data: consuming sending _Proto_MeshDataUpdate_v1) {
        let identifier = data.identifier
        self.dispatchSerialQueue.async {
            self.objcLoader.updateMesh(webRequest: webUpdateMeshRequestFromUpdateMeshRequest(data))
        }
    }

    func meshDestroyed(identifier: String) {
        // FIXME: https://bugs.webkit.org/show_bug.cgi?id=303906
    }

    func materialUpdated(data: consuming sending _Proto_MaterialDataUpdate_v1) {
        let identifier = data.identifier
        self.dispatchSerialQueue.async {
            self.objcLoader.updateMaterial(webRequest: webUpdateMaterialRequestFromUpdateMaterialRequest(data))
        }
    }

    func materialDestroyed(identifier: String) {
        // FIXME: https://bugs.webkit.org/show_bug.cgi?id=303906
    }

    func textureUpdated(data: consuming sending _Proto_TextureDataUpdate_v1) {
        let identifier = data.identifier
        self.dispatchSerialQueue.async {
            self.objcLoader.updateTexture(webRequest: webUpdateTextureRequestFromUpdateTextureRequest(data))
        }
    }

    func textureDestroyed(identifier: String) {
        // FIXME: https://bugs.webkit.org/show_bug.cgi?id=303906
    }

    func loadModel(from url: Foundation.URL) {
        do {
            let stage = try UsdStage(contentsOf: url)
            self.timeCodePerSecond = stage.timeCodesPerSecond
            self.startTime = stage.startTimeCode
            self.endTime = stage.endTimeCode
            self.usdLoader.loadStage(stage)
        } catch {
            fatalError(error.localizedDescription)
        }
    }

    func loadModel(from data: Data) {
    }

    func update(deltaTime: TimeInterval) {
        usdLoader.update(time: time)

        time = fmod(deltaTime * self.timeCodePerSecond + time - startTime, max(endTime - startTime, 1)) + startTime
    }
}

@objc
@implementation
extension DDBridgeModelLoader {
    @nonobjc
    var loader: USDModelLoader?
    @nonobjc
    var modelUpdated: ((DDBridgeUpdateMesh) -> (Void))?
    @nonobjc
    var textureUpdatedCallback: ((DDBridgeUpdateTexture) -> (Void))?
    @nonobjc
    var materialUpdatedCallback: ((DDBridgeUpdateMaterial) -> (Void))?

    @nonobjc
    fileprivate var retainedRequests: Set<NSObject> = []

    override init() {
        super.init()

        self.loader = USDModelLoader(objcInstance: self)
    }

    @objc(
        setCallbacksWithModelUpdatedCallback:
        textureUpdatedCallback:
        materialUpdatedCallback:
    )
    func setCallbacksWithModelUpdatedCallback(
        _ modelUpdatedCallback: @escaping ((DDBridgeUpdateMesh) -> (Void)),
        textureUpdatedCallback: @escaping ((DDBridgeUpdateTexture) -> (Void)),
        materialUpdatedCallback: @escaping ((DDBridgeUpdateMaterial) -> (Void))
    ) {
        self.modelUpdated = modelUpdatedCallback
        self.textureUpdatedCallback = textureUpdatedCallback
        self.materialUpdatedCallback = materialUpdatedCallback
    }

    @objc
    func loadModel(from url: Foundation.URL) {
        self.loader?.loadModel(from: url)
    }

    @objc
    func update(_ deltaTime: Double) {
        self.loader?.update(deltaTime: deltaTime)
    }

    @objc
    func requestCompleted(_ request: NSObject) {
        retainedRequests.remove(request)
    }

    fileprivate func updateMesh(webRequest: DDBridgeUpdateMesh) {
        if let modelUpdated {
            retainedRequests.insert(webRequest)
            modelUpdated(webRequest)
        }
    }

    fileprivate func updateTexture(webRequest: DDBridgeUpdateTexture) {
        if let textureUpdatedCallback {
            retainedRequests.insert(webRequest)
            textureUpdatedCallback(webRequest)
        }
    }

    fileprivate func updateMaterial(webRequest: DDBridgeUpdateMaterial) {
        if let materialUpdatedCallback {
            retainedRequests.insert(webRequest)
            materialUpdatedCallback(webRequest)
        }
    }
}

extension DDBridgeSkinningData {
    fileprivate func makeDeformerDescription(device: MTLDevice) -> _Proto_LowLevelDeformerDescription_v1 {
        // swift-format-ignore: NeverForceUnwrap
        let jointTransformsBuffer = device.makeBuffer(
            bytes: self.jointTransforms,
            length: self.jointTransforms.count * MemoryLayout<simd_float4x4>.size,
            options: .storageModeShared
        )!

        // swift-format-ignore: NeverForceUnwrap
        let jointTransformsDescription = _Proto_LowLevelDeformationDescription_v1.Buffer.make(
            jointTransformsBuffer,
            offset: 0,
            occupiedLength: jointTransformsBuffer.length,
            elementType: .float4x4
        )!

        // swift-format-ignore: NeverForceUnwrap
        let inverseBindPosesBuffer = device.makeBuffer(
            bytes: self.inverseBindPoses,
            length: self.inverseBindPoses.count * MemoryLayout<simd_float4x4>.size,
            options: .storageModeShared
        )!

        // swift-format-ignore: NeverForceUnwrap
        let inverseBindPosesDescription = _Proto_LowLevelDeformationDescription_v1.Buffer.make(
            inverseBindPosesBuffer,
            offset: 0,
            occupiedLength: inverseBindPosesBuffer.length,
            elementType: .float4x4
        )!

        // swift-format-ignore: NeverForceUnwrap
        let jointIndicesBuffer = device.makeBuffer(
            bytes: self.influenceJointIndices,
            length: self.influenceJointIndices.count * MemoryLayout<UInt32>.size,
            options: .storageModeShared
        )!
        // swift-format-ignore: NeverForceUnwrap
        let jointIndicesDescription = _Proto_LowLevelDeformationDescription_v1.Buffer.make(
            jointIndicesBuffer,
            offset: 0,
            occupiedLength: jointIndicesBuffer.length,
            elementType: .uint
        )!

        // swift-format-ignore: NeverForceUnwrap
        let influenceWeightsBuffer = device.makeBuffer(
            bytes: self.influenceWeights,
            length: self.influenceWeights.count * MemoryLayout<Float>.size,
            options: .storageModeShared
        )!
        // swift-format-ignore: NeverForceUnwrap
        let influenceWeightsDescription = _Proto_LowLevelDeformationDescription_v1.Buffer.make(
            influenceWeightsBuffer,
            offset: 0,
            occupiedLength: influenceWeightsBuffer.length,
            elementType: .float
        )!

        let deformerDescription = _Proto_LowLevelSkinningDescription_v1(
            jointTransforms: jointTransformsDescription,
            inverseBindPoses: inverseBindPosesDescription,
            influenceJointIndices: jointIndicesDescription,
            influenceWeights: influenceWeightsDescription,
            geometryBindTransform: self.geometryBindTransform,
            influencePerVertexCount: self.influencePerVertexCount
        )

        return deformerDescription
    }
}

#else
@objc
@implementation
extension DDUSDConfiguration {
    init(device: MTLDevice) {
    }

    @objc(createMaterialCompiler:)
    func createMaterialCompiler() async {
    }
}

@objc
@implementation
extension DDBridgeReceiver {
    init(
        configuration: DDUSDConfiguration,
        diffuseAsset: DDBridgeImageAsset,
        specularAsset: DDBridgeImageAsset
    ) throws {
    }

    @objc(renderWithTexture:)
    func render(with texture: MTLTexture) {
    }

    @objc(updateTexture:)
    func updateTexture(_ data: DDBridgeUpdateTexture) {
    }

    @objc(updateMaterial:completionHandler:)
    func updateMaterial(_ data: DDBridgeUpdateMaterial) async {
    }

    @objc(updateMesh:completionHandler:)
    func updateMesh(_ data: DDBridgeUpdateMesh) async {
    }

    @objc(setTransform:)
    func setTransform(_ transform: simd_float4x4) {
    }

    @objc
    func setCameraDistance(_ distance: Float) {
    }

    @objc
    func setPlaying(_ play: Bool) {
    }
}

@objc
@implementation
extension DDBridgeModelLoader {
    override init() {
        super.init()
    }

    @objc(
        setCallbacksWithModelUpdatedCallback:
        textureUpdatedCallback:
        materialUpdatedCallback:
    )
    func setCallbacksWithModelUpdatedCallback(
        _ modelUpdatedCallback: @escaping ((DDBridgeUpdateMesh) -> (Void)),
        textureUpdatedCallback: @escaping ((DDBridgeUpdateTexture) -> (Void)),
        materialUpdatedCallback: @escaping ((DDBridgeUpdateMaterial) -> (Void))
    ) {
    }

    @objc
    func loadModel(from url: Foundation.URL) {
    }

    @objc
    func update(_ deltaTime: Double) {
    }

    @objc
    func requestCompleted(_ request: NSObject) {
    }
}
#endif
