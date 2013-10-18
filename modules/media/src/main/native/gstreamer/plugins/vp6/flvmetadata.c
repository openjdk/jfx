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

#include "flvmetadata.h"
#include "flvparser.h"

#include <string.h>

GST_DEBUG_CATEGORY_EXTERN (fxm_plugin_debug);
#define GST_CAT_DEFAULT fxm_plugin_debug


static double FLV_READ_DOUBLE_BE(guchar* src) {
    /* XXX: Use union here */
    double r = 0;
    guchar* t = (guchar*)(&r);
    t[0] = src[7];
    t[1] = src[6];
    t[2] = src[5];
    t[3] = src[4];
    t[4] = src[3];
    t[5] = src[2];
    t[6] = src[1];
    t[7] = src[0];
    return r;
};

/* ScriptData Readers */

/*!
 * \brief Reads UI8 value and advances stream position by 1 byte.
 */
static gboolean
flv_script_data_read_ui8(FlvScriptDataReader* reader, guint8* dest);

/*!
 * \brief Reads UI32 value and advances stream position by 4 bytes.
 */
static gboolean
flv_script_data_read_ui32(FlvScriptDataReader* reader, guint32* dest);

/*!
 * \brief Reads DOUBLE value and advances stream position by 8 bytes.
 */
static gboolean
flv_script_data_read_double(FlvScriptDataReader* reader, gdouble* dest);

/*!
 * \brief Reads SCRIPTDATASTRING value and advances stream position.
 *
 * Caller is responsible for freeing dest when no longer needed.
 */
static gboolean
flv_script_data_read_string(FlvScriptDataReader* reader, gchar** dest, gboolean longString);

/*!
 * \brief Callback function for variable handlers.
 *
 * This callback function must read variable of specified type from the stream
 * and properly advance reader's position.
 *
 * \param reader        FlvScriptDataReader that performs reading.
 * \param value_name    Name of the value. This parameter might be NULL for nested arrays.
 * \param value_type    Type of value to read.
 *
 * \return  TRUE if reading completed successfully and position advanced properly,
 *          FALSE otherwise.
 */
typedef gboolean (*FlvScriptDataValueHandler)(FlvScriptDataReader* reader,
        gchar* value_name, gint value_type, void* param);

/*!
 * \brief Reads ECMA array and invokes specified callback for every value in array.
 */
static gboolean
flv_script_data_read_object(FlvScriptDataReader* reader,
        FlvScriptDataValueHandler callback, void* callback_param);

/*!
 * \brief Reads ECMA array and invokes specified callback for every value in array.
 */
static gboolean
flv_script_data_read_ecma(FlvScriptDataReader* reader,
        FlvScriptDataValueHandler callback, void* callback_param);

/*!
 * \brief Reads strict array and invokes specified handler for every value in array.
 */
static gboolean
flv_script_data_read_strict_array(FlvScriptDataReader* reader,
        FlvScriptDataValueHandler callback, void* callback_param);

/* Handlers */
/*!
 * \brief Value handler for top-level values in onMetaData tag.
 *
 * This callback should be called with pointer to FLVMetaData as param.
 */
static gboolean
flv_metadata_value_handler(FlvScriptDataReader* reader,
        gchar* value_name, gint value_type, void* param);

/*!
 * \brief Value handler for values on arbitrary nesting level that should be skipped.
 */
static gboolean
flv_metadata_skip_handler(FlvScriptDataReader* reader,
        gchar* value_name, gint value_type, void* param);

FlvMetadata *flv_metadata_new()
{
    FlvMetadata *metadata = g_malloc(sizeof(FlvMetadata));

    if (metadata)
    {
        metadata->duration = GST_CLOCK_TIME_NONE;
        metadata->file_size = 0;
        metadata->can_seek_to_end = 0;
        metadata->video_codec_id = 0;
        metadata->video_data_rate = 0;
        metadata->width = 0;
        metadata->height = 0;
        metadata->par_x = 0;
        metadata->par_y = 0;
        metadata->framerate = 0;
        metadata->audio_codec_id = 0;
        metadata->audio_data_rate = 0;
        metadata->audio_sample_size = 0;
        metadata->is_stereo = 0;
        metadata->tag_list = NULL;
        metadata->keyframes = NULL;
    }

    return metadata;
}

