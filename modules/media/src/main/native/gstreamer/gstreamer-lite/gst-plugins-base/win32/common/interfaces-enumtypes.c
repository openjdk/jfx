


#include "interfaces-enumtypes.h"

#include "colorbalance.h"
#include "colorbalancechannel.h"
#include "mixer.h"
#include "mixeroptions.h"
#include "mixertrack.h"
#include "navigation.h"
#include "propertyprobe.h"
#include "streamvolume.h"
#include "tuner.h"
#include "tunernorm.h"
#include "tunerchannel.h"
#include "videoorientation.h"
#include "xoverlay.h"

/* enumerations from "colorbalance.h" */
GType
gst_color_balance_type_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_COLOR_BALANCE_HARDWARE, "GST_COLOR_BALANCE_HARDWARE", "hardware"},
      {GST_COLOR_BALANCE_SOFTWARE, "GST_COLOR_BALANCE_SOFTWARE", "software"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstColorBalanceType", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "mixer.h" */
GType
gst_mixer_type_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_MIXER_HARDWARE, "GST_MIXER_HARDWARE", "hardware"},
      {GST_MIXER_SOFTWARE, "GST_MIXER_SOFTWARE", "software"},
      {0, NULL, NULL}
    };
    GType g_define_type_id = g_enum_register_static ("GstMixerType", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_mixer_message_type_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_MIXER_MESSAGE_INVALID, "GST_MIXER_MESSAGE_INVALID", "invalid"},
      {GST_MIXER_MESSAGE_MUTE_TOGGLED, "GST_MIXER_MESSAGE_MUTE_TOGGLED",
          "mute-toggled"},
      {GST_MIXER_MESSAGE_RECORD_TOGGLED, "GST_MIXER_MESSAGE_RECORD_TOGGLED",
          "record-toggled"},
      {GST_MIXER_MESSAGE_VOLUME_CHANGED, "GST_MIXER_MESSAGE_VOLUME_CHANGED",
          "volume-changed"},
      {GST_MIXER_MESSAGE_OPTION_CHANGED, "GST_MIXER_MESSAGE_OPTION_CHANGED",
          "option-changed"},
      {GST_MIXER_MESSAGE_OPTIONS_LIST_CHANGED,
          "GST_MIXER_MESSAGE_OPTIONS_LIST_CHANGED", "options-list-changed"},
      {GST_MIXER_MESSAGE_MIXER_CHANGED, "GST_MIXER_MESSAGE_MIXER_CHANGED",
          "mixer-changed"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstMixerMessageType", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_mixer_flags_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      {GST_MIXER_FLAG_NONE, "GST_MIXER_FLAG_NONE", "none"},
      {GST_MIXER_FLAG_AUTO_NOTIFICATIONS, "GST_MIXER_FLAG_AUTO_NOTIFICATIONS",
          "auto-notifications"},
      {GST_MIXER_FLAG_HAS_WHITELIST, "GST_MIXER_FLAG_HAS_WHITELIST",
          "has-whitelist"},
      {GST_MIXER_FLAG_GROUPING, "GST_MIXER_FLAG_GROUPING", "grouping"},
      {0, NULL, NULL}
    };
    GType g_define_type_id = g_flags_register_static ("GstMixerFlags", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "mixertrack.h" */
GType
gst_mixer_track_flags_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      {GST_MIXER_TRACK_INPUT, "GST_MIXER_TRACK_INPUT", "input"},
      {GST_MIXER_TRACK_OUTPUT, "GST_MIXER_TRACK_OUTPUT", "output"},
      {GST_MIXER_TRACK_MUTE, "GST_MIXER_TRACK_MUTE", "mute"},
      {GST_MIXER_TRACK_RECORD, "GST_MIXER_TRACK_RECORD", "record"},
      {GST_MIXER_TRACK_MASTER, "GST_MIXER_TRACK_MASTER", "master"},
      {GST_MIXER_TRACK_SOFTWARE, "GST_MIXER_TRACK_SOFTWARE", "software"},
      {GST_MIXER_TRACK_NO_RECORD, "GST_MIXER_TRACK_NO_RECORD", "no-record"},
      {GST_MIXER_TRACK_NO_MUTE, "GST_MIXER_TRACK_NO_MUTE", "no-mute"},
      {GST_MIXER_TRACK_WHITELIST, "GST_MIXER_TRACK_WHITELIST", "whitelist"},
      {GST_MIXER_TRACK_READONLY, "GST_MIXER_TRACK_READONLY", "readonly"},
      {GST_MIXER_TRACK_WRITEONLY, "GST_MIXER_TRACK_WRITEONLY", "writeonly"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_flags_register_static ("GstMixerTrackFlags", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "navigation.h" */
GType
gst_navigation_command_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_NAVIGATION_COMMAND_INVALID, "GST_NAVIGATION_COMMAND_INVALID",
          "invalid"},
      {GST_NAVIGATION_COMMAND_MENU1, "GST_NAVIGATION_COMMAND_MENU1", "menu1"},
      {GST_NAVIGATION_COMMAND_MENU2, "GST_NAVIGATION_COMMAND_MENU2", "menu2"},
      {GST_NAVIGATION_COMMAND_MENU3, "GST_NAVIGATION_COMMAND_MENU3", "menu3"},
      {GST_NAVIGATION_COMMAND_MENU4, "GST_NAVIGATION_COMMAND_MENU4", "menu4"},
      {GST_NAVIGATION_COMMAND_MENU5, "GST_NAVIGATION_COMMAND_MENU5", "menu5"},
      {GST_NAVIGATION_COMMAND_MENU6, "GST_NAVIGATION_COMMAND_MENU6", "menu6"},
      {GST_NAVIGATION_COMMAND_MENU7, "GST_NAVIGATION_COMMAND_MENU7", "menu7"},
      {GST_NAVIGATION_COMMAND_LEFT, "GST_NAVIGATION_COMMAND_LEFT", "left"},
      {GST_NAVIGATION_COMMAND_RIGHT, "GST_NAVIGATION_COMMAND_RIGHT", "right"},
      {GST_NAVIGATION_COMMAND_UP, "GST_NAVIGATION_COMMAND_UP", "up"},
      {GST_NAVIGATION_COMMAND_DOWN, "GST_NAVIGATION_COMMAND_DOWN", "down"},
      {GST_NAVIGATION_COMMAND_ACTIVATE, "GST_NAVIGATION_COMMAND_ACTIVATE",
          "activate"},
      {GST_NAVIGATION_COMMAND_PREV_ANGLE, "GST_NAVIGATION_COMMAND_PREV_ANGLE",
          "prev-angle"},
      {GST_NAVIGATION_COMMAND_NEXT_ANGLE, "GST_NAVIGATION_COMMAND_NEXT_ANGLE",
          "next-angle"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstNavigationCommand", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_navigation_query_type_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_NAVIGATION_QUERY_INVALID, "GST_NAVIGATION_QUERY_INVALID", "invalid"},
      {GST_NAVIGATION_QUERY_COMMANDS, "GST_NAVIGATION_QUERY_COMMANDS",
          "commands"},
      {GST_NAVIGATION_QUERY_ANGLES, "GST_NAVIGATION_QUERY_ANGLES", "angles"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstNavigationQueryType", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_navigation_message_type_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_NAVIGATION_MESSAGE_INVALID, "GST_NAVIGATION_MESSAGE_INVALID",
          "invalid"},
      {GST_NAVIGATION_MESSAGE_MOUSE_OVER, "GST_NAVIGATION_MESSAGE_MOUSE_OVER",
          "mouse-over"},
      {GST_NAVIGATION_MESSAGE_COMMANDS_CHANGED,
          "GST_NAVIGATION_MESSAGE_COMMANDS_CHANGED", "commands-changed"},
      {GST_NAVIGATION_MESSAGE_ANGLES_CHANGED,
          "GST_NAVIGATION_MESSAGE_ANGLES_CHANGED", "angles-changed"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstNavigationMessageType", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

GType
gst_navigation_event_type_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_NAVIGATION_EVENT_INVALID, "GST_NAVIGATION_EVENT_INVALID", "invalid"},
      {GST_NAVIGATION_EVENT_KEY_PRESS, "GST_NAVIGATION_EVENT_KEY_PRESS",
          "key-press"},
      {GST_NAVIGATION_EVENT_KEY_RELEASE, "GST_NAVIGATION_EVENT_KEY_RELEASE",
          "key-release"},
      {GST_NAVIGATION_EVENT_MOUSE_BUTTON_PRESS,
          "GST_NAVIGATION_EVENT_MOUSE_BUTTON_PRESS", "mouse-button-press"},
      {GST_NAVIGATION_EVENT_MOUSE_BUTTON_RELEASE,
            "GST_NAVIGATION_EVENT_MOUSE_BUTTON_RELEASE",
          "mouse-button-release"},
      {GST_NAVIGATION_EVENT_MOUSE_MOVE, "GST_NAVIGATION_EVENT_MOUSE_MOVE",
          "mouse-move"},
      {GST_NAVIGATION_EVENT_COMMAND, "GST_NAVIGATION_EVENT_COMMAND", "command"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstNavigationEventType", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "streamvolume.h" */
GType
gst_stream_volume_format_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_STREAM_VOLUME_FORMAT_LINEAR, "GST_STREAM_VOLUME_FORMAT_LINEAR",
          "linear"},
      {GST_STREAM_VOLUME_FORMAT_CUBIC, "GST_STREAM_VOLUME_FORMAT_CUBIC",
          "cubic"},
      {GST_STREAM_VOLUME_FORMAT_DB, "GST_STREAM_VOLUME_FORMAT_DB", "db"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_enum_register_static ("GstStreamVolumeFormat", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "tunerchannel.h" */
GType
gst_tuner_channel_flags_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      {GST_TUNER_CHANNEL_INPUT, "GST_TUNER_CHANNEL_INPUT", "input"},
      {GST_TUNER_CHANNEL_OUTPUT, "GST_TUNER_CHANNEL_OUTPUT", "output"},
      {GST_TUNER_CHANNEL_FREQUENCY, "GST_TUNER_CHANNEL_FREQUENCY", "frequency"},
      {GST_TUNER_CHANNEL_AUDIO, "GST_TUNER_CHANNEL_AUDIO", "audio"},
      {0, NULL, NULL}
    };
    GType g_define_type_id =
        g_flags_register_static ("GstTunerChannelFlags", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
