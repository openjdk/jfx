


#include "tag-enumtypes.h"

#include "tag.h"
#include "gsttagdemux.h"

/* enumerations from "gsttagdemux.h" */
GType
gst_tag_demux_result_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_TAG_DEMUX_RESULT_BROKEN_TAG, "GST_TAG_DEMUX_RESULT_BROKEN_TAG", "broken-tag" },
      { GST_TAG_DEMUX_RESULT_AGAIN, "GST_TAG_DEMUX_RESULT_AGAIN", "again" },
      { GST_TAG_DEMUX_RESULT_OK, "GST_TAG_DEMUX_RESULT_OK", "ok" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstTagDemuxResult", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}

/* enumerations from "tag.h" */
GType
gst_tag_image_type_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GEnumValue values[] = {
      { GST_TAG_IMAGE_TYPE_NONE, "GST_TAG_IMAGE_TYPE_NONE", "none" },
      { GST_TAG_IMAGE_TYPE_UNDEFINED, "GST_TAG_IMAGE_TYPE_UNDEFINED", "undefined" },
      { GST_TAG_IMAGE_TYPE_FRONT_COVER, "GST_TAG_IMAGE_TYPE_FRONT_COVER", "front-cover" },
      { GST_TAG_IMAGE_TYPE_BACK_COVER, "GST_TAG_IMAGE_TYPE_BACK_COVER", "back-cover" },
      { GST_TAG_IMAGE_TYPE_LEAFLET_PAGE, "GST_TAG_IMAGE_TYPE_LEAFLET_PAGE", "leaflet-page" },
      { GST_TAG_IMAGE_TYPE_MEDIUM, "GST_TAG_IMAGE_TYPE_MEDIUM", "medium" },
      { GST_TAG_IMAGE_TYPE_LEAD_ARTIST, "GST_TAG_IMAGE_TYPE_LEAD_ARTIST", "lead-artist" },
      { GST_TAG_IMAGE_TYPE_ARTIST, "GST_TAG_IMAGE_TYPE_ARTIST", "artist" },
      { GST_TAG_IMAGE_TYPE_CONDUCTOR, "GST_TAG_IMAGE_TYPE_CONDUCTOR", "conductor" },
      { GST_TAG_IMAGE_TYPE_BAND_ORCHESTRA, "GST_TAG_IMAGE_TYPE_BAND_ORCHESTRA", "band-orchestra" },
      { GST_TAG_IMAGE_TYPE_COMPOSER, "GST_TAG_IMAGE_TYPE_COMPOSER", "composer" },
      { GST_TAG_IMAGE_TYPE_LYRICIST, "GST_TAG_IMAGE_TYPE_LYRICIST", "lyricist" },
      { GST_TAG_IMAGE_TYPE_RECORDING_LOCATION, "GST_TAG_IMAGE_TYPE_RECORDING_LOCATION", "recording-location" },
      { GST_TAG_IMAGE_TYPE_DURING_RECORDING, "GST_TAG_IMAGE_TYPE_DURING_RECORDING", "during-recording" },
      { GST_TAG_IMAGE_TYPE_DURING_PERFORMANCE, "GST_TAG_IMAGE_TYPE_DURING_PERFORMANCE", "during-performance" },
      { GST_TAG_IMAGE_TYPE_VIDEO_CAPTURE, "GST_TAG_IMAGE_TYPE_VIDEO_CAPTURE", "video-capture" },
      { GST_TAG_IMAGE_TYPE_FISH, "GST_TAG_IMAGE_TYPE_FISH", "fish" },
      { GST_TAG_IMAGE_TYPE_ILLUSTRATION, "GST_TAG_IMAGE_TYPE_ILLUSTRATION", "illustration" },
      { GST_TAG_IMAGE_TYPE_BAND_ARTIST_LOGO, "GST_TAG_IMAGE_TYPE_BAND_ARTIST_LOGO", "band-artist-logo" },
      { GST_TAG_IMAGE_TYPE_PUBLISHER_STUDIO_LOGO, "GST_TAG_IMAGE_TYPE_PUBLISHER_STUDIO_LOGO", "publisher-studio-logo" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_enum_register_static ("GstTagImageType", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}
GType
gst_tag_license_flags_get_type (void)
{
  static volatile gsize g_define_type_id__volatile = 0;
  if (g_once_init_enter (&g_define_type_id__volatile)) {
    static const GFlagsValue values[] = {
      { GST_TAG_LICENSE_PERMITS_REPRODUCTION, "GST_TAG_LICENSE_PERMITS_REPRODUCTION", "permits-reproduction" },
      { GST_TAG_LICENSE_PERMITS_DISTRIBUTION, "GST_TAG_LICENSE_PERMITS_DISTRIBUTION", "permits-distribution" },
      { GST_TAG_LICENSE_PERMITS_DERIVATIVE_WORKS, "GST_TAG_LICENSE_PERMITS_DERIVATIVE_WORKS", "permits-derivative-works" },
      { GST_TAG_LICENSE_PERMITS_SHARING, "GST_TAG_LICENSE_PERMITS_SHARING", "permits-sharing" },
      { GST_TAG_LICENSE_REQUIRES_NOTICE, "GST_TAG_LICENSE_REQUIRES_NOTICE", "requires-notice" },
      { GST_TAG_LICENSE_REQUIRES_ATTRIBUTION, "GST_TAG_LICENSE_REQUIRES_ATTRIBUTION", "requires-attribution" },
      { GST_TAG_LICENSE_REQUIRES_SHARE_ALIKE, "GST_TAG_LICENSE_REQUIRES_SHARE_ALIKE", "requires-share-alike" },
      { GST_TAG_LICENSE_REQUIRES_SOURCE_CODE, "GST_TAG_LICENSE_REQUIRES_SOURCE_CODE", "requires-source-code" },
      { GST_TAG_LICENSE_REQUIRES_COPYLEFT, "GST_TAG_LICENSE_REQUIRES_COPYLEFT", "requires-copyleft" },
      { GST_TAG_LICENSE_REQUIRES_LESSER_COPYLEFT, "GST_TAG_LICENSE_REQUIRES_LESSER_COPYLEFT", "requires-lesser-copyleft" },
      { GST_TAG_LICENSE_PROHIBITS_COMMERCIAL_USE, "GST_TAG_LICENSE_PROHIBITS_COMMERCIAL_USE", "prohibits-commercial-use" },
      { GST_TAG_LICENSE_PROHIBITS_HIGH_INCOME_NATION_USE, "GST_TAG_LICENSE_PROHIBITS_HIGH_INCOME_NATION_USE", "prohibits-high-income-nation-use" },
      { GST_TAG_LICENSE_CREATIVE_COMMONS_LICENSE, "GST_TAG_LICENSE_CREATIVE_COMMONS_LICENSE", "creative-commons-license" },
      { GST_TAG_LICENSE_FREE_SOFTWARE_FOUNDATION_LICENSE, "GST_TAG_LICENSE_FREE_SOFTWARE_FOUNDATION_LICENSE", "free-software-foundation-license" },
      { 0, NULL, NULL }
    };
    GType g_define_type_id = g_flags_register_static ("GstTagLicenseFlags", values);
    g_once_init_leave (&g_define_type_id__volatile, g_define_type_id);
  }
  return g_define_type_id__volatile;
}



