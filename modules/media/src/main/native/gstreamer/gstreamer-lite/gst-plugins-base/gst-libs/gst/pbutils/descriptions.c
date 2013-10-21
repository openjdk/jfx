/* GStreamer Plugins Base utils library source/sink/codec description support
 * Copyright (C) 2006 Tim-Philipp Müller <tim centricular net>
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
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/**
 * SECTION:gstpbutilsdescriptions
 * @short_description: Provides human-readable descriptions for caps/codecs
 * and encoder, decoder, URI source and URI sink elements
 *
 * <refsect2>
 * <para>
 * The above functions provide human-readable strings for media formats
 * and decoder/demuxer/depayloader/encoder/muxer/payloader elements for use
 * in error dialogs or other messages shown to users.
 * </para>
 * <para>
 * gst_pb_utils_add_codec_description_to_tag_list() is a utility function
 * for demuxer and decoder elements to add audio/video codec tags from a
 * given (fixed) #GstCaps.
 * </para>
 * </refsect2>
 */

#ifdef HAVE_CONFIG_H
# include "config.h"
#endif

#include "gst/gst-i18n-plugin.h"

#include "pbutils.h"
#include "pbutils-private.h"

#include <string.h>

typedef enum
{
  FLAG_CONTAINER = (1 << 0),    /* format is a container format (muxed)             */
  FLAG_SYSTEMSTREAM = (1 << 1)  /* match record only if caps have systemstream=true */
} FormatFlags;

typedef struct
{
  const gchar *type;
  const gchar *desc;
  FormatFlags flags;
} FormatInfo;

