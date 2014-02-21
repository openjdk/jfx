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

#include "GlassApplication.h"
#include "GlassClipboard.h"
#include "GlassDnD.h"
#include "Pixels.h"

#include "com_sun_glass_ui_win_WinSystemClipboard.h"
#include "com_sun_glass_ui_win_WinDndClipboard.h"


// Helper LEAVE_MAIN_THREAD for GlassClipboard
#define LEAVE_MAIN_THREAD_WITH_p  \
    IDataObject * p;  \
    LEAVE_MAIN_THREAD;  \
    ARG(p) = getPtr(env, obj);

jfieldID fidPtr = 0;
static jfieldID fidName = 0;
static jmethodID midFosSerialize = 0;
jmethodID midContentChanged = 0;
jmethodID midActionPerformed = 0;
#define GALLOCFLG (GMEM_DDESHARE | GMEM_MOVEABLE | GMEM_ZEROINIT)



bool operator < (const FORMATETC &fr, const FORMATETC &fl) {
    if (fr.cfFormat != fl.cfFormat)
        return fr.cfFormat < fl.cfFormat;
    if (fr.dwAspect != fl.dwAspect)
        return fr.dwAspect < fl.dwAspect;
    if (fr.lindex != fl.lindex)
        return fr.lindex < fl.lindex;
    if (fr.ptd != fl.ptd)
        return fr.ptd < fl.ptd;
    return fr.tymed < fl.tymed;
}

bool operator == (const FORMATETC &fr, const FORMATETC &fl) {
    return  fr.cfFormat == fl.cfFormat
         && fr.dwAspect == fl.dwAspect
         && fr.lindex   == fl.lindex
         && fr.ptd      == fl.ptd
         && fr.tymed    == fl.tymed;
}

size_t hash_value(const FORMATETC &fr)
{
    size_t _Val = size_t(fr.cfFormat) << 21;
    _Val += size_t(fr.dwAspect);
    _Val <<= 5;
    _Val += size_t(fr.lindex);
    _Val <<= 7;
    _Val += size_t(fr.ptd);
    _Val >>= 13;
    _Val += size_t(fr.tymed);
    return _Val ^ _HASH_SEED;
}

//NB! There are two suffixes for mimes:
// ";locale" - the ASCII/UTF8 version of mime type that is not transferred to Java
// ";cf="    - the mime type that conflicts with Java alias for system standard clipboard type.

//Have to be synchronized with Java class [Clipboard].
static LPCWSTR PASTE_SUCCEEDED = L"ms-stuff/paste-succeeded";
static LPCWSTR PREFERRED_DROP_EFFECT_MIME = L"ms-stuff/preferred-drop-effect";
static LPCWSTR PERFORMED_DROP_EFFECT_MIME = L"ms-stuff/performed-drop-effect";
static LPCWSTR GLASS_TEXT_PLAIN = L"text/plain";
static LPCWSTR GLASS_TEXT_PLAIN_LOCALE = L"text/plain;locale";
static LPCWSTR GLASS_TEXT_HTML = L"text/html";
static LPCWSTR GLASS_TEXT_RTF = L"text/rtf";
static LPCWSTR GLASS_IMAGE = L"application/x-java-rawimage";
static LPCWSTR GLASS_IMAGE_DRAG = L"application/x-java-drag-image";
static LPCWSTR GLASS_IMAGE_DRAG_OFFSET = L"application/x-java-drag-image-offset";
static LPCWSTR GLASS_URI_LIST = L"text/uri-list";
static LPCWSTR GLASS_URI_LIST_LOCALE = L"text/uri-list;locale";
static LPCWSTR GLASS_FILE_LIST = L"application/x-java-file-list";
static LPCWSTR MS_LOCALE = L"ms-stuff/locale";
static LPCWSTR MS_OEMTEXT = L"ms-stuff/oem-text";
static LPCWSTR MS_FILE_DESCRIPTOR = L"ms-stuff/file-descriptor";
static LPCWSTR MS_FILE_DESCRIPTOR_UNICODE = L"ms-stuff/file-descriptor-unicode";
static LPCWSTR MS_FILE_CONTENT = L"message/external-body";

//hidden mimes for supplementary procedures
static LPCWSTR GLASS_IE_URL_SHORTCUT_FILENAME = L"text/ie-shortcut-filename";
static LPCWSTR GLASS_IE_URL_SHORTCUT_CONTENT = L"text/ie-shortcut-content";

struct Mime2oscfstrPair {
    LPCWSTR mime;
    LPCWSTR osString;
};

Mime2oscfstrPair pairs[] = {
    {GLASS_TEXT_HTML, L"HTML Format"},
    {GLASS_TEXT_RTF, L"Rich Text Format"},
    {GLASS_URI_LIST, CFSTR_INETURLW},
    {GLASS_URI_LIST_LOCALE, CFSTR_INETURLA}, //that is used by IE and shell
    {PASTE_SUCCEEDED, CFSTR_PASTESUCCEEDED},
    {PERFORMED_DROP_EFFECT_MIME, CFSTR_PERFORMEDDROPEFFECT},
    {PREFERRED_DROP_EFFECT_MIME, CFSTR_PREFERREDDROPEFFECT},
    {MS_FILE_DESCRIPTOR, CFSTR_FILEDESCRIPTORA},
    {MS_FILE_DESCRIPTOR_UNICODE, CFSTR_FILEDESCRIPTORW},
    {MS_FILE_CONTENT, CFSTR_FILECONTENTS},
};

inline size_t hash_value(const _bstr_t &_Str) {
    return stdext::hash_value((const wchar_t *)_Str);
}


typedef stdext::hash_map<_bstr_t, CLIPFORMAT> MIME2OSCF;
typedef stdext::hash_map<CLIPFORMAT, _bstr_t> OSCF2MIME;
typedef stdext::hash_map<FORMATETC, _bstr_t> FMC2MIME;
typedef stdext::hash_map<FORMATETC, STGMEDIUM> FMC2DATA;
typedef stdext::hash_set<_bstr_t> HASH_STR_SET;

MIME2OSCF mime2oscf;
OSCF2MIME oscf2mime;

void addPair(LPCWSTR mime, const CLIPFORMAT &cf) {
    mime2oscf[mime] = cf;
    oscf2mime[cf] = mime;
}

CLIPFORMAT getClipboardFormat(LPCWSTR mime)
{
    MIME2OSCF::const_iterator i = mime2oscf.find(mime);
    if (mime2oscf.end() == i) {
        CLIPFORMAT cf = ::RegisterClipboardFormat(mime);
        addPair(mime, cf);
        return cf;
    }
    return i->second;
}

_bstr_t getMime(CLIPFORMAT cf)
{
    OSCF2MIME::const_iterator i = oscf2mime.find(cf);
    if (oscf2mime.end() == i) {
        const size_t LEN = 1024;
        WCHAR str[LEN] = {0};

        int res = ::GetClipboardFormatNameW(cf, str, LEN - 1);
        if (res <= 0 || res >= LEN) {
            //make it manually...
            wcscpy_s(str, LEN, L"cf");
            _itow_s(cf, str + 2, LEN - 2, 10);
        }
        //...and permanent
        _bstr_t newMime(str);
        MIME2OSCF::const_iterator p = mime2oscf.find(newMime);
        if (mime2oscf.end() != p) {
            //...FF registers their own independent "text/html"
            //not "HTML Format"
            const size_t SUF_LEN = 32;
            WCHAR suffix[SUF_LEN] = {0};
            wcscpy_s(suffix, SUF_LEN, L";cf=");
            _itow_s(cf, suffix + 4, SUF_LEN - 4, 10);
            newMime += suffix;
        }
        addPair(newMime, cf);
        return newMime;
    }
    return i->second;
}

#define CF_JAVA_BITMAP CF_DIB

