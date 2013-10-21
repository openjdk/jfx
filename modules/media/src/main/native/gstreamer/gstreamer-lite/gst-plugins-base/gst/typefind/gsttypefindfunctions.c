/* GStreamer
 * Copyright (C) 2003 Benjamin Otte <in7y118@public.uni-hamburg.de>
 * Copyright (C) 2005-2009 Tim-Philipp Müller <tim centricular net>
 * Copyright (C) 2009 Sebastian Dröge <sebastian.droege@collabora.co.uk>
 *
 * gsttypefindfunctions.c: collection of various typefind functions
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

#ifdef HAVE_CONFIG_H
#include "config.h"
#endif

#include <glib.h>
#include <glib/gprintf.h>

/* don't want to add gio xdgmime typefinder if gio was disabled via configure */
#ifdef HAVE_GIO
#include <gio/gio.h>
#define USE_GIO
#endif

#include <gst/gst.h>

#include <stdio.h>
#include <string.h>
#include <ctype.h>

#include <gst/pbutils/pbutils.h>

GST_DEBUG_CATEGORY_STATIC (type_find_debug);
#define GST_CAT_DEFAULT type_find_debug

/* DataScanCtx: helper for typefind functions that scan through data
 * step-by-step, to avoid doing a peek at each and every offset */

#define DATA_SCAN_CTX_CHUNK_SIZE 4096

typedef struct
{
  guint64 offset;
  const guint8 *data;
  gint size;
} DataScanCtx;

static inline void
data_scan_ctx_advance (GstTypeFind * tf, DataScanCtx * c, guint bytes_to_skip)
{
  c->offset += bytes_to_skip;
  if (G_LIKELY (c->size > bytes_to_skip)) {
    c->size -= bytes_to_skip;
    c->data += bytes_to_skip;
  } else {
    c->data += c->size;
    c->size = 0;
  }
}

static inline gboolean
data_scan_ctx_ensure_data (GstTypeFind * tf, DataScanCtx * c, gint min_len)
{
  const guint8 *data;
  guint64 len;
  guint chunk_len = MAX (DATA_SCAN_CTX_CHUNK_SIZE, min_len);

  if (G_LIKELY (c->size >= min_len))
    return TRUE;

  data = gst_type_find_peek (tf, c->offset, chunk_len);
  if (G_LIKELY (data != NULL)) {
    c->data = data;
    c->size = chunk_len;
    return TRUE;
  }

  /* if there's less than our chunk size, try to get as much as we can, but
   * always at least min_len bytes (we might be typefinding the first buffer
   * of the stream and not have as much data available as we'd like) */
  len = gst_type_find_get_length (tf);
  if (len > 0) {
    len = CLAMP (len - c->offset, min_len, chunk_len);
  } else {
    len = min_len;
  }

  data = gst_type_find_peek (tf, c->offset, len);
  if (data != NULL) {
    c->data = data;
    c->size = len;
    return TRUE;
  }

  return FALSE;
}

static inline gboolean
data_scan_ctx_memcmp (GstTypeFind * tf, DataScanCtx * c, guint offset,
    const gchar * data, guint len)
{
  if (!data_scan_ctx_ensure_data (tf, c, offset + len))
    return FALSE;

  return (memcmp (c->data + offset, data, len) == 0);
}

/*** text/plain ***/
static gboolean xml_check_first_element (GstTypeFind * tf,
    const gchar * element, guint elen, gboolean strict);
static gboolean sdp_check_header (GstTypeFind * tf);

static GstStaticCaps utf8_caps = GST_STATIC_CAPS ("text/plain");

#define UTF8_CAPS gst_static_caps_get(&utf8_caps)

static gboolean
utf8_type_find_have_valid_utf8_at_offset (GstTypeFind * tf, guint64 offset,
    GstTypeFindProbability * prob)
{
  guint8 *data;

  /* randomly decided values */
  guint min_size = 16;          /* minimum size  */
  guint size = 32 * 1024;       /* starting size */
  guint probability = 95;       /* starting probability */
  guint step = 10;              /* how much we reduce probability in each
                                 * iteration */

  while (probability > step && size > min_size) {
    data = gst_type_find_peek (tf, offset, size);
    if (data) {
      gchar *end;
      gchar *start = (gchar *) data;

      if (g_utf8_validate (start, size, (const gchar **) &end) || (end - start + 4 > size)) {   /* allow last char to be cut off */
        *prob = probability;
        return TRUE;
      }
      *prob = 0;
      return FALSE;
    }
    size /= 2;
    probability -= step;
  }
  *prob = 0;
  return FALSE;
}

static void
utf8_type_find (GstTypeFind * tf, gpointer unused)
{
  GstTypeFindProbability start_prob, mid_prob;
  guint64 length;

  /* leave xml to the xml typefinders */
  if (xml_check_first_element (tf, "", 0, TRUE))
    return;

  /* leave sdp to the sdp typefinders */
  if (sdp_check_header (tf))
    return;

  /* check beginning of stream */
  if (!utf8_type_find_have_valid_utf8_at_offset (tf, 0, &start_prob))
    return;

  GST_LOG ("start is plain text with probability of %u", start_prob);

  /* POSSIBLE is the highest probability we ever return if we can't
   * probe into the middle of the file and don't know its length */

  length = gst_type_find_get_length (tf);
  if (length == 0 || length == (guint64) - 1) {
    gst_type_find_suggest (tf, MIN (start_prob, GST_TYPE_FIND_POSSIBLE),
        UTF8_CAPS);
    return;
  }

  if (length < 64 * 1024) {
    gst_type_find_suggest (tf, start_prob, UTF8_CAPS);
    return;
  }

  /* check middle of stream */
  if (!utf8_type_find_have_valid_utf8_at_offset (tf, length / 2, &mid_prob))
    return;

  GST_LOG ("middle is plain text with probability of %u", mid_prob);
  gst_type_find_suggest (tf, (start_prob + mid_prob) / 2, UTF8_CAPS);
}

/*** text/uri-list ***/

static GstStaticCaps uri_caps = GST_STATIC_CAPS ("text/uri-list");

#define URI_CAPS (gst_static_caps_get(&uri_caps))
#define BUFFER_SIZE 16          /* If the string is < 16 bytes we're screwed */
#define INC_BUFFER {                                                    \
  pos++;                                                                \
  if (pos == BUFFER_SIZE) {                                             \
    pos = 0;                                                            \
    offset += BUFFER_SIZE;                                              \
    data = gst_type_find_peek (tf, offset, BUFFER_SIZE);                \
    if (data == NULL) return;                                           \
  } else {                                                              \
    data++;                                                             \
  }                                                                     \
}
static void
uri_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, BUFFER_SIZE);
  guint pos = 0;
  guint offset = 0;

  if (data) {
    /* Search for # comment lines */
    while (*data == '#') {
      /* Goto end of line */
      while (*data != '\n') {
        INC_BUFFER;
      }

      INC_BUFFER;
    }

    if (!g_ascii_isalpha (*data)) {
      /* Had a non alpha char - can't be uri-list */
      return;
    }

    INC_BUFFER;

    while (g_ascii_isalnum (*data)) {
      INC_BUFFER;
    }

    if (*data != ':') {
      /* First non alpha char is not a : */
      return;
    }

    /* Get the next 2 bytes as well */
    data = gst_type_find_peek (tf, offset + pos, 3);
    if (data == NULL)
      return;

    if (data[1] != '/' && data[2] != '/') {
      return;
    }

    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, URI_CAPS);
  }
}

/*** application/x-hls ***/

static GstStaticCaps hls_caps = GST_STATIC_CAPS ("application/x-hls");
#define HLS_CAPS (gst_static_caps_get(&hls_caps))

/* See http://tools.ietf.org/html/draft-pantos-http-live-streaming-05 */
static void
hls_type_find (GstTypeFind * tf, gpointer unused)
{
  DataScanCtx c = { 0, NULL, 0 };

  if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 7)))
    return;

  if (memcmp (c.data, "#EXTM3U", 7))
    return;

  data_scan_ctx_advance (tf, &c, 7);

  /* Check only the first 256 bytes */
  while (c.offset < 256) {
    if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 21)))
      return;

    /* Search for # comment lines */
    if (c.data[0] == '#' && (memcmp (c.data, "#EXT-X-TARGETDURATION", 21) == 0
            || memcmp (c.data, "#EXT-X-STREAM-INF", 17) == 0)) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, HLS_CAPS);
      return;
    }

    data_scan_ctx_advance (tf, &c, 1);
  }
}


/*** application/xml **********************************************************/

#define XML_BUFFER_SIZE 16
#define XML_INC_BUFFER {                                                \
  pos++;                                                                \
  if (pos == XML_BUFFER_SIZE) {                                         \
    pos = 0;                                                            \
    offset += XML_BUFFER_SIZE;                                          \
    data = gst_type_find_peek (tf, offset, XML_BUFFER_SIZE);            \
    if (data == NULL) return FALSE;                                     \
  } else {                                                              \
    data++;                                                             \
  }                                                                     \
}

static gboolean
xml_check_first_element (GstTypeFind * tf, const gchar * element, guint elen,
    gboolean strict)
{
  gboolean got_xmldec;
  guint8 *data;
  guint offset = 0;
  guint pos = 0;

  data = gst_type_find_peek (tf, 0, XML_BUFFER_SIZE);
  if (!data)
    return FALSE;

  /* look for the XMLDec
   * see XML spec 2.8, Prolog and Document Type Declaration
   * http://www.w3.org/TR/2004/REC-xml-20040204/#sec-prolog-dtd */
  got_xmldec = (memcmp (data, "<?xml", 5) == 0);

  if (strict && !got_xmldec)
    return FALSE;

  /* skip XMLDec in any case if we've got one */
  if (got_xmldec) {
    pos += 5;
    data += 5;
  }

  /* look for the first element, it has to be the requested element. Bail
   * out if it is not within the first 4kB. */
  while (data && (offset + pos) < 4096) {
    while (*data != '<' && (offset + pos) < 4096) {
      XML_INC_BUFFER;
    }

    XML_INC_BUFFER;
    if (!g_ascii_isalpha (*data)) {
      /* if not alphabetic, it's a PI or an element / attribute declaration
       * like <?xxx or <!xxx */
      XML_INC_BUFFER;
      continue;
    }

    /* the first normal element, check if it's the one asked for */
    data = gst_type_find_peek (tf, offset + pos, elen + 1);
    return (data && element && strncmp ((char *) data, element, elen) == 0);
  }

  return FALSE;
}

static GstStaticCaps generic_xml_caps = GST_STATIC_CAPS ("application/xml");

#define GENERIC_XML_CAPS (gst_static_caps_get(&generic_xml_caps))
static void
xml_type_find (GstTypeFind * tf, gpointer unused)
{
  if (xml_check_first_element (tf, "", 0, TRUE)) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MINIMUM, GENERIC_XML_CAPS);
  }
}

/*** application/sdp *********************************************************/

static GstStaticCaps sdp_caps = GST_STATIC_CAPS ("application/sdp");

#define SDP_CAPS (gst_static_caps_get(&sdp_caps))
static gboolean
sdp_check_header (GstTypeFind * tf)
{
  guint8 *data;

  data = gst_type_find_peek (tf, 0, 5);
  if (!data)
    return FALSE;

  /* sdp must start with v=0[\r]\n */
  if (memcmp (data, "v=0", 3))
    return FALSE;

  if (data[3] == '\r' && data[4] == '\n')
    return TRUE;
  if (data[3] == '\n')
    return TRUE;

  return FALSE;
}

static void
sdp_type_find (GstTypeFind * tf, gpointer unused)
{
  if (sdp_check_header (tf))
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, SDP_CAPS);
}

/*** application/smil *********************************************************/

static GstStaticCaps smil_caps = GST_STATIC_CAPS ("application/smil");

#define SMIL_CAPS (gst_static_caps_get(&smil_caps))
static void
smil_type_find (GstTypeFind * tf, gpointer unused)
{
  if (xml_check_first_element (tf, "smil", 4, FALSE)) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, SMIL_CAPS);
  }
}

/*** text/html ***/

static GstStaticCaps html_caps = GST_STATIC_CAPS ("text/html");

#define HTML_CAPS gst_static_caps_get (&html_caps)

static void
html_type_find (GstTypeFind * tf, gpointer unused)
{
  gchar *d, *data;

  data = (gchar *) gst_type_find_peek (tf, 0, 16);
  if (!data)
    return;

  if (!g_ascii_strncasecmp (data, "<!DOCTYPE HTML", 14)) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, HTML_CAPS);
  } else if (xml_check_first_element (tf, "html", 4, FALSE)) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, HTML_CAPS);
  } else if ((d = memchr (data, '<', 16))) {
    data = (gchar *) gst_type_find_peek (tf, d - data, 6);
    if (data && g_ascii_strncasecmp (data, "<html>", 6) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, HTML_CAPS);
    }
  }
}

/*** audio/midi ***/

static GstStaticCaps mid_caps = GST_STATIC_CAPS ("audio/midi");

#define MID_CAPS gst_static_caps_get(&mid_caps)
static void
mid_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 4);

  /* http://jedi.ks.uiuc.edu/~johns/links/music/midifile.html */
  if (data && data[0] == 'M' && data[1] == 'T' && data[2] == 'h'
      && data[3] == 'd')
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MID_CAPS);
}

/*** audio/mobile-xmf ***/

static GstStaticCaps mxmf_caps = GST_STATIC_CAPS ("audio/mobile-xmf");

#define MXMF_CAPS gst_static_caps_get(&mxmf_caps)
static void
mxmf_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = NULL;

  /* Search FileId "XMF_" 4 bytes */
  data = gst_type_find_peek (tf, 0, 4);
  if (data && data[0] == 'X' && data[1] == 'M' && data[2] == 'F'
      && data[3] == '_') {
    /* Search Format version "2.00" 4 bytes */
    data = gst_type_find_peek (tf, 4, 4);
    if (data && data[0] == '2' && data[1] == '.' && data[2] == '0'
        && data[3] == '0') {
      /* Search TypeId 2     1 byte */
      data = gst_type_find_peek (tf, 11, 1);
      if (data && data[0] == 2) {
        gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MXMF_CAPS);
      }
    }
  }
}


/*** video/x-fli ***/

static GstStaticCaps flx_caps = GST_STATIC_CAPS ("video/x-fli");

#define FLX_CAPS gst_static_caps_get(&flx_caps)
static void
flx_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 134);

  if (data) {
    /* check magic and the frame type of the first frame */
    if ((data[4] == 0x11 || data[4] == 0x12 ||
            data[4] == 0x30 || data[4] == 0x44) &&
        data[5] == 0xaf &&
        ((data[132] == 0x00 || data[132] == 0xfa) && data[133] == 0xf1)) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, FLX_CAPS);
    }
    return;
  }
  data = gst_type_find_peek (tf, 0, 6);
  if (data) {
    /* check magic only */
    if ((data[4] == 0x11 || data[4] == 0x12 ||
            data[4] == 0x30 || data[4] == 0x44) && data[5] == 0xaf) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY, FLX_CAPS);
    }
    return;
  }
}

/*** application/x-id3 ***/

static GstStaticCaps id3_caps = GST_STATIC_CAPS ("application/x-id3");

#define ID3_CAPS gst_static_caps_get(&id3_caps)
static void
id3v2_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 10);

  if (data && memcmp (data, "ID3", 3) == 0 &&
      data[3] != 0xFF && data[4] != 0xFF &&
      (data[6] & 0x80) == 0 && (data[7] & 0x80) == 0 &&
      (data[8] & 0x80) == 0 && (data[9] & 0x80) == 0) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, ID3_CAPS);
  }
}

static void
id3v1_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, -128, 3);

  if (data && memcmp (data, "TAG", 3) == 0) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, ID3_CAPS);
  }
}

/*** application/x-ape ***/

static GstStaticCaps apetag_caps = GST_STATIC_CAPS ("application/x-apetag");

#define APETAG_CAPS gst_static_caps_get(&apetag_caps)
static void
apetag_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data;

  /* APEv1/2 at start of file */
  data = gst_type_find_peek (tf, 0, 8);
  if (data && !memcmp (data, "APETAGEX", 8)) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, APETAG_CAPS);
    return;
  }

  /* APEv1/2 at end of file */
  data = gst_type_find_peek (tf, -32, 8);
  if (data && !memcmp (data, "APETAGEX", 8)) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, APETAG_CAPS);
    return;
  }
}

/*** audio/x-ttafile ***/

static GstStaticCaps tta_caps = GST_STATIC_CAPS ("audio/x-ttafile");

#define TTA_CAPS gst_static_caps_get(&tta_caps)
static void
tta_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 3);

  if (data) {
    if (memcmp (data, "TTA", 3) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, TTA_CAPS);
      return;
    }
  }
}

/*** audio/x-flac ***/
static GstStaticCaps flac_caps = GST_STATIC_CAPS ("audio/x-flac");

#define FLAC_CAPS (gst_static_caps_get(&flac_caps))

static void
flac_type_find (GstTypeFind * tf, gpointer unused)
{
  DataScanCtx c = { 0, NULL, 0 };

  if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 4)))
    return;

  /* standard flac (also old/broken flac-in-ogg with an initial 4-byte marker
   * packet and without the usual packet framing) */
  if (memcmp (c.data, "fLaC", 4) == 0) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, FLAC_CAPS);
    return;
  }

  if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 6)))
    return;

  /* flac-in-ogg, see http://flac.sourceforge.net/ogg_mapping.html */
  if (memcmp (c.data, "\177FLAC\001", 6) == 0) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, FLAC_CAPS);
    return;
  }

/* disabled because it happily typefinds /dev/urandom as audio/x-flac, and
 * because I yet have to see header-less flac in the wild */
#if 0
  /* flac without headers (subset format) */
  /* 64K should be enough */
  while (c.offset < (64 * 1024)) {
    if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 4)))
      break;

    /* look for frame header,
     * http://flac.sourceforge.net/format.html#frame_header
     */
    if (c.data[0] == 0xff && (c.data[1] >> 2) == 0x3e) {
      /* bit 15 in the header must be 0 */
      if (((c.data[1] >> 1) & 0x01) == 0x01)
        goto advance;

      /* blocksize must be != 0x00 */
      if ((c.data[2] >> 4) == 0x00)
        goto advance;

      /* samplerate must be != 0x0f */
      if ((c.data[2] & 0x0f) == 0x0f)
        goto advance;
      /* also 0 is invalid, as it means get the info from the header and we
       * don't have headers if we are here */
      if ((c.data[2] & 0x0f) == 0x00)
        goto advance;

      /* channel assignment must be < 11 */
      if ((c.data[3] >> 4) >= 11)
        goto advance;

      /* sample size must be != 0x07 and != 0x05 */
      if (((c.data[3] >> 1) & 0x07) == 0x07)
        goto advance;
      if (((c.data[3] >> 1) & 0x07) == 0x05)
        goto advance;
      /* also 0 is invalid, as it means get the info from the header and we
       * don't have headers if we are here */
      if (((c.data[3] >> 1) & 0x07) == 0x00)
        goto advance;

      /* next bit must be 0 */
      if ((c.data[3] & 0x01) == 0x01)
        goto advance;

      /* FIXME: shouldn't we include the crc check ? */

      GST_DEBUG ("Found flac without headers at %d", (gint) c.offset);
      gst_type_find_suggest (tf, GST_TYPE_FIND_POSSIBLE, FLAC_CAPS);
      return;
    }
  advance:
    data_scan_ctx_advance (tf, &c, 1);
  }
#endif
}

/*** audio/mpeg version 2, 4 ***/

static GstStaticCaps aac_caps = GST_STATIC_CAPS ("audio/mpeg, "
    "mpegversion = (int) { 2, 4 }, framed = (bool) false");
