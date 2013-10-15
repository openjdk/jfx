/*
 * Copyright (c) 2006, 2013, Oracle and/or its affiliates. All rights reserved.
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

#ifndef XMLPARSER_H
#define XMLPARSER_H

#include "DeployPlatform.h"

/*
 *  Contains a simply-minded XML parser.
 *
 *  The following assumptions are made about the DTD, XML document:
 *   -  The encoding is UTF-8
 *
 *   -  The parser sets all   unicode character >255 to 255. Thus,
 *      there can be no non-acsii characters in tags, attributes, or
 *      data used by the C program.
 *
 *   -  All attributes are passed as C data
 *
 *   -  No entities are defined except for the default ones, e.g.,
 *      &amp; (&), &lt; (<), &gt; (>), &apos; ('), and &quote(")
 *
 */

#define xmlTagType    0
#define xmlPCDataType 1

typedef struct _xmlNode XMLNode;
typedef struct _xmlAttribute XMLAttribute;

struct _xmlNode {
    int           _type;        /* Type of node: tag, pcdata, cdate */
    TCHAR*         _name;        /* Contents of node */
    XMLNode*      _next;        /* Next node at same level */
    XMLNode*      _sub;         /* First sub-node */
    XMLAttribute* _attributes;  /* List of attributes */
};

struct _xmlAttribute {
    TCHAR* _name;              /* Name of attribute */
    TCHAR* _value;             /* Value of attribute */
    XMLAttribute* _next;      /* Next attribute for this tag */
};


/* Public interface */
static void     RemoveNonAsciiUTF8FromBuffer(char *buf);
XMLNode* ParseXMLDocument    (TCHAR* buf);
void     FreeXMLDocument     (XMLNode* root);

/* Utility methods for parsing document */
XMLNode* FindXMLChild        (XMLNode* root,      TCHAR* name);
TCHAR*    FindXMLAttribute    (XMLAttribute* attr, TCHAR* name);

/* Debugging */
void PrintXMLDocument(XMLNode* node, int indt);

#endif
