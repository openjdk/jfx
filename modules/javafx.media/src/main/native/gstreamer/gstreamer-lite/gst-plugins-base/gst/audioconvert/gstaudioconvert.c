/* GStreamer
 * Copyright (C) 2003 Benjamin Otte <in7y118@public.uni-hamburg.de>
 * Copyright (C) 2005 Thomas Vander Stichele <thomas at apestaart dot org>
 * Copyright (C) 2011 Wim Taymans <wim.taymans at gmail dot com>
 *
 * gstaudioconvert.c: Convert audio to different audio formats automatically
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

/**
 * SECTION:element-audioconvert
 * @title: audioconvert
 *
 * Audioconvert converts raw audio buffers between various possible formats.
 * It supports integer to float conversion, width/depth conversion,
 * signedness and endianness conversion and channel transformations
 * (ie. upmixing and downmixing), as well as dithering and noise-shaping.
 *
 * ## Example launch line
 * |[
 * gst-launch-1.0 -v -m audiotestsrc ! audioconvert ! audio/x-raw,format=S8,channels=2 ! level ! fakesink silent=TRUE
 * ]|
 *  This pipeline converts audio to 8-bit.  The level element shows that
 * the output levels still match the one for a sine wave.
 * |[
 * gst-launch-1.0 -v -m uridecodebin uri=file:///path/to/audio.flac ! audioconvert ! vorbisenc ! oggmux ! filesink location=audio.ogg
 * ]|
 *  The vorbis encoder takes float audio data instead of the integer data
 * output by most other audio elements. This pipeline decodes a FLAC audio file
 * (or any other audio file for which decoders are installed) and re-encodes
 * it into an Ogg/Vorbis audio file.
 *
 * A mix matrix can be passed to audioconvert, that will govern the
 * remapping of input to output channels.
 * This is required if the input channels are unpositioned and no standard layout can be determined.
 * If an empty mix matrix is specified, a (potentially truncated) identity matrix will be generated.
 *
 * ## Example matrix generation code
 * To generate the matrix using code:
 *
 * |[
 * GValue v = G_VALUE_INIT;
 * GValue v2 = G_VALUE_INIT;
 * GValue v3 = G_VALUE_INIT;
 *
 * g_value_init (&v2, GST_TYPE_ARRAY);
 * g_value_init (&v3, G_TYPE_FLOAT);
 * g_value_set_float (&v3, 1);
 * gst_value_array_append_value (&v2, &v3);
 * g_value_unset (&v3);
 * [ Repeat for as many float as your input channels - unset and reinit v3 ]
 * g_value_init (&v, GST_TYPE_ARRAY);
 * gst_value_array_append_value (&v, &v2);
 * g_value_unset (&v2);
 * [ Repeat for as many v2's as your output channels - unset and reinit v2]
 * g_object_set_property (G_OBJECT (audioconvert), "mix-matrix", &v);
 * g_value_unset (&v);
 * ]|
 *
 * The mix matrix can also be passed through a custom upstream event:
 *
 * |[
 * GstStructure *s = gst_structure_new("GstRequestAudioMixMatrix", "matrix", GST_TYPE_ARRAY, &v, NULL);
 * GstEvent *event = gst_event_new_custom (GST_EVENT_CUSTOM_UPSTREAM, s);
 * GstPad *srcpad = gst_element_get_static_pad(audioconvert, "src");
 * gst_pad_send_event (srcpad, event);
 * gst_object_unref (pad);
 * ]|
 *
 * ## Example launch line
 * |[
 * gst-launch-1.0 audiotestsrc ! audio/x-raw, channels=4 ! audioconvert mix-matrix="<<(float)1.0, (float)0.0, (float)0.0, (float)0.0>, <(float)0.0, (float)1.0, (float)0.0, (float)0.0>>" ! audio/x-raw,channels=2 ! autoaudiosink
 * ]|
 *
 *
 * ## Example empty matrix generation code
 * |[
 * GValue v = G_VALUE_INIT;
 *
 * g_value_init (&v, GST_TYPE_ARRAY);
 * g_object_set_property (G_OBJECT (audioconvert), "mix-matrix", &v);
 * g_value_unset (&v);
 * ]|
 *
 * ## Example empty matrix launch line
 * |[
 * gst-launch-1.0 -v audiotestsrc ! audio/x-raw,channels=8 ! audioconvert mix-matrix="<>" ! audio/x-raw,channels=16,channel-mask=\(bitmask\)0x0000000000000000 ! fakesink
 * ]|
 *
 * If input channels are unpositioned but follow a standard layout, they can be
 * automatically positioned according to their index using one of the reorder
 * configurations.
 *
 * ## Example with unpositioned input channels reordering
 * |[
 * gst-launch-1.0 -v audiotestsrc ! audio/x-raw,channels=6,channel-mask=\(bitmask\)0x0000000000000000 ! audioconvert input-channels-reorder-mode=unpositioned input-channels-reorder=smpte ! fakesink
 * ]|
 *  In this case the input channels will be automatically positioned to the
 * SMPTE order (left, right, center, lfe, rear-left and rear-right).
 *
 * The input channels reorder configurations can also be used to force the
 * repositioning of the input channels when needed, for example when channels'
 * positions are not correctly identified in an encoded file.
 *
 * ## Example with the forced reordering of input channels wrongly positioned
 * |[
 * gst-launch-1.0 -v audiotestsrc ! audio/x-raw,channels=3,channel-mask=\(bitmask\)0x0000000000000034 ! audioconvert input-channels-reorder-mode=force input-channels-reorder=aac ! fakesink
 * ]|
 *  In this case the input channels are positioned upstream as center,
 * rear-left and rear-right in this order. Using the "force" reorder mode and
 * the "aac" order, the input channels are going to be repositioned to left,
 * right and lfe, ignoring the actual value of the `channel-mask` in the input
 * caps.
 */

/*
 * design decisions:
 * - audioconvert converts buffers in a set of supported caps. If it supports
 *   a caps, it supports conversion from these caps to any other caps it
 *   supports. (example: if it does A=>B and A=>C, it also does B=>C)
 * - audioconvert does not save state between buffers. Every incoming buffer is
 *   converted and the converted buffer is pushed out.
 * conclusion:
 * audioconvert is not supposed to be a one-element-does-anything solution for
 * audio conversions.
 */

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <string.h>

#include "gstaudioconvert.h"

GST_DEBUG_CATEGORY (audio_convert_debug);
GST_DEBUG_CATEGORY_STATIC (GST_CAT_PERFORMANCE);
#define GST_CAT_DEFAULT (audio_convert_debug)

/*** DEFINITIONS **************************************************************/

/* type functions */
static void gst_audio_convert_dispose (GObject * obj);

/* gstreamer functions */
static gboolean gst_audio_convert_get_unit_size (GstBaseTransform * base,
    GstCaps * caps, gsize * size);
static GstCaps *gst_audio_convert_transform_caps (GstBaseTransform * base,
    GstPadDirection direction, GstCaps * caps, GstCaps * filter);
static GstCaps *gst_audio_convert_fixate_caps (GstBaseTransform * base,
    GstPadDirection direction, GstCaps * caps, GstCaps * othercaps);
static gboolean gst_audio_convert_set_caps (GstBaseTransform * base,
    GstCaps * incaps, GstCaps * outcaps);
static GstFlowReturn gst_audio_convert_transform (GstBaseTransform * base,
    GstBuffer * inbuf, GstBuffer * outbuf);
static GstFlowReturn gst_audio_convert_transform_ip (GstBaseTransform * base,
    GstBuffer * buf);
static gboolean gst_audio_convert_transform_meta (GstBaseTransform * trans,
    GstBuffer * outbuf, GstMeta * meta, GstBuffer * inbuf);
static GstFlowReturn gst_audio_convert_submit_input_buffer (GstBaseTransform *
    base, gboolean is_discont, GstBuffer * input);
static GstFlowReturn gst_audio_convert_prepare_output_buffer (GstBaseTransform *
    base, GstBuffer * inbuf, GstBuffer ** outbuf);