#define AAC_CAPS (gst_static_caps_get(&aac_caps))
#define AAC_AMOUNT (4096)
static void
aac_type_find (GstTypeFind * tf, gpointer unused)
{
  /* LUT to convert the AudioObjectType from the ADTS header to a string */
  DataScanCtx c = { 0, NULL, 0 };

  while (c.offset < AAC_AMOUNT) {
    guint snc, len;

    /* detect adts header or adif header.
     * The ADIF header is 4 bytes, that should be OK. The ADTS header, on
     * the other hand, is 14 bits only, so we require one valid frame with
     * again a valid syncpoint on the next one (28 bits) for certainty. We
     * require 4 kB, which is quite a lot, since frames are generally 200-400
     * bytes.
     * LOAS has 2 possible syncwords, which are 11 bits and 16 bits long.
     * The following stream syntax depends on which one is found.
     */
    if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 6)))
      break;

    snc = GST_READ_UINT16_BE (c.data);
    if (G_UNLIKELY ((snc & 0xfff6) == 0xfff0)) {
      /* ADTS header - find frame length */
      GST_DEBUG ("Found one ADTS syncpoint at offset 0x%" G_GINT64_MODIFIER
          "x, tracing next...", c.offset);
      len = ((c.data[3] & 0x03) << 11) |
          (c.data[4] << 3) | ((c.data[5] & 0xe0) >> 5);

      if (len == 0 || !data_scan_ctx_ensure_data (tf, &c, len + 2)) {
        GST_DEBUG ("Wrong sync or next frame not within reach, len=%u", len);
        goto next;
      }

      /* check if there's a second ADTS frame */
      snc = GST_READ_UINT16_BE (c.data + len);
      if ((snc & 0xfff6) == 0xfff0) {
        GstCaps *caps;
        guint mpegversion, sample_freq_idx, channel_config, profile_idx, rate;
        guint8 audio_config[2];

        mpegversion = (c.data[1] & 0x08) ? 2 : 4;
        profile_idx = c.data[2] >> 6;
        sample_freq_idx = ((c.data[2] & 0x3c) >> 2);
        channel_config = ((c.data[2] & 0x01) << 2) + (c.data[3] >> 6);

        GST_DEBUG ("Found second ADTS-%d syncpoint at offset 0x%"
            G_GINT64_MODIFIER "x, framelen %u", mpegversion, c.offset, len);

        /* 0xd and 0xe are reserved. 0xf means the sample frequency is directly
         * specified in the header, but that's not allowed for ADTS */
        if (sample_freq_idx > 0xc) {
          GST_DEBUG ("Unexpected sample frequency index %d or wrong sync",
              sample_freq_idx);
          goto next;
        }

        rate = gst_codec_utils_aac_get_sample_rate_from_index (sample_freq_idx);
        GST_LOG ("ADTS: profile=%u, rate=%u", profile_idx, rate);

        /* The ADTS frame header is slightly different from the
         * AudioSpecificConfig defined for the MPEG-4 container, so we just
         * construct enough of it for getting the level here. */
        /* ADTS counts profiles from 0 instead of 1 to save bits */
        audio_config[0] = (profile_idx + 1) << 3;
        audio_config[0] |= (sample_freq_idx >> 1) & 0x7;
        audio_config[1] = (sample_freq_idx & 0x1) << 7;
        audio_config[1] |= (channel_config & 0xf) << 3;

        caps = gst_caps_new_simple ("audio/mpeg",
            "framed", G_TYPE_BOOLEAN, FALSE,
            "mpegversion", G_TYPE_INT, mpegversion,
            "stream-format", G_TYPE_STRING, "adts", NULL);

        gst_codec_utils_aac_caps_set_level_and_profile (caps, audio_config, 2);

        /* add rate and number of channels if we can */
        if (channel_config != 0 && channel_config <= 7) {
          const guint channels_map[] = { 0, 1, 2, 3, 4, 5, 6, 8 };

          gst_caps_set_simple (caps, "channels", G_TYPE_INT,
              channels_map[channel_config], "rate", G_TYPE_INT, rate, NULL);
        }

        gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY, caps);
        gst_caps_unref (caps);
        break;
      }

      GST_DEBUG ("No next frame found... (should have been at 0x%x)", len);
    } else if (G_UNLIKELY (((snc & 0xffe0) == 0x56e0) || (snc == 0x4de1))) {
      /* LOAS frame */

      GST_DEBUG ("Found one LOAS syncword at offset 0x%" G_GINT64_MODIFIER
          "x, tracing next...", c.offset);

      /* check length of frame for each type of detectable LOAS streams */
      if (snc == 0x4de1) {
        /* EPAudioSyncStream */
        len = ((c.data[2] & 0x0f) << 9) | (c.data[3] << 1) |
            ((c.data[4] & 0x80) >> 7);
        /* add size of EP sync stream header */
        len += 7;
      } else {
        /* AudioSyncStream */
        len = ((c.data[1] & 0x1f) << 8) | c.data[2];
        /* add size of sync stream header */
        len += 3;
      }

      if (len == 0 || !data_scan_ctx_ensure_data (tf, &c, len + 2)) {
        GST_DEBUG ("Wrong sync or next frame not within reach, len=%u", len);
        goto next;
      }

      /* check if there's a second LOAS frame */
      snc = GST_READ_UINT16_BE (c.data + len);
      if (((snc & 0xffe0) == 0x56e0) || (snc == 0x4de1)) {
        GST_DEBUG ("Found second LOAS syncword at offset 0x%"
            G_GINT64_MODIFIER "x, framelen %u", c.offset, len);

        gst_type_find_suggest_simple (tf, GST_TYPE_FIND_LIKELY, "audio/mpeg",
            "framed", G_TYPE_BOOLEAN, FALSE,
            "mpegversion", G_TYPE_INT, 4,
            "stream-format", G_TYPE_STRING, "loas", NULL);
        break;
      }

      GST_DEBUG ("No next frame found... (should have been at 0x%x)", len);
    } else if (!memcmp (c.data, "ADIF", 4)) {
      /* ADIF header */
      gst_type_find_suggest_simple (tf, GST_TYPE_FIND_LIKELY, "audio/mpeg",
          "framed", G_TYPE_BOOLEAN, FALSE, "mpegversion", G_TYPE_INT, 4,
          "stream-format", G_TYPE_STRING, "adif", NULL);
      break;
    }

  next:

    data_scan_ctx_advance (tf, &c, 1);
  }
}

/*** audio/mpeg version 1 ***/

/*
 * The chance that random data is identified as a valid mp3 header is 63 / 2^18
 * (0.024%) per try. This makes the function for calculating false positives
 *   1 - (1 - ((63 / 2 ^18) ^ GST_MP3_TYPEFIND_MIN_HEADERS)) ^ buffersize)
 * This has the following probabilities of false positives:
 * datasize               MIN_HEADERS
 * (bytes)      1       2       3       4
 * 4096         62.6%    0.02%   0%      0%
 * 16384        98%      0.09%   0%      0%
 * 1 MiB       100%      5.88%   0%      0%
 * 1 GiB       100%    100%      1.44%   0%
 * 1 TiB       100%    100%    100%      0.35%
 * This means that the current choice (3 headers by most of the time 4096 byte
 * buffers is pretty safe for now.
 *
 * The max. size of each frame is 1440 bytes, which means that for N frames to
 * be detected, we need 1440 * GST_MP3_TYPEFIND_MIN_HEADERS + 3 bytes of data.
 * Assuming we step into the stream right after the frame header, this
 * means we need 1440 * (GST_MP3_TYPEFIND_MIN_HEADERS + 1) - 1 + 3 bytes
 * of data (5762) to always detect any mp3.
 */

static const guint mp3types_bitrates[2][3][16] =
    { {{0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448,},
    {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384,},
    {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320,}},
{{0, 32, 48, 56, 64, 80, 96, 112, 128, 144, 160, 176, 192, 224, 256,},
    {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160,},
    {0, 8, 16, 24, 32, 40, 48, 56, 64, 80, 96, 112, 128, 144, 160,}},
};

static const guint mp3types_freqs[3][3] = { {11025, 12000, 8000},
{22050, 24000, 16000},
{44100, 48000, 32000}
};

static inline guint
mp3_type_frame_length_from_header (guint32 header, guint * put_layer,
    guint * put_channels, guint * put_bitrate, guint * put_samplerate,
    gboolean * may_be_free_format, gint possible_free_framelen)
{
  guint bitrate, layer, length, mode, samplerate, version, channels;

  if ((header & 0xffe00000) != 0xffe00000)
    return 0;

  /* we don't need extension, copyright, original or
   * emphasis for the frame length */
  header >>= 6;

  /* mode */
  mode = header & 0x3;
  header >>= 3;

  /* padding */
  length = header & 0x1;
  header >>= 1;

  /* sampling frequency */
  samplerate = header & 0x3;
  if (samplerate == 3)
    return 0;
  header >>= 2;

  /* bitrate index */
  bitrate = header & 0xF;
  if (bitrate == 0 && possible_free_framelen == -1) {
    GST_LOG ("Possibly a free format mp3 - signalling");
    *may_be_free_format = TRUE;
  }
  if (bitrate == 15 || (bitrate == 0 && possible_free_framelen == -1))
    return 0;

  /* ignore error correction, too */
  header >>= 5;

  /* layer */
  layer = 4 - (header & 0x3);
  if (layer == 4)
    return 0;
  header >>= 2;

  /* version 0=MPEG2.5; 2=MPEG2; 3=MPEG1 */
  version = header & 0x3;
  if (version == 1)
    return 0;

  /* lookup */
  channels = (mode == 3) ? 1 : 2;
  samplerate = mp3types_freqs[version > 0 ? version - 1 : 0][samplerate];
  if (bitrate == 0) {
    if (layer == 1) {
      length *= 4;
      length += possible_free_framelen;
      bitrate = length * samplerate / 48000;
    } else {
      length += possible_free_framelen;
      bitrate = length * samplerate /
          ((layer == 3 && version != 3) ? 72000 : 144000);
    }
  } else {
    /* calculating */
    bitrate = mp3types_bitrates[version == 3 ? 0 : 1][layer - 1][bitrate];
    if (layer == 1) {
      length = ((12000 * bitrate / samplerate) + length) * 4;
    } else {
      length += ((layer == 3
              && version != 3) ? 72000 : 144000) * bitrate / samplerate;
    }
  }

  GST_LOG ("mp3typefind: calculated mp3 frame length of %u bytes", length);
  GST_LOG
      ("mp3typefind: samplerate = %u - bitrate = %u - layer = %u - version = %u"
      " - channels = %u", samplerate, bitrate, layer, version, channels);

  if (put_layer)
    *put_layer = layer;
  if (put_channels)
    *put_channels = channels;
  if (put_bitrate)
    *put_bitrate = bitrate;
  if (put_samplerate)
    *put_samplerate = samplerate;

  return length;
}


static GstStaticCaps mp3_caps = GST_STATIC_CAPS ("audio/mpeg, "
    "mpegversion = (int) 1, layer = (int) [ 1, 3 ]");
#define MP3_CAPS (gst_static_caps_get(&mp3_caps))
/*
 * random values for typefinding
 * if no more data is available, we will return a probability of
 * (found_headers/TRY_HEADERS) * (MAXIMUM * (TRY_SYNC - bytes_skipped)
 *        / TRY_SYNC)
 * if found_headers >= MIN_HEADERS
 */
#define GST_MP3_TYPEFIND_MIN_HEADERS (2)
#define GST_MP3_TYPEFIND_TRY_HEADERS (5)
#define GST_MP3_TYPEFIND_TRY_SYNC (GST_TYPE_FIND_MAXIMUM * 100) /* 10kB */
#define GST_MP3_TYPEFIND_SYNC_SIZE (2048)
#define GST_MP3_WRONG_HEADER (10)

static void
mp3_type_find_at_offset (GstTypeFind * tf, guint64 start_off,
    guint * found_layer, GstTypeFindProbability * found_prob)
{
  guint8 *data = NULL;
  guint8 *data_end = NULL;
  guint size;
  guint64 skipped;
  gint last_free_offset = -1;
  gint last_free_framelen = -1;
  gboolean headerstart = TRUE;

  *found_layer = 0;
  *found_prob = 0;

  size = 0;
  skipped = 0;
  while (skipped < GST_MP3_TYPEFIND_TRY_SYNC) {
    if (size <= 0) {
      size = GST_MP3_TYPEFIND_SYNC_SIZE * 2;
      do {
        size /= 2;
        data = gst_type_find_peek (tf, skipped + start_off, size);
      } while (size > 10 && !data);
      if (!data)
        break;
      data_end = data + size;
    }
    if (*data == 0xFF) {
      guint8 *head_data = NULL;
      guint layer = 0, bitrate, samplerate, channels;
      guint found = 0;          /* number of valid headers found */
      guint64 offset = skipped;

      while (found < GST_MP3_TYPEFIND_TRY_HEADERS) {
        guint32 head;
        guint length;
        guint prev_layer = 0;
        guint prev_channels = 0, prev_samplerate = 0;
        gboolean free = FALSE;

        if ((gint64) (offset - skipped + 4) >= 0 &&
            data + offset - skipped + 4 < data_end) {
          head_data = data + offset - skipped;
        } else {
          head_data = gst_type_find_peek (tf, offset + start_off, 4);
        }
        if (!head_data)
          break;
        head = GST_READ_UINT32_BE (head_data);
        if (!(length = mp3_type_frame_length_from_header (head, &layer,
                    &channels, &bitrate, &samplerate, &free,
                    last_free_framelen))) {
          if (free) {
            if (last_free_offset == -1)
              last_free_offset = offset;
            else {
              last_free_framelen = offset - last_free_offset;
              offset = last_free_offset;
              continue;
            }
          } else {
            last_free_framelen = -1;
          }

          /* Mark the fact that we didn't find a valid header at the beginning */
          if (found == 0)
            headerstart = FALSE;

          GST_LOG ("%d. header at offset %" G_GUINT64_FORMAT
              " (0x%" G_GINT64_MODIFIER "x) was not an mp3 header "
              "(possibly-free: %s)", found + 1, start_off + offset,
              start_off + offset, free ? "yes" : "no");
          break;
        }
        if ((prev_layer && prev_layer != layer) ||
            /* (prev_bitrate && prev_bitrate != bitrate) || <-- VBR */
            (prev_samplerate && prev_samplerate != samplerate) ||
            (prev_channels && prev_channels != channels)) {
          /* this means an invalid property, or a change, which might mean
           * that this is not a mp3 but just a random bytestream. It could
           * be a freaking funky encoded mp3 though. We'll just not count
           * this header*/
          prev_layer = layer;
          prev_channels = channels;
          prev_samplerate = samplerate;
        } else {
          found++;
          GST_LOG ("found %d. header at offset %" G_GUINT64_FORMAT " (0x%"
              G_GINT64_MODIFIER "X)", found, start_off + offset,
              start_off + offset);
        }
        offset += length;
      }
      g_assert (found <= GST_MP3_TYPEFIND_TRY_HEADERS);
      if (head_data == NULL &&
          gst_type_find_peek (tf, offset + start_off - 1, 1) == NULL)
        /* Incomplete last frame - don't count it. */
        found--;
      if (found == GST_MP3_TYPEFIND_TRY_HEADERS ||
          (found >= GST_MP3_TYPEFIND_MIN_HEADERS && head_data == NULL)) {
        /* we can make a valid guess */
        guint probability = found * GST_TYPE_FIND_MAXIMUM *
            (GST_MP3_TYPEFIND_TRY_SYNC - skipped) /
            GST_MP3_TYPEFIND_TRY_HEADERS / GST_MP3_TYPEFIND_TRY_SYNC;

        if (!headerstart
            && probability > (GST_TYPE_FIND_MINIMUM + GST_MP3_WRONG_HEADER))
          probability -= GST_MP3_WRONG_HEADER;
        if (probability < GST_TYPE_FIND_MINIMUM)
          probability = GST_TYPE_FIND_MINIMUM;
        if (start_off > 0)
          probability /= 2;

        GST_INFO
            ("audio/mpeg calculated %u  =  %u  *  %u / %u  *  (%u - %"
            G_GUINT64_FORMAT ") / %u", probability, GST_TYPE_FIND_MAXIMUM,
            found, GST_MP3_TYPEFIND_TRY_HEADERS, GST_MP3_TYPEFIND_TRY_SYNC,
            (guint64) skipped, GST_MP3_TYPEFIND_TRY_SYNC);
        /* make sure we're not id3 tagged */
        head_data = gst_type_find_peek (tf, -128, 3);
        if (head_data && (memcmp (head_data, "TAG", 3) == 0)) {
          probability = 0;
        }
        g_assert (probability <= GST_TYPE_FIND_MAXIMUM);

        *found_prob = probability;
        if (probability > 0)
          *found_layer = layer;
        return;
      }
    }
    data++;
    skipped++;
    size--;
  }
}

static void
mp3_type_find (GstTypeFind * tf, gpointer unused)
{
  GstTypeFindProbability prob, mid_prob;
  guint8 *data;
  guint layer, mid_layer;
  guint64 length;

  mp3_type_find_at_offset (tf, 0, &layer, &prob);
  length = gst_type_find_get_length (tf);

  if (length == 0 || length == (guint64) - 1) {
    if (prob != 0)
      goto suggest;
    return;
  }

  /* if we're pretty certain already, skip the additional check */
  if (prob >= GST_TYPE_FIND_LIKELY)
    goto suggest;

  mp3_type_find_at_offset (tf, length / 2, &mid_layer, &mid_prob);

  if (mid_prob > 0) {
    if (prob == 0) {
      GST_LOG ("detected audio/mpeg only in the middle (p=%u)", mid_prob);
      layer = mid_layer;
      prob = mid_prob;
      goto suggest;
    }

    if (layer != mid_layer) {
      GST_WARNING ("audio/mpeg layer discrepancy: %u vs. %u", layer, mid_layer);
      return;                   /* FIXME: or should we just go with the one in the middle? */
    }

    /* detected mpeg audio both in middle of the file and at the start */
    prob = (prob + mid_prob) / 2;
    goto suggest;
  }

  /* let's see if there's a valid header right at the start */
  data = gst_type_find_peek (tf, 0, 4); /* use min. frame size? */
  if (data && mp3_type_frame_length_from_header (GST_READ_UINT32_BE (data),
          &layer, NULL, NULL, NULL, NULL, 0) != 0) {
    if (prob == 0)
      prob = GST_TYPE_FIND_POSSIBLE - 10;
    else
      prob = MAX (GST_TYPE_FIND_POSSIBLE - 10, prob + 10);
  }

  if (prob > 0)
    goto suggest;

  return;

suggest:
  {
    g_return_if_fail (layer >= 1 && layer <= 3);

    gst_type_find_suggest_simple (tf, prob, "audio/mpeg",
        "mpegversion", G_TYPE_INT, 1, "layer", G_TYPE_INT, layer, NULL);
  }
}

/*** audio/x-musepack ***/

static GstStaticCaps musepack_caps =
GST_STATIC_CAPS ("audio/x-musepack, streamversion= (int) { 7, 8 }");

#define MUSEPACK_CAPS (gst_static_caps_get(&musepack_caps))
static void
musepack_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 4);
  GstTypeFindProbability prop = GST_TYPE_FIND_MINIMUM;
  gint streamversion = -1;

  if (data && memcmp (data, "MP+", 3) == 0) {
    streamversion = 7;
    if ((data[3] & 0x7f) == 7) {
      prop = GST_TYPE_FIND_MAXIMUM;
    } else {
      prop = GST_TYPE_FIND_LIKELY + 10;
    }
  } else if (data && memcmp (data, "MPCK", 4) == 0) {
    streamversion = 8;
    prop = GST_TYPE_FIND_MAXIMUM;
  }

  if (streamversion != -1) {
    gst_type_find_suggest_simple (tf, prop, "audio/x-musepack",
        "streamversion", G_TYPE_INT, streamversion, NULL);
  }
}

/*** audio/x-ac3 ***/
/* FIXME 0.11: should be audio/ac3, but isn't for backwards compatibility */
static GstStaticCaps ac3_caps = GST_STATIC_CAPS ("audio/x-ac3");

#define AC3_CAPS (gst_static_caps_get(&ac3_caps))

static GstStaticCaps eac3_caps = GST_STATIC_CAPS ("audio/x-eac3");

#define EAC3_CAPS (gst_static_caps_get(&eac3_caps))

struct ac3_frmsize
{
  unsigned short bit_rate;
  unsigned short frm_size[3];
};

static const struct ac3_frmsize ac3_frmsizecod_tbl[] = {
  {32, {64, 69, 96}},
  {32, {64, 70, 96}},
  {40, {80, 87, 120}},
  {40, {80, 88, 120}},
  {48, {96, 104, 144}},
  {48, {96, 105, 144}},
  {56, {112, 121, 168}},
  {56, {112, 122, 168}},
  {64, {128, 139, 192}},
  {64, {128, 140, 192}},
  {80, {160, 174, 240}},
  {80, {160, 175, 240}},
  {96, {192, 208, 288}},
  {96, {192, 209, 288}},
  {112, {224, 243, 336}},
  {112, {224, 244, 336}},
  {128, {256, 278, 384}},
  {128, {256, 279, 384}},
  {160, {320, 348, 480}},
  {160, {320, 349, 480}},
  {192, {384, 417, 576}},
  {192, {384, 418, 576}},
  {224, {448, 487, 672}},
  {224, {448, 488, 672}},
  {256, {512, 557, 768}},
  {256, {512, 558, 768}},
  {320, {640, 696, 960}},
  {320, {640, 697, 960}},
  {384, {768, 835, 1152}},
  {384, {768, 836, 1152}},
  {448, {896, 975, 1344}},
  {448, {896, 976, 1344}},
  {512, {1024, 1114, 1536}},
  {512, {1024, 1115, 1536}},
  {576, {1152, 1253, 1728}},
  {576, {1152, 1254, 1728}},
  {640, {1280, 1393, 1920}},
  {640, {1280, 1394, 1920}}
};

