/* GStreamer AAC parser
 * Copyright (C) 2008 Nokia Corporation. All rights reserved.
 *
 * Contact: Stefan Kost <stefan.kost@nokia.com>
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

#ifndef __GST_AAC_PARSE_H__
#define __GST_AAC_PARSE_H__

#include <gst/gst.h>
#include <gst/base/gstbaseparse.h>

G_BEGIN_DECLS

#define GST_TYPE_AAC_PARSE \
  (gst_aac_parse_get_type())
#define GST_AAC_PARSE(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj), GST_TYPE_AAC_PARSE, GstAacParse))
#define GST_AAC_PARSE_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass), GST_TYPE_AAC_PARSE, GstAacParseClass))
#define GST_IS_AAC_PARSE(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj), GST_TYPE_AAC_PARSE))
#define GST_IS_AAC_PARSE_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass), GST_TYPE_AAC_PARSE))


/**
 * GstAacHeaderType:
 * @DSPAAC_HEADER_NOT_PARSED: Header not parsed yet.
 * @DSPAAC_HEADER_UNKNOWN: Unknown (not recognized) header.
 * @DSPAAC_HEADER_ADIF: ADIF header found.
 * @DSPAAC_HEADER_ADTS: ADTS header found.
 * @DSPAAC_HEADER_NONE: Raw stream, no header.
 *
 * Type header enumeration set in #header_type.
 */
typedef enum {
  DSPAAC_HEADER_NOT_PARSED,
  DSPAAC_HEADER_UNKNOWN,
  DSPAAC_HEADER_ADIF,
  DSPAAC_HEADER_ADTS,
  DSPAAC_HEADER_NONE
} GstAacHeaderType;


typedef struct _GstAacParse GstAacParse;
typedef struct _GstAacParseClass GstAacParseClass;

/**
 * GstAacParse:
 * @element: the parent element.
 * @object_type: AAC object type of the stream.
 * @bitrate:  Current media bitrate.
 * @sample_rate: Current media samplerate.
 * @channels: Current media channel count.
 * @frames_per_sec: FPS value of the current stream.
 * @header_type: #GstAacHeaderType indicating the current stream type.
 * @framecount: The amount of frames that has been processed this far.
 * @bytecount: The amount of bytes that has been processed this far.
 * @sync: Tells whether the parser is in sync (a.k.a. not searching for header)
 * @eos: End-of-Stream indicator. Set when EOS event arrives.
 * @duration: Duration of the current stream.
 * @ts: Current stream timestamp.
 *
 * The opaque GstAacParse data structure.
 */
struct _GstAacParse {
  GstBaseParse element;

  /* Stream type -related info */
  gint           object_type;
  gint           bitrate;
  gint           sample_rate;
  gint           channels;
  gint           mpegversion;

  GstAacHeaderType header_type;
};

/**
 * GstAacParseClass:
 * @parent_class: Element parent class.
 *
 * The opaque GstAacParseClass data structure.
 */
struct _GstAacParseClass {
  GstBaseParseClass parent_class;
};

GType gst_aac_parse_get_type (void);

G_END_DECLS

#endif /* __GST_AAC_PARSE_H__ */
