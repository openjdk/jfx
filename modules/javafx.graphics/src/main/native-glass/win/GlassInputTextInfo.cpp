/*
 * Copyright (c) 2011, 2026, Oracle and/or its affiliates. All rights reserved.
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

#include "common.h"

#include "GlassInputTextInfo.h"

#define WCHAR_SZ sizeof(WCHAR)
#define DWORD_SZ sizeof(DWORD)

// The start and end index of the result and composition in GCS_INDEX array.
#define START_RESULTSTR 0
#define END_RESULTSTR 1
#define START_COMPSTR 2
#define END_COMPSTR 4

// The GCS_INDEX array is partitioned into 2 parts, one is result string related and the
// other is composing string related.
const DWORD GlassInputTextInfo::GCS_INDEX[5]= {
    GCS_RESULTSTR,
    GCS_RESULTCLAUSE,
    GCS_COMPSTR,
    GCS_COMPCLAUSE,
    GCS_COMPATTR
};

GlassInputTextInfo::GlassInputTextInfo(ViewContainer * const vc) :
    m_flags(0), m_cursorPosW(0), m_lpMergedW(NULL), m_cMergedW(0), m_pResultTextInfo(NULL), \
    m_cStrW(0), m_cClauseW(0), m_cAttrW(0), \
    m_lpStrW(NULL), m_lpClauseW(NULL), m_lpAttrW(NULL)
{ m_viewContainer = vc; }


/* Retrieve the context data from the current IMC.
   Params:
   HIMC hIMC - the input method context, must NOT be NULL
   LPARAMS flags - message param to WM_IME_COMPOSITION.
   Returns 0 if success.
*/
int
GlassInputTextInfo::GetContextData(HIMC hIMC, const LPARAM flags) {

    ASSERT(hIMC != 0);

    m_flags = flags;
    // Based on different flags received, we use different GCS_XXX from the
    // GCS_INDEX array.
    int startIndex = 0, endIndex = 0;

    if (flags & GCS_COMPSTR) {
        startIndex = START_COMPSTR;
        endIndex = END_COMPSTR;
        /* For some window input method such as Chinese QuanPing, when the user
         * commits some text, the IMM sends WM_IME_COMPOSITION with GCS_COMPSTR/GCS_RESULTSTR.
         * So we have to extract the result string from IMC. For most of other cases,
         * m_pResultTextInfo is NULL and this is why we choose to have a pointer as its member
         * rather than having a list of the result string information.
         */
        if (flags & GCS_RESULTSTR) {
            m_pResultTextInfo = new GlassInputTextInfo(m_viewContainer);
            m_pResultTextInfo->GetContextData(hIMC, GCS_RESULTSTR);
        }
    } else if (flags & GCS_RESULTSTR) {
        startIndex = START_RESULTSTR;
        endIndex = END_RESULTSTR;
    } else { // unknown flags.
        return -1;
    }

    /* Get the data from the input context */
    LONG   cbData[3] = {0};
    LPVOID lpData[3] = {NULL};
    for (int i = startIndex, j = 0; i <= endIndex; i++, j++) {
        cbData[j] = ::ImmGetCompositionString(hIMC, GCS_INDEX[i], NULL, 0);
        if (cbData[j] == 0) {
            lpData[j] = NULL;
        } else {
            LPBYTE lpTemp = new BYTE[cbData[j]];
            cbData[j] = ::ImmGetCompositionString(hIMC, GCS_INDEX[i], lpTemp, cbData[j]);
            if (IMM_ERROR_GENERAL != cbData[j]) {
                lpData[j] = (LPVOID)lpTemp;
            } else {
                lpData[j] = NULL;
                return -1;
            }
        }
    }

    // Assign the context data
    m_cStrW = cbData[0]/WCHAR_SZ;
    m_lpStrW = (LPWSTR)lpData[0];

    m_cClauseW = cbData[1]/DWORD_SZ - 1;
    m_lpClauseW = (LPDWORD)lpData[1];

    if (cbData[2] > 0) {
        m_cAttrW = cbData[2];
        m_lpAttrW = (LPBYTE)lpData[2];
    }

    // Get the cursor position
    if (flags & GCS_COMPSTR) {
        m_cursorPosW = ::ImmGetCompositionString(hIMC, GCS_CURSORPOS,
                                                NULL, 0);
    }

    // Merge the string if necessary
    if (m_pResultTextInfo != NULL) {
        /* Result first, then composition, as the former ConcatJStrings put them. new (std::nothrow),
         * so a failed join leaves m_lpMergedW NULL and GetTextW() answers NULL - what ConcatJStrings
         * answered when its upcall failed - instead of throwing on a path that has no throw today.
         * One owner per buffer: the result text stays owned by m_pResultTextInfo and is only read
         * here. */
        int cResultW = 0;
        const wchar_t* lpResultW = m_pResultTextInfo->GetTextW(&cResultW);
        if (m_cStrW > 0 && m_lpStrW != NULL && lpResultW != NULL && cResultW > 0) {
            m_lpMergedW = new (std::nothrow) wchar_t[cResultW + m_cStrW];
            if (m_lpMergedW != NULL) {
                memcpy(m_lpMergedW, lpResultW, cResultW * sizeof(wchar_t));
                memcpy(m_lpMergedW + cResultW, m_lpStrW, m_cStrW * sizeof(wchar_t));
                m_cMergedW = cResultW + m_cStrW;
            }
        }
    }

    return 0;
}

