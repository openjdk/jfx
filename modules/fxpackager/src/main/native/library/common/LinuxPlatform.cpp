/*
 * Copyright (c) 2014, Oracle and/or its affiliates.
 * All rights reserved. Use is subject to license terms.
 *
 * This file is available and licensed under the following license:
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the distribution.
 *  - Neither the name of Oracle Corporation nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


#include "Platform.h"

#ifdef LINUX

#include "LinuxPlatform.h"
#include "PlatformString.h"


#include <stdlib.h>
#include <pwd.h>


TString GetEnv(const TString &name) {
    TString result;

    char *value = ::getenv((TCHAR*)name.c_str());

    if (value != NULL) {
       result = value;
    }

    return result;
}

LinuxPlatform::LinuxPlatform(void) : Platform(), GenericPlatform(), PosixPlatform() {
    FMainThread = pthread_self();
}

LinuxPlatform::~LinuxPlatform(void) {
}

void LinuxPlatform::ShowMessage(TString title, TString description) {
    printf("%s %s\n", PlatformString(title).toPlatformString(), PlatformString(description).toPlatformString());
    fflush(stdout);
}

void LinuxPlatform::ShowMessage(TString description) {
    TString appname = GetModuleFileName();
    appname = FilePath::ExtractFileName(appname);
    ShowMessage(PlatformString(appname).toPlatformString(), PlatformString(description).toPlatformString());
}

TCHAR* LinuxPlatform::ConvertStringToFileSystemString(TCHAR* Source, bool &release) {
    // Not Implemented.
    return NULL;
}

TCHAR* LinuxPlatform::ConvertFileSystemStringToString(TCHAR* Source, bool &release) {
    // Not Implemented.
    return NULL;
}

TString LinuxPlatform::GetModuleFileName() {
    TString result;
    DynamicBuffer<TCHAR> buffer(MAX_PATH);

    if (readlink("/proc/self/exe", buffer.GetData(), MAX_PATH - 1) != -1) {
        result = buffer.GetData();
    }

    return result;
}

void LinuxPlatform::SetCurrentDirectory(TString Value) {
    chdir(PlatformString(Value).toPlatformString());
}

TString LinuxPlatform::GetPackageRootDirectory() {
    TString filename = GetModuleFileName();
    return FilePath::ExtractFilePath(filename);
}

TString LinuxPlatform::GetAppDataDirectory() {
    TString result;
    TString home = GetEnv(_T("HOME"));

    if (home.empty() == false) {
        result += FilePath::IncludeTrailingSeparater(home) + _T(".local");
    }

    return result;
}

ISectionalPropertyContainer* LinuxPlatform::GetConfigFile(TString FileName) {
    IniFile *result = new IniFile();

    if (result->LoadFromFile(FileName) == false) {
        // New property file format was not found, attempt to load old property file format.
        Helpers::LoadOldConfigFile(FileName, result);
    }

    return result;
}

TString LinuxPlatform::GetBundledJVMLibraryFileName(TString RuntimePath) {
    TString result = FilePath::IncludeTrailingSeparater(RuntimePath) +
        "jre/lib/"JAVAARCH"/client/libjvm.so";

    if (FilePath::FileExists(result) == false) {
        result = FilePath::IncludeTrailingSeparater(RuntimePath) +
            "jre/lib/"JAVAARCH"/server/libjvm.so";
    }

    if (FilePath::FileExists(result) == false) {
        result = FilePath::IncludeTrailingSeparater(RuntimePath) +
            "lib/"JAVAARCH"/server/libjvm.so";
    }

    if (FilePath::FileExists(result) == false) {
        result = FilePath::IncludeTrailingSeparater(RuntimePath) +
            "lib/"JAVAARCH"/server/libjvm.so";
    }

    return result;
}

TString LinuxPlatform::GetSystemJRE() {
    if (GetAppCDSState() == cdsOn || GetAppCDSState() == cdsGenCache) {
        //TODO throw exception
        return _T("");
    }

    TString result;
    TString jreHome = GetEnv("JRE_HOME");

    if (jreHome.empty() == false) {
        result = FilePath::IncludeTrailingSeparater(jreHome);

        if (FilePath::FileExists(result + _T("lib/rt.jar")) == false) {
            result = FilePath::IncludeTrailingSeparater(jreHome) + _T("jre");

            if (FilePath::FileExists(result + _T("/lib/rt.jar")) == false) {
                //check redhat location
                if (FilePath::FileExists(_T("/usr/java/latest/jre/lib/rt.jar")) == true) {
                    result = _T("/usr/java/latest/jre");
                }
                else if (FilePath::FileExists(_T("/usr/lib/jvm/default-java/jre/lib/rt.jar")) == true) {
                    result = _T("/usr/lib/jvm/default-java/jre");
                }
                else {
                    result = _T("");
                }
            }
        }
    }

    return result;
}

TString LinuxPlatform::GetSystemJVMLibraryFileName() {
    TString result;
    TString jreHome = GetSystemJRE();

    if (jreHome.empty() == false && FilePath::DirectoryExists(jreHome) == true) {
        result = FilePath::IncludeTrailingSeparater(jreHome) +
            _T("/lib/"JAVAARCH"/client/libjvm.so");

        if (FilePath::FileExists(result) == false) {
            result = FilePath::IncludeTrailingSeparater(jreHome) +
                _T("/lib/"JAVAARCH"/server/libjvm.so");
        }
    }

    return result;
}

bool LinuxPlatform::IsMainThread() {
    bool result = (FMainThread == pthread_self());
    return result;
}

TPlatformNumber LinuxPlatform::GetMemorySize() {
    long pages = sysconf(_SC_PHYS_PAGES);
    long page_size = sysconf(_SC_PAGE_SIZE);
    TPlatformNumber result = pages * page_size;
    result = result / 1048576; // Convert from bytes to megabytes.
    return result;
}

#ifdef DEBUG
bool LinuxPlatform::IsNativeDebuggerPresent() {
    // gdb opens file descriptors stdin=3, stdout=4, stderr=5 whereas
    // a typical prog uses only stdin=0, stdout=1, stderr=2.
    bool result = false;
    FILE *fd = fopen("/tmp", "r");

    if (fileno(fd) > 5) {
        result = true;
    }

    fclose(fd);
    return result;
}

int LinuxPlatform::GetProcessID() {
    int pid = getpid();
    return pid;
}
#endif //DEBUG

//--------------------------------------------------------------------------------------------------

#ifndef __UNIX_DEPLOY_PLATFORM__
#define __UNIX_DEPLOY_PLATFORM__

/** Provide an abstraction for difference in the platform APIs,
     e.g. string manipulation functions, etc. */