static void
ac3_type_find (GstTypeFind * tf, gpointer unused)
{
  DataScanCtx c = { 0, NULL, 0 };

  /* Search for an ac3 frame; not neccesarily right at the start, but give it
   * a lower probability if not found right at the start. Check that the
   * frame is followed by a second frame at the expected offset.
   * We could also check the two ac3 CRCs, but we don't do that right now */
  while (c.offset < 1024) {
    if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 5)))
      break;

    if (c.data[0] == 0x0b && c.data[1] == 0x77) {
      guint bsid = c.data[5] >> 3;

      if (bsid <= 8) {
        /* ac3 */
        guint fscod = c.data[4] >> 6;
        guint frmsizecod = c.data[4] & 0x3f;

        if (fscod < 3 && frmsizecod < 38) {
          DataScanCtx c_next = c;
          guint frame_size;

          frame_size = ac3_frmsizecod_tbl[frmsizecod].frm_size[fscod];
          GST_LOG ("possible AC3 frame sync at offset %"
              G_GUINT64_FORMAT ", size=%u", c.offset, frame_size);
          if (data_scan_ctx_ensure_data (tf, &c_next, (frame_size * 2) + 5)) {
            data_scan_ctx_advance (tf, &c_next, frame_size * 2);

            if (c_next.data[0] == 0x0b && c_next.data[1] == 0x77) {
              fscod = c_next.data[4] >> 6;
              frmsizecod = c_next.data[4] & 0x3f;

              if (fscod < 3 && frmsizecod < 38) {
                GstTypeFindProbability prob;

                GST_LOG ("found second AC3 frame (size=%u), looks good",
                    ac3_frmsizecod_tbl[frmsizecod].frm_size[fscod]);
                if (c.offset == 0)
                  prob = GST_TYPE_FIND_MAXIMUM;
                else
                  prob = GST_TYPE_FIND_NEARLY_CERTAIN;

                gst_type_find_suggest (tf, prob, AC3_CAPS);
                return;
              }
            } else {
              GST_LOG ("no second AC3 frame found, false sync");
            }
          }
        }
      } else if (bsid <= 16 && bsid > 10) {
        /* eac3 */
        DataScanCtx c_next = c;
        guint frame_size;

        frame_size = (((c.data[2] & 0x07) << 8) + c.data[3]) + 1;
        GST_LOG ("possible E-AC3 frame sync at offset %"
            G_GUINT64_FORMAT ", size=%u", c.offset, frame_size);
        if (data_scan_ctx_ensure_data (tf, &c_next, (frame_size * 2) + 5)) {
          data_scan_ctx_advance (tf, &c_next, frame_size * 2);

          if (c_next.data[0] == 0x0b && c_next.data[1] == 0x77) {
            GstTypeFindProbability prob;

            GST_LOG ("found second E-AC3 frame, looks good");
            if (c.offset == 0)
              prob = GST_TYPE_FIND_MAXIMUM;
            else
              prob = GST_TYPE_FIND_NEARLY_CERTAIN;

            gst_type_find_suggest (tf, prob, EAC3_CAPS);
            return;
          } else {
            GST_LOG ("no second E-AC3 frame found, false sync");
          }
        }
      } else {
        GST_LOG ("invalid AC3 BSID: %u", bsid);
      }
    }
    data_scan_ctx_advance (tf, &c, 1);
  }
}

/*** audio/x-dts ***/
static GstStaticCaps dts_caps = GST_STATIC_CAPS ("audio/x-dts");
#define DTS_CAPS (gst_static_caps_get (&dts_caps))
#define DTS_MIN_FRAMESIZE 96
#define DTS_MAX_FRAMESIZE 18725 /* 16384*16/14 */

static gboolean
dts_parse_frame_header (DataScanCtx * c, guint * frame_size,
    guint * sample_rate, guint * channels, guint * depth, guint * endianness)
{
  static const int sample_rates[16] = { 0, 8000, 16000, 32000, 0, 0, 11025,
    22050, 44100, 0, 0, 12000, 24000, 48000, 96000, 192000
  };
  static const guint8 channels_table[16] = { 1, 2, 2, 2, 2, 3, 3, 4, 4, 5,
    6, 6, 6, 7, 8, 8
  };
  guint16 hdr[8];
  guint32 marker;
  guint num_blocks, chans, lfe, i;

  marker = GST_READ_UINT32_BE (c->data);

  /* raw big endian or 14-bit big endian */
  if (marker == 0x7FFE8001 || marker == 0x1FFFE800) {
    *endianness = G_BIG_ENDIAN;
    for (i = 0; i < G_N_ELEMENTS (hdr); ++i)
      hdr[i] = GST_READ_UINT16_BE (c->data + (i * sizeof (guint16)));
  } else
    /* raw little endian or 14-bit little endian */
  if (marker == 0xFE7F0180 || marker == 0xFF1F00E8) {
    *endianness = G_LITTLE_ENDIAN;
    for (i = 0; i < G_N_ELEMENTS (hdr); ++i)
      hdr[i] = GST_READ_UINT16_LE (c->data + (i * sizeof (guint16)));
  } else {
    return FALSE;
  }

  GST_LOG ("dts sync marker 0x%08x at offset %u", marker, (guint) c->offset);

  /* 14-bit mode */
  if (marker == 0x1FFFE800 || marker == 0xFF1F00E8) {
    if ((hdr[2] & 0xFFF0) != 0x07F0)
      return FALSE;
    /* discard top 2 bits (2 void), shift in 2 */
    hdr[0] = (hdr[0] << 2) | ((hdr[1] >> 12) & 0x0003);
    /* discard top 4 bits (2 void, 2 shifted into hdr[0]), shift in 4 etc. */
    hdr[1] = (hdr[1] << 4) | ((hdr[2] >> 10) & 0x000F);
    hdr[2] = (hdr[2] << 6) | ((hdr[3] >> 8) & 0x003F);
    hdr[3] = (hdr[3] << 8) | ((hdr[4] >> 6) & 0x00FF);
    hdr[4] = (hdr[4] << 10) | ((hdr[5] >> 4) & 0x03FF);
    hdr[5] = (hdr[5] << 12) | ((hdr[6] >> 2) & 0x0FFF);
    hdr[6] = (hdr[6] << 14) | ((hdr[7] >> 0) & 0x3FFF);
    g_assert (hdr[0] == 0x7FFE && hdr[1] == 0x8001);
    *depth = 14;
  } else {
    *depth = 16;
  }

  GST_LOG ("frame header: %04x%04x%04x%04x", hdr[2], hdr[3], hdr[4], hdr[5]);

  num_blocks = (hdr[2] >> 2) & 0x7F;
  *frame_size = (((hdr[2] & 0x03) << 12) | (hdr[3] >> 4)) + 1;
  chans = ((hdr[3] & 0x0F) << 2) | (hdr[4] >> 14);
  *sample_rate = sample_rates[(hdr[4] >> 10) & 0x0F];
  lfe = (hdr[5] >> 9) & 0x03;

  if (num_blocks < 5 || *frame_size < 96 || *sample_rate == 0)
    return FALSE;

  if (marker == 0x1FFFE800 || marker == 0xFF1F00E8)
    *frame_size = (*frame_size * 16) / 14;      /* FIXME: round up? */

  if (chans < G_N_ELEMENTS (channels_table))
    *channels = channels_table[chans] + ((lfe) ? 1 : 0);
  else
    *channels = 0;

  return TRUE;
}

static void
dts_type_find (GstTypeFind * tf, gpointer unused)
{
  DataScanCtx c = { 0, NULL, 0 };

  /* Search for an dts frame; not neccesarily right at the start, but give it
   * a lower probability if not found right at the start. Check that the
   * frame is followed by a second frame at the expected offset. */
  while (c.offset <= DTS_MAX_FRAMESIZE) {
    guint frame_size = 0, rate = 0, chans = 0, depth = 0, endianness = 0;

    if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, DTS_MIN_FRAMESIZE)))
      return;

    if (G_UNLIKELY (dts_parse_frame_header (&c, &frame_size, &rate, &chans,
                &depth, &endianness))) {
      GstTypeFindProbability prob;
      DataScanCtx next_c;

      prob = (c.offset == 0) ? GST_TYPE_FIND_LIKELY : GST_TYPE_FIND_POSSIBLE;

      /* check for second frame sync */
      next_c = c;
      data_scan_ctx_advance (tf, &next_c, frame_size);
      if (data_scan_ctx_ensure_data (tf, &next_c, 4)) {
        GST_LOG ("frame size: %u 0x%04x", frame_size, frame_size);
        GST_MEMDUMP ("second frame sync", next_c.data, 4);
        if (GST_READ_UINT32_BE (c.data) == GST_READ_UINT32_BE (next_c.data))
          prob = GST_TYPE_FIND_MAXIMUM;
      }

      if (chans > 0) {
        gst_type_find_suggest_simple (tf, prob, "audio/x-dts",
            "rate", G_TYPE_INT, rate, "channels", G_TYPE_INT, chans,
            "depth", G_TYPE_INT, depth, "endianness", G_TYPE_INT, endianness,
            "framed", G_TYPE_BOOLEAN, FALSE, NULL);
      } else {
        gst_type_find_suggest_simple (tf, prob, "audio/x-dts",
            "rate", G_TYPE_INT, rate, "depth", G_TYPE_INT, depth,
            "endianness", G_TYPE_INT, endianness,
            "framed", G_TYPE_BOOLEAN, FALSE, NULL);
      }

      return;
    }

    data_scan_ctx_advance (tf, &c, 1);
  }
}

/*** gsm ***/

/* can only be detected by using the extension, in which case we use the default
 * GSM properties */
static GstStaticCaps gsm_caps =
GST_STATIC_CAPS ("audio/x-gsm, rate=8000, channels=1");

#define GSM_CAPS (gst_static_caps_get(&gsm_caps))

/*** wavpack ***/

static GstStaticCaps wavpack_caps =
GST_STATIC_CAPS ("audio/x-wavpack, framed = (boolean) false");

#define WAVPACK_CAPS (gst_static_caps_get(&wavpack_caps))

static GstStaticCaps wavpack_correction_caps =
GST_STATIC_CAPS ("audio/x-wavpack-correction, framed = (boolean) false");

#define WAVPACK_CORRECTION_CAPS (gst_static_caps_get(&wavpack_correction_caps))

static void
wavpack_type_find (GstTypeFind * tf, gpointer unused)
{
  guint64 offset;
  guint32 blocksize;
  guint8 *data;

  data = gst_type_find_peek (tf, 0, 32);
  if (!data)
    return;

  if (data[0] != 'w' || data[1] != 'v' || data[2] != 'p' || data[3] != 'k')
    return;

  /* Note: wavpack blocks can be fairly large (easily 60-110k), possibly
   * larger than the max. limits imposed by certain typefinding elements
   * like id3demux or apedemux, so typefinding is most likely only going to
   * work in pull-mode */
  blocksize = GST_READ_UINT32_LE (data + 4);
  GST_LOG ("wavpack header, blocksize=0x%04x", blocksize);
  offset = 32;
  while (offset < 32 + blocksize) {
    guint32 sublen;

    /* get chunk header */
    GST_LOG ("peeking at chunk at offset 0x%04x", (guint) offset);
    data = gst_type_find_peek (tf, offset, 4);
    if (data == NULL)
      break;
    sublen = ((guint32) data[1]) << 1;
    if (data[0] & 0x80) {
      sublen |= (((guint32) data[2]) << 9) | (((guint32) data[3]) << 17);
      sublen += 1 + 3;          /* id + length */
    } else {
      sublen += 1 + 1;          /* id + length */
    }
    if (sublen > blocksize - offset + 32) {
      GST_LOG ("chunk length too big (%u > %" G_GUINT64_FORMAT ")", sublen,
          blocksize - offset);
      break;
    }
    if ((data[0] & 0x20) == 0) {
      switch (data[0] & 0x0f) {
        case 0xa:              /* ID_WV_BITSTREAM  */
        case 0xc:              /* ID_WVX_BITSTREAM */
          gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY, WAVPACK_CAPS);
          return;
        case 0xb:              /* ID_WVC_BITSTREAM */
          gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY,
              WAVPACK_CORRECTION_CAPS);
          return;
        default:
          break;
      }
    }
    offset += sublen;
  }
}

/*** application/postscrip ***/
static GstStaticCaps postscript_caps =
GST_STATIC_CAPS ("application/postscript");

#define POSTSCRIPT_CAPS (gst_static_caps_get(&postscript_caps))

static void
postscript_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 3);
  if (!data)
    return;

  if (data[0] == 0x04)
    data++;
  if (data[0] == '%' && data[1] == '!')
    gst_type_find_suggest (tf, GST_TYPE_FIND_POSSIBLE, POSTSCRIPT_CAPS);

}

/*** image/svg+xml ***/
static GstStaticCaps svg_caps = GST_STATIC_CAPS ("image/svg+xml");

#define SVG_CAPS (gst_static_caps_get(&svg_caps))

static void
svg_type_find (GstTypeFind * tf, gpointer unused)
{
  static const gchar svg_doctype[] = "!DOCTYPE svg";
  static const gchar svg_tag[] = "<svg";
  DataScanCtx c = { 0, NULL, 0 };

  while (c.offset <= 1024) {
    if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 12)))
      break;

    if (memcmp (svg_doctype, c.data, 12) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, SVG_CAPS);
      return;
    } else if (memcmp (svg_tag, c.data, 4) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY, SVG_CAPS);
      return;
    }
    data_scan_ctx_advance (tf, &c, 1);
  }
}

/*** multipart/x-mixed-replace mimestream ***/

static GstStaticCaps multipart_caps =
GST_STATIC_CAPS ("multipart/x-mixed-replace");
#define MULTIPART_CAPS gst_static_caps_get(&multipart_caps)

/* multipart/x-mixed replace is: 
 *   <maybe some whitespace>--<some ascii chars>[\r]\n
 *   <more ascii chars>[\r]\nContent-type:<more ascii>[\r]\n */
static void
multipart_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data;
  guint8 *x;

#define MULTIPART_MAX_BOUNDARY_OFFSET 16
  data = gst_type_find_peek (tf, 0, MULTIPART_MAX_BOUNDARY_OFFSET);
  if (!data)
    return;

  for (x = data;
      x - data < MULTIPART_MAX_BOUNDARY_OFFSET - 2 && g_ascii_isspace (*x);
      x++);
  if (x[0] != '-' || x[1] != '-')
    return;

  /* Could be okay, peek what should be enough for a complete header */
#define MULTIPART_MAX_HEADER_SIZE 256
  data = gst_type_find_peek (tf, 0, MULTIPART_MAX_HEADER_SIZE);
  if (!data)
    return;

  for (x = data; x - data < MULTIPART_MAX_HEADER_SIZE - 14; x++) {
    if (!isascii (*x)) {
      return;
    }
    if (*x == '\n' &&
        !g_ascii_strncasecmp ("content-type:", (gchar *) x + 1, 13)) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MULTIPART_CAPS);
      return;
    }
  }
}

/*** video/mpeg systemstream ***/
static GstStaticCaps mpeg_sys_caps = GST_STATIC_CAPS ("video/mpeg, "
    "systemstream = (boolean) true, mpegversion = (int) [ 1, 2 ]");

#define MPEG_SYS_CAPS gst_static_caps_get(&mpeg_sys_caps)
#define IS_MPEG_HEADER(data) (G_UNLIKELY((((guint8 *)(data))[0] == 0x00) &&  \
                                         (((guint8 *)(data))[1] == 0x00) &&  \
                                         (((guint8 *)(data))[2] == 0x01)))

#define IS_MPEG_PACK_CODE(b) ((b) == 0xBA)
#define IS_MPEG_SYS_CODE(b) ((b) == 0xBB)
#define IS_MPEG_PACK_HEADER(data)       (IS_MPEG_HEADER (data) &&            \
                                         IS_MPEG_PACK_CODE (((guint8 *)(data))[3]))

#define IS_MPEG_PES_CODE(b) (((b) & 0xF0) == 0xE0 || ((b) & 0xF0) == 0xC0 || \
                             (b) >= 0xBD)
#define IS_MPEG_PES_HEADER(data)        (IS_MPEG_HEADER (data) &&            \
                                         IS_MPEG_PES_CODE (((guint8 *)(data))[3]))

#define MPEG2_MAX_PROBE_LENGTH (128 * 1024)     /* 128kB should be 64 packs of the 
                                                 * most common 2kB pack size. */

#define MPEG2_MIN_SYS_HEADERS 2
#define MPEG2_MAX_SYS_HEADERS 5

static gboolean
mpeg_sys_is_valid_pack (GstTypeFind * tf, const guint8 * data, guint len,
    guint * pack_size)
{
  /* Check the pack header @ offset for validity, assuming that the 4 byte header
   * itself has already been checked. */
  guint8 stuff_len;

  if (len < 12)
    return FALSE;

  /* Check marker bits */
  if ((data[4] & 0xC4) == 0x44) {
    /* MPEG-2 PACK */
    if (len < 14)
      return FALSE;

    if ((data[6] & 0x04) != 0x04 ||
        (data[8] & 0x04) != 0x04 ||
        (data[9] & 0x01) != 0x01 || (data[12] & 0x03) != 0x03)
      return FALSE;

    stuff_len = data[13] & 0x07;

    /* Check the following header bytes, if we can */
    if ((14 + stuff_len + 4) <= len) {
      if (!IS_MPEG_HEADER (data + 14 + stuff_len))
        return FALSE;
    }
    if (pack_size)
      *pack_size = 14 + stuff_len;
    return TRUE;
  } else if ((data[4] & 0xF1) == 0x21) {
    /* MPEG-1 PACK */
    if ((data[6] & 0x01) != 0x01 ||
        (data[8] & 0x01) != 0x01 ||
        (data[9] & 0x80) != 0x80 || (data[11] & 0x01) != 0x01)
      return FALSE;

    /* Check the following header bytes, if we can */
    if ((12 + 4) <= len) {
      if (!IS_MPEG_HEADER (data + 12))
        return FALSE;
    }
    if (pack_size)
      *pack_size = 12;
    return TRUE;
  }

  return FALSE;
}

static gboolean
mpeg_sys_is_valid_pes (GstTypeFind * tf, guint8 * data, guint len,
    guint * pack_size)
{
  guint pes_packet_len;

  /* Check the PES header at the given position, assuming the header code itself
   * was already checked */
  if (len < 6)
    return FALSE;

  /* For MPEG Program streams, unbounded PES is not allowed, so we must have a
   * valid length present */
  pes_packet_len = GST_READ_UINT16_BE (data + 4);
  if (pes_packet_len == 0)
    return FALSE;

  /* Check the following header, if we can */
  if (6 + pes_packet_len + 4 <= len) {
    if (!IS_MPEG_HEADER (data + 6 + pes_packet_len))
      return FALSE;
  }

  if (pack_size)
    *pack_size = 6 + pes_packet_len;
  return TRUE;
}

static gboolean
mpeg_sys_is_valid_sys (GstTypeFind * tf, guint8 * data, guint len,
    guint * pack_size)
{
  guint sys_hdr_len;

  /* Check the System header at the given position, assuming the header code itself
   * was already checked */
  if (len < 6)
    return FALSE;
  sys_hdr_len = GST_READ_UINT16_BE (data + 4);
  if (sys_hdr_len < 6)
    return FALSE;

  /* Check the following header, if we can */
  if (6 + sys_hdr_len + 4 <= len) {
    if (!IS_MPEG_HEADER (data + 6 + sys_hdr_len))
      return FALSE;
  }

  if (pack_size)
    *pack_size = 6 + sys_hdr_len;

  return TRUE;
}

/* calculation of possibility to identify random data as mpeg systemstream:
 * bits that must match in header detection:            32 (or more)
 * chance that random data is identifed:                1/2^32
 * chance that MPEG2_MIN_PACK_HEADERS headers are identified:
 *       1/2^(32*MPEG2_MIN_PACK_HEADERS)
 * chance that this happens in MPEG2_MAX_PROBE_LENGTH bytes:
 *       1-(1+1/2^(32*MPEG2_MIN_PACK_HEADERS)^MPEG2_MAX_PROBE_LENGTH)
 * for current values:
 *       1-(1+1/2^(32*4)^101024)
 *       = <some_number>
 * Since we also check marker bits and pes packet lengths, this probability is a
 * very coarse upper bound.
 */
static void
mpeg_sys_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data, *data0, *first_sync, *end;
  gint mpegversion = 0;
  guint pack_headers = 0;
  guint pes_headers = 0;
  guint pack_size;
  guint since_last_sync = 0;
  guint32 sync_word = 0xffffffff;

  G_STMT_START {
    gint len;

    len = MPEG2_MAX_PROBE_LENGTH;
    do {
      len = len / 2;
      data = gst_type_find_peek (tf, 0, 5 + len);
    } while (data == NULL && len >= 32);

    if (!data)
      return;

    end = data + len;
  }
  G_STMT_END;

  data0 = data;
  first_sync = NULL;

  while (data < end) {
    sync_word <<= 8;
    if (sync_word == 0x00000100) {
      /* Found potential sync word */
      if (first_sync == NULL)
        first_sync = data - 3;

      if (since_last_sync > 4) {
        /* If more than 4 bytes since the last sync word, reset our counters,
         * as we're only interested in counting contiguous packets */
        pes_headers = pack_headers = 0;
      }
      pack_size = 0;

      if (IS_MPEG_PACK_CODE (data[0])) {
        if ((data[1] & 0xC0) == 0x40) {
          /* MPEG-2 */
          mpegversion = 2;
        } else if ((data[1] & 0xF0) == 0x20) {
          mpegversion = 1;
        }
        if (mpegversion != 0 &&
            mpeg_sys_is_valid_pack (tf, data - 3, end - data + 3, &pack_size)) {
          pack_headers++;
        }
      } else if (IS_MPEG_PES_CODE (data[0])) {
        /* PES stream */
        if (mpeg_sys_is_valid_pes (tf, data - 3, end - data + 3, &pack_size)) {
          pes_headers++;
          if (mpegversion == 0)
            mpegversion = 2;
        }
      } else if (IS_MPEG_SYS_CODE (data[0])) {
        if (mpeg_sys_is_valid_sys (tf, data - 3, end - data + 3, &pack_size)) {
          pack_headers++;
        }
      }

      /* If we found a packet with a known size, skip the bytes in it and loop
       * around to check the next packet. */
      if (pack_size != 0) {
        data += pack_size - 3;
        sync_word = 0xffffffff;
        since_last_sync = 0;
        continue;
      }
    }

    sync_word |= data[0];
    since_last_sync++;
    data++;

    /* If we have found MAX headers, and *some* were pes headers (pack headers
     * are optional in an mpeg system stream) then return our high-probability
     * result */
    if (pes_headers > 0 && (pack_headers + pes_headers) > MPEG2_MAX_SYS_HEADERS)
      goto suggest;
  }

  /* If we at least saw MIN headers, and *some* were pes headers (pack headers
   * are optional in an mpeg system stream) then return a lower-probability 
   * result */
  if (pes_headers > 0 && (pack_headers + pes_headers) > MPEG2_MIN_SYS_HEADERS)
    goto suggest;

  return;
