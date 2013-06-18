/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#ifndef __UNIX_DEPLOY_PLATFORM__
#define __UNIX_DEPLOY_PLATFORM__

/** Provide an abstraction for difference in the platform APIs,
     e.g. string manipulation functions, etc. */
#include<stdio.h>
#include<string.h>
#include<strings.h>
#include<sys/stat.h>

#define TCHAR char

#define _T(x) x

#define DEPLOY_MULTIBYTE_SNPRINTF snprintf

#define DEPLOY_SNPRINTF(buffer, sizeOfBuffer, count, format, ...) \
    snprintf((buffer), (count), (format), __VA_ARGS__)

#define DEPLOY_PRINTF(format, ...) \
    printf((format), ##__VA_ARGS__)

#define DEPLOY_FPRINTF(dest, format, ...) \
    fprintf((dest), (format), __VA_ARGS__)

#define DEPLOY_SSCANF(buf, format, ...) \
    sscanf((buf), (format), __VA_ARGS__)

#define DEPLOY_STRDUP(strSource) \
    strdup((strSource))

//return "error code" (like on Windows)
static int DEPLOY_STRNCPY(char *strDest, size_t numberOfElements, const char *strSource, size_t count) {
    char *s = strncpy(strDest, strSource, count);
    // Duplicate behavior of the Windows' _tcsncpy_s() by adding a NULL
    // terminator at the end of the string.
    if (count < numberOfElements) {
        s[count] = '\0';
    } else {
        s[numberOfElements - 1] = '\0';
    }
    return (s == strDest) ? 0 : 1;
}

static int DEPLOY_STRNCAT(char *strDest, size_t numberOfElements, const char *strSource, size_t count) {
    // strncat always return null terminated string
    char *s = strncat(strDest, strSource, count);
    return (s == strDest) ? 0 : 1;
}

#define DEPLOY_STRICMP(x, y) \
    strcasecmp((x), (y))

#define DEPLOY_STRNICMP(x, y, cnt) \
    strncasecmp((x), (y), (cnt))

#define DEPLOY_STRNCMP(x, y, cnt) \
    strncmp((x), (y), (cnt))

#define DEPLOY_STRLEN(x) \
    strlen((x))

#define DEPLOY_STRSTR(x, y) \
    strstr((x), (y))

#define DEPLOY_STRCHR(x, y) \
    strchr((x), (y))

#define DEPLOY_STRRCHR(x, y) \
    strrchr((x), (y))

#define DEPLOY_STRPBRK(x, y) \
    strpbrk((x), (y))

#define DEPLOY_GETENV(x) \
    getenv((x))

#define DEPLOY_PUTENV(x) \
    putenv((x))

#define DEPLOY_STRCMP(x, y) \
    strcmp((x), (y))

#define DEPLOY_STRCPY(x, y) \
    strcpy((x), (y))

#define DEPLOY_STRCAT(x, y) \
    strcat((x), (y))

#define DEPLOY_ATOI(x) \
    atoi((x))

static int getFileSize(TCHAR* filename) {
    struct stat statBuf;
    if (stat(filename, &statBuf) == 0) {
        return statBuf.st_size;
    }
    return -1;
}

#define DEPLOY_FILE_SIZE(filename) getFileSize(filename)

#define DEPLOY_FOPEN(x, y) \
    fopen((x), (y))

#define DEPLOY_FGETS(x, y, z) \
    fgets((x), (y), (z))

#define DEPLOY_REMOVE(x) \
    remove((x))

#define DEPLOY_SPAWNV(mode, cmd, args) \
    spawnv((mode), (cmd), (args))

#define DEPLOY_ISDIGIT(ch) isdigit(ch)

// for non-unicode, just return the input string for
// the following 2 conversions
#define DEPLOY_NEW_MULTIBYTE(message) message

#define DEPLOY_NEW_FROM_MULTIBYTE(message) message

// for non-unicode, no-op for the relase operation
// since there is no memory allocated for the
// string conversions
#define DEPLOY_RELEASE_MULTIBYTE(tmpMBCS)

#define DEPLOY_RELEASE_FROM_MULTIBYTE(tmpMBCS)

// The size will be used for converting from 1 byte to 1 byte encoding.
// Ensure have space for zero-terminator.
#define DEPLOY_GET_SIZE_FOR_ENCODING(message, theLength) (theLength + 1)

#endif
