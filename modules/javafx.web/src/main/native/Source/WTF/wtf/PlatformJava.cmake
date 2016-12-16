set(WTF_LIBRARY_TYPE STATIC)

list(APPEND WTF_INCLUDE_DIRECTORIES
    "${ICU_INCLUDE_DIRS}"
    "${WTF_DIR}/wtf/java"
    "${CMAKE_SOURCE_DIR}/Source"
    "${JAVA_INCLUDE_PATH}"
    "${JAVA_INCLUDE_PATH2}"
)

list(APPEND WTF_SOURCES
    java/RunLoopJava.cpp
    java/StringJava.cpp
    java/MainThreadJava.cpp
    # FIXME-java: Move JavaEnv.{cpp, h} to wtf
    ../../WebCore/platform/java/JavaEnv.cpp
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
    list(APPEND WTF_INCLUDE_DIRECTORIES
        "${WTF_DIR}/icu"
    )

    list(APPEND WTF_SOURCES
        cocoa/WorkQueueCocoa.cpp
        text/cf/StringImplCF.cpp
        text/cf/StringCF.cpp
    )
elseif (UNIX)
    list(APPEND WTF_SOURCES
        efl/DispatchQueueEfl.cpp
        efl/WorkQueueEfl.cpp
    )
    list(APPEND WTF_INCLUDE_DIRECTORIES
        "${WTF_DIR}/wtf/efl"
    )
elseif (WIN32)
    list(APPEND WTF_SOURCES
      win/WorkItemWin.cpp
      win/WorkQueueWin.cpp
    )

    list(APPEND WTF_LIBRARIES
        winmm
    )
endif ()

if (DEFINED CMAKE_USE_PTHREADS_INIT)
    list(APPEND WTF_LIBRARIES pthread)
endif()
