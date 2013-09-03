/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 
#define _GNU_SOURCE
#include <fcntl.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>

#include "os.h"
#include "iolib.h"

#define TRACEFNAME "gl.trace"
#define CHUNKSZ 0x100000
#define MARKER 0xdeadbead

static int ioMode;
static int wsize = 0;
static char *fileName = NULL;
static char *filePtr = MAP_FAILED;
static char *endPtr;
static char *curPtr;
static size_t fileSize;
static off_t fileOffset;

void
iolib_init(int mode, const char *fname)
{
    fileName = strdup(fname ? fname : TRACEFNAME);
    ioMode = mode;
    
    if (ioMode == IO_WRITE) {
        int fd = open(fileName, O_RDWR|O_CREAT|O_TRUNC,
                  S_IRUSR|S_IWUSR|S_IRGRP|S_IWGRP|S_IROTH);
        if (fd < 0) {
            fprintf(stderr, "FATAL: can't create file %s\n", fileName);
            exit(1);
        }             
        close(fd);
        curPtr = endPtr = NULL;
        fileOffset = (off_t)0;
        fileSize = (size_t)0;
        
        putInt(OPC_VERSION);
        putInt(VERSION_MAJOR);
        putInt(VERSION_MINOR);
        putInt(VERSION_REV);
        putInt(sizeof(void*));
    }
    else if (ioMode == IO_READ) {
        struct stat sb;

        int fd = open(fileName, O_RDONLY);
        if (fstat(fd, &sb) == -1) {
            fprintf(stderr, "FATAL: can't open file %s\n", fileName);
            exit(1);
        }
        fileSize = sb.st_size;
        filePtr = (char*)mmap(NULL, fileSize, PROT_READ, MAP_POPULATE | MAP_PRIVATE, fd, (off_t)0);
        close(fd);
        if (filePtr == MAP_FAILED) {
            fprintf(stderr, "FATAL: can't mmap file %s\n", fileName);
            exit(1);
        }             
        curPtr = filePtr;
        endPtr = filePtr + fileSize;

        int version = getInt();
        if (version != OPC_VERSION) {
            fprintf(stderr, "ERROR: not a trace: %s\n", fileName);
            exit(1);
        }
        int major = getInt();
        int minor = getInt();
        int rev   = getInt();
        if (major != VERSION_MAJOR || minor > VERSION_MINOR) {
            fprintf(stderr, "ERROR: version mismatch: current %d.%d.%d, trace %d.%d.%d (%s)\n",
                    VERSION_MAJOR, VERSION_MINOR, VERSION_REV,
                    major, minor, rev, fileName);
        }
        wsize = getInt();
    }
}

void
iolib_fini()
{
    if (fileSize > 0) {
        munmap(filePtr, fileSize);
    }
    if (ioMode == IO_WRITE && curPtr < endPtr) {
        truncate(fileName, fileOffset - (endPtr - curPtr));
    }
    free(fileName);
}

/*
 *    Write
 */

static void
enlarge()
{
    if (fileSize > 0) {
        munmap(filePtr, fileSize);
    }
    
    int fd = open(fileName, O_RDWR);
    if (fd < 0) {
        fprintf(stderr, "FATAL: can't create file %s\n", fileName);
        exit(1);
    }
    char zero = (char)0;           
    int n = pwrite(fd, &zero, sizeof(zero), fileOffset + CHUNKSZ - sizeof(zero));
    if (n != sizeof(zero)) {
        fprintf(stderr, "FATAL: can't allocate file %s\n", fileName);
        exit(1);
    }
    filePtr = (char*)mmap(NULL, CHUNKSZ, PROT_READ|PROT_WRITE, MAP_SHARED, fd, fileOffset);
    close(fd);
    if (filePtr == MAP_FAILED) {
        fprintf(stderr, "FATAL: can't mmap file %s\n", fileName);
        exit(1);
    }
    fileSize = CHUNKSZ;
    fileOffset += CHUNKSZ;
    curPtr = filePtr;
    endPtr = filePtr + CHUNKSZ;
}

static pthread_mutex_t memlock = PTHREAD_RECURSIVE_MUTEX_INITIALIZER_NP;
static pthread_t curThread = (pthread_t)0;
static int reentrance = 0;

void
putCmd(int cmd)
{
    pthread_mutex_lock(&memlock);
    if (++reentrance > 1) return;
    
    pthread_t thr = pthread_self();
    if (thr != curThread) {
        putInt(OPC_THREAD);
        putPtr((void*)thr);
        curThread = thr;
    }
    
    putInt(cmd);
}

void
putInt(int arg)
{
    if (reentrance > 1) return;
    if (curPtr >= endPtr) enlarge();
    *(int*)curPtr = arg;
    curPtr += sizeof(int);
}

void
putIntPtr(const int *arg)
{
    if (reentrance > 1) return;
    if (curPtr >= endPtr) enlarge();
    int val = 0;
    if (arg == NULL || *arg == MARKER) {
        *(int*)curPtr = MARKER;
        curPtr += sizeof(int);
        if (curPtr >= endPtr) enlarge();
        if (arg) val = MARKER;
    }
    else {
        val = *arg;
    }
    *(int*)curPtr = val;
    curPtr += sizeof(int);
}