#ifndef GSTREAMER_LITE
static const FormatInfo formats[] = {
  /* container/tag formats with static descriptions */
  {"application/gxf", "General Exchange Format (GXF)", FLAG_CONTAINER},
  {"application/ogg", "Ogg", FLAG_CONTAINER},
  {"application/mxf", "Material eXchange Format (MXF)", FLAG_CONTAINER},
  {"application/vnd.rn-realmedia", "Realmedia", FLAG_CONTAINER},
  {"application/x-annodex", "Ogg", FLAG_CONTAINER},
  {"application/x-id3", N_("ID3 tag"), FLAG_CONTAINER},
  {"application/x-ape", N_("APE tag"), FLAG_CONTAINER},
  {"application/x-apetag", N_("APE tag"), FLAG_CONTAINER},
  {"application/x-icy", N_("ICY internet radio"), FLAG_CONTAINER},
  {"application/x-3gp", "3GP", FLAG_CONTAINER},
  {"application/x-pn-realaudio", "RealAudio", FLAG_CONTAINER},
  {"application/x-yuv4mpeg", "Y4M", FLAG_CONTAINER},
  {"multipart/x-mixed-replace", "Multipart", FLAG_CONTAINER},
  {"video/x-fli", "FLI/FLC/FLX Animation", FLAG_CONTAINER},
  {"video/x-flv", "Flash", FLAG_CONTAINER},
  {"video/x-matroska", "Matroska", FLAG_CONTAINER},
  {"video/webm", "WebM", FLAG_CONTAINER},
  {"video/x-ms-asf", "Advanced Streaming Format (ASF)", FLAG_CONTAINER},
  {"video/x-msvideo", "Audio Video Interleave (AVI)", FLAG_CONTAINER},
  {"video/x-quicktime", "Quicktime", FLAG_CONTAINER},
  {"video/quicktime", "Quicktime", FLAG_CONTAINER},
  {"video/mj2", "Motion JPEG 2000", FLAG_CONTAINER},

  /* audio formats with static descriptions */
  {"audio/x-ac3", "AC-3 (ATSC A/52)", 0},
  {"audio/ac3", "AC-3 (ATSC A/52)", 0},
  {"audio/x-private-ac3", "DVD AC-3 (ATSC A/52)", 0},
  {"audio/x-private1-ac3", "DVD AC-3 (ATSC A/52)", 0},
  {"audio/x-alaw", "A-Law", 0},
  {"audio/amr", "Adaptive Multi Rate (AMR)", 0},
  {"audio/AMR", "Adaptive Multi Rate (AMR)", 0},
  {"audio/AMR-WB", "Adaptive Multi Rate WideBand (AMR-WB)", 0},
  {"audio/iLBC-sh", "Internet Low Bitrate Codec (iLBC)", 0},
  {"audio/ms-gsm", "MS GSM", 0},
  {"audio/qcelp", "QCELP", 0},
  {"audio/aiff", "Audio Interchange File Format (AIFF)", 0},
  {"audio/x-aiff", "Audio Interchange File Format (AIFF)", 0},
  {"audio/x-alac", N_("Apple Lossless Audio (ALAC)"), 0},
  {"audio/x-amr-nb-sh", "Adaptive Multi Rate NarrowBand (AMR-NB)", 0},
  {"audio/x-amr-wb-sh", "Adaptive Multi Rate WideBand (AMR-WB)", 0},
  {"audio/x-au", "Sun .au", 0},
  {"audio/x-celt", "Constrained Energy Lapped Transform (CELT)", 0},
  {"audio/x-cinepak", "Cinepak Audio", 0},
  {"audio/x-dpcm", "DPCM", 0},
  {"audio/x-dts", "DTS", 0},
  {"audio/x-private1-dts", "DTS", 0},
  {"audio/x-dv", "DV Audio", 0},
  {"audio/x-flac", N_("Free Lossless Audio Codec (FLAC)"), 0},
  {"audio/x-gsm", "GSM", 0},
  {"audio/x-iec958", "S/PDIF IEC958", 0},       /* TODO: check description */
  {"audio/x-iLBC", "Internet Low Bitrate Codec (iLBC)", 0},
  {"audio/x-ircam", "Berkeley/IRCAM/CARL", 0},
  {"audio/x-lpcm", "LPCM", 0},
  {"audio/x-private1-lpcm", "DVD LPCM", 0},
  {"audio/x-m4a", "MPEG-4 AAC", FLAG_CONTAINER},
  {"audio/x-mod", "Module Music Format (MOD)", 0},
  {"audio/x-mulaw", "Mu-Law", 0},
  {"audio/x-musepack", "Musepack (MPC)", 0},
  {"audio/x-nellymoser", "Nellymoser Asao", 0},
  {"audio/x-nist", "Sphere NIST", 0},
  {"audio/x-nsf", "Nintendo NSF", 0},
  {"audio/x-paris", "Ensoniq PARIS", 0},
  {"audio/x-qdm", "QDesign Music (QDM)", 0},
  {"audio/x-qdm2", "QDesign Music (QDM) 2", 0},
  {"audio/x-ralf-mpeg4-generic", "Real Audio Lossless (RALF)", 0},
  {"audio/x-sds", "SDS", 0},
  {"audio/x-shorten", "Shorten Lossless", 0},
  {"audio/x-sid", "Sid", 0},
  {"audio/x-sipro", "Sipro/ACELP.NET Voice", 0},
  {"audio/x-siren", "Siren", 0},
  {"audio/x-spc", "SNES-SPC700 Sound File Data", 0},
  {"audio/x-speex", "Speex", 0},
  {"audio/x-svx", "Amiga IFF / SVX8 / SV16", 0},
  {"audio/x-tta", N_("Lossless True Audio (TTA)"), 0},
  {"audio/x-ttafile", N_("Lossless True Audio (TTA)"), 0},
  {"audio/x-vnd.sony.atrac3", "Sony ATRAC3", 0},
  {"audio/x-vorbis", "Vorbis", 0},
  {"audio/x-voc", "SoundBlaster VOC", 0},
  {"audio/x-w64", "Sonic Foundry Wave64", 0},
  {"audio/x-wav", "WAV", 0},
  {"audio/x-wavpack", "Wavpack", 0},
  {"audio/x-wavpack-correction", "Wavpack", 0},
  {"audio/x-wms", N_("Windows Media Speech"), 0},
  {"audio/x-voxware", "Voxware", 0},


  /* video formats with static descriptions */
  {"video/sp5x", "Sunplus JPEG 5.x", 0},
  {"video/vivo", "Vivo", 0},
  {"video/x-3ivx", "3ivx", 0},
  {"video/x-4xm", "4X Techologies Video", 0},
  {"video/x-apple-video", "Apple video", 0},
  {"video/x-aasc", "Autodesk Animator", 0},
  {"video/x-camtasia", "TechSmith Camtasia", 0},
  {"video/x-cdxa", "RIFF/CDXA (VCD)", 0},
  {"video/x-cinepak", "Cinepak Video", 0},
  {"video/x-cirrus-logic-accupak", "Cirrus Logipak AccuPak", 0},
  {"video/x-compressed-yuv", N_("CYUV Lossless"), 0},
  {"video/x-dirac", "Dirac", 0},
  {"video/x-dnxhd", "Digital Nonlinear Extensible High Definition (DNxHD)", 0},
  /* FIXME 0.11: rename to subpicture/x-dvd or so */
  {"video/x-dvd-subpicture", "DVD subpicture", 0},
  {"video/x-ffv", N_("FFMpeg v1"), 0},
  {"video/x-flash-screen", "Flash Screen Video", 0},
  {"video/x-flash-video", "Sorenson Spark Video", 0},
  {"video/x-h261", "H.261", 0},
  {"video/x-huffyuv", "Huffyuv", 0},
  {"video/x-intel-h263", "Intel H.263", 0},
  {"video/x-jpeg", "Motion JPEG", 0},
  /* { "video/x-jpeg-b", "", 0 }, does this actually exist? */
  {"video/x-loco", "LOCO Lossless", 0},
  {"video/x-mimic", "MIMIC", 0},
  {"video/x-mjpeg", "Motion-JPEG", 0},
  {"video/x-mjpeg-b", "Motion-JPEG format B", 0},
  {"video/mpegts", "MPEG-2 Transport Stream", FLAG_CONTAINER},
  {"video/x-mng", "Multiple Image Network Graphics (MNG)", 0},
  {"video/x-mszh", N_("Lossless MSZH"), 0},
  {"video/x-msvideocodec", "Microsoft Video 1", 0},
  {"video/x-mve", "Interplay MVE", FLAG_CONTAINER},
  {"video/x-nut", "NUT", FLAG_CONTAINER},
  {"video/x-nuv", "MythTV NuppelVideo (NUV)", FLAG_CONTAINER},
  {"video/x-qdrw", "Apple QuickDraw", 0},
  {"video/x-raw-gray", N_("Uncompressed Gray Image"), 0},
  {"video/x-smc", "Apple SMC", 0},
  {"video/x-smoke", "Smoke", 0},
  {"video/x-tarkin", "Tarkin", 0},
  {"video/x-theora", "Theora", 0},
  {"video/x-rle", N_("Run-length encoding"), 0},
  {"video/x-ultimotion", "IBM UltiMotion", 0},
  {"video/x-vcd", "VideoCD (VCD)", 0},
  {"video/x-vmnc", "VMWare NC", 0},
  {"video/x-vp3", "On2 VP3", 0},
  {"video/x-vp5", "On2 VP5", 0},
  {"video/x-vp6", "On2 VP6", 0},
  {"video/x-vp6-flash", "On2 VP6/Flash", 0},
  {"video/x-vp6-alpha", "On2 VP6 with alpha", 0},
  {"video/x-vp7", "On2 VP7", 0},
  {"video/x-vp8", "VP8", 0},
  {"video/x-xvid", "XVID MPEG-4", 0},
  {"video/x-zlib", "Lossless zlib video", 0},
  {"video/x-zmbv", "Zip Motion Block video", 0},

  /* image formats with static descriptions */
  {"image/bmp", "BMP", 0},
  {"image/x-bmp", "BMP", 0},
  {"image/x-MS-bmp", "BMP", 0},
  {"image/gif", "GIF", 0},
  {"image/jpeg", "JPEG", 0},
  {"image/jng", "JPEG Network Graphics (JNG)", 0},
  {"image/png", "PNG", 0},
  {"image/pbm", "Portable BitMap (PBM)", 0},
  {"image/ppm", "Portable PixMap (PPM)", 0},
  {"image/svg+xml", "Scalable Vector Graphics (SVG)", 0},
  {"image/tiff", "TIFF", 0},
  {"image/x-cmu-raster", "CMU Raster Format", 0},
  {"image/x-degas", "DEGAS", 0},
  {"image/x-icon", "ICO", 0},
  {"image/x-j2c", "JPEG 2000", 0},
  {"image/x-jpc", "JPEG 2000", 0},
  {"image/jp2", "JPEG 2000", 0},
  {"image/x-pcx", "PCX", 0},
  {"image/x-xcf", "XFC", 0},
  {"image/x-pixmap", "XPM", 0},
  {"image/x-portable-anymap", "Portable AnyMap (PAM)", 0},
  {"image/x-portable-graymap", "Portable GrayMap (PGM)", 0},
  {"image/x-xpixmap", "XPM", 0},
  {"image/x-quicktime", "QuickTime Image Format (QTIF)", 0},
  {"image/x-sun-raster", "Sun Raster Format (RAS)", 0},
  {"image/x-tga", "TGA", 0},

  /* subtitle formats with static descriptions */
  {"application/x-ass", "ASS", 0},
  {"application/x-subtitle-sami", N_("Sami subtitle format"), 0},
  {"application/x-subtitle-tmplayer", N_("TMPlayer subtitle format"), 0},
  {"application/x-kate", "Kate", 0},
  {"subtitle/x-kate", N_("Kate subtitle format"), 0},
  {"subpicture/x-dvb", "DVB subtitles", 0},
  /* add variant field to typefinder? { "application/x-subtitle", N_("subtitle"), 0}, */

  /* non-audio/video/container formats */
  {"hdv/aux-v", "HDV AUX-V", 0},
  {"hdv/aux-a", "HDV AUX-A", 0},

  /* formats with dynamic descriptions */
  {"audio/mpeg", NULL, 0},
  {"audio/x-adpcm", NULL, 0},
  {"audio/x-mace", NULL, 0},
  {"audio/x-pn-realaudio", NULL, 0},
  {"audio/x-raw-int", NULL, 0},
  {"audio/x-raw-float", NULL, 0},
  {"audio/x-wma", NULL, 0},
  {"video/mpeg", NULL, FLAG_CONTAINER | FLAG_SYSTEMSTREAM},
  {"video/mpeg", NULL, 0},
  {"video/x-asus", NULL, 0},
  {"video/x-ati-vcr", NULL, 0},
  {"video/x-divx", NULL, 0},
  {"video/x-dv", "Digital Video (DV) System Stream",
      FLAG_CONTAINER | FLAG_SYSTEMSTREAM},
  {"video/x-dv", "Digital Video (DV)", 0},
  {"video/x-h263", NULL, 0},
  {"video/x-h264", NULL, 0},
  {"video/x-indeo", NULL, 0},
  {"video/x-msmpeg", NULL, 0},
  {"video/x-pn-realvideo", NULL, 0},
#if 0
  /* do these exist? are they used anywhere? */
  {"video/x-pn-multirate-realvideo", NULL, 0},
  {"audio/x-pn-multirate-realaudio", NULL, 0},
  {"audio/x-pn-multirate-realaudio-live", NULL, 0},
#endif
  {"video/x-truemotion", NULL, 0},
  {"video/x-raw-rgb", NULL, 0},
  {"video/x-raw-yuv", NULL, 0},
  {"video/x-svq", NULL, 0},
  {"video/x-wmv", NULL, 0},
  {"video/x-xan", NULL, 0}
};
#else // GSTREAMER_LITE
static const FormatInfo formats[] = {
  /* container/tag formats with static descriptions */
  {"application/x-id3", N_("ID3 tag"), FLAG_CONTAINER},
  {"video/x-flv", "Flash", FLAG_CONTAINER},

  /* audio formats with static descriptions */
  {"audio/aiff", "Audio Interchange File Format (AIFF)", 0},
  {"audio/x-aiff", "Audio Interchange File Format (AIFF)", 0},
  {"audio/x-wav", "WAV", 0},

  /* video formats with static descriptions */
  {"video/x-vp6", "On2 VP6", 0},

  /* image formats with static descriptions */

  /* subtitle formats with static descriptions */

  /* non-audio/video/container formats */

  /* formats with dynamic descriptions */
  {"audio/mpeg", NULL, 0},
  {"audio/x-raw-int", NULL, 0},
  {"audio/x-raw-float", NULL, 0}
};
#endif // GSTREAMER_LITE