#include <stdio.h>
#include <string.h>
#include <strings.h>
#include <sys/stat.h>

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
#define xmlTagType    0
#define xmlPCDataType 1

typedef struct _xmlNode XMLNode;
typedef struct _xmlAttribute XMLAttribute;

struct _xmlNode {
    int           _type;        // Type of node: tag, pcdata, cdate
    TCHAR*         _name;       // Contents of node
    XMLNode*      _next;        // Next node at same level
    XMLNode*      _sub;         // First sub-node
    XMLAttribute* _attributes;  // List of attributes
};

struct _xmlAttribute {
    TCHAR* _name;               // Name of attribute
    TCHAR* _value;              // Value of attribute
    XMLAttribute* _next;        // Next attribute for this tag
};

// Public interface
static void     RemoveNonAsciiUTF8FromBuffer(char *buf);
XMLNode* ParseXMLDocument    (TCHAR* buf);
void     FreeXMLDocument     (XMLNode* root);

// Utility methods for parsing document
XMLNode* FindXMLChild        (XMLNode* root,      const TCHAR* name);
TCHAR*    FindXMLAttribute    (XMLAttribute* attr, const TCHAR* name);

// Debugging
void PrintXMLDocument(XMLNode* node, int indt);


#include <sys/types.h>
#include <sys/stat.h>
#include <setjmp.h>
#include <stdlib.h>
#include <wctype.h>


