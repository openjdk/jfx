


#include "audio-enumtypes.h"

#include "audio.h"
#include "audio-format.h"
#include "audio-channels.h"
#ifndef GSTREAMER_LITE
#include "audio-channel-mixer.h"
#endif // GSTREAMER_LITE
#include "audio-converter.h"
#include "audio-info.h"
#include "audio-quantize.h"
#include "audio-resampler.h"
#include "gstaudioringbuffer.h"
#include "gstaudiobasesrc.h"
#ifndef GSTREAMER_LITE
#include "gstaudiocdsrc.h"
#endif // GSTREAMER_LITE
#include "gstaudiobasesink.h"

/* enumerations from "audio-channel-mixer.h" */
GType
gst_audio_channel_mixer_flags_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      { GST_AUDIO_CHANNEL_MIXER_FLAGS_NONE, "GST_AUDIO_CHANNEL_MIXER_FLAGS_NONE", "none" },
      { GST_AUDIO_CHANNEL_MIXER_FLAGS_NON_INTERLEAVED_IN, "GST_AUDIO_CHANNEL_MIXER_FLAGS_NON_INTERLEAVED_IN", "non-interleaved-in" },
      { GST_AUDIO_CHANNEL_MIXER_FLAGS_NON_INTERLEAVED_OUT, "GST_AUDIO_CHANNEL_MIXER_FLAGS_NON_INTERLEAVED_OUT", "non-interleaved-out" },
      { GST_AUDIO_CHANNEL_MIXER_FLAGS_UNPOSITIONED_IN, "GST_AUDIO_CHANNEL_MIXER_FLAGS_UNPOSITIONED_IN", "unpositioned-in" },
      { GST_AUDIO_CHANNEL_MIXER_FLAGS_UNPOSITIONED_OUT, "GST_AUDIO_CHANNEL_MIXER_FLAGS_UNPOSITIONED_OUT", "unpositioned-out" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_flags_register_static ("GstAudioChannelMixerFlags", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "audio-channels.h" */
GType
gst_audio_channel_position_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_CHANNEL_POSITION_NONE, "GST_AUDIO_CHANNEL_POSITION_NONE", "none" },
      { GST_AUDIO_CHANNEL_POSITION_MONO, "GST_AUDIO_CHANNEL_POSITION_MONO", "mono" },
      { GST_AUDIO_CHANNEL_POSITION_INVALID, "GST_AUDIO_CHANNEL_POSITION_INVALID", "invalid" },
      { GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT, "GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT", "front-left" },
      { GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, "GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT", "front-right" },
      { GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER, "GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER", "front-center" },
      { GST_AUDIO_CHANNEL_POSITION_LFE1, "GST_AUDIO_CHANNEL_POSITION_LFE1", "lfe1" },
      { GST_AUDIO_CHANNEL_POSITION_REAR_LEFT, "GST_AUDIO_CHANNEL_POSITION_REAR_LEFT", "rear-left" },
      { GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT, "GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT", "rear-right" },
      { GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT_OF_CENTER, "GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT_OF_CENTER", "front-left-of-center" },
      { GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT_OF_CENTER, "GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT_OF_CENTER", "front-right-of-center" },
      { GST_AUDIO_CHANNEL_POSITION_REAR_CENTER, "GST_AUDIO_CHANNEL_POSITION_REAR_CENTER", "rear-center" },
      { GST_AUDIO_CHANNEL_POSITION_LFE2, "GST_AUDIO_CHANNEL_POSITION_LFE2", "lfe2" },
      { GST_AUDIO_CHANNEL_POSITION_SIDE_LEFT, "GST_AUDIO_CHANNEL_POSITION_SIDE_LEFT", "side-left" },
      { GST_AUDIO_CHANNEL_POSITION_SIDE_RIGHT, "GST_AUDIO_CHANNEL_POSITION_SIDE_RIGHT", "side-right" },
      { GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_LEFT, "GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_LEFT", "top-front-left" },
      { GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_RIGHT, "GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_RIGHT", "top-front-right" },
      { GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_CENTER, "GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_CENTER", "top-front-center" },
      { GST_AUDIO_CHANNEL_POSITION_TOP_CENTER, "GST_AUDIO_CHANNEL_POSITION_TOP_CENTER", "top-center" },
      { GST_AUDIO_CHANNEL_POSITION_TOP_REAR_LEFT, "GST_AUDIO_CHANNEL_POSITION_TOP_REAR_LEFT", "top-rear-left" },
      { GST_AUDIO_CHANNEL_POSITION_TOP_REAR_RIGHT, "GST_AUDIO_CHANNEL_POSITION_TOP_REAR_RIGHT", "top-rear-right" },
      { GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_LEFT, "GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_LEFT", "top-side-left" },
      { GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_RIGHT, "GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_RIGHT", "top-side-right" },
      { GST_AUDIO_CHANNEL_POSITION_TOP_REAR_CENTER, "GST_AUDIO_CHANNEL_POSITION_TOP_REAR_CENTER", "top-rear-center" },
      { GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_CENTER, "GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_CENTER", "bottom-front-center" },
      { GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_LEFT, "GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_LEFT", "bottom-front-left" },
      { GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_RIGHT, "GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_RIGHT", "bottom-front-right" },
      { GST_AUDIO_CHANNEL_POSITION_WIDE_LEFT, "GST_AUDIO_CHANNEL_POSITION_WIDE_LEFT", "wide-left" },
      { GST_AUDIO_CHANNEL_POSITION_WIDE_RIGHT, "GST_AUDIO_CHANNEL_POSITION_WIDE_RIGHT", "wide-right" },
      { GST_AUDIO_CHANNEL_POSITION_SURROUND_LEFT, "GST_AUDIO_CHANNEL_POSITION_SURROUND_LEFT", "surround-left" },
      { GST_AUDIO_CHANNEL_POSITION_SURROUND_RIGHT, "GST_AUDIO_CHANNEL_POSITION_SURROUND_RIGHT", "surround-right" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioChannelPosition", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "audio-converter.h" */
GType
gst_audio_converter_flags_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      { GST_AUDIO_CONVERTER_FLAG_NONE, "GST_AUDIO_CONVERTER_FLAG_NONE", "none" },
      { GST_AUDIO_CONVERTER_FLAG_IN_WRITABLE, "GST_AUDIO_CONVERTER_FLAG_IN_WRITABLE", "in-writable" },
      { GST_AUDIO_CONVERTER_FLAG_VARIABLE_RATE, "GST_AUDIO_CONVERTER_FLAG_VARIABLE_RATE", "variable-rate" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_flags_register_static ("GstAudioConverterFlags", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "audio-format.h" */
GType
gst_audio_format_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_FORMAT_UNKNOWN, "GST_AUDIO_FORMAT_UNKNOWN", "unknown" },
      { GST_AUDIO_FORMAT_ENCODED, "GST_AUDIO_FORMAT_ENCODED", "encoded" },
      { GST_AUDIO_FORMAT_S8, "GST_AUDIO_FORMAT_S8", "s8" },
      { GST_AUDIO_FORMAT_U8, "GST_AUDIO_FORMAT_U8", "u8" },
      { GST_AUDIO_FORMAT_S16LE, "GST_AUDIO_FORMAT_S16LE", "s16le" },
      { GST_AUDIO_FORMAT_S16BE, "GST_AUDIO_FORMAT_S16BE", "s16be" },
      { GST_AUDIO_FORMAT_U16LE, "GST_AUDIO_FORMAT_U16LE", "u16le" },
      { GST_AUDIO_FORMAT_U16BE, "GST_AUDIO_FORMAT_U16BE", "u16be" },
      { GST_AUDIO_FORMAT_S24_32LE, "GST_AUDIO_FORMAT_S24_32LE", "s24-32le" },
      { GST_AUDIO_FORMAT_S24_32BE, "GST_AUDIO_FORMAT_S24_32BE", "s24-32be" },
      { GST_AUDIO_FORMAT_U24_32LE, "GST_AUDIO_FORMAT_U24_32LE", "u24-32le" },
      { GST_AUDIO_FORMAT_U24_32BE, "GST_AUDIO_FORMAT_U24_32BE", "u24-32be" },
      { GST_AUDIO_FORMAT_S32LE, "GST_AUDIO_FORMAT_S32LE", "s32le" },
      { GST_AUDIO_FORMAT_S32BE, "GST_AUDIO_FORMAT_S32BE", "s32be" },
      { GST_AUDIO_FORMAT_U32LE, "GST_AUDIO_FORMAT_U32LE", "u32le" },
      { GST_AUDIO_FORMAT_U32BE, "GST_AUDIO_FORMAT_U32BE", "u32be" },
      { GST_AUDIO_FORMAT_S24LE, "GST_AUDIO_FORMAT_S24LE", "s24le" },
      { GST_AUDIO_FORMAT_S24BE, "GST_AUDIO_FORMAT_S24BE", "s24be" },
      { GST_AUDIO_FORMAT_U24LE, "GST_AUDIO_FORMAT_U24LE", "u24le" },
      { GST_AUDIO_FORMAT_U24BE, "GST_AUDIO_FORMAT_U24BE", "u24be" },
      { GST_AUDIO_FORMAT_S20LE, "GST_AUDIO_FORMAT_S20LE", "s20le" },
      { GST_AUDIO_FORMAT_S20BE, "GST_AUDIO_FORMAT_S20BE", "s20be" },
      { GST_AUDIO_FORMAT_U20LE, "GST_AUDIO_FORMAT_U20LE", "u20le" },
      { GST_AUDIO_FORMAT_U20BE, "GST_AUDIO_FORMAT_U20BE", "u20be" },
      { GST_AUDIO_FORMAT_S18LE, "GST_AUDIO_FORMAT_S18LE", "s18le" },
      { GST_AUDIO_FORMAT_S18BE, "GST_AUDIO_FORMAT_S18BE", "s18be" },
      { GST_AUDIO_FORMAT_U18LE, "GST_AUDIO_FORMAT_U18LE", "u18le" },
      { GST_AUDIO_FORMAT_U18BE, "GST_AUDIO_FORMAT_U18BE", "u18be" },
      { GST_AUDIO_FORMAT_F32LE, "GST_AUDIO_FORMAT_F32LE", "f32le" },
      { GST_AUDIO_FORMAT_F32BE, "GST_AUDIO_FORMAT_F32BE", "f32be" },
      { GST_AUDIO_FORMAT_F64LE, "GST_AUDIO_FORMAT_F64LE", "f64le" },
      { GST_AUDIO_FORMAT_F64BE, "GST_AUDIO_FORMAT_F64BE", "f64be" },
      { GST_AUDIO_FORMAT_S16, "GST_AUDIO_FORMAT_S16", "s16" },
      { GST_AUDIO_FORMAT_U16, "GST_AUDIO_FORMAT_U16", "u16" },
      { GST_AUDIO_FORMAT_S24_32, "GST_AUDIO_FORMAT_S24_32", "s24-32" },
      { GST_AUDIO_FORMAT_U24_32, "GST_AUDIO_FORMAT_U24_32", "u24-32" },
      { GST_AUDIO_FORMAT_S32, "GST_AUDIO_FORMAT_S32", "s32" },
      { GST_AUDIO_FORMAT_U32, "GST_AUDIO_FORMAT_U32", "u32" },
      { GST_AUDIO_FORMAT_S24, "GST_AUDIO_FORMAT_S24", "s24" },
      { GST_AUDIO_FORMAT_U24, "GST_AUDIO_FORMAT_U24", "u24" },
      { GST_AUDIO_FORMAT_S20, "GST_AUDIO_FORMAT_S20", "s20" },
      { GST_AUDIO_FORMAT_U20, "GST_AUDIO_FORMAT_U20", "u20" },
      { GST_AUDIO_FORMAT_S18, "GST_AUDIO_FORMAT_S18", "s18" },
      { GST_AUDIO_FORMAT_U18, "GST_AUDIO_FORMAT_U18", "u18" },
      { GST_AUDIO_FORMAT_F32, "GST_AUDIO_FORMAT_F32", "f32" },
      { GST_AUDIO_FORMAT_F64, "GST_AUDIO_FORMAT_F64", "f64" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioFormat", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
GType
gst_audio_format_flags_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      { GST_AUDIO_FORMAT_FLAG_INTEGER, "GST_AUDIO_FORMAT_FLAG_INTEGER", "integer" },
      { GST_AUDIO_FORMAT_FLAG_FLOAT, "GST_AUDIO_FORMAT_FLAG_FLOAT", "float" },
      { GST_AUDIO_FORMAT_FLAG_SIGNED, "GST_AUDIO_FORMAT_FLAG_SIGNED", "signed" },
      { GST_AUDIO_FORMAT_FLAG_COMPLEX, "GST_AUDIO_FORMAT_FLAG_COMPLEX", "complex" },
      { GST_AUDIO_FORMAT_FLAG_UNPACK, "GST_AUDIO_FORMAT_FLAG_UNPACK", "unpack" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_flags_register_static ("GstAudioFormatFlags", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
GType
gst_audio_pack_flags_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      { GST_AUDIO_PACK_FLAG_NONE, "GST_AUDIO_PACK_FLAG_NONE", "none" },
      { GST_AUDIO_PACK_FLAG_TRUNCATE_RANGE, "GST_AUDIO_PACK_FLAG_TRUNCATE_RANGE", "truncate-range" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_flags_register_static ("GstAudioPackFlags", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "audio-info.h" */
GType
gst_audio_flags_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      { GST_AUDIO_FLAG_NONE, "GST_AUDIO_FLAG_NONE", "none" },
      { GST_AUDIO_FLAG_UNPOSITIONED, "GST_AUDIO_FLAG_UNPOSITIONED", "unpositioned" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_flags_register_static ("GstAudioFlags", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
GType
gst_audio_layout_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_LAYOUT_INTERLEAVED, "GST_AUDIO_LAYOUT_INTERLEAVED", "interleaved" },
      { GST_AUDIO_LAYOUT_NON_INTERLEAVED, "GST_AUDIO_LAYOUT_NON_INTERLEAVED", "non-interleaved" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioLayout", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "audio-quantize.h" */
GType
gst_audio_dither_method_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_DITHER_NONE, "GST_AUDIO_DITHER_NONE", "none" },
      { GST_AUDIO_DITHER_RPDF, "GST_AUDIO_DITHER_RPDF", "rpdf" },
      { GST_AUDIO_DITHER_TPDF, "GST_AUDIO_DITHER_TPDF", "tpdf" },
      { GST_AUDIO_DITHER_TPDF_HF, "GST_AUDIO_DITHER_TPDF_HF", "tpdf-hf" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioDitherMethod", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
GType
gst_audio_noise_shaping_method_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_NOISE_SHAPING_NONE, "GST_AUDIO_NOISE_SHAPING_NONE", "none" },
      { GST_AUDIO_NOISE_SHAPING_ERROR_FEEDBACK, "GST_AUDIO_NOISE_SHAPING_ERROR_FEEDBACK", "error-feedback" },
      { GST_AUDIO_NOISE_SHAPING_SIMPLE, "GST_AUDIO_NOISE_SHAPING_SIMPLE", "simple" },
      { GST_AUDIO_NOISE_SHAPING_MEDIUM, "GST_AUDIO_NOISE_SHAPING_MEDIUM", "medium" },
      { GST_AUDIO_NOISE_SHAPING_HIGH, "GST_AUDIO_NOISE_SHAPING_HIGH", "high" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioNoiseShapingMethod", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
GType
gst_audio_quantize_flags_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      { GST_AUDIO_QUANTIZE_FLAG_NONE, "GST_AUDIO_QUANTIZE_FLAG_NONE", "none" },
      { GST_AUDIO_QUANTIZE_FLAG_NON_INTERLEAVED, "GST_AUDIO_QUANTIZE_FLAG_NON_INTERLEAVED", "non-interleaved" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_flags_register_static ("GstAudioQuantizeFlags", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "audio-resampler.h" */
GType
gst_audio_resampler_filter_mode_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_RESAMPLER_FILTER_MODE_INTERPOLATED, "GST_AUDIO_RESAMPLER_FILTER_MODE_INTERPOLATED", "interpolated" },
      { GST_AUDIO_RESAMPLER_FILTER_MODE_FULL, "GST_AUDIO_RESAMPLER_FILTER_MODE_FULL", "full" },
      { GST_AUDIO_RESAMPLER_FILTER_MODE_AUTO, "GST_AUDIO_RESAMPLER_FILTER_MODE_AUTO", "auto" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioResamplerFilterMode", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
GType
gst_audio_resampler_filter_interpolation_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_RESAMPLER_FILTER_INTERPOLATION_NONE, "GST_AUDIO_RESAMPLER_FILTER_INTERPOLATION_NONE", "none" },
      { GST_AUDIO_RESAMPLER_FILTER_INTERPOLATION_LINEAR, "GST_AUDIO_RESAMPLER_FILTER_INTERPOLATION_LINEAR", "linear" },
      { GST_AUDIO_RESAMPLER_FILTER_INTERPOLATION_CUBIC, "GST_AUDIO_RESAMPLER_FILTER_INTERPOLATION_CUBIC", "cubic" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioResamplerFilterInterpolation", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
GType
gst_audio_resampler_method_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_RESAMPLER_METHOD_NEAREST, "GST_AUDIO_RESAMPLER_METHOD_NEAREST", "nearest" },
      { GST_AUDIO_RESAMPLER_METHOD_LINEAR, "GST_AUDIO_RESAMPLER_METHOD_LINEAR", "linear" },
      { GST_AUDIO_RESAMPLER_METHOD_CUBIC, "GST_AUDIO_RESAMPLER_METHOD_CUBIC", "cubic" },
      { GST_AUDIO_RESAMPLER_METHOD_BLACKMAN_NUTTALL, "GST_AUDIO_RESAMPLER_METHOD_BLACKMAN_NUTTALL", "blackman-nuttall" },
      { GST_AUDIO_RESAMPLER_METHOD_KAISER, "GST_AUDIO_RESAMPLER_METHOD_KAISER", "kaiser" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioResamplerMethod", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
GType
gst_audio_resampler_flags_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      { GST_AUDIO_RESAMPLER_FLAG_NONE, "GST_AUDIO_RESAMPLER_FLAG_NONE", "none" },
      { GST_AUDIO_RESAMPLER_FLAG_NON_INTERLEAVED_IN, "GST_AUDIO_RESAMPLER_FLAG_NON_INTERLEAVED_IN", "non-interleaved-in" },
      { GST_AUDIO_RESAMPLER_FLAG_NON_INTERLEAVED_OUT, "GST_AUDIO_RESAMPLER_FLAG_NON_INTERLEAVED_OUT", "non-interleaved-out" },
      { GST_AUDIO_RESAMPLER_FLAG_VARIABLE_RATE, "GST_AUDIO_RESAMPLER_FLAG_VARIABLE_RATE", "variable-rate" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_flags_register_static ("GstAudioResamplerFlags", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "gstaudiobasesink.h" */
GType
gst_audio_base_sink_slave_method_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_BASE_SINK_SLAVE_RESAMPLE, "GST_AUDIO_BASE_SINK_SLAVE_RESAMPLE", "resample" },
      { GST_AUDIO_BASE_SINK_SLAVE_SKEW, "GST_AUDIO_BASE_SINK_SLAVE_SKEW", "skew" },
      { GST_AUDIO_BASE_SINK_SLAVE_NONE, "GST_AUDIO_BASE_SINK_SLAVE_NONE", "none" },
      { GST_AUDIO_BASE_SINK_SLAVE_CUSTOM, "GST_AUDIO_BASE_SINK_SLAVE_CUSTOM", "custom" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioBaseSinkSlaveMethod", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
GType
gst_audio_base_sink_discont_reason_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_BASE_SINK_DISCONT_REASON_NO_DISCONT, "GST_AUDIO_BASE_SINK_DISCONT_REASON_NO_DISCONT", "no-discont" },
      { GST_AUDIO_BASE_SINK_DISCONT_REASON_NEW_CAPS, "GST_AUDIO_BASE_SINK_DISCONT_REASON_NEW_CAPS", "new-caps" },
      { GST_AUDIO_BASE_SINK_DISCONT_REASON_FLUSH, "GST_AUDIO_BASE_SINK_DISCONT_REASON_FLUSH", "flush" },
      { GST_AUDIO_BASE_SINK_DISCONT_REASON_SYNC_LATENCY, "GST_AUDIO_BASE_SINK_DISCONT_REASON_SYNC_LATENCY", "sync-latency" },
      { GST_AUDIO_BASE_SINK_DISCONT_REASON_ALIGNMENT, "GST_AUDIO_BASE_SINK_DISCONT_REASON_ALIGNMENT", "alignment" },
      { GST_AUDIO_BASE_SINK_DISCONT_REASON_DEVICE_FAILURE, "GST_AUDIO_BASE_SINK_DISCONT_REASON_DEVICE_FAILURE", "device-failure" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioBaseSinkDiscontReason", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "gstaudiobasesrc.h" */
GType
gst_audio_base_src_slave_method_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_BASE_SRC_SLAVE_RESAMPLE, "GST_AUDIO_BASE_SRC_SLAVE_RESAMPLE", "resample" },
      { GST_AUDIO_BASE_SRC_SLAVE_RE_TIMESTAMP, "GST_AUDIO_BASE_SRC_SLAVE_RE_TIMESTAMP", "re-timestamp" },
      { GST_AUDIO_BASE_SRC_SLAVE_SKEW, "GST_AUDIO_BASE_SRC_SLAVE_SKEW", "skew" },
      { GST_AUDIO_BASE_SRC_SLAVE_NONE, "GST_AUDIO_BASE_SRC_SLAVE_NONE", "none" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioBaseSrcSlaveMethod", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

#ifndef GSTREAMER_LITE
/* enumerations from "gstaudiocdsrc.h" */
GType
gst_audio_cd_src_mode_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_CD_SRC_MODE_NORMAL, "GST_AUDIO_CD_SRC_MODE_NORMAL", "normal" },
      { GST_AUDIO_CD_SRC_MODE_CONTINUOUS, "GST_AUDIO_CD_SRC_MODE_CONTINUOUS", "continuous" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioCdSrcMode", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
#endif // GSTREAMER_LITE

/* enumerations from "gstaudioringbuffer.h" */
GType
gst_audio_ring_buffer_state_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_RING_BUFFER_STATE_STOPPED, "GST_AUDIO_RING_BUFFER_STATE_STOPPED", "stopped" },
      { GST_AUDIO_RING_BUFFER_STATE_PAUSED, "GST_AUDIO_RING_BUFFER_STATE_PAUSED", "paused" },
      { GST_AUDIO_RING_BUFFER_STATE_STARTED, "GST_AUDIO_RING_BUFFER_STATE_STARTED", "started" },
      { GST_AUDIO_RING_BUFFER_STATE_ERROR, "GST_AUDIO_RING_BUFFER_STATE_ERROR", "error" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioRingBufferState", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
GType
gst_audio_ring_buffer_format_type_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_RAW, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_RAW", "raw" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_MU_LAW, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_MU_LAW", "mu-law" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_A_LAW, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_A_LAW", "a-law" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_IMA_ADPCM, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_IMA_ADPCM", "ima-adpcm" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_MPEG, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_MPEG", "mpeg" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_GSM, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_GSM", "gsm" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_IEC958, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_IEC958", "iec958" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_AC3, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_AC3", "ac3" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_EAC3, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_EAC3", "eac3" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_DTS, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_DTS", "dts" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_MPEG2_AAC, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_MPEG2_AAC", "mpeg2-aac" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_MPEG4_AAC, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_MPEG4_AAC", "mpeg4-aac" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_MPEG2_AAC_RAW, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_MPEG2_AAC_RAW", "mpeg2-aac-raw" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_MPEG4_AAC_RAW, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_MPEG4_AAC_RAW", "mpeg4-aac-raw" },
      { GST_AUDIO_RING_BUFFER_FORMAT_TYPE_FLAC, "GST_AUDIO_RING_BUFFER_FORMAT_TYPE_FLAC", "flac" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioRingBufferFormatType", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}