/* returns static descriptions and dynamic ones (such as video/x-raw-yuv),
 * or NULL if caps aren't known at all */
static gchar *
format_info_get_desc (const FormatInfo * info, const GstCaps * caps)
{
  const GstStructure *s;

  g_assert (info != NULL);

  if (info->desc != NULL)
    return g_strdup (_(info->desc));

  s = gst_caps_get_structure (caps, 0);

  if (strcmp (info->type, "video/x-raw-yuv") == 0) {
    const gchar *ret = NULL;
    guint32 fourcc = 0;

    gst_structure_get_fourcc (s, "format", &fourcc);
    switch (fourcc) {
      case GST_MAKE_FOURCC ('I', '4', '2', '0'):
        ret = _("Uncompressed planar YUV 4:2:0");
        break;
      case GST_MAKE_FOURCC ('Y', 'V', '1', '2'):
        ret = _("Uncompressed planar YVU 4:2:0");
        break;
      case GST_MAKE_FOURCC ('Y', 'U', 'Y', '2'):
        ret = _("Uncompressed packed YUV 4:2:2");
        break;
      case GST_MAKE_FOURCC ('Y', 'U', 'V', '9'):
        ret = _("Uncompressed packed YUV 4:1:0");
        break;
      case GST_MAKE_FOURCC ('Y', 'V', 'U', '9'):
        ret = _("Uncompressed packed YVU 4:1:0");
        break;
      case GST_MAKE_FOURCC ('Y', 'V', 'Y', 'U'):
      case GST_MAKE_FOURCC ('U', 'Y', 'V', 'Y'):
        ret = _("Uncompressed packed YUV 4:2:2");
        break;
      case GST_MAKE_FOURCC ('Y', '4', '1', 'P'):
        ret = _("Uncompressed packed YUV 4:1:1");
        break;
      case GST_MAKE_FOURCC ('I', 'Y', 'U', '2'):
        ret = _("Uncompressed packed YUV 4:4:4");
        break;
      case GST_MAKE_FOURCC ('Y', '4', '2', 'B'):
        ret = _("Uncompressed planar YUV 4:2:2");
        break;
      case GST_MAKE_FOURCC ('Y', '4', '1', 'B'):
        ret = _("Uncompressed planar YUV 4:1:1");
        break;
      case GST_MAKE_FOURCC ('Y', '8', '0', '0'):
        ret = _("Uncompressed black and white Y-plane");
        break;
      default:
        ret = _("Uncompressed YUV");
        break;
    }
    return g_strdup (ret);
  } else if (strcmp (info->type, "video/x-raw-rgb") == 0) {
    const gchar *rgb_str;
    gint depth = 0;

    gst_structure_get_int (s, "depth", &depth);
    rgb_str = gst_structure_has_field (s, "alpha_mask") ? "RGBA" : "RGB";
    if (gst_structure_has_field (s, "paletted_data")) {
      return g_strdup_printf (_("Uncompressed palettized %d-bit %s"), depth,
          rgb_str);
    } else {
      return g_strdup_printf ("Uncompressed %d-bit %s", depth, rgb_str);
    }
  } else if (strcmp (info->type, "video/x-h263") == 0) {
    const gchar *variant, *ret;

    variant = gst_structure_get_string (s, "variant");
    if (variant == NULL)
      ret = "H.263";
    else if (strcmp (variant, "itu") == 0)
      ret = "ITU H.26n";        /* why not ITU H.263? (tpm) */
    else if (strcmp (variant, "lead") == 0)
      ret = "Lead H.263";
    else if (strcmp (variant, "microsoft") == 0)
      ret = "Microsoft H.263";
    else if (strcmp (variant, "vdolive") == 0)
      ret = "VDOLive";
    else if (strcmp (variant, "vivo") == 0)
      ret = "Vivo H.263";
    else if (strcmp (variant, "xirlink") == 0)
      ret = "Xirlink H.263";
    else {
      GST_WARNING ("Unknown H263 variant '%s'", variant);
      ret = "H.263";
    }
    return g_strdup (ret);
  } else if (strcmp (info->type, "video/x-h264") == 0) {
    const gchar *variant, *ret;

    variant = gst_structure_get_string (s, "variant");
    if (variant == NULL)
      ret = "H.264";
    else if (strcmp (variant, "itu") == 0)
      ret = "ITU H.264";
    else if (strcmp (variant, "videosoft") == 0)
      ret = "Videosoft H.264";
    else if (strcmp (variant, "lead") == 0)
      ret = "Lead H.264";
    else {
      GST_WARNING ("Unknown H264 variant '%s'", variant);
      ret = "H.264";
    }
    return g_strdup (ret);
  } else if (strcmp (info->type, "video/x-divx") == 0) {
    gint ver = 0;

    if (!gst_structure_get_int (s, "divxversion", &ver) || ver <= 2) {
      GST_WARNING ("Unexpected DivX version in %" GST_PTR_FORMAT, caps);
      return g_strdup ("DivX MPEG-4");
    }
    return g_strdup_printf (_("DivX MPEG-4 Version %d"), ver);
  } else if (strcmp (info->type, "video/x-msmpeg") == 0) {
    gint ver = 0;

    if (!gst_structure_get_int (s, "msmpegversion", &ver) ||
        ver < 40 || ver > 49) {
      GST_WARNING ("Unexpected msmpegversion in %" GST_PTR_FORMAT, caps);
      return g_strdup ("Microsoft MPEG-4 4.x");
    }
    return g_strdup_printf ("Microsoft MPEG-4 4.%d", ver % 10);
  } else if (strcmp (info->type, "video/x-truemotion") == 0) {
    gint ver = 0;

    gst_structure_get_int (s, "trueversion", &ver);
    switch (ver) {
      case 1:
        return g_strdup_printf ("Duck TrueMotion 1");
      case 2:
        return g_strdup_printf ("TrueMotion 2.0");
      default:
        GST_WARNING ("Unexpected trueversion in %" GST_PTR_FORMAT, caps);
        break;
    }
    return g_strdup_printf ("TrueMotion");
  } else if (strcmp (info->type, "video/x-xan") == 0) {
    gint ver = 0;

    if (!gst_structure_get_int (s, "wcversion", &ver) || ver < 1) {
      GST_WARNING ("Unexpected wcversion in %" GST_PTR_FORMAT, caps);
      return g_strdup ("Xan Wing Commander");
    }
    return g_strdup_printf ("Xan Wing Commander %u", ver);
  } else if (strcmp (info->type, "video/x-indeo") == 0) {
    gint ver = 0;

    if (!gst_structure_get_int (s, "indeoversion", &ver) || ver < 2) {
      GST_WARNING ("Unexpected indeoversion in %" GST_PTR_FORMAT, caps);
      return g_strdup ("Intel Indeo");
    }
    return g_strdup_printf ("Intel Indeo %u", ver);
  } else if (strcmp (info->type, "audio/x-wma") == 0) {
    gint ver = 0;

    gst_structure_get_int (s, "wmaversion", &ver);
    switch (ver) {
      case 1:
      case 2:
      case 3:
        return g_strdup_printf ("Windows Media Audio %d", ver + 6);
      default:
        break;
    }
    GST_WARNING ("Unexpected wmaversion in %" GST_PTR_FORMAT, caps);
    return g_strdup ("Windows Media Audio");
  } else if (strcmp (info->type, "video/x-wmv") == 0) {
    gint ver = 0;

    gst_structure_get_int (s, "wmvversion", &ver);
    switch (ver) {
      case 1:
      case 2:
      case 3:
        return g_strdup_printf ("Windows Media Video %d", ver + 6);
      default:
        break;
    }
    GST_WARNING ("Unexpected wmvversion in %" GST_PTR_FORMAT, caps);
    return g_strdup ("Windows Media Video");
  } else if (strcmp (info->type, "audio/x-mace") == 0) {
    gint ver = 0;

    gst_structure_get_int (s, "maceversion", &ver);
    if (ver == 3 || ver == 6) {
      return g_strdup_printf ("MACE-%d", ver);
    } else {
      GST_WARNING ("Unexpected maceversion in %" GST_PTR_FORMAT, caps);
      return g_strdup ("MACE");
    }
  } else if (strcmp (info->type, "video/x-svq") == 0) {
    gint ver = 0;

    gst_structure_get_int (s, "svqversion", &ver);
    if (ver == 1 || ver == 3) {
      return g_strdup_printf ("Sorensen Video %d", ver);
    } else {
      GST_WARNING ("Unexpected svqversion in %" GST_PTR_FORMAT, caps);
      return g_strdup ("Sorensen Video");
    }
  } else if (strcmp (info->type, "video/x-asus") == 0) {
    gint ver = 0;

    gst_structure_get_int (s, "asusversion", &ver);
    if (ver == 1 || ver == 2) {
      return g_strdup_printf ("Asus Video %d", ver);
    } else {
      GST_WARNING ("Unexpected asusversion in %" GST_PTR_FORMAT, caps);
      return g_strdup ("Asus Video");
    }
  } else if (strcmp (info->type, "video/x-ati-vcr") == 0) {
    gint ver = 0;

    gst_structure_get_int (s, "vcrversion", &ver);
    if (ver == 1 || ver == 2) {
      return g_strdup_printf ("ATI VCR %d", ver);
    } else {
      GST_WARNING ("Unexpected acrversion in %" GST_PTR_FORMAT, caps);
      return g_strdup ("ATI VCR");
    }
  } else if (strcmp (info->type, "audio/x-adpcm") == 0) {
    const GValue *layout_val;

    layout_val = gst_structure_get_value (s, "layout");
    if (layout_val != NULL && G_VALUE_HOLDS_STRING (layout_val)) {
      const gchar *layout;

      if ((layout = g_value_get_string (layout_val))) {
        gchar *layout_upper, *ret;

        if (strcmp (layout, "swf") == 0)
          return g_strdup ("Shockwave ADPCM");
        if (strcmp (layout, "microsoft") == 0)
          return g_strdup ("Microsoft ADPCM");
        if (strcmp (layout, "quicktime") == 0)
          return g_strdup ("Quicktime ADPCM");
        if (strcmp (layout, "westwood") == 0)
          return g_strdup ("Westwood ADPCM");
        if (strcmp (layout, "yamaha") == 0)
          return g_strdup ("Yamaha ADPCM");
        /* FIXME: other layouts: sbpro2, sbpro3, sbpro4, ct, g726, ea,
         * adx, xa, 4xm, smjpeg, dk4, dk3, dvi */
        layout_upper = g_ascii_strup (layout, -1);
        ret = g_strdup_printf ("%s ADPCM", layout_upper);
        g_free (layout_upper);
        return ret;
      }
    }
    return g_strdup ("ADPCM");
  } else if (strcmp (info->type, "audio/mpeg") == 0) {
    gint ver = 0, layer = 0;

    gst_structure_get_int (s, "mpegversion", &ver);

    switch (ver) {
      case 1:
        gst_structure_get_int (s, "layer", &layer);
        switch (layer) {
          case 1:
          case 2:
          case 3:
            return g_strdup_printf ("MPEG-1 Layer %d (MP%d)", layer, layer);
          default:
            break;
        }
        GST_WARNING ("Unexpected MPEG-1 layer in %" GST_PTR_FORMAT, caps);
        return g_strdup ("MPEG-1 Audio");
      case 4:
        return g_strdup ("MPEG-4 AAC");
      default:
        break;
    }
    GST_WARNING ("Unexpected audio mpegversion in %" GST_PTR_FORMAT, caps);
    return g_strdup ("MPEG Audio");
  } else if (strcmp (info->type, "audio/x-pn-realaudio") == 0) {
    gint ver = 0;

    gst_structure_get_int (s, "raversion", &ver);
    switch (ver) {
      case 1:
        return g_strdup ("RealAudio 14k4bps");
      case 2:
        return g_strdup ("RealAudio 28k8bps");
      case 8:
        return g_strdup ("RealAudio G2 (Cook)");
      default:
        break;
    }
    GST_WARNING ("Unexpected raversion in %" GST_PTR_FORMAT, caps);
    return g_strdup ("RealAudio");
  } else if (strcmp (info->type, "video/x-pn-realvideo") == 0) {
    gint ver = 0;

    gst_structure_get_int (s, "rmversion", &ver);
    switch (ver) {
      case 1:
        return g_strdup ("RealVideo 1.0");
      case 2:
        return g_strdup ("RealVideo 2.0");
      case 3:
        return g_strdup ("RealVideo 3.0");
      case 4:
        return g_strdup ("RealVideo 4.0");
      default:
        break;
    }
    GST_WARNING ("Unexpected rmversion in %" GST_PTR_FORMAT, caps);
    return g_strdup ("RealVideo");
  } else if (strcmp (info->type, "video/mpeg") == 0) {
    gboolean sysstream;
    gint ver = 0;

    if (!gst_structure_get_boolean (s, "systemstream", &sysstream) ||
        !gst_structure_get_int (s, "mpegversion", &ver) || ver < 1 || ver > 4) {
      GST_WARNING ("Missing fields in mpeg video caps %" GST_PTR_FORMAT, caps);
    } else {
      if (sysstream) {
        return g_strdup_printf ("MPEG-%d System Stream", ver);
      } else {
        return g_strdup_printf ("MPEG-%d Video", ver);
      }
    }
    return g_strdup ("MPEG Video");
  } else if (strcmp (info->type, "audio/x-raw-int") == 0) {
    gint bitdepth = 0;

    /* 8-bit pcm might not have depth field (?) */
    if (!gst_structure_get_int (s, "depth", &bitdepth))
      gst_structure_get_int (s, "width", &bitdepth);
    if (bitdepth != 0)
      return g_strdup_printf (_("Raw %d-bit PCM audio"), bitdepth);
    else
      return g_strdup (_("Raw PCM audio"));
  } else if (strcmp (info->type, "audio/x-raw-float") == 0) {
    gint bitdepth = 0;

    gst_structure_get_int (s, "width", &bitdepth);
    if (bitdepth != 0)
      return g_strdup_printf (_("Raw %d-bit floating-point audio"), bitdepth);
    else
      return g_strdup (_("Raw floating-point audio"));
  }

  return NULL;
}