void flv_metadata_free(FlvMetadata *metadata)
{
    if (metadata->tag_list) {
        gst_tag_list_free(metadata->tag_list);
        metadata->tag_list = NULL;
    }

    if (metadata->keyframes) {
        g_array_free(metadata->keyframes, TRUE);
        metadata->keyframes = NULL;
    }

    g_free(metadata);
}

gboolean
flv_script_data_read(FlvScriptDataReader* reader, FlvMetadata* metadata)
{
    guint8 value_type;
    gchar* str;
    gboolean result;

    /* Parse only 'onMetaData' blocks, return TRUE for any other valid blocks. */
    if (!flv_script_data_read_ui8(reader, &value_type))
        return TRUE;
    if (value_type != FLV_SCRIPT_DATA_TYPE_STRING)
        return TRUE;
    if (!flv_script_data_read_string(reader, &str, FALSE))
        return FALSE;
    if (strcmp(str, "onMetaData") != 0) {
        g_free(str);
        return TRUE;
    }
    g_free(str);

    /*onMetaData block should have ECMA Array type. Return FALSE for other types. */
    if (!flv_script_data_read_ui8(reader, &value_type))
        return FALSE;
    if (value_type != FLV_SCRIPT_DATA_TYPE_ECMA)
        return FALSE;

    /* Initialize metadata->tags member */
    if (metadata->tag_list == NULL)
        metadata->tag_list = gst_tag_list_new();

    /* Read tag contents */
    result =  flv_script_data_read_ecma(reader,
            flv_metadata_value_handler, metadata);

    /* Reset allocated memory if something went wrong */
    if (!result) {
        gst_tag_list_free(metadata->tag_list);
        metadata->tag_list = NULL;
    }

    return result;
}

static gboolean
flv_script_data_read_ui8(FlvScriptDataReader* reader, guint8* dest)
{
    if (reader->position + 1 > reader->end) {
        return FALSE;
    }
    *dest = *reader->position;
    reader->position++;
    return TRUE;
}

static gboolean
flv_script_data_read_ui32(FlvScriptDataReader* reader, guint32* dest)
{
    if (reader->position + 4 > reader->end) {
        return FALSE;
    }
    *dest = GST_READ_UINT32_BE(reader->position);
    reader->position+=4;
    return TRUE;
}


static gboolean
flv_script_data_read_double(FlvScriptDataReader* reader, gdouble* dest)
{
    if (reader->position + 8 > reader->end) {
        return FALSE;
    }
    *dest = FLV_READ_DOUBLE_BE(reader->position);
    reader->position+=8;
    return TRUE;
}

static gboolean
flv_script_data_read_string(FlvScriptDataReader* reader, gchar** dest, gboolean longString)
{
    gsize length;

    /* Read length of string */
    if ((reader->position + (longString ? 4 : 2)) > reader->end)
        return FALSE;
    if (longString) {
        length = GST_READ_UINT32_BE(reader->position);
        reader->position += 4;
    } else {
        length = GST_READ_UINT16_BE(reader->position);
        reader->position += 2;
    }

    /* Alloc buffer and copy string into it */
    if ((reader->position + length) > reader->end)
        return FALSE;

    if (length >= G_MAXSIZE - 1)
        return FALSE;

    *dest = g_malloc(length + 1);
    if (*dest == NULL)
        return FALSE;
    memcpy(*dest, reader->position, length);
    (*dest)[length] = 0;
    reader->position += length;

    return TRUE;
}

