/*
 * Copyright (c) 2011, 2013, Oracle and/or its affiliates. All rights reserved.
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
    m_flags(0), m_cursorPosW(0), m_jtext(NULL), m_pResultTextInfo(NULL), \
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

    JNIEnv *env = GetEnv();
    if (m_cStrW > 0) {
        m_jtext = MakeJavaString(env, m_lpStrW, m_cStrW);
    }

    // Merge the string if necessary
    if (m_pResultTextInfo != NULL) {
        jstring jresultText = m_pResultTextInfo->GetText();
        if (m_jtext != NULL && jresultText != NULL) {
            m_jtext = ConcatJStrings(env, jresultText, m_jtext);
        }
        else if (m_jtext == NULL && jresultText != NULL) {
            /* No composing text, assign the committed text to m_jtext */
            m_jtext = (jstring)env->NewLocalRef(jresultText);
        }
    }

    return 0;
}

/*
 * Destructor
 * free the pointer in the m_lpInfoStrW array
 */
GlassInputTextInfo::~GlassInputTextInfo() {

    if (m_jtext) {
        JNIEnv *env = GetEnv();
        env->DeleteLocalRef(m_jtext);
        m_jtext = NULL;
    }

    delete [] m_lpStrW;
    delete [] m_lpClauseW;
    delete [] m_lpAttrW;

    if (m_pResultTextInfo) {
        delete m_pResultTextInfo;
        m_pResultTextInfo = NULL;
    }
}


jstring GlassInputTextInfo::MakeJavaString(JNIEnv* env, LPWSTR lpStrW, int cStrW) {

    if (env == NULL || lpStrW == NULL || cStrW == 0) {
        return NULL;
    } else {
        jstring jStr = env->NewString(reinterpret_cast<jchar*>(lpStrW), cStrW);
        if (CheckAndClearException(env)) return NULL;
        return jStr;
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

    //Convert ANSI string caluse information to UNICODE string clause information.
    try {
        bndClauseW = new int[m_cClauseW + 1];
    } catch (std::bad_alloc&) {
        lpBndClauseW = NULL;
        delete [] bndClauseW;
        throw;
    }

    JNIEnv *env = GetEnv();

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
        int cResultClauseW = m_pResultTextInfo->GetClauseInfo(bndResultClauseW); 
        // Concatenate Clause information.
        int cMergedClauseW = m_cClauseW + cResultClauseW;
        int* bndMergedClauseW = NULL;
        try {
            bndMergedClauseW = new int[cMergedClauseW+1];
        } catch (std::bad_alloc&) {
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
    if (m_cStrW == 0 || m_cAttrW != m_cStrW) {
        lpBndAttrW = NULL;
        lpValAttrW = NULL;

        return 0;
    }

    int* bndAttrW = NULL;
    BYTE* valAttrW = NULL;

    //Scan attribute byte array and make attribute run information.
    try {
        bndAttrW = new int[m_cAttrW + 1];
        valAttrW = new BYTE[m_cAttrW];
    } catch (std::bad_alloc&) {
        lpBndAttrW = NULL;
        lpValAttrW = NULL;
        delete [] bndAttrW;
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
            delete [] bndMergedAttrW;
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