#define JWS_assert(s, msg)      \
    if (!(s)) { Abort(msg); }


// Internal declarations
static XMLNode*      ParseXMLElement(void);
static XMLAttribute* ParseXMLAttribute(void);
static TCHAR*         SkipWhiteSpace(TCHAR *p);
static TCHAR*         SkipXMLName(TCHAR *p);
static TCHAR*         SkipXMLComment(TCHAR *p);
static TCHAR*         SkipXMLDocType(TCHAR *p);
static TCHAR*         SkipXMLProlog(TCHAR *p);
static TCHAR*         SkipPCData(TCHAR *p);
static int           IsPCData(TCHAR *p);
static void          ConvertBuiltInEntities(TCHAR* p);
static void          SetToken(int type, TCHAR* start, TCHAR* end);
static void          GetNextToken(void);
static XMLNode*      CreateXMLNode(int type, TCHAR* name);
static XMLAttribute* CreateXMLAttribute(TCHAR *name, TCHAR* value);
static XMLNode*      ParseXMLElement(void);
static XMLAttribute* ParseXMLAttribute(void);
static void          FreeXMLAttribute(XMLAttribute* attr);
static void          PrintXMLAttributes(XMLAttribute* attr);
static void          indent(int indt);

static jmp_buf       jmpbuf;
static XMLNode*      root_node = NULL;

/** definition of error codes for setjmp/longjmp,
 *  that can be handled in ParseXMLDocument()
 */
#define JMP_NO_ERROR     0
#define JMP_OUT_OF_RANGE 1

#define NEXT_CHAR(p) {if (*p != 0) { p++;} else {longjmp(jmpbuf, JMP_OUT_OF_RANGE);}}
#define NEXT_CHAR_OR_BREAK(p) {if (*p != 0) { p++;} else {break;}}
#define NEXT_CHAR_OR_RETURN(p) {if (*p != 0) { p++;} else {return;}}
#define SKIP_CHARS(p,n) {int i; for (i = 0; i < (n); i++) \
                                            {if (*p != 0) { p++;} else \
                                                {longjmp(jmpbuf, JMP_OUT_OF_RANGE);}}}
#define SKIP_CHARS_OR_BREAK(p,n) {int i; for (i = 0; i < (n); i++) \
                                            {if (*p != 0) { p++;} else {break;}} \
                                            {if (i < (n)) {break;}}}

/** Iterates through the null-terminated buffer (i.e., C string) and replaces all
 *  UTF-8 encoded character >255 with 255
 *
 *  UTF-8 encoding:
 *
 *   Range A:  0x0000 - 0x007F
 *                               0 | bits 0 - 7
 *   Range B : 0x0080 - 0x07FF  :
 *                               110 | bits 6 - 10
 *                               10  | bits 0 - 5
 *   Range C : 0x0800 - 0xFFFF  :
 *                               1110 | bits 12-15
 *                               10   | bits  6-11
 *                               10   | bits  0-5
 */
static void RemoveNonAsciiUTF8FromBuffer(char *buf) {
    char* p;
    char* q;
    char c;
    p = q = buf;
    /* We are not using NEXT_CHAR() to check if *q is NULL, as q is output location
       and offset for q is smaller than for p. */
    while(*p != '\0') {
        c = *p;
        if ( (c & 0x80) == 0) {
            /* Range A */
            *q++ = *p;
            NEXT_CHAR(p);
        } else if ((c & 0xE0) == 0xC0){
            /* Range B */
            *q++ = (char)0xFF;
            NEXT_CHAR(p);
            NEXT_CHAR_OR_BREAK(p);
        } else {
            /* Range C */
            *q++ = (char)0xFF;
            NEXT_CHAR(p);
            SKIP_CHARS_OR_BREAK(p, 2);
        }
    }
    /* Null terminate string */
    *q = '\0';
}

