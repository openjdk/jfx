/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package test.com.sun.prism.d3d;

import com.sun.prism.PixelFormat;
import com.sun.prism.Texture;
import com.sun.prism.d3d.D3DNativeShim;
import com.sun.prism.d3d.D3DNativeShim.TextureInfo;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import test.com.sun.javafx.test.ParityGate;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Binding tests for {@code com.sun.prism.d3d.D3DNative}, the FFM facade over the {@code d3d_*} C ABI of
 * {@code prism_d3d} ({@code src/main/native-prism-d3d/prism_d3d_api.h}): symbol resolution, the ABI
 * guard, the three struct layouts against the C compiler's {@code sizeof} and the measured offsets, the
 * enum values the two sides share (pinned here because the C no longer reads the generated JNI header),
 * the answers the C gives for NULL handles, and - when this session can create a Direct3D device - the
 * pipeline, context, texture and 3D object lifecycles.
 * <p>
 * The library has to load and bind on every Windows build ({@link D3DNatives}); only the cases that need
 * a device skip, and only when {@code d3d_pipeline_init} itself says why - or, under
 * {@code -Djfx.parity.require=true}, fail with that reason, because a GPU-less runner reporting the device
 * cases green is the defect {@link ParityGate} exists to catch. Unconditionally, at the end of the class, a
 * machine that reports at least one adapter must have run at least one device body.
 */
@EnabledOnOs(OS.WINDOWS)
public class D3DNativeTest {

    /** Every function {@code prism_d3d_api.h} exports, in header order. */
    static final List<String> EXPORTED_SYMBOLS = List.of(
            "d3d_abi_version", "d3d_sizeof_driver_info", "d3d_sizeof_frame_stats", "d3d_sizeof_texture_info",
            "d3d_pipeline_init", "d3d_pipeline_get_error_message", "d3d_pipeline_dispose",
            "d3d_pipeline_get_adapter_ordinal", "d3d_pipeline_get_adapter_count",
            "d3d_pipeline_get_driver_information", "d3d_pipeline_get_max_sample_support",
            "d3d_context_get", "d3d_context_test_cooperative_level", "d3d_context_reset_device",
            "d3d_context_get_max_texture_size", "d3d_texture_create", "d3d_swapchain_create",
            "d3d_resource_release", "d3d_resource_get_size", "d3d_resource_is_default_pool",
            "d3d_texture_update", "d3d_texture_read_pixels",
            "d3d_shader_create", "d3d_shader_enable", "d3d_shader_disable", "d3d_shader_set_constants_f",
            "d3d_swapchain_present", "d3d_context_get_frame_stats", "d3d_context_draw_indexed_quads",
            "d3d_context_clear", "d3d_context_set_blend_mode", "d3d_context_set_render_target",
            "d3d_context_set_texture", "d3d_context_set_camera_position", "d3d_context_set_proj_view_matrix",
            "d3d_context_set_transform", "d3d_context_reset_transform", "d3d_context_set_world_transform",
            "d3d_context_set_clip_rect", "d3d_context_reset_clip_rect", "d3d_context_set_device_parameters_2d",
            "d3d_context_set_device_parameters_3d", "d3d_context_blit",
            "d3d_mesh_create", "d3d_mesh_release", "d3d_mesh_build_geometry",
            "d3d_material_create", "d3d_material_release", "d3d_material_set_diffuse_color",
            "d3d_material_set_specular_color", "d3d_material_set_map",
            "d3d_meshview_create", "d3d_meshview_release", "d3d_meshview_set_culling_mode",
            "d3d_meshview_set_material", "d3d_meshview_set_wireframe", "d3d_meshview_set_ambient_light",
            "d3d_meshview_set_light", "d3d_meshview_render");