static void gst_audio_convert_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec);
static void gst_audio_convert_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec);

/* AudioConvert signals and args */
enum
{
  /* FILL ME */
  LAST_SIGNAL
};

enum
{
  PROP_0,
  PROP_DITHERING,
  PROP_NOISE_SHAPING,
  PROP_MIX_MATRIX,
  PROP_DITHERING_THRESHOLD,
  PROP_INPUT_CHANNELS_REORDER,
  PROP_INPUT_CHANNELS_REORDER_MODE
};

#define DEBUG_INIT \
  GST_DEBUG_CATEGORY_INIT (audio_convert_debug, "audioconvert", 0, "audio conversion element"); \
  GST_DEBUG_CATEGORY_GET (GST_CAT_PERFORMANCE, "GST_PERFORMANCE");
#define gst_audio_convert_parent_class parent_class
G_DEFINE_TYPE_WITH_CODE (GstAudioConvert, gst_audio_convert,
    GST_TYPE_BASE_TRANSFORM, DEBUG_INIT);
GST_ELEMENT_REGISTER_DEFINE (audioconvert, "audioconvert",
    GST_RANK_PRIMARY, GST_TYPE_AUDIO_CONVERT);
/*** GSTREAMER PROTOTYPES *****************************************************/

#define STATIC_CAPS \
GST_STATIC_CAPS (GST_AUDIO_CAPS_MAKE (GST_AUDIO_FORMATS_ALL) \
    ", layout = (string) { interleaved, non-interleaved }")

static GstStaticPadTemplate gst_audio_convert_src_template =
GST_STATIC_PAD_TEMPLATE ("src",
    GST_PAD_SRC,
    GST_PAD_ALWAYS,
    STATIC_CAPS);

static GstStaticPadTemplate gst_audio_convert_sink_template =
GST_STATIC_PAD_TEMPLATE ("sink",
    GST_PAD_SINK,
    GST_PAD_ALWAYS,
    STATIC_CAPS);

/* cached quark to avoid contention on the global quark table lock */
#define META_TAG_AUDIO meta_tag_audio_quark
static GQuark meta_tag_audio_quark;

/*** TYPE FUNCTIONS ***********************************************************/

#define GST_TYPE_AUDIO_CONVERT_INPUT_CHANNELS_REORDER (gst_audio_convert_input_channels_reorder_get_type ())

static GType
gst_audio_convert_input_channels_reorder_get_type (void)
{
  static GType reorder_type = 0;

  if (g_once_init_enter (&reorder_type)) {
    static GEnumValue reorder_types[] = {
      {GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_GST,
            "Reorder the input channels using the default GStreamer order",
          "gst"},
      {GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_SMPTE,
            "Reorder the input channels using the SMPTE order",
          "smpte"},
      {GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_CINE,
            "Reorder the input channels using the CINE order",
          "cine"},
      {GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_AC3,
            "Reorder the input channels using the AC3 order",
          "ac3"},
      {GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_AAC,
            "Reorder the input channels using the AAC order",
          "aac"},
      {GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MONO,
            "Reorder and mix all input channels to a single mono channel",
          "mono"},
      {GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_ALTERNATE,
            "Reorder and mix all input channels to a single left and a single right stereo channels alternately",
          "alternate"},
      {0, NULL, NULL},
    };

    GType type = g_enum_register_static ("GstAudioConvertInputChannelsReorder",
        reorder_types);

    g_once_init_leave (&reorder_type, type);
  }

  return reorder_type;
}

#define GST_TYPE_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE (gst_audio_convert_input_channels_reorder_mode_get_type ())

static GType
gst_audio_convert_input_channels_reorder_mode_get_type (void)
{
  static GType reorder_mode_type = 0;

  if (g_once_init_enter (&reorder_mode_type)) {
    static GEnumValue reorder_mode_types[] = {
      {GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_NONE,
            "Never reorder the input channels",
          "none"},
      {GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_UNPOSITIONED,
            "Reorder the input channels only if they are unpositioned",
          "unpositioned"},
      {GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_FORCE,
            "Always reorder the input channels according to the selected configuration",
          "force"},
      {0, NULL, NULL},
    };

    GType type =
        g_enum_register_static ("GstAudioConvertInputChannelsReorderMode",
        reorder_mode_types);

    g_once_init_leave (&reorder_mode_type, type);
  }

  return reorder_mode_type;
}

static void
gst_audio_convert_set_mix_matrix (GstAudioConvert * this, const GValue * value);

static gboolean
gst_audio_convert_src_event (GstBaseTransform * trans, GstEvent * event)
{
  gboolean ret = TRUE;

  switch (GST_EVENT_TYPE (event)) {
    case GST_EVENT_CUSTOM_UPSTREAM:
    {
      const GstStructure *s = gst_event_get_structure (event);

      if (s && gst_structure_has_name (s, "GstRequestAudioMixMatrix")) {
        const GValue *matrix = gst_structure_get_value (s, "matrix");

        if (matrix) {
          gst_audio_convert_set_mix_matrix (GST_AUDIO_CONVERT (trans), matrix);
          g_object_notify (G_OBJECT (trans), "mix-matrix");
        }
        goto done;
      }
      break;
    }
    default:
      break;
  }
  ret = GST_BASE_TRANSFORM_CLASS (parent_class)->src_event (trans, event);

done:
  return ret;
}

