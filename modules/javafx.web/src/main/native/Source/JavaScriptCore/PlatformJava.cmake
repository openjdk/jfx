set(JavaScriptCore_LIBRARY_TYPE STATIC)
add_definitions(-DSTATICALLY_LINKED_WITH_WTF)

if (APPLE)
    find_library(COREFOUNDATION_LIBRARY CoreFoundation)
    list(APPEND JavaScriptCore_LIBRARIES
        ${COREFOUNDATION_LIBRARY}
    )
endif()

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