    /** {@code D3dDriverInfo} field offsets as the MSVC x64 compiler lays the struct out. */
    static final String[] DRIVER_INFO_FIELDS = {
        "device_description", "device_name", "driver_name", "warning_message",
        "vendor_id", "device_id", "subsys_id", "product", "version", "sub_version", "build_id",
        "ps_version_major", "ps_version_minor", "max_samples", "os_major", "os_minor", "os_build",
    };
    static final long[] DRIVER_INFO_OFFSETS = {
        0, 512, 544, 1056,
        1312, 1316, 1320, 1324, 1328, 1332, 1336,
        1340, 1344, 1348, 1352, 1356, 1360,
    };

    static final String[] FRAME_STATS_FIELDS = {
        "num_triangles_drawn", "num_draw_calls", "num_buffer_locks", "num_texture_locks",
        "num_texture_transfer_bytes", "num_set_texture", "num_set_pixel_shader", "num_render_target_switch",
    };

    /** The {@code PrismSettings} defaults {@code D3DPipeline} passes on a plain run. */
    private static final int PRODUCTION_FLAGS = D3DNativeShim.initFlags(false, false, false);

    private static final int INT_ARGB_PRE = PixelFormat.INT_ARGB_PRE.ordinal();

    private static final ParityGate.Ledger LEDGER = ParityGate.ledger(D3DNativeTest.class);

    /** How many cases asked {@link #requireDevice()} for a device, whether or not they got one. */
    private static final AtomicInteger DEVICE_REQUESTS = new AtomicInteger();

    /** What {@code d3d_pipeline_init} said the last time it refused, for the end-of-class report. */
    private static volatile String lastRefusal = "(it never refused)";

    @FunctionalInterface
    private interface DeviceCase {
        void run(MemorySegment ctx) throws Exception;
    }

    @BeforeAll
    static void requireNatives() {
        D3DNatives.require();
    }

    /**
     * The unconditional half of the rule. {@code d3d_pipeline_get_adapter_count} answers 0 without a live
     * pipeline, so the machine is asked once more here, the way {@link #pipelineInitSucceedsOrExplainsWhyNot}
     * asks it: if it reports an adapter now and some case asked for a device, then a device body must have
     * run. Nothing is probed when no case asked, so a run filtered to the binding tests, or a class that
     * {@link D3DNatives} skipped before any test, says nothing here.
     */
    @AfterAll
    static void theOracleRan() {
        LEDGER.assertOracleRan();
        int requested = DEVICE_REQUESTS.get();
        if (requested == 0) {
            return;
        }
        boolean up = D3DNativeShim.pipelineInit(PRODUCTION_FLAGS);
        int adapters = up ? D3DNativeShim.pipelineGetAdapterCount() : 0;
        D3DNativeShim.pipelineDispose();
        assertTrue(adapters == 0 || LEDGER.comparisons() > 0, () -> "this machine reports " + adapters
                + " Direct3D adapter(s) at the end of the class and " + requested + " case(s) asked for a"
                + " device, yet no device body ran: the device cases skipped on a machine that has the"
                + " device. d3d_pipeline_init refused " + LEDGER.timesUnavailable() + " time(s), last with: "
                + lastRefusal);
    }

    /* ---------------------------------------------------------------------------------------------
     * Binding
     * ------------------------------------------------------------------------------------------- */

    @Test
    public void facadeBindsEveryExportedSymbolAndNothingElse() {
        List<String> bound = D3DNativeShim.boundSymbols();
        assertEquals(EXPORTED_SYMBOLS.size(), bound.size(), "bound symbols: " + bound);
        for (String name : EXPORTED_SYMBOLS) {
            assertTrue(bound.contains(name), "facade does not bind " + name);
        }
        assertEquals(List.of(), D3DNativeShim.missingSymbols(), "symbols the library does not export");
    }

    @Test
    public void everyExportedSymbolResolvesInTheLoadedLibrary() {
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        for (String name : EXPORTED_SYMBOLS) {
            assertTrue(lookup.find(name).isPresent(), "prism_d3d does not export " + name);
        }
    }

    @Test
    public void abiVersionIsTheOneTheFacadeWasWrittenFor() {
        assertEquals(1, D3DNativeShim.expectedAbiVersion());
        assertEquals(D3DNativeShim.expectedAbiVersion(), D3DNativeShim.abiVersion());
    }