/* --------------------------------------------------------------------- */

static TCHAR* SkipWhiteSpace(TCHAR *p) {
    if (p != NULL) {
        while(iswspace(*p))
            NEXT_CHAR_OR_BREAK(p);
    }
    return p;
}

static TCHAR* SkipXMLName(TCHAR *p) {
    TCHAR c = *p;
    /* Check if start of token */
    if ( ('a' <= c && c <= 'z') ||
         ('A' <= c && c <= 'Z') ||
         c == '_' || c == ':') {

        while( ('a' <= c && c <= 'z') ||
               ('A' <= c && c <= 'Z') ||
               ('0' <= c && c <= '9') ||
               c == '_' || c == ':' || c == '.' || c == '-' ) {
            NEXT_CHAR(p);
            c = *p;
            if (c == '\0') break;
        }
    }
    return p;
}

static TCHAR* SkipXMLComment(TCHAR *p) {
    if (p != NULL) {
        if (DEPLOY_STRNCMP(p, _T("<!--"), 4) == 0) {
            SKIP_CHARS(p, 4);
            do {
                if (DEPLOY_STRNCMP(p, _T("-->"), 3) == 0) {
                    SKIP_CHARS(p, 3);
                    return p;
                }
                NEXT_CHAR(p);
            } while(*p != '\0');
        }
    }
    return p;
}

static TCHAR* SkipXMLDocType(TCHAR *p) {
    if (p != NULL) {
        if (DEPLOY_STRNCMP(p, _T("<!"), 2) == 0) {
            SKIP_CHARS(p, 2);
            while (*p != '\0') {
                if (*p == '>') {
                    NEXT_CHAR(p);
                    return p;
                }
                NEXT_CHAR(p);
            }
        }
    }
    return p;
}

static TCHAR* SkipXMLProlog(TCHAR *p) {
    if (p != NULL) {
        if (DEPLOY_STRNCMP(p, _T("<?"), 2) == 0) {
            SKIP_CHARS(p, 2);
            do {
                if (DEPLOY_STRNCMP(p, _T("?>"), 2) == 0) {
                    SKIP_CHARS(p, 2);
                    return p;
                }
                NEXT_CHAR(p);
            } while(*p != '\0');
        }
    }
    return p;
}

/* Search for the built-in XML entities:
 * &amp; (&), &lt; (<), &gt; (>), &apos; ('), and &quote(")
 * and convert them to a real TCHARacter
 */
static void ConvertBuiltInEntities(TCHAR* p) {
    TCHAR* q;
    q = p;
    /* We are not using NEXT_CHAR() to check if *q is NULL, as q is output location
       and offset for q is smaller than for p. */
    while(*p) {
      if (IsPCData(p)) {
        /* dont convert &xxx values within PData */
        TCHAR *end;
        end = SkipPCData(p);
        while(p < end) {
            *q++ = *p;
            NEXT_CHAR(p);
        }
      } else {
        if (DEPLOY_STRNCMP(p, _T("&amp;"), 5) == 0) {
            *q++ = '&';
            SKIP_CHARS(p, 5);
        } else if (DEPLOY_STRNCMP(p, _T("&lt;"), 4)  == 0) {
            *q = '<';
            SKIP_CHARS(p, 4);
        } else if (DEPLOY_STRNCMP(p, _T("&gt;"), 4)  == 0) {
            *q = '>';
            SKIP_CHARS(p, 4);
        } else if (DEPLOY_STRNCMP(p, _T("&apos;"), 6)  == 0) {
            *q = '\'';
            SKIP_CHARS(p, 6);
        } else if (DEPLOY_STRNCMP(p, _T("&quote;"), 7)  == 0) {
            *q = '\"';
            SKIP_CHARS(p, 7);
        } else {
            *q++ = *p;
            NEXT_CHAR(p);
        }
      }
    }
    *q = '\0';
}