suggest:
  {
    guint prob;

    prob = GST_TYPE_FIND_POSSIBLE + (10 * (pack_headers + pes_headers));
    prob = MIN (prob, GST_TYPE_FIND_MAXIMUM);

    /* lower probability if the first packet wasn't right at the start */
    if (data0 != first_sync && prob >= 10)
      prob -= 10;

    GST_LOG ("Suggesting MPEG %d system stream, %d packs, %d pes, prob %u%%\n",
        mpegversion, pack_headers, pes_headers, prob);

    gst_type_find_suggest_simple (tf, prob, "video/mpeg",
        "systemstream", G_TYPE_BOOLEAN, TRUE,
        "mpegversion", G_TYPE_INT, mpegversion, NULL);
  }
};

/*** video/mpegts Transport Stream ***/
static GstStaticCaps mpegts_caps = GST_STATIC_CAPS ("video/mpegts, "
    "systemstream = (boolean) true, packetsize = (int) [ 188, 208 ]");
#define MPEGTS_CAPS gst_static_caps_get(&mpegts_caps)

#define GST_MPEGTS_TYPEFIND_MIN_HEADERS 4
#define GST_MPEGTS_TYPEFIND_MAX_HEADERS 10
#define GST_MPEGTS_MAX_PACKET_SIZE 208
#define GST_MPEGTS_TYPEFIND_SYNC_SIZE \
            (GST_MPEGTS_TYPEFIND_MIN_HEADERS * GST_MPEGTS_MAX_PACKET_SIZE)
#define GST_MPEGTS_TYPEFIND_MAX_SYNC \
            (GST_MPEGTS_TYPEFIND_MAX_HEADERS * GST_MPEGTS_MAX_PACKET_SIZE)
#define GST_MPEGTS_TYPEFIND_SCAN_LENGTH \
            (GST_MPEGTS_TYPEFIND_MAX_SYNC * 4)

#define MPEGTS_HDR_SIZE 4
/* Check for sync byte, error_indicator == 0 and packet has payload */
#define IS_MPEGTS_HEADER(data) (((data)[0] == 0x47) && \
                                (((data)[1] & 0x80) == 0x00) && \
                                (((data)[3] & 0x30) != 0x00))

/* Helper function to search ahead at intervals of packet_size for mpegts
 * headers */
static gint
mpeg_ts_probe_headers (GstTypeFind * tf, guint64 offset, gint packet_size)
{
  /* We always enter this function having found at least one header already */
  gint found = 1;
  guint8 *data = NULL;

  GST_LOG ("looking for mpeg-ts packets of size %u", packet_size);
  while (found < GST_MPEGTS_TYPEFIND_MAX_HEADERS) {
    offset += packet_size;

    data = gst_type_find_peek (tf, offset, MPEGTS_HDR_SIZE);
    if (data == NULL || !IS_MPEGTS_HEADER (data))
      return found;

    found++;
    GST_LOG ("mpeg-ts sync #%2d at offset %" G_GUINT64_FORMAT, found, offset);
  }

  return found;
}

/* Try and detect at least 4 packets in at most 10 packets worth of
 * data. Need to try several possible packet sizes */
static void
mpeg_ts_type_find (GstTypeFind * tf, gpointer unused)
{
  /* TS packet sizes to test: normal, DVHS packet size and 
   * FEC with 16 or 20 byte codes packet size. */
  const gint pack_sizes[] = { 188, 192, 204, 208 };

  guint8 *data = NULL;
  guint size = 0;
  guint64 skipped = 0;

  while (skipped < GST_MPEGTS_TYPEFIND_SCAN_LENGTH) {
    if (size < MPEGTS_HDR_SIZE) {
      data = gst_type_find_peek (tf, skipped, GST_MPEGTS_TYPEFIND_SYNC_SIZE);
      if (!data)
        break;
      size = GST_MPEGTS_TYPEFIND_SYNC_SIZE;
    }

    /* Have at least MPEGTS_HDR_SIZE bytes at this point */
    if (IS_MPEGTS_HEADER (data)) {
      gint p;

      GST_LOG ("possible mpeg-ts sync at offset %" G_GUINT64_FORMAT, skipped);

      for (p = 0; p < G_N_ELEMENTS (pack_sizes); p++) {
        gint found;

        /* Probe ahead at size pack_sizes[p] */
        found = mpeg_ts_probe_headers (tf, skipped, pack_sizes[p]);
        if (found >= GST_MPEGTS_TYPEFIND_MIN_HEADERS) {
          gint probability;

          /* found at least 4 headers. 10 headers = MAXIMUM probability. 
           * Arbitrarily, I assigned 10% probability for each header we
           * found, 40% -> 100% */
          probability = MIN (10 * found, GST_TYPE_FIND_MAXIMUM);

          gst_type_find_suggest_simple (tf, probability, "video/mpegts",
              "systemstream", G_TYPE_BOOLEAN, TRUE,
              "packetsize", G_TYPE_INT, pack_sizes[p], NULL);
          return;
        }
      }
    }
    data++;
    skipped++;
    size--;
  }
}

#define GST_MPEGVID_TYPEFIND_TRY_PICTURES 6
#define GST_MPEGVID_TYPEFIND_TRY_SYNC (100 * 1024)      /* 100 kB */

/* Scan ahead a maximum of max_extra_offset bytes until the next IS_MPEG_HEADER
 * offset.  After the call, offset will be after the 0x000001, i.e. at the 4th
 * byte of the MPEG header.  Returns TRUE if a header was found, FALSE if not.
 */
static gboolean
mpeg_find_next_header (GstTypeFind * tf, DataScanCtx * c,
    guint64 max_extra_offset)
{
  guint64 extra_offset;

  for (extra_offset = 0; extra_offset <= max_extra_offset; ++extra_offset) {
    if (!data_scan_ctx_ensure_data (tf, c, 4))
      return FALSE;
    if (IS_MPEG_HEADER (c->data)) {
      data_scan_ctx_advance (tf, c, 3);
      return TRUE;
    }
    data_scan_ctx_advance (tf, c, 1);
  }
  return FALSE;
}

/*** video/mpeg MPEG-4 elementary video stream ***/

static GstStaticCaps mpeg4_video_caps = GST_STATIC_CAPS ("video/mpeg, "
    "systemstream=(boolean)false, mpegversion=4, parsed=(boolean)false");
#define MPEG4_VIDEO_CAPS gst_static_caps_get(&mpeg4_video_caps)

/*
 * This typefind is based on the elementary video header defined in
 * http://xhelmboyx.tripod.com/formats/mpeg-layout.txt
 * In addition, it allows the visual object sequence header to be
 * absent, and even the VOS header to be absent.  In the latter case,
 * a number of VOPs have to be present.
 */
static void
mpeg4_video_type_find (GstTypeFind * tf, gpointer unused)
{
  DataScanCtx c = { 0, NULL, 0 };
  gboolean seen_vios_at_0 = FALSE;
  gboolean seen_vios = FALSE;
  gboolean seen_vos = FALSE;
  gboolean seen_vol = FALSE;
  guint num_vop_headers = 0;
  guint8 sc;

  while (c.offset < GST_MPEGVID_TYPEFIND_TRY_SYNC) {
    if (num_vop_headers >= GST_MPEGVID_TYPEFIND_TRY_PICTURES)
      break;

    if (!mpeg_find_next_header (tf, &c,
            GST_MPEGVID_TYPEFIND_TRY_SYNC - c.offset))
      break;

    sc = c.data[0];

    /* visual_object_sequence_start_code */
    if (sc == 0xB0) {
      if (seen_vios)
        break;                  /* Terminate at second vios */
      if (c.offset == 0)
        seen_vios_at_0 = TRUE;
      seen_vios = TRUE;
      data_scan_ctx_advance (tf, &c, 2);
      if (!mpeg_find_next_header (tf, &c, 0))
        break;

      sc = c.data[0];

      /* Optional metadata */
      if (sc == 0xB2)
        if (!mpeg_find_next_header (tf, &c, 24))
          break;
    }

    /* visual_object_start_code (consider it optional) */
    if (sc == 0xB5) {
      data_scan_ctx_advance (tf, &c, 2);
      /* may contain ID marker and YUV clamping */
      if (!mpeg_find_next_header (tf, &c, 7))
        break;

      sc = c.data[0];
    }

    /* video_object_start_code */
    if (sc <= 0x1F) {
      if (seen_vos)
        break;                  /* Terminate at second vos */
      seen_vos = TRUE;
      data_scan_ctx_advance (tf, &c, 2);
      continue;
    }

    /* video_object_layer_start_code */
    if (sc >= 0x20 && sc <= 0x2F) {
      seen_vol = TRUE;
      data_scan_ctx_advance (tf, &c, 5);
      continue;
    }

    /* video_object_plane_start_code */
    if (sc == 0xB6) {
      num_vop_headers++;
      data_scan_ctx_advance (tf, &c, 2);
      continue;
    }

    /* Unknown start code. */
  }

  if (num_vop_headers > 0 || seen_vol) {
    GstTypeFindProbability probability = 0;

    GST_LOG ("Found %d pictures, vios: %d, vos:%d, vol:%d", num_vop_headers,
        seen_vios, seen_vos, seen_vol);

    if (num_vop_headers >= GST_MPEGVID_TYPEFIND_TRY_PICTURES && seen_vios_at_0
        && seen_vos && seen_vol)
      probability = GST_TYPE_FIND_MAXIMUM - 1;
    else if (num_vop_headers >= GST_MPEGVID_TYPEFIND_TRY_PICTURES && seen_vios
        && seen_vos && seen_vol)
      probability = GST_TYPE_FIND_NEARLY_CERTAIN - 1;
    else if (seen_vios_at_0 && seen_vos && seen_vol)
      probability = GST_TYPE_FIND_NEARLY_CERTAIN - 6;
    else if (num_vop_headers >= GST_MPEGVID_TYPEFIND_TRY_PICTURES && seen_vos
        && seen_vol)
      probability = GST_TYPE_FIND_NEARLY_CERTAIN - 6;
    else if (num_vop_headers >= GST_MPEGVID_TYPEFIND_TRY_PICTURES && seen_vol)
      probability = GST_TYPE_FIND_NEARLY_CERTAIN - 9;
    else if (num_vop_headers >= GST_MPEGVID_TYPEFIND_TRY_PICTURES)
      probability = GST_TYPE_FIND_LIKELY - 1;
    else if (num_vop_headers > 2 && seen_vios && seen_vos && seen_vol)
      probability = GST_TYPE_FIND_LIKELY - 9;
    else if (seen_vios && seen_vos && seen_vol)
      probability = GST_TYPE_FIND_LIKELY - 20;
    else if (num_vop_headers > 0 && seen_vos && seen_vol)
      probability = GST_TYPE_FIND_POSSIBLE;
    else if (num_vop_headers > 0)
      probability = GST_TYPE_FIND_POSSIBLE - 10;
    else if (seen_vos && seen_vol)
      probability = GST_TYPE_FIND_POSSIBLE - 20;

    gst_type_find_suggest (tf, probability, MPEG4_VIDEO_CAPS);
  }
}

/*** video/x-h263 H263 video stream ***/
static GstStaticCaps h263_video_caps = GST_STATIC_CAPS ("video/x-h263");

#define H263_VIDEO_CAPS gst_static_caps_get(&h263_video_caps)

#define H263_MAX_PROBE_LENGTH (128 * 1024)

static void
h263_video_type_find (GstTypeFind * tf, gpointer unused)
{
  DataScanCtx c = { 0, NULL, 0 };
  guint64 data = 0;
  guint64 psc = 0;
  guint8 tr = 0;
  guint format;
  guint good = 0;
  guint bad = 0;

  while (c.offset < H263_MAX_PROBE_LENGTH) {
    if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 4)))
      break;

    /* Find the picture start code */
    data = (data << 8) + c.data[0];
    psc = data & G_GUINT64_CONSTANT (0xfffffc0000);
    if (psc == 0x800000) {
      /* Found PSC */
      /* TR */
      tr = (data & 0x3fc) >> 2;
      /* Source Format */
      format = tr & 0x07;

      /* Now that we have a Valid PSC, check if we also have a valid PTYPE and
         the Source Format, which should range between 1 and 5 */
      if (((tr >> 6) == 0x2) && (format > 0 && format < 6))
        good++;
      else
        bad++;

      /* FIXME: maybe bail out early if we get mostly bad syncs ? */
    }

    data_scan_ctx_advance (tf, &c, 1);
  }

  if (good > 0 && bad == 0)
    gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY, H263_VIDEO_CAPS);
  else if (good > 2 * bad)
    gst_type_find_suggest (tf, GST_TYPE_FIND_POSSIBLE, H263_VIDEO_CAPS);

  return;
}

/*** video/x-h264 H264 elementary video stream ***/

static GstStaticCaps h264_video_caps =
GST_STATIC_CAPS ("video/x-h264,stream-format=byte-stream");

#define H264_VIDEO_CAPS gst_static_caps_get(&h264_video_caps)

#define H264_MAX_PROBE_LENGTH (128 * 1024)      /* 128kB for HD should be enough. */

static void
h264_video_type_find (GstTypeFind * tf, gpointer unused)
{
  DataScanCtx c = { 0, NULL, 0 };

  /* Stream consists of: a series of sync codes (00 00 00 01) followed 
   * by NALs
   */
  int nut, ref;
  int good = 0;
  int bad = 0;

  while (c.offset < H264_MAX_PROBE_LENGTH) {
    if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 4)))
      break;

    if (IS_MPEG_HEADER (c.data)) {
      nut = c.data[3] & 0x9f;   /* forbiden_zero_bit | nal_unit_type */
      ref = c.data[3] & 0x60;   /* nal_ref_idc */

      /* if forbiden bit is different to 0 won't be h264 */
      if (nut > 0x1f) {
        bad++;
        break;
      }

      /* collect statistics about the NAL types */
      if ((nut >= 1 && nut <= 13) || nut == 19) {
        if ((nut == 5 && ref == 0) ||
            ((nut == 6 || (nut >= 9 && nut <= 12)) && ref != 0)) {
          bad++;
        } else {
          good++;
        }
      } else if (nut >= 14 && nut <= 33) {
        /* reserved */
        /* Theoretically these are good, since if they exist in the
           stream it merely means that a newer backwards-compatible
           h.264 stream.  But we should be identifying that separately. */
        bad++;
      } else {
        /* unspecified, application specific */
        /* don't consider these bad */
      }

      GST_DEBUG ("good %d bad %d", good, bad);

      if (good >= 10 && bad < 4) {
        gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY, H264_VIDEO_CAPS);
        return;
      }

      data_scan_ctx_advance (tf, &c, 4);
    }
    data_scan_ctx_advance (tf, &c, 1);
  }

  if (good >= 2 && bad < 1) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_POSSIBLE, H264_VIDEO_CAPS);
    return;
  }
}

/*** video/mpeg video stream ***/

static GstStaticCaps mpeg_video_caps = GST_STATIC_CAPS ("video/mpeg, "
    "systemstream = (boolean) false");
#define MPEG_VIDEO_CAPS gst_static_caps_get(&mpeg_video_caps)

/*
 * Idea is the same as MPEG system stream typefinding: We check each
 * byte of the stream to see if - from that point on - the stream
 * matches a predefined set of marker bits as defined in the MPEG
 * video specs.
 *
 * I'm sure someone will do a chance calculation here too.
 */

static void
mpeg_video_stream_type_find (GstTypeFind * tf, gpointer unused)
{
  DataScanCtx c = { 0, NULL, 0 };
  gboolean seen_seq_at_0 = FALSE;
  gboolean seen_seq = FALSE;
  gboolean seen_gop = FALSE;
  guint64 last_pic_offset = 0;
  guint num_pic_headers = 0;
  gint found = 0;

  while (c.offset < GST_MPEGVID_TYPEFIND_TRY_SYNC) {
    if (found >= GST_MPEGVID_TYPEFIND_TRY_PICTURES)
      break;

    if (!data_scan_ctx_ensure_data (tf, &c, 5))
      break;

    if (!IS_MPEG_HEADER (c.data))
      goto next;

    /* a pack header indicates that this isn't an elementary stream */
    if (c.data[3] == 0xBA && mpeg_sys_is_valid_pack (tf, c.data, c.size, NULL))
      return;

    /* do we have a sequence header? */
    if (c.data[3] == 0xB3) {
      seen_seq_at_0 = seen_seq_at_0 || (c.offset == 0);
      seen_seq = TRUE;
      data_scan_ctx_advance (tf, &c, 4 + 8);
      continue;
    }

    /* or a GOP header */
    if (c.data[3] == 0xB8) {
      seen_gop = TRUE;
      data_scan_ctx_advance (tf, &c, 8);
      continue;
    }

    /* but what we'd really like to see is a picture header */
    if (c.data[3] == 0x00) {
      ++num_pic_headers;
      last_pic_offset = c.offset;
      data_scan_ctx_advance (tf, &c, 8);
      continue;
    }

    /* ... each followed by a slice header with slice_vertical_pos=1 that's
     * not too far away from the previously seen picture header. */
    if (c.data[3] == 0x01 && num_pic_headers > found &&
        (c.offset - last_pic_offset) >= 4 &&
        (c.offset - last_pic_offset) <= 64) {
      data_scan_ctx_advance (tf, &c, 4);
      found += 1;
      continue;
    }

  next:

    data_scan_ctx_advance (tf, &c, 1);
  }

  if (found > 0 || seen_seq) {
    GstTypeFindProbability probability = 0;

    GST_LOG ("Found %d pictures, seq:%d, gop:%d", found, seen_seq, seen_gop);

    if (found >= GST_MPEGVID_TYPEFIND_TRY_PICTURES && seen_seq && seen_gop)
      probability = GST_TYPE_FIND_NEARLY_CERTAIN - 1;
    else if (found >= GST_MPEGVID_TYPEFIND_TRY_PICTURES && seen_seq)
      probability = GST_TYPE_FIND_NEARLY_CERTAIN - 9;
    else if (found >= GST_MPEGVID_TYPEFIND_TRY_PICTURES)
      probability = GST_TYPE_FIND_LIKELY;
    else if (seen_seq_at_0 && seen_gop && found > 2)
      probability = GST_TYPE_FIND_LIKELY - 10;
    else if (seen_seq && seen_gop && found > 2)
      probability = GST_TYPE_FIND_LIKELY - 20;
    else if (seen_seq_at_0 && found > 0)
      probability = GST_TYPE_FIND_POSSIBLE;
    else if (seen_seq && found > 0)
      probability = GST_TYPE_FIND_POSSIBLE - 5;
    else if (found > 0)
      probability = GST_TYPE_FIND_POSSIBLE - 10;
    else if (seen_seq)
      probability = GST_TYPE_FIND_POSSIBLE - 20;

    gst_type_find_suggest_simple (tf, probability, "video/mpeg",
        "systemstream", G_TYPE_BOOLEAN, FALSE,
        "mpegversion", G_TYPE_INT, 1, NULL);
  }
}

/*** audio/x-aiff ***/

static GstStaticCaps aiff_caps = GST_STATIC_CAPS ("audio/x-aiff");

#define AIFF_CAPS gst_static_caps_get(&aiff_caps)
static void
aiff_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 4);

  if (data && memcmp (data, "FORM", 4) == 0) {
    data += 8;
    if (memcmp (data, "AIFF", 4) == 0 || memcmp (data, "AIFC", 4) == 0)
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, AIFF_CAPS);
  }
}

/*** audio/x-svx ***/

static GstStaticCaps svx_caps = GST_STATIC_CAPS ("audio/x-svx");

#define SVX_CAPS gst_static_caps_get(&svx_caps)
static void
svx_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 4);

  if (data && memcmp (data, "FORM", 4) == 0) {
    data += 8;
    if (memcmp (data, "8SVX", 4) == 0 || memcmp (data, "16SV", 4) == 0)
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, SVX_CAPS);
  }
}

/*** audio/x-shorten ***/