static gboolean
flv_script_data_read_object(FlvScriptDataReader* reader,
        FlvScriptDataValueHandler callback, void* callback_param)
{
    gchar* var_name;
    guint8 value_type;
    gboolean result;

    while (reader->position < reader->end) {
        if ((reader->position + 3) > reader->end)
            return FALSE;
        if ((reader->position[0] == 0) && (reader->position[1] == 0)
                && reader->position[2] == 9) {
            /* This is special case - object terminator */
            var_name = NULL;
            value_type = FLV_SCRIPT_DATA_TYPE_TERMINATOR;
            reader->position += 3;
        } else {
            /* Read variable name */
            if (!flv_script_data_read_string(reader, &var_name, FALSE)) {
                return FALSE;
            }

            /* Read value type */
            if (!flv_script_data_read_ui8(reader, &value_type)) {
                g_free(var_name);
                return FALSE;
            }
        }

        /* Invoke callback */
        result = callback(reader, var_name, value_type, callback_param);

        if (var_name != NULL)
            g_free(var_name);

        if (value_type == FLV_SCRIPT_DATA_TYPE_TERMINATOR)
            return TRUE;

        if (!result)
            return FALSE;

    }

    /* Return TRUE upon reaching end of ScriptData, because some videos
     * don't have required terminator after object.*/
    return TRUE;
}

static gboolean
flv_script_data_read_ecma(FlvScriptDataReader* reader,
        FlvScriptDataValueHandler callback, void* callback_param)
{
    guint32 size;
    gchar* var_name;
    guint8 value_type;
    gboolean result;

    /* Read approximate size of ECMA array and ignore it */
    if (!flv_script_data_read_ui32(reader, &size))
        return FALSE;

    while (reader->position < reader->end) {
        if ((reader->position + 3) > reader->end)
            return FALSE;
        if ((reader->position[0] == 0) && (reader->position[1] == 0)
                && reader->position[2] == 9) {
            /* This is special case - ECMA terminator */
            var_name = NULL;
            value_type = FLV_SCRIPT_DATA_TYPE_TERMINATOR;
            reader->position += 3;
        } else {
            /* Read variable name */
            if (!flv_script_data_read_string(reader, &var_name, FALSE)) {
                return FALSE;
            }

            /* Read value type */
            if (!flv_script_data_read_ui8(reader, &value_type)) {
                g_free(var_name);
                return FALSE;
            }

        }

        /* Invoke callback */
        result = callback(reader, var_name, value_type, callback_param);

        if (var_name != NULL)
            g_free(var_name);

        if (value_type == FLV_SCRIPT_DATA_TYPE_TERMINATOR)
            return TRUE;

        if (!result)
            return FALSE;

    }

    /* Return TRUE upon reaching end of ScriptData, because some videos
     * don't have required terminator after ECMA array.*/
    return TRUE;
}

static gboolean
flv_script_data_read_strict_array(FlvScriptDataReader* reader,
        FlvScriptDataValueHandler callback, void* callback_param)
{
    guint32 size;
    guint32 i;
    guint8 value_type;
    gboolean result;

    /* Read size of array and ignore it */
    if (!flv_script_data_read_ui32(reader, &size))
        return FALSE;

    for (i = 0; i < size; i++) {
        if (!flv_script_data_read_ui8(reader, &value_type))
            return FALSE;

        /* Invoke callback */
        result = callback(reader, NULL, value_type, callback_param);

        if (!result)
            return FALSE;

    }
    return TRUE;
}

