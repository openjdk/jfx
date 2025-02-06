


#include "pbutils-enumtypes.h"

#include "pbutils.h"
#include "pbutils-prelude.h"
#include "codec-utils.h"
#include "descriptions.h"
#ifndef GSTREAMER_LITE
#include "encoding-profile.h"
#include "encoding-target.h"
#include "install-plugins.h"
#endif // GSTREAMER_LITE
#include "missing-plugins.h"
#ifndef GSTREAMER_LITE
#include "gstdiscoverer.h"
#endif // GSTREAMER_LITE
#include "gstaudiovisualizer.h"

/* enumerations from "gstaudiovisualizer.h" */
GType
gst_audio_visualizer_shader_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_AUDIO_VISUALIZER_SHADER_NONE, "GST_AUDIO_VISUALIZER_SHADER_NONE", "none" },
      { GST_AUDIO_VISUALIZER_SHADER_FADE, "GST_AUDIO_VISUALIZER_SHADER_FADE", "fade" },
      { GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_UP, "GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_UP", "fade-and-move-up" },
      { GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_DOWN, "GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_DOWN", "fade-and-move-down" },
      { GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_LEFT, "GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_LEFT", "fade-and-move-left" },
      { GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_RIGHT, "GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_RIGHT", "fade-and-move-right" },
      { GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_HORIZ_OUT, "GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_HORIZ_OUT", "fade-and-move-horiz-out" },
      { GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_HORIZ_IN, "GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_HORIZ_IN", "fade-and-move-horiz-in" },
      { GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_VERT_OUT, "GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_VERT_OUT", "fade-and-move-vert-out" },
      { GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_VERT_IN, "GST_AUDIO_VISUALIZER_SHADER_FADE_AND_MOVE_VERT_IN", "fade-and-move-vert-in" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAudioVisualizerShader", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

#ifndef GSTREAMER_LITE
/* enumerations from "gstdiscoverer.h" */
GType
gst_discoverer_result_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_DISCOVERER_OK, "GST_DISCOVERER_OK", "ok" },
      { GST_DISCOVERER_URI_INVALID, "GST_DISCOVERER_URI_INVALID", "uri-invalid" },
      { GST_DISCOVERER_ERROR, "GST_DISCOVERER_ERROR", "error" },
      { GST_DISCOVERER_TIMEOUT, "GST_DISCOVERER_TIMEOUT", "timeout" },
      { GST_DISCOVERER_BUSY, "GST_DISCOVERER_BUSY", "busy" },
      { GST_DISCOVERER_MISSING_PLUGINS, "GST_DISCOVERER_MISSING_PLUGINS", "missing-plugins" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstDiscovererResult", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
GType
gst_discoverer_serialize_flags_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      { GST_DISCOVERER_SERIALIZE_BASIC, "GST_DISCOVERER_SERIALIZE_BASIC", "basic" },
      { GST_DISCOVERER_SERIALIZE_CAPS, "GST_DISCOVERER_SERIALIZE_CAPS", "caps" },
      { GST_DISCOVERER_SERIALIZE_TAGS, "GST_DISCOVERER_SERIALIZE_TAGS", "tags" },
      { GST_DISCOVERER_SERIALIZE_MISC, "GST_DISCOVERER_SERIALIZE_MISC", "misc" },
      { GST_DISCOVERER_SERIALIZE_ALL, "GST_DISCOVERER_SERIALIZE_ALL", "all" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_flags_register_static ("GstDiscovererSerializeFlags", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "install-plugins.h" */
GType
gst_install_plugins_return_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_INSTALL_PLUGINS_SUCCESS, "GST_INSTALL_PLUGINS_SUCCESS", "success" },
      { GST_INSTALL_PLUGINS_NOT_FOUND, "GST_INSTALL_PLUGINS_NOT_FOUND", "not-found" },
      { GST_INSTALL_PLUGINS_ERROR, "GST_INSTALL_PLUGINS_ERROR", "error" },
      { GST_INSTALL_PLUGINS_PARTIAL_SUCCESS, "GST_INSTALL_PLUGINS_PARTIAL_SUCCESS", "partial-success" },
      { GST_INSTALL_PLUGINS_USER_ABORT, "GST_INSTALL_PLUGINS_USER_ABORT", "user-abort" },
      { GST_INSTALL_PLUGINS_CRASHED, "GST_INSTALL_PLUGINS_CRASHED", "crashed" },
      { GST_INSTALL_PLUGINS_INVALID, "GST_INSTALL_PLUGINS_INVALID", "invalid" },
      { GST_INSTALL_PLUGINS_STARTED_OK, "GST_INSTALL_PLUGINS_STARTED_OK", "started-ok" },
      { GST_INSTALL_PLUGINS_INTERNAL_FAILURE, "GST_INSTALL_PLUGINS_INTERNAL_FAILURE", "internal-failure" },
      { GST_INSTALL_PLUGINS_HELPER_MISSING, "GST_INSTALL_PLUGINS_HELPER_MISSING", "helper-missing" },
      { GST_INSTALL_PLUGINS_INSTALL_IN_PROGRESS, "GST_INSTALL_PLUGINS_INSTALL_IN_PROGRESS", "install-in-progress" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstInstallPluginsReturn", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
#endif // GSTREAMER_LITE