static GstStaticCaps shn_caps = GST_STATIC_CAPS ("audio/x-shorten");

#define SHN_CAPS gst_static_caps_get(&shn_caps)
static void
shn_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 4);

  if (data && memcmp (data, "ajkg", 4) == 0) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, SHN_CAPS);
  }
  data = gst_type_find_peek (tf, -8, 8);
  if (data && memcmp (data, "SHNAMPSK", 8) == 0) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, SHN_CAPS);
  }
}

/*** application/x-ape ***/

static GstStaticCaps ape_caps = GST_STATIC_CAPS ("application/x-ape");

#define APE_CAPS gst_static_caps_get(&ape_caps)
static void
ape_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 4);

  if (data && memcmp (data, "MAC ", 4) == 0) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY + 10, APE_CAPS);
  }
}

/*** ISO FORMATS ***/

/*** audio/x-m4a ***/

static GstStaticCaps m4a_caps = GST_STATIC_CAPS ("audio/x-m4a");

#define M4A_CAPS (gst_static_caps_get(&m4a_caps))
static void
m4a_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 4, 8);

  if (data && (memcmp (data, "ftypM4A ", 8) == 0)) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, M4A_CAPS);
  }
}

/*** application/x-3gp ***/

/* The Q is there because variables can't start with a number. */
static GstStaticCaps q3gp_caps = GST_STATIC_CAPS ("application/x-3gp");
#define Q3GP_CAPS (gst_static_caps_get(&q3gp_caps))

static const gchar *
q3gp_type_find_get_profile (const guint8 * data)
{
  switch (GST_MAKE_FOURCC (data[0], data[1], data[2], 0)) {
    case GST_MAKE_FOURCC ('3', 'g', 'g', 0):
      return "general";
    case GST_MAKE_FOURCC ('3', 'g', 'p', 0):
      return "basic";
    case GST_MAKE_FOURCC ('3', 'g', 's', 0):
      return "streaming-server";
    case GST_MAKE_FOURCC ('3', 'g', 'r', 0):
      return "progressive-download";
    default:
      break;
  }
  return NULL;
}

static void
q3gp_type_find (GstTypeFind * tf, gpointer unused)
{
  const gchar *profile;
  guint32 ftyp_size = 0;
  gint offset = 0;
  guint8 *data = NULL;

  if ((data = gst_type_find_peek (tf, 0, 12)) == NULL) {
    return;
  }

  data += 4;
  if (memcmp (data, "ftyp", 4) != 0) {
    return;
  }

  /* check major brand */
  data += 4;
  if ((profile = q3gp_type_find_get_profile (data))) {
    gst_type_find_suggest_simple (tf, GST_TYPE_FIND_MAXIMUM,
        "application/x-3gp", "profile", G_TYPE_STRING, profile, NULL);
    return;
  }

  /* check compatible brands */
  if ((data = gst_type_find_peek (tf, 0, 4)) != NULL) {
    ftyp_size = GST_READ_UINT32_BE (data);
  }
  for (offset = 16; offset < ftyp_size; offset += 4) {
    if ((data = gst_type_find_peek (tf, offset, 3)) == NULL) {
      break;
    }
    if ((profile = q3gp_type_find_get_profile (data))) {
      gst_type_find_suggest_simple (tf, GST_TYPE_FIND_MAXIMUM,
          "application/x-3gp", "profile", G_TYPE_STRING, profile, NULL);
      return;
    }
  }

  return;

}

/*** video/mj2 and image/jp2 ***/
static GstStaticCaps mj2_caps = GST_STATIC_CAPS ("video/mj2");

#define MJ2_CAPS gst_static_caps_get(&mj2_caps)

static GstStaticCaps jp2_caps = GST_STATIC_CAPS ("image/jp2");

#define JP2_CAPS gst_static_caps_get(&jp2_caps)

static void
jp2_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data;

  data = gst_type_find_peek (tf, 0, 24);
  if (!data)
    return;

  /* jp2 signature */
  if (memcmp (data, "\000\000\000\014jP  \015\012\207\012", 12) != 0)
    return;

  /* check ftyp box */
  data += 12;
  if (memcmp (data + 4, "ftyp", 4) == 0) {
    if (memcmp (data + 8, "jp2 ", 4) == 0)
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, JP2_CAPS);
    else if (memcmp (data + 8, "mjp2", 4) == 0)
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MJ2_CAPS);
  }
}

/*** video/quicktime ***/

static GstStaticCaps qt_caps = GST_STATIC_CAPS ("video/quicktime");

#define QT_CAPS gst_static_caps_get(&qt_caps)
#define STRNCMP(x,y,z) (strncmp ((char*)(x), (char*)(y), z))

/* FIXME 0.11: go through http://www.ftyps.com/ */
static void
qt_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data;
  guint tip = 0;
  guint64 offset = 0;
  guint64 size;
  const gchar *variant = NULL;

  while ((data = gst_type_find_peek (tf, offset, 12)) != NULL) {
    guint64 new_offset;

    if (STRNCMP (&data[4], "ftypqt  ", 8) == 0) {
      tip = GST_TYPE_FIND_MAXIMUM;
      break;
    }

    if (STRNCMP (&data[4], "ftypisom", 8) == 0 ||
        STRNCMP (&data[4], "ftypavc1", 8) == 0 ||
        STRNCMP (&data[4], "ftypmp42", 8) == 0) {
      tip = GST_TYPE_FIND_MAXIMUM;
      variant = "iso";
      break;
    }

    /* box/atom types that are in common with ISO base media file format */
    if (STRNCMP (&data[4], "moov", 4) == 0 ||
        STRNCMP (&data[4], "mdat", 4) == 0 ||
        STRNCMP (&data[4], "ftyp", 4) == 0 ||
        STRNCMP (&data[4], "free", 4) == 0 ||
        STRNCMP (&data[4], "uuid", 4) == 0 ||
        STRNCMP (&data[4], "skip", 4) == 0) {
      if (tip == 0) {
        tip = GST_TYPE_FIND_LIKELY;
      } else {
        tip = GST_TYPE_FIND_NEARLY_CERTAIN;
      }
    }
    /* other box/atom types, apparently quicktime specific */
    else if (STRNCMP (&data[4], "pnot", 4) == 0 ||
        STRNCMP (&data[4], "PICT", 4) == 0 ||
        STRNCMP (&data[4], "wide", 4) == 0 ||
        STRNCMP (&data[4], "prfl", 4) == 0) {
      tip = GST_TYPE_FIND_MAXIMUM;
      break;
    } else {
      tip = 0;
      break;
    }

    size = GST_READ_UINT32_BE (data);
    /* check compatible brands rather than ever expaning major brands above */
    if ((STRNCMP (&data[4], "ftyp", 4) == 0) && (size >= 16)) {
      new_offset = offset + 12;
      while (new_offset + 4 <= offset + size) {
        data = gst_type_find_peek (tf, new_offset, 4);
        if (data == NULL)
          goto done;
        if (STRNCMP (&data[4], "isom", 4) == 0 ||
            STRNCMP (&data[4], "avc1", 4) == 0 ||
            STRNCMP (&data[4], "mp41", 4) == 0 ||
            STRNCMP (&data[4], "mp42", 4) == 0) {
          tip = GST_TYPE_FIND_MAXIMUM;
          variant = "iso";
          goto done;
        }
        new_offset += 4;
      }
    }
    if (size == 1) {
      guint8 *sizedata;

      sizedata = gst_type_find_peek (tf, offset + 8, 8);
      if (sizedata == NULL)
        break;

      size = GST_READ_UINT64_BE (sizedata);
    } else {
      if (size < 8)
        break;
    }
    new_offset = offset + size;
    if (new_offset <= offset)
      break;
    offset = new_offset;
  }

done:
  if (tip > 0) {
    if (variant) {
      GstCaps *caps = gst_caps_copy (QT_CAPS);

      gst_caps_set_simple (caps, "variant", G_TYPE_STRING, variant, NULL);
      gst_type_find_suggest (tf, tip, caps);
      gst_caps_unref (caps);
    } else {
      gst_type_find_suggest (tf, tip, QT_CAPS);
    }
  }
};


/*** image/x-quicktime ***/

static GstStaticCaps qtif_caps = GST_STATIC_CAPS ("image/x-quicktime");

#define QTIF_CAPS gst_static_caps_get(&qtif_caps)

/* how many atoms we check before we give up */
#define QTIF_MAXROUNDS 25

static void
qtif_type_find (GstTypeFind * tf, gpointer unused)
{
  const guint8 *data;
  gboolean found_idsc = FALSE;
  gboolean found_idat = FALSE;
  guint64 offset = 0;
  guint rounds = 0;

  while ((data = gst_type_find_peek (tf, offset, 8)) != NULL) {
    guint64 size;

    size = GST_READ_UINT32_BE (data);
    if (size == 1) {
      const guint8 *sizedata;

      sizedata = gst_type_find_peek (tf, offset + 8, 8);
      if (sizedata == NULL)
        break;

      size = GST_READ_UINT64_BE (sizedata);
    }
    if (size < 8)
      break;

    if (STRNCMP (data + 4, "idsc", 4) == 0)
      found_idsc = TRUE;
    if (STRNCMP (data + 4, "idat", 4) == 0)
      found_idat = TRUE;

    if (found_idsc && found_idat) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, QTIF_CAPS);
      return;
    }

    offset += size;
    if (++rounds > QTIF_MAXROUNDS)
      break;
  }

  if (found_idsc || found_idat) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY, QTIF_CAPS);
    return;
  }
};

/*** audio/x-mod ***/

static GstStaticCaps mod_caps = GST_STATIC_CAPS ("audio/x-mod");

#define MOD_CAPS gst_static_caps_get(&mod_caps)
/* FIXME: M15 CheckType to do */
static void
mod_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data;

  /* MOD */
  if ((data = gst_type_find_peek (tf, 1080, 4)) != NULL) {
    /* Protracker and variants */
    if ((memcmp (data, "M.K.", 4) == 0) || (memcmp (data, "M!K!", 4) == 0) ||
        /* Star Tracker */
        (memcmp (data, "FLT", 3) == 0 && isdigit (data[3])) ||
        (memcmp (data, "EXO", 3) == 0 && isdigit (data[3])) ||
        /* Oktalyzer (Amiga) */
        (memcmp (data, "OKTA", 4) == 0) ||
        /* Oktalyser (Atari) */
        (memcmp (data, "CD81", 4) == 0) ||
        /* Fasttracker */
        (memcmp (data + 1, "CHN", 3) == 0 && isdigit (data[0])) ||
        /* Fasttracker or Taketracker */
        (memcmp (data + 2, "CH", 2) == 0 && isdigit (data[0])
            && isdigit (data[1])) || (memcmp (data + 2, "CN", 2) == 0
            && isdigit (data[0]) && isdigit (data[1]))) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MOD_CAPS);
      return;
    }
  }
  /* XM */
  if ((data = gst_type_find_peek (tf, 0, 38)) != NULL) {
    if (memcmp (data, "Extended Module: ", 17) == 0 && data[37] == 0x1A) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MOD_CAPS);
      return;
    }
  }
  /* OKT */
  if (data || (data = gst_type_find_peek (tf, 0, 8)) != NULL) {
    if (memcmp (data, "OKTASONG", 8) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MOD_CAPS);
      return;
    }
  }
  if (data || (data = gst_type_find_peek (tf, 0, 4)) != NULL) {
    /* 669 */
    if ((memcmp (data, "if", 2) == 0) || (memcmp (data, "JN", 2) == 0)) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY, MOD_CAPS);
      return;
    }
    /* AMF */
    if ((memcmp (data, "AMF", 3) == 0 && data[3] > 10 && data[3] < 14) ||
        /* IT */
        (memcmp (data, "IMPM", 4) == 0) ||
        /* MED */
        (memcmp (data, "MMD0", 4) == 0) || (memcmp (data, "MMD1", 4) == 0) ||
        /* MTM */
        (memcmp (data, "MTM", 3) == 0)) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MOD_CAPS);
      return;
    }
    /* DSM */
    if (memcmp (data, "RIFF", 4) == 0) {
      guint8 *data2 = gst_type_find_peek (tf, 8, 4);

      if (data2) {
        if (memcmp (data2, "DSMF", 4) == 0) {
          gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MOD_CAPS);
          return;
        }
      }
    }
    /* FAM */
    if (memcmp (data, "FAM\xFE", 4) == 0) {
      guint8 *data2 = gst_type_find_peek (tf, 44, 3);

      if (data2) {
        if (memcmp (data2, "compare", 3) == 0) {
          gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MOD_CAPS);
          return;
        }
      } else {
        gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY, MOD_CAPS);
        return;
      }
    }
    /* GDM */
    if (memcmp (data, "GDM\xFE", 4) == 0) {
      guint8 *data2 = gst_type_find_peek (tf, 71, 4);

      if (data2) {
        if (memcmp (data2, "GMFS", 4) == 0) {
          gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MOD_CAPS);
          return;
        }
      } else {
        gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY, MOD_CAPS);
        return;
      }
    }
  }
  /* IMF */
  if ((data = gst_type_find_peek (tf, 60, 4)) != NULL) {
    if (memcmp (data, "IM10", 4) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MOD_CAPS);
      return;
    }
  }
  /* S3M */
  if ((data = gst_type_find_peek (tf, 44, 4)) != NULL) {
    if (memcmp (data, "SCRM", 4) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MOD_CAPS);
      return;
    }
  }
  /* STM */
  if ((data = gst_type_find_peek (tf, 20, 8)) != NULL) {
    if (g_ascii_strncasecmp ((gchar *) data, "!Scream!", 8) == 0 ||
        g_ascii_strncasecmp ((gchar *) data, "BMOD2STM", 8) == 0) {
      guint8 *id, *stmtype;

      if ((id = gst_type_find_peek (tf, 28, 1)) == NULL)
        return;
      if ((stmtype = gst_type_find_peek (tf, 29, 1)) == NULL)
        return;
      if (*id == 0x1A && *stmtype == 2)
        gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MOD_CAPS);
      return;
    }
  }
}

/*** application/x-shockwave-flash ***/

static GstStaticCaps swf_caps =
GST_STATIC_CAPS ("application/x-shockwave-flash");
#define SWF_CAPS (gst_static_caps_get(&swf_caps))
static void
swf_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 4);

  if (data && (data[0] == 'F' || data[0] == 'C') &&
      data[1] == 'W' && data[2] == 'S') {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, SWF_CAPS);
  }
}

/*** image/jpeg ***/

#define JPEG_MARKER_IS_START_OF_FRAME(x) \
    ((x)>=0xc0 && (x) <= 0xcf && (x)!=0xc4 && (x)!=0xc8 && (x)!=0xcc)

static GstStaticCaps jpeg_caps = GST_STATIC_CAPS ("image/jpeg");

#define JPEG_CAPS (gst_static_caps_get(&jpeg_caps))
static void
jpeg_type_find (GstTypeFind * tf, gpointer unused)
{
  GstTypeFindProbability prob = GST_TYPE_FIND_POSSIBLE;
  DataScanCtx c = { 0, NULL, 0 };
  GstCaps *caps;
  guint num_markers;

  if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 2)))
    return;

  if (c.data[0] != 0xff || c.data[1] != 0xd8)
    return;

  num_markers = 1;
  data_scan_ctx_advance (tf, &c, 2);

  caps = gst_caps_copy (JPEG_CAPS);

  while (data_scan_ctx_ensure_data (tf, &c, 4) && c.offset < (200 * 1024)) {
    guint16 len;
    guint8 marker;

    if (c.data[0] != 0xff)
      break;

    marker = c.data[1];
    if (G_UNLIKELY (marker == 0xff)) {
      data_scan_ctx_advance (tf, &c, 1);
      continue;
    }

    data_scan_ctx_advance (tf, &c, 2);

    /* we assume all markers we'll see before SOF have a payload length; if
     * that's not the case we'll just detect a false sync and bail out, but
     * still report POSSIBLE probability */
    len = GST_READ_UINT16_BE (c.data);

    GST_LOG ("possible JPEG marker 0x%02x (@0x%04x), segment length %u",
        marker, (guint) c.offset, len);

    if (!data_scan_ctx_ensure_data (tf, &c, len))
      break;

    if (marker == 0xc4 ||       /* DEFINE_HUFFMAN_TABLES          */
        marker == 0xcc ||       /* DEFINE_ARITHMETIC_CONDITIONING */
        marker == 0xdb ||       /* DEFINE_QUANTIZATION_TABLES     */
        marker == 0xdd ||       /* DEFINE_RESTART_INTERVAL        */
        marker == 0xfe) {       /* COMMENT                        */
      data_scan_ctx_advance (tf, &c, len);
      ++num_markers;
    } else if (marker == 0xe0 && len >= (2 + 4) &&      /* APP0 */
        data_scan_ctx_memcmp (tf, &c, 2, "JFIF", 4)) {
      GST_LOG ("found JFIF tag");
      prob = GST_TYPE_FIND_MAXIMUM;
      data_scan_ctx_advance (tf, &c, len);
      ++num_markers;
      /* we continue until we find a start of frame marker */
    } else if (marker == 0xe1 && len >= (2 + 4) &&      /* APP1 */
        data_scan_ctx_memcmp (tf, &c, 2, "Exif", 4)) {
      GST_LOG ("found Exif tag");
      prob = GST_TYPE_FIND_MAXIMUM;
      data_scan_ctx_advance (tf, &c, len);
      ++num_markers;
      /* we continue until we find a start of frame marker */
    } else if (marker >= 0xe0 && marker <= 0xef) {      /* APPn */
      data_scan_ctx_advance (tf, &c, len);
      ++num_markers;
    } else if (JPEG_MARKER_IS_START_OF_FRAME (marker) && len >= (2 + 8)) {
      int h, w;

      h = GST_READ_UINT16_BE (c.data + 2 + 1);
      w = GST_READ_UINT16_BE (c.data + 2 + 1 + 2);
      if (h == 0 || w == 0) {
        GST_WARNING ("bad width %u and/or height %u in SOF header", w, h);
        break;
      }

      GST_LOG ("SOF at offset %" G_GUINT64_FORMAT ", num_markers=%d, "
          "WxH=%dx%d", c.offset - 2, num_markers, w, h);

      if (num_markers >= 5 || prob == GST_TYPE_FIND_MAXIMUM)
        prob = GST_TYPE_FIND_MAXIMUM;
      else
        prob = GST_TYPE_FIND_LIKELY;

      gst_caps_set_simple (caps, "width", G_TYPE_INT, w,
          "height", G_TYPE_INT, h, NULL);
      break;
    } else {
      GST_WARNING ("bad length or unexpected JPEG marker 0xff 0x%02x", marker);
      break;
    }
  }

  gst_type_find_suggest (tf, prob, caps);
  gst_caps_unref (caps);
}

/*** image/bmp ***/

static GstStaticCaps bmp_caps = GST_STATIC_CAPS ("image/bmp");

#define BMP_CAPS (gst_static_caps_get(&bmp_caps))
static void
bmp_type_find (GstTypeFind * tf, gpointer unused)
{
  DataScanCtx c = { 0, NULL, 0 };
  guint32 struct_size, w, h, planes, bpp;

  if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 54)))
    return;

  if (c.data[0] != 'B' || c.data[1] != 'M')
    return;

  /* skip marker + size */
  data_scan_ctx_advance (tf, &c, 2 + 4);

  /* reserved, must be 0 */
  if (c.data[0] != 0 || c.data[1] != 0 || c.data[2] != 0 || c.data[3] != 0)
    return;

  data_scan_ctx_advance (tf, &c, 2 + 2);

  /* offset to start of image data in bytes (check for sanity) */
  GST_LOG ("offset=%u", GST_READ_UINT32_LE (c.data));
  if (GST_READ_UINT32_LE (c.data) > (10 * 1024 * 1024))
    return;

  struct_size = GST_READ_UINT32_LE (c.data + 4);
  GST_LOG ("struct_size=%u", struct_size);

  data_scan_ctx_advance (tf, &c, 4 + 4);

  if (struct_size == 0x0C) {
    w = GST_READ_UINT16_LE (c.data);
    h = GST_READ_UINT16_LE (c.data + 2);
    planes = GST_READ_UINT16_LE (c.data + 2 + 2);
    bpp = GST_READ_UINT16_LE (c.data + 2 + 2 + 2);
  } else if (struct_size == 40 || struct_size == 64 || struct_size == 108
      || struct_size == 124 || struct_size == 0xF0) {
    w = GST_READ_UINT32_LE (c.data);
    h = GST_READ_UINT32_LE (c.data + 4);
    planes = GST_READ_UINT16_LE (c.data + 4 + 4);
    bpp = GST_READ_UINT16_LE (c.data + 4 + 4 + 2);
  } else {
    return;
  }

  /* image sizes sanity check */
  GST_LOG ("w=%u, h=%u, planes=%u, bpp=%u", w, h, planes, bpp);
  if (w == 0 || w > 0xfffff || h == 0 || h > 0xfffff || planes != 1 ||
      (bpp != 1 && bpp != 4 && bpp != 8 && bpp != 16 && bpp != 24 && bpp != 32))
    return;

  gst_type_find_suggest_simple (tf, GST_TYPE_FIND_MAXIMUM, "image/bmp",
      "width", G_TYPE_INT, w, "height", G_TYPE_INT, h, "bpp", G_TYPE_INT, bpp,
      NULL);
}