/* ------------------------------------------------------------- */
/* XML tokenizer */

#define TOKEN_UNKNOWN             0
#define TOKEN_BEGIN_TAG           1  /* <tag */
#define TOKEN_END_TAG             2  /* </tag */
#define TOKEN_CLOSE_BRACKET       3  /* >  */
#define TOKEN_EMPTY_CLOSE_BRACKET 4  /* /> */
#define TOKEN_PCDATA              5  /* pcdata */
#define TOKEN_CDATA               6  /* cdata */
#define TOKEN_EOF                 7

static TCHAR* CurPos       = NULL;
static TCHAR* CurTokenName        = NULL;
static int   CurTokenType;
static int   MaxTokenSize = -1;

/* Copy token from buffer to Token variable */
static void SetToken(int type, TCHAR* start, TCHAR* end) {
    int len = end - start;
    if (len > MaxTokenSize) {
        if (CurTokenName != NULL) free(CurTokenName);
        CurTokenName = (TCHAR *)malloc((len + 1) * sizeof(TCHAR));
        if (CurTokenName == NULL ) {
            return;
        }
        MaxTokenSize = len;
    }

    CurTokenType = type;
    DEPLOY_STRNCPY(CurTokenName, len + 1, start, len);
    CurTokenName[len] = '\0';
}

/* Skip XML comments, doctypes, and prolog tags */
static TCHAR* SkipFilling(void) {
    TCHAR *q = CurPos;

    /* Skip white space and comment sections */
    do {
        q = CurPos;
        CurPos = SkipWhiteSpace(CurPos);
        CurPos = SkipXMLComment(CurPos); /* Must be called befor DocTypes */
        CurPos = SkipXMLDocType(CurPos); /* <! ... > directives */
        CurPos = SkipXMLProlog(CurPos);   /* <? ... ?> directives */
    } while(CurPos != q);

    return CurPos;
}

/* Parses next token and initializes the global token variables above
   The tokennizer automatically skips comments (<!-- comment -->) and
   <! ... > directives.
*/
static void GetNextToken(void) {
    TCHAR *p, *q;

    /* Skip white space and comment sections */
    p = SkipFilling();

    if (p == NULL || *p == '\0') {
        CurTokenType = TOKEN_EOF;
        return;
    } else if (p[0] == '<' && p[1] == '/') {
        /* TOKEN_END_TAG */
        q = SkipXMLName(p + 2);
        SetToken(TOKEN_END_TAG, p + 2, q);
        p = q;
    } else  if (*p == '<') {
        /* TOKEN_BEGIN_TAG */
        q = SkipXMLName(p + 1);
        SetToken(TOKEN_BEGIN_TAG, p + 1, q);
        p = q;
    } else if (p[0] == '>') {
        CurTokenType = TOKEN_CLOSE_BRACKET;
        NEXT_CHAR(p);
    } else if (p[0] == '/' && p[1] == '>') {
        CurTokenType = TOKEN_EMPTY_CLOSE_BRACKET;
        SKIP_CHARS(p, 2);
    } else {
        /* Search for end of data */
        q = p + 1;
        while(*q && *q != '<') {
            if (IsPCData(q)) {
                q = SkipPCData(q);
            } else {
                NEXT_CHAR(q);
            }
        }
        SetToken(TOKEN_PCDATA, p, q);
        /* Convert all entities inside token */
        ConvertBuiltInEntities(CurTokenName);
        p = q;
    }
    /* Advance pointer to beginning of next token */
    CurPos = p;
}

static XMLNode* CreateXMLNode(int type, TCHAR* name) {
    XMLNode* node;
    node  = (XMLNode*)malloc(sizeof(XMLNode));
    if (node == NULL) {
        return NULL;
    }
    node->_type = type;
    node->_name = name;
    node->_next = NULL;
    node->_sub  = NULL;
    node->_attributes = NULL;
    return node;
}