    /* ---------------------------------------------------------------------------------------------
     * Layouts
     * ------------------------------------------------------------------------------------------- */

    @Test
    public void driverInfoLayoutMatchesTheCStruct() {
        assertEquals(1364, D3DNativeShim.sizeofDriverInfo(), "sizeof(D3dDriverInfo)");
        assertEquals(D3DNativeShim.sizeofDriverInfo(), D3DNativeShim.driverInfoLayoutByteSize());
        for (int i = 0; i < DRIVER_INFO_FIELDS.length; i++) {
            assertEquals(DRIVER_INFO_OFFSETS[i], D3DNativeShim.driverInfoLayoutOffset(DRIVER_INFO_FIELDS[i]),
                    "offset of " + DRIVER_INFO_FIELDS[i]);
        }
    }

    @Test
    public void frameStatsLayoutMatchesTheCStruct() {
        assertEquals(32, D3DNativeShim.sizeofFrameStats(), "sizeof(D3dFrameStats)");
        assertEquals(D3DNativeShim.sizeofFrameStats(), D3DNativeShim.frameStatsLayoutByteSize());
        for (int i = 0; i < FRAME_STATS_FIELDS.length; i++) {
            assertEquals(4L * i, D3DNativeShim.frameStatsLayoutOffset(FRAME_STATS_FIELDS[i]),
                    "offset of " + FRAME_STATS_FIELDS[i]);
        }
    }

    @Test
    public void textureInfoLayoutMatchesTheCStruct() {
        assertEquals(24, D3DNativeShim.sizeofTextureInfo(), "sizeof(D3dTextureInfo)");
        assertEquals(D3DNativeShim.sizeofTextureInfo(), D3DNativeShim.textureInfoLayoutByteSize());
        assertEquals(0, D3DNativeShim.textureInfoLayoutOffset("handle"));
        assertEquals(8, D3DNativeShim.textureInfoLayoutOffset("width"));
        assertEquals(12, D3DNativeShim.textureInfoLayoutOffset("height"));
        assertEquals(16, D3DNativeShim.textureInfoLayoutOffset("is_default_pool"));
        assertEquals(20, D3DNativeShim.textureInfoLayoutOffset("reserved"));
    }

    @Test
    public void pointerArgumentsHaveTheSizesTheCReads() {
        assertEquals(16 * 8, D3DNativeShim.matrixLayoutByteSize(), "const double* m16");
        assertEquals(18 * 4, D3DNativeShim.lightLayoutByteSize(), "const float* params18");
    }

    /* ---------------------------------------------------------------------------------------------
     * Constants the C side maps onto D3D9 values - pinned here, not read from a generated header
     * ------------------------------------------------------------------------------------------- */

    @Test
    public void compositeModesMatchTheHeaderEnum() {
        assertArrayEquals(new int[] {0, 1, 2, 3, 4}, D3DNativeShim.contextCompositeModes(), "D3dCompMode");
    }

    @Test
    public void cullModesMatchTheHeaderEnum() {
        assertArrayEquals(new int[] {110, 111, 112}, D3DNativeShim.contextCullModes(), "D3dCullMode");
    }

    @Test
    public void pixelFormatOrdinalsMatchTheHeaderEnum() {
        assertEquals(8, PixelFormat.values().length, "D3dPixelFormat has an entry per PixelFormat");
        assertEquals(0, PixelFormat.INT_ARGB_PRE.ordinal());
        assertEquals(1, PixelFormat.BYTE_BGRA_PRE.ordinal());
        assertEquals(2, PixelFormat.BYTE_RGB.ordinal());
        assertEquals(3, PixelFormat.BYTE_GRAY.ordinal());
        assertEquals(4, PixelFormat.BYTE_ALPHA.ordinal());
        assertEquals(5, PixelFormat.MULTI_YCbCr_420.ordinal());
        assertEquals(6, PixelFormat.BYTE_APPLE_422.ordinal());
        assertEquals(7, PixelFormat.FLOAT_XYZW.ordinal());
        assertEquals(PixelFormat.INT_ARGB_PRE.ordinal(), D3DNativeShim.pixelFormatIntArgbPre());
        assertEquals(PixelFormat.FLOAT_XYZW.ordinal(), D3DNativeShim.pixelFormatFloatXyzw());
    }