/* returns format info structure, will return NULL for dynamic media types! */
static const FormatInfo *
find_format_info (const GstCaps * caps)
{
  const GstStructure *s;
  const gchar *media_type;
  guint i;

  s = gst_caps_get_structure (caps, 0);
  media_type = gst_structure_get_name (s);

  for (i = 0; i < G_N_ELEMENTS (formats); ++i) {
    if (strcmp (media_type, formats[i].type) == 0) {
      gboolean is_sys = FALSE;

      if ((formats[i].flags & FLAG_SYSTEMSTREAM) == 0)
        return &formats[i];

      /* this record should only be matched if the systemstream field is set */
      if (gst_structure_get_boolean (s, "systemstream", &is_sys) && is_sys)
        return &formats[i];
    }
  }

  return NULL;
}

static gboolean
caps_are_rtp_caps (const GstCaps * caps, const gchar * media, gchar ** format)
{
  const GstStructure *s;
  const gchar *str;

  g_assert (media != NULL && format != NULL);

  s = gst_caps_get_structure (caps, 0);
  if (!gst_structure_has_name (s, "application/x-rtp"))
    return FALSE;
  if (!gst_structure_has_field_typed (s, "media", G_TYPE_STRING))
    return FALSE;
  str = gst_structure_get_string (s, "media");
  if (str == NULL || !g_str_equal (str, media))
    return FALSE;
  str = gst_structure_get_string (s, "encoding-name");
  if (str == NULL || *str == '\0')
    return FALSE;

  if (strcmp (str, "X-ASF-PF") == 0) {
    *format = g_strdup ("Windows Media");
  } else if (g_str_has_prefix (str, "X-")) {
    *format = g_strdup (str + 2);
  } else {
    *format = g_strdup (str);
  }

  return TRUE;
}

