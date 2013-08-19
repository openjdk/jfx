/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 
#include <stdio.h>
#include <string.h>

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <EGL/egl.h>

const char *
eglEnum2str(EGLenum val)
{
    switch (val) {
    case EGL_SUCCESS:           return "EGL_SUCCESS";
    case EGL_NOT_INITIALIZED:   return "EGL_NOT_INITIALIZED";
    case EGL_BAD_ACCESS :       return "EGL_BAD_ACCESS";
    case EGL_BAD_ALLOC  :       return "EGL_BAD_ALLOC";
    case EGL_BAD_ATTRIBUTE:     return "EGL_BAD_ATTRIBUTE";
    case EGL_BAD_CONFIG :       return "EGL_BAD_CONFIG";
    case EGL_BAD_CONTEXT:       return "EGL_BAD_CONTEXT";
    case EGL_BAD_CURRENT_SURFACE: return "EGL_BAD_CURRENT_SURFACE";
    case EGL_BAD_DISPLAY:       return "EGL_BAD_DISPLAY";
    case EGL_BAD_MATCH:         return "EGL_BAD_MATCH";
    case EGL_BAD_NATIVE_PIXMAP: return "EGL_BAD_NATIVE_PIXMAP";
    case EGL_BAD_NATIVE_WINDOW: return "EGL_BAD_NATIVE_WINDOW";
    case EGL_BAD_PARAMETER:     return "EGL_BAD_PARAMETER";
    case EGL_BAD_SURFACE:       return "EGL_BAD_SURFACE";
    case EGL_CONTEXT_LOST:      return "EGL_CONTEXT_LOST";
    case EGL_BUFFER_SIZE:       return "EGL_BUFFER_SIZE";
    case EGL_ALPHA_SIZE :       return "EGL_ALPHA_SIZE";
    case EGL_BLUE_SIZE  :       return "EGL_BLUE_SIZE";
    case EGL_GREEN_SIZE :       return "EGL_GREEN_SIZE";
    case EGL_RED_SIZE   :       return "EGL_RED_SIZE";
    case EGL_DEPTH_SIZE :       return "EGL_DEPTH_SIZE";
    case EGL_STENCIL_SIZE:      return "EGL_STENCIL_SIZE";
    case EGL_CONFIG_CAVEAT:     return "EGL_CONFIG_CAVEAT";
    case EGL_CONFIG_ID  :       return "EGL_CONFIG_ID";
    case EGL_LEVEL      :       return "EGL_LEVEL";
    case EGL_MAX_PBUFFER_HEIGHT: return "EGL_MAX_PBUFFER_HEIGHT";
    case EGL_MAX_PBUFFER_PIXELS: return "EGL_MAX_PBUFFER_PIXELS";
    case EGL_MAX_PBUFFER_WIDTH: return "EGL_MAX_PBUFFER_WIDTH";
    case EGL_NATIVE_RENDERABLE: return "EGL_NATIVE_RENDERABLE";
    case EGL_NATIVE_VISUAL_ID:  return "EGL_NATIVE_VISUAL_ID";
    case EGL_NATIVE_VISUAL_TYPE: return "EGL_NATIVE_VISUAL_TYPE";
    case EGL_SAMPLES    :       return "EGL_SAMPLES";
    case EGL_SAMPLE_BUFFERS:    return "EGL_SAMPLE_BUFFERS";
    case EGL_SURFACE_TYPE:      return "EGL_SURFACE_TYPE";
    case EGL_TRANSPARENT_TYPE:  return "EGL_TRANSPARENT_TYPE";
    case EGL_TRANSPARENT_BLUE_VALUE: return "EGL_TRANSPARENT_BLUE_VALUE";
    case EGL_TRANSPARENT_GREEN_VALUE: return "EGL_TRANSPARENT_GREEN_VALUE";
    case EGL_TRANSPARENT_RED_VALUE: return "EGL_TRANSPARENT_RED_VALUE";
    case EGL_NONE       :       return "EGL_NONE";
    case EGL_BIND_TO_TEXTURE_RGB: return "EGL_BIND_TO_TEXTURE_RGB";
    case EGL_BIND_TO_TEXTURE_RGBA: return "EGL_BIND_TO_TEXTURE_RGBA";
    case EGL_MIN_SWAP_INTERVAL: return "EGL_MIN_SWAP_INTERVAL";
    case EGL_MAX_SWAP_INTERVAL: return "EGL_MAX_SWAP_INTERVAL";
    case EGL_LUMINANCE_SIZE:    return "EGL_LUMINANCE_SIZE";
    case EGL_ALPHA_MASK_SIZE:   return "EGL_ALPHA_MASK_SIZE";
    case EGL_COLOR_BUFFER_TYPE: return "EGL_COLOR_BUFFER_TYPE";
    case EGL_RENDERABLE_TYPE:   return "EGL_RENDERABLE_TYPE";
    case EGL_MATCH_NATIVE_PIXMAP: return "EGL_MATCH_NATIVE_PIXMAP";
    case EGL_CONFORMANT :       return "EGL_CONFORMANT";
    case EGL_SLOW_CONFIG:       return "EGL_SLOW_CONFIG";
    case EGL_NON_CONFORMANT_CONFIG: return "EGL_NON_CONFORMANT_CONFIG";
    case EGL_TRANSPARENT_RGB:   return "EGL_TRANSPARENT_RGB";
    case EGL_RGB_BUFFER :       return "EGL_RGB_BUFFER";
    case EGL_LUMINANCE_BUFFER:  return "EGL_LUMINANCE_BUFFER";
    case EGL_NO_TEXTURE :       return "EGL_NO_TEXTURE";
    case EGL_TEXTURE_RGB:       return "EGL_TEXTURE_RGB";
    case EGL_TEXTURE_RGBA:      return "EGL_TEXTURE_RGBA";
    case EGL_TEXTURE_2D :       return "EGL_TEXTURE_2D";
    case EGL_VENDOR     :       return "EGL_VENDOR";
    case EGL_VERSION    :       return "EGL_VERSION";
    case EGL_EXTENSIONS :       return "EGL_EXTENSIONS";
    case EGL_CLIENT_APIS:       return "EGL_CLIENT_APIS";
    case EGL_HEIGHT     :       return "EGL_HEIGHT";
    case EGL_WIDTH      :       return "EGL_WIDTH";
    case EGL_LARGEST_PBUFFER:   return "EGL_LARGEST_PBUFFER";
    case EGL_TEXTURE_FORMAT:    return "EGL_TEXTURE_FORMAT";
    case EGL_TEXTURE_TARGET:    return "EGL_TEXTURE_TARGET";
    case EGL_MIPMAP_TEXTURE:    return "EGL_MIPMAP_TEXTURE";
    case EGL_MIPMAP_LEVEL:      return "EGL_MIPMAP_LEVEL";
    case EGL_RENDER_BUFFER:     return "EGL_RENDER_BUFFER";
    case EGL_VG_COLORSPACE:     return "EGL_VG_COLORSPACE";
    case EGL_VG_ALPHA_FORMAT:   return "EGL_VG_ALPHA_FORMAT";
    case EGL_HORIZONTAL_RESOLUTION: return "EGL_HORIZONTAL_RESOLUTION";
    case EGL_VERTICAL_RESOLUTION: return "EGL_VERTICAL_RESOLUTION";
    case EGL_PIXEL_ASPECT_RATIO: return "EGL_PIXEL_ASPECT_RATIO";
    case EGL_SWAP_BEHAVIOR:     return "EGL_SWAP_BEHAVIOR";
    case EGL_MULTISAMPLE_RESOLVE: return "EGL_MULTISAMPLE_RESOLVE";
    case EGL_BACK_BUFFER:       return "EGL_BACK_BUFFER";
    case EGL_SINGLE_BUFFER:     return "EGL_SINGLE_BUFFER";
    case EGL_VG_COLORSPACE_sRGB: return "EGL_VG_COLORSPACE_sRGB";
    case EGL_VG_COLORSPACE_LINEAR: return "EGL_VG_COLORSPACE_LINEAR";
    case EGL_VG_ALPHA_FORMAT_NONPRE: return "EGL_VG_ALPHA_FORMAT_NONPRE";
    case EGL_VG_ALPHA_FORMAT_PRE: return "EGL_VG_ALPHA_FORMAT_PRE";
    case EGL_BUFFER_PRESERVED:  return "EGL_BUFFER_PRESERVED";
    case EGL_BUFFER_DESTROYED:  return "EGL_BUFFER_DESTROYED";
    case EGL_OPENVG_IMAGE:      return "EGL_OPENVG_IMAGE";
    case EGL_CONTEXT_CLIENT_TYPE: return "EGL_CONTEXT_CLIENT_TYPE";
    case EGL_CONTEXT_CLIENT_VERSION: return "EGL_CONTEXT_CLIENT_VERSION";
    case EGL_MULTISAMPLE_RESOLVE_DEFAULT: return "EGL_MULTISAMPLE_RESOLVE_DEFAULT";
    case EGL_MULTISAMPLE_RESOLVE_BOX: return "EGL_MULTISAMPLE_RESOLVE_BOX";
    case EGL_OPENGL_ES_API:     return "EGL_OPENGL_ES_API";
    case EGL_OPENVG_API :       return "EGL_OPENVG_API";
    case EGL_OPENGL_API :       return "EGL_OPENGL_API";
    case EGL_DRAW       :       return "EGL_DRAW";
    case EGL_READ       :       return "EGL_READ";
    case EGL_CORE_NATIVE_ENGINE: return "EGL_CORE_NATIVE_ENGINE";
    }

    static char buf[1024];
    static char *ptr = buf;

    if (ptr + 16 > buf + sizeof(buf)) ptr = buf;
    char *res = ptr;
    snprintf(ptr, 16, "0x%x", val);
    ptr += strlen(ptr) + 1;
    return res;
}

