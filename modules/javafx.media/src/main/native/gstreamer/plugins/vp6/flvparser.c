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

#include "flvparser.h"

#include <string.h>

GST_DEBUG_CATEGORY_EXTERN (fxm_plugin_debug);
#define GST_CAT_DEFAULT fxm_plugin_debug

/*
 * FLV File Structure:
 *
 * Header               FLV_HEADER
 * Prev tag size        FLV_TAG_SUFFIX  Always 0, skip this
 * Tag prefix           FLV_TAG_PREFIX
 * Tag data             UI8[]           Data body
 * Tag suffix           FLV_TAG_SUFFIX  Previous tag length including its header
 * ...
 * Last tag prefix      FLV_TAG_PREFIX
 * Tag data             UI8[]           Data body
 * Last tah suffix      FLV_TAG_SUFFIX  Previous tag length including its header
 */

/*
 * FLV_HEADER:
 * Signature            UI8     'F'
 * Signature            UI8     'L'
 * Signature            UI8     'V'
 * Version              UI8     File Version (0x01 for FLV version 1)
 * TypeFlagsReserved    UB[5]   Must be 0
 * TypeFlagsAudio       UB[1]   Audio tags are present
 * TypeFagsReserved     UB[1]   Must be 0
 * TypeFlagsVideo       UB[1]   Video tags are present
 * DataOffset           UI32    Size of Header
 */
#define FLV_HEADER_SIZE (9)

#define FLV_HEADER_FLAG_HAS_VIDEO_TAGS (0x01)
#define FLV_HEADER_FLAG_HAS_AUDIO_TAGS (0x04)
#define FLV_HEADER_FLAG_RESERVED       (0xFA)

/*
 * FLV_TAG_PREFIX
 * TagType              UI8     Type of Tag (8: Audio, 9: Video, 18: Script data)
 * DataSize             UI24    Length of data in the Data field
 * Timestamp            UI24    Time in ms at which the data in this tags applies
 * TimestampExtended    UI8     Extention of timestamp field
 * StreamID             UI24    Always 0
 */

#define FLV_TAG_PREFIX_SIZE (11)

/*
 * FLV_TAG_SUFFIX
 * Tag size             UI32    Previous tag size including its header
 */
#define FLV_TAG_SUFFIX_SIZE (4)

/*
 * FLV_AUDIO_TAG_BODY
 * SoundFormat          UB[4]
 * SoundRate            UB[2]
 * SoundSize            UB[1]   0 - 8bit, 1 - 16bit
 * SoundType            UB[1]   0 - Mono, 1 - Stereo
 * AudioCodecData       UI8[]
 * AudioPacketData      UI8[]
 */
#define FLV_AUDIO_PREFIX_LENGTH (1)

static gsize
flv_audio_format_data_size[16] = {
    0, 0, 0, 0, /* PCM, ADPCM, MP3, PCM */
    0, 0, 0, 0, /* Nelly, Nelly, Nelly, G.711 */
    0, 0, 1, 0, /* G.711, reserved, AAC, Unused */
    0, 0, 0, 0  /* Unused, Unused, MP3, Device-specific */
};

/*
 * FLV_VIDEO_TAG_BODY
 * FrameType            UB[4]
 * CodecID              UB[4]
 * VideoCodecData       UI8[]   See flv_video_codec_data_size
 * VideoPacket
 */
#define FLV_VIDEO_PREFIX_LENGTH (1)

static gsize
flv_video_codec_data_size[16] = {
    0, 0, 0, 0, /* Unused, JPEG, Sorenson, Screen video */
    1, 1, 0, 6, /* VP6, VP6 w. Alpha, Screen video v 2, AVC */
    0, 0, 0, 0, /* Unused */
    0, 0, 0, 0  /* Unused */
};

void
flv_parser_init(FlvParser* parser)
{
    parser->state = FLV_PARSER_EXPECT_HEADER;
    parser->parsed_block_size = 0;
    parser->file_position = 0;
    parser->next_block_size = FLV_HEADER_SIZE;
}

void
flv_parser_reset(FlvParser* parser)
{
    parser->state = FLV_PARSER_EXPECT_HEADER;
    parser->parsed_block_size = 0;
    parser->file_position = 0;
    parser->next_block_size = FLV_HEADER_SIZE;
}

