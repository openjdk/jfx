/* GStreamer
 * Copyright (C) <1999> Erik Walthinsen <omega@cse.ogi.edu>
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


#ifndef __GST_QTDEMUX_H__
#define __GST_QTDEMUX_H__

#include <gst/gst.h>
#include <gst/base/gstadapter.h>
#include <gst/base/gstflowcombiner.h>
#include <gst/base/gstbytereader.h>
#include <gst/video/video.h>
#include "gstisoff.h"

G_BEGIN_DECLS

#define GST_TYPE_QTDEMUX \
  (gst_qtdemux_get_type())
#define GST_QTDEMUX(obj) \
  (G_TYPE_CHECK_INSTANCE_CAST((obj),GST_TYPE_QTDEMUX,GstQTDemux))
#define GST_QTDEMUX_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_CAST((klass),GST_TYPE_QTDEMUX,GstQTDemuxClass))
#define GST_IS_QTDEMUX(obj) \
  (G_TYPE_CHECK_INSTANCE_TYPE((obj),GST_TYPE_QTDEMUX))
#define GST_IS_QTDEMUX_CLASS(klass) \
  (G_TYPE_CHECK_CLASS_TYPE((klass),GST_TYPE_QTDEMUX))

#define GST_QTDEMUX_CAST(obj) ((GstQTDemux *)(obj))

/* qtdemux produces these for atoms it cannot parse */
#define GST_QT_DEMUX_PRIVATE_TAG "private-qt-tag"
#define GST_QT_DEMUX_CLASSIFICATION_TAG "classification"

typedef struct _GstQTDemux GstQTDemux;
typedef struct _GstQTDemuxClass GstQTDemuxClass;
typedef struct _QtDemuxStream QtDemuxStream;
typedef struct _QtDemuxSample QtDemuxSample;
typedef struct _QtDemuxSegment QtDemuxSegment;
typedef struct _QtDemuxRandomAccessEntry QtDemuxRandomAccessEntry;
typedef struct _QtDemuxStreamStsdEntry QtDemuxStreamStsdEntry;

enum QtDemuxState
{
  QTDEMUX_STATE_INITIAL,        /* Initial state (haven't got the header yet) */
  QTDEMUX_STATE_HEADER,         /* Parsing the header */
  QTDEMUX_STATE_MOVIE,          /* Parsing/Playing the media data */
  QTDEMUX_STATE_BUFFER_MDAT     /* Buffering the mdat atom */
};

struct _GstQTDemux {
  GstElement element;

  /* Global state */
  enum QtDemuxState state;

  /* static sink pad */
  GstPad *sinkpad;

  /* TRUE if pull-based */
  gboolean pullbased;

  gchar *redirect_location;

  /* Protect pad exposing from flush event */
  GMutex expose_lock;

  /* list of QtDemuxStream */
  GPtrArray *active_streams;
  GPtrArray *old_streams;

  gint     n_video_streams;
  gint     n_audio_streams;
  gint     n_sub_streams;

  GstFlowCombiner *flowcombiner;

  /* Incoming stream group-id to set on downstream STREAM_START events.
   * If upstream doesn't contain one, a global one will be generated */
  gboolean have_group_id;
  guint group_id;

  guint  major_brand;
  GstBuffer *comp_brands;

  /* [moov] header.
   * FIXME : This is discarded just after it's created. Just move it
   * to a temporary variable ? */
  GNode *moov_node;

  /* FIXME : This is never freed. It is only assigned once. memleak ? */
  GNode *moov_node_compressed;

  /* Set to TRUE when the [moov] header has been fully parsed */
  gboolean got_moov;

  /* Global timescale for the incoming stream. Use the QTTIME macros
   * to convert values to/from GstClockTime */
  guint32 timescale;

  /* Global duration (in global timescale). Use QTTIME macros to get GstClockTime */
  guint64 duration;

  /* Total size of header atoms. Used to calculate fallback overall bitrate */
  guint header_size;

  GstTagList *tag_list;

  /* configured playback region */
  GstSegment segment;

  /* State for key_units trickmode */
  GstClockTime trickmode_interval;