    @Test
    public void initFlagsComposeTheHeaderBits() {
        assertArrayEquals(new int[] {1, 2, 4}, D3DNativeShim.initFlagBits(), "D3dInitFlags");
        assertEquals(0, D3DNativeShim.initFlags(false, false, false));
        assertEquals(1, D3DNativeShim.initFlags(true, false, false), "PrismSettings.forceGPU");
        assertEquals(2, D3DNativeShim.initFlags(false, true, false), "PrismSettings.isVsyncEnabled");
        assertEquals(4, D3DNativeShim.initFlags(false, false, true), "PrismSettings.verbose");
        assertEquals(7, D3DNativeShim.initFlags(true, true, true));
    }

    @Test
    public void indexTypesMatchTheHeaderEnum() {
        assertArrayEquals(new int[] {16, 32}, D3DNativeShim.indexTypes(), "D3dIndexType");
    }

    @Test
    public void hresultVocabularyIsUnchanged() {
        assertEquals(0, D3DNativeShim.d3dOk());
        assertEquals(0x80004005, D3DNativeShim.eFail());
        assertTrue(D3DNativeShim.failed(D3DNativeShim.eFail()));
        assertFalse(D3DNativeShim.failed(D3DNativeShim.d3dOk()));
        assertFalse(D3DNativeShim.failed(1), "S_FALSE is a success");
    }

    /* ---------------------------------------------------------------------------------------------
     * Answers for NULL handles - no device needed; what the JNI entry points answered for 0L
     * ------------------------------------------------------------------------------------------- */

    @Test
    public void nullResourceHasNoSize() {
        assertArrayEquals(new int[] {-1, 0, 0}, D3DNativeShim.resourceGetSize(MemorySegment.NULL),
                "d3d_resource_get_size(NULL) is -1 and writes nothing, as nGetTextureWidth/Height(0) were -1");
    }

    @Test
    public void releasingANullResourceIsOk() {
        assertEquals(D3DNativeShim.d3dOk(), D3DNativeShim.resourceRelease(MemorySegment.NULL, MemorySegment.NULL));
    }

    @Test
    public void nullContextAnswersAsTheJniDid() {
        assertEquals(D3DNativeShim.eFail(), D3DNativeShim.contextTestCooperativeLevel(MemorySegment.NULL));
        assertEquals(-1, D3DNativeShim.contextGetMaxTextureSize(MemorySegment.NULL));
        assertNull(D3DNativeShim.contextGetFrameStats(MemorySegment.NULL, false));
        assertFalse(D3DNativeShim.resourceIsDefaultPool(MemorySegment.NULL));
    }

    @Test
    public void renderingANullMeshViewIsANoOp() {
        D3DNativeShim.meshviewRender(MemorySegment.NULL);
    }

    @Test
    public void adapterCountIsZeroWithoutAPipeline() {
        D3DNativeShim.pipelineDispose(); // a no-op unless an earlier case leaked a pipeline
        assertEquals(0, D3DNativeShim.pipelineGetAdapterCount());
    }

    @Test
    public void driverInformationOfAnAbsentAdapterIsNull() {
        assertNull(D3DNativeShim.pipelineGetDriverStrings(Integer.MAX_VALUE));
        assertNull(D3DNativeShim.pipelineGetDriverInts(Integer.MAX_VALUE));
    }

    /* ---------------------------------------------------------------------------------------------
     * Pipeline - the one case that never skips: init succeeds, or says why it did not
     * ------------------------------------------------------------------------------------------- */