/**
 * gst_pb_utils_get_source_description:
 * @protocol: the protocol the source element needs to handle, e.g. "http"
 *
 * Returns a localised string describing a source element handling the protocol
 * specified in @protocol, for use in error dialogs or other messages to be
 * seen by the user. Should never return NULL unless @protocol is invalid.
 *
 * This function is mainly for internal use, applications would typically
 * use gst_missing_plugin_message_get_description() to get a description of
 * a missing feature from a missing-plugin message.
 *
 * Returns: a newly-allocated description string, or NULL on error. Free
 *          string with g_free() when not needed any longer.
 */
gchar *
gst_pb_utils_get_source_description (const gchar * protocol)
{
  gchar *proto_uc, *ret;

  g_return_val_if_fail (protocol != NULL, NULL);

  if (strcmp (protocol, "cdda") == 0)
    return g_strdup (_("Audio CD source"));

  if (strcmp (protocol, "dvd") == 0)
    return g_strdup (_("DVD source"));

  if (strcmp (protocol, "rtsp") == 0)
    return g_strdup (_("Real Time Streaming Protocol (RTSP) source"));

  /* TODO: what about mmst, mmsu, mmsh? */
  if (strcmp (protocol, "mms") == 0)
    return g_strdup (_("Microsoft Media Server (MMS) protocol source"));

  /* make protocol uppercase */
  proto_uc = g_ascii_strup (protocol, -1);

  /* TODO: find out how to add a comment for translators to the source code
   * (and tell them to make the first letter uppercase below if they move
   * the protocol to the middle or end of the string) */
  ret = g_strdup_printf (_("%s protocol source"), proto_uc);

  g_free (proto_uc);

  return ret;
}

