#if defined(WIN32) || defined(_WIN32)

#ifndef WINVER
#define WINVER 0x0502
#endif

#ifndef _WIN32_WINNT
#define _WIN32_WINNT 0x0502
#endif

#ifdef __cplusplus
#define max max
#define min min
#endif

#ifndef _WINSOCKAPI_
#define _WINSOCKAPI_ // Prevent inclusion of winsock.h in windows.h
#else
#error ERROR: winsock.h is already included
#endif

#include <windows.h>

#if defined(DEBUG) || defined(_DEBUG)
//Please, read more at "Finding Memory Leaks Using the CRT Library":
// http://msdn.microsoft.com/en-us/library/x98tx3cf.aspx
#include <crtdbg.h>
#define CRASH _CrtDbgBreak
#endif //WIN32-debug


#endif //WIN32


#include <sys/types.h>
#include <fcntl.h>
#include <signal.h>
#include <stdarg.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <jni.h>

#ifdef __cplusplus
#include <algorithm>
#include <cstddef>
#include <limits>
#include <new>
#endif

#include <config.h>

#ifdef __cplusplus

#if OS(WINDOWS)
#include <platform/win/SystemInfo.h>
#endif

#include <wtf/Assertions.h>
#include <wtf/CurrentTime.h>
#include <wtf/DateMath.h>
#include <wtf/Deque.h>
#include <wtf/DisallowCType.h>
#include <wtf/FastMalloc.h>
#include <wtf/Forward.h>
#include <wtf/GetPtr.h>
#include <wtf/HashCountedSet.h>
#include <wtf/HashFunctions.h>
#include <wtf/HashMap.h>
#include <wtf/HashSet.h>
#include <wtf/HashTable.h>
#include <wtf/HashTraits.h>
#include <wtf/ListHashSet.h>
#include <wtf/MathExtras.h>
#include <wtf/Noncopyable.h>
#include <wtf/OwnPtr.h>
#include <wtf/OwnPtrCommon.h>
#include <wtf/PassOwnPtr.h>
#include <wtf/PassRefPtr.h>
#include <wtf/RefCounted.h>
#include <wtf/RefPtr.h>
#include <wtf/StringExtras.h>
#include <wtf/TCPageMap.h>
#include <wtf/TCSystemAlloc.h>
#include <wtf/Vector.h>
#include <wtf/VectorTraits.h>

#include <wtf/unicode/UTF8.h>

#include <wtf/text/CString.h>
#include <wtf/text/WTFString.h>
#include <wtf/text/StringImpl.h>
#include <wtf/text/StringHash.h>
#include <JavaScriptCore/APICast.h>
#include <JavaScriptCore/JavaScript.h>
#include <JavaScriptCore/JSBase.h>
#include <JavaScriptCore/JSContextRef.h>
#include <JavaScriptCore/JSObjectRef.h>
#include <JavaScriptCore/JSRetainPtr.h>
#include <JavaScriptCore/JSStringRef.h>
#include <JavaScriptCore/JSValueRef.h>
#include <JavaScriptCore/OpaqueJSString.h>
#include <JavaScriptCore/inspector/InspectorAgentBase.h>

#if USE(JAVA_UNICODE)
#include <wtf/unicode/java/UnicodeJava.h>
#endif

#endif
