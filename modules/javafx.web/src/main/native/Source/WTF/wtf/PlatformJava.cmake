set(WTF_LIBRARY_TYPE STATIC)

list(APPEND WTF_INCLUDE_DIRECTORIES
    "${WTF_DIR}/wtf/java"
    "${CMAKE_SOURCE_DIR}/Source"
    "${JAVA_INCLUDE_PATH}"
    "${JAVA_INCLUDE_PATH2}"
)

list(APPEND WTF_SOURCES
    java/StringJava.cpp
    java/MainThreadJava.cpp
    java/JavaEnv.cpp
    java/TextBreakIteratorInternalICUJava.cpp #ICU_UNICODE=1 //XXX: make switch for ICU_UNICODE
)

list(APPEND WTF_LIBRARIES
    "${ICU_LIBRARIES}"
    "${ICU_I18N_LIBRARIES}"
    "${JAVA_JVM_LIBRARY}"
)

list(APPEND WTF_SYSTEM_INCLUDE_DIRECTORIES
	"${JDK_INCLUDE_DIRS}"
)

if (APPLE)
    list(APPEND WTF_SOURCES
        cf/RunLoopCF.cpp
        cocoa/WorkQueueCocoa.cpp
        text/cf/StringImplCF.cpp
        text/cf/StringCF.cpp
        text/mac/StringMac.mm
        text/mac/StringImplMac.mm
        PlatformUserPreferredLanguagesMac.mm
        BlockObjCExceptions.mm
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
        PlatformUserPreferredLanguagesUnix.cpp
    )
    list(APPEND WTF_INCLUDE_DIRECTORIES
        "${WTF_DIR}/wtf/efl"
    )
elseif (WIN32)
    list(APPEND WTF_SOURCES
        PlatformUserPreferredLanguagesWin.cpp
        win/RunLoopWin.cpp
        win/WorkQueueWin.cpp
    )

    list(APPEND WTF_LIBRARIES
        winmm
    )
endif ()

if (DEFINED CMAKE_USE_PTHREADS_INIT)
    list(APPEND WTF_LIBRARIES pthread)
endif()