static gboolean
flv_script_read_keyframe_array(FlvScriptDataReader* reader,
        gchar *value_name, GArray *keyframes)
{
    guint32 size, readSize;
    guint32 i;
    guint8 value_type;
    gdouble double_value;
    gint whichField = 0;

    /* Read size of array */
    if (!flv_script_data_read_ui32(reader, &size))
        return FALSE;
    readSize = size;

    if (strcmp(value_name, "times") == 0) {
        whichField = 1;
    } else if (strcmp(value_name, "filepositions") == 0) {
        whichField = 2;
    }
    /* if keyframes is already populated, make sure we have the right size */
    if (keyframes->len > 0) {
        if (keyframes->len > (guint)size) {
            // if keyframes is longer, remove the trailing entries
            g_array_set_size(keyframes, size);
        } else if (keyframes->len < (guint)size) {
            // else adjust readSize to skip incoming trailing entries
            readSize = (guint32)keyframes->len;
        }
    }

    // pre-populate the array
    if (keyframes->len != readSize) {
        g_array_set_size(keyframes, readSize);
    }

    for (i = 0; i < size; i++, readSize--) {
        if (!flv_script_data_read_ui8(reader, &value_type))
            return FALSE;

        if (value_type != FLV_SCRIPT_DATA_TYPE_DOUBLE) {
            return FALSE;
        }

        if (!flv_script_data_read_double(reader, &double_value)) {
            return FALSE;
        }

        if (readSize > 0 && whichField > 0) {
            FlvKeyframe *entry = &g_array_index(keyframes, FlvKeyframe, i);
            if (whichField == 1) {
                entry->time = (GstClockTime)(double_value * GST_SECOND);
            } else if (whichField == 2) {
                entry->fileposition = (guint64)double_value;
            }
        }
    }
    return TRUE;
}

static gboolean flv_metadata_keyframe_handler(FlvScriptDataReader* reader,
        gchar *value_name, gint value_type, void *param)
{
    gboolean result = TRUE;
    FlvMetadata* metadata = (FlvMetadata*)param;

    switch (value_type) {
        case FLV_SCRIPT_DATA_TYPE_TERMINATOR:
            /* Just return true for ECMA terminator */
            break;

        case FLV_SCRIPT_DATA_TYPE_STRICT:
            if (!metadata->keyframes) {
                metadata->keyframes = g_array_new(FALSE, TRUE, sizeof(FlvKeyframe));
            }
            result &= flv_script_read_keyframe_array(reader, value_name, metadata->keyframes);
            break;

        default:
            /* Non-handled type */
            result = FALSE;
            break;
    }

    return result;
}