/*** image/tiff ***/
static GstStaticCaps tiff_caps = GST_STATIC_CAPS ("image/tiff, "
    "endianness = (int) { BIG_ENDIAN, LITTLE_ENDIAN }");
#define TIFF_CAPS (gst_static_caps_get(&tiff_caps))
static GstStaticCaps tiff_be_caps = GST_STATIC_CAPS ("image/tiff, "
    "endianness = (int) BIG_ENDIAN");
#define TIFF_BE_CAPS (gst_static_caps_get(&tiff_be_caps))
static GstStaticCaps tiff_le_caps = GST_STATIC_CAPS ("image/tiff, "
    "endianness = (int) LITTLE_ENDIAN");
#define TIFF_LE_CAPS (gst_static_caps_get(&tiff_le_caps))
static void
tiff_type_find (GstTypeFind * tf, gpointer ununsed)
{
  guint8 *data = gst_type_find_peek (tf, 0, 8);
  guint8 le_header[4] = { 0x49, 0x49, 0x2A, 0x00 };
  guint8 be_header[4] = { 0x4D, 0x4D, 0x00, 0x2A };

  if (data) {
    if (memcmp (data, le_header, 4) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, TIFF_LE_CAPS);
    } else if (memcmp (data, be_header, 4) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, TIFF_BE_CAPS);
    }
  }
}

/*** PNM ***/

static GstStaticCaps pnm_caps = GST_STATIC_CAPS ("image/x-portable-bitmap; "
    "image/x-portable-graymap; image/x-portable-pixmap; "
    "image/x-portable-anymap");

#define PNM_CAPS (gst_static_caps_get(&pnm_caps))

#define IS_PNM_WHITESPACE(c) \
    ((c) == ' ' || (c) == '\r' || (c) == '\n' || (c) == 't')

static void
pnm_type_find (GstTypeFind * tf, gpointer ununsed)
{
  const gchar *media_type = NULL;
  DataScanCtx c = { 0, NULL, 0 };
  guint h = 0, w = 0;

  if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 16)))
    return;

  /* see http://en.wikipedia.org/wiki/Netpbm_format */
  if (c.data[0] != 'P' || c.data[1] < '1' || c.data[1] > '7' ||
      !IS_PNM_WHITESPACE (c.data[2]) ||
      (c.data[3] != '#' && c.data[3] < '0' && c.data[3] > '9'))
    return;

  switch (c.data[1]) {
    case '1':
      media_type = "image/x-portable-bitmap";   /* ASCII */
      break;
    case '2':
      media_type = "image/x-portable-graymap";  /* ASCII */
      break;
    case '3':
      media_type = "image/x-portable-pixmap";   /* ASCII */
      break;
    case '4':
      media_type = "image/x-portable-bitmap";   /* Raw */
      break;
    case '5':
      media_type = "image/x-portable-graymap";  /* Raw */
      break;
    case '6':
      media_type = "image/x-portable-pixmap";   /* Raw */
      break;
    case '7':
      media_type = "image/x-portable-anymap";
      break;
    default:
      g_return_if_reached ();
  }

  /* try to extract width and height as well */
  if (c.data[1] != '7') {
    gchar s[64] = { 0, }
    , sep1, sep2;

    /* need to skip any comment lines first */
    data_scan_ctx_advance (tf, &c, 3);
    while (c.data[0] == '#') {  /* we know there's still data left */
      data_scan_ctx_advance (tf, &c, 1);
      while (c.data[0] != '\n' && c.data[0] != '\r') {
        if (!data_scan_ctx_ensure_data (tf, &c, 4))
          return;
        data_scan_ctx_advance (tf, &c, 1);
      }
      data_scan_ctx_advance (tf, &c, 1);
      GST_LOG ("skipped comment line in PNM header");
    }

    if (!data_scan_ctx_ensure_data (tf, &c, 32) &&
        !data_scan_ctx_ensure_data (tf, &c, 4)) {
      return;
    }

    /* need to NUL-terminate data for sscanf */
    memcpy (s, c.data, MIN (sizeof (s) - 1, c.size));
    if (sscanf (s, "%u%c%u%c", &w, &sep1, &h, &sep2) == 4 &&
        IS_PNM_WHITESPACE (sep1) && IS_PNM_WHITESPACE (sep2) &&
        w > 0 && w < G_MAXINT && h > 0 && h < G_MAXINT) {
      GST_LOG ("extracted PNM width and height: %dx%d", w, h);
    } else {
      w = 0;
      h = 0;
    }
  } else {
    /* FIXME: extract width + height for anymaps too */
  }

  if (w > 0 && h > 0) {
    gst_type_find_suggest_simple (tf, GST_TYPE_FIND_MAXIMUM, media_type,
        "width", G_TYPE_INT, w, "height", G_TYPE_INT, h, NULL);
  } else {
    gst_type_find_suggest_simple (tf, GST_TYPE_FIND_LIKELY, media_type, NULL);
  }
}

static GstStaticCaps sds_caps = GST_STATIC_CAPS ("audio/x-sds");

#define SDS_CAPS (gst_static_caps_get(&sds_caps))
static void
sds_type_find (GstTypeFind * tf, gpointer ununsed)
{
  guint8 *data = gst_type_find_peek (tf, 0, 4);
  guint8 mask[4] = { 0xFF, 0xFF, 0x80, 0xFF };
  guint8 match[4] = { 0xF0, 0x7E, 0, 0x01 };
  gint x;

  if (data) {
    for (x = 0; x < 4; x++) {
      if ((data[x] & mask[x]) != match[x]) {
        return;
      }
    }
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, SDS_CAPS);
  }
}

static GstStaticCaps ircam_caps = GST_STATIC_CAPS ("audio/x-ircam");

#define IRCAM_CAPS (gst_static_caps_get(&ircam_caps))
static void
ircam_type_find (GstTypeFind * tf, gpointer ununsed)
{
  guint8 *data = gst_type_find_peek (tf, 0, 4);
  guint8 mask[4] = { 0xFF, 0xFF, 0xF8, 0xFF };
  guint8 match[4] = { 0x64, 0xA3, 0x00, 0x00 };
  gint x;
  gboolean matched = TRUE;

  if (!data) {
    return;
  }
  for (x = 0; x < 4; x++) {
    if ((data[x] & mask[x]) != match[x]) {
      matched = FALSE;
    }
  }
  if (matched) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, IRCAM_CAPS);
    return;
  }
  /* now try the reverse version */
  matched = TRUE;
  for (x = 0; x < 4; x++) {
    if ((data[x] & mask[3 - x]) != match[3 - x]) {
      matched = FALSE;
    }
  }
}

/* EBML typefind helper */
static gboolean
ebml_check_header (GstTypeFind * tf, const gchar * doctype, int doctype_len)
{
  /* 4 bytes for EBML ID, 1 byte for header length identifier */
  guint8 *data = gst_type_find_peek (tf, 0, 4 + 1);
  gint len_mask = 0x80, size = 1, n = 1, total;

  if (!data)
    return FALSE;

  /* ebml header? */
  if (data[0] != 0x1A || data[1] != 0x45 || data[2] != 0xDF || data[3] != 0xA3)
    return FALSE;

  /* length of header */
  total = data[4];
  while (size <= 8 && !(total & len_mask)) {
    size++;
    len_mask >>= 1;
  }
  if (size > 8)
    return FALSE;
  total &= (len_mask - 1);
  while (n < size)
    total = (total << 8) | data[4 + n++];

  /* get new data for full header, 4 bytes for EBML ID,
   * EBML length tag and the actual header */
  data = gst_type_find_peek (tf, 0, 4 + size + total);
  if (!data)
    return FALSE;

  /* only check doctype if asked to do so */
  if (doctype == NULL || doctype_len == 0)
    return TRUE;

  /* the header must contain the doctype. For now, we don't parse the
   * whole header but simply check for the availability of that array
   * of characters inside the header. Not fully fool-proof, but good
   * enough. */
  for (n = 4 + size; n <= 4 + size + total - doctype_len; n++)
    if (!memcmp (&data[n], doctype, doctype_len))
      return TRUE;

  return FALSE;
}

/*** video/x-matroska ***/
static GstStaticCaps matroska_caps = GST_STATIC_CAPS ("video/x-matroska");

#define MATROSKA_CAPS (gst_static_caps_get(&matroska_caps))
static void
matroska_type_find (GstTypeFind * tf, gpointer ununsed)
{
  if (ebml_check_header (tf, "matroska", 8))
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MATROSKA_CAPS);
  else if (ebml_check_header (tf, NULL, 0))
    gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY, MATROSKA_CAPS);
}

/*** video/webm ***/
static GstStaticCaps webm_caps = GST_STATIC_CAPS ("video/webm");

#define WEBM_CAPS (gst_static_caps_get(&webm_caps))
static void
webm_type_find (GstTypeFind * tf, gpointer ununsed)
{
  if (ebml_check_header (tf, "webm", 4))
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, WEBM_CAPS);
}

/*** application/mxf ***/
static GstStaticCaps mxf_caps = GST_STATIC_CAPS ("application/mxf");

#define MXF_MAX_PROBE_LENGTH (1024 * 64)
#define MXF_CAPS (gst_static_caps_get(&mxf_caps))

/*
 * MXF files start with a header partition pack key of 16 bytes which is defined
 * at SMPTE-377M 6.1. Before this there can be up to 64K of run-in which _must_
 * not contain the partition pack key.
 */
static void
mxf_type_find (GstTypeFind * tf, gpointer ununsed)
{
  static const guint8 partition_pack_key[] =
      { 0x06, 0x0e, 0x2b, 0x34, 0x02, 0x05, 0x01, 0x01, 0x0d, 0x01, 0x02, 0x01,
    0x01
  };
  DataScanCtx c = { 0, NULL, 0 };

  while (c.offset <= MXF_MAX_PROBE_LENGTH) {
    guint i;
    if (G_UNLIKELY (!data_scan_ctx_ensure_data (tf, &c, 1024)))
      break;

    /* look over in chunks of 1kbytes to avoid too much overhead */

    for (i = 0; i < 1024 - 16; i++) {
      /* Check first byte before calling more expensive memcmp function */
      if (G_UNLIKELY (c.data[i] == 0x06
              && memcmp (c.data + i, partition_pack_key, 13) == 0)) {
        /* Header partition pack? */
        if (c.data[i + 13] != 0x02)
          goto advance;

        /* Partition status */
        if (c.data[i + 14] >= 0x05)
          goto advance;

        /* Reserved, must be 0x00 */
        if (c.data[i + 15] != 0x00)
          goto advance;

        gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, MXF_CAPS);
        return;
      }
    }

  advance:
    data_scan_ctx_advance (tf, &c, 1024 - 16);
  }
}

/*** video/x-dv ***/

static GstStaticCaps dv_caps = GST_STATIC_CAPS ("video/x-dv, "
    "systemstream = (boolean) true");
#define DV_CAPS (gst_static_caps_get(&dv_caps))
static void
dv_type_find (GstTypeFind * tf, gpointer private)
{
  guint8 *data;

  data = gst_type_find_peek (tf, 0, 5);

  /* check for DIF  and DV flag */
  if (data && (data[0] == 0x1f) && (data[1] == 0x07) && (data[2] == 0x00)) {
    const gchar *format;

    if (data[3] & 0x80) {
      format = "PAL";
    } else {
      format = "NTSC";
    }

    gst_type_find_suggest_simple (tf, GST_TYPE_FIND_MAXIMUM, "video/x-dv",
        "systemstream", G_TYPE_BOOLEAN, TRUE,
        "format", G_TYPE_STRING, format, NULL);
  }
}


/*** application/ogg and application/x-annodex ***/
static GstStaticCaps ogg_caps = GST_STATIC_CAPS ("application/ogg");
static GstStaticCaps annodex_caps = GST_STATIC_CAPS ("application/x-annodex");
static GstStaticCaps ogg_annodex_caps =
    GST_STATIC_CAPS ("application/ogg;application/x-annodex");

#define OGGANX_CAPS (gst_static_caps_get(&ogg_annodex_caps))

static void
ogganx_type_find (GstTypeFind * tf, gpointer private)
{
  guint8 *data = gst_type_find_peek (tf, 0, 4);

  if ((data != NULL) && (memcmp (data, "OggS", 4) == 0)) {

    /* Check for an annodex fishbone header */
    data = gst_type_find_peek (tf, 28, 8);
    if (data && memcmp (data, "fishead\0", 8) == 0)
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM,
          gst_static_caps_get (&annodex_caps));

    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM,
        gst_static_caps_get (&ogg_caps));
  }
}

/*** audio/x-vorbis ***/
static GstStaticCaps vorbis_caps = GST_STATIC_CAPS ("audio/x-vorbis");

#define VORBIS_CAPS (gst_static_caps_get(&vorbis_caps))
static void
vorbis_type_find (GstTypeFind * tf, gpointer private)
{
  guint8 *data = gst_type_find_peek (tf, 0, 30);

  if (data) {
    guint blocksize_0;
    guint blocksize_1;

    /* 1 byte packet type (identification=0x01)
       6 byte string "vorbis"
       4 byte vorbis version */
    if (memcmp (data, "\001vorbis\000\000\000\000", 11) != 0)
      return;
    data += 11;
    /* 1 byte channels must be != 0 */
    if (data[0] == 0)
      return;
    data++;
    /* 4 byte samplerate must be != 0 */
    if (GST_READ_UINT32_LE (data) == 0)
      return;
    data += 16;
    /* blocksize checks */
    blocksize_0 = data[0] & 0x0F;
    blocksize_1 = (data[0] & 0xF0) >> 4;
    if (blocksize_0 > blocksize_1)
      return;
    if (blocksize_0 < 6 || blocksize_0 > 13)
      return;
    if (blocksize_1 < 6 || blocksize_1 > 13)
      return;
    data++;
    /* framing bit */
    if ((data[0] & 0x01) != 1)
      return;
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, VORBIS_CAPS);
  }
}

/*** video/x-theora ***/

static GstStaticCaps theora_caps = GST_STATIC_CAPS ("video/x-theora");

#define THEORA_CAPS (gst_static_caps_get(&theora_caps))
static void
theora_type_find (GstTypeFind * tf, gpointer private)
{
  guint8 *data = gst_type_find_peek (tf, 0, 7); //42);

  if (data) {
    if (data[0] != 0x80)
      return;
    if (memcmp (&data[1], "theora", 6) != 0)
      return;
    /* FIXME: make this more reliable when specs are out */

    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, THEORA_CAPS);
  }
}

/*** kate ***/
static void
kate_type_find (GstTypeFind * tf, gpointer private)
{
  guint8 *data = gst_type_find_peek (tf, 0, 64);
  gchar category[16] = { 0, };

  if (G_UNLIKELY (data == NULL))
    return;

  /* see: http://wiki.xiph.org/index.php/OggKate#Format_specification */
  if (G_LIKELY (memcmp (data, "\200kate\0\0\0", 8) != 0))
    return;

  /* make sure we always have a NUL-terminated string */
  memcpy (category, data + 48, 15);
  GST_LOG ("kate category: %s", category);
  /* canonical categories for subtitles: subtitles, spu-subtitles, SUB, K-SPU */
  if (strcmp (category, "subtitles") == 0 || strcmp (category, "SUB") == 0 ||
      strcmp (category, "spu-subtitles") == 0 ||
      strcmp (category, "K-SPU") == 0) {
    gst_type_find_suggest_simple (tf, GST_TYPE_FIND_MAXIMUM,
        "subtitle/x-kate", NULL);
  } else {
    gst_type_find_suggest_simple (tf, GST_TYPE_FIND_MAXIMUM,
        "application/x-kate", NULL);
  }
}

/*** application/x-ogm-video or audio***/

static GstStaticCaps ogmvideo_caps =
GST_STATIC_CAPS ("application/x-ogm-video");
#define OGMVIDEO_CAPS (gst_static_caps_get(&ogmvideo_caps))
static void
ogmvideo_type_find (GstTypeFind * tf, gpointer private)
{
  guint8 *data = gst_type_find_peek (tf, 0, 9);

  if (data) {
    if (memcmp (data, "\001video\000\000\000", 9) != 0)
      return;
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, OGMVIDEO_CAPS);
  }
}

static GstStaticCaps ogmaudio_caps =
GST_STATIC_CAPS ("application/x-ogm-audio");
#define OGMAUDIO_CAPS (gst_static_caps_get(&ogmaudio_caps))
static void
ogmaudio_type_find (GstTypeFind * tf, gpointer private)
{
  guint8 *data = gst_type_find_peek (tf, 0, 9);

  if (data) {
    if (memcmp (data, "\001audio\000\000\000", 9) != 0)
      return;
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, OGMAUDIO_CAPS);
  }
}

static GstStaticCaps ogmtext_caps = GST_STATIC_CAPS ("application/x-ogm-text");

#define OGMTEXT_CAPS (gst_static_caps_get(&ogmtext_caps))
static void
ogmtext_type_find (GstTypeFind * tf, gpointer private)
{
  guint8 *data = gst_type_find_peek (tf, 0, 9);

  if (data) {
    if (memcmp (data, "\001text\000\000\000\000", 9) != 0)
      return;
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, OGMTEXT_CAPS);
  }
}

/*** audio/x-speex ***/

static GstStaticCaps speex_caps = GST_STATIC_CAPS ("audio/x-speex");

#define SPEEX_CAPS (gst_static_caps_get(&speex_caps))
static void
speex_type_find (GstTypeFind * tf, gpointer private)
{
  guint8 *data = gst_type_find_peek (tf, 0, 80);

  if (data) {
    /* 8 byte string "Speex   "
       24 byte speex version string + int */
    if (memcmp (data, "Speex   ", 8) != 0)
      return;
    data += 32;

    /* 4 byte header size >= 80 */
    if (GST_READ_UINT32_LE (data) < 80)
      return;
    data += 4;

    /* 4 byte sample rate <= 48000 */
    if (GST_READ_UINT32_LE (data) > 48000)
      return;
    data += 4;

    /* currently there are only 3 speex modes. */
    if (GST_READ_UINT32_LE (data) > 3)
      return;
    data += 12;

    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, SPEEX_CAPS);
  }
}

/*** audio/x-celt ***/

static GstStaticCaps celt_caps = GST_STATIC_CAPS ("audio/x-celt");

#define CELT_CAPS (gst_static_caps_get(&celt_caps))
static void
celt_type_find (GstTypeFind * tf, gpointer private)
{
  guint8 *data = gst_type_find_peek (tf, 0, 8);

  if (data) {
    /* 8 byte string "CELT   " */
    if (memcmp (data, "CELT    ", 8) != 0)
      return;

    /* TODO: Check other values of the CELT header */
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, CELT_CAPS);
  }
}

/*** application/x-ogg-skeleton ***/
static GstStaticCaps ogg_skeleton_caps =
GST_STATIC_CAPS ("application/x-ogg-skeleton, parsed=(boolean)FALSE");
#define OGG_SKELETON_CAPS (gst_static_caps_get(&ogg_skeleton_caps))
static void
oggskel_type_find (GstTypeFind * tf, gpointer private)
{
  guint8 *data = gst_type_find_peek (tf, 0, 12);

  if (data) {
    /* 8 byte string "fishead\0" for the ogg skeleton stream */
    if (memcmp (data, "fishead\0", 8) != 0)
      return;
    data += 8;

    /* Require that the header contains version 3.0 */
    if (GST_READ_UINT16_LE (data) != 3)
      return;
    data += 2;
    if (GST_READ_UINT16_LE (data) != 0)
      return;

    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, OGG_SKELETON_CAPS);
  }
}

static GstStaticCaps cmml_caps = GST_STATIC_CAPS ("text/x-cmml");

#define CMML_CAPS (gst_static_caps_get(&cmml_caps))
static void
cmml_type_find (GstTypeFind * tf, gpointer private)
{
  /* Header is 12 bytes minimum (though we don't check the minor version */
  guint8 *data = gst_type_find_peek (tf, 0, 12);

  if (data) {

    /* 8 byte string "CMML\0\0\0\0" for the magic number */
    if (memcmp (data, "CMML\0\0\0\0", 8) != 0)
      return;
    data += 8;

    /* Require that the header contains at least version 2.0 */
    if (GST_READ_UINT16_LE (data) < 2)
      return;

    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, CMML_CAPS);
  }
}

/*** application/x-tar ***/

static GstStaticCaps tar_caps = GST_STATIC_CAPS ("application/x-tar");

#define TAR_CAPS (gst_static_caps_get(&tar_caps))
#define OLDGNU_MAGIC "ustar  "  /* 7 chars and a NUL */
#define NEWGNU_MAGIC "ustar"    /* 5 chars and a NUL */
static void
tar_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 257, 8);

  /* of course we are not certain, but we don't want other typefind funcs
   * to detect formats of files within the tar archive, e.g. mp3s */
  if (data) {
    if (memcmp (data, OLDGNU_MAGIC, 8) == 0) {  /* sic */
      gst_type_find_suggest (tf, GST_TYPE_FIND_NEARLY_CERTAIN, TAR_CAPS);
    } else if (memcmp (data, NEWGNU_MAGIC, 6) == 0 &&   /* sic */
        g_ascii_isdigit (data[6]) && g_ascii_isdigit (data[7])) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_NEARLY_CERTAIN, TAR_CAPS);
    }
  }
}

