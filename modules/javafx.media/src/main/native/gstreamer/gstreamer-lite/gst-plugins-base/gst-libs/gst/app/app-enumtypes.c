


#include "app-enumtypes.h"

#include "gstappsrc.h"

/* enumerations from "gstappsrc.h" */
GType
gst_app_stream_type_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_APP_STREAM_TYPE_STREAM, "GST_APP_STREAM_TYPE_STREAM", "stream" },
      { GST_APP_STREAM_TYPE_SEEKABLE, "GST_APP_STREAM_TYPE_SEEKABLE", "seekable" },
      { GST_APP_STREAM_TYPE_RANDOM_ACCESS, "GST_APP_STREAM_TYPE_RANDOM_ACCESS", "random-access" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstAppStreamType", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}



