add_subdirectory(ThirdParty/sqlite)
add_subdirectory(ThirdParty/icu)

# libxml2 should be compiled only on windows
if (WIN32)
    add_subdirectory(ThirdParty/libxml)
    add_subdirectory(ThirdParty/libxslt)
endif ()