int create_mime_stuff()
{
    addPair(GLASS_TEXT_PLAIN, CF_UNICODETEXT);
    addPair(GLASS_TEXT_PLAIN_LOCALE, CF_TEXT);
    addPair(GLASS_IMAGE, CF_JAVA_BITMAP);
    addPair(GLASS_FILE_LIST, CF_HDROP);
    addPair(MS_LOCALE, CF_LOCALE);
    addPair(MS_OEMTEXT, CF_OEMTEXT);
    Mime2oscfstrPair *p = pairs;
    for (int i = 0; i < sizeof(pairs)/sizeof(*pairs); ++i, ++p) {
        addPair(p->mime, ::RegisterClipboardFormat(p->osString));
    }
    return 1;
}

static const int _init_mime_stuff = create_mime_stuff();

static const jint ACTIONS[] = {
    com_sun_glass_ui_win_WinSystemClipboard_ACTION_COPY,
    com_sun_glass_ui_win_WinSystemClipboard_ACTION_MOVE,
    com_sun_glass_ui_win_WinSystemClipboard_ACTION_REFERENCE
};

static const DROPEFFECT DFS[] = {
    DROPEFFECT_COPY,
    DROPEFFECT_MOVE,
    DROPEFFECT_LINK
};

DROPEFFECT getDROPEFFECT(jint actions)
{
    DROPEFFECT ret = DROPEFFECT_NONE;
    for (size_t i = 0; i < sizeof(ACTIONS)/sizeof(*ACTIONS); ++i) {
        if (actions & ACTIONS[i]) {
            ret |= DFS[i];
        }
    }
    return ret;
}

jint getACTION(DROPEFFECT df)
{
    jint  ret = com_sun_glass_ui_win_WinSystemClipboard_ACTION_NONE;
    for (size_t i = 0; i < sizeof(DFS)/sizeof(*DFS); ++i) {
        if (df & DFS[i]) {
            ret |= ACTIONS[i];
        }
    }
    return ret;
}

struct BinaryChunk {
private: //should never be called
    BinaryChunk(const BinaryChunk &) {}
    BinaryChunk& operator = (const BinaryChunk &) { return *this; }

public:
    BinaryChunk()
    : initialized(false)
    , pdata(NULL)
    , cdata(0)
    {}

    ~BinaryChunk() {
        Dispose();
    }

    HRESULT Allocate(jsize size) {
        Dispose();

        data.tymed = TYMED_HGLOBAL;
        data.hGlobal = ::GlobalAlloc(GALLOCFLG, size);
        if (!data.hGlobal)
            return E_OUTOFMEMORY;

        initialized = true;
        pdata = reinterpret_cast<jbyte *>(::GlobalLock(data.hGlobal));
        if (NULL != pdata) {
            cdata = jsize(::GlobalSize(data.hGlobal));
        }
        return S_OK;
    }

    HRESULT AllocateFromString(const _bstr_t &content) {
        OLE_DECL
        jsize size = content.length()*sizeof(wchar_t);
        OLE_HR = Allocate(size);
        if (SUCCEEDED(OLE_HR)) {
            memcpy(pdata, (const wchar_t *)content, size);
        }
        OLE_RETURN_HR
    }

    STGMEDIUM *Detach()
    {
        if (!initialized)
            return NULL;

        initialized = false;
        if (NULL != pdata) {
            ::GlobalUnlock(data.hGlobal);
            pdata = NULL;
            cdata = 0;
        }
        return &data;
    }

    HRESULT Load(
        IN IDataObject *p,
        IN CLIPFORMAT cf,
        IN jlong lindex = -1
    ) {
        Dispose();
        FORMATETC fmt = {
            cf,
            NULL,
            DVASPECT_CONTENT,
            LONG(lindex),
            TYMED_HGLOBAL};

        OLE_DECL
        OLE_HR = p->GetData(&fmt, &data);
        if (SUCCEEDED(OLE_HR)) {
            initialized = true;
            //ordinal treatment with direct conversion
            if (TYMED_HGLOBAL == data.tymed && NULL != data.hGlobal) {
                pdata = reinterpret_cast<jbyte *>(::GlobalLock(data.hGlobal));
                if (NULL != pdata) {
                    cdata = jsize(::GlobalSize(data.hGlobal));
                }
            }
        }
        OLE_RETURN_HR
    }

    inline bool isInternalAddress(const void *p, jsize size) const {
        return p >= pdata
            && ((jbyte *)p + size) <= (pdata + cdata);
    }

    inline bool isEmpty() const {
        return 0 == cdata;
    }

    void Dispose() {
        if (initialized) {
            if (NULL != pdata) {
                GlobalUnlock(data.hGlobal);
                pdata = NULL;
                cdata = 0;
            }
            ReleaseStgMedium(&data);
            initialized = false;
        }
        ZeroMemory(&data, sizeof(data));
    }

    inline jbyte *getMem() {
        return pdata;
    }

    inline _bstr_t getString() {
        static const _bstr_t empty;
        return isEmpty()
            ? empty
            : _bstr_t(::SysAllocStringLen(reinterpret_cast<const wchar_t *>(pdata), cdata/sizeof(wchar_t)));
    }

    inline jsize size() const {
        return cdata;
    }

private:
    jbyte    *pdata;
    jsize     cdata;
    bool      initialized;
    STGMEDIUM data;
};

HRESULT PopMemory(
    IN JNIEnv *env,
    IN CLIPFORMAT cf,
    IN jlong lindex,
    IN IDataObject *p,
    IN OUT jbyteArray *pret)
{
    OLE_DECL

    BinaryChunk me;
    OLE_HR = me.Load(p, cf, lindex);
    if (SUCCEEDED(OLE_HR) && !me.isEmpty()) {
        jsize offset = 0L;
        jlong cdata  = (jlong)me.size();
        if (CF_HDROP == cf) {
            offset = sizeof(DROPFILES);
            cdata -= (jlong)offset;
            DROPFILES *dropfiles = reinterpret_cast<DROPFILES *>(me.getMem());
            if (!dropfiles->fWide || cdata < 0) {
                //ASCII file names aren't supported
                //as well as corrupted format
                cdata = 0;
            }
        }
        if (0 != cdata) {
            *pret = env->NewByteArray((jsize)cdata);
            if (NULL != *pret) {
                env->SetByteArrayRegion(*pret, 0, (jsize)cdata, me.getMem() + offset);
            }
        }
    } else {
        *pret = NULL;
    }

    OLE_RETURN_HR
}

#define HIMETRIC_INCH   2540    // HIMETRIC units per inch
#define BSWAP_32(x) (((DWORD)(x) << 24) | \
    (((DWORD)(x) << 8) & 0xff0000) | \
    (((DWORD)(x) >> 8) & 0xff00) | \
    ((DWORD)(x)  >> 24))

HRESULT PopImage(
    IN JNIEnv *env,
    IN IDataObject *p,
    IN OUT jbyteArray *pret)
{
    //image extractor
    STRACE(_T("image extractor"));

    OLE_TRY
    IStoragePtr spStorage;
    OLE_HRT( ::StgCreateDocfile(
        NULL,
        STGM_READWRITE | STGM_SHARE_EXCLUSIVE | STGM_DIRECT | STGM_CREATE,
        NULL,
        &spStorage))

    IViewObject2Ptr view;
    OLE_HRT(::OleCreateStaticFromData(
        p,
        IID_IViewObject2,
        OLERENDER_DRAW,
        NULL,
        NULL,
        spStorage,
        (LPVOID *)&view))

    if (view) {
        SIZEL size = {0};
        IOleObjectPtr obj(view);
        //This method retrieves the display size in HIMETRIC units (0.01 millimeter per unit)
        OLE_HRT(obj->GetExtent(
            DVASPECT_CONTENT,
            &size))

        //Below OLE_HRT macro is forbidden because we have not auto-dispose wrappers over
        //the system handlers.
        HDC hMemoryDC = ::CreateCompatibleDC(NULL);
        if (!hMemoryDC) {
            OLE_REPORT_ERR(_T("CreateCompatibleDC"))
        } else {
            int cxPerInch = GetDeviceCaps(hMemoryDC, LOGPIXELSX);
            int cyPerInch = GetDeviceCaps(hMemoryDC, LOGPIXELSY);
            size.cx = MulDiv(size.cx, cxPerInch, HIMETRIC_INCH);
            size.cy = MulDiv(size.cy, cyPerInch, HIMETRIC_INCH);

            jbyte *pPoints = NULL;
            Bitmap bm(size.cx, size.cy, (void **)&pPoints, hMemoryDC);
            HBITMAP hBM = bm;
            if (!hBM) {
                OLE_REPORT_ERR(_T("CreateDIBSection"))
            } else {
                HBITMAP hOldBM = SelectBitmap(hMemoryDC, hBM);
                if (!hOldBM) {
                    OLE_REPORT_ERR(_T("SelectBitmap"))
                } else {
                    RECTL rc = {0, 0, size.cx, size.cy};
                    OLE_HR = view->Draw(
                        DVASPECT_CONTENT,
                        -1,
                        NULL,
                        NULL,
                        NULL,
                        hMemoryDC,
                        &rc,
                        &rc,
                        NULL,
                        0);
                    if (FAILED(OLE_HR)) {
                        STRACE(_T("view->Draw Error:%08x"), OLE_HR);
                    } else {
                        jsize cdata = jsize(size.cx) * jsize(size.cy) * 4 + 8;
                        *pret = env->NewByteArray(cdata);
                        if (NULL != *pret) {
                            DWORD w = BSWAP_32(size.cx);
                            DWORD h = BSWAP_32(size.cy);
                            env->SetByteArrayRegion(*pret, 0, 4, (jbyte *)&w);
                            env->SetByteArrayRegion(*pret, 4, 4, (jbyte *)&h);
                            env->SetByteArrayRegion(*pret, 8, cdata - 8, pPoints);
                        }
                    }
                    SelectBitmap(hMemoryDC, hOldBM);
                }
            }
            ::DeleteDC(hMemoryDC);
        }
        STRACE(_T("IViewObject size: %08x %08x"), size.cx, size.cy);
    }
    OLE_CATCH
    OLE_RETURN_HR
}

HRESULT PushImage(
    IN JNIEnv *env,
    IN jbyteArray data,
    IN OUT STGMEDIUM *psm)
{
    OLE_TRY
    jint cdata = env->GetArrayLength(data);
    if (cdata < 8) {
        OLE_HRT(E_INVALIDARG)
    }

    jint w, h;
    env->GetByteArrayRegion(data, 0, 4, (jbyte *)&w);
    env->GetByteArrayRegion(data, 4, 4, (jbyte *)&h);
    w = BSWAP_32(w);
    h = BSWAP_32(h);

    int numPixels = w*h;
    OLE_HRT(checkJavaException(env))
    if (cdata < (numPixels*4 + 8)) {
        OLE_HRT(E_INVALIDARG)
    }
    jbyte *pBytes;
    Bitmap bitmap(w, h, (void **)&pBytes);
    OLE_CHECK_NOTNULL((HBITMAP)bitmap)
    env->GetByteArrayRegion(data, 8, numPixels*4, pBytes);
    OLE_HRT(checkJavaException(env))

    psm->hGlobal = bitmap.GetGlobalDIB();
    psm->tymed = TYMED_HGLOBAL;
    OLE_CATCH
    OLE_RETURN_HR
}


class ClipboardData : public IUnknownImpl<IDataObject>
{
public:
    ClipboardData(JNIEnv *env, jobject clipboard, jstring name)
    : m_name(env, name),
      m_jclipboard(env->NewGlobalRef(clipboard))
    {
        STRACE(_T("{Clipboard %s"), (LPCWSTR)m_name);
    }

    virtual ~ClipboardData()
    {
        if (m_jclipboard) {
            JNIEnv* env = GetEnv();
            env->DeleteGlobalRef(m_jclipboard);
            m_jclipboard = NULL;
        }
        for (FMC2DATA::iterator i = m_fmc2data.begin(); m_fmc2data.end() != i; ++i) {
            ReleaseStgMedium(&i->second);
        }
        STRACE(_T("}Clipboard %s"), (LPCWSTR)m_name);
    }

    HRESULT pushCommit(JNIEnv *env, jobjectArray keys, jint supportedActions) {
        jint ckeys = env->GetArrayLength(keys);

        bool hasUrl = false;
        bool hasFileContent = false;
        bool hasIEShortcutName = false;
        static const STGMEDIUM empty_data = {0};
        for (jsize i = 0; i < ckeys; ++i) {
            JString mime(env, (jstring)env->GetObjectArrayElement(keys, i));
            static const size_t fcsSize = wcslen(MS_FILE_CONTENT);
            static const size_t gulSize = wcslen(GLASS_URI_LIST);
            static const size_t gulAFNSize = wcslen(GLASS_IE_URL_SHORTCUT_FILENAME);
            if (wcsncmp(MS_FILE_CONTENT, mime, fcsSize) == 0) {
                //File content transfer.
                //Need to be rewritten.
                hasFileContent = true;
            } else if (wcsncmp(GLASS_URI_LIST, mime, gulSize) == 0) {
                hasUrl = true;
            } else if (wcsncmp(GLASS_IE_URL_SHORTCUT_FILENAME, mime, gulAFNSize) == 0) {
                hasIEShortcutName = true;
                //that is the synthetic mime, it would be translated to
                //system pair MS_FILE_DESCRIPTOR_UNICODE/MS_FILE_CONTENT
                //below
                continue;
            }
            CLIPFORMAT cf = getClipboardFormat(mime);
            FORMATETC fmt = {
                cf,
                NULL,
                DVASPECT_CONTENT,
                -1L,
                TYMED_HGLOBAL};
            m_fmc2data[fmt] = empty_data;
            m_fmc2mime[fmt] = (LPCWSTR)mime;
        }

        //helpful extension for transferred data
        //to make JavaFX compatible with system applications
        if (!hasFileContent && hasUrl && hasIEShortcutName) {
            //prepare the shortcut for desktop Explorer
            static const FORMATETC fmtFileDescriptor = {
                getClipboardFormat(MS_FILE_DESCRIPTOR_UNICODE),
                NULL,
                DVASPECT_CONTENT,
                -1L,
                TYMED_HGLOBAL};
            //local per IDataObject substitution
            //MS_FILE_DESCRIPTOR_UNICODE->GLASS_IE_URL_SHORTCUT_FILENAME
            m_fmc2data[fmtFileDescriptor] = empty_data;
            m_fmc2mime[fmtFileDescriptor] = GLASS_IE_URL_SHORTCUT_FILENAME;

            static const FORMATETC fmtFileContent = {
                getClipboardFormat(MS_FILE_CONTENT),
                NULL,
                DVASPECT_CONTENT,
                0L,
                TYMED_HGLOBAL};
            //local per IDataObject substitution
            //MS_FILE_CONTENT->GLASS_IE_URL_SHORTCUT_CONTENT
            m_fmc2data[fmtFileContent] = empty_data;
            m_fmc2mime[fmtFileContent] = GLASS_IE_URL_SHORTCUT_CONTENT;
        }

        if (com_sun_glass_ui_win_WinSystemClipboard_ACTION_ANY != supportedActions) {
            BinaryChunk me;
            OLE_DECL
            OLE_HR = me.Allocate(sizeof(DROPEFFECT));
            if (FAILED(OLE_HR))
                return OLE_HR;

            *reinterpret_cast<DROPEFFECT *>(me.getMem()) = getDROPEFFECT(supportedActions);

            static const FORMATETC fmt = {
                getClipboardFormat(PREFERRED_DROP_EFFECT_MIME),
                NULL,
                DVASPECT_CONTENT,
                -1L,
                TYMED_HGLOBAL};

            m_fmc2data[fmt] = *me.Detach();
            m_fmc2mime[fmt] = PREFERRED_DROP_EFFECT_MIME;
        }
        return S_OK;
    }

    HRESULT checkMedium(FORMATETC *pformatetcIn, STGMEDIUM **ppmedium, _bstr_t *pmime) {
        if (NULL == pformatetcIn || NULL == ppmedium || NULL == pmime) {
            return E_POINTER;
        }
        FMC2DATA::iterator i = m_fmc2data.find(*pformatetcIn);
        if (m_fmc2data.end() == i) {
            FORMATETC fmt = {
                pformatetcIn->cfFormat,
                NULL,
                DVASPECT_CONTENT,
                pformatetcIn->lindex,
                TYMED_HGLOBAL};
            i = m_fmc2data.find(fmt);
            if (m_fmc2data.end() == i) {
                STRACE(_T("Decline Clipboard request for CF=%08x"), pformatetcIn->cfFormat);
                return DV_E_FORMATETC;
            }
        }
        *ppmedium = &i->second;
        *pmime = m_fmc2mime[i->first];//it should be registered
        STRACE(_T("Accept Clipboard request for CF=%08x [%s]"), pformatetcIn->cfFormat, (LPCTSTR)*pmime);
        return S_OK;
    }

    //IDataObject interface
    STDMETHOD(GetData)(FORMATETC *pformatetcIn, STGMEDIUM *pmedium)
    {
        if (NULL == pmedium || NULL == pformatetcIn) {
            return E_POINTER;
        }
        OLE_TRY
        STGMEDIUM *psm = NULL;
        _bstr_t mime;
        if (FAILED(OLE_HR = checkMedium(pformatetcIn, &psm, &mime))) {
            //that kind of fail is OK!
            OLE_RETURN_HR
        }

        //mime is here, but value by-demand
        if (0 == psm->tymed) {
            //that is synthetic mime, no direct Java callback with
            //GLASS_IE_URL_SHORTCUT_CONTENT mime!
            if (_bstr_t(GLASS_IE_URL_SHORTCUT_CONTENT) == mime) {
                //get URL from Java - it mandatory exists.
                //see also [pushCommit] implementation
                BinaryChunk urlUnicodeString;
                OLE_HRT(urlUnicodeString.Load(this, getClipboardFormat(GLASS_URI_LIST)))

                static const _bstr_t bsContentHeader(L"[InternetShortcut]\r\nURL=");
                BinaryChunk me;
                OLE_HRT(me.AllocateFromString(bsContentHeader + urlUnicodeString.getString()))
                *psm = *me.Detach();
            } else {
                //callback java
                JNIEnv *env = GetEnv();
                JLocalRef<jbyteArray> data(env, (jbyteArray)env->CallObjectMethod(
                    m_jclipboard, midFosSerialize,
                    jstring(JLString(env, CreateJString(env, (LPCWSTR)mime))),
                    jlong(pformatetcIn->lindex)));
                OLE_HRT(checkJavaException(env))
                OLE_CHECK_NOTNULL(data)

                if (CF_JAVA_BITMAP == pformatetcIn->cfFormat) {
                    OLE_HRT(PushImage(env, data, psm))
                } else {
                    jsize cdata = env->GetArrayLength(data);
                    BinaryChunk me;
                    if (_bstr_t(GLASS_IE_URL_SHORTCUT_FILENAME) == mime) {
                        OLE_HRT(me.Allocate(sizeof(FILEGROUPDESCRIPTORW)))
                        FILEGROUPDESCRIPTORW *fgd = reinterpret_cast<FILEGROUPDESCRIPTORW *>(me.getMem());
                        //FILEGROUPDESCRIPTORW reserve exactly one file entry
                        ZeroMemory(fgd, sizeof(FILEGROUPDESCRIPTORW));
                        fgd->cItems = 1;
                        fgd->fgd->dwFlags = FD_UNICODE | FD_FILESIZE
                            | FD_CREATETIME | FD_ACCESSTIME | FD_WRITESTIME;

                        size_t len = cdata/sizeof(wchar_t) + 1;
                        MemHolder<wchar_t> shortcutName(len);
                        wchar_t *name = shortcutName.get();
                        env->GetByteArrayRegion(data, 0, cdata, reinterpret_cast<jbyte *>(name));

                        //file name validation
                        name[len-1] = 0;
                        for (wchar_t *cur = name; *cur; ++cur)
                            if (wcschr(L"|\\?*<\"\':>+[]/", *cur) != NULL)
                                name = cur + 1;
                        //[name] points to the last valid for NTSF/VFAT subsequence of chars or it is empty
                        //http://en.wikipedia.org/wiki/Filename
                        if (*name == 0) {
                            OLE_HRT(E_INVALIDARG)
                        }
                        if (wcslen(name) > (MAX_PATH - 5)) {
                            name[MAX_PATH - 5] = 0;
                        }
                        wchar_t *name_in = fgd->fgd->cFileName;
                        wcscpy_s(name_in, MAX_PATH, name);

                        //check [.url] extension
                        wchar_t *ext_in = name_in + wcslen(name_in) - 4;
                        static wchar_t *urlExt = L".url";
                        if (ext_in < name_in || _wcsnicmp(urlExt, ext_in, 4) != 0)
                            wcscat_s(name_in, MAX_PATH, urlExt);

                        //get file size
                        BinaryChunk fileContent;
                        //for local IDataObject:
                        // [MS_FILE_CONTENT-mime]->[CF-word]->[GLASS_IE_URL_SHORTCUT_CONTENT-mime]
                        //[lindex] parameter need to be zero (the first and the only array item)
                        //see also [pushCommit] implementation
                        OLE_HRT(fileContent.Load(this, getClipboardFormat(MS_FILE_CONTENT), 0i64))
                        fgd->fgd->nFileSizeLow = fileContent.size();

                        //set file times
                        FILETIME ft;
                        SYSTEMTIME st;
                        GetSystemTime(&st);// Gets the current system time
                        SystemTimeToFileTime(&st, &ft);
                        fgd->fgd->ftCreationTime =
                            fgd->fgd->ftLastAccessTime =
                                fgd->fgd->ftLastWriteTime = ft;
                    } else if (CF_HDROP == pformatetcIn->cfFormat) {
                        OLE_HRT(me.Allocate(sizeof(DROPFILES) + cdata))
                        DROPFILES *dropfiles = reinterpret_cast<DROPFILES *>(me.getMem());
                        ZeroMemory(dropfiles, sizeof(DROPFILES));
                        dropfiles->pFiles = sizeof(DROPFILES);
                        dropfiles->fWide = TRUE;
                        env->GetByteArrayRegion(data, 0, cdata, me.getMem() + dropfiles->pFiles);
                    } else {
                        OLE_HRT(me.Allocate(cdata))
                        env->GetByteArrayRegion(data, 0, cdata, me.getMem());
                    }
                    //cache the mime-value
                    *psm = *me.Detach();
                }//not an image
            }//Java data
        }
        *pmedium = *psm;
        //[POSTPONED RELEASE]
        //no owner => [this] gets the ownership
        if (NULL == pmedium->pUnkForRelease)
            pmedium->pUnkForRelease = this;

        //protect the owner, till caller needs the resource
        pmedium->pUnkForRelease->AddRef();

        //external system [STGMEDIUM]-entities need protection from deallocation
        //http://msdn.microsoft.com/en-us/library/windows/desktop/ms693491%28v=vs.85%29.aspx
        if (TYMED_ISTREAM == psm->tymed)
            pmedium->pstm->AddRef();
        else if (TYMED_ISTORAGE == psm->tymed)
            pmedium->pstm->AddRef();
        OLE_CATCH
        OLE_RETURN_HR
    }

    STDMETHOD(GetDataHere)(FORMATETC *pformatetcIn, STGMEDIUM *pmedium)
    {
        if (NULL == pformatetcIn || NULL == pmedium) {
            return E_POINTER;
        }
        STGMEDIUM sm = {0};
        OLE_TRY
        OLE_HRT(GetData(pformatetcIn, &sm))

        //let's create independent copy of the resource (without ownership)
        IUnknown *p = sm.pUnkForRelease;
        sm.pUnkForRelease = NULL;
        OLE_HRT(CopyStgMedium(&sm, pmedium))
        sm.pUnkForRelease = p;
        ReleaseStgMedium(&sm);
        OLE_CATCH
        OLE_RETURN_HR
    }

    STDMETHOD(QueryGetData)(FORMATETC* pformatetc)
    {
        STGMEDIUM *psm = NULL;
        _bstr_t mime;
        return checkMedium(pformatetc, &psm, &mime);
    }

    STDMETHOD(GetCanonicalFormatEtc)(FORMATETC *pformatectIn, FORMATETC *pformatetcOut)
    {
        if (NULL == pformatectIn || NULL == pformatetcOut) {
            return E_POINTER;
        }
        STGMEDIUM *psm = NULL;
        _bstr_t mime;
        HRESULT hr = checkMedium(pformatectIn, &psm, &mime);
        if (S_OK == hr) {
            *pformatetcOut = *pformatectIn;
            hr = DATA_S_SAMEFORMATETC;
        }
        return hr;
    }

    STDMETHOD(SetData)(FORMATETC *pformatetc, STGMEDIUM *pmedium, BOOL fRelease)
    {
        //System calls this method to store additional information
        //about the drag (like specially prepared system drag image).
        if (NULL == pformatetc || NULL == pmedium) {
            return E_POINTER;
        }
        //Reject unsafe transfer type TYMED_FILE. Canonical treatment procedure
        //includes "frees the disk file by deleting it" call:
        //http://msdn.microsoft.com/en-us/library/windows/desktop/ms693491%28v=vs.85%29.aspx
        //We don't like to participate in that kind of communication.
        if (TYMED_FILE == pmedium->tymed) {
            return E_NOTIMPL;
        }

        STGMEDIUM sm = {0};
        OLE_TRY

        if (!fRelease) {
            //We cannot get the ownership under the [pmedium].
            //Call [CopyStgMedium] can increment ref for [pUnkForRelease],
            //or make a deep copy. Both ways are acceptable.
            OLE_HRT(CopyStgMedium(pmedium, &sm))
            pmedium = &sm;
        }

        FMC2DATA::iterator i = m_fmc2data.find(*pformatetc);
        if (m_fmc2data.end() != i) {
            //mime already exists - only update
            ReleaseStgMedium(&i->second);
            i->second = *pmedium;
        } else {
            //new entry
            m_fmc2data[*pformatetc] = *pmedium;
            //lazy cf->mime decoding
            m_fmc2mime[*pformatetc] = getMime(pformatetc->cfFormat);
        }

        static const CLIPFORMAT cf = getClipboardFormat(PERFORMED_DROP_EFFECT_MIME);
        if (pformatetc->cfFormat == cf && TYMED_HGLOBAL == pformatetc->tymed) {
            OLE_CHECK_NOTNULL(pmedium->hGlobal)
            DROPEFFECT *pDF = reinterpret_cast<DROPEFFECT *>(::GlobalLock(pmedium->hGlobal));
            if (NULL != pDF && ::GlobalSize(pmedium->hGlobal) >= sizeof(DROPEFFECT)) {
                GetEnv()->CallVoidMethod(m_jclipboard, midActionPerformed, getACTION(*pDF));
                OLE_HRT(checkJavaException(GetEnv()));
            }
            GlobalUnlock(pmedium->hGlobal);
        }
        OLE_CATCH
        OLE_RETURN_HR
    }

    STDMETHOD(EnumFormatEtc)(DWORD dwDirection, IEnumFORMATETC **ppenumFormatEtc)
    {
        if (NULL == ppenumFormatEtc) {
            return E_POINTER;
        }
        if (DATADIR_SET == dwDirection) {
            return E_NOTIMPL;
        }
        *ppenumFormatEtc = new ClipboardEnumFORMATETC(this);
        return S_OK;
    }

    STDMETHOD(DAdvise)(FORMATETC *pformatetc, DWORD advf, IAdviseSink *pAdvSink,
        DWORD *pdwConnection)
    {
        OLE_TRY
        if (!bool(m_spDataAdviseHolder)) {
            OLE_HRT(CreateDataAdviseHolder(&m_spDataAdviseHolder))
        }
        OLE_HRT(m_spDataAdviseHolder->Advise((IDataObject*)this, pformatetc, advf, pAdvSink, pdwConnection))
        OLE_CATCH
        OLE_RETURN_HR
    }

    STDMETHOD(DUnadvise)(DWORD dwConnection)
    {
        OLE_TRY
        if (!bool(m_spDataAdviseHolder))
            return OLE_E_NOCONNECTION;
        OLE_HRT(m_spDataAdviseHolder->Unadvise(dwConnection))
        OLE_CATCH
        OLE_RETURN_HR
    }

    STDMETHOD(EnumDAdvise)(IEnumSTATDATA **ppenumAdvise)
    {
        if (ppenumAdvise == NULL)
            return E_POINTER;
        *ppenumAdvise = NULL;
        if (m_spDataAdviseHolder)
            return m_spDataAdviseHolder->EnumAdvise(ppenumAdvise);
        return E_FAIL;
    }

protected:
    IDataAdviseHolderPtr m_spDataAdviseHolder;
    JString m_name;
    jobject m_jclipboard;
    FMC2MIME m_fmc2mime;
    FMC2DATA m_fmc2data;

    class ClipboardEnumFORMATETC: public IUnknownImpl<IEnumFORMATETC>
    {
    private:
        ClipboardEnumFORMATETC(ClipboardData *owner, FMC2DATA::iterator pos)
        : m_owner(owner),
          m_pos(pos)
        {
            m_owner->AddRef();
        }

    public:
        ClipboardEnumFORMATETC(ClipboardData *owner)
        : m_owner(owner)
        {
            m_owner->AddRef();
            Reset();
        }


        virtual ~ClipboardEnumFORMATETC() {
            m_owner->Release();
        }

        STDMETHOD(Next)(ULONG celt, FORMATETC *rgelt, ULONG *pceltFetched) {
            ULONG i = 0;
            for (; i < celt && m_owner->m_fmc2data.end() != m_pos ; ++i, ++m_pos) {
                *rgelt++ = m_pos->first;
            }
            if (pceltFetched) {
                *pceltFetched = i;
            }
            return (i == celt)
                ? S_OK
                : S_FALSE;

        }

        STDMETHOD(Skip)(ULONG celt) {
            ULONG i = 0;
            for (; i < celt && m_owner->m_fmc2data.end() != m_pos ; ++i, ++m_pos) ;
            return (i == celt)
                ? S_OK
                : S_FALSE;
        }

        STDMETHOD(Reset)() {
            m_pos = m_owner->m_fmc2data.begin();
            return S_OK;
        }

        STDMETHOD(Clone)(IEnumFORMATETC **ppenum) {
            *ppenum = new ClipboardEnumFORMATETC(m_owner, m_pos);
            return S_OK;
        }
   protected:
        ClipboardData *m_owner;
        FMC2DATA::iterator m_pos;
    };
};

extern "C" {

/*
* Class:     com_sun_glass_ui_win_WinSystemClipboard
* Method:    initIDs
* Signature: ()V
*/
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinSystemClipboard_initIDs
    (JNIEnv *env, jclass cls)
{
    fidPtr = env->GetFieldID(cls, "ptr", "J");
    fidName = env->GetFieldID(cls, "name", "Ljava/lang/String;");
    midFosSerialize = env->GetMethodID(cls, "fosSerialize", "(Ljava/lang/String;J)[B");
    midContentChanged = env->GetMethodID(cls, "contentChanged", "()V");
    midActionPerformed = env->GetMethodID(cls, "actionPerformed", "(I)V");
}

/*
* Class:     com_sun_glass_ui_win_WinSystemClipboard
* Method:    isOwner
* Signature: ()Z
*/
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinSystemClipboard_isOwner
    (JNIEnv *env, jobject obj)
{
    ENTER_MAIN_THREAD_AND_RETURN(jboolean)
    {
        if (NULL == p) {
            return false;
        }
        return S_OK == ::OleIsCurrentClipboard(p);
    }
    LEAVE_MAIN_THREAD_WITH_p;

    return PERFORM_AND_RETURN();
}

/*
 * Class:     com_sun_glass_ui_win_WinSystemClipboard
 * Method:    create
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinSystemClipboard_create
  (JNIEnv *env, jobject obj)
{
    ENTER_MAIN_THREAD()
    {
        GlassApplication::GetInstance()->RegisterClipboardViewer(obj);
    }
    DECL_jobject(obj);
    LEAVE_MAIN_THREAD;

    ARG(obj) = obj;
    PERFORM();
}

void OLE_CoPump()
{
    MSG msg;
    while (::PeekMessage(&msg,NULL,0,0,PM_REMOVE)) {
        ::TranslateMessage(&msg);
        ::DispatchMessage(&msg);
    }
}

/*
* Class:     com_sun_glass_ui_win_WinSystemClipboard
* Method:    dispose
* Signature: ()V
*/
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinSystemClipboard_dispose
    (JNIEnv *env, jobject obj)
{
    ENTER_MAIN_THREAD()
    {
        GlassApplication::GetInstance()->UnregisterClipboardViewer();
        if (NULL != p) {
            OLE_TRY
            OLE_HRT( ::OleIsCurrentClipboard(p) )
            if (S_OK == OLE_HR) {
                for (int i = 0; i < 1000; ++i) {
                    OLE_HR = ::OleFlushClipboard();
                    if (CLIPBRD_E_CANT_OPEN == OLE_HR) {
                        OLE_CoPump();
                        continue;
                    }
                    break;
                }
            }
            OLE_CATCH
            p->Release();
            STRACE(_T("System Clipboard Closed"));
        }
    }
    LEAVE_MAIN_THREAD_WITH_p;
    PERFORM();
}


/*
 * Class:     com_sun_glass_ui_win_WinSystemClipboard
 * Method:    push
 * Signature: ([Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinSystemClipboard_push
  (JNIEnv *env, jobject obj, jobjectArray keys, jint supportedActions)
{
    ENTER_MAIN_THREAD()
    {
        OLE_TRY
            if (NULL != p) {
                //We need to create new object here due to [POSTPONED RELEASE] algorithm
                //in data provider.
                p->Release();
            }
            JNIEnv* env = GetEnv();
            ClipboardData *pcd = new ClipboardData(
                env,
                obj,
                JLString(env, (jstring)env->GetObjectField(obj, fidName)));
            setPtr(env, obj, pcd);
            OLE_HRT( pcd->pushCommit(env, keys, supportedActions) )
            OLE_HRT( ::OleSetClipboard(pcd) )
        OLE_CATCH
    }
    DECL_jobject(obj);
    DECL_JREF(jobjectArray, keys);
    jint supportedActions;
    LEAVE_MAIN_THREAD_WITH_p;

    ARG(obj) = obj;
    ARG(keys) = keys;
    ARG(supportedActions) = supportedActions;
    PERFORM();
}

/*
 * Class:     com_sun_glass_ui_win_WinSystemClipboard
 * Method:    pop
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_glass_ui_win_WinSystemClipboard_pop
  (JNIEnv *env, jobject obj)
{
    ENTER_MAIN_THREAD_AND_RETURN(jboolean)
    {
        if (p) {
            p->Release();
        }
        p = SUCCEEDED( ::OleGetClipboard(&p) ) ? p : NULL;
        JNIEnv* env = GetEnv();
        setPtr(env, obj, p);
        return NULL != p;
    }
    DECL_jobject(obj);
    LEAVE_MAIN_THREAD_WITH_p;
    ARG(obj) = obj;
    return PERFORM_AND_RETURN();
}

/*
* Class:     com_sun_glass_ui_win_WinSystemClipboard
* Method:    popBytes
* Signature: (Ljava/lang/String;J)[B
*/
JNIEXPORT jbyteArray JNICALL Java_com_sun_glass_ui_win_WinSystemClipboard_popBytes
    (JNIEnv *env, jobject obj, jstring jmime, jlong lindex)
{
    ENTER_MAIN_THREAD_AND_RETURN(jbyteArray)
    {
        //So we are here if we are not the owners of the clipboard
        jbyteArray ret = NULL;
        if (NULL != p) {
            OLE_DECL
            JNIEnv* env = GetEnv();
            JString mime(env, jmime);
            if (0 == wcscmp(mime, GLASS_IMAGE)) {
                //custom conversion for image
                OLE_HR = ::OleQueryCreateFromData(p);
                // http://msdn.microsoft.com/en-us/library/windows/desktop/ms683739%28v=vs.85%29.aspx
                // "If OleQueryCreateFromData finds one of the other formats (CF_EMBEDDEDOBJECT,
                // CF_EMBEDSOURCE, or cfFileName), !*even in combination with the static formats*!,
                // it returns S_OK, indicating that you should call the OleCreateFromData
                // function to create the embedded object."

                // We do not like CF_EMBEDXXXX, but we want CF_METAFILEPICT, CF_DIB, CF_BITMAP.
                // Make a try!
                if (OLE_S_STATIC == OLE_HR || S_OK == OLE_HR) {
                    //We don't like to report error. Maybe only CF_EMBEDXXXX types are present.
                    OLE_HR = PopImage(env, p, &ret);
                }
            } else {
                //We don't like to report error. Fail is ordinal here.
                OLE_HR = PopMemory(
                    env,
                    getClipboardFormat(mime),
                    lindex,
                    p,
                    &ret);
            }
        }
        return ret;
    }
    DECL_JREF(jstring, jmime);
    jlong lindex;
    LEAVE_MAIN_THREAD_WITH_p;

    ARG(jmime) = jmime;
    ARG(lindex) = lindex;
    return PERFORM_AND_RETURN();
}

/*
* Class:     com_sun_glass_ui_win_WinSystemClipboard
* Method:    mimesFromSystem
* Signature: ()Ljava/util/Set;
*/
JNIEXPORT jobjectArray JNICALL Java_com_sun_glass_ui_win_WinSystemClipboard_popMimesFromSystem
    (JNIEnv *env, jobject obj)
{
    ENTER_MAIN_THREAD_AND_RETURN(jobjectArray)
    {
        //So we are here if we are not the owners of the clipboard
        jobjectArray ret = NULL;
        if (NULL != p) {
            OLE_TRY
            IEnumFORMATETCPtr pos;
            OLE_HRT(p->EnumFormatEtc(DATADIR_GET, &pos))
            FORMATETC fmc;
            HASH_STR_SET mimes;
            while (S_OK == pos->Next(1, &fmc, NULL)) {
                if (TYMED_HGLOBAL & fmc.tymed) {
                    _bstr_t mime = getMime(fmc.cfFormat);
                    if (mime.length()) {
                        if (_bstr_t(GLASS_URI_LIST_LOCALE) == mime) {
                            //we can convert it to the URL list
                            mimes.insert(GLASS_URI_LIST);
                        } else if (_bstr_t(GLASS_TEXT_PLAIN_LOCALE) == mime){
                            //we can convert it to the text
                            mimes.insert(GLASS_TEXT_PLAIN);
                        } else {
                            mimes.insert(mime);
                        }
                    }
                    if (CF_HDROP == fmc.cfFormat) {
                        //we can convert it to the URL list
                        mimes.insert(GLASS_URI_LIST);
                    }
                }
            }
            if (OLE_S_STATIC==::OleQueryCreateFromData(p)) {
                //we can convert it to the image
                mimes.insert(GLASS_IMAGE);
            }

            if (mimes.end() != mimes.find(MS_FILE_DESCRIPTOR_UNICODE)
               || mimes.end() != mimes.find(MS_FILE_DESCRIPTOR))
            {//MS stuff formats post processing.
                static const CLIPFORMAT stuffFormas[] = {
                    getClipboardFormat(MS_FILE_DESCRIPTOR_UNICODE),
                    getClipboardFormat(MS_FILE_DESCRIPTOR)
                };
                bool bContinue = true;

                for (int i = 0; i < 2 && bContinue; ++i) {
                    //FILEGROUPDESCRIPTORW for MS_FILE_DESCRIPTOR_UNICODE
                    //FILEGROUPDESCRIPTORA for MS_FILE_DESCRIPTOR
                    jsize headerSize = (0 == i)
                        ? sizeof(FILEGROUPDESCRIPTORW)
                        : sizeof(FILEGROUPDESCRIPTORA);

                    jsize itemSize = (0 == i)
                        ? sizeof(FILEDESCRIPTORW)
                        : sizeof(FILEDESCRIPTORA);

                    BinaryChunk me;
                    OLE_HR = me.Load(p, stuffFormas[i]);
                    if (SUCCEEDED(OLE_HR) && me.size() >= headerSize) {
                        //LPFILEGROUPDESCRIPTORW for MS_FILE_DESCRIPTOR_UNICODE
                        //LPFILEGROUPDESCRIPTORA for MS_FILE_DESCRIPTOR
                        LPFILEGROUPDESCRIPTORW pdata = reinterpret_cast<LPFILEGROUPDESCRIPTORW>(me.getMem());
                        if (pdata->cItems > 0) {
                            mimes.erase(MS_FILE_CONTENT);
                            mimes.erase(MS_FILE_DESCRIPTOR_UNICODE);
                            mimes.erase(MS_FILE_DESCRIPTOR);
                            for (UINT k = 0; k < pdata->cItems; ++k) {
                                WCHAR buffer[64];
                                _bstr_t bsId;

                                _itow_s(k, buffer, 64, 10);
                                bsId += _bstr_t(L";index=") + buffer;

                                //binary part is the same for ASCII and Unicode versions
                                const FILEDESCRIPTORW &fd = (0==i)
                                    ? pdata->fgd[k]
                                    : reinterpret_cast<const FILEDESCRIPTORW &>(reinterpret_cast<LPFILEGROUPDESCRIPTORA>(pdata)->fgd[k]);

                                if (!me.isInternalAddress(&fd, itemSize)) {
                                    OLE_HRT(E_INVALIDARG)
                                }

                                if (fd.dwFlags & FD_FILESIZE) {
                                    CY t;
                                    t.Lo = fd.nFileSizeLow;
                                    t.Hi = fd.nFileSizeHigh;
                                    _i64tow_s(t.int64, buffer, 64, 10);
                                    bsId += _bstr_t(L";size=") + buffer;
                                }

                                if (fd.dwFlags & FD_CLSID) {
                                    LPOLESTR pCOMid;
                                    OLE_HRT(::StringFromIID(fd.clsid, &pCOMid))
                                    bsId += _bstr_t(L";clsid=") + pCOMid;
                                    ::CoTaskMemFree(pCOMid);
                                }

                                //it is safe to have the name at the end
                                bsId += L";name=\"";
                                bsId += (0==i)
                                    ? _bstr_t(pdata->fgd[k].cFileName)
                                    : _bstr_t(reinterpret_cast<LPFILEGROUPDESCRIPTORA>(pdata)->fgd[k].cFileName);
                                bsId += "\"";

                                //RFC 1521 extension for [message/external-body] mime
                                static const _bstr_t bsAcessType(L";access-type=clipboard");
                                mimes.insert(MS_FILE_CONTENT + bsAcessType + bsId);
                            }
                            //stop on the first success
                            bContinue = false;
                        }
                    }
                }
            }

            jsize cmimes = jsize(mimes.size());
            if (cmimes) {
                JNIEnv * env = GetEnv();
                ret = env->NewObjectArray(
                    cmimes,
                    JLClass(env, env->FindClass("java/lang/String")),
                    NULL);
                if (ret) {
                    jsize index = 0;
                    for (HASH_STR_SET::const_iterator i = mimes.begin(); mimes.end() != i; ++i, ++index) {
                        env->SetObjectArrayElement(ret, index,
                            jstring(JLString(env,
                                CreateJString(env,(LPCWSTR)*i)
                            ))
                        );
                    }
                }
            }
            OLE_CATCH
        }
        return ret;
    }
    LEAVE_MAIN_THREAD_WITH_p;

    return PERFORM_AND_RETURN();
}

//The basic procedure for a delete-on-paste operation is as follows:
//1. The source marks the screen display of the selected data.
//2. The source creates a data object. It indicates a cut operation by adding the
//   CFSTR_PREFERREDDROPEFFECT format with a data value of DROPEFFECT_MOVE.
//3. The source places the data object on the Clipboard using OleSetClipboard.
//4. The target retrieves the data object from the Clipboard using OleGetClipboard.
//5. The target extracts the CFSTR_PREFERREDDROPEFFECT data. If it is set to only
//   DROPEFFECT_MOVE, the target can either do an optimized move or simply copy the data.
//6. If the target does not do an optimized move, it calls the IDataObject::SetData
//   method with the CFSTR_PERFORMEDDROPEFFECT format set to DROPEFFECT_MOVE.
//7. When the paste is complete, the target calls the IDataObject::SetData method
//   with the CFSTR_PASTESUCCEEDED format set to DROPEFFECT_MOVE.
//8. When the source's IDataObject::SetData method is called with
//   the CFSTR_PASTESUCCEEDED format set to DROPEFFECT_MOVE, it must check to see
//   if it also received the CFSTR_PERFORMEDDROPEFFECT format set to DROPEFFECT_MOVE.
//   [!IF BOTH FORMATS ARE SENT BY THE TARGET!], the source will have to delete the data.
//
//If only the CFSTR_PASTESUCCEEDED format is received, the source can simply remove the data
//from its display. If the transfer fails, the source updates the display to its original
//appearance.
//(c) http://msdn.microsoft.com/en-us/library/bb776904%28VS.85%29.aspx

/*
 * Class:     com_sun_glass_ui_win_WinSystemClipboard
 * Method:    pushTargetActionToSystem
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinSystemClipboard_pushTargetActionToSystem
  (JNIEnv *env, jobject obj, jint actionDone)
{
    ENTER_MAIN_THREAD()
    {
        //please, read: http://msdn.microsoft.com/en-us/library/bb776904%28VS.85%29.aspx
        if (NULL != p) {
            //Make it in one step!
            static const CLIPFORMAT stuffFormas[] = {
                getClipboardFormat(PASTE_SUCCEEDED),
                getClipboardFormat(PERFORMED_DROP_EFFECT_MIME)
            };
            OLE_TRY
            for (int i = 0; i < 2; ++i) {
                FORMATETC fmt = {
                    stuffFormas[i],
                    NULL,
                    DVASPECT_CONTENT,
                    -1L,
                    TYMED_HGLOBAL};

                BinaryChunk me;
                OLE_HRT(me.Allocate(sizeof(DROPEFFECT)))
                *reinterpret_cast<DROPEFFECT *>(me.getMem()) = getDROPEFFECT(actionDone);
                OLE_HRT(p->SetData(&fmt, me.Detach(), TRUE))
            }
            OLE_CATCH
        }
    }
    jint actionDone;
    LEAVE_MAIN_THREAD_WITH_p;

    ARG(actionDone) = actionDone;
    PERFORM();
}

/*
 * Class:     com_sun_glass_ui_win_WinSystemClipboard
 * Method:    popSupportedActionFromSystem
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_sun_glass_ui_win_WinSystemClipboard_popSupportedSourceActions
  (JNIEnv *env, jobject obj)
{
    ENTER_MAIN_THREAD_AND_RETURN(jint)
    {
        //please, read: http://msdn.microsoft.com/en-us/library/bb776904%28VS.85%29.aspx
        //So we are here if we are not the owners of the clipboard
        jint ret = com_sun_glass_ui_win_WinSystemClipboard_ACTION_NONE;
        if (NULL != p) {
            OLE_DECL
            BinaryChunk me;
            OLE_HR = me.Load(p, getClipboardFormat(PREFERRED_DROP_EFFECT_MIME));
            ret = (FAILED(OLE_HR) || me.size() < sizeof(DROPEFFECT))
                ? com_sun_glass_ui_win_WinSystemClipboard_ACTION_ANY
                : getACTION(*reinterpret_cast<DROPEFFECT *>(me.getMem()));
        }
        return ret;
    }
    LEAVE_MAIN_THREAD_WITH_p;
    return PERFORM_AND_RETURN();
}

//////////////////////////////////////////////////////////////////////////
//WinDnDClipboard
//////////////////////////////////////////////////////////////////////////


/*
 * Class:     com_sun_glass_ui_win_WinDnDClipboard
 * Method:    dispose
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinDnDClipboard_dispose
  (JNIEnv *env, jobject obj)
{
    ENTER_MAIN_THREAD()
    {
        if (NULL != p) {
            p->Release();
            STRACE(_T("Dnd Clipboard Closed"));
        }
    }
    LEAVE_MAIN_THREAD_WITH_p;
    PERFORM();
}

HRESULT setDragImage(IDataObject *p)
{
    OLE_TRY

    BaseBitmap bm;
    DWORD w = 0;
    DWORD h = 0;

    BinaryChunk me;
    static const CLIPFORMAT cfDImage = getClipboardFormat(GLASS_IMAGE_DRAG);
    if (SUCCEEDED(me.Load(p, cfDImage))) {
        static const jsize header_size = sizeof(jint)*2/sizeof(jbyte);
        if (me.size() < header_size)
            return E_INVALIDARG;

        w = reinterpret_cast<jint *>(me.getMem())[0];
        h = reinterpret_cast<jint *>(me.getMem())[1];
        w = BSWAP_32(w);
        h = BSWAP_32(h);

        jsize bmpSize = w*h*4;
        if (me.size() < jsize(header_size + bmpSize))
            return E_INVALIDARG;

        bm.Attach(CreateBitmap(w, h, 1, 32, me.getMem() + header_size));
    } if (SUCCEEDED(me.Load(p, CF_JAVA_BITMAP))) {
        //that entry was prepared by [pushImage] call (BaseBitmap::GetGlobalDIB()),
        //so it is 4-bytes DIB image with reversed scan line sequence.
        //It cannot be changed due to compatibility reason (Wordpad)
        if (me.size() < sizeof(BITMAPINFOHEADER))
            return E_INVALIDARG;

        const LPBITMAPINFO lpbi = reinterpret_cast<LPBITMAPINFO>(me.getMem());
        w = abs(lpbi->bmiHeader.biWidth);
        h = abs(lpbi->bmiHeader.biHeight);

        jsize bmpSize = w*h*4;
        if (me.size() < jsize(bmpSize + lpbi->bmiHeader.biSize))
            return E_INVALIDARG;

        //reverse rows order
        MemHolder<jbyte> rows(bmpSize);
        jbyte *d = rows;
        jbyte *de = d + bmpSize;
        jsize lineSize = w*4;
        jbyte *s = me.getMem() + lpbi->bmiHeader.biSize + bmpSize - lineSize;
        while (d < de) {
            memcpy(d, s, lineSize);
            d += lineSize;
            s -= lineSize;
        }

        bm.Attach(CreateBitmap(w, h, 1, 32, (jbyte *)rows));
    }


    if (bm) {
        DWORD offsetX = w/2;
        DWORD offsetY = h/2;

        static const CLIPFORMAT cfDImageOffset = getClipboardFormat(GLASS_IMAGE_DRAG_OFFSET);
        if (SUCCEEDED(me.Load(p, cfDImageOffset))) {
            static const jsize header_size = sizeof(jint)*2/sizeof(jbyte);
            if (me.size() < header_size)
                return E_INVALIDARG;
            offsetX = reinterpret_cast<jint *>(me.getMem())[0];
            offsetY = reinterpret_cast<jint *>(me.getMem())[1];
            offsetX = BSWAP_32(offsetX);
            offsetY = BSWAP_32(offsetY);
        }

        SHDRAGIMAGE sdi;
        sdi.sizeDragImage.cx = w;
        sdi.sizeDragImage.cy = h;
        sdi.ptOffset.x = offsetX;
        sdi.ptOffset.y = offsetY;
        sdi.crColorKey = 0xFFFFFFFF;
        sdi.hbmpDragImage = bm;

        IDragSourceHelperPtr spHelper;
        OLE_HRT(::CoCreateInstance(
            CLSID_DragDropHelper,
            NULL,
            CLSCTX_ALL,
            IID_IDragSourceHelper,
            (LPVOID*)&spHelper))
        OLE_HRT(spHelper->InitializeFromBitmap(
            &sdi, p))
    }

    OLE_CATCH
    OLE_RETURN_HR
}

/*
 * Class:     com_sun_glass_ui_win_WinDnDClipboard
 * Method:    push
 * Signature: ([Ljava/lang/Object;I)V
 */
JNIEXPORT void JNICALL Java_com_sun_glass_ui_win_WinDnDClipboard_push
  (JNIEnv *env, jobject obj, jobjectArray keys, jint supportedActions)
{
    ENTER_MAIN_THREAD()
    {
        DWORD performedDropEffect = DROPEFFECT_MOVE;
        JNIEnv * env = GetEnv();
        OLE_TRY
        if (NULL != p) {
            //We need to create new object here due to [POSTPONED RELEASE] algorithm
            //in data provider.
            p->Release();
            STRACE(_T("Alarm Dnd Clipboard Release"));
        }
        ClipboardData *pcd = new ClipboardData(
            env,
            obj,
            JLString(env, (jstring)env->GetObjectField(obj, fidName)));
        setPtr(env, obj, pcd);
        //from now 'pcd' would be destroyed on dispose

        OLE_HRT( pcd->pushCommit(env, keys, supportedActions) )

        //here is the drag image setup
        //we are not interested in return value
        //pictured drag is not a primary functionality
        setDragImage(pcd);


        STRACE(_T("{DoDragDrop %08x"), getDROPEFFECT(supportedActions));
        OLE_HRT( ::DoDragDrop(
            pcd,
            IDropSourcePtr(new GlassDropSource(obj), false),
            getDROPEFFECT(supportedActions),
            &performedDropEffect) )
        OLE_CATCH
        env->CallVoidMethod(obj, midActionPerformed,
            getACTION(SUCCEEDED(OLE_HR) ? performedDropEffect : DROPEFFECT_NONE));
        CheckAndClearException(env);
        GlassDropSource::SetDragButton(0);
        STRACE(_T("}DoDragDrop effect:%08x result:%08x"), performedDropEffect, OLE_HR);
    }
    DECL_jobject(obj);
    DECL_JREF(jobjectArray, keys);
    jint supportedActions;
    LEAVE_MAIN_THREAD_WITH_p;

    ARG(obj) = obj;
    ARG(keys) = keys;
    ARG(supportedActions) = supportedActions;
    PERFORM();
}

}