FlvParserResult
flv_parser_read_header(FlvParser* parser,
        guchar* buffer, gsize buffer_size, FlvHeader* header)
{
    gsize data_offset;

    /* Check state */
    if (parser->state != FLV_PARSER_EXPECT_HEADER)
        return FLV_PARSER_INVALID_STATE;

    /* Check length */
    if (buffer_size < FLV_HEADER_SIZE)
        return FLV_PARSER_BUFFER_UNDERRUN;

    /* Check signature */
    if (buffer[0] != 'F' || buffer[1] != 'L' || buffer[2] != 'V')
        return FLV_PARSER_BAD_STREAM;

    /* Get version */
    header->file_version = buffer[3];
    if (header->file_version != 1)
        return FLV_PARSER_UNSUPPORTED_STREAM;

    /* Check if stream has valid flags */
    if ((buffer[4] & FLV_HEADER_FLAG_RESERVED) != 0)
        return FLV_PARSER_BAD_STREAM;

    /* Get whether the stream has audio and video tags */
    header->has_audio_tags =
            ((buffer[4] & FLV_HEADER_FLAG_HAS_AUDIO_TAGS) != 0);
    header->has_video_tags =
            ((buffer[4] & FLV_HEADER_FLAG_HAS_VIDEO_TAGS) != 0);

    /* Read data offset */
    data_offset = GST_READ_UINT32_BE(buffer + 5);
    if (data_offset < FLV_HEADER_SIZE)
        return FLV_PARSER_BAD_STREAM;

    /* Update parser and return */
    parser->parsed_block_size = FLV_HEADER_SIZE;
    parser->file_position += parser->parsed_block_size;
    parser->next_block_size = (data_offset == FLV_HEADER_SIZE) ?
            FLV_TAG_SUFFIX_SIZE
            : data_offset - FLV_HEADER_SIZE + FLV_TAG_SUFFIX_SIZE;

    parser->state = FLV_PARSER_EXPECT_SKIP_BLOCK;
    return FLV_PARSER_OK;
}

FlvParserResult
flv_parser_skip(FlvParser* parser,
        guchar* buffer, gsize buffer_size)
{
    if (parser->state != FLV_PARSER_EXPECT_SKIP_BLOCK)
        return FLV_PARSER_INVALID_STATE;

    if (buffer_size < parser->next_block_size)
        return FLV_PARSER_BUFFER_UNDERRUN;

    /* Update parser and return */
    parser->parsed_block_size = parser->next_block_size;
    parser->file_position += parser->parsed_block_size;
    parser->next_block_size = FLV_TAG_PREFIX_SIZE;
    parser->state = FLV_PARSER_EXPECT_TAG_PREFIX;
    return FLV_PARSER_OK;
}

FlvParserResult
flv_parser_read_tag_prefix(FlvParser* parser,
        guchar* buffer, gsize buffer_size, FlvTagPrefix* tag)
{
    guint stream_id;
    if (parser->state != FLV_PARSER_EXPECT_TAG_PREFIX)
        return FLV_PARSER_INVALID_STATE;

    if (buffer_size < FLV_TAG_PREFIX_SIZE)
        return FLV_PARSER_BUFFER_UNDERRUN;

    //Fill tag
    tag->tag_type = buffer[0];
    tag->body_size = FLV_READ_UINT24_BE(buffer + 1);
    tag->timestamp = FLV_READ_TS32(buffer + 4);

    //Check that StreamID is 0
    stream_id = FLV_READ_UINT24_BE(buffer + 8);
    if (stream_id != 0)
        return FLV_PARSER_UNSUPPORTED_STREAM;

    /* Update parser and return */
    parser->parsed_block_size = FLV_TAG_PREFIX_SIZE;
    parser->file_position += parser->parsed_block_size;
    parser->next_block_size = tag->body_size + FLV_TAG_SUFFIX_SIZE;
    switch (tag->tag_type) {
        case FLV_TAG_TYPE_AUDIO:
            parser->state = FLV_PARSER_EXPECT_AUDIO_TAG_BODY;
            break;
        case FLV_TAG_TYPE_VIDEO:
            parser->state = FLV_PARSER_EXPECT_VIDEO_TAG_BODY;
            break;
        case FLV_TAG_TYPE_SCRIPT_DATA:
            parser->state = FLV_PARSER_EXPECT_SCRIPT_DATA_TAG_BODY;
            break;
        default:
            return FLV_PARSER_UNSUPPORTED_STREAM;
    }
    return FLV_PARSER_OK;
}

