set(JavaScriptCore_LIBRARY_TYPE STATIC)
add_definitions(-DSTATICALLY_LINKED_WITH_WTF)

if (APPLE)
    find_library(COREFOUNDATION_LIBRARY CoreFoundation)
    list(APPEND JavaScriptCore_LIBRARIES
        ${COREFOUNDATION_LIBRARY}
    )
endif()

list(APPEND JavaScriptCore_LIBRARIES
    ${JAVA_JVM_LIBRARY}
)

list(APPEND JavaScriptCore_SYSTEM_INCLUDE_DIRECTORIES
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
)

if (USE_LD_LLD)
    if ("${LD_VERSION}" MATCHES "LLD")
       set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,--no-gc-sections")
    endif ()
endif ()

if (USE_LD_GOLD)
    if ("${LD_VERSION}" MATCHES "GNU gold")
        set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,--no-gc-sections")
    endif ()
endif ()
