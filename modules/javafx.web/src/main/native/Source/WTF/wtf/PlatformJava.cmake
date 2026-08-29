set(WTF_LIBRARY_TYPE STATIC)

# No JDK include path here any more. WTF was the only target whose include directories were
# load bearing for the JVM headers: _WEBKIT_TARGET_SETUP applies <target>_INCLUDE_DIRECTORIES
# as PUBLIC, so this list propagates to PAL, JavaScriptCore, WebCore and WebKitLegacy, and the
# equivalent entries in their own PlatformJava.cmake files were redundant. Nothing under Source
# or Tools includes the JNI header now, so it comes out. The redundant copies in
# WebCore/PlatformJava.cmake and JavaScriptCore/PlatformJava.cmake, and the JNI package lookup
# in cmake/OptionsJava.cmake, are the remaining three and belong to those slices.
list(APPEND WTF_INCLUDE_DIRECTORIES
    "${WTF_DIR}/wtf/java"
    "${CMAKE_SOURCE_DIR}/Source"
    # FFM C ABI: webkit_java_api.h (WKJ_EXPORT, wkj_ref, WKJHost)
    "${WEBKITLEGACY_DIR}/java/api"
)

list(APPEND WTF_PUBLIC_HEADERS
    java/WKJHandle.h
    java/WKJRuntime.h
    java/JavaMath.h
)

if (UNIX)
    list(APPEND WTF_PUBLIC_HEADERS
        unix/UnixFileDescriptor.h
    )
endif ()

list(APPEND WTF_SOURCES
    java/FileSystemJava.cpp
    java/MainThreadJava.cpp
    java/TextBreakIteratorInternalICUJava.cpp
    java/CPUTimeJava.cpp
    java/WKJRuntime.cpp
)

# WTF_LIBRARIES no longer names the JVM import library: nothing in WTF calls a JNI function,
# so there is nothing to link against. WTF_SYSTEM_INCLUDE_DIRECTORIES went with it - it named
# a variable that is defined nowhere in the tree and always expanded to nothing.

if (APPLE)
    file(COPY mac/MachExceptions.defs DESTINATION ${WTF_DERIVED_SOURCES_DIR})

    add_custom_command(
        OUTPUT
            ${WTF_DERIVED_SOURCES_DIR}/MachExceptionsServer.h
            ${WTF_DERIVED_SOURCES_DIR}/mach_exc.h
            ${WTF_DERIVED_SOURCES_DIR}/mach_excServer.c
            ${WTF_DERIVED_SOURCES_DIR}/mach_excUser.c
        MAIN_DEPENDENCY mac/MachExceptions.defs
        WORKING_DIRECTORY ${WTF_DERIVED_SOURCES_DIR}
        COMMAND mig -sheader MachExceptionsServer.h MachExceptions.defs
        VERBATIM)

    list(APPEND WTF_SOURCES
        ${WTF_DERIVED_SOURCES_DIR}/mach_excServer.c
        ${WTF_DERIVED_SOURCES_DIR}/mach_excUser.c
    )
    #if_platform_JAVA 
    list(APPEND WTF_PUBLIC_HEADERS
        cf/TypeCastsCF.h
    )
    #endif_platform_JAVA  
    list(APPEND WTF_PRIVATE_INCLUDE_DIRECTORIES
        # Check whether we can use WTF/icu
        # "${WTF_DIR}/icu"
        ${WTF_DERIVED_SOURCES_DIR}
    )
    list(APPEND WTF_SOURCES
        BlockObjCExceptions.mm
        cf/LanguageCF.cpp
        cf/RunLoopCF.cpp
        cocoa/MachSendRight.cpp
        cocoa/MemoryFootprintCocoa.cpp
        cocoa/MemoryPressureHandlerCocoa.mm
        cocoa/WorkQueueCocoa.cpp
        text/cf/StringCF.cpp
        text/cf/StringImplCF.cpp
	text/cocoa/ASCIILiteralCocoa.mm
        text/cocoa/StringImplCocoa.mm
    )

    find_library(COCOA_LIBRARY Cocoa)
    find_library(COREFOUNDATION_LIBRARY CoreFoundation)
    list(APPEND WTF_LIBRARIES
        ${COREFOUNDATION_LIBRARY}
        ${COCOA_LIBRARY}
    )
elseif (UNIX)
    list(APPEND WTF_SOURCES
        generic/RunLoopGeneric.cpp
        generic/WorkQueueGeneric.cpp
        linux/CurrentProcessMemoryStatus.cpp
        linux/MemoryFootprintLinux.cpp
        unix/LanguageUnix.cpp
        unix/MemoryPressureHandlerUnix.cpp
        linux/RealTimeThreads.cpp
    )
    list(APPEND WTF_LIBRARIES rt)
elseif (WIN32)
    list(APPEND WTF_SOURCES
        generic/WorkQueueGeneric.cpp

        win/CPUTimeWin.cpp
        win/DbgHelperWin.cpp
        win/LanguageWin.cpp
        win/MemoryFootprintWin.cpp
        win/MemoryPressureHandlerWin.cpp
        win/OSAllocatorWin.cpp
        win/RunLoopWin.cpp
        win/ThreadingWin.cpp
        win/Win32Handle.cpp
        win/SignalsWin.cpp
    )

    list(APPEND WTF_PUBLIC_HEADERS
        text/win/WCharStringExtras.h

        win/DbgHelperWin.h
        win/Win32Handle.h
    )

    list(APPEND WTF_LIBRARIES
        DbgHelp
        winmm
    )
endif ()

if (UNIX)
    list(APPEND WTF_SOURCES
        posix/OSAllocatorPOSIX.cpp
        posix/ThreadingPOSIX.cpp
    )
endif ()

if (DEFINED CMAKE_USE_PTHREADS_INIT)
    list(APPEND WTF_LIBRARIES pthread)
endif()
