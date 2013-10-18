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

#ifndef _LOCATOR_STREAM_H_
#define _LOCATOR_STREAM_H_

#include "Locator.h"
#include <stdint.h>

class CStreamCallbacks
{
public:
    /* NeedBuffer returns true if the pipeline needs progressbuffer, false otherwise.
    * This can be detected by analysing url schemes.
    */
    virtual bool NeedBuffer() = 0;

    /* ReadNextBlock reads next available block of data and
     * returns the number of bytes actually have been read. The number may differ
     * from the size of the allocated buffer.
     * -1 must be returned if we encounter EndOfStream
     * -2 must be returned if there was an exception.
     */
    virtual int  ReadNextBlock() = 0;

    /* ReadBlock reads arbitrary block of data and
     * returns the number of bytes actually have been read. The number may differ
     * from the size passed in.
     * -1 must be returned if we encounter EndOfStream
     * -2 must be returned if there was an exception.
     */
    virtual int  ReadBlock(int64_t position, int size) = 0;

    /* CopyBlock copies the datra from whatever internal buffer to the destination.*/
    virtual void CopyBlock(void* destination, int size) = 0;

    /* Detects whether the source is seekable.*/
    virtual bool IsSeekable() = 0;

    /* Detects whether the source is a random access source.*/
    virtual bool IsRandomAccess() = 0;

    /* Seek performs seeking to the specified position. Next ReadBlock/CopyBlock call must
    * return buffers from the new position*/
    virtual int64_t Seek(int64_t position) = 0;

    /* CloseConnection closes all connections and cleans references. */
    virtual void CloseConnection() = 0;

    /* Get or set properties. Value parameter and return value depends on prop value. */
    virtual int Property(int prop, int value) = 0;

    /* Get stream size. */
    virtual int GetStreamSize() = 0;

    /* Virtual destructor */
    virtual ~CStreamCallbacks() {}
};

class CLocatorStream : public CLocator
{
public:
    CLocatorStream(CStreamCallbacks *callbacks, const char* contentType, const char* location, int64_t llSizeHint);

    inline CStreamCallbacks* GetCallbacks() { return m_pStreamCallbacks; }

protected:
    CStreamCallbacks *m_pStreamCallbacks;
};

#endif // _LOCATOR_STREAM_H_
