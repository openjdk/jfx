/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

#ifndef __FLV_PARSER_H__
#define __FLV_PARSER_H__

#include <gst/gst.h>
#include "flvmetadata.h"

/* Definitions of parse results */
typedef enum {
    FLV_PARSER_OK, //Pasring completed successfully
    FLV_PARSER_BUFFER_UNDERRUN, //Buffer is not as long as expected
    FLV_PARSER_BAD_STREAM, //Stream in wrong format
    FLV_PARSER_UNSUPPORTED_STREAM, //Stream of wrong version or with unrecognized tags
    FLV_PARSER_INVALID_STATE //Parser is not in appropriate state for this method call
} FlvParserResult;

/* Definitions of parser states */
typedef enum {
    FLV_PARSER_EXPECT_HEADER, //Parser expects header
    FLV_PARSER_EXPECT_SKIP_BLOCK, //Parser expects skip block after header
    FLV_PARSER_EXPECT_TAG_PREFIX, //Parser expects
    FLV_PARSER_EXPECT_VIDEO_TAG_BODY, //Parser expects video tag body
    FLV_PARSER_EXPECT_AUDIO_TAG_BODY, //Parser expects audio tag body
    FLV_PARSER_EXPECT_SCRIPT_DATA_TAG_BODY, //Parser expects ScriptData tag body
    FLV_PARSER_EOF
} FlvParserState;

#define FLV_TAG_TYPE_AUDIO (8)
#define FLV_TAG_TYPE_VIDEO (9)
#define FLV_TAG_TYPE_SCRIPT_DATA (18)

#define FLV_VIDEO_CODEC_VP6 (4)
#define FLV_VIDEO_FRAME_KEY (1)

typedef struct _FlvParser FlvParser;
typedef struct _FlvHeader FlvHeader;
typedef struct _FlvTagPrefix FlvTagPrefix;
typedef struct _FlvAudioTag FlvAudioTag;
typedef struct _FlvVideoTag FlvVideoTag;

struct _FlvParser {
    FlvParserState  state;
    guint64         file_position;
    gsize           parsed_block_size;
    gsize           next_block_size;
};

struct _FlvHeader {
    gint            file_version;
    gboolean        has_audio_tags;
    gboolean        has_video_tags;
};

struct _FlvTagPrefix {
    guchar          tag_type;
    gsize           body_size;
    gint            timestamp;
};

struct _FlvAudioTag {
    guchar          sound_format;
    guchar          sampling_rate;
    gboolean        is_16bit;
    gboolean        is_stereo;
    gsize           audio_packet_offset;
    gsize           audio_packet_size;
};

struct _FlvVideoTag {
    guchar          codec_id;
    guchar          frame_type;
    gsize           video_packet_offset;
    gsize           video_packet_size;
};

/*!
 * \brief Initialize parsing context
 */
void
flv_parser_init(FlvParser* parser);

/*!
 * \brief Resets parsing context to the beginning of file
 */
void
flv_parser_reset(FlvParser* parser);

/*!
 * \brief Parse FLV Header
 */
FlvParserResult
flv_parser_read_header(FlvParser* parser,
        guchar* buffer, gsize buffer_size, FlvHeader* header);

/*!
 * \brief Skip data block
 */
FlvParserResult
flv_parser_skip(FlvParser* parser,
        guchar* buffer, gsize buffer_size);

/*!
 * \brief Parse Tag prefix
 */
FlvParserResult
flv_parser_read_tag_prefix(FlvParser* parser,
        guchar* buffer, gsize buffer_size, FlvTagPrefix* tag);

/*!
 * \brief Parse Audio tag
 */
FlvParserResult
flv_parser_read_audio_tag(FlvParser* parser,
        guchar* buffer, gsize buffer_size, FlvAudioTag* audio_tag);

/*!
 * \brief Parse Video tag
 */
FlvParserResult
flv_parser_read_video_tag(FlvParser* parser,
        guchar* buffer, gsize buffer_size, FlvVideoTag* video_tag);

/*!
 * \brief Parse Script Data tag. Returns reader.
 */
FlvParserResult
flv_parser_read_script_data_tag(FlvParser* parser,
        guchar* buffer, gsize buffer_size, FlvScriptDataReader* reader);

/*!
 * \brief Seek parser to another position in the stream that corresponds to the
 * beginning of the tag.
 */
FlvParserResult
flv_parser_seek(FlvParser* parser, guint64 new_position);

/* Support macros */
#define FLV_READ_UINT24_BE(data) ((GST_READ_UINT16_BE(data) << 8) | \
    GST_READ_UINT8 (data + 2))


#define FLV_READ_TS32(data) ((GST_READ_UINT16_BE(data) << 8)| \
    GST_READ_UINT8 (data + 2) | (GST_READ_UINT8(data + 3) << 24))

#endif /* __FLVPARSE_H__ */