static XMLAttribute* CreateXMLAttribute(TCHAR *name, TCHAR* value) {
    XMLAttribute* attr;
    attr = (XMLAttribute*)malloc(sizeof(XMLAttribute));
    if (attr == NULL) {
        return NULL;
    }
    attr->_name = name;
    attr->_value = value;
    attr->_next =  NULL;
    return attr;
}

XMLNode* ParseXMLDocument(TCHAR* buf) {
    XMLNode* root;
    int err_code = setjmp(jmpbuf);
    switch (err_code)
    {
    case JMP_NO_ERROR:
#ifndef _UNICODE
        /* Remove UTF-8 encoding from buffer */
        RemoveNonAsciiUTF8FromBuffer(buf);
#endif

        /* Get first Token */
        CurPos = buf;
        GetNextToken();

        /* Parse document*/
        root =  ParseXMLElement();
    break;
    case JMP_OUT_OF_RANGE:
        /* cleanup: */
        if (root_node != NULL) {
            FreeXMLDocument(root_node);
            root_node = NULL;
        }
        if (CurTokenName != NULL) free(CurTokenName);
        fprintf(stderr,"Error during parsing jnlp file...\n");
        exit(-1);
    break;
    default:
        root = NULL;
    break;
    }

    return root;
}

static XMLNode* ParseXMLElement(void) {
    XMLNode*  node     = NULL;
    XMLNode*  subnode  = NULL;
    XMLNode*  nextnode = NULL;
    XMLAttribute* attr = NULL;

    if (CurTokenType == TOKEN_BEGIN_TAG) {

        /* Create node for new element tag */
        node = CreateXMLNode(xmlTagType, DEPLOY_STRDUP(CurTokenName));
        /* We need to save root node pointer to be able to cleanup
           if an error happens during parsing */
        if(!root_node) {
            root_node = node;
        }
        /* Parse attributes. This section eats a all input until
           EOF, a > or a /> */
        attr = ParseXMLAttribute();
        while(attr != NULL) {
          attr->_next = node->_attributes;
          node->_attributes = attr;
          attr = ParseXMLAttribute();
        }

        /* This will eihter be a TOKEN_EOF, TOKEN_CLOSE_BRACKET, or a
         * TOKEN_EMPTY_CLOSE_BRACKET */
        GetNextToken();

        /* Skip until '>', '/>' or EOF. This should really be an error, */
        /* but we are loose */
//        if(CurTokenType == TOKEN_EMPTY_CLOSE_BRACKET ||
//               CurTokenType == TOKEN_CLOSE_BRACKET ||
//               CurTokenType  == TOKEN_EOF) {
//            println("XML Parsing error: wrong kind of token found");
//            return NULL;
//        }

        if (CurTokenType == TOKEN_EMPTY_CLOSE_BRACKET) {
            GetNextToken();
            /* We are done with the sublevel - fall through to continue */
            /* parsing tags at the same level */
        } else if (CurTokenType == TOKEN_CLOSE_BRACKET) {
            GetNextToken();

            /* Parse until end tag if found */
            node->_sub  = ParseXMLElement();

            if (CurTokenType == TOKEN_END_TAG) {
                /* Find closing bracket '>' for end tag */
                do {
                   GetNextToken();
                } while(CurTokenType != TOKEN_EOF && CurTokenType != TOKEN_CLOSE_BRACKET);
                GetNextToken();
            }
        }

        /* Continue parsing rest on same level */
        if (CurTokenType != TOKEN_EOF) {
                /* Parse rest of stream at same level */
                node->_next = ParseXMLElement();
        }
        return node;

    } else if (CurTokenType == TOKEN_PCDATA) {
        /* Create node for pcdata */
        node = CreateXMLNode(xmlPCDataType, DEPLOY_STRDUP(CurTokenName));
        /* We need to save root node pointer to be able to cleanup
           if an error happens during parsing */
        if(!root_node) {
            root_node = node;
        }
        GetNextToken();
        return node;
    }

    /* Something went wrong. */
    return NULL;
}

