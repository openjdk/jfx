set(ICU_PKG_INCLUDE_DIRECTORIES
    ../../../source/common
    ../../../source/tools/pkgdata
    ../../../source/tools/toolutil
    )

set(ICU_PKG_SOURCES
    ../../../source/tools/icupkg/icupkg.cpp
    )

set(ICU_PKG_LIBRARIES
    ${ICU_JAVA_TOOLUTIL_LIB}
    ${ICU_I18N_LIBRARIES}
    ${ICU_JAVA_COMMON_LIB}
    ${ICU_JAVA_ICU_STUBDATA_LIB}
    )

include_directories(${ICU_PKG_INCLUDE_DIRECTORIES})

add_executable(icupkg ${ICU_PKG_SOURCES})
target_link_libraries(icupkg LINK_PUBLIC ${ICU_PKG_LIBRARIES} ${CMAKE_DL_LIBS})

add_dependencies(icupkg ${ICU_JAVA_TOOLUTIL_LIB})