const char *
glEnum2str(GLenum val)
{
    switch (val) {
    case GL_SRC_COLOR:          return "GL_SRC_COLOR";
    case GL_ONE_MINUS_SRC_COLOR:return "GL_ONE_MINUS_SRC_COLOR";
    case GL_SRC_ALPHA:          return "GL_SRC_ALPHA";
    case GL_ONE_MINUS_SRC_ALPHA:return "GL_ONE_MINUS_SRC_ALPHA";
    case GL_DST_ALPHA:          return "GL_DST_ALPHA";
    case GL_ONE_MINUS_DST_ALPHA:return "GL_ONE_MINUS_DST_ALPHA";

/* BlendingFactorSrc */
/*      GL_ZERO */
/*      GL_ONE */
    case GL_DST_COLOR:          return "GL_DST_COLOR";
    case GL_ONE_MINUS_DST_COLOR:return "GL_ONE_MINUS_DST_COLOR";
    case GL_SRC_ALPHA_SATURATE: return "GL_SRC_ALPHA_SATURATE";
/*      GL_SRC_ALPHA */
/*      GL_ONE_MINUS_SRC_ALPHA */
/*      GL_DST_ALPHA */
/*      GL_ONE_MINUS_DST_ALPHA */

/* BlendEquationSeparate */
    case GL_FUNC_ADD:           return "GL_FUNC_ADD";
    case GL_BLEND_EQUATION:     return "GL_BLEND_EQUATION";
    case GL_BLEND_EQUATION_ALPHA:return "GL_BLEND_EQUATION_ALPHA";

/* BlendSubtract */
    case GL_FUNC_SUBTRACT:              return "GL_FUNC_SUBTRACT";
    case GL_FUNC_REVERSE_SUBTRACT:      return "GL_FUNC_REVERSE_SUBTRACT";

/* Separate Blend Functions */
    case GL_BLEND_DST_RGB:              return "GL_BLEND_DST_RGB";
    case GL_BLEND_SRC_RGB:              return "GL_BLEND_SRC_RGB";
    case GL_BLEND_DST_ALPHA:            return "GL_BLEND_DST_ALPHA";
    case GL_BLEND_SRC_ALPHA:            return "GL_BLEND_SRC_ALPHA";
    case GL_CONSTANT_COLOR:             return "GL_CONSTANT_COLOR";
    case GL_ONE_MINUS_CONSTANT_COLOR:   return "GL_ONE_MINUS_CONSTANT_COLOR";
    case GL_CONSTANT_ALPHA:             return "GL_CONSTANT_ALPHA";
    case GL_ONE_MINUS_CONSTANT_ALPHA:   return "GL_ONE_MINUS_CONSTANT_ALPHA";
    case GL_BLEND_COLOR:                return "GL_BLEND_COLOR";

/* Buffer Objects */
    case GL_ARRAY_BUFFER:               return "GL_ARRAY_BUFFER";
    case GL_ELEMENT_ARRAY_BUFFER:       return "GL_ELEMENT_ARRAY_BUFFER";
    case GL_ARRAY_BUFFER_BINDING:       return "GL_ARRAY_BUFFER_BINDING";
    case GL_ELEMENT_ARRAY_BUFFER_BINDING:return "GL_ELEMENT_ARRAY_BUFFER_BINDING";

    case GL_STREAM_DRAW:                return "GL_STREAM_DRAW";
    case GL_STATIC_DRAW:                return "GL_STATIC_DRAW";
    case GL_DYNAMIC_DRAW:               return "GL_DYNAMIC_DRAW";

    case GL_BUFFER_SIZE:                return "GL_BUFFER_SIZE";
    case GL_BUFFER_USAGE:               return "GL_BUFFER_USAGE";

    case GL_CURRENT_VERTEX_ATTRIB:      return "GL_CURRENT_VERTEX_ATTRIB";

/* CullFaceMode */
    case GL_FRONT:              return "GL_FRONT";
    case GL_BACK:               return "GL_BACK";
    case GL_FRONT_AND_BACK:     return "GL_FRONT_AND_BACK";

/* EnableCap */
    case GL_TEXTURE_2D:         return "GL_TEXTURE_2D";
    case GL_CULL_FACE:          return "GL_CULL_FACE";
    case GL_BLEND:              return "GL_BLEND";
    case GL_DITHER:             return "GL_DITHER";
    case GL_STENCIL_TEST:       return "GL_STENCIL_TEST";
    case GL_DEPTH_TEST:         return "GL_DEPTH_TEST";
    case GL_SCISSOR_TEST:       return "GL_SCISSOR_TEST"; 
    case GL_POLYGON_OFFSET_FILL:return "GL_POLYGON_OFFSET_FILL";
    case GL_SAMPLE_ALPHA_TO_COVERAGE:return "GL_SAMPLE_ALPHA_TO_COVERAGE";
    case GL_SAMPLE_COVERAGE:    return "GL_SAMPLE_COVERAGE";

/* ErrorCode */
/*    case GL_NO_ERROR                       0 */
    case GL_INVALID_ENUM:       return "GL_INVALID_ENUM";
    case GL_INVALID_VALUE:      return "GL_INVALID_VALUE";
    case GL_INVALID_OPERATION:  return "GL_INVALID_OPERATION";
    case GL_OUT_OF_MEMORY:      return "GL_OUT_OF_MEMORY";

/* FrontFaceDirection */
    case GL_CW:                 return "GL_CW";
    case GL_CCW:                return "GL_CCW";

/* GetPName */
    case GL_LINE_WIDTH:                 return "GL_LINE_WIDTH";
    case GL_ALIASED_POINT_SIZE_RANGE:   return "GL_ALIASED_POINT_SIZE_RANGE";
    case GL_ALIASED_LINE_WIDTH_RANGE:   return "GL_ALIASED_LINE_WIDTH_RANGE";
    case GL_CULL_FACE_MODE:             return "GL_CULL_FACE_MODE";
    case GL_FRONT_FACE:                 return "GL_FRONT_FACE";
    case GL_DEPTH_RANGE:                return "GL_DEPTH_RANGE";
    case GL_DEPTH_WRITEMASK:            return "GL_DEPTH_WRITEMASK";
    case GL_DEPTH_CLEAR_VALUE:          return "GL_DEPTH_CLEAR_VALUE";
    case GL_DEPTH_FUNC:                 return "GL_DEPTH_FUNC";
    case GL_STENCIL_CLEAR_VALUE:        return "GL_STENCIL_CLEAR_VALUE";
    case GL_STENCIL_FUNC:               return "GL_STENCIL_FUNC";
    case GL_STENCIL_FAIL:               return "GL_STENCIL_FAIL";
    case GL_STENCIL_PASS_DEPTH_FAIL:    return "GL_STENCIL_PASS_DEPTH_FAIL";
    case GL_STENCIL_PASS_DEPTH_PASS:    return "GL_STENCIL_PASS_DEPTH_PASS";
    case GL_STENCIL_REF:                return "GL_STENCIL_REF";
    case GL_STENCIL_VALUE_MASK:         return "GL_STENCIL_VALUE_MASK";
    case GL_STENCIL_WRITEMASK:          return "GL_STENCIL_WRITEMASK";
    case GL_STENCIL_BACK_FUNC:          return "GL_STENCIL_BACK_FUNC";
    case GL_STENCIL_BACK_FAIL:          return "GL_STENCIL_BACK_FAIL";
    case GL_STENCIL_BACK_PASS_DEPTH_FAIL:return "GL_STENCIL_BACK_PASS_DEPTH_FAIL";
    case GL_STENCIL_BACK_PASS_DEPTH_PASS:return "GL_STENCIL_BACK_PASS_DEPTH_PASS";
    case GL_STENCIL_BACK_REF:           return "GL_STENCIL_BACK_REF";
    case GL_STENCIL_BACK_VALUE_MASK:    return "GL_STENCIL_BACK_VALUE_MASK";
    case GL_STENCIL_BACK_WRITEMASK:     return "GL_STENCIL_BACK_WRITEMASK";
    case GL_VIEWPORT:                   return "GL_VIEWPORT";
    case GL_SCISSOR_BOX:                return "GL_SCISSOR_BOX";
    case GL_COLOR_CLEAR_VALUE:          return "GL_COLOR_CLEAR_VALUE";
    case GL_COLOR_WRITEMASK:            return "GL_COLOR_WRITEMASK";
    case GL_UNPACK_ALIGNMENT:           return "GL_UNPACK_ALIGNMENT";
    case GL_PACK_ALIGNMENT:             return "GL_PACK_ALIGNMENT";
    case GL_MAX_TEXTURE_SIZE:           return "GL_MAX_TEXTURE_SIZE";
    case GL_MAX_VIEWPORT_DIMS:          return "GL_MAX_VIEWPORT_DIMS";
    case GL_SUBPIXEL_BITS:              return "GL_SUBPIXEL_BITS";
    case GL_RED_BITS:                   return "GL_RED_BITS";
    case GL_GREEN_BITS:                 return "GL_GREEN_BITS";
    case GL_BLUE_BITS:                  return "GL_BLUE_BITS";
    case GL_ALPHA_BITS:                 return "GL_ALPHA_BITS";
    case GL_DEPTH_BITS:                 return "GL_DEPTH_BITS";
    case GL_STENCIL_BITS:               return "GL_STENCIL_BITS";
    case GL_POLYGON_OFFSET_UNITS:       return "GL_POLYGON_OFFSET_UNITS";
    case GL_POLYGON_OFFSET_FACTOR:      return "GL_POLYGON_OFFSET_FACTOR";
    case GL_TEXTURE_BINDING_2D:         return "GL_TEXTURE_BINDING_2D";
    case GL_SAMPLE_BUFFERS:             return "GL_SAMPLE_BUFFERS";
    case GL_SAMPLES:                    return "GL_SAMPLES";
    case GL_SAMPLE_COVERAGE_VALUE:      return "GL_SAMPLE_COVERAGE_VALUE";
    case GL_SAMPLE_COVERAGE_INVERT:     return "GL_SAMPLE_COVERAGE_INVERT";

    case GL_NUM_COMPRESSED_TEXTURE_FORMATS:return "GL_NUM_COMPRESSED_TEXTURE_FORMATS";
    case GL_COMPRESSED_TEXTURE_FORMATS: return "GL_COMPRESSED_TEXTURE_FORMATS";

/* HintMode */
    case GL_DONT_CARE:                  return "GL_DONT_CARE";
    case GL_FASTEST:                    return "GL_FASTEST";
    case GL_NICEST:                     return "GL_NICEST";

/* HintTarget */
    case GL_GENERATE_MIPMAP_HINT:       return "GL_GENERATE_MIPMAP_HINT";

/* DataType */
    case GL_BYTE:                       return "GL_BYTE";
    case GL_UNSIGNED_BYTE:              return "GL_UNSIGNED_BYTE";
    case GL_SHORT:                      return "GL_SHORT";
    case GL_UNSIGNED_SHORT:             return "GL_UNSIGNED_SHORT";
    case GL_INT:                        return "GL_INT";
    case GL_UNSIGNED_INT:               return "GL_UNSIGNED_INT";
    case GL_FLOAT:                      return "GL_FLOAT";
    case GL_FIXED:                      return "GL_FIXED";

/* PixelFormat */
    case GL_DEPTH_COMPONENT:            return "GL_DEPTH_COMPONENT";
    case GL_ALPHA:                      return "GL_ALPHA";
    case GL_RGB:                        return "GL_RGB";
    case GL_RGBA:                       return "GL_RGBA";
    case GL_LUMINANCE:                  return "GL_LUMINANCE";
    case GL_LUMINANCE_ALPHA:            return "GL_LUMINANCE_ALPHA";

/* PixelType */
    case GL_UNSIGNED_SHORT_4_4_4_4:     return "GL_UNSIGNED_SHORT_4_4_4_4";
    case GL_UNSIGNED_SHORT_5_5_5_1:     return "GL_UNSIGNED_SHORT_5_5_5_1";
    case GL_UNSIGNED_SHORT_5_6_5:       return "GL_UNSIGNED_SHORT_5_6_5";

/* Shaders */
    case GL_FRAGMENT_SHADER:            return "GL_FRAGMENT_SHADER";
    case GL_VERTEX_SHADER:              return "GL_VERTEX_SHADER";
    case GL_MAX_VERTEX_ATTRIBS:         return "GL_MAX_VERTEX_ATTRIBS";
    case GL_MAX_VERTEX_UNIFORM_VECTORS: return "GL_MAX_VERTEX_UNIFORM_VECTORS";
    case GL_MAX_VARYING_VECTORS:        return "GL_MAX_VARYING_VECTORS";
    case GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS:return "GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS";
    case GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS:return "GL_MAX_VERTEX_TEXTURE_IMAGE_UNITS";
    case GL_MAX_TEXTURE_IMAGE_UNITS:    return "GL_MAX_TEXTURE_IMAGE_UNITS";
    case GL_MAX_FRAGMENT_UNIFORM_VECTORS:return "GL_MAX_FRAGMENT_UNIFORM_VECTORS";
    case GL_SHADER_TYPE:                return "GL_SHADER_TYPE";
    case GL_DELETE_STATUS:              return "GL_DELETE_STATUS";
    case GL_LINK_STATUS:                return "GL_LINK_STATUS";
    case GL_VALIDATE_STATUS:            return "GL_VALIDATE_STATUS";
    case GL_ATTACHED_SHADERS:           return "GL_ATTACHED_SHADERS";
    case GL_ACTIVE_UNIFORMS:            return "GL_ACTIVE_UNIFORMS";
    case GL_ACTIVE_UNIFORM_MAX_LENGTH:  return "GL_ACTIVE_UNIFORM_MAX_LENGTH";
    case GL_ACTIVE_ATTRIBUTES:          return "GL_ACTIVE_ATTRIBUTES";
    case GL_ACTIVE_ATTRIBUTE_MAX_LENGTH:return "GL_ACTIVE_ATTRIBUTE_MAX_LENGTH";
    case GL_SHADING_LANGUAGE_VERSION:   return "GL_SHADING_LANGUAGE_VERSION";
    case GL_CURRENT_PROGRAM:            return "GL_CURRENT_PROGRAM";

/* StencilFunction */
    case GL_NEVER:              return "GL_NEVER";
    case GL_LESS:               return "GL_LESS";
    case GL_EQUAL:              return "GL_EQUAL";
    case GL_LEQUAL:             return "GL_LEQUAL";
    case GL_GREATER:            return "GL_GREATER";
    case GL_NOTEQUAL:           return "GL_NOTEQUAL";
    case GL_GEQUAL:             return "GL_GEQUAL";
    case GL_ALWAYS:             return "GL_ALWAYS";

/* StencilOp */
/*      GL_ZERO */
    case GL_KEEP:               return "GL_KEEP";
    case GL_REPLACE:            return "GL_REPLACE";
    case GL_INCR:               return "GL_INCR";
    case GL_DECR:               return "GL_DECR";
    case GL_INVERT:             return "GL_INVERT";
    case GL_INCR_WRAP:          return "GL_INCR_WRAP";
    case GL_DECR_WRAP:          return "GL_DECR_WRAP";

/* StringName */
    case GL_VENDOR:             return "GL_VENDOR";
    case GL_RENDERER:           return "GL_RENDERER";
    case GL_VERSION:            return "GL_VERSION";
    case GL_EXTENSIONS:         return "GL_EXTENSIONS";

/* TextureMagFilter */
    case GL_NEAREST:            return "GL_NEAREST";
    case GL_LINEAR:             return "GL_LINEAR"; 

/* TextureMinFilter */
    case GL_NEAREST_MIPMAP_NEAREST:     return "GL_NEAREST_MIPMAP_NEAREST";
    case GL_LINEAR_MIPMAP_NEAREST:      return "GL_LINEAR_MIPMAP_NEAREST";
    case GL_NEAREST_MIPMAP_LINEAR:      return "GL_NEAREST_MIPMAP_LINEAR";
    case GL_LINEAR_MIPMAP_LINEAR:       return "GL_LINEAR_MIPMAP_LINEAR";

/* TextureParameterName */
    case GL_TEXTURE_MAG_FILTER:         return "GL_TEXTURE_MAG_FILTER";
    case GL_TEXTURE_MIN_FILTER:         return "GL_TEXTURE_MIN_FILTER";
    case GL_TEXTURE_WRAP_S:             return "GL_TEXTURE_WRAP_S";
    case GL_TEXTURE_WRAP_T:             return "GL_TEXTURE_WRAP_T";

/* TextureTarget */
    case GL_TEXTURE:                    return "GL_TEXTURE";

    case GL_TEXTURE_CUBE_MAP:           return "GL_TEXTURE_CUBE_MAP";
    case GL_TEXTURE_BINDING_CUBE_MAP:   return "GL_TEXTURE_BINDING_CUBE_MAP";
    case GL_TEXTURE_CUBE_MAP_POSITIVE_X:return "GL_TEXTURE_CUBE_MAP_POSITIVE_X";
    case GL_TEXTURE_CUBE_MAP_NEGATIVE_X:return "GL_TEXTURE_CUBE_MAP_NEGATIVE_X";
    case GL_TEXTURE_CUBE_MAP_POSITIVE_Y:return "GL_TEXTURE_CUBE_MAP_POSITIVE_Y";
    case GL_TEXTURE_CUBE_MAP_NEGATIVE_Y:return "GL_TEXTURE_CUBE_MAP_NEGATIVE_Y";
    case GL_TEXTURE_CUBE_MAP_POSITIVE_Z:return "GL_TEXTURE_CUBE_MAP_POSITIVE_Z";
    case GL_TEXTURE_CUBE_MAP_NEGATIVE_Z:return "GL_TEXTURE_CUBE_MAP_NEGATIVE_Z";
    case GL_MAX_CUBE_MAP_TEXTURE_SIZE:  return "GL_MAX_CUBE_MAP_TEXTURE_SIZE";

/* TextureUnit */
    case GL_TEXTURE0:           return "GL_TEXTURE0";
    case GL_TEXTURE1:           return "GL_TEXTURE1";
    case GL_TEXTURE2:           return "GL_TEXTURE2";
    case GL_TEXTURE3:           return "GL_TEXTURE3";
    case GL_TEXTURE4:           return "GL_TEXTURE4";
    case GL_TEXTURE5:           return "GL_TEXTURE5";
    case GL_TEXTURE6:           return "GL_TEXTURE6";
    case GL_TEXTURE7:           return "GL_TEXTURE7";
    case GL_TEXTURE8:           return "GL_TEXTURE8";
    case GL_TEXTURE9:           return "GL_TEXTURE9";
    case GL_TEXTURE10:          return "GL_TEXTURE10";
    case GL_TEXTURE11:          return "GL_TEXTURE11";
    case GL_TEXTURE12:          return "GL_TEXTURE12";
    case GL_TEXTURE13:          return "GL_TEXTURE13";
    case GL_TEXTURE14:          return "GL_TEXTURE14";
    case GL_TEXTURE15:          return "GL_TEXTURE15";
    case GL_TEXTURE16:          return "GL_TEXTURE16";
    case GL_TEXTURE17:          return "GL_TEXTURE17";
    case GL_TEXTURE18:          return "GL_TEXTURE18";
    case GL_TEXTURE19:          return "GL_TEXTURE19";
    case GL_TEXTURE20:          return "GL_TEXTURE20";
    case GL_TEXTURE21:          return "GL_TEXTURE21";
    case GL_TEXTURE22:          return "GL_TEXTURE22";
    case GL_TEXTURE23:          return "GL_TEXTURE23";
    case GL_TEXTURE24:          return "GL_TEXTURE24";
    case GL_TEXTURE25:          return "GL_TEXTURE25";
    case GL_TEXTURE26:          return "GL_TEXTURE26";
    case GL_TEXTURE27:          return "GL_TEXTURE27";
    case GL_TEXTURE28:          return "GL_TEXTURE28";
    case GL_TEXTURE29:          return "GL_TEXTURE29";
    case GL_TEXTURE30:          return "GL_TEXTURE30";
    case GL_TEXTURE31:          return "GL_TEXTURE31";
    case GL_ACTIVE_TEXTURE:     return "GL_ACTIVE_TEXTURE";

/* TextureWrapMode */
    case GL_REPEAT:             return "GL_REPEAT";
    case GL_CLAMP_TO_EDGE:      return "GL_CLAMP_TO_EDGE";
    case GL_MIRRORED_REPEAT:    return "GL_MIRRORED_REPEAT";

/* Uniform Types */
    case GL_FLOAT_VEC2:         return "GL_FLOAT_VEC2";
    case GL_FLOAT_VEC3:         return "GL_FLOAT_VEC3";
    case GL_FLOAT_VEC4:         return "GL_FLOAT_VEC4";
    case GL_INT_VEC2:           return "GL_INT_VEC2";
    case GL_INT_VEC3:           return "GL_INT_VEC3";
    case GL_INT_VEC4:           return "GL_INT_VEC4";
    case GL_BOOL:               return "GL_BOOL";
    case GL_BOOL_VEC2:          return "GL_BOOL_VEC2";
    case GL_BOOL_VEC3:          return "GL_BOOL_VEC3";
    case GL_BOOL_VEC4:          return "GL_BOOL_VEC4";
    case GL_FLOAT_MAT2:         return "GL_FLOAT_MAT2";
    case GL_FLOAT_MAT3:         return "GL_FLOAT_MAT3";
    case GL_FLOAT_MAT4:         return "GL_FLOAT_MAT4";
    case GL_SAMPLER_2D:         return "GL_SAMPLER_2D";
    case GL_SAMPLER_CUBE:       return "GL_SAMPLER_CUBE";

/* Vertex Arrays */
    case GL_VERTEX_ATTRIB_ARRAY_ENABLED:return "GL_VERTEX_ATTRIB_ARRAY_ENABLED";
    case GL_VERTEX_ATTRIB_ARRAY_SIZE:   return "GL_VERTEX_ATTRIB_ARRAY_SIZE";
    case GL_VERTEX_ATTRIB_ARRAY_STRIDE: return "GL_VERTEX_ATTRIB_ARRAY_STRIDE";
    case GL_VERTEX_ATTRIB_ARRAY_TYPE:   return "GL_VERTEX_ATTRIB_ARRAY_TYPE";
    case GL_VERTEX_ATTRIB_ARRAY_NORMALIZED:return "GL_VERTEX_ATTRIB_ARRAY_NORMALIZED";
    case GL_VERTEX_ATTRIB_ARRAY_POINTER:return "GL_VERTEX_ATTRIB_ARRAY_POINTER";
    case GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING: return "GL_VERTEX_ATTRIB_ARRAY_BUFFER_BINDING";

/* Read Format */
    case GL_IMPLEMENTATION_COLOR_READ_TYPE:return "GL_IMPLEMENTATION_COLOR_READ_TYPE";
    case GL_IMPLEMENTATION_COLOR_READ_FORMAT:return "GL_IMPLEMENTATION_COLOR_READ_FORMAT";

/* Shader Source */
    case GL_COMPILE_STATUS:             return "GL_COMPILE_STATUS";
    case GL_INFO_LOG_LENGTH:            return "GL_INFO_LOG_LENGTH";
    case GL_SHADER_SOURCE_LENGTH:       return "GL_SHADER_SOURCE_LENGTH";
    case GL_SHADER_COMPILER:            return "GL_SHADER_COMPILER";

/* Shader Binary */
    case GL_SHADER_BINARY_FORMATS:      return "GL_SHADER_BINARY_FORMATS";
    case GL_NUM_SHADER_BINARY_FORMATS:  return "GL_NUM_SHADER_BINARY_FORMATS";

/* Shader Precision-Specified Types */
    case GL_LOW_FLOAT:          return "GL_LOW_FLOAT";
    case GL_MEDIUM_FLOAT:       return "GL_MEDIUM_FLOAT";
    case GL_HIGH_FLOAT:         return "GL_HIGH_FLOAT";
    case GL_LOW_INT:            return "GL_LOW_INT";
    case GL_MEDIUM_INT:         return "GL_MEDIUM_INT";
    case GL_HIGH_INT:           return "GL_HIGH_INT";

/* Framebuffer Object. */
    case GL_FRAMEBUFFER:        return "GL_FRAMEBUFFER";
    case GL_RENDERBUFFER:       return "GL_RENDERBUFFER";

    case GL_RGBA4:              return "GL_RGBA4";
    case GL_RGB5_A1:            return "GL_RGB5_A1";
    case GL_RGB565:             return "GL_RGB565";
    case GL_DEPTH_COMPONENT16:  return "GL_DEPTH_COMPONENT16";
    case GL_STENCIL_INDEX:      return "GL_STENCIL_INDEX";
    case GL_STENCIL_INDEX8:     return "GL_STENCIL_INDEX8";

    case GL_RENDERBUFFER_WIDTH:         return "GL_RENDERBUFFER_WIDTH";
    case GL_RENDERBUFFER_HEIGHT:        return "GL_RENDERBUFFER_HEIGHT";
    case GL_RENDERBUFFER_INTERNAL_FORMAT:return "GL_RENDERBUFFER_INTERNAL_FORMAT";
    case GL_RENDERBUFFER_RED_SIZE:      return "GL_RENDERBUFFER_RED_SIZE";
    case GL_RENDERBUFFER_GREEN_SIZE:    return "GL_RENDERBUFFER_GREEN_SIZE";
    case GL_RENDERBUFFER_BLUE_SIZE:     return "GL_RENDERBUFFER_BLUE_SIZE";
    case GL_RENDERBUFFER_ALPHA_SIZE:    return "GL_RENDERBUFFER_ALPHA_SIZE";
    case GL_RENDERBUFFER_DEPTH_SIZE:    return "GL_RENDERBUFFER_DEPTH_SIZE";
    case GL_RENDERBUFFER_STENCIL_SIZE:  return "GL_RENDERBUFFER_STENCIL_SIZE";

    case GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE:         return "GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE";
    case GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME:         return "GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME";
    case GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL:       return "GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_LEVEL";
    case GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE:return "GL_FRAMEBUFFER_ATTACHMENT_TEXTURE_CUBE_MAP_FACE";

    case GL_COLOR_ATTACHMENT0:          return "GL_COLOR_ATTACHMENT0";
    case GL_DEPTH_ATTACHMENT:           return "GL_DEPTH_ATTACHMENT";
    case GL_STENCIL_ATTACHMENT:         return "GL_STENCIL_ATTACHMENT";

    case GL_FRAMEBUFFER_COMPLETE:       return "GL_FRAMEBUFFER_COMPLETE";
    case GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:  return "GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT";
    case GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:  return "GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT";
    case GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS:          return "GL_FRAMEBUFFER_INCOMPLETE_DIMENSIONS";
    case GL_FRAMEBUFFER_UNSUPPORTED:            return "GL_FRAMEBUFFER_UNSUPPORTED";

    case GL_FRAMEBUFFER_BINDING:                return "GL_FRAMEBUFFER_BINDING";
    case GL_RENDERBUFFER_BINDING:               return "GL_RENDERBUFFER_BINDING";
    case GL_MAX_RENDERBUFFER_SIZE:              return "GL_MAX_RENDERBUFFER_SIZE";

    case GL_INVALID_FRAMEBUFFER_OPERATION:      return "GL_INVALID_FRAMEBUFFER_OPERATION";
    }

    static char buf[1024];
    static char *ptr = buf;

    if (ptr + 16 > buf + sizeof(buf)) ptr = buf;
    char *res = ptr;
    snprintf(ptr, 16, "0x%x", val);
    ptr += strlen(ptr) + 1;
    return res;
}