  /* PUSH-BASED only: If the initial segment event, or a segment consequence of
   * a seek or incoming TIME segment from upstream needs to be pushed. This
   * variable is used instead of pushing the event directly because at that
   * point we may not have yet emitted the srcpads. */
  gboolean need_segment;

  guint32 segment_seqnum;

  /* flag to indicate that we're working with a smoothstreaming fragment
   * Mss doesn't have 'moov' or any information about the streams format,
   * requiring qtdemux to expose and create the streams */
  gboolean mss_mode;

  /* Set to TRUE if the incoming stream is either a MSS stream or
   * a Fragmented MP4 (containing the [mvex] atom in the header) */
  gboolean fragmented;

  /* PULL-BASED only : If TRUE there is a pending seek */
  gboolean fragmented_seek_pending;

  /* PULL-BASED : offset of first [moof] or of fragment to seek to
   * PUSH-BASED : offset of latest [moof] */
  guint64 moof_offset;

  /* MSS streams have a single media that is unspecified at the atoms, so
   * upstream provides it at the caps */
  GstCaps *media_caps;

  /* Set to TRUE when all streams have been exposed */
  gboolean exposed;

  gint64 chapters_track_id;

  /* protection support */
  GPtrArray *protection_system_ids; /* Holds identifiers of all content protection systems for all tracks */
  GQueue protection_event_queue; /* holds copy of upstream protection events */
  guint64 cenc_aux_info_offset;
  guint8 *cenc_aux_info_sizes;
  guint32 cenc_aux_sample_count;
  gchar *preferred_protection_system_id;

  /* Whether the parent bin is streams-aware, meaning we can
   * add/remove streams at any point in time */
  gboolean streams_aware;

  /*
   * ALL VARIABLES BELOW ARE ONLY USED IN PUSH-BASED MODE
   */
  GstAdapter *adapter;
  guint neededbytes;
  guint todrop;
  /* Used to store data if [mdat] is before the headers */
  GstBuffer *mdatbuffer;
  /* Amount of bytes left to read in the current [mdat] */
  guint64 mdatleft, mdatsize;

  /* When restoring the mdat to the adapter, this buffer stores any
   * trailing data that was after the last atom parsed as it has to be
   * restored later along with the correct offset. Used in fragmented
   * scenario where mdat/moof are one after the other in any order.
   *
   * Check https://bugzilla.gnome.org/show_bug.cgi?id=710623 */
  GstBuffer *restoredata_buffer;
  guint64 restoredata_offset;

  /* The current offset in bytes from upstream.
   * Note: While it makes complete sense when we are PULL-BASED (pulling
   * in BYTES from upstream) and PUSH-BASED with a BYTE SEGMENT (receiving
   * buffers with actual offsets), it is undefined in PUSH-BASED with a
   * TIME SEGMENT */
  guint64 offset;

  /* offset of the mdat atom */
  guint64 mdatoffset;
  /* Offset of the first mdat */
  guint64 first_mdat;
  /* offset of last [moov] seen */
  guint64 last_moov_offset;

  /* If TRUE, qtdemux received upstream newsegment in TIME format
   * which likely means that upstream is driving the pipeline (such as
   * adaptive demuxers or dlna sources) */
  gboolean upstream_format_is_time;

  /* Seqnum of the seek event sent upstream.  Will be used to
   * detect incoming FLUSH events corresponding to that */
  guint32 offset_seek_seqnum;

  /* UPSTREAM BYTE: Requested upstream byte seek offset.
   * Currently it is only used to check if an incoming BYTE SEGMENT
   * corresponds to a seek event that was sent upstream */
  gint64 seek_offset;

  /* UPSTREAM BYTE: Requested start/stop TIME values from
   * downstream.
   * Used to set on the downstream segment once the corresponding upstream
   * BYTE SEEK has succeeded */
  gint64 push_seek_start;
  gint64 push_seek_stop;

#if 0
  /* gst index support */
  GstIndex *element_index;
  gint index_id;
#endif