void
putFloat(float arg)
{
    if (reentrance > 1) return;
    if (curPtr >= endPtr) enlarge();
    *(float*)curPtr = arg;
    curPtr += sizeof(float);
}

void
putFloatPtr(const float *arg)
{
    if (reentrance > 1) return;
    if (curPtr >= endPtr) enlarge();
    float val = 0.;
    if (arg == NULL || *arg == (float)MARKER) {
        *(float*)curPtr = (float)MARKER;
        curPtr += sizeof(float);
        if (curPtr >= endPtr) enlarge();
        if (arg) val = (float)MARKER;
    }
    else {
        val = *arg;
    }
    *(float*)curPtr = val;
    curPtr += sizeof(float);
}

void
putLongLong(long long arg)
{
    if (reentrance > 1) return;
    int left = sizeof(arg);
    char *s = (char*)&arg;
    while (left > 0) {
        int avail = endPtr - curPtr;
        if (left <= avail) {
            while (left--) *curPtr++ = *s++;
        }
        else {
            left -= avail;
            while (avail--) *curPtr++ = *s++;
            enlarge();
        }
    }
}

void
putPtr(const void *arg)
{
    if (reentrance > 1) return;
    int left = sizeof(arg);
    char *s = (char*)&arg;
    while (left > 0) {
        int avail = endPtr - curPtr;
        if (left <= avail) {
            while (left--) *curPtr++ = *s++;
        }
        else {
            left -= avail;
            while (avail--) *curPtr++ = *s++;
            enlarge();
        }
    }
}

void
putString(const char *str)
{
    if (reentrance > 1) return;
    if (str == NULL) {
        putInt(0);
        return;
    }
    
    int left = strlen(str) + 1;
    while (left > 0) {
        int avail = endPtr - curPtr;
        if (left <= avail) {
            while (left--) *curPtr++ = *str++;
        }
        else {
            left -= avail;
            while (avail--) *curPtr++ = *str++;
            enlarge();
        }
    }
    while ((long)curPtr & (sizeof(int)-1)) *curPtr++ = (char)0; 
}

void
putBytes(const void *data, int size)
{
    if (reentrance > 1) return;
    int left = size;
    if (data == NULL) left = 0;
    char *src = (char*)data;
    putInt(left);
    while (left > 0) {
        int avail = endPtr - curPtr;
        if (left <= avail) {
            while (left--) *curPtr++ = *src++;
        }
        else {
            left -= avail;
            while (avail--) *curPtr++ = *src++;
            enlarge();
        }
    }
    while ((long)curPtr & (sizeof(int)-1)) *curPtr++ = (char)0; 
}

void
putTime(uint64_t bgn, uint64_t end)
{
    putLongLong(bgn);
    putLongLong(end);
    endCmd();
}

void
endCmd()
{
    --reentrance;
    pthread_mutex_unlock(&memlock);
}

/*
 *    Read
 */

int
getCmd()
{
    if (curPtr + sizeof(int) > endPtr) return OPC_EOF;
    int cmd = *(int*)curPtr;
    curPtr += sizeof(int);
    return cmd;
}

int
getInt()
{
    if (curPtr + sizeof(int) > endPtr) return 0;
    int val = *(int*)curPtr;
    curPtr += sizeof(int);
    return val;
}

const int *
getIntPtr()
{
    if (curPtr + sizeof(int) > endPtr) return 0;
    const int *res = (int*)curPtr;
    curPtr += sizeof(int);
    int val = *res;
    if (val == MARKER) {
        res = (int*)curPtr;
        curPtr += sizeof(int);
        val = *res;
        if (val == 0) return NULL;
    }
    return res;
}

float
getFloat()
{
    if (curPtr + sizeof(float) > endPtr) return 0.;
    float val = *(float*)curPtr;
    curPtr += sizeof(float);
    return val;
}

const float *
getFloatPtr()
{
    if (curPtr + sizeof(int) > endPtr) return 0;
    const float *res = (float*)curPtr;
    curPtr += sizeof(float);
    float val = *res;
    if (val == (float)MARKER) {
        res = (float*)curPtr;
        val = *res;
        if (val == 0.) return NULL;
    }
    return res;
}

long long
getLongLong()
{
    if (curPtr + sizeof(long long) > endPtr) return 0L;
    long long val = *(long long*)curPtr;
    curPtr += sizeof(long long);
    return val;
}

uint64_t
getPtr()
{
    if (curPtr + wsize > endPtr) return (uint64_t)0;
    uint64_t val = wsize == 4 ? *(int32_t*)curPtr : *(int64_t*)curPtr;
    curPtr += wsize;
    return val;
}

const char      *
getString()
{
    int len = strlen(curPtr) + 1;
    if (curPtr + len > endPtr) return NULL;
    len += sizeof(int) - 1;
    len -= len % sizeof(int);
    char *str = curPtr;
    curPtr += len;
    return str;
}

const void      *
getBytes()
{
    if (curPtr + sizeof(int) > endPtr) return NULL;
    int size = *(int*)curPtr;
    curPtr += sizeof(int);

    if (size <= 0 || curPtr + size > endPtr) return NULL;
    void *data = curPtr;
    size += sizeof(int) - 1;
    size -= size % sizeof(int);
    curPtr += size;
    return data;
}

void
getTime(uint64_t *bgn, uint64_t *end)
{
    *bgn = getLongLong();
    *end = getLongLong();
}