/*** application/x-ar ***/

static GstStaticCaps ar_caps = GST_STATIC_CAPS ("application/x-ar");

#define AR_CAPS (gst_static_caps_get(&ar_caps))
static void
ar_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 24);

  if (data && memcmp (data, "!<arch>", 7) == 0) {
    gint i;

    for (i = 7; i < 24; ++i) {
      if (!g_ascii_isprint (data[i]) && data[i] != '\n') {
        gst_type_find_suggest (tf, GST_TYPE_FIND_POSSIBLE, AR_CAPS);
      }
    }

    gst_type_find_suggest (tf, GST_TYPE_FIND_NEARLY_CERTAIN, AR_CAPS);
  }
}

/*** audio/x-au ***/

/* NOTE: we cannot replace this function with TYPE_FIND_REGISTER_START_WITH,
 * as it is only possible to register one typefind factory per 'name'
 * (which is in this case the caps), and the first one would be replaced by
 * the second one. */
static GstStaticCaps au_caps = GST_STATIC_CAPS ("audio/x-au");

#define AU_CAPS (gst_static_caps_get(&au_caps))
static void
au_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 4);

  if (data) {
    if (memcmp (data, ".snd", 4) == 0 || memcmp (data, "dns.", 4) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, AU_CAPS);
    }
  }
}


/*** video/x-nuv ***/

/* NOTE: we cannot replace this function with TYPE_FIND_REGISTER_START_WITH,
 * as it is only possible to register one typefind factory per 'name'
 * (which is in this case the caps), and the first one would be replaced by
 * the second one. */
static GstStaticCaps nuv_caps = GST_STATIC_CAPS ("video/x-nuv");

#define NUV_CAPS (gst_static_caps_get(&nuv_caps))
static void
nuv_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 11);

  if (data) {
    if (memcmp (data, "MythTVVideo", 11) == 0
        || memcmp (data, "NuppelVideo", 11) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, NUV_CAPS);
    }
  }
}

/*** audio/x-paris ***/
/* NOTE: do not replace this function with two TYPE_FIND_REGISTER_START_WITH */
static GstStaticCaps paris_caps = GST_STATIC_CAPS ("audio/x-paris");

#define PARIS_CAPS (gst_static_caps_get(&paris_caps))
static void
paris_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 4);

  if (data) {
    if (memcmp (data, " paf", 4) == 0 || memcmp (data, "fap ", 4) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, PARIS_CAPS);
    }
  }
}

/*** audio/iLBC-sh ***/
/* NOTE: do not replace this function with two TYPE_FIND_REGISTER_START_WITH */
static GstStaticCaps ilbc_caps = GST_STATIC_CAPS ("audio/iLBC-sh");

#define ILBC_CAPS (gst_static_caps_get(&ilbc_caps))
static void
ilbc_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 8);

  if (data) {
    if (memcmp (data, "#!iLBC30", 8) == 0 || memcmp (data, "#!iLBC20", 8) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY, ILBC_CAPS);
    }
  }
}

/*** application/x-ms-dos-executable ***/

static GstStaticCaps msdos_caps =
GST_STATIC_CAPS ("application/x-ms-dos-executable");
#define MSDOS_CAPS (gst_static_caps_get(&msdos_caps))
/* see http://www.madchat.org/vxdevl/papers/winsys/pefile/pefile.htm */
static void
msdos_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 64);

  if (data && data[0] == 'M' && data[1] == 'Z' &&
      GST_READ_UINT16_LE (data + 8) == 4) {
    guint32 pe_offset = GST_READ_UINT32_LE (data + 60);

    data = gst_type_find_peek (tf, pe_offset, 2);
    if (data && data[0] == 'P' && data[1] == 'E') {
      gst_type_find_suggest (tf, GST_TYPE_FIND_NEARLY_CERTAIN, MSDOS_CAPS);
    }
  }
}

/*** application/x-mmsh ***/

static GstStaticCaps mmsh_caps = GST_STATIC_CAPS ("application/x-mmsh");

#define MMSH_CAPS gst_static_caps_get(&mmsh_caps)

/* This is to recognise mssh-over-http */
static void
mmsh_type_find (GstTypeFind * tf, gpointer unused)
{
  static const guint8 asf_marker[16] = { 0x30, 0x26, 0xb2, 0x75, 0x8e, 0x66,
    0xcf, 0x11, 0xa6, 0xd9, 0x00, 0xaa, 0x00, 0x62, 0xce, 0x6c
  };

  guint8 *data;

  data = gst_type_find_peek (tf, 0, 2 + 2 + 4 + 2 + 2 + 16);
  if (data && data[0] == 0x24 && data[1] == 0x48 &&
      GST_READ_UINT16_LE (data + 2) > 2 + 2 + 4 + 2 + 2 + 16 &&
      memcmp (data + 2 + 2 + 4 + 2 + 2, asf_marker, 16) == 0) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_LIKELY, MMSH_CAPS);
  }
}

/*** video/x-dirac ***/

/* NOTE: we cannot replace this function with TYPE_FIND_REGISTER_START_WITH,
 * as it is only possible to register one typefind factory per 'name'
 * (which is in this case the caps), and the first one would be replaced by
 * the second one. */
static GstStaticCaps dirac_caps = GST_STATIC_CAPS ("video/x-dirac");

#define DIRAC_CAPS (gst_static_caps_get(&dirac_caps))
static void
dirac_type_find (GstTypeFind * tf, gpointer unused)
{
  guint8 *data = gst_type_find_peek (tf, 0, 8);

  if (data) {
    if (memcmp (data, "BBCD", 4) == 0 || memcmp (data, "KW-DIRAC", 8) == 0) {
      gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, DIRAC_CAPS);
    }
  }
}

/*** video/vivo ***/

static GstStaticCaps vivo_caps = GST_STATIC_CAPS ("video/vivo");

#define VIVO_CAPS gst_static_caps_get(&vivo_caps)

static void
vivo_type_find (GstTypeFind * tf, gpointer unused)
{
  static const guint8 vivo_marker[] = { 'V', 'e', 'r', 's', 'i', 'o', 'n',
    ':', 'V', 'i', 'v', 'o', '/'
  };
  guint8 *data;
  guint hdr_len, pos;

  data = gst_type_find_peek (tf, 0, 1024);
  if (data == NULL || data[0] != 0x00)
    return;

  if ((data[1] & 0x80)) {
    if ((data[2] & 0x80))
      return;
    hdr_len = ((guint) (data[1] & 0x7f)) << 7;
    hdr_len += data[2];
    if (hdr_len > 2048)
      return;
    pos = 3;
  } else {
    hdr_len = data[1];
    pos = 2;
  }

  /* 1008 = 1022 - strlen ("Version:Vivo/") - 1 */
  while (pos < 1008 && data[pos] == '\r' && data[pos + 1] == '\n')
    pos += 2;

  if (memcmp (data + pos, vivo_marker, sizeof (vivo_marker)) == 0) {
    gst_type_find_suggest (tf, GST_TYPE_FIND_MAXIMUM, VIVO_CAPS);
  }
}

/*** XDG MIME typefinder (to avoid false positives mostly) ***/

#ifdef USE_GIO
static void
xdgmime_typefind (GstTypeFind * find, gpointer user_data)
{
  gchar *mimetype;
  gsize length = 16384;
  guint64 tf_length;
  guint8 *data;
  gchar *tmp;

  if ((tf_length = gst_type_find_get_length (find)) > 0)
    length = MIN (length, tf_length);

  if ((data = gst_type_find_peek (find, 0, length)) == NULL)
    return;

  tmp = g_content_type_guess (NULL, data, length, NULL);
  if (tmp == NULL || g_content_type_is_unknown (tmp)) {
    g_free (tmp);
    return;
  }

  mimetype = g_content_type_get_mime_type (tmp);
  g_free (tmp);

  if (mimetype == NULL)
    return;

  GST_DEBUG ("Got mimetype '%s'", mimetype);

  /* Ignore audio/video types:
   *  - our own typefinders in -base are likely to be better at this
   *    (and if they're not, we really want to fix them, that's why we don't
   *    report xdg-detected audio/video types at all, not even with a low
   *    probability)
   *  - we want to detect GStreamer media types and not MIME types
   *  - the purpose of this xdg mime finder is mainly to prevent false
   *    positives of non-media formats, not to typefind audio/video formats */
  if (g_str_has_prefix (mimetype, "audio/") ||
      g_str_has_prefix (mimetype, "video/")) {
    GST_LOG ("Ignoring audio/video mime type");
    g_free (mimetype);
    return;
  }

  /* Again, we mainly want the xdg typefinding to prevent false-positives on
   * non-media formats, so suggest the type with a probability that trumps
   * uncertain results of our typefinders, but not more than that. */
  GST_LOG ("Suggesting '%s' with probability POSSIBLE", mimetype);
  gst_type_find_suggest_simple (find, GST_TYPE_FIND_POSSIBLE, mimetype, NULL);
  g_free (mimetype);
}
#endif /* USE_GIO */

/*** Windows icon typefinder (to avoid false positives mostly) ***/

static void
windows_icon_typefind (GstTypeFind * find, gpointer user_data)
{
  guint8 *data;
  gint64 datalen;
  guint16 type, nimages;
  gint32 size, offset;

  datalen = gst_type_find_get_length (find);
  if ((data = gst_type_find_peek (find, 0, 6)) == NULL)
    return;

  /* header - simple and not enough to rely on it alone */
  if (GST_READ_UINT16_LE (data) != 0)
    return;
  type = GST_READ_UINT16_LE (data + 2);
  if (type != 1 && type != 2)
    return;
  nimages = GST_READ_UINT16_LE (data + 4);
  if (nimages == 0)             /* we can assume we can't have an empty image file ? */
    return;

  /* first image */
  if (data[6 + 3] != 0)
    return;
  if (type == 1) {
    guint16 planes = GST_READ_UINT16_LE (data + 6 + 4);
    if (planes > 1)
      return;
  }
  size = GST_READ_UINT32_LE (data + 6 + 8);
  offset = GST_READ_UINT32_LE (data + 6 + 12);
  if (offset < 0 || size <= 0 || size >= datalen || offset >= datalen
      || size + offset > datalen)
    return;

  gst_type_find_suggest_simple (find, GST_TYPE_FIND_NEARLY_CERTAIN,
      "image/x-icon", NULL);
}


/*** DEGAS Atari images (also to avoid false positives, see #625129) ***/
static void
degas_type_find (GstTypeFind * tf, gpointer private)
{
  /* No magic, but it should have a fixed size and a few invalid values */
  /* http://www.fileformat.info/format/atari/spec/6ecf9f6eb5be494284a47feb8a214687/view.htm */
  gint64 len;
  const guint8 *data;
  guint16 resolution;
  int n;

  len = gst_type_find_get_length (tf);
  if (len < 34)                 /* smallest header of the lot */
    return;
  data = gst_type_find_peek (tf, 0, 4);
  resolution = GST_READ_UINT16_BE (data);
  if (len == 32034) {
    /* could be DEGAS */
    if (resolution <= 2)
      gst_type_find_suggest_simple (tf, GST_TYPE_FIND_POSSIBLE + 5,
          "image/x-degas", NULL);
  } else if (len == 32066) {
    /* could be DEGAS Elite */
    if (resolution <= 2) {
      data = gst_type_find_peek (tf, len - 16, 8);
      for (n = 0; n < 4; n++) {
        if (GST_READ_UINT16_BE (data + n * 2) > 2)
          return;
      }
      gst_type_find_suggest_simple (tf, GST_TYPE_FIND_POSSIBLE + 5,
          "image/x-degas", NULL);
    }
  } else if (len >= 66 && len < 32066) {
    /* could be compressed DEGAS Elite, but it's compressed and so we can't rely on size,
       it does have 4 16 bytes values near the end that are 0-2 though. */
    if ((resolution & 0x8000) && (resolution & 0x7fff) <= 2) {
      data = gst_type_find_peek (tf, len - 16, 8);
      for (n = 0; n < 4; n++) {
        if (GST_READ_UINT16_BE (data + n * 2) > 2)
          return;
      }
      gst_type_find_suggest_simple (tf, GST_TYPE_FIND_POSSIBLE + 5,
          "image/x-degas", NULL);
    }
  }
}

/*** generic typefind for streams that have some data at a specific position***/
typedef struct
{
  const guint8 *data;
  guint size;
  guint probability;
  GstCaps *caps;
}
GstTypeFindData;

static void
start_with_type_find (GstTypeFind * tf, gpointer private)
{
  GstTypeFindData *start_with = (GstTypeFindData *) private;
  guint8 *data;

  GST_LOG ("trying to find mime type %s with the first %u bytes of data",
      gst_structure_get_name (gst_caps_get_structure (start_with->caps, 0)),
      start_with->size);
  data = gst_type_find_peek (tf, 0, start_with->size);
  if (data && memcmp (data, start_with->data, start_with->size) == 0) {
    gst_type_find_suggest (tf, start_with->probability, start_with->caps);
  }
}

static void
sw_data_destroy (GstTypeFindData * sw_data)
{
  if (G_LIKELY (sw_data->caps != NULL))
    gst_caps_unref (sw_data->caps);
  g_free (sw_data);
}

#define TYPE_FIND_REGISTER_START_WITH(plugin,name,rank,ext,_data,_size,_probability)\
G_BEGIN_DECLS{                                                          \
  GstTypeFindData *sw_data = g_new (GstTypeFindData, 1);                \
  sw_data->data = (const guint8 *)_data;                                \
  sw_data->size = _size;                                                \
  sw_data->probability = _probability;                                  \
  sw_data->caps = gst_caps_new_simple (name, NULL);                     \
  if (!gst_type_find_register (plugin, name, rank, start_with_type_find,\
                      (char **) ext, sw_data->caps, sw_data,            \
                     (GDestroyNotify) (sw_data_destroy))) {             \
    gst_caps_unref (sw_data->caps);                                     \
    g_free (sw_data);                                                   \
  }                                                                     \
}G_END_DECLS

/*** same for riff types ***/

static void
riff_type_find (GstTypeFind * tf, gpointer private)
{
  GstTypeFindData *riff_data = (GstTypeFindData *) private;
  guint8 *data = gst_type_find_peek (tf, 0, 12);

  if (data && (memcmp (data, "RIFF", 4) == 0 || memcmp (data, "AVF0", 4) == 0)) {
    data += 8;
    if (memcmp (data, riff_data->data, 4) == 0)
      gst_type_find_suggest (tf, riff_data->probability, riff_data->caps);
  }
}

#define TYPE_FIND_REGISTER_RIFF(plugin,name,rank,ext,_data)             \
G_BEGIN_DECLS{                                                          \
  GstTypeFindData *sw_data = g_new (GstTypeFindData, 1);                \
  sw_data->data = (gpointer)_data;                                      \
  sw_data->size = 4;                                                    \
  sw_data->probability = GST_TYPE_FIND_MAXIMUM;                         \
  sw_data->caps = gst_caps_new_simple (name, NULL);                     \
  if (!gst_type_find_register (plugin, name, rank, riff_type_find,      \
                      (char **) ext, sw_data->caps, sw_data,            \
                      (GDestroyNotify) (sw_data_destroy))) {            \
    gst_caps_unref (sw_data->caps);                                     \
    g_free (sw_data);                                                   \
  }                                                                     \
}G_END_DECLS


/*** plugin initialization ***/

#define TYPE_FIND_REGISTER(plugin,name,rank,func,ext,caps,priv,notify) \
G_BEGIN_DECLS{\
  if (!gst_type_find_register (plugin, name, rank, func, (char **) ext, caps, priv, notify))\
    return FALSE; \
}G_END_DECLS