  /* Whether upstream is seekable in BYTES */
  gboolean upstream_seekable;
  /* UPSTREAM BYTE: Size of upstream content.
   * Note : This is only computed once ! If upstream grows in the meantime
   * it will not be updated */
  gint64 upstream_size;

  /* UPSTREAM TIME : Contains the PTS (if any) of the
   * buffer that contains a [moof] header. Will be used to establish
   * the actual PTS of the samples contained within that fragment. */
  guint64 fragment_start;
  /* UPSTREAM TIME : The offset in bytes of the [moof]
   * header start.
   * Note : This is not computed from the GST_BUFFER_OFFSET field */
  guint64 fragment_start_offset;

  /* These two fields are used to perform an implicit seek when a fragmented
   * file whose first tfdt is not zero. This way if the first fragment starts
   * at 1 hour, the user does not have to wait 1 hour or perform a manual seek
   * for the image to move and the sound to play.
   *
   * This implicit seek is only done if the first parsed fragment has a non-zero
   * decode base time and a seek has not been received previously, hence these
   * fields. */
  gboolean received_seek;
  gboolean first_moof_already_parsed;
};

struct _GstQTDemuxClass {
  GstElementClass parent_class;
};

GType gst_qtdemux_get_type (void);

struct _QtDemuxStreamStsdEntry
{
  GstCaps *caps;
  guint32 fourcc;
  gboolean sparse;

  /* video info */
  gint width;
  gint height;
  gint par_w;
  gint par_h;
  /* Numerator/denominator framerate */
  gint fps_n;
  gint fps_d;
  GstVideoColorimetry colorimetry;
  guint16 bits_per_sample;
  guint16 color_table_id;
  GstMemory *rgb8_palette;
  guint interlace_mode;
  guint field_order;

  /* audio info */
  gdouble rate;
  gint n_channels;
  guint samples_per_packet;
  guint samples_per_frame;
  guint bytes_per_packet;
  guint bytes_per_sample;
  guint bytes_per_frame;
  guint compression;

  /* if we use chunks or samples */
  gboolean sampled;
  guint padding;

};

struct _QtDemuxSample
{
  guint32 size;
  gint32 pts_offset;            /* Add this value to timestamp to get the pts */
  guint64 offset;
  guint64 timestamp;            /* DTS In mov time */
  guint32 duration;             /* In mov time */
  gboolean keyframe;            /* TRUE when this packet is a keyframe */
};

struct _QtDemuxStream
{
  GstPad *pad;

  GstQTDemux *demux;
  gchar *stream_id;

  QtDemuxStreamStsdEntry *stsd_entries;
  guint stsd_entries_length;
  guint cur_stsd_entry_index;

  /* stream type */
  guint32 subtype;

  gboolean new_caps;            /* If TRUE, caps need to be generated (by
                                 * calling _configure_stream()) This happens
                                 * for MSS and fragmented streams */

  gboolean new_stream;          /* signals that a stream_start is required */
  gboolean on_keyframe;         /* if this stream last pushed buffer was a
                                 * keyframe. This is important to identify
                                 * where to stop pushing buffers after a
                                 * segment stop time */

  /* if the stream has a redirect URI in its headers, we store it here */
  gchar *redirect_uri;

  /* track id */
  guint track_id;
#ifdef GSTREAMER_LITE
  gboolean track_enabled;
#endif // GSTREAMER_LITE

  /* duration/scale */
  guint64 duration;             /* in timescale units */
  guint32 timescale;

  /* language */
  gchar lang_id[4];             /* ISO 639-2T language code */

  /* our samples */
  guint32 n_samples;
  QtDemuxSample *samples;
  gboolean all_keyframe;        /* TRUE when all samples are keyframes (no stss) */
  guint32 n_samples_moof;       /* sample count in a moof */
  guint64 duration_moof;        /* duration in timescale of a moof, used for figure out
                                 * the framerate of fragmented format stream */
  guint64 duration_last_moof;

