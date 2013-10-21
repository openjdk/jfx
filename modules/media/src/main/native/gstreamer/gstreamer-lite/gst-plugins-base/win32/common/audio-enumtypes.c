


#include "audio-enumtypes.h"

#include "multichannel.h"
#include "gstringbuffer.h"

/* enumerations from "multichannel.h" */
GType
gst_audio_channel_position_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_AUDIO_CHANNEL_POSITION_INVALID, "GST_AUDIO_CHANNEL_POSITION_INVALID",
          "invalid"},
      {GST_AUDIO_CHANNEL_POSITION_FRONT_MONO,
          "GST_AUDIO_CHANNEL_POSITION_FRONT_MONO", "front-mono"},
      {GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,
          "GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT", "front-left"},
      {GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT,
          "GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT", "front-right"},
      {GST_AUDIO_CHANNEL_POSITION_REAR_CENTER,
          "GST_AUDIO_CHANNEL_POSITION_REAR_CENTER", "rear-center"},
      {GST_AUDIO_CHANNEL_POSITION_REAR_LEFT,
          "GST_AUDIO_CHANNEL_POSITION_REAR_LEFT", "rear-left"},
      {GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT,
          "GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT", "rear-right"},
      {GST_AUDIO_CHANNEL_POSITION_LFE, "GST_AUDIO_CHANNEL_POSITION_LFE", "lfe"},
      {GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER,
          "GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER", "front-center"},
      {GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT_OF_CENTER,
            "GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT_OF_CENTER",
          "front-left-of-center"},
      {GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT_OF_CENTER,
            "GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT_OF_CENTER",
          "front-right-of-center"},
      {GST_AUDIO_CHANNEL_POSITION_SIDE_LEFT,
          "GST_AUDIO_CHANNEL_POSITION_SIDE_LEFT", "side-left"},
      {GST_AUDIO_CHANNEL_POSITION_SIDE_RIGHT,
          "GST_AUDIO_CHANNEL_POSITION_SIDE_RIGHT", "side-right"},
      {GST_AUDIO_CHANNEL_POSITION_NONE, "GST_AUDIO_CHANNEL_POSITION_NONE",
          "none"},
      {GST_AUDIO_CHANNEL_POSITION_NUM, "GST_AUDIO_CHANNEL_POSITION_NUM", "num"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstAudioChannelPosition", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "gstringbuffer.h" */
GType
gst_ring_buffer_state_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_RING_BUFFER_STATE_STOPPED, "GST_RING_BUFFER_STATE_STOPPED",
          "stopped"},
      {GST_RING_BUFFER_STATE_PAUSED, "GST_RING_BUFFER_STATE_PAUSED", "paused"},
      {GST_RING_BUFFER_STATE_STARTED, "GST_RING_BUFFER_STATE_STARTED",
          "started"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstRingBufferState", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_ring_buffer_seg_state_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_SEGSTATE_INVALID, "GST_SEGSTATE_INVALID", "invalid"},
      {GST_SEGSTATE_EMPTY, "GST_SEGSTATE_EMPTY", "empty"},
      {GST_SEGSTATE_FILLED, "GST_SEGSTATE_FILLED", "filled"},
      {GST_SEGSTATE_PARTIAL, "GST_SEGSTATE_PARTIAL", "partial"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstRingBufferSegState", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_buffer_format_type_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_BUFTYPE_LINEAR, "GST_BUFTYPE_LINEAR", "linear"},
      {GST_BUFTYPE_FLOAT, "GST_BUFTYPE_FLOAT", "float"},
      {GST_BUFTYPE_MU_LAW, "GST_BUFTYPE_MU_LAW", "mu-law"},
      {GST_BUFTYPE_A_LAW, "GST_BUFTYPE_A_LAW", "a-law"},
      {GST_BUFTYPE_IMA_ADPCM, "GST_BUFTYPE_IMA_ADPCM", "ima-adpcm"},
      {GST_BUFTYPE_MPEG, "GST_BUFTYPE_MPEG", "mpeg"},
      {GST_BUFTYPE_GSM, "GST_BUFTYPE_GSM", "gsm"},
      {GST_BUFTYPE_IEC958, "GST_BUFTYPE_IEC958", "iec958"},
      {GST_BUFTYPE_AC3, "GST_BUFTYPE_AC3", "ac3"},
      {GST_BUFTYPE_EAC3, "GST_BUFTYPE_EAC3", "eac3"},
      {GST_BUFTYPE_DTS, "GST_BUFTYPE_DTS", "dts"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstBufferFormatType", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_buffer_format_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_UNKNOWN, "GST_UNKNOWN", "unknown"},
      {GST_S8, "GST_S8", "s8"},
      {GST_U8, "GST_U8", "u8"},
      {GST_S16_LE, "GST_S16_LE", "s16-le"},
      {GST_S16_BE, "GST_S16_BE", "s16-be"},
      {GST_U16_LE, "GST_U16_LE", "u16-le"},
      {GST_U16_BE, "GST_U16_BE", "u16-be"},
      {GST_S24_LE, "GST_S24_LE", "s24-le"},
      {GST_S24_BE, "GST_S24_BE", "s24-be"},
      {GST_U24_LE, "GST_U24_LE", "u24-le"},
      {GST_U24_BE, "GST_U24_BE", "u24-be"},
      {GST_S32_LE, "GST_S32_LE", "s32-le"},
      {GST_S32_BE, "GST_S32_BE", "s32-be"},
      {GST_U32_LE, "GST_U32_LE", "u32-le"},
      {GST_U32_BE, "GST_U32_BE", "u32-be"},
      {GST_S24_3LE, "GST_S24_3LE", "s24-3le"},
      {GST_S24_3BE, "GST_S24_3BE", "s24-3be"},
      {GST_U24_3LE, "GST_U24_3LE", "u24-3le"},
      {GST_U24_3BE, "GST_U24_3BE", "u24-3be"},
      {GST_S20_3LE, "GST_S20_3LE", "s20-3le"},
      {GST_S20_3BE, "GST_S20_3BE", "s20-3be"},
      {GST_U20_3LE, "GST_U20_3LE", "u20-3le"},
      {GST_U20_3BE, "GST_U20_3BE", "u20-3be"},
      {GST_S18_3LE, "GST_S18_3LE", "s18-3le"},
      {GST_S18_3BE, "GST_S18_3BE", "s18-3be"},
      {GST_U18_3LE, "GST_U18_3LE", "u18-3le"},
      {GST_U18_3BE, "GST_U18_3BE", "u18-3be"},
      {GST_FLOAT32_LE, "GST_FLOAT32_LE", "float32-le"},
      {GST_FLOAT32_BE, "GST_FLOAT32_BE", "float32-be"},
      {GST_FLOAT64_LE, "GST_FLOAT64_LE", "float64-le"},
      {GST_FLOAT64_BE, "GST_FLOAT64_BE", "float64-be"},
      {GST_MU_LAW, "GST_MU_LAW", "mu-law"},
      {GST_A_LAW, "GST_A_LAW", "a-law"},
      {GST_IMA_ADPCM, "GST_IMA_ADPCM", "ima-adpcm"},
      {GST_MPEG, "GST_MPEG", "mpeg"},
      {GST_GSM, "GST_GSM", "gsm"},
      {GST_IEC958, "GST_IEC958", "iec958"},
      {GST_AC3, "GST_AC3", "ac3"},
      {GST_EAC3, "GST_EAC3", "eac3"},
      {GST_DTS, "GST_DTS", "dts"},
      {0, NULL, NULL}
    };
    GType g_define_type_id = g_enum_register_static ("GstBufferFormat", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