/**
 * gst_pb_utils_get_sink_description:
 * @protocol: the protocol the sink element needs to handle, e.g. "http"
 *
 * Returns a localised string describing a sink element handling the protocol
 * specified in @protocol, for use in error dialogs or other messages to be
 * seen by the user. Should never return NULL unless @protocol is invalid.
 *
 * This function is mainly for internal use, applications would typically
 * use gst_missing_plugin_message_get_description() to get a description of
 * a missing feature from a missing-plugin message.
 *
 * Returns: a newly-allocated description string, or NULL on error. Free
 *          string with g_free() when not needed any longer.
 */
gchar *
gst_pb_utils_get_sink_description (const gchar * protocol)
{
  gchar *proto_uc, *ret;

  g_return_val_if_fail (protocol != NULL, NULL);

  /* make protocol uppercase */
  proto_uc = g_ascii_strup (protocol, -1);

  /* TODO: find out how to add a comment for translators to the source code
   * (and tell them to make the first letter uppercase below if they move
   * the protocol to the middle or end of the string) */
  ret = g_strdup_printf ("%s protocol sink", proto_uc);

  g_free (proto_uc);

  return ret;
}

/**
 * gst_pb_utils_get_decoder_description:
 * @caps: the (fixed) #GstCaps for which an decoder description is needed
 *
 * Returns a localised string describing an decoder for the format specified
 * in @caps, for use in error dialogs or other messages to be seen by the user.
 * Should never return NULL unless @factory_name or @caps are invalid.
 *
 * This function is mainly for internal use, applications would typically
 * use gst_missing_plugin_message_get_description() to get a description of
 * a missing feature from a missing-plugin message.
 *
 * Returns: a newly-allocated description string, or NULL on error. Free
 *          string with g_free() when not needed any longer.
 */