  guint32 offset_in_sample;     /* Offset in the current sample, used for
                                 * streams which have got exceedingly big
                                 * sample size (such as 24s of raw audio).
                                 * Only used when max_buffer_size is non-NULL */
  guint32 min_buffer_size;      /* Minimum allowed size for output buffers.
                                 * Currently only set for raw audio streams*/
  guint32 max_buffer_size;      /* Maximum allowed size for output buffers.
                                 * Currently only set for raw audio streams*/

  /* video info */
  /* aspect ratio */
  gint display_width;
  gint display_height;

  /* allocation */
  gboolean use_allocator;
  GstAllocator *allocator;
  GstAllocationParams params;

  gsize alignment;

  /* when a discontinuity is pending */
  gboolean discont;

  /* list of buffers to push first */
  GSList *buffers;

  /* if we need to clip this buffer. This is only needed for uncompressed
   * data */
  gboolean need_clip;

  /* buffer needs some custom processing, e.g. subtitles */
  gboolean need_process;
  /* buffer needs potentially be split, e.g. CEA608 subtitles */
  gboolean need_split;

  /* current position */
  guint32 segment_index;
  guint32 sample_index;
  GstClockTime time_position;   /* in gst time */
  guint64 accumulated_base;

  /* the Gst segment we are processing out, used for clipping */
  GstSegment segment;

  /* quicktime segments */
  guint32 n_segments;
  QtDemuxSegment *segments;
  gboolean dummy_segment;
  guint32 from_sample;
  guint32 to_sample;

  gboolean sent_eos;
  GstTagList *stream_tags;
  gboolean send_global_tags;

  GstEvent *pending_event;

  GstByteReader stco;
  GstByteReader stsz;
  GstByteReader stsc;
  GstByteReader stts;
  GstByteReader stss;
  GstByteReader stps;
  GstByteReader ctts;

  gboolean chunks_are_samples;  /* TRUE means treat chunks as samples */
  gint64 stbl_index;
  /* stco */
  guint co_size;
  GstByteReader co_chunk;
  guint32 first_chunk;
  guint32 current_chunk;
  guint32 last_chunk;
  guint32 samples_per_chunk;
  guint32 stsd_sample_description_id;
  guint32 stco_sample_index;
  /* stsz */
  guint32 sample_size;          /* 0 means variable sizes are stored in stsz */
  /* stsc */
  guint32 stsc_index;
  guint32 n_samples_per_chunk;
  guint32 stsc_chunk_index;
  guint32 stsc_sample_index;
  guint64 chunk_offset;
  /* stts */
  guint32 stts_index;
  guint32 stts_samples;
  guint32 n_sample_times;
  guint32 stts_sample_index;
  guint64 stts_time;
  guint32 stts_duration;
  /* stss */
  gboolean stss_present;
  guint32 n_sample_syncs;
  guint32 stss_index;
  /* stps */
  gboolean stps_present;
  guint32 n_sample_partial_syncs;
  guint32 stps_index;
  QtDemuxRandomAccessEntry *ra_entries;
  guint n_ra_entries;

  const QtDemuxRandomAccessEntry *pending_seek;

  /* ctts */
  gboolean ctts_present;
  guint32 n_composition_times;
  guint32 ctts_index;
  guint32 ctts_sample_index;
  guint32 ctts_count;
  gint32 ctts_soffset;

  /* cslg */
  guint32 cslg_shift;

  /* fragmented */
  gboolean parsed_trex;
  guint32 def_sample_description_index; /* index is 1-based */
  guint32 def_sample_duration;
  guint32 def_sample_size;
  guint32 def_sample_flags;

  gboolean disabled;

  /* stereoscopic video streams */
  GstVideoMultiviewMode multiview_mode;
  GstVideoMultiviewFlags multiview_flags;

  /* protected streams */
  gboolean protected;
  guint32 protection_scheme_type;
  guint32 protection_scheme_version;
  gpointer protection_scheme_info;      /* specific to the protection scheme */
  GQueue protection_scheme_event_queue;

  /* KEY_UNITS trickmode with an interval */
  GstClockTime last_keyframe_dts;

  gint ref_count;               /* atomic */
};

G_END_DECLS

#endif /* __GST_QTDEMUX_H__ */