static void
gst_audio_convert_class_init (GstAudioConvertClass * klass)
{
  GObjectClass *gobject_class = G_OBJECT_CLASS (klass);
  GstElementClass *element_class = GST_ELEMENT_CLASS (klass);
  GstBaseTransformClass *basetransform_class = GST_BASE_TRANSFORM_CLASS (klass);

  gobject_class->dispose = gst_audio_convert_dispose;
  gobject_class->set_property = gst_audio_convert_set_property;
  gobject_class->get_property = gst_audio_convert_get_property;

  g_object_class_install_property (gobject_class, PROP_DITHERING,
      g_param_spec_enum ("dithering", "Dithering",
          "Selects between different dithering methods.",
          GST_TYPE_AUDIO_DITHER_METHOD, GST_AUDIO_DITHER_TPDF,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  g_object_class_install_property (gobject_class, PROP_NOISE_SHAPING,
      g_param_spec_enum ("noise-shaping", "Noise shaping",
          "Selects between different noise shaping methods.",
          GST_TYPE_AUDIO_NOISE_SHAPING_METHOD, GST_AUDIO_NOISE_SHAPING_NONE,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstAudioConvert:mix-matrix:
   *
   * Transformation matrix for input/output channels.
   * Required if the input channels are unpositioned and no standard layout can be determined.
   * Setting an empty matrix like \"< >\" will generate an identity matrix."
   *
   */
  g_object_class_install_property (gobject_class, PROP_MIX_MATRIX,
      gst_param_spec_array ("mix-matrix",
          "Input/output channel matrix",
          "Transformation matrix for input/output channels.",
          gst_param_spec_array ("matrix-rows", "rows", "rows",
              g_param_spec_float ("matrix-cols", "cols", "cols",
                  -1, 1, 0,
                  G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS),
              G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS),
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstAudioConvert:dithering-threshold:
   *
   * Threshold for the output bit depth at/below which to apply dithering.
   *
   * Since: 1.22
   */
  g_object_class_install_property (gobject_class, PROP_DITHERING_THRESHOLD,
      g_param_spec_uint ("dithering-threshold", "Dithering Threshold",
          "Threshold for the output bit depth at/below which to apply dithering.",
          0, 32, 20, G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));

  /**
   * GstAudioConvert:input-channels-reorder:
   *
   * The positions configuration to use to reorder the input channels
   * consecutively according to their index. If a `mix-matrix` is specified,
   * this configuration is ignored.
   *
   * When the input channels reordering is activated (because the
   * `input-channels-reorder-mode` property is
   * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_FORCE or the input channels
   * are unpositioned and the reorder mode is
   * @GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_UNPOSITIONED), input
   * channels will be reordered consecutively according to their index
   * independently of the `channel-mask` value in the sink pad audio caps.
   *
   * Since: 1.26
   */
  g_object_class_install_property (gobject_class,
      PROP_INPUT_CHANNELS_REORDER,
      g_param_spec_enum ("input-channels-reorder",
          "Input Channels Reorder",
          "The positions configuration to use to reorder the input channels consecutively according to their index.",
          GST_TYPE_AUDIO_CONVERT_INPUT_CHANNELS_REORDER,
          GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_GST,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  gst_type_mark_as_plugin_api (GST_TYPE_AUDIO_CONVERT_INPUT_CHANNELS_REORDER,
      0);

  /**
   * GstAudioConvert:input-channels-reorder-mode:
   *
   * The input channels reordering mode used to apply the selected positions
   * configuration.
   *
   * Since: 1.26
   */
  g_object_class_install_property (gobject_class,
      PROP_INPUT_CHANNELS_REORDER_MODE,
      g_param_spec_enum ("input-channels-reorder-mode",
          "Input Channels Reorder Mode",
          "The input channels reordering mode used to apply the selected positions configuration.",
          GST_TYPE_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE,
          GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_NONE,
          G_PARAM_READWRITE | G_PARAM_STATIC_STRINGS));
  gst_type_mark_as_plugin_api
      (GST_TYPE_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE, 0);

  gst_element_class_add_static_pad_template (element_class,
      &gst_audio_convert_src_template);
  gst_element_class_add_static_pad_template (element_class,
      &gst_audio_convert_sink_template);
  gst_element_class_set_static_metadata (element_class, "Audio converter",
      "Filter/Converter/Audio", "Convert audio to different formats",
      "Benjamin Otte <otte@gnome.org>");

  basetransform_class->get_unit_size =
      GST_DEBUG_FUNCPTR (gst_audio_convert_get_unit_size);
  basetransform_class->transform_caps =
      GST_DEBUG_FUNCPTR (gst_audio_convert_transform_caps);
  basetransform_class->fixate_caps =
      GST_DEBUG_FUNCPTR (gst_audio_convert_fixate_caps);
  basetransform_class->set_caps =
      GST_DEBUG_FUNCPTR (gst_audio_convert_set_caps);
  basetransform_class->transform =
      GST_DEBUG_FUNCPTR (gst_audio_convert_transform);
  basetransform_class->transform_ip =
      GST_DEBUG_FUNCPTR (gst_audio_convert_transform_ip);
  basetransform_class->transform_meta =
      GST_DEBUG_FUNCPTR (gst_audio_convert_transform_meta);
  basetransform_class->submit_input_buffer =
      GST_DEBUG_FUNCPTR (gst_audio_convert_submit_input_buffer);
  basetransform_class->prepare_output_buffer =
      GST_DEBUG_FUNCPTR (gst_audio_convert_prepare_output_buffer);
  basetransform_class->src_event =
      GST_DEBUG_FUNCPTR (gst_audio_convert_src_event);

  basetransform_class->transform_ip_on_passthrough = FALSE;

  meta_tag_audio_quark = g_quark_from_static_string (GST_META_TAG_AUDIO_STR);
}

static void
gst_audio_convert_init (GstAudioConvert * this)
{
  this->dither = GST_AUDIO_DITHER_TPDF;
  this->dither_threshold = 20;
  this->ns = GST_AUDIO_NOISE_SHAPING_NONE;
  g_value_init (&this->mix_matrix, GST_TYPE_ARRAY);

  gst_base_transform_set_gap_aware (GST_BASE_TRANSFORM (this), TRUE);
}

static void
gst_audio_convert_dispose (GObject * obj)
{
  GstAudioConvert *this = GST_AUDIO_CONVERT (obj);

  if (this->convert) {
    gst_audio_converter_free (this->convert);
    this->convert = NULL;
  }

  g_value_unset (&this->mix_matrix);

  G_OBJECT_CLASS (parent_class)->dispose (obj);
}

/*** INPUT CHANNELS REORDER FUNCTIONS *****************************************/

typedef struct
{
  gboolean has_stereo;
  gboolean lfe_as_last_channel;
} GstAudioConvertInputChannelsReorderConfig;

static const GstAudioConvertInputChannelsReorderConfig
    input_channels_reorder_config[] = {
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_GST
  {TRUE, FALSE},
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_SMPTE
  {TRUE, FALSE},
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_CINE
  {TRUE, TRUE},
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_AC3
  {TRUE, TRUE},
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_AAC
  {TRUE, TRUE},
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MONO
  {FALSE, FALSE},
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_ALTERNATE
  {TRUE, FALSE}
};

#define GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_NB G_N_ELEMENTS (input_channels_reorder_config)

static const GstAudioChannelPosition
    channel_position_per_reorder_config
    [GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_NB][64] = {
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_GST
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER,
        GST_AUDIO_CHANNEL_POSITION_LFE1,
        GST_AUDIO_CHANNEL_POSITION_REAR_LEFT,
        GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT_OF_CENTER,
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT_OF_CENTER,
        GST_AUDIO_CHANNEL_POSITION_REAR_CENTER,
        GST_AUDIO_CHANNEL_POSITION_LFE2,
        GST_AUDIO_CHANNEL_POSITION_SIDE_LEFT,
        GST_AUDIO_CHANNEL_POSITION_SIDE_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_LEFT,
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_CENTER,
        GST_AUDIO_CHANNEL_POSITION_TOP_CENTER,
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_LEFT,
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_LEFT,
        GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_CENTER,
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_CENTER,
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_LEFT,
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_WIDE_LEFT,
        GST_AUDIO_CHANNEL_POSITION_WIDE_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_SURROUND_LEFT,
        GST_AUDIO_CHANNEL_POSITION_SURROUND_RIGHT,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
      },
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_SMPTE (see: https://www.sis.se/api/document/preview/919377/)
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // Left front (L)
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // Right front (R)
        GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER,        // Center front (C)
        GST_AUDIO_CHANNEL_POSITION_LFE1,        // Low frequency enhancement (LFE)
        GST_AUDIO_CHANNEL_POSITION_REAR_LEFT,   // Left surround (Ls)
        GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT,  // Right surround (Rs)
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT_OF_CENTER,        // Left front center (Lc)
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT_OF_CENTER,       // Right front center (Rc)
        GST_AUDIO_CHANNEL_POSITION_SURROUND_LEFT,       // Rear surround left (Lsr)
        GST_AUDIO_CHANNEL_POSITION_SURROUND_RIGHT,      // Rear surround right (Rsr)
        GST_AUDIO_CHANNEL_POSITION_REAR_CENTER, // Rear center (Cs)
        GST_AUDIO_CHANNEL_POSITION_SIDE_LEFT,   // Left side surround (Lss)
        GST_AUDIO_CHANNEL_POSITION_SIDE_RIGHT,  // Right side surround (Rss)
        GST_AUDIO_CHANNEL_POSITION_WIDE_LEFT,   // Left wide front (Lw)
        GST_AUDIO_CHANNEL_POSITION_WIDE_RIGHT,  // Right wide front (Rw)
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_LEFT,      // Left front vertical height (Lv)
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_RIGHT,     // Right front vertical height (Rv)
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_CENTER,    // Center front vertical height (Cv)
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_LEFT,       // Left surround vertical height rear (Lvr)
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_RIGHT,      // Right surround vertical height rear (Rvr)
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_CENTER,     // Center vertical height rear (Cvr)
        GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_LEFT,       // Left vertical height side surround (Lvss)
        GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_RIGHT,      // Right vertical height side surround (Rvss)
        GST_AUDIO_CHANNEL_POSITION_TOP_CENTER,  // Top center surround (Ts)
        GST_AUDIO_CHANNEL_POSITION_LFE2,        // Low frequency enhancement 2 (LFE2)
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_LEFT,   // Left front vertical bottom (Lb)
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_RIGHT,  // Right front vertical bottom (Rb)
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_CENTER, // Center front vertical bottom (Cb)
        GST_AUDIO_CHANNEL_POSITION_INVALID,     // Left vertical height surround (Lvs)
        GST_AUDIO_CHANNEL_POSITION_INVALID,     // Right vertical height surround (Rvs)
        GST_AUDIO_CHANNEL_POSITION_INVALID,     // Reserved
        GST_AUDIO_CHANNEL_POSITION_INVALID,     // Reserved
        GST_AUDIO_CHANNEL_POSITION_INVALID,     // Reserved
        GST_AUDIO_CHANNEL_POSITION_INVALID,     // Reserved
        GST_AUDIO_CHANNEL_POSITION_INVALID,     // Low frequency enhancement 3 (LFE3)
        GST_AUDIO_CHANNEL_POSITION_INVALID,     // Left edge of screen (Leos)
        GST_AUDIO_CHANNEL_POSITION_INVALID,     // Right edge of screen (Reos)
        GST_AUDIO_CHANNEL_POSITION_INVALID,     // Half-way between center of screen and left edge of screen (Hwbcal)
        GST_AUDIO_CHANNEL_POSITION_INVALID,     // Half-way between center of screen and right edge of screen (Hwbcar)
        GST_AUDIO_CHANNEL_POSITION_INVALID,     // Left back surround (Lbs)
        GST_AUDIO_CHANNEL_POSITION_INVALID,     // Right back surround (Rbs)
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
      },
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_CINE
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER,        // C
        GST_AUDIO_CHANNEL_POSITION_REAR_LEFT,   // Ls
        GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT,  // Rs
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT_OF_CENTER,        // Lc
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT_OF_CENTER,       // Rc
        GST_AUDIO_CHANNEL_POSITION_SURROUND_LEFT,       // Lsr
        GST_AUDIO_CHANNEL_POSITION_SURROUND_RIGHT,      // Rsr
        GST_AUDIO_CHANNEL_POSITION_REAR_CENTER, // Cs
        GST_AUDIO_CHANNEL_POSITION_TOP_CENTER,  // Ts
        GST_AUDIO_CHANNEL_POSITION_WIDE_LEFT,   // Lw
        GST_AUDIO_CHANNEL_POSITION_WIDE_RIGHT,  // Rw
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_LEFT,      // Lv
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_RIGHT,     // Rv
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_CENTER,    // Cv
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_LEFT,       // Lvr
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_RIGHT,      // Rvr
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_CENTER,     // Cvr
        GST_AUDIO_CHANNEL_POSITION_SIDE_LEFT,   // Lss
        GST_AUDIO_CHANNEL_POSITION_SIDE_RIGHT,  // Rss
        GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_LEFT,       // Lvss
        GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_RIGHT,      // Rvss
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_LEFT,   // Lb
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_RIGHT,  // Rb
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_CENTER, // Cb
        GST_AUDIO_CHANNEL_POSITION_LFE2,        // LFE2
        GST_AUDIO_CHANNEL_POSITION_LFE1,        // LFE1
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
      },
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_AC3
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER,        // C
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_REAR_LEFT,   // Ls
        GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT,  // Rs
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT_OF_CENTER,        // Lc
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT_OF_CENTER,       // Rc
        GST_AUDIO_CHANNEL_POSITION_SURROUND_LEFT,       // Lsr
        GST_AUDIO_CHANNEL_POSITION_SURROUND_RIGHT,      // Rsr
        GST_AUDIO_CHANNEL_POSITION_REAR_CENTER, // Cs
        GST_AUDIO_CHANNEL_POSITION_TOP_CENTER,  // Ts
        GST_AUDIO_CHANNEL_POSITION_WIDE_LEFT,   // Lw
        GST_AUDIO_CHANNEL_POSITION_WIDE_RIGHT,  // Rw
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_LEFT,      // Lv
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_RIGHT,     // Rv
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_CENTER,    // Cv
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_LEFT,       // Lvr
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_RIGHT,      // Rvr
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_CENTER,     // Cvr
        GST_AUDIO_CHANNEL_POSITION_SIDE_LEFT,   // Lss
        GST_AUDIO_CHANNEL_POSITION_SIDE_RIGHT,  // Rss
        GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_LEFT,       // Lvss
        GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_RIGHT,      // Rvss
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_LEFT,   // Lb
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_RIGHT,  // Rb
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_CENTER, // Cb
        GST_AUDIO_CHANNEL_POSITION_LFE2,        // LFE2
        GST_AUDIO_CHANNEL_POSITION_LFE1,        // LFE1
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
      },
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_AAC
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_CENTER,        // C
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_REAR_LEFT,   // Ls
        GST_AUDIO_CHANNEL_POSITION_REAR_RIGHT,  // Rs
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT_OF_CENTER,        // Lc
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT_OF_CENTER,       // Rc
        GST_AUDIO_CHANNEL_POSITION_SURROUND_LEFT,       // Lsr
        GST_AUDIO_CHANNEL_POSITION_SURROUND_RIGHT,      // Rsr
        GST_AUDIO_CHANNEL_POSITION_REAR_CENTER, // Cs
        GST_AUDIO_CHANNEL_POSITION_TOP_CENTER,  // Ts
        GST_AUDIO_CHANNEL_POSITION_WIDE_LEFT,   // Lw
        GST_AUDIO_CHANNEL_POSITION_WIDE_RIGHT,  // Rw
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_LEFT,      // Lv
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_RIGHT,     // Rv
        GST_AUDIO_CHANNEL_POSITION_TOP_FRONT_CENTER,    // Cv
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_LEFT,       // Lvr
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_RIGHT,      // Rvr
        GST_AUDIO_CHANNEL_POSITION_TOP_REAR_CENTER,     // Cvr
        GST_AUDIO_CHANNEL_POSITION_SIDE_LEFT,   // Lss
        GST_AUDIO_CHANNEL_POSITION_SIDE_RIGHT,  // Rss
        GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_LEFT,       // Lvss
        GST_AUDIO_CHANNEL_POSITION_TOP_SIDE_RIGHT,      // Rvss
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_LEFT,   // Lb
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_RIGHT,  // Rb
        GST_AUDIO_CHANNEL_POSITION_BOTTOM_FRONT_CENTER, // Cb
        GST_AUDIO_CHANNEL_POSITION_LFE2,        // LFE2
        GST_AUDIO_CHANNEL_POSITION_LFE1,        // LFE1
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
        GST_AUDIO_CHANNEL_POSITION_INVALID,
      },
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MONO
  {
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
        GST_AUDIO_CHANNEL_POSITION_MONO,
      },
  // GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_ALTERNATE
  {
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
        GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT,  // L
        GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT, // R
      }
};

static const gchar *gst_audio_convert_input_channels_reorder_to_string
    (GstAudioConvertInputChannelsReorder reorder)
{
  switch (reorder) {
    case GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_GST:
      return "GST";
    case GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_SMPTE:
      return "SMPTE";
    case GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_CINE:
      return "CINE";
    case GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_AC3:
      return "AC3";
    case GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_AAC:
      return "AAC";
    case GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MONO:
      return "MONO";
    case GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_ALTERNATE:
      return "ALTERNATE";
    default:
      return "UNKNOWN";
  }
}

static gboolean
gst_audio_convert_position_channels_from_reorder_configuration (gint channels,
    GstAudioConvertInputChannelsReorder reorder,
    GstAudioChannelPosition * position)
{
  g_return_val_if_fail (channels > 0, FALSE);
  g_return_val_if_fail (reorder >= 0
      && reorder < GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_NB, FALSE);
  g_return_val_if_fail (position != NULL, FALSE);

  GST_DEBUG ("ordering %d audio channel(s) according to the %s configuration",
      channels, gst_audio_convert_input_channels_reorder_to_string (reorder));

  if (channels == 1) {
    position[0] = GST_AUDIO_CHANNEL_POSITION_MONO;
    return TRUE;
  }

  if (channels == 2 && input_channels_reorder_config[reorder].has_stereo) {
    position[0] = GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT;
    position[1] = GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT;
    return TRUE;
  }

  for (gint i = 0; i < channels; ++i) {
    if (i < G_N_ELEMENTS (channel_position_per_reorder_config[reorder]))
      position[i] = channel_position_per_reorder_config[reorder][i];
    else
      position[i] = GST_AUDIO_CHANNEL_POSITION_INVALID;
  }

  if (channels > 2
      && input_channels_reorder_config[reorder].lfe_as_last_channel) {
    position[channels - 1] = GST_AUDIO_CHANNEL_POSITION_LFE1;
    if (channels == 3 && input_channels_reorder_config[reorder].has_stereo) {
      position[0] = GST_AUDIO_CHANNEL_POSITION_FRONT_LEFT;
      position[1] = GST_AUDIO_CHANNEL_POSITION_FRONT_RIGHT;
    }
  }

  return TRUE;
}

/*** GSTREAMER FUNCTIONS ******************************************************/

/* BaseTransform vmethods */
static gboolean
gst_audio_convert_get_unit_size (GstBaseTransform * base, GstCaps * caps,
    gsize * size)
{
  GstAudioInfo info;

  g_assert (size);

  if (!gst_audio_info_from_caps (&info, caps))
    goto parse_error;

  *size = info.bpf;
  GST_DEBUG_OBJECT (base, "unit_size = %" G_GSIZE_FORMAT, *size);

  return TRUE;

parse_error:
  {
    GST_WARNING_OBJECT (base, "failed to parse caps to get unit_size");
    return FALSE;
  }
}

static gboolean
remove_format_from_structure (GstCapsFeatures * features,
    GstStructure * structure, gpointer user_data G_GNUC_UNUSED)
{
  gst_structure_remove_field (structure, "format");
  return TRUE;
}

static gboolean
remove_layout_from_structure (GstCapsFeatures * features,
    GstStructure * structure, gpointer user_data G_GNUC_UNUSED)
{
  gst_structure_remove_field (structure, "layout");
  return TRUE;
}

static gboolean
remove_channels_from_structure (GstCapsFeatures * features, GstStructure * s,
    gpointer user_data)
{
  guint64 mask;
  gint channels;
  gboolean force_removing = *(gboolean *) user_data;

  /* Only remove the channels and channel-mask if a mix matrix was manually
   * specified or an input channels reordering is applied, or if no
   * channel-mask is specified, for non-NONE channel layouts or for a single
   * channel layout.
   */
  if (force_removing ||
      !gst_structure_get (s, "channel-mask", GST_TYPE_BITMASK, &mask, NULL) ||
      (mask != 0 || (gst_structure_get_int (s, "channels", &channels)
              && channels == 1))) {
    gst_structure_remove_fields (s, "channel-mask", "channels", NULL);
  }

  return TRUE;
}

static gboolean
add_other_channels_to_structure (GstCapsFeatures * features, GstStructure * s,
    gpointer user_data)
{
  gint other_channels = GPOINTER_TO_INT (user_data);

  gst_structure_set_static_str (s, "channels", G_TYPE_INT, other_channels,
      NULL);

  return TRUE;
}

/* The caps can be transformed into any other caps with format info removed.
 * However, we should prefer passthrough, so if passthrough is possible,
 * put it first in the list. */
static GstCaps *
gst_audio_convert_transform_caps (GstBaseTransform * btrans,
    GstPadDirection direction, GstCaps * caps, GstCaps * filter)
{
  GstCaps *tmp, *tmp2;
  GstCaps *result;
  GstAudioConvert *this = GST_AUDIO_CONVERT (btrans);

  tmp = gst_caps_copy (caps);

  gst_caps_map_in_place (tmp, remove_format_from_structure, NULL);
  gst_caps_map_in_place (tmp, remove_layout_from_structure, NULL);

  GST_OBJECT_LOCK (this);
  gboolean force_removing = this->mix_matrix_is_set
      || (direction == GST_PAD_SINK
      && this->input_channels_reorder_mode !=
      GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_NONE);
  gst_caps_map_in_place (tmp, remove_channels_from_structure, &force_removing);

  /* We can infer the required input / output channels based on the
   * matrix dimensions */
  if (gst_value_array_get_size (&this->mix_matrix)) {
    gint other_channels;

    if (direction == GST_PAD_SRC) {
      const GValue *first_row =
          gst_value_array_get_value (&this->mix_matrix, 0);
      other_channels = gst_value_array_get_size (first_row);
    } else {
      other_channels = gst_value_array_get_size (&this->mix_matrix);
    }

    gst_caps_map_in_place (tmp, add_other_channels_to_structure,
        GINT_TO_POINTER (other_channels));
  }
  GST_OBJECT_UNLOCK (this);

  if (filter) {
    tmp2 = gst_caps_intersect_full (filter, tmp, GST_CAPS_INTERSECT_FIRST);
    gst_caps_unref (tmp);
    tmp = tmp2;
  }

  result = tmp;

  GST_DEBUG_OBJECT (btrans, "transformed %" GST_PTR_FORMAT " into %"
      GST_PTR_FORMAT, caps, result);

  return result;
}

/* Count the number of bits set
 * Optimized for the common case, assuming that the number of channels
 * (i.e. bits set) is small
 */
static gint
n_bits_set (guint64 x)
{
  gint c;

  for (c = 0; x; c++)
    x &= x - 1;

  return c;
}

/* Reduce the mask to the n_chans lowest set bits
 *
 * The algorithm clears the n_chans lowest set bits and subtracts the
 * result from the original mask to get the desired mask.
 * It is optimized for the common case where n_chans is a small
 * number. In the worst case, however, it stops after 64 iterations.
 */
static guint64
find_suitable_mask (guint64 mask, gint n_chans)
{
  guint64 x = mask;

  for (; x && n_chans; n_chans--)
    x &= x - 1;

  g_assert (x || n_chans == 0);
  /* assertion fails if mask contained less bits than n_chans
   * or n_chans was < 0 */

  return mask - x;
}

static void
gst_audio_convert_fixate_format (GstBaseTransform * base, GstStructure * ins,
    GstStructure * outs)
{
  const gchar *in_format;
  const GValue *format;
  const GstAudioFormatInfo *in_info, *out_info = NULL;
  GstAudioFormatFlags in_flags, out_flags = 0;
  gint in_depth, out_depth = -1;
  gint i, len;

  in_format = gst_structure_get_string (ins, "format");
  if (!in_format)
    return;

  format = gst_structure_get_value (outs, "format");
  /* should not happen */
  if (format == NULL)
    return;

  /* nothing to fixate? */
  if (!GST_VALUE_HOLDS_LIST (format))
    return;

  in_info =
      gst_audio_format_get_info (gst_audio_format_from_string (in_format));
  if (!in_info)
    return;

  in_flags = GST_AUDIO_FORMAT_INFO_FLAGS (in_info);
  in_flags &= ~(GST_AUDIO_FORMAT_FLAG_UNPACK);
  in_flags &= ~(GST_AUDIO_FORMAT_FLAG_SIGNED);

  in_depth = GST_AUDIO_FORMAT_INFO_DEPTH (in_info);

  len = gst_value_list_get_size (format);
  for (i = 0; i < len; i++) {
    const GstAudioFormatInfo *t_info;
    GstAudioFormatFlags t_flags;
    gboolean t_flags_better;
    const GValue *val;
    const gchar *fname;
    gint t_depth;

    val = gst_value_list_get_value (format, i);
    if (!G_VALUE_HOLDS_STRING (val))
      continue;

    fname = g_value_get_string (val);
    t_info = gst_audio_format_get_info (gst_audio_format_from_string (fname));
    if (!t_info)
      continue;

    /* accept input format immediately */
    if (strcmp (fname, in_format) == 0) {
      out_info = t_info;
      break;
    }

    t_flags = GST_AUDIO_FORMAT_INFO_FLAGS (t_info);
    t_flags &= ~(GST_AUDIO_FORMAT_FLAG_UNPACK);
    t_flags &= ~(GST_AUDIO_FORMAT_FLAG_SIGNED);

    t_depth = GST_AUDIO_FORMAT_INFO_DEPTH (t_info);

    /* Any output format is better than no output format at all */
    if (!out_info) {
      out_info = t_info;
      out_depth = t_depth;
      out_flags = t_flags;
      continue;
    }

    t_flags_better = (t_flags == in_flags && out_flags != in_flags);

    if (t_depth == in_depth && (out_depth != in_depth || t_flags_better)) {
      /* Prefer to use the first format that has the same depth with the same
       * flags, and if none with the same flags exist use the first other one
       * that has the same depth */
      out_info = t_info;
      out_depth = t_depth;
      out_flags = t_flags;
    } else if (t_depth >= in_depth && (in_depth > out_depth
            || (out_depth >= in_depth && t_flags_better))) {
      /* Otherwise use the first format that has a higher depth with the same flags,
       * if none with the same flags exist use the first other one that has a higher
       * depth */
      out_info = t_info;
      out_depth = t_depth;
      out_flags = t_flags;
    } else if ((t_depth > out_depth && out_depth < in_depth)
        || (t_flags_better && out_depth == t_depth)) {
      /* Else get at least the one with the highest depth, ideally with the same flags */
      out_info = t_info;
      out_depth = t_depth;
      out_flags = t_flags;
    }

  }

  if (out_info)
    gst_structure_set_static_str (outs, "format", G_TYPE_STRING,
        GST_AUDIO_FORMAT_INFO_NAME (out_info), NULL);
}

static void
gst_audio_convert_fixate_channels (GstBaseTransform * base, GstStructure * ins,
    GstStructure * outs)
{
  GstAudioConvert *this = GST_AUDIO_CONVERT (base);
  gint in_chans, out_chans;
  guint64 in_mask = 0, out_mask = 0;
  gboolean has_in_mask = FALSE, has_out_mask = FALSE;

  if (!gst_structure_get_int (ins, "channels", &in_chans))
    return;                     /* this shouldn't really happen, should it? */

  if (!gst_structure_has_field (outs, "channels")) {
    /* we could try to get the implied number of channels from the layout,
     * but that seems overdoing it for a somewhat exotic corner case */
    gst_structure_remove_field (outs, "channel-mask");
    return;
  }

  /* ok, let's fixate the channels if they are not fixated yet */
  gst_structure_fixate_field_nearest_int (outs, "channels", in_chans);

  if (!gst_structure_get_int (outs, "channels", &out_chans)) {
    /* shouldn't really happen ... */
    gst_structure_remove_field (outs, "channel-mask");
    return;
  }

  /* get the channel layout of the output if any */
  has_out_mask = gst_structure_has_field (outs, "channel-mask");
  if (has_out_mask) {
    gst_structure_get (outs, "channel-mask", GST_TYPE_BITMASK, &out_mask, NULL);
  } else {
    /* channels == 1 => MONO */
    if (out_chans == 2) {
      out_mask =
          GST_AUDIO_CHANNEL_POSITION_MASK (FRONT_LEFT) |
          GST_AUDIO_CHANNEL_POSITION_MASK (FRONT_RIGHT);
      has_out_mask = TRUE;
      gst_structure_set_static_str (outs, "channel-mask", GST_TYPE_BITMASK,
          out_mask, NULL);
    }
  }

  /* get the channel layout of the input if any */
  has_in_mask = gst_structure_has_field (ins, "channel-mask");
  if (has_in_mask) {
    gst_structure_get (ins, "channel-mask", GST_TYPE_BITMASK, &in_mask, NULL);
  } else {
    /* channels == 1 => MONO */
    if (in_chans == 2) {
      in_mask =
          GST_AUDIO_CHANNEL_POSITION_MASK (FRONT_LEFT) |
          GST_AUDIO_CHANNEL_POSITION_MASK (FRONT_RIGHT);
      has_in_mask = TRUE;
    } else if (in_chans > 2)
      GST_WARNING_OBJECT (base, "Upstream caps contain no channel mask");
  }

  if (!has_out_mask && out_chans == 1 && (in_chans != out_chans
          || !has_in_mask))
    return;                     /* nothing to do, default layout will be assumed */

  if (in_chans == out_chans && (has_in_mask || in_chans == 1)) {
    /* same number of channels and no output layout: just use input layout */
    if (!has_out_mask) {
      /* in_chans == 1 handled above already */
      gst_structure_set_static_str (outs, "channel-mask", GST_TYPE_BITMASK,
          in_mask, NULL);
      return;
    }

    /* If both masks are the same we're done, this includes the NONE layout case */
    if (in_mask == out_mask)
      return;

    /* if output layout is fixed already and looks sane, we're done */
    if (n_bits_set (out_mask) == out_chans)
      return;

    if (n_bits_set (out_mask) < in_chans) {
      /* Not much we can do here, this shouldn't just happen */
      GST_WARNING_OBJECT (base,
          "Invalid downstream channel-mask with too few bits set");
    } else {
      guint64 intersection;

      /* if the output layout is not fixed, check if the output layout contains
       * the input layout */
      intersection = in_mask & out_mask;
      if (n_bits_set (intersection) >= in_chans) {
        gst_structure_set_static_str (outs, "channel-mask", GST_TYPE_BITMASK,
            in_mask, NULL);
        return;
      }

      /* output layout is not fixed and does not contain the input layout, so
       * just pick the first possibility */
      intersection = find_suitable_mask (out_mask, out_chans);
      if (intersection) {
        gst_structure_set_static_str (outs, "channel-mask", GST_TYPE_BITMASK,
            intersection, NULL);
        return;
      }
    }

    /* ... else fall back to default layout (NB: out_layout is NULL here) */
    GST_WARNING_OBJECT (base, "unexpected output channel layout");
  } else {
    guint64 intersection;

    /* number of input channels != number of output channels:
     * if this value contains a list of channel layouts (or even worse: a list
     * with another list), just pick the first value and repeat until we find a
     * channel position array or something else that's not a list; we assume
     * the input if half-way sane and don't try to fall back on other list items
     * if the first one is something unexpected or non-channel-pos-array-y */
    if (has_out_mask && out_mask == 0) {
      gst_structure_set_static_str (outs, "channel-mask", GST_TYPE_BITMASK,
          out_mask, NULL);
      return;
    } else if (n_bits_set (out_mask) >= out_chans) {
      intersection = find_suitable_mask (out_mask, out_chans);
      gst_structure_set_static_str (outs, "channel-mask", GST_TYPE_BITMASK,
          intersection, NULL);
      return;
    } else if (this->mix_matrix_is_set) {
      /* Assume the matrix matches the number of in/out channels. This will be
       * validated when creating the converter. */
    } else {
      /* what now?! Just ignore what we're given and use default positions */
      GST_WARNING_OBJECT (base, "invalid or unexpected channel-positions");
    }
  }

  /* missing or invalid output layout and we can't use the input layout for
   * one reason or another, so just pick a default layout (we could be smarter
   * and try to add/remove channels from the input layout, or pick a default
   * layout based on LFE-presence in input layout, but let's save that for
   * another day). For mono, no mask is required and the fallback mask is 0 */
  if (out_chans > 1
      && (out_mask = gst_audio_channel_get_fallback_mask (out_chans))) {
    GST_DEBUG_OBJECT (base, "using default channel layout as fallback");
    gst_structure_set_static_str (outs, "channel-mask", GST_TYPE_BITMASK,
        out_mask, NULL);
  } else if (out_chans > 1) {
    GST_ERROR_OBJECT (base, "Have no default layout for %d channels",
        out_chans);
    gst_structure_set_static_str (outs, "channel-mask", GST_TYPE_BITMASK,
        G_GUINT64_CONSTANT (0), NULL);
  }
}

/* try to keep as many of the structure members the same by fixating the
 * possible ranges; this way we convert the least amount of things as possible
 */
static GstCaps *
gst_audio_convert_fixate_caps (GstBaseTransform * base,
    GstPadDirection direction, GstCaps * caps, GstCaps * othercaps)
{
  GstStructure *ins, *outs;
  GstCaps *result;

  GST_DEBUG_OBJECT (base, "trying to fixate othercaps %" GST_PTR_FORMAT
      " based on caps %" GST_PTR_FORMAT, othercaps, caps);

  result = gst_caps_intersect (othercaps, caps);
  if (gst_caps_is_empty (result)) {
    GstCaps *removed = gst_caps_copy (caps);

    if (result)
      gst_caps_unref (result);
    gst_caps_map_in_place (removed, remove_format_from_structure, NULL);
    gst_caps_map_in_place (removed, remove_layout_from_structure, NULL);
    result = gst_caps_intersect (othercaps, removed);
    gst_caps_unref (removed);
    if (gst_caps_is_empty (result)) {
      if (result)
        gst_caps_unref (result);
      result = othercaps;
    } else {
      gst_caps_unref (othercaps);
    }
  } else {
    gst_caps_unref (othercaps);
  }

  GST_DEBUG_OBJECT (base, "now fixating %" GST_PTR_FORMAT, result);

  /* fixate remaining fields */
  result = gst_caps_make_writable (result);

  ins = gst_caps_get_structure (caps, 0);
  outs = gst_caps_get_structure (result, 0);

  gst_audio_convert_fixate_channels (base, ins, outs);
  gst_audio_convert_fixate_format (base, ins, outs);

  /* fixate remaining */
  result = gst_caps_fixate (result);

  GST_DEBUG_OBJECT (base, "fixated othercaps to %" GST_PTR_FORMAT, result);

  return result;
}

static gboolean
gst_audio_convert_ensure_converter (GstBaseTransform * base,
    GstAudioInfo * in_info, GstAudioInfo * out_info)
{
  GstAudioConvert *this = GST_AUDIO_CONVERT (base);
  GstStructure *config;
  gboolean in_place;
  gboolean ret = TRUE;

  GST_OBJECT_LOCK (this);
  if (this->convert) {
    GST_TRACE_OBJECT (this, "We already have a converter");
    goto done;
  }

  if (!GST_AUDIO_INFO_IS_VALID (in_info) || !GST_AUDIO_INFO_IS_VALID (out_info)) {
    GST_LOG_OBJECT (this,
        "No format information (yet), not creating converter");
    goto done;
  }

  config = gst_structure_new_static_str ("GstAudioConverterConfig",
      GST_AUDIO_CONVERTER_OPT_DITHER_METHOD, GST_TYPE_AUDIO_DITHER_METHOD,
      this->dither,
      GST_AUDIO_CONVERTER_OPT_DITHER_THRESHOLD, G_TYPE_UINT,
      this->dither_threshold,
      GST_AUDIO_CONVERTER_OPT_NOISE_SHAPING_METHOD,
      GST_TYPE_AUDIO_NOISE_SHAPING_METHOD, this->ns, NULL);

  if (this->mix_matrix_is_set) {
    gst_structure_set_value_static_str (config,
        GST_AUDIO_CONVERTER_OPT_MIX_MATRIX, &this->mix_matrix);

    this->convert = gst_audio_converter_new (0, in_info, out_info, config);
  } else if (this->input_channels_reorder_mode !=
      GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_NONE) {
    GstAudioFlags in_flags;
    GstAudioChannelPosition in_position[64];
    gboolean restore_in = FALSE;

    if (this->input_channels_reorder_mode ==
        GST_AUDIO_CONVERT_INPUT_CHANNELS_REORDER_MODE_FORCE
        || GST_AUDIO_INFO_IS_UNPOSITIONED (in_info)) {
      in_flags = GST_AUDIO_INFO_FLAGS (in_info);
      memcpy (in_position, in_info->position,
          GST_AUDIO_INFO_CHANNELS (in_info) * sizeof (GstAudioChannelPosition));

      if (gst_audio_convert_position_channels_from_reorder_configuration
          (GST_AUDIO_INFO_CHANNELS (in_info), this->input_channels_reorder,
              in_info->position)) {
        GST_AUDIO_INFO_FLAGS (in_info) &= ~GST_AUDIO_FLAG_UNPOSITIONED;
        restore_in = TRUE;
      }
    }

    this->convert = gst_audio_converter_new (0, in_info, out_info, config);

    if (restore_in) {
      GST_AUDIO_INFO_FLAGS (in_info) = in_flags;
      memcpy (in_info->position, in_position,
          GST_AUDIO_INFO_CHANNELS (in_info) * sizeof (GstAudioChannelPosition));
    }

  } else {
    this->convert = gst_audio_converter_new (0, in_info, out_info, config);
  }

  if (this->convert == NULL)
    goto no_converter;

  in_place = gst_audio_converter_supports_inplace (this->convert);
  GST_OBJECT_UNLOCK (this);

  gst_base_transform_set_in_place (base, in_place);

  gst_base_transform_set_passthrough (base,
      gst_audio_converter_is_passthrough (this->convert));

  GST_OBJECT_LOCK (this);

done:
  GST_OBJECT_UNLOCK (this);
  return ret;

no_converter:
  GST_ERROR_OBJECT (this, "Failed to make converter");
  ret = FALSE;
  goto done;
}

static gboolean
gst_audio_convert_set_caps (GstBaseTransform * base, GstCaps * incaps,
    GstCaps * outcaps)
{
  GstAudioConvert *this = GST_AUDIO_CONVERT (base);
  GstAudioInfo in_info;
  GstAudioInfo out_info;
  gboolean ret;

  GST_DEBUG_OBJECT (base, "incaps %" GST_PTR_FORMAT ", outcaps %"
      GST_PTR_FORMAT, incaps, outcaps);

  if (this->convert) {
    gst_audio_converter_free (this->convert);
    this->convert = NULL;
  }

  if (!gst_audio_info_from_caps (&in_info, incaps))
    goto invalid_in;
  if (!gst_audio_info_from_caps (&out_info, outcaps))
    goto invalid_out;

  ret = gst_audio_convert_ensure_converter (base, &in_info, &out_info);

  if (ret) {
    this->in_info = in_info;
    this->out_info = out_info;
  }

done:
  return ret;

  /* ERRORS */
invalid_in:
  {
    GST_ERROR_OBJECT (base, "invalid input caps");
    ret = FALSE;
    goto done;
  }
invalid_out:
  {
    GST_ERROR_OBJECT (base, "invalid output caps");
    ret = FALSE;
    goto done;
  }
}

/* if called through gst_audio_convert_transform_ip() inbuf == outbuf */
static GstFlowReturn
gst_audio_convert_transform (GstBaseTransform * base, GstBuffer * inbuf,
    GstBuffer * outbuf)
{
  GstFlowReturn ret;
  GstAudioConvert *this = GST_AUDIO_CONVERT (base);
  GstAudioBuffer srcabuf, dstabuf;
  gboolean inbuf_writable;
  GstAudioConverterFlags flags;

  /* https://bugzilla.gnome.org/show_bug.cgi?id=396835 */
  if (gst_buffer_get_size (inbuf) == 0)
    return GST_FLOW_OK;

  gst_audio_convert_ensure_converter (base, &this->in_info, &this->out_info);

  if (!this->convert) {
    GST_ERROR_OBJECT (this, "No audio converter at transform time");
    return GST_FLOW_ERROR;
  }

  if (inbuf != outbuf) {
    inbuf_writable = gst_buffer_is_writable (inbuf)
        && gst_buffer_n_memory (inbuf) == 1
        && gst_memory_is_writable (gst_buffer_peek_memory (inbuf, 0));

    if (!gst_audio_buffer_map (&srcabuf, &this->in_info, inbuf,
            inbuf_writable ? GST_MAP_READWRITE : GST_MAP_READ))
      goto inmap_error;
  } else {
    inbuf_writable = TRUE;
  }

  if (!gst_audio_buffer_map (&dstabuf, &this->out_info, outbuf, GST_MAP_WRITE))
    goto outmap_error;

  /* and convert the samples */
  flags = 0;
  if (inbuf_writable)
    flags |= GST_AUDIO_CONVERTER_FLAG_IN_WRITABLE;

  if (!GST_BUFFER_FLAG_IS_SET (inbuf, GST_BUFFER_FLAG_GAP)) {
    if (!gst_audio_converter_samples (this->convert, flags,
            inbuf != outbuf ? srcabuf.planes : dstabuf.planes,
            dstabuf.n_samples, dstabuf.planes, dstabuf.n_samples))
      goto convert_error;
  } else {
    /* Create silence buffer */
    gint i;
    for (i = 0; i < dstabuf.n_planes; i++) {
      gst_audio_format_info_fill_silence (this->out_info.finfo,
          dstabuf.planes[i], GST_AUDIO_BUFFER_PLANE_SIZE (&dstabuf));
    }
  }
  ret = GST_FLOW_OK;

done:
  gst_audio_buffer_unmap (&dstabuf);
  if (inbuf != outbuf)
    gst_audio_buffer_unmap (&srcabuf);

  return ret;

  /* ERRORS */
convert_error:
  {
    GST_ELEMENT_ERROR (this, STREAM, FORMAT,
        (NULL), ("error while converting"));
    ret = GST_FLOW_ERROR;
    goto done;
  }
inmap_error:
  {
    GST_ELEMENT_ERROR (this, STREAM, FORMAT,
        (NULL), ("failed to map input buffer"));
    return GST_FLOW_ERROR;
  }
outmap_error:
  {
    GST_ELEMENT_ERROR (this, STREAM, FORMAT,
        (NULL), ("failed to map output buffer"));
    if (inbuf != outbuf)
      gst_audio_buffer_unmap (&srcabuf);
    return GST_FLOW_ERROR;
  }
}

static GstFlowReturn
gst_audio_convert_transform_ip (GstBaseTransform * base, GstBuffer * buf)
{
  return gst_audio_convert_transform (base, buf, buf);
}

static gboolean
gst_audio_convert_transform_meta (GstBaseTransform * trans, GstBuffer * outbuf,
    GstMeta * meta, GstBuffer * inbuf)
{
  const GstMetaInfo *info = meta->info;
  const gchar *const *tags;

  tags = gst_meta_api_type_get_tags (info->api);

  if (!tags || (g_strv_length ((gchar **) tags) == 1
          && gst_meta_api_type_has_tag (info->api, META_TAG_AUDIO)))
    return TRUE;

  return FALSE;
}

static GstFlowReturn
gst_audio_convert_submit_input_buffer (GstBaseTransform * base,
    gboolean is_discont, GstBuffer * input)
{
  GstAudioConvert *this = GST_AUDIO_CONVERT (base);

  if (base->segment.format == GST_FORMAT_TIME) {
    if (!GST_AUDIO_INFO_IS_VALID (&this->in_info)) {
      GST_WARNING_OBJECT (this, "Got buffer, but not negotiated yet!");
      return GST_FLOW_NOT_NEGOTIATED;
    }

    input =
        gst_audio_buffer_clip (input, &base->segment, this->in_info.rate,
        this->in_info.bpf);

    if (!input)
      return GST_FLOW_OK;
  }

  return GST_BASE_TRANSFORM_CLASS (parent_class)->submit_input_buffer (base,
      is_discont, input);
}

static GstFlowReturn
gst_audio_convert_prepare_output_buffer (GstBaseTransform * base,
    GstBuffer * inbuf, GstBuffer ** outbuf)
{
  GstAudioConvert *this = GST_AUDIO_CONVERT (base);
  GstAudioMeta *meta;
  GstFlowReturn ret;

  ret = GST_BASE_TRANSFORM_CLASS (parent_class)->prepare_output_buffer (base,
      inbuf, outbuf);

  if (ret != GST_FLOW_OK)
    return ret;

  meta = gst_buffer_get_audio_meta (inbuf);

  if (inbuf != *outbuf) {
    gsize samples = meta ?
        meta->samples : (gst_buffer_get_size (inbuf) / this->in_info.bpf);

    /* ensure that the output buffer is not bigger than what we need */
    gst_buffer_resize (*outbuf, 0, samples * this->out_info.bpf);

    /* add the audio meta on the output buffer if it's planar */
    if (this->out_info.layout == GST_AUDIO_LAYOUT_NON_INTERLEAVED) {
      gst_buffer_add_audio_meta (*outbuf, &this->out_info, samples, NULL);
    }
  } else {
    /* if the input buffer came with a GstAudioMeta,
     * update it to reflect the properties of the output format */
    if (meta)
      meta->info = this->out_info;
  }

  return ret;
}

static void
gst_audio_convert_set_mix_matrix (GstAudioConvert * this, const GValue * value)
{
  GST_OBJECT_LOCK (this);

  g_clear_pointer (&this->convert, gst_audio_converter_free);

  if (!gst_value_array_get_size (value)) {
    g_value_copy (value, &this->mix_matrix);
    this->mix_matrix_is_set = TRUE;
  } else {
    const GValue *first_row = gst_value_array_get_value (value, 0);

    if (gst_value_array_get_size (first_row)) {
      g_value_copy (value, &this->mix_matrix);
      this->mix_matrix_is_set = TRUE;
    } else {
      GST_WARNING_OBJECT (this, "Empty mix matrix's first row.");
      this->mix_matrix_is_set = FALSE;
    }
  }

  GST_OBJECT_UNLOCK (this);

  /* We can't create the converter here because the application could be setting
   * a new mix-matrix for caps we haven't received yet (e.g. number of input
   * channels changed). Assume for now we can't be passthrough and in-place,
   * that will be revised once new caps or next buffer arrives. */
  gst_base_transform_set_in_place (GST_BASE_TRANSFORM_CAST (this), FALSE);
  gst_base_transform_set_passthrough (GST_BASE_TRANSFORM_CAST (this), FALSE);
  gst_base_transform_reconfigure_sink (GST_BASE_TRANSFORM_CAST (this));
}

static void
gst_audio_convert_set_property (GObject * object, guint prop_id,
    const GValue * value, GParamSpec * pspec)
{
  GstAudioConvert *this = GST_AUDIO_CONVERT (object);

  switch (prop_id) {
    case PROP_DITHERING:
      this->dither = g_value_get_enum (value);
      break;
    case PROP_NOISE_SHAPING:
      this->ns = g_value_get_enum (value);
      break;
    case PROP_DITHERING_THRESHOLD:
      this->dither_threshold = g_value_get_uint (value);
      break;
    case PROP_MIX_MATRIX:
      gst_audio_convert_set_mix_matrix (this, value);
      break;
    case PROP_INPUT_CHANNELS_REORDER:
      this->input_channels_reorder = g_value_get_enum (value);
      break;
    case PROP_INPUT_CHANNELS_REORDER_MODE:
      this->input_channels_reorder_mode = g_value_get_enum (value);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}

static void
gst_audio_convert_get_property (GObject * object, guint prop_id,
    GValue * value, GParamSpec * pspec)
{
  GstAudioConvert *this = GST_AUDIO_CONVERT (object);

  switch (prop_id) {
    case PROP_DITHERING:
      g_value_set_enum (value, this->dither);
      break;
    case PROP_NOISE_SHAPING:
      g_value_set_enum (value, this->ns);
      break;
    case PROP_DITHERING_THRESHOLD:
      g_value_set_uint (value, this->dither_threshold);
      break;
    case PROP_MIX_MATRIX:
      GST_OBJECT_LOCK (object);
      if (this->mix_matrix_is_set)
        g_value_copy (&this->mix_matrix, value);
      GST_OBJECT_UNLOCK (object);
      break;
    case PROP_INPUT_CHANNELS_REORDER:
      g_value_set_enum (value, this->input_channels_reorder);
      break;
    case PROP_INPUT_CHANNELS_REORDER_MODE:
      g_value_set_enum (value, this->input_channels_reorder_mode);
      break;
    default:
      G_OBJECT_WARN_INVALID_PROPERTY_ID (object, prop_id, pspec);
      break;
  }
}