/* Parses an XML attribute. */
static XMLAttribute* ParseXMLAttribute(void) {
    TCHAR* q = NULL;
    TCHAR* name = NULL;
    TCHAR* PrevPos = NULL;

    do
    {
        /* We need to check this condition to avoid endless loop
           in case if an error happend during parsing. */
        if (PrevPos == CurPos) {
            if (name != NULL) {
                free(name);
                name = NULL;
            }

            return NULL;
        }

        PrevPos = CurPos;

        /* Skip whitespace etc. */
        SkipFilling();

        /* Check if we are done witht this attribute section */
        if (CurPos[0] == '\0' ||
            CurPos[0] == '>' ||
            CurPos[0] == '/' && CurPos[1] == '>') {

            if (name != NULL) {
                free(name);
                name = NULL;
            }

            return NULL;
        }

        /* Find end of name */
        q = CurPos;
        while(*q && !iswspace(*q) && *q !='=') NEXT_CHAR(q);

        SetToken(TOKEN_UNKNOWN, CurPos, q);
        if (name) {
            free(name);
            name = NULL;
        }
        name = DEPLOY_STRDUP(CurTokenName);

        /* Skip any whitespace */
        CurPos = q;
        CurPos = SkipFilling();

        /* Next TCHARacter must be '=' for a valid attribute.
           If it is not, this is really an error.
           We ignore this, and just try to parse an attribute
           out of the rest of the string.
        */
    } while(*CurPos != '=');

    NEXT_CHAR(CurPos);
    CurPos = SkipWhiteSpace(CurPos);
    /* Parse CDATA part of attribute */
    if ((*CurPos == '\"') || (*CurPos == '\'')) {
        TCHAR quoteChar = *CurPos;
        q = ++CurPos;
        while(*q != '\0' && *q != quoteChar) NEXT_CHAR(q);
        SetToken(TOKEN_CDATA, CurPos, q);
        CurPos = q + 1;
    } else {
        q = CurPos;
        while(*q != '\0' && !iswspace(*q)) NEXT_CHAR(q);
        SetToken(TOKEN_CDATA, CurPos, q);
        CurPos = q;
    }

    //Note: no need to free name and CurTokenName duplicate; they're assigned
    // to an XMLAttribute structure in CreateXMLAttribute

    return CreateXMLAttribute(name, DEPLOY_STRDUP(CurTokenName));
}

void FreeXMLDocument(XMLNode* root) {
  if (root == NULL) return;
  FreeXMLDocument(root->_sub);
  FreeXMLDocument(root->_next);
  FreeXMLAttribute(root->_attributes);
  free(root->_name);
  free(root);
}

static void FreeXMLAttribute(XMLAttribute* attr) {
  if (attr == NULL) return;
  free(attr->_name);
  free(attr->_value);
  FreeXMLAttribute(attr->_next);
  free(attr);
}

/* Find element at current level with a given name */
XMLNode* FindXMLChild(XMLNode* root, const TCHAR* name) {
  if (root == NULL) return NULL;

  if (root->_type == xmlTagType && DEPLOY_STRCMP(root->_name, name) == 0) {
    return root;
  }

  return FindXMLChild(root->_next, name);
}

/* Search for an attribute with the given name and returns the contents. Returns NULL if
 * attribute is not found
 */
TCHAR* FindXMLAttribute(XMLAttribute* attr, const TCHAR* name) {
  if (attr == NULL) return NULL;
  if (DEPLOY_STRCMP(attr->_name, name) == 0) return attr->_value;
  return FindXMLAttribute(attr->_next, name);
}


void PrintXMLDocument(XMLNode* node, int indt) {
  if (node == NULL) return;

  if (node->_type == xmlTagType) {
     DEPLOY_PRINTF(_T("\n"));
     indent(indt);
     DEPLOY_PRINTF(_T("<%s"), node->_name);
     PrintXMLAttributes(node->_attributes);
     if (node->_sub == NULL) {
       DEPLOY_PRINTF(_T("/>\n"));
     } else {
       DEPLOY_PRINTF(_T(">"));
       PrintXMLDocument(node->_sub, indt + 1);
       indent(indt);
       DEPLOY_PRINTF(_T("</%s>"), node->_name);
     }
  } else {
    DEPLOY_PRINTF(_T("%s"), node->_name);
  }
  PrintXMLDocument(node->_next, indt);
}

