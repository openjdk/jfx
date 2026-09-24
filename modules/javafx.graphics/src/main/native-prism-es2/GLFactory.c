/*
 * Copyright (c) 2012, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include <stdlib.h>
#include <assert.h>
#include <stdio.h>
#include <string.h>
#include <math.h>

#include "PrismES2Defs.h"

/*
 * Extract the version numbers from a copy of the version string.
 * Upon return, numbers[0] contains major version number
 * numbers[1] contains minor version number
 * Note that the passed in version string is modified.
 */
void extractVersionInfo(char *versionStr, int *numbers)
{
    char *majorNumStr;
    char *minorNumStr;
    char *next_token = NULL;

    if ((versionStr == NULL) || (numbers == NULL)) {
        return;
    }

    numbers[0] = numbers[1] = -1;
#if WIN32
    majorNumStr = strtok_s(versionStr, (char *)".", &next_token);
    minorNumStr = strtok_s(0, (char *)".", &next_token);
#else
    majorNumStr = strtok(versionStr, (char *)".");
    minorNumStr = strtok(0, (char *)".");
#endif
    if (majorNumStr != NULL)
        numbers[0] = atoi(majorNumStr);
    if (minorNumStr != NULL)
        numbers[1] = atoi(minorNumStr);
}

/*
 * check if the extension is supported
 */
int isExtensionSupported(const char *allExtensions, const char *extension)
{
    const char *start;
    const char *where, *terminator;

    if ((allExtensions == NULL) || (extension == NULL)) {
        return 0;
    }

    /* Extension names should not have spaces. */
    where = (const char *) strchr(extension, ' ');
    if (where || *extension == '\0')
        return 0;

    /*
     * It takes a bit of care to be fool-proof about parsing the
     * OpenGL extensions string. Don't be fooled by sub-strings,
     * etc.
     */
    start = allExtensions;
    for (;;) {
        where = (const char *) strstr((const char *) start, extension);
        if (!where)
            break;
        terminator = where + strlen(extension);
        if (where == start || *(where - 1) == ' ')
            if (*terminator == ' ' || *terminator == '\0')
                return 1;
        start = terminator;
    }
    return 0;
}

