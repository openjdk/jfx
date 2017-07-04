set(ICU_PKG_INCLUDE_DIRECTORIES
    ../../../source/common
    ../../../source/tools/pkgdata/../toolutil
    )

set(ICU_PKG_SOURCES
    ../../../source/tools/pkgdata/pkgtypes.c
    ../../../source/tools/pkgdata/pkgdata.cpp
    )

set(ICU_PKG_LIBRARIES
    ${ICU_JAVA_TOOLUTIL_LIB}
    ${ICU_I18N_LIBRARIES}
    ${ICU_JAVA_COMMON_LIB}
    ${ICU_JAVA_ICU_STUBDATA_LIB}
    )

include_directories(${ICU_PKG_INCLUDE_DIRECTORIES})

add_executable(pkgdata ${ICU_PKG_SOURCES} ${ICU_PKG_INCLUDE_DIRECTORIES})
target_link_libraries(pkgdata ${ICU_PKG_LIBRARIES} ${CMAKE_DL_LIBS} ${CMAKE_THREAD_LIBS_INIT})

add_dependencies(pkgdata icupkg)