gchar *
gst_pb_utils_get_decoder_description (const GstCaps * caps)
{
  gchar *str, *ret;
  GstCaps *tmp;

  g_return_val_if_fail (caps != NULL, NULL);
  g_return_val_if_fail (GST_IS_CAPS (caps), NULL);

  tmp = copy_and_clean_caps (caps);

  g_return_val_if_fail (gst_caps_is_fixed (tmp), NULL);

  /* special-case RTP caps */
  if (caps_are_rtp_caps (tmp, "video", &str)) {
    ret = g_strdup_printf (_("%s video RTP depayloader"), str);
  } else if (caps_are_rtp_caps (tmp, "audio", &str)) {
    ret = g_strdup_printf (_("%s audio RTP depayloader"), str);
  } else if (caps_are_rtp_caps (tmp, "application", &str)) {
    ret = g_strdup_printf (_("%s RTP depayloader"), str);
  } else {
    const FormatInfo *info;

    str = gst_pb_utils_get_codec_description (tmp);
    info = find_format_info (tmp);
    if (info != NULL && (info->flags & FLAG_CONTAINER) != 0) {
      ret = g_strdup_printf (_("%s demuxer"), str);
    } else {
      ret = g_strdup_printf (_("%s decoder"), str);
    }
  }

  g_free (str);
  gst_caps_unref (tmp);

  return ret;
}

/**
 * gst_pb_utils_get_encoder_description:
 * @caps: the (fixed) #GstCaps for which an encoder description is needed
 *
 * Returns a localised string describing an encoder for the format specified
 * in @caps, for use in error dialogs or other messages to be seen by the user.
 * Should never return NULL unless @factory_name or @caps are invalid.
 *
 * This function is mainly for internal use, applications would typically
 * use gst_missing_plugin_message_get_description() to get a description of
 * a missing feature from a missing-plugin message.
 *
 * Returns: a newly-allocated description string, or NULL on error. Free
 *          string with g_free() when not needed any longer.
 */