    @Test
    public void pipelineInitSucceedsOrExplainsWhyNot() {
        boolean up = D3DNativeShim.pipelineInit(PRODUCTION_FLAGS);
        try {
            if (up) {
                assertTrue(D3DNativeShim.pipelineGetAdapterCount() >= 1, "adapter count of a live pipeline");
                assertTrue(D3DNativeShim.pipelineGetMaxSampleSupport(0) >= 0);
            } else {
                String message = D3DNativeShim.pipelineGetErrorMessage();
                assertNotNull(message, "a failed d3d_pipeline_init leaves an error message");
                assertFalse(message.isEmpty(), "a failed d3d_pipeline_init leaves a non-empty error message");
            }
        } finally {
            D3DNativeShim.pipelineDispose();
        }
    }

    @Test
    public void pipelineCanBeInitializedAgainAfterDispose() {
        requireDevice();
        LEDGER.compared();
        D3DNativeShim.pipelineDispose();
        assertTrue(D3DNativeShim.pipelineInit(PRODUCTION_FLAGS),
                "d3d_pipeline_init after d3d_pipeline_dispose - the D3DPipeline.reinitialize contract");
        try {
            assertNotEquals(0L, D3DNativeShim.contextGet(0).address(), "context of the re-created pipeline");
        } finally {
            D3DNativeShim.pipelineDispose();
        }
    }

    @Test
    public void secondInitWithoutDisposeIsRefusedWithAMessage() throws Exception {
        withDevice(ctx -> {
            assertFalse(D3DNativeShim.pipelineInit(PRODUCTION_FLAGS), "init while a pipeline exists");
            String message = D3DNativeShim.pipelineGetErrorMessage();
            assertNotNull(message);
            assertFalse(message.isEmpty());
        });
    }

    @Test
    public void liveAdapterHasDriverInformation() throws Exception {
        withDevice(ctx -> {
            String[] strings = D3DNativeShim.pipelineGetDriverStrings(0);
            assertNotNull(strings, "driver information of adapter 0");
            assertNotNull(strings[0], "device description");
            assertFalse(strings[0].isEmpty(), "device description");
            int[] ints = D3DNativeShim.pipelineGetDriverInts(0);
            assertNotNull(ints);
            assertEquals(13, ints.length);
            assertTrue(ints[10] > 0, "os_major from GetVersionEx");
        });
    }

    /* ---------------------------------------------------------------------------------------------
     * Context and resources - skip when the session cannot create a device
     * ------------------------------------------------------------------------------------------- */

    @Test
    public void contextReportsADeviceAndItsLimits() throws Exception {
        withDevice(ctx -> {
            assertFalse(D3DNativeShim.failed(D3DNativeShim.contextTestCooperativeLevel(ctx)),
                    "CheckDeviceState of a fresh device");
            assertTrue(D3DNativeShim.contextGetMaxTextureSize(ctx) > 0, "max texture size");
        });
    }

    @Test
    public void textureCreateFillsTheInfoStructAndTheResourceAgrees() throws Exception {
        withDevice(ctx -> {
            int max = D3DNativeShim.contextGetMaxTextureSize(ctx);
            TextureInfo info = D3DNativeShim.textureCreate(ctx, INT_ARGB_PRE, Texture.Usage.DEFAULT.ordinal(),
                    false, 64, 48, 0, false);
            assertNotEquals(0L, info.handle().address(), "handle of a 64x48 INT_ARGB_PRE texture");
            assertFalse(D3DNativeShim.failed(info.hresult()), "HRESULT of a create that produced a handle");
            assertTrue(info.width() >= 64 && info.width() <= max, "width " + info.width());
            assertTrue(info.height() >= 48 && info.height() <= max, "height " + info.height());
            assertArrayEquals(new int[] {0, info.width(), info.height()},
                    D3DNativeShim.resourceGetSize(info.handle()), "d3d_resource_get_size agrees with the struct");
            assertEquals(info.isDefaultPool(), D3DNativeShim.resourceIsDefaultPool(info.handle()),
                    "d3d_resource_is_default_pool agrees with the struct");
            assertEquals(D3DNativeShim.d3dOk(), D3DNativeShim.resourceRelease(ctx, info.handle()));
        });
    }