FlvParserResult
flv_parser_read_audio_tag(FlvParser* parser,
        guchar* buffer, gsize buffer_size, FlvAudioTag* audio_tag)
{
    if (parser->state != FLV_PARSER_EXPECT_AUDIO_TAG_BODY)
        return FLV_PARSER_INVALID_STATE;

    if (buffer_size < parser->next_block_size)
        return FLV_PARSER_BUFFER_UNDERRUN;

    //Fill format info
    audio_tag->sound_format = (buffer[0] & 0xF0) >> 4;
    audio_tag->sampling_rate = (buffer[0] & 0x0C) >> 2;
    audio_tag->is_16bit = ((buffer[0] & 0x02) != 0);
    audio_tag->is_stereo = ((buffer[0] & 0x01) != 0);

    //Fill audio packet info
    audio_tag->audio_packet_offset = FLV_AUDIO_PREFIX_LENGTH +
            flv_audio_format_data_size[audio_tag->sound_format];
    audio_tag->audio_packet_size = parser->next_block_size
            - FLV_TAG_SUFFIX_SIZE - audio_tag->audio_packet_offset;


    /* Update parser and return */
    parser->parsed_block_size = parser->next_block_size;
    parser->file_position += parser->parsed_block_size;
    parser->next_block_size = FLV_TAG_PREFIX_SIZE;
    parser->state = FLV_PARSER_EXPECT_TAG_PREFIX;
    return FLV_PARSER_OK;
}

FlvParserResult
flv_parser_read_video_tag(FlvParser* parser,
        guchar* buffer, gsize buffer_size, FlvVideoTag* video_tag)
{
    if (parser->state != FLV_PARSER_EXPECT_VIDEO_TAG_BODY)
        return FLV_PARSER_INVALID_STATE;

    if (buffer_size < parser->next_block_size)
        return FLV_PARSER_BUFFER_UNDERRUN;

    //Fill format info
    video_tag->frame_type = (buffer[0] & 0xF0) >> 4;
    video_tag->codec_id = buffer[0] & 0x0F;

    video_tag->video_packet_offset = FLV_VIDEO_PREFIX_LENGTH
            + flv_video_codec_data_size[video_tag->codec_id];

    video_tag->video_packet_size = parser->next_block_size
            - FLV_TAG_SUFFIX_SIZE - video_tag->video_packet_offset;

    /* Update parser and return */
    parser->parsed_block_size = parser->next_block_size;
    parser->file_position += parser->parsed_block_size;
    parser->next_block_size = FLV_TAG_PREFIX_SIZE;
    parser->state = FLV_PARSER_EXPECT_TAG_PREFIX;
    return FLV_PARSER_OK;
}

FlvParserResult
flv_parser_read_script_data_tag(FlvParser* parser,
        guchar* buffer, gsize buffer_size,
        FlvScriptDataReader* script_data_reader)
{
    if (parser->state != FLV_PARSER_EXPECT_SCRIPT_DATA_TAG_BODY)
        return FLV_PARSER_INVALID_STATE;

    if (buffer_size < parser->next_block_size)
        return FLV_PARSER_BUFFER_UNDERRUN;

    //Fill iterator
    script_data_reader->position = buffer;
    script_data_reader->end = buffer + parser->next_block_size
            - FLV_TAG_SUFFIX_SIZE;

    /* Update parser and return */
    parser->parsed_block_size = parser->next_block_size;
    parser->file_position += parser->parsed_block_size;
    parser->next_block_size = FLV_TAG_PREFIX_SIZE;
    parser->state = FLV_PARSER_EXPECT_TAG_PREFIX;
    return FLV_PARSER_OK;
}

FlvParserResult
flv_parser_seek(FlvParser* parser, guint64 new_position)
{
    parser->state = FLV_PARSER_EXPECT_TAG_PREFIX;
    parser->file_position = new_position;
    parser->parsed_block_size = 0;
    parser->next_block_size = FLV_TAG_PREFIX_SIZE;
    return FLV_PARSER_OK;
}