gchar *
gst_pb_utils_get_encoder_description (const GstCaps * caps)
{
  gchar *str, *ret;
  GstCaps *tmp;

  g_return_val_if_fail (caps != NULL, NULL);
  g_return_val_if_fail (GST_IS_CAPS (caps), NULL);
  tmp = copy_and_clean_caps (caps);
  g_return_val_if_fail (gst_caps_is_fixed (tmp), NULL);

  /* special-case RTP caps */
  if (caps_are_rtp_caps (tmp, "video", &str)) {
    ret = g_strdup_printf (_("%s video RTP payloader"), str);
  } else if (caps_are_rtp_caps (tmp, "audio", &str)) {
    ret = g_strdup_printf (_("%s audio RTP payloader"), str);
  } else if (caps_are_rtp_caps (tmp, "application", &str)) {
    ret = g_strdup_printf (_("%s RTP payloader"), str);
  } else {
    const FormatInfo *info;

    str = gst_pb_utils_get_codec_description (tmp);
    info = find_format_info (tmp);
    if (info != NULL && (info->flags & FLAG_CONTAINER) != 0) {
      ret = g_strdup_printf (_("%s muxer"), str);
    } else {
      ret = g_strdup_printf (_("%s encoder"), str);
    }
  }

  g_free (str);
  gst_caps_unref (tmp);

  return ret;
}

/**
 * gst_pb_utils_get_element_description:
 * @factory_name: the name of the element, e.g. "gnomevfssrc"
 *
 * Returns a localised string describing the given element, for use in
 * error dialogs or other messages to be seen by the user. Should never
 * return NULL unless @factory_name is invalid.
 *
 * This function is mainly for internal use, applications would typically
 * use gst_missing_plugin_message_get_description() to get a description of
 * a missing feature from a missing-plugin message.
 *
 * Returns: a newly-allocated description string, or NULL on error. Free
 *          string with g_free() when not needed any longer.
 */
gchar *
gst_pb_utils_get_element_description (const gchar * factory_name)
{
  gchar *ret;

  g_return_val_if_fail (factory_name != NULL, NULL);

  ret = g_strdup_printf (_("GStreamer element %s"), factory_name);
  if (ret && g_str_has_prefix (ret, factory_name))
    *ret = g_ascii_toupper (*ret);

  return ret;
}

/**
 * gst_pb_utils_add_codec_description_to_tag_list:
 * @taglist: a #GstTagList
 * @codec_tag: a GStreamer codec tag such as #GST_TAG_AUDIO_CODEC,
 *             #GST_TAG_VIDEO_CODEC or #GST_TAG_CODEC
 * @caps: the (fixed) #GstCaps for which a codec tag should be added.
 *
 * Adds a codec tag describing the format specified by @caps to @taglist.
 *
 * Returns: TRUE if a codec tag was added, FALSE otherwise.
 */
gboolean
gst_pb_utils_add_codec_description_to_tag_list (GstTagList * taglist,
    const gchar * codec_tag, const GstCaps * caps)
{
  const FormatInfo *info;
  gchar *desc;

  g_return_val_if_fail (taglist != NULL, FALSE);
  g_return_val_if_fail (GST_IS_TAG_LIST (taglist), FALSE);
  g_return_val_if_fail (codec_tag != NULL, FALSE);
  g_return_val_if_fail (gst_tag_exists (codec_tag), FALSE);
  g_return_val_if_fail (gst_tag_get_type (codec_tag) == G_TYPE_STRING, FALSE);
  g_return_val_if_fail (caps != NULL, FALSE);
  g_return_val_if_fail (GST_IS_CAPS (caps), FALSE);

  info = find_format_info (caps);
  if (info == NULL)
    return FALSE;

  desc = format_info_get_desc (info, caps);
  gst_tag_list_add (taglist, GST_TAG_MERGE_REPLACE, codec_tag, desc, NULL);
  g_free (desc);

  return TRUE;
}

/**
 * gst_pb_utils_get_codec_description:
 * @caps: the (fixed) #GstCaps for which an format description is needed
 *
 * Returns a localised (as far as this is possible) string describing the
 * media format specified in @caps, for use in error dialogs or other messages
 * to be seen by the user. Should never return NULL unless @caps is invalid.
 *
 * Also see the convenience function
 * gst_pb_utils_add_codec_description_to_tag_list().
 *
 * Returns: a newly-allocated description string, or NULL on error. Free
 *          string with g_free() when not needed any longer.
 */
gchar *
gst_pb_utils_get_codec_description (const GstCaps * caps)
{
  const FormatInfo *info;
  gchar *str, *comma;
  GstCaps *tmp;

  g_return_val_if_fail (caps != NULL, NULL);
  g_return_val_if_fail (GST_IS_CAPS (caps), NULL);
  tmp = copy_and_clean_caps (caps);
  g_return_val_if_fail (gst_caps_is_fixed (tmp), NULL);

  info = find_format_info (tmp);

  if (info) {
    str = format_info_get_desc (info, tmp);
  } else {
    str = gst_caps_to_string (tmp);

    /* cut off everything after the media type, if there is anything */
    if ((comma = strchr (str, ','))) {
      *comma = '\0';
      g_strchomp (str);
      /* we could do something more elaborate here, like taking into account
       * audio/, video/, image/ and application/ prefixes etc. */
    }

    GST_WARNING ("No description available for media type: %s", str);
  }
  gst_caps_unref (tmp);

  return str;
}

#if 0
void
gst_pb_utils_list_all (void)
{
  gint i;

  g_print ("static const gchar *caps_strings[] = { ");

  for (i = 0; i < G_N_ELEMENTS (formats); ++i) {
    if (formats[i].desc != NULL)
      g_print ("  \"%s\", ", formats[i].type);
  }
  g_print ("\n#if 0\n");
  for (i = 0; i < G_N_ELEMENTS (formats); ++i) {
    if (formats[i].desc == NULL)
      g_print ("  \"%s\", \n", formats[i].type);
  }
  g_print ("\n#endif\n");
}
#endif
