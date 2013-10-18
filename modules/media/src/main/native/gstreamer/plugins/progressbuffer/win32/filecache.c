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
#include <windows.h>

#define DEFAULT_BUFFER_SIZE 4096
static char tempDir[MAX_PATH];

struct _Cache
{
    char    filename[MAX_PATH];
    HANDLE  readHandle;
    HANDLE  writeHandle;

    gint64  read_position;
    gint64  write_position;
};

void cache_static_init(void)
{
    DWORD   dwRetVal = GetTempPath(MAX_PATH, tempDir);
    if ((dwRetVal >= MAX_PATH) || (dwRetVal == 0))
    {
        GST_WARNING("GetTempPath failed");
        g_strlcpy(tempDir, ".", MAX_PATH);
    }
}

Cache* create_cache()
{
    Cache* result= (Cache*)g_try_malloc(sizeof(Cache));
    if (result)
    {
        UINT uRetVal = GetTempFileName(tempDir, "jfx", 0, result->filename);
        if (uRetVal == 0)
            goto _error_exit;
        else
        {
            result->writeHandle = CreateFile(result->filename, GENERIC_READ | GENERIC_WRITE, FILE_SHARE_READ|FILE_SHARE_DELETE, NULL,
                                             CREATE_ALWAYS, FILE_ATTRIBUTE_TEMPORARY|FILE_FLAG_DELETE_ON_CLOSE, NULL);
            result->readHandle = CreateFile(result->filename, GENERIC_READ, FILE_SHARE_READ | FILE_SHARE_WRITE |FILE_SHARE_DELETE, NULL,
                                            OPEN_EXISTING, FILE_ATTRIBUTE_TEMPORARY|FILE_FLAG_DELETE_ON_CLOSE, NULL);
            if(result->writeHandle == INVALID_HANDLE_VALUE || result->readHandle == INVALID_HANDLE_VALUE)
                goto _error_exit;

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
    CloseHandle(instance->writeHandle);
    CloseHandle(instance->readHandle);

    g_free(instance);
}

void cache_write_buffer(Cache* cache, GstBuffer* buffer)
{
    DWORD written = 0;
    if (WriteFile(cache->writeHandle, GST_BUFFER_DATA(buffer), GST_BUFFER_SIZE(buffer), &written, NULL))
        cache->write_position += written;
}

gint64 cache_read_buffer(Cache* cache, GstBuffer** buffer)
{
    DWORD read = 0;
    DWORD size = 0;
    guint8 *data = (guint8*)g_try_malloc(DEFAULT_BUFFER_SIZE);
    *buffer = NULL;

    if ((cache->write_position - cache->read_position) > 0 && (cache->write_position - cache->read_position) < DEFAULT_BUFFER_SIZE)
        size = cache->write_position - cache->read_position;
    else
        size = DEFAULT_BUFFER_SIZE;

    if (data && ReadFile(cache->readHandle, data, size, &read, NULL))
    {
        *buffer = gst_buffer_new ();
        GST_BUFFER_SIZE(*buffer) = read;
        GST_BUFFER_OFFSET(*buffer) = cache->read_position;
        GST_BUFFER_MALLOCDATA(*buffer) = data;
        GST_BUFFER_DATA(*buffer) = GST_BUFFER_MALLOCDATA(*buffer);

        cache->read_position += read;
        return cache->read_position;
    }
    else if (data) // ReadError, deleting buffer to avoid leaking.
        g_free(data);

    return 0;
}

GstFlowReturn cache_read_buffer_from_position(Cache* cache, gint64 start_position, guint size, GstBuffer** buffer)
{
    GstFlowReturn result = GST_FLOW_ERROR;
    *buffer = NULL;

    if (cache_set_read_position(cache, start_position))
    {
        DWORD  read = 0;
        guint8 *data = (guint8*)g_try_malloc(size);
        if (data && ReadFile(cache->readHandle, data, size, &read, NULL))
        {
            if (read == size)
            {
                *buffer = gst_buffer_new ();
                GST_BUFFER_SIZE(*buffer) = read;
                GST_BUFFER_OFFSET(*buffer) = cache->read_position;
                GST_BUFFER_MALLOCDATA(*buffer) = data;
                GST_BUFFER_DATA(*buffer) = GST_BUFFER_MALLOCDATA(*buffer);
                result = GST_FLOW_OK;
            }
            else
                g_free(data); // Wrong size, deleting buffer to avoid leaking.

            cache->read_position += read;
        }
        else if (data) // ReadError, deleting buffer to avoid leaking.
            g_free(data);
    }
    return result;
}

static gboolean cache_set_handler_position(HANDLE handle, guint64 position)
{
    LARGE_INTEGER li;
    li.QuadPart = position;
    li.LowPart = SetFilePointer (handle, li.LowPart, &li.HighPart, FILE_BEGIN);

    return (li.LowPart != INVALID_SET_FILE_POINTER || GetLastError() == NO_ERROR);
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