/*
 * The former GetText()'s three cases over the wide buffers: the join when both strings existed (NULL
 * if the join failed, as ConcatJStrings then returned NULL), else this object's string, else the
 * result string borrowed from m_pResultTextInfo (the former jstring alias case), else nothing.
 */
const wchar_t* GlassInputTextInfo::GetTextW(int* outLen) const {
    int cResultW = 0;
    const wchar_t* lpResultW = m_pResultTextInfo != NULL ? m_pResultTextInfo->GetTextW(&cResultW) : NULL;
    const wchar_t* lpOwnW = (m_cStrW > 0 && m_lpStrW != NULL) ? m_lpStrW : NULL;
    if (lpOwnW != NULL && lpResultW != NULL) {
        *outLen = m_cMergedW;
        return m_lpMergedW;
    }
    if (lpOwnW != NULL) {
        *outLen = m_cStrW;
        return lpOwnW;
    }
    if (lpResultW != NULL) {
        *outLen = cResultW;
        return lpResultW;
    }
    *outLen = 0;
    return NULL;
}

/*
 * Destructor
 * free the pointer in the m_lpInfoStrW array
 */
GlassInputTextInfo::~GlassInputTextInfo() {

    delete [] m_lpStrW;
    delete [] m_lpClauseW;
    delete [] m_lpAttrW;
    delete [] m_lpMergedW;

    if (m_pResultTextInfo) {
        delete m_pResultTextInfo;
        m_pResultTextInfo = NULL;
    }
}


//
//  Convert Clause Information for DBCS string to that for Unicode string
//  *lpBndClauseW must be deleted by caller.
//
int GlassInputTextInfo::GetClauseInfo(int*& lpBndClauseW) {

    if ( m_cStrW ==0 || m_cClauseW ==0 ||
         m_lpClauseW == NULL ||
         m_lpClauseW[0] != 0 || m_lpClauseW[m_cClauseW] != (DWORD)m_cStrW) {
        lpBndClauseW = NULL;
        return 0;
    }

    int*    bndClauseW = NULL;

    //Convert ANSI string clause information to UNICODE string clause information.
    try {
        bndClauseW = new int[m_cClauseW + 1];
    } catch (std::bad_alloc&) {
        lpBndClauseW = NULL;
        delete [] bndClauseW;
        throw;
    }

    for ( int cls = 0; cls < m_cClauseW; cls++ ) {
        bndClauseW[cls] = m_lpClauseW[cls];
    }

    bndClauseW[m_cClauseW] = m_cStrW;

    int retVal = 0;
    int cCommittedStrW = GetCommittedTextLength();

    /* The conditions to merge the clause information are described below:
       Senario 1:
       m_flags & GCS_RESULTSTR is true only, this case m_pResultTextInfo must be NULL.
       No need to merge.

       Senario 2:
       m_flags & GCS_COMPSTR is true only, this case m_pResultTextInfo is also NULL.
       No need to merge either.

       Senario 3:
       m_flags & GCS_COMPSTR and m_flags & GCS_RESULTSTR both yield to true, in this case
       m_pResultTextInfo won't be NULL and if there is nothing to commit though, we don't
       have to merge. Or if the current composing string size is 0, we don't have to merge either.

       So in clusion, the three conditions not not merge are:
       1. no commited string
       2. m_pResultTextInfo points to NULL
       3. the current string size is 0;

       Same rule applies to merge the attribute information.
    */
    if (m_cStrW == 0 || cCommittedStrW == 0 ||
        m_pResultTextInfo == NULL) {
        lpBndClauseW = bndClauseW;
        retVal = m_cClauseW;
    } else { /* partial commit case */
        int* bndResultClauseW = NULL;
        int* bndMergedClauseW = NULL;
        int cResultClauseW = 0, cMergedClauseW = 0;
        try {
            cResultClauseW = m_pResultTextInfo->GetClauseInfo(bndResultClauseW);
            // Concatenate Clause information.
            cMergedClauseW = m_cClauseW + cResultClauseW;
            bndMergedClauseW = new int[cMergedClauseW+1];
        } catch (std::bad_alloc&) {
            delete [] bndClauseW;
            delete [] bndResultClauseW;
            delete [] bndMergedClauseW;
            throw;
        }

        int i = 0;
        if (cResultClauseW > 0 && bndResultClauseW) {
            for (; i < cResultClauseW; i++) {
                bndMergedClauseW[i] = bndResultClauseW[i];
            }
        }

        if (m_cClauseW > 0 && bndClauseW) {
            for(int j = 0; j < m_cClauseW; j++, i++) {
                bndMergedClauseW[i] = bndClauseW[j] + cCommittedStrW;
            }
        }
        delete [] bndClauseW;
        delete [] bndResultClauseW;
        bndMergedClauseW[cMergedClauseW] = m_cStrW + cCommittedStrW;
        lpBndClauseW = bndMergedClauseW;
        retVal = cMergedClauseW;
    }

    return retVal;
}