    @Test
    public void renderTargetCreateFillsTheInfoStruct() throws Exception {
        withDevice(ctx -> {
            TextureInfo rtt = D3DNativeShim.textureCreate(ctx, INT_ARGB_PRE, Texture.Usage.DEFAULT.ordinal(),
                    true, 32, 32, 0, false);
            assertNotEquals(0L, rtt.handle().address(), "handle of a 32x32 render target");
            assertTrue(rtt.width() >= 32 && rtt.height() >= 32, rtt.width() + "x" + rtt.height());
            assertEquals(D3DNativeShim.d3dOk(), D3DNativeShim.resourceRelease(ctx, rtt.handle()));
        });
    }

    @Test
    public void frameStatsAreReportedAsTheLibraryWasCompiled() throws Exception {
        withDevice(ctx -> {
            int[] stats = D3DNativeShim.contextGetFrameStats(ctx, false);
            // PERF_COUNTERS compiled out: null, as nGetFrameStats answered false. D3DContext.h defines it
            // unless NO_PERF_COUNTERS, so null is a build that switched it off - a skip that names itself,
            // never a silent pass over eight unchecked counters.
            LEDGER.requireOracle(stats != null, () -> "d3d_context_get_frame_stats(ctx, false) returned NULL:"
                    + " prism_d3d was built without PERF_COUNTERS, so the frame statistics cannot be checked"
                    + " in this build.");
            assertEquals(8, stats.length);
            for (int i = 0; i < stats.length; i++) {
                assertTrue(stats[i] >= 0, FRAME_STATS_FIELDS[i] + " = " + stats[i]);
            }
            assertNotNull(D3DNativeShim.contextGetFrameStats(ctx, true), "reset variant");
        });
    }

    @Test
    public void threeDObjectsCreateAndReleaseInPairs() throws Exception {
        withDevice(ctx -> {
            for (int i = 0; i < 8; i++) {
                MemorySegment mesh = D3DNativeShim.meshCreate(ctx);
                assertNotEquals(0L, mesh.address(), "D3DMesh*");
                MemorySegment material = D3DNativeShim.materialCreate(ctx);
                assertNotEquals(0L, material.address(), "D3DPhongMaterial*");
                MemorySegment meshView = D3DNativeShim.meshviewCreate(ctx, mesh);
                assertNotEquals(0L, meshView.address(), "D3DMeshView*");
                D3DNativeShim.meshviewSetMaterial(meshView, material);
                D3DNativeShim.meshviewSetCullingMode(meshView, D3DNativeShim.contextCullModes()[2]);
                D3DNativeShim.meshviewRelease(meshView);
                D3DNativeShim.materialRelease(material);
                D3DNativeShim.meshRelease(mesh);
            }
        });
    }

    /* ---------------------------------------------------------------------------------------------
     * Helpers
     * ------------------------------------------------------------------------------------------- */

    /**
     * Brings the pipeline up with the production flags, or aborts the calling case with the reason
     * {@code d3d_pipeline_init} gave. A library that is present but cannot create a device in this
     * session (no interactive desktop, a rejected driver) is an environment fact; a library that
     * cannot load is not, and {@link D3DNatives} has already failed the class for that. Under
     * {@code -Djfx.parity.require=true} the abort is a failure that names the refusal ({@link ParityGate}).
     */
    private static void requireDevice() {
        DEVICE_REQUESTS.incrementAndGet();
        boolean up = D3DNativeShim.pipelineInit(PRODUCTION_FLAGS);
        String reason = up ? null : D3DNativeShim.pipelineGetErrorMessage();
        if (!up) {
            D3DNativeShim.pipelineDispose();
            lastRefusal = reason;
        }
        LEDGER.requireOracle(up, () -> "no Direct3D device in this session: d3d_pipeline_init("
                + PRODUCTION_FLAGS + ") refused with: " + reason);
    }

    private static void withDevice(DeviceCase body) throws Exception {
        requireDevice();
        try {
            MemorySegment ctx = D3DNativeShim.contextGet(0);
            assertNotEquals(0L, ctx.address(), "d3d_context_get(0) with a live pipeline");
            LEDGER.compared();
            body.run(ctx);
        } finally {
            D3DNativeShim.pipelineDispose();
        }
    }
}
