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

#include <cache.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>

#define DEFAULT_BUFFER_SIZE 4096
static const char *tempDir = NULL;

struct _Cache
{
    char*   filename;
    int     readHandle;
    int     writeHandle;

    gint64  read_position;
    gint64  write_position;
};

void cache_static_init(void)
{
    tempDir = g_get_tmp_dir();
}

Cache* create_cache()
{
    Cache* result= (Cache*)g_try_malloc(sizeof(Cache));
    if (result)
    {
        result->filename = g_build_filename(tempDir, "jfxmpbXXXXXX", NULL);
        if (result->filename == NULL)
            goto _error_exit;
        else
        {
        result->writeHandle = g_mkstemp_full(result->filename, O_RDWR, S_IRUSR|S_IWUSR);
            result->readHandle = open(result->filename, O_RDONLY, 0);

            if(result->writeHandle < 0 || result->readHandle < 0)
                goto _error_exit;

        if (unlink(result->filename) < 0)
        {
            close (result->writeHandle);
            close (result->readHandle);
        goto _error_exit;
        }

            result->read_position = result->write_position = 0;
        }
    }
    return result;

_error_exit:
    g_free(result);
    return NULL;
}

void destroy_cache(Cache* instance)
{
    close(instance->writeHandle);
    close(instance->readHandle);
    g_free(instance->filename);

    g_free(instance);
}

void cache_write_buffer(Cache* cache, GstBuffer* buffer)
{
    ssize_t written = write(cache->writeHandle, GST_BUFFER_DATA(buffer), GST_BUFFER_SIZE(buffer));
    if (written > 0)
        cache->write_position += written;
}

gint64 cache_read_buffer(Cache* cache, GstBuffer** buffer)
{
    guint8 *data = (guint8*)g_try_malloc(DEFAULT_BUFFER_SIZE);
    *buffer = NULL;

    if (data)
    {
        ssize_t size = 0;
        if ((cache->write_position - cache->read_position) > 0 &&
            (cache->write_position - cache->read_position) < DEFAULT_BUFFER_SIZE)
            size = cache->write_position - cache->read_position;
        else
            size = DEFAULT_BUFFER_SIZE;

        ssize_t read_bytes = read(cache->readHandle, data, size);
        if (read_bytes > 0)
        {
            *buffer = gst_buffer_new ();
            GST_BUFFER_SIZE(*buffer) = read_bytes;
            GST_BUFFER_OFFSET(*buffer) = cache->read_position;
            GST_BUFFER_MALLOCDATA(*buffer) = data;
            GST_BUFFER_DATA(*buffer) = GST_BUFFER_MALLOCDATA(*buffer);

            cache->read_position += read_bytes;
            return cache->read_position;
        }
        else // read error, deleting buffer to avoid leaking.
            g_free(data);
    }

    return 0;
}

GstFlowReturn cache_read_buffer_from_position(Cache* cache, gint64 start_position, guint size, GstBuffer** buffer)
{
    GstFlowReturn result = GST_FLOW_ERROR;
    *buffer = NULL;

    if (cache_set_read_position(cache, start_position))
    {
        guint8 *data = (guint8*)g_try_malloc(size);
        if (data)
    {
        ssize_t read_bytes = read(cache->readHandle, data, size);
            if (read_bytes == size)
            {
                *buffer = gst_buffer_new ();
                GST_BUFFER_SIZE(*buffer) = read_bytes;
                GST_BUFFER_OFFSET(*buffer) = cache->read_position;
                GST_BUFFER_MALLOCDATA(*buffer) = data;
                GST_BUFFER_DATA(*buffer) = GST_BUFFER_MALLOCDATA(*buffer);
                result = GST_FLOW_OK;
            }
            else
            g_free(data); // Wrong size, deleting buffer to avoid leaking.

            cache->read_position += read_bytes;
    }
    }
    return result;
}

static inline gboolean cache_set_handler_position(int handle, guint64 position)
{
    return lseek(handle, position, SEEK_SET) >= 0;
}

gboolean cache_set_write_position(Cache* cache, gint64 position)
{
    gboolean result = (position == cache->write_position);
    if (!result)
    {
        result = cache_set_handler_position(cache->writeHandle, position);
        if (result)
            cache->write_position = position;
    }
    return result;
}

gboolean cache_set_read_position(Cache* cache, gint64 position)
{
    gboolean result = (position == cache->read_position);
    if (!result)
    {
        result = cache_set_handler_position(cache->readHandle, position);
        if (result)
            cache->read_position = position;
    }
    return result;
}

gboolean cache_has_enough_data(Cache* cache)
{
    return cache->read_position < cache->write_position;
}