#ifdef GSTREAMER_LITE
gboolean
plugin_init_typefind (GstPlugin * plugin)
#else // GSTREAMER_LITE
static gboolean
plugin_init (GstPlugin * plugin)
#endif // GSTREAMER_LITE
{
  /* can't initialize this via a struct as caps can't be statically initialized */

  /* note: asx/wax/wmx are XML files, asf doesn't handle them */
  /* FIXME-0.11: these should be const,
     this requires gstreamer/gst/gsttypefind::gst_type_find_register()
     to have define the parameter as const
   */
#ifndef GSTREAMER_LITE
  static const gchar *asf_exts[] = { "asf", "wm", "wma", "wmv", NULL };
  static const gchar *au_exts[] = { "au", "snd", NULL };
  static const gchar *avi_exts[] = { "avi", NULL };
  static const gchar *qcp_exts[] = { "qcp", NULL };
  static const gchar *cdxa_exts[] = { "dat", NULL };
  static const gchar *flac_exts[] = { "flac", NULL };
  static const gchar *flx_exts[] = { "flc", "fli", NULL };
#endif // GSTREAMER_LITE
  static const gchar *id3_exts[] =
      { "mp3", "mp2", "mp1", "mpga", "ogg", "flac", "tta", NULL };
#ifndef GSTREAMER_LITE
  static const gchar *apetag_exts[] = { "mp3", "ape", "mpc", "wv", NULL };
  static const gchar *tta_exts[] = { "tta", NULL };
  static const gchar *mod_exts[] = { "669", "amf", "dsm", "gdm", "far", "imf",
    "it", "med", "mod", "mtm", "okt", "sam",
    "s3m", "stm", "stx", "ult", "xm", NULL
  };
#endif // GSTREAMER_LITE
  static const gchar *mp3_exts[] = { "mp3", "mp2", "mp1", "mpga", NULL };
#ifndef GSTREAMER_LITE
  static const gchar *ac3_exts[] = { "ac3", "eac3", NULL };
  static const gchar *dts_exts[] = { "dts", NULL };
  static const gchar *gsm_exts[] = { "gsm", NULL };
  static const gchar *musepack_exts[] = { "mpc", "mpp", "mp+", NULL };
  static const gchar *mpeg_sys_exts[] = { "mpe", "mpeg", "mpg", NULL };
  static const gchar *mpeg_video_exts[] = { "mpv", "mpeg", "mpg", NULL };
  static const gchar *mpeg_ts_exts[] = { "ts", "mts", NULL };
  static const gchar *ogg_exts[] = { "anx", "ogg", "ogm", NULL };
  static const gchar *qt_exts[] = { "mov", NULL };
  static const gchar *qtif_exts[] = { "qif", "qtif", "qti", NULL };
  static const gchar *mj2_exts[] = { "mj2", NULL };
  static const gchar *jp2_exts[] = { "jp2", NULL };
  static const gchar *rm_exts[] = { "ra", "ram", "rm", "rmvb", NULL };
  static const gchar *swf_exts[] = { "swf", "swfl", NULL };
  static const gchar *utf8_exts[] = { "txt", NULL };
#endif // GSTREAMER_LITE
  static const gchar *wav_exts[] = { "wav", NULL };
  static const gchar *aiff_exts[] = { "aiff", "aif", "aifc", NULL };
#ifndef GSTREAMER_LITE
  static const gchar *svx_exts[] = { "iff", "svx", NULL };
  static const gchar *paris_exts[] = { "paf", NULL };
  static const gchar *nist_exts[] = { "nist", NULL };
  static const gchar *voc_exts[] = { "voc", NULL };
  static const gchar *sds_exts[] = { "sds", NULL };
  static const gchar *ircam_exts[] = { "sf", NULL };
  static const gchar *w64_exts[] = { "w64", NULL };
  static const gchar *shn_exts[] = { "shn", NULL };
  static const gchar *ape_exts[] = { "ape", NULL };
  static const gchar *uri_exts[] = { "ram", NULL };
  static const gchar *hls_exts[] = { "m3u8", NULL };
  static const gchar *sdp_exts[] = { "sdp", NULL };
  static const gchar *smil_exts[] = { "smil", NULL };
  static const gchar *html_exts[] = { "htm", "html", NULL };
  static const gchar *xml_exts[] = { "xml", NULL };
  static const gchar *jpeg_exts[] = { "jpg", "jpe", "jpeg", NULL };
  static const gchar *gif_exts[] = { "gif", NULL };
  static const gchar *png_exts[] = { "png", NULL };
  static const gchar *bmp_exts[] = { "bmp", NULL };
  static const gchar *tiff_exts[] = { "tif", "tiff", NULL };
  static const gchar *matroska_exts[] = { "mkv", "mka", NULL };
  static const gchar *webm_exts[] = { "webm", NULL };
  static const gchar *mve_exts[] = { "mve", NULL };
  static const gchar *dv_exts[] = { "dv", "dif", NULL };
  static const gchar *amr_exts[] = { "amr", NULL };
  static const gchar *ilbc_exts[] = { "ilbc", NULL };
  static const gchar *sid_exts[] = { "sid", NULL };
  static const gchar *xcf_exts[] = { "xcf", NULL };
  static const gchar *mng_exts[] = { "mng", NULL };
  static const gchar *jng_exts[] = { "jng", NULL };
  static const gchar *xpm_exts[] = { "xpm", NULL };
  static const gchar *pnm_exts[] = { "pnm", "ppm", "pgm", "pbm", NULL };
  static const gchar *ras_exts[] = { "ras", NULL };
  static const gchar *bz2_exts[] = { "bz2", NULL };
  static const gchar *gz_exts[] = { "gz", NULL };
  static const gchar *zip_exts[] = { "zip", NULL };
  static const gchar *compress_exts[] = { "Z", NULL };
  static const gchar *m4a_exts[] = { "m4a", NULL };
  static const gchar *q3gp_exts[] = { "3gp", NULL };
#endif // GSTREAMER_LITE
  static const gchar *aac_exts[] = { "aac", "adts", "adif", "loas", NULL };
#ifndef GSTREAMER_LITE
  static const gchar *spc_exts[] = { "spc", NULL };
  static const gchar *wavpack_exts[] = { "wv", "wvp", NULL };
  static const gchar *wavpack_correction_exts[] = { "wvc", NULL };
  static const gchar *rar_exts[] = { "rar", NULL };
  static const gchar *tar_exts[] = { "tar", NULL };
  static const gchar *ar_exts[] = { "a", NULL };
  static const gchar *msdos_exts[] = { "dll", "exe", "ocx", "sys", "scr",
    "msstyles", "cpl", NULL
  };
#endif // GSTREAMER_LITE
  static const gchar *flv_exts[] = { "flv", NULL };
#ifndef GSTREAMER_LITE
  static const gchar *m4v_exts[] = { "m4v", NULL };
  static const gchar *h263_exts[] = { "h263", "263", NULL };
  static const gchar *h264_exts[] = { "h264", "x264", "264", NULL };
  static const gchar *nuv_exts[] = { "nuv", NULL };
  static const gchar *vivo_exts[] = { "viv", NULL };
  static const gchar *nsf_exts[] = { "nsf", NULL };
  static const gchar *gym_exts[] = { "gym", NULL };
  static const gchar *ay_exts[] = { "ay", NULL };
  static const gchar *gbs_exts[] = { "gbs", NULL };
  static const gchar *kss_exts[] = { "kss", NULL };
  static const gchar *sap_exts[] = { "sap", NULL };
  static const gchar *vgm_exts[] = { "vgm", NULL };
  static const gchar *mid_exts[] = { "mid", "midi", NULL };
  static const gchar *mxmf_exts[] = { "mxmf", NULL };
  static const gchar *imelody_exts[] = { "imy", "ime", "imelody", NULL };
  static const gchar *pdf_exts[] = { "pdf", NULL };
  static const gchar *ps_exts[] = { "ps", NULL };
  static const gchar *svg_exts[] = { "svg", NULL };
  static const gchar *mxf_exts[] = { "mxf", NULL };
  static const gchar *ivf_exts[] = { "ivf", NULL };
  static const gchar *msword_exts[] = { "doc", NULL };
  static const gchar *dsstore_exts[] = { "DS_Store", NULL };
  static const gchar *psd_exts[] = { "psd", NULL };
  static const gchar *y4m_exts[] = { "y4m", NULL };
#endif // GSTREAMER_LITE

  GST_DEBUG_CATEGORY_INIT (type_find_debug, "typefindfunctions",
      GST_DEBUG_FG_GREEN | GST_DEBUG_BG_RED, "generic type find functions");

  /* must use strings, macros don't accept initializers */
#ifndef GSTREAMER_LITE
  TYPE_FIND_REGISTER_START_WITH (plugin, "video/x-ms-asf", GST_RANK_SECONDARY,
      asf_exts,
      "\060\046\262\165\216\146\317\021\246\331\000\252\000\142\316\154", 16,
      GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER (plugin, "audio/x-musepack", GST_RANK_PRIMARY,
      musepack_type_find, musepack_exts, MUSEPACK_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "audio/x-au", GST_RANK_MARGINAL,
      au_type_find, au_exts, AU_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER_RIFF (plugin, "video/x-msvideo", GST_RANK_PRIMARY,
      avi_exts, "AVI ");
  TYPE_FIND_REGISTER_RIFF (plugin, "audio/qcelp", GST_RANK_PRIMARY,
      qcp_exts, "QLCM");
  TYPE_FIND_REGISTER_RIFF (plugin, "video/x-cdxa", GST_RANK_PRIMARY,
      cdxa_exts, "CDXA");
  TYPE_FIND_REGISTER_START_WITH (plugin, "video/x-vcd", GST_RANK_PRIMARY,
      cdxa_exts, "\000\377\377\377\377\377\377\377\377\377\377\000", 12,
      GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-imelody", GST_RANK_PRIMARY,
      imelody_exts, "BEGIN:IMELODY", 13, GST_TYPE_FIND_MAXIMUM);
#if 0
  TYPE_FIND_REGISTER_START_WITH (plugin, "video/x-smoke", GST_RANK_PRIMARY,
      NULL, "\x80smoke\x00\x01\x00", 6, GST_TYPE_FIND_MAXIMUM);
#endif
  TYPE_FIND_REGISTER (plugin, "audio/midi", GST_RANK_PRIMARY, mid_type_find,
      mid_exts, MID_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER_RIFF (plugin, "audio/riff-midi", GST_RANK_PRIMARY,
      mid_exts, "RMID");
  TYPE_FIND_REGISTER (plugin, "audio/mobile-xmf", GST_RANK_PRIMARY,
      mxmf_type_find, mxmf_exts, MXMF_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/x-fli", GST_RANK_MARGINAL, flx_type_find,
      flx_exts, FLX_CAPS, NULL, NULL);
#endif // GSTREAMER_LITE
  TYPE_FIND_REGISTER (plugin, "application/x-id3v2", GST_RANK_PRIMARY + 103,
      id3v2_type_find, id3_exts, ID3_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/x-id3v1", GST_RANK_PRIMARY + 101,
      id3v1_type_find, id3_exts, ID3_CAPS, NULL, NULL);
#ifndef GSTREAMER_LITE
  TYPE_FIND_REGISTER (plugin, "application/x-apetag", GST_RANK_PRIMARY + 102,
      apetag_type_find, apetag_exts, APETAG_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "audio/x-ttafile", GST_RANK_PRIMARY,
      tta_type_find, tta_exts, TTA_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "audio/x-mod", GST_RANK_SECONDARY, mod_type_find,
      mod_exts, MOD_CAPS, NULL, NULL);
#endif // GSTREAMER_LITE
  TYPE_FIND_REGISTER (plugin, "audio/mpeg", GST_RANK_PRIMARY, mp3_type_find,
      mp3_exts, MP3_CAPS, NULL, NULL);
#ifndef GSTREAMER_LITE
  TYPE_FIND_REGISTER (plugin, "audio/x-ac3", GST_RANK_PRIMARY, ac3_type_find,
      ac3_exts, AC3_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "audio/x-dts", GST_RANK_SECONDARY, dts_type_find,
      dts_exts, DTS_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "audio/x-gsm", GST_RANK_PRIMARY, NULL, gsm_exts,
      GSM_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/mpeg-sys", GST_RANK_PRIMARY,
      mpeg_sys_type_find, mpeg_sys_exts, MPEG_SYS_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/mpegts", GST_RANK_PRIMARY,
      mpeg_ts_type_find, mpeg_ts_exts, MPEGTS_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/ogg", GST_RANK_PRIMARY,
      ogganx_type_find, ogg_exts, OGGANX_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/mpeg-elementary", GST_RANK_MARGINAL,
      mpeg_video_stream_type_find, mpeg_video_exts, MPEG_VIDEO_CAPS, NULL,
      NULL);
  TYPE_FIND_REGISTER (plugin, "video/mpeg4", GST_RANK_PRIMARY,
      mpeg4_video_type_find, m4v_exts, MPEG_VIDEO_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/x-h263", GST_RANK_SECONDARY,
      h263_video_type_find, h263_exts, H263_VIDEO_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/x-h264", GST_RANK_PRIMARY,
      h264_video_type_find, h264_exts, H264_VIDEO_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/x-nuv", GST_RANK_SECONDARY, nuv_type_find,
      nuv_exts, NUV_CAPS, NULL, NULL);

  /* ISO formats */
  TYPE_FIND_REGISTER (plugin, "audio/x-m4a", GST_RANK_PRIMARY, m4a_type_find,
      m4a_exts, M4A_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/x-3gp", GST_RANK_PRIMARY,
      q3gp_type_find, q3gp_exts, Q3GP_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/quicktime", GST_RANK_SECONDARY,
      qt_type_find, qt_exts, QT_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "image/x-quicktime", GST_RANK_SECONDARY,
      qtif_type_find, qtif_exts, QTIF_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "image/jp2", GST_RANK_PRIMARY,
      jp2_type_find, jp2_exts, JP2_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/mj2", GST_RANK_PRIMARY,
      jp2_type_find, mj2_exts, MJ2_CAPS, NULL, NULL);

  TYPE_FIND_REGISTER (plugin, "text/html", GST_RANK_SECONDARY, html_type_find,
      html_exts, HTML_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER_START_WITH (plugin, "application/vnd.rn-realmedia",
      GST_RANK_SECONDARY, rm_exts, ".RMF", 4, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "application/x-pn-realaudio",
      GST_RANK_SECONDARY, rm_exts, ".ra\375", 4, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER (plugin, "application/x-shockwave-flash",
      GST_RANK_SECONDARY, swf_type_find, swf_exts, SWF_CAPS, NULL, NULL);
#endif // GSTREAMER_LITE
  TYPE_FIND_REGISTER_START_WITH (plugin, "video/x-flv", GST_RANK_SECONDARY,
      flv_exts, "FLV", 3, GST_TYPE_FIND_MAXIMUM);
#ifndef GSTREAMER_LITE
  TYPE_FIND_REGISTER (plugin, "text/plain", GST_RANK_MARGINAL, utf8_type_find,
      utf8_exts, UTF8_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "text/uri-list", GST_RANK_MARGINAL, uri_type_find,
      uri_exts, URI_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/x-hls", GST_RANK_MARGINAL,
      hls_type_find, hls_exts, HLS_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/sdp", GST_RANK_SECONDARY,
      sdp_type_find, sdp_exts, SDP_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/smil", GST_RANK_SECONDARY,
      smil_type_find, smil_exts, SMIL_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/xml", GST_RANK_MARGINAL,
      xml_type_find, xml_exts, GENERIC_XML_CAPS, NULL, NULL);
#endif // GSTREAMER_LITE
  TYPE_FIND_REGISTER_RIFF (plugin, "audio/x-wav", GST_RANK_PRIMARY, wav_exts,
      "WAVE");
  TYPE_FIND_REGISTER (plugin, "audio/x-aiff", GST_RANK_SECONDARY,
      aiff_type_find, aiff_exts, AIFF_CAPS, NULL, NULL);
#ifndef GSTREAMER_LITE
  TYPE_FIND_REGISTER (plugin, "audio/x-svx", GST_RANK_SECONDARY, svx_type_find,
      svx_exts, SVX_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "audio/x-paris", GST_RANK_SECONDARY,
      paris_type_find, paris_exts, PARIS_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-nist", GST_RANK_SECONDARY,
      nist_exts, "NIST", 4, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-voc", GST_RANK_SECONDARY,
      voc_exts, "Creative", 8, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER (plugin, "audio/x-sds", GST_RANK_SECONDARY, sds_type_find,
      sds_exts, SDS_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "audio/x-ircam", GST_RANK_SECONDARY,
      ircam_type_find, ircam_exts, IRCAM_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-w64", GST_RANK_SECONDARY,
      w64_exts, "riff", 4, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER (plugin, "audio/x-shorten", GST_RANK_SECONDARY,
      shn_type_find, shn_exts, SHN_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/x-ape", GST_RANK_SECONDARY,
      ape_type_find, ape_exts, APE_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "image/jpeg", GST_RANK_PRIMARY + 15,
      jpeg_type_find, jpeg_exts, JPEG_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER_START_WITH (plugin, "image/gif", GST_RANK_PRIMARY,
      gif_exts, "GIF8", 4, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "image/png", GST_RANK_PRIMARY + 14,
      png_exts, "\211PNG\015\012\032\012", 8, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER (plugin, "image/bmp", GST_RANK_PRIMARY, bmp_type_find,
      bmp_exts, BMP_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "image/tiff", GST_RANK_PRIMARY, tiff_type_find,
      tiff_exts, TIFF_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "image/x-portable-pixmap", GST_RANK_SECONDARY,
      pnm_type_find, pnm_exts, PNM_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/x-matroska", GST_RANK_PRIMARY,
      matroska_type_find, matroska_exts, MATROSKA_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/webm", GST_RANK_PRIMARY,
      webm_type_find, webm_exts, WEBM_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/mxf", GST_RANK_PRIMARY,
      mxf_type_find, mxf_exts, MXF_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER_START_WITH (plugin, "video/x-mve", GST_RANK_SECONDARY,
      mve_exts, "Interplay MVE File\032\000\032\000\000\001\063\021", 26,
      GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER (plugin, "video/x-dv", GST_RANK_SECONDARY, dv_type_find,
      dv_exts, DV_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-amr-nb-sh", GST_RANK_PRIMARY,
      amr_exts, "#!AMR", 5, GST_TYPE_FIND_LIKELY);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-amr-wb-sh", GST_RANK_PRIMARY,
      amr_exts, "#!AMR-WB", 7, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER (plugin, "audio/iLBC-sh", GST_RANK_PRIMARY,
      ilbc_type_find, ilbc_exts, ILBC_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-sid", GST_RANK_MARGINAL,
      sid_exts, "PSID", 4, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "image/x-xcf", GST_RANK_SECONDARY,
      xcf_exts, "gimp xcf", 8, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "video/x-mng", GST_RANK_SECONDARY,
      mng_exts, "\212MNG\015\012\032\012", 8, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "image/x-jng", GST_RANK_SECONDARY,
      jng_exts, "\213JNG\015\012\032\012", 8, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "image/x-xpixmap", GST_RANK_SECONDARY,
      xpm_exts, "/* XPM */", 9, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "image/x-sun-raster",
      GST_RANK_SECONDARY, ras_exts, "\131\246\152\225", 4,
      GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "application/x-bzip",
      GST_RANK_SECONDARY, bz2_exts, "BZh", 3, GST_TYPE_FIND_LIKELY);
  TYPE_FIND_REGISTER_START_WITH (plugin, "application/x-gzip",
      GST_RANK_SECONDARY, gz_exts, "\037\213", 2, GST_TYPE_FIND_LIKELY);
  TYPE_FIND_REGISTER_START_WITH (plugin, "application/zip", GST_RANK_SECONDARY,
      zip_exts, "PK\003\004", 4, GST_TYPE_FIND_LIKELY);
  TYPE_FIND_REGISTER_START_WITH (plugin, "application/x-compress",
      GST_RANK_SECONDARY, compress_exts, "\037\235", 2, GST_TYPE_FIND_LIKELY);
  TYPE_FIND_REGISTER (plugin, "subtitle/x-kate", GST_RANK_MARGINAL,
      kate_type_find, NULL, NULL, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "audio/x-flac", GST_RANK_PRIMARY,
      flac_type_find, flac_exts, FLAC_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "audio/x-vorbis", GST_RANK_PRIMARY,
      vorbis_type_find, NULL, VORBIS_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/x-theora", GST_RANK_PRIMARY,
      theora_type_find, NULL, THEORA_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/x-ogm-video", GST_RANK_PRIMARY,
      ogmvideo_type_find, NULL, OGMVIDEO_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/x-ogm-audio", GST_RANK_PRIMARY,
      ogmaudio_type_find, NULL, OGMAUDIO_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/x-ogm-text", GST_RANK_PRIMARY,
      ogmtext_type_find, NULL, OGMTEXT_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "audio/x-speex", GST_RANK_PRIMARY,
      speex_type_find, NULL, SPEEX_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "audio/x-celt", GST_RANK_PRIMARY,
      celt_type_find, NULL, CELT_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/x-ogg-skeleton", GST_RANK_PRIMARY,
      oggskel_type_find, NULL, OGG_SKELETON_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "text/x-cmml", GST_RANK_PRIMARY, cmml_type_find,
      NULL, CMML_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER_START_WITH (plugin, "application/x-executable",
      GST_RANK_MARGINAL, NULL, "\177ELF", 4, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER (plugin, "audio/aac", GST_RANK_SECONDARY,
      aac_type_find, aac_exts, AAC_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-spc", GST_RANK_SECONDARY,
      spc_exts, "SNES-SPC700 Sound File Data", 27, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER (plugin, "audio/x-wavpack", GST_RANK_SECONDARY,
      wavpack_type_find, wavpack_exts, WAVPACK_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "audio/x-wavpack-correction", GST_RANK_SECONDARY,
      wavpack_type_find, wavpack_correction_exts, WAVPACK_CORRECTION_CAPS, NULL,
      NULL);
  TYPE_FIND_REGISTER (plugin, "application/postscript", GST_RANK_SECONDARY,
      postscript_type_find, ps_exts, POSTSCRIPT_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "image/svg+xml", GST_RANK_SECONDARY,
      svg_type_find, svg_exts, SVG_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER_START_WITH (plugin, "application/x-rar",
      GST_RANK_SECONDARY, rar_exts, "Rar!", 4, GST_TYPE_FIND_LIKELY);
  TYPE_FIND_REGISTER (plugin, "application/x-tar", GST_RANK_SECONDARY,
      tar_type_find, tar_exts, TAR_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/x-ar", GST_RANK_SECONDARY,
      ar_type_find, ar_exts, AR_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/x-ms-dos-executable",
      GST_RANK_SECONDARY, msdos_type_find, msdos_exts, MSDOS_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/x-dirac", GST_RANK_PRIMARY,
      dirac_type_find, NULL, DIRAC_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "multipart/x-mixed-replace", GST_RANK_SECONDARY,
      multipart_type_find, NULL, MULTIPART_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "application/x-mmsh", GST_RANK_SECONDARY,
      mmsh_type_find, NULL, MMSH_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER (plugin, "video/vivo", GST_RANK_SECONDARY,
      vivo_type_find, vivo_exts, VIVO_CAPS, NULL, NULL);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-nsf",
      GST_RANK_SECONDARY, nsf_exts, "NESM\x1a", 5, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-gym",
      GST_RANK_SECONDARY, gym_exts, "GYMX", 4, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-ay",
      GST_RANK_SECONDARY, ay_exts, "ZXAYEMUL", 8, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-gbs",
      GST_RANK_SECONDARY, gbs_exts, "GBS\x01", 4, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-vgm",
      GST_RANK_SECONDARY, vgm_exts, "Vgm\x20", 4, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-sap",
      GST_RANK_SECONDARY, sap_exts, "SAP\x0d\x0aAUTHOR\x20", 12,
      GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "video/x-ivf", GST_RANK_SECONDARY,
      ivf_exts, "DKIF", 4, GST_TYPE_FIND_NEARLY_CERTAIN);
  TYPE_FIND_REGISTER_START_WITH (plugin, "audio/x-kss", GST_RANK_SECONDARY,
      kss_exts, "KSSX\0", 5, GST_TYPE_FIND_MAXIMUM);
  TYPE_FIND_REGISTER_START_WITH (plugin, "application/pdf", GST_RANK_SECONDARY,
      pdf_exts, "%PDF-", 5, GST_TYPE_FIND_LIKELY);
  TYPE_FIND_REGISTER_START_WITH (plugin, "application/msword",
      GST_RANK_SECONDARY, msword_exts, "\320\317\021\340\241\261\032\341", 8,
      GST_TYPE_FIND_LIKELY);
  /* Mac OS X .DS_Store files tend to be taken for video/mpeg */
  TYPE_FIND_REGISTER_START_WITH (plugin, "application/octet-stream",
      GST_RANK_SECONDARY, dsstore_exts, "\000\000\000\001Bud1", 8,
      GST_TYPE_FIND_LIKELY);
  TYPE_FIND_REGISTER_START_WITH (plugin, "image/vnd.adobe.photoshop",
      GST_RANK_SECONDARY, psd_exts, "8BPS\000\001\000\000\000\000", 10,
      GST_TYPE_FIND_LIKELY);
  TYPE_FIND_REGISTER_START_WITH (plugin, "application/x-yuv4mpeg",
      GST_RANK_SECONDARY, y4m_exts, "YUV4MPEG2 ", 10, GST_TYPE_FIND_LIKELY);
  TYPE_FIND_REGISTER (plugin, "image/x-icon", GST_RANK_MARGINAL,
      windows_icon_typefind, NULL, NULL, NULL, NULL);

#ifdef USE_GIO
  TYPE_FIND_REGISTER (plugin, "xdgmime-base", GST_RANK_MARGINAL,
      xdgmime_typefind, NULL, NULL, NULL, NULL);
#endif

  TYPE_FIND_REGISTER (plugin, "image/x-degas", GST_RANK_MARGINAL,
      degas_type_find, NULL, NULL, NULL, NULL);
#endif // GSTREAMER_LITE

  return TRUE;
}

#ifndef GSTREAMER_LITE
GST_PLUGIN_DEFINE (GST_VERSION_MAJOR,
    GST_VERSION_MINOR,
    "typefindfunctions",
    "default typefind functions",
    plugin_init, VERSION, GST_LICENSE, GST_PACKAGE_NAME, GST_PACKAGE_ORIGIN)
#endif // GSTREAMER_LITE