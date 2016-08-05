#ifndef _DBGUTILS_H
#define _DBGUTILS_H

#if (defined(WIN32) || defined(_WIN32)) && (defined(DEBUG) || defined(_DEBUG))
//#define __RQ_LOG
//Please, read more at "Finding Memory Leaks Using the CRT Library":
// http://msdn.microsoft.com/en-us/library/x98tx3cf.aspx
#define _CRTMAP_ALLOC
#include <windows.h>
#include <comdef.h>
#include <comutil.h>
#include <crtdbg.h>

// A part of MSDN:
// For diagnostic purpose, blocks are allocated with extra information and
// stored in a doubly-linked list.  This makes all blocks registered with
// how big they are, when they were allocated, and what they are used for.
#define nNoMansLandSize 4

typedef struct _CrtMemBlockHeader
{
        struct _CrtMemBlockHeader * pBlockHeaderNext;
        struct _CrtMemBlockHeader * pBlockHeaderPrev;
        char *                      szFileName;
        int                         nLine;
#ifdef _WIN64
        /* These items are reversed on Win64 to eliminate gaps in the struct
            * and ensure that sizeof(struct)%16 == 0, so 16-byte alignment is
            * maintained in the debug heap.
            */
        int                         nBlockUse;
        size_t                      nDataSize;
#else  /* _WIN64 */
        size_t                      nDataSize;
        int                         nBlockUse;
#endif  /* _WIN64 */
        long                        lRequest;
        unsigned char               gap[nNoMansLandSize];
        /* followed by:
            *  unsigned char           data[nDataSize];
            *  unsigned char           anotherGap[nNoMansLandSize];
            */
} _CrtMemBlockHeader;

#define pbData(pblock) ((unsigned char *)((_CrtMemBlockHeader *)pblock + 1))
#define pHdr(pbData) (((_CrtMemBlockHeader *)pbData)-1)

//#include <hash_map>
//inline size_t hash_value(const _bstr_t &_Str) {
//    return stdext::hash_value((const wchar_t *)_Str);
//}
#include <map>


namespace DBG {
// Trace functions & macros:
inline void snvTrace(const WCHAR *lpszFormat, va_list argList)
{
    vfwprintf(stdout, _bstr_t(lpszFormat) + L"\n", argList);
    fflush(stdout);
}
inline void snTraceEmp(const WCHAR *, ...) { }
inline void snTrace(const WCHAR *lpszFormat, ... )
{
    va_list argList;
    va_start(argList, lpszFormat);
    snvTrace(lpszFormat, argList);
    va_end(argList);
}

#define STRACE1       DBG::snTrace
#if   defined(_DEBUG) || defined(DEBUG)
    #define STRACE      DBG::snTrace
#else // _DEBUG
    #define STRACE      DBG::snTraceEmp
#endif// _DEBUG
#define STRACE0       DBG::snTraceEmp

struct DState {
    long startPass;
    long beforeDumpPass;
    long startAllocationNum;
    long endAllocationNum;
};

//static collections:
//Oops! Hash map is 1000 time slower than red-black tree on
//key-set greater than 65000.
//typedef stdext::hash_map<_bstr_t, _CrtMemState> FileName2MemState;
//typedef stdext::hash_map<_bstr_t, DState> FileName2MemStateEx;
//typedef stdext::hash_map<size_t, size_t> Content2Size;
typedef std::map<_bstr_t, _CrtMemState> FileName2MemState;
typedef std::map<_bstr_t, DState> FileName2MemStateEx;
typedef std::map<size_t, size_t> Content2Size;

typedef std::map<size_t, size_t> Size2Content;


#define STD_ID          L"WK_CP"
#define PRE_UPDATE      L"UPDATE_"
#define PRE_DUMP        L"DUMP_"

inline void init() {
    static bool isInitiated = false;
    if (!isInitiated) {
        isInitiated = true;
        _CrtSetReportMode( _CRT_WARN, _CRTDBG_MODE_FILE );
        _CrtSetReportFile( _CRT_WARN, _CRTDBG_FILE_STDOUT );
        _CrtSetReportMode( _CRT_ERROR, _CRTDBG_MODE_FILE );
        _CrtSetReportFile( _CRT_ERROR, _CRTDBG_FILE_STDOUT );
        _CrtSetReportMode( _CRT_ASSERT, _CRTDBG_MODE_FILE );
        _CrtSetReportFile( _CRT_ASSERT, _CRTDBG_FILE_STDOUT );
    }
}

inline FileName2MemState &getStateMap() {
    init();
    static FileName2MemState map;
    return map;
}

inline FileName2MemStateEx &getStateExMap() {
    init();
    static FileName2MemStateEx map;
    return map;
}

inline bool isObjectExists(
    const _bstr_t &bsID,
    const _bstr_t &bsTriggerSuffix
) {
    _bstr_t bsEventName(bsTriggerSuffix + bsID);
    HANDLE h = ::OpenEvent(EVENT_ALL_ACCESS, FALSE, bsEventName);
    if (h) {
        ::CloseHandle(h);
        // Handle exists?
        return true;
    }
    ::CreateEvent(NULL, FALSE, FALSE, bsEventName);
    return false;
}

inline bool isUpdate(const _bstr_t &bsID) {
    return !isObjectExists(bsID, PRE_UPDATE);
}

inline bool isDump(const _bstr_t &bsID) {
    return !isObjectExists(bsID, PRE_DUMP);
}

inline void checkPoint(
    const _bstr_t &bsID /*= STD_ID*/,
    bool bEachPass /*= false*/
) {
    int oldVal = _CrtSetDbgFlag(0);
    FileName2MemState &m = getStateMap();
    FileName2MemState::const_iterator i = m.find(bsID);
    if (isUpdate(bsID) || m.end() == i) {
        _CrtMemState curState;
        _CrtMemCheckpoint(&curState);
        m[bsID] = curState;
        isDump(bsID);
    } if (isDump(bsID) || bEachPass) {
        const _CrtMemState &oldState = i->second;
        _CrtMemState curState, diffState;
        _CrtMemCheckpoint(&curState);

        snTrace(L"{%s =======================", (const WCHAR *)bsID);
        if ( _CrtMemDifference( &diffState, &oldState, &curState) ) {
            _CrtMemDumpAllObjectsSince(&oldState);
            _CrtMemDumpStatistics(&diffState);
        }
        snTrace(L"}%s =======================", (const WCHAR *)bsID);
    }
    _CrtSetDbgFlag(oldVal | _CRTDBG_ALLOC_MEM_DF);
}

// Collects information about generation #[startPass].
// The "generation" is a set of heap allocations between check point
// call #[startPass]  and #[startPass + 1].
// Allocations that are not disposed on [startPass + beforeDumpPass] entry would
// be printed as leaks.
// To make a detailed report, please, open the process in [Process Explorer]
// and close named event "DUMP_%bsID%" (ex: Event "DUMP_WK_CP") in low panel
// with named objects.
// After [startPass + beforeDumpPass] calls the check point reports all
// alive allocations from generation #[startPass]
//                    to  generation #[current - beforeDumpPass]
// on each [beforeDumpPass]th call.
inline void checkPointEx(
    const _bstr_t &bsID /*= STD_ID*/,
    long startPass /*= 3*/,
    long beforeDumpPass /*= 5*/
) {
    int oldVal = _CrtSetDbgFlag(0); //ignore staff allocations
    bool reset = false;
    FileName2MemStateEx &m = getStateExMap();
    if (m.end() == m.find(bsID)) {
        DState st = {startPass, beforeDumpPass, 0};
        m[bsID] = st;
    }
    DState &st = m[bsID];
    if (st.startPass == 0) {
        //start "generation" bookmark
        _CrtMemState curState;
        _CrtMemCheckpoint(&curState);
        st.startAllocationNum = curState.pBlockHeader
            ? curState.pBlockHeader->lRequest
            : 0;
    } else if (st.startPass == -1) {
        //end "generation" bookmark (next entry)
        _CrtMemState curState;
        _CrtMemCheckpoint(&curState);
        st.endAllocationNum = curState.pBlockHeader
            ? curState.pBlockHeader->lRequest
            : 0;
    } else if (st.startPass < 0) {
            //check releases in "generation"...
            if (st.beforeDumpPass == 0) {
            //... after [beforeDumpPass] generations
            _CrtMemState curState;
            _CrtMemCheckpoint(&curState);
            if (curState.pBlockHeader) {
                long lBlocks = 0L;
                long lAllocatedSize = 0L;
                long lBlocksAll = 0L;
                long lAllocatedSizeAll = 0L;

                Content2Size c2s;
                bool bDumpAll = isDump(bsID);

                if (bDumpAll) {
                    snTrace(L"{%s: #:Leaks", (const WCHAR *)bsID);
                }

                for (_CrtMemBlockHeader *pH = curState.pBlockHeader->pBlockHeaderNext;
                        pH;
                        pH = pH->pBlockHeaderNext
                ) {
                    ++lBlocksAll;
                    lAllocatedSizeAll += pH->nDataSize;
                    if ( _BLOCK_TYPE(pH->nBlockUse)!=_IGNORE_BLOCK
                        && pH->lRequest >  st.startAllocationNum
                        && pH->lRequest <= st.endAllocationNum
                    ) {
                        //all heap records that are alive in the current and start generations.
                        ++lBlocks;
                        lAllocatedSize += pH->nDataSize;
                        DWORD *pWords = (DWORD *)pbData(pH);

                        if (bDumpAll) {
                            snTrace(
                                L"%08x-%08x-%08x-%08x "
                                L"Size:%08x Adr:%p %s:%d #:%d"
                                , pWords[0], pWords[1]
                                , pWords[2], pWords[3]
                                , int(pH->nDataSize), pbData(pH)
                                , (const WCHAR *)_bstr_t(pH->szFileName), pH->nLine, pH->lRequest
                            );
                        }

                        c2s[*(size_t *)pbData(pH)] += pH->nDataSize;
                    }
                }
                if (bDumpAll) {
                    snTrace(L"}%s: #:Leaks", (const WCHAR *)bsID);
                }

                Size2Content s2c;
                for (Content2Size::const_iterator i = c2s.begin(); c2s.end() != i; ++i) {
                    s2c[i->second] = i->first;
                }
                int top = 0;
                snTrace(L"{%s: #:Context top 10 (blocks:%ld/%ld lost:%ld/%ld bytes)", (const WCHAR *)bsID, lBlocks, lBlocksAll, lAllocatedSize, lAllocatedSizeAll);
                for (Size2Content::const_reverse_iterator i = s2c.rbegin(); s2c.rend() != i && 10 > top++; ++i) {
                    snTrace(L"context:%08x size:%ld bytes", i->second, i->first);
                }
                snTrace(L"}%s: #:Context", (const WCHAR *)bsID);
            }

            //reset the upper border of extended generation (re-check all previous allocations)
            st.endAllocationNum = curState.pBlockHeader
                ? curState.pBlockHeader->lRequest
                : 0;
            st.beforeDumpPass = beforeDumpPass;
        }
        --st.beforeDumpPass;
    }
    --st.startPass;
    _CrtSetDbgFlag(oldVal | _CRTDBG_ALLOC_MEM_DF);
}
}
#define DBG_CHECKPOINT(a1, a2)          DBG::checkPoint(a1, a2)
#define DBG_CHECKPOINTEX(a1, a2, a3)    DBG::checkPointEx(a1, a2, a3)

