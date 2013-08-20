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
 
/*
 *    Object mapping. A very simple implementation.
 */

#include <stdio.h>
#include <stdlib.h>

#include "map.h"

#define MAXMAPS         16
#define MAXKEYS         16

static void *mapSpace[MAXMAPS*MAXKEYS*2];
static int lastMap = -1;

void    *
createMap()
{
    if (++lastMap >= MAXMAPS) {
        fprintf(stderr, "FATAL: too many object maps\n");
        exit(1);
    }
    return mapSpace + lastMap * MAXKEYS * 2;
}

void
putMap(void *map, void *key, void *val)
{
    void **ptr = (void**)map;
    int i;
    for (i=0; i<MAXKEYS; ++i) {
        if (ptr[2*i] == NULL) {
            ptr[2*i] = key;
            ptr[2*i+1] = val;
            return;
        }
        else if (ptr[2*i] == key) {
            ptr[2*i+1] = val;
            return;
        }
    }
    fprintf(stderr, "FATAL: too many object map keys\n");
    exit(1);
}

void    *
getMap(void *map, void *key)
{
    void **ptr = (void**)map;
    int i;
    for (i=0; i<MAXKEYS; ++i) {
        if (ptr[2*i] == NULL) {
            return NULL;
        }
        else if (ptr[2*i] == key) {
            return ptr[2*i+1];
        }
    }
    return NULL;
}

