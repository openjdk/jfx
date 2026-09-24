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

package test.com.sun.prism.es2;

import com.sun.prism.es2.ES2NativeShim;
import java.lang.foreign.SymbolLookup;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Binding tests for {@code com.sun.prism.es2.ES2Native}, the FFM facade over the {@code es2_*} C ABI of
 * {@code prism_es2} ({@code src/main/native-prism-es2/prism_es2_api.h}): symbol resolution, the ABI guard
 * and the one struct layout against the C compiler's {@code sizeof}.
 * <p>
 * The library has to load and bind on every build that included ES2 ({@link ES2Natives}); a build that
 * left ES2 out - {@code INCLUDE_ES2} defaults off on Windows - skips the whole class instead, because a
 * missing optional pipeline is not a broken one.
 */
public class ES2NativeTest {

    /** Every function {@code prism_es2_api.h} exports, in header order. */
    static final List<String> EXPORTED_SYMBOLS = List.of(
            "es2_abi_version", "es2_sizeof_pixel_format_attrs", "es2_gl_enum_count", "es2_gl_enum",
            "es2_factory_init", "es2_factory_get_x11_info", "es2_context_get_string",
            "es2_pixel_format_create", "es2_pixel_format_release",
            "es2_drawable_create", "es2_drawable_create_dummy", "es2_drawable_release",
            "es2_drawable_swap_buffers",
            "es2_context_create", "es2_context_release", "es2_context_get_native_handle",
            "es2_context_make_current", "es2_context_get_proc_address", "es2_context_adopt",
            "es2_active_texture", "es2_bind_fbo", "es2_bind_texture", "es2_blend_func", "es2_clear_buffers",
            "es2_scissor_test", "es2_set_depth_test", "es2_set_msaa", "es2_update_viewport",
            "es2_tex_params_min_max", "es2_update_filter_state", "es2_update_wrap_state",
            "es2_pixel_storei", "es2_use_program", "es2_enable_vertex_attributes",
            "es2_disable_vertex_attributes", "es2_set_index_buffer", "es2_set_device_parameters_2d",
            "es2_set_device_parameters_3d", "es2_finish",
            "es2_get_int_param", "es2_get_max_sample_size", "es2_get_fbo", "es2_gen_and_bind_texture",
            "es2_uniform1f", "es2_uniform2f", "es2_uniform3f", "es2_uniform4f",
            "es2_uniform1i", "es2_uniform2i", "es2_uniform3i", "es2_uniform4i",
            "es2_uniform4fv", "es2_uniform4iv", "es2_uniform_matrix4fv",
            "es2_texture_create", "es2_texture_delete", "es2_fbo_create", "es2_fbo_delete",
            "es2_depth_buffer_create", "es2_render_buffer_create", "es2_render_buffer_delete", "es2_blit",
            "es2_index_buffer16_create", "es2_tex_image_2d", "es2_tex_sub_image_2d", "es2_read_pixels",
            "es2_shader_compile", "es2_program_create", "es2_shader_delete", "es2_shaders_dispose",
            "es2_get_uniform_location", "es2_draw_indexed_quads",
            "es2_mesh_create", "es2_mesh_release", "es2_mesh_build_geometry_short",
            "es2_mesh_build_geometry_int", "es2_mesh_render");

    @BeforeAll
    static void requireNatives() {
        ES2Natives.require();
    }

    @Test
    public void facadeBindsEveryExportedSymbolAndNothingElse() {
        assertEquals(77, EXPORTED_SYMBOLS.size(), "the header exports 77 es2_* functions");
        List<String> bound = ES2NativeShim.boundSymbols();
        assertEquals(EXPORTED_SYMBOLS.size(), bound.size(), "bound symbols: " + bound);
        for (String name : EXPORTED_SYMBOLS) {
            assertTrue(bound.contains(name), "facade does not bind " + name);
        }
        assertEquals(List.of(), ES2NativeShim.missingSymbols(), "symbols the library does not export");
    }

    @Test
    public void everyExportedSymbolResolvesInTheLoadedLibrary() {
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        for (String name : EXPORTED_SYMBOLS) {
            assertTrue(lookup.find(name).isPresent(), "prism_es2 does not export " + name);
        }
    }

    @Test
    public void abiVersionIsTheOneTheFacadeWasWrittenFor() {
        assertEquals(3, ES2NativeShim.expectedAbiVersion());
        assertEquals(ES2NativeShim.expectedAbiVersion(), ES2NativeShim.abiVersion());
    }

    @Test
    public void pixelFormatAttrsLayoutMatchesTheCStruct() {
        assertEquals(28, ES2NativeShim.sizeofPixelFormatAttrs(), "sizeof(Es2PixelFormatAttrs)");
        assertEquals(ES2NativeShim.sizeofPixelFormatAttrs(), ES2NativeShim.pixelFormatAttrsLayoutByteSize());
    }
}