#define LOG_INSTANCE_COUNT(T) \
struct SInstanceCounterLogger {\
    SInstanceCounterLogger() {\
        LONG p = InterlockedIncrement(getCounter()); \
        DBG::snTrace(L"{%p " L#T L" Count:%d->%d", this, p - 1, p);\
    }\
    ~SInstanceCounterLogger() {\
        LONG p = InterlockedDecrement(getCounter()); \
        DBG::snTrace(L"}%p " L#T L" Count:%d->%d", this, p + 1, p);\
    }\
    LPLONG getCounter() {\
        static LONG instanceCount = 0L;\
        return &instanceCount;\
    }\
} __lInstanceCounter__;

#define LOG_COMMON_SIZE(T) \
struct SSizeCounterLogger {\
    void add(LONG value) {\
        LPLONG ps = getSize();\
        LONG p = InterlockedExchangeAdd(ps, value);\
        DBG::snTrace(L"{%p " L#T L" Size:%d->%d", this, p, *ps);\
    }\
    void remove(LONG value) {\
        LPLONG ps = getSize();\
        LONG p = InterlockedExchangeAdd(ps, -value);\
        DBG::snTrace(L"}%p " L#T L" Size:%d->%d", this, p, *ps);\
    }\
    LPLONG getSize() {\
        static LONG commonSize = 0L;\
        return &commonSize;\
    }\
} __lSizeCounter__;

#define LOG_COMMON_SIZE_ADD(size)    __lSizeCount__.add((LONG)size);
#define LOG_COMMON_SIZE_REMOVE(size) __lSizeCount__.remove((LONG)size);

#else //WIN32 & debug

#define DBG_CHECKPOINT(a1, a2)
#define DBG_CHECKPOINTEX(a1, a2, a3)
#define LOG_INSTANCE_COUNT(T)
#define LOG_COMMON_SIZE(T)
#define LOG_COMMON_SIZE_ADD(size)
#define LOG_COMMON_SIZE_REMOVE(size)

#endif//WIN32 & debug

#ifdef __RQ_LOG
#define RQ_LOG_INSTANCE_COUNT(T) LOG_INSTANCE_COUNT(T)
#define RQ_LOG_COMMON_SIZE(T) LOG_COMMON_SIZE(T)
#define RQ_LOG_COMMON_SIZE_ADD(size) LOG_COMMON_SIZE_ADD(size)
#define RQ_LOG_COMMON_SIZE_REMOVE(size) LOG_COMMON_SIZE_REMOVE(size)
#else
#define RQ_LOG_INSTANCE_COUNT(T)
#define RQ_LOG_COMMON_SIZE(T)
#define RQ_LOG_COMMON_SIZE_ADD(size)
#define RQ_LOG_COMMON_SIZE_REMOVE(size)
#endif

#endif//_DBGUTILS_H
