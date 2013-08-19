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
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>

#include "iolib.h"

#define TRACEFNAME "ogl.trace"
#define CHUNKSZ 0x100000

static int ioMode;
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
}

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

void
putCmd(int cmd)
{
    if (curPtr >= endPtr) enlarge();
    *(int*)curPtr = cmd;
    curPtr += sizeof(int);
}

void
putInt(int arg)
{
    if (curPtr >= endPtr) enlarge();
    *(int*)curPtr = arg;
    curPtr += sizeof(int);
}

void
putFloat(float arg)
{
    if (curPtr >= endPtr) enlarge();
    *(float*)curPtr = arg;
    curPtr += sizeof(float);
}

void
putLongLong(long long arg)
{
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
}

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

float
getFloat()
{
    if (curPtr + sizeof(float) > endPtr) return 0.;
    float val = *(float*)curPtr;
    curPtr += sizeof(float);
    return val;
}

long long
getLongLong()
{
    if (curPtr + sizeof(long long) > endPtr) return 0L;
    long long val = *(long long*)curPtr;
    curPtr += sizeof(long long);
    return val;
}

const void      *
getPtr()
{
    if (curPtr + sizeof(void*) > endPtr) return NULL;
    void *val = *(void**)curPtr;
    curPtr += sizeof(void*);
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