//
//  Convert Attribute Information for DBCS string to that for Unicode string
//  *lpBndAttrW and *lpValAttrW  must be deleted by caller.
//
int GlassInputTextInfo::GetAttributeInfo(int*& lpBndAttrW, BYTE*& lpValAttrW) {

    lpBndAttrW = NULL;
    lpValAttrW = NULL;

    if (m_cStrW == 0 || m_cAttrW != m_cStrW) {
        return 0;
    }

    int* bndAttrW = NULL;
    BYTE* valAttrW = NULL;

    //Scan attribute byte array and make attribute run information.
    try {
        bndAttrW = new int[m_cAttrW + 1];
        valAttrW = new BYTE[m_cAttrW];
    } catch (std::bad_alloc&) {
        delete [] bndAttrW;
        delete [] valAttrW;
        throw;
    }

    int cAttrWT = 0;
    bndAttrW[0] = 0;
    valAttrW[0] = m_lpAttrW[0];
    /* remove duplicate attribute in the m_lpAttrW array. */
    for ( int offW = 1; offW < m_cAttrW; offW++ ) {
        if ( m_lpAttrW[offW] != valAttrW[cAttrWT]) {
            cAttrWT++;
            bndAttrW[cAttrWT] = offW;
            valAttrW[cAttrWT] = m_lpAttrW[offW];
        }
    }
    bndAttrW[++cAttrWT] =  m_cStrW;

    int retVal = 0;

    int cCommittedStrW = GetCommittedTextLength();
    if (m_cStrW == 0 ||
        cCommittedStrW == 0 || m_pResultTextInfo == NULL) {
        lpBndAttrW = bndAttrW;
        lpValAttrW = valAttrW;
        retVal = cAttrWT;
    } else {
        int cMergedAttrW = 1 + cAttrWT;
        int*    bndMergedAttrW = NULL;
        BYTE*   valMergedAttrW = NULL;
        try {
            bndMergedAttrW = new int[cMergedAttrW+1];
            valMergedAttrW = new BYTE[cMergedAttrW];
        } catch (std::bad_alloc&) {
            delete [] bndAttrW;
            delete [] valAttrW;
            delete [] bndMergedAttrW;
            delete [] valMergedAttrW;
            throw;
        }
        bndMergedAttrW[0] = 0;
        valMergedAttrW[0] = ATTR_CONVERTED;
        for (int j = 0; j < cAttrWT; j++) {
            bndMergedAttrW[j+1] = bndAttrW[j]+cCommittedStrW;
            valMergedAttrW[j+1] = valAttrW[j];
        }
        bndMergedAttrW[cMergedAttrW] = m_cStrW + cCommittedStrW;

        delete [] bndAttrW;
        delete [] valAttrW;
        lpBndAttrW = bndMergedAttrW;
        lpValAttrW = valMergedAttrW;
        retVal = cMergedAttrW;
    }

    return retVal;
}

//
// Returns the cursor position of the current composition.
// returns 0 if the current mode is not GCS_COMPSTR
//
int GlassInputTextInfo::GetCursorPosition() const {
    if (m_flags & GCS_COMPSTR) {
        return m_cursorPosW;
    } else {
        return 0;
    }
}


//
// Returns the committed text length
//
int GlassInputTextInfo::GetCommittedTextLength() const {

    if ((m_flags & GCS_COMPSTR) && m_pResultTextInfo) {
        return m_pResultTextInfo->GetCommittedTextLength();
    }

    if (m_flags & GCS_RESULTSTR)
        return m_cStrW;
    else
        return 0;
}
