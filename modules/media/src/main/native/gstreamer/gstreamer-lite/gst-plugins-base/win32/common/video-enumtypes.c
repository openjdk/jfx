


#include "video-enumtypes.h"

#include "video.h"

/* enumerations from "video.h" */
GType
gst_video_format_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      {GST_VIDEO_FORMAT_UNKNOWN, "GST_VIDEO_FORMAT_UNKNOWN", "unknown"},
      {GST_VIDEO_FORMAT_I420, "GST_VIDEO_FORMAT_I420", "i420"},
      {GST_VIDEO_FORMAT_YV12, "GST_VIDEO_FORMAT_YV12", "yv12"},
      {GST_VIDEO_FORMAT_YUY2, "GST_VIDEO_FORMAT_YUY2", "yuy2"},
      {GST_VIDEO_FORMAT_UYVY, "GST_VIDEO_FORMAT_UYVY", "uyvy"},
      {GST_VIDEO_FORMAT_AYUV, "GST_VIDEO_FORMAT_AYUV", "ayuv"},
      {GST_VIDEO_FORMAT_RGBx, "GST_VIDEO_FORMAT_RGBx", "rgbx"},
      {GST_VIDEO_FORMAT_BGRx, "GST_VIDEO_FORMAT_BGRx", "bgrx"},
      {GST_VIDEO_FORMAT_xRGB, "GST_VIDEO_FORMAT_xRGB", "xrgb"},
      {GST_VIDEO_FORMAT_xBGR, "GST_VIDEO_FORMAT_xBGR", "xbgr"},
      {GST_VIDEO_FORMAT_RGBA, "GST_VIDEO_FORMAT_RGBA", "rgba"},
      {GST_VIDEO_FORMAT_BGRA, "GST_VIDEO_FORMAT_BGRA", "bgra"},
      {GST_VIDEO_FORMAT_ARGB, "GST_VIDEO_FORMAT_ARGB", "argb"},
      {GST_VIDEO_FORMAT_ABGR, "GST_VIDEO_FORMAT_ABGR", "abgr"},
      {GST_VIDEO_FORMAT_RGB, "GST_VIDEO_FORMAT_RGB", "rgb"},
      {GST_VIDEO_FORMAT_BGR, "GST_VIDEO_FORMAT_BGR", "bgr"},
      {GST_VIDEO_FORMAT_Y41B, "GST_VIDEO_FORMAT_Y41B", "y41b"},
      {GST_VIDEO_FORMAT_Y42B, "GST_VIDEO_FORMAT_Y42B", "y42b"},
      {GST_VIDEO_FORMAT_YVYU, "GST_VIDEO_FORMAT_YVYU", "yvyu"},
      {GST_VIDEO_FORMAT_Y444, "GST_VIDEO_FORMAT_Y444", "y444"},
      {GST_VIDEO_FORMAT_v210, "GST_VIDEO_FORMAT_v210", "v210"},
      {GST_VIDEO_FORMAT_v216, "GST_VIDEO_FORMAT_v216", "v216"},
      {GST_VIDEO_FORMAT_NV12, "GST_VIDEO_FORMAT_NV12", "nv12"},
      {GST_VIDEO_FORMAT_NV21, "GST_VIDEO_FORMAT_NV21", "nv21"},
      {GST_VIDEO_FORMAT_GRAY8, "GST_VIDEO_FORMAT_GRAY8", "gray8"},
      {GST_VIDEO_FORMAT_GRAY16_BE, "GST_VIDEO_FORMAT_GRAY16_BE", "gray16-be"},
      {GST_VIDEO_FORMAT_GRAY16_LE, "GST_VIDEO_FORMAT_GRAY16_LE", "gray16-le"},
      {GST_VIDEO_FORMAT_v308, "GST_VIDEO_FORMAT_v308", "v308"},
      {GST_VIDEO_FORMAT_Y800, "GST_VIDEO_FORMAT_Y800", "y800"},
      {GST_VIDEO_FORMAT_Y16, "GST_VIDEO_FORMAT_Y16", "y16"},
      {GST_VIDEO_FORMAT_RGB16, "GST_VIDEO_FORMAT_RGB16", "rgb16"},
      {GST_VIDEO_FORMAT_BGR16, "GST_VIDEO_FORMAT_BGR16", "bgr16"},
      {GST_VIDEO_FORMAT_RGB15, "GST_VIDEO_FORMAT_RGB15", "rgb15"},
      {GST_VIDEO_FORMAT_BGR15, "GST_VIDEO_FORMAT_BGR15", "bgr15"},
      {GST_VIDEO_FORMAT_UYVP, "GST_VIDEO_FORMAT_UYVP", "uyvp"},
      {GST_VIDEO_FORMAT_A420, "GST_VIDEO_FORMAT_A420", "a420"},
      {GST_VIDEO_FORMAT_RGB8_PALETTED, "GST_VIDEO_FORMAT_RGB8_PALETTED",
          "rgb8-paletted"},
      {GST_VIDEO_FORMAT_YUV9, "GST_VIDEO_FORMAT_YUV9", "yuv9"},
      {GST_VIDEO_FORMAT_YVU9, "GST_VIDEO_FORMAT_YVU9", "yvu9"},
      {GST_VIDEO_FORMAT_IYU1, "GST_VIDEO_FORMAT_IYU1", "iyu1"},
      {GST_VIDEO_FORMAT_ARGB64, "GST_VIDEO_FORMAT_ARGB64", "argb64"},
      {GST_VIDEO_FORMAT_AYUV64, "GST_VIDEO_FORMAT_AYUV64", "ayuv64"},
      {GST_VIDEO_FORMAT_r210, "GST_VIDEO_FORMAT_r210", "r210"},
      {0, NULL, NULL}
    };
    GType g_define_type_id = g_enum_register_static ("GstVideoFormat", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