static gboolean
flv_metadata_value_handler(FlvScriptDataReader* reader,
        gchar* value_name, gint value_type, void* param)
{
    gdouble double_value;
    guint8 boolean_value;
    gchar* string_value;
    gboolean result = TRUE;
    FlvMetadata* metadata = (FlvMetadata*)param;

    switch (value_type) {
        case FLV_SCRIPT_DATA_TYPE_DOUBLE:
            result = flv_script_data_read_double(reader, &double_value);
            if (!result)
                break;

            if (strcmp(value_name, "duration") == 0) {
                metadata->duration = (GstClockTime)(double_value * GST_SECOND);
                gst_tag_list_add (metadata->tag_list, GST_TAG_MERGE_REPLACE,
                    GST_TAG_DURATION, metadata->duration, NULL);
            } else if (strcmp(value_name, "filesize") == 0) {
                metadata->file_size = (gint)double_value;
            } else if (strcmp(value_name, "videocodecid") == 0) {
                metadata->video_codec_id = (gint)double_value;
            } else if (strcmp(value_name, "videodatarate") == 0) {
                metadata->video_data_rate = double_value;
            } else if (strcmp(value_name, "width") == 0) {
                metadata->width = (gint)double_value;
            } else if (strcmp(value_name, "height") == 0) {
                metadata->height = (gint)double_value;
            } else if (strcmp(value_name, "AspectRatioX") == 0) {
                metadata->par_x = (gint)double_value;
            } else if (strcmp(value_name, "AspectRatioY") == 0) {
                metadata->par_y = (gint)double_value;
            } else if (strcmp(value_name, "framerate") == 0) {
                metadata->framerate = double_value;
            } else if (strcmp(value_name, "audiocodecid") == 0) {
                metadata->audio_codec_id = (gint)double_value;
            } else if (strcmp(value_name, "audiosamplesize") == 0) {
                metadata->audio_sample_size = (gint)double_value;
            }
            break;

        case FLV_SCRIPT_DATA_TYPE_BOOL:
            result = flv_script_data_read_ui8(reader, &boolean_value);
            if (!result)
                break;

            if (strcmp(value_name, "canSeekToEnd") == 0) {
                metadata->can_seek_to_end = boolean_value;
            } else if (strcmp(value_name, "stereo") == 0) {
                metadata->is_stereo = boolean_value;
            }
            break;

        case FLV_SCRIPT_DATA_TYPE_STRING:
        case FLV_SCRIPT_DATA_TYPE_LONG_STRING:
            result = flv_script_data_read_string(reader, &string_value, value_type == FLV_SCRIPT_DATA_TYPE_LONG_STRING);
            if (!result)
                break;
            // Register the tag if needed so it shows up
            if (!gst_tag_exists(value_name)) {
                gst_tag_register(value_name, GST_TAG_FLAG_META, G_TYPE_STRING, value_name, "FLV Metadata Tag", NULL);
            }
            gst_tag_list_add(metadata->tag_list, GST_TAG_MERGE_REPLACE, value_name, string_value, NULL);
            g_free(string_value);
            break;

        case FLV_SCRIPT_DATA_TYPE_OBJECT:
                // Only read the keyframe metadata once
            if ((strcmp(value_name, "keyframes") == 0) && (metadata->keyframes == NULL)) {
                result &= flv_script_data_read_object(reader, flv_metadata_keyframe_handler, param);
            } else {
                result &= flv_script_data_read_object(reader, flv_metadata_skip_handler, param);
            }
            break;

        case FLV_SCRIPT_DATA_TYPE_MOVIE_CLIP:
            // Per AMF version 0: "This type is not supported and is reserved for future use."
            result = FALSE;
            break;

        case FLV_SCRIPT_DATA_TYPE_NULL:
        case FLV_SCRIPT_DATA_TYPE_UNDEFINED:
            // Nothing follows
            break;

        case FLV_SCRIPT_DATA_TYPE_REFERENCE:
            // U16, two byte index follows
            // this is just an index to some other object in this script tag, we can safely ignore this
            reader->position += 2;
            break;

        case FLV_SCRIPT_DATA_TYPE_ECMA:
            result &= flv_script_data_read_ecma(reader, flv_metadata_skip_handler, param);
            break;

        case FLV_SCRIPT_DATA_TYPE_TERMINATOR:
            /* Just return true for ECMA terminator */
            break;

        case FLV_SCRIPT_DATA_TYPE_STRICT:
            result &= flv_script_data_read_strict_array(reader, flv_metadata_skip_handler, param);
            break;

        case FLV_SCRIPT_DATA_TYPE_DATE:
            // Skip date value (64 bits milliseconds + 16 bits time zone == 80 bits == 10 bytes)
            reader->position += 10;
            break;

        default:
            /* Non-handled type */
            result = FALSE;
            break;
    }
    return result;
}

static gboolean
flv_metadata_skip_handler(FlvScriptDataReader* reader,
        gchar* value_name, gint value_type, void* param)
{
    gdouble double_value;
    guint8 boolean_value;
    gchar* string_value;
    gboolean result = TRUE;

    switch (value_type) {
        case FLV_SCRIPT_DATA_TYPE_DOUBLE:
            result = flv_script_data_read_double(reader, &double_value);
            break;

        case FLV_SCRIPT_DATA_TYPE_BOOL:
            result = flv_script_data_read_ui8(reader, &boolean_value);
            break;

        case FLV_SCRIPT_DATA_TYPE_STRING:
            result = flv_script_data_read_string(reader, &string_value, FALSE);
            if (result)
                g_free(string_value);
            break;

        case FLV_SCRIPT_DATA_TYPE_OBJECT:
            result &= flv_script_data_read_object(reader, flv_metadata_skip_handler, param);
            break;

        case FLV_SCRIPT_DATA_TYPE_ECMA:
            result &= flv_script_data_read_ecma(reader, flv_metadata_skip_handler, param);
            break;

        case FLV_SCRIPT_DATA_TYPE_TERMINATOR:
            /* Just return true for terminator */
            break;

        case FLV_SCRIPT_DATA_TYPE_STRICT:
            result &= flv_script_data_read_strict_array(reader, flv_metadata_skip_handler, param);
            break;

        default:
            /* Non-handled type */
            return FALSE;
    }
    return result;
}