static void PrintXMLAttributes(XMLAttribute* attr) {
  if (attr == NULL) return;

  DEPLOY_PRINTF(_T(" %s=\"%s\""), attr->_name, attr->_value);
  PrintXMLAttributes(attr->_next);
}

static void indent(int indt) {
    int i;
    for(i = 0; i < indt; i++) {
        DEPLOY_PRINTF(_T("  "));
    }
}

const TCHAR *CDStart = _T("<![CDATA[");
const TCHAR *CDEnd = _T("]]>");


static TCHAR* SkipPCData(TCHAR *p) {
    TCHAR *end = DEPLOY_STRSTR(p, CDEnd);
    if (end != NULL) {
        return end+sizeof(CDEnd);
    }
    return (++p);
}

static int IsPCData(TCHAR *p) {
    return (DEPLOY_STRNCMP(CDStart, p, sizeof(CDStart)) == 0);
}

//--------------------------------------------------------------------------------------------------

LinuxJavaUserPreferences::LinuxJavaUserPreferences(void) : JavaUserPreferences() {
}

LinuxJavaUserPreferences::~LinuxJavaUserPreferences(void) {
}

TString LinuxJavaUserPreferences::GetUserPrefFileName(TString Appid) {
    TString result;
    struct passwd *pw = getpwuid(getuid());
    TString homedir = pw->pw_dir;
    TString userOverrideFileName = FilePath::IncludeTrailingSeparater(homedir) +
        FilePath::IncludeTrailingSeparater(_T(".java/.userPrefs")) +
        FilePath::IncludeTrailingSeparater(Appid) +
        _T("JVMUserOptions/prefs.xml");

    if (FilePath::FileExists(userOverrideFileName) == true) {
        result = userOverrideFileName;
    }

    return result;
}

OrderedMap<TString, TString> ReadNode(XMLNode* node) {
    OrderedMap<TString, TString> result;
    XMLNode* keyNode = FindXMLChild(node->_sub, _T("entry"));

    while (keyNode != NULL) {
        TString key = FindXMLAttribute(keyNode->_attributes, _T("key"));
        TString value = FindXMLAttribute(keyNode->_attributes, _T("value"));
        keyNode = keyNode->_next;

        if (key.empty() == false) {
            result.Append(key, value);
        }
    }

    return result;
}

OrderedMap<TString, TString> GetJvmUserArgs(TString filename) {
    OrderedMap<TString, TString> result;

    if (FilePath::FileExists(filename) == true) {
        //scan file for the key
        FILE* fp = fopen(PlatformString(filename).toPlatformString(), "r");

        if (fp != NULL) {
            fseek(fp, 0, SEEK_END);
            long fsize = ftell(fp);
            rewind(fp);
            DynamicBuffer<char> buffer(fsize + 1);
            fread(buffer.GetData(), fsize, 1, fp);
            fclose(fp);
            buffer[fsize] = 0;

            XMLNode* node = NULL;
            XMLNode* doc = ParseXMLDocument(buffer.GetData());

            if (doc != NULL) {
                node = FindXMLChild(doc, _T("map"));

                if (node != NULL) {
                    result = ReadNode(node);
                }
            }
        }
    }

    return result;
}

bool LinuxJavaUserPreferences::Load(TString Appid) {
    bool result = false;
    TString filename = GetUserPrefFileName(Appid);

    if (FilePath::FileExists(filename) == true) {
        FMap = GetJvmUserArgs(filename);
        result = true;
    }

    return result;
}

//--------------------------------------------------------------------------------------------------

#endif // LINUX
