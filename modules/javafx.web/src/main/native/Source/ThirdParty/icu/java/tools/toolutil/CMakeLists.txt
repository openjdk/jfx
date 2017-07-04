set(ICU_TOOLSUTILS_SOURCES
        ../../../source/tools/toolutil/filestrm.c
        ../../../source/tools/toolutil/package.cpp
        ../../../source/tools/toolutil/pkgitems.cpp
        ../../../source/tools/toolutil/swapimpl.cpp
        ../../../source/tools/toolutil/toolutil.cpp
        ../../../source/tools/toolutil/unewdata.c
        ../../../source/tools/toolutil/denseranges.cpp
        ../../../source/tools/toolutil/ucm.c
        ../../../source/tools/toolutil/ucmstate.c
        ../../../source/tools/toolutil/uoptions.c
        ../../../source/tools/toolutil/uparse.c
        ../../../source/tools/toolutil/ucbuf.c
        ../../../source/tools/toolutil/xmlparser.cpp
        ../../../source/tools/toolutil/writesrc.c
        ../../../source/tools/toolutil/pkg_icu.cpp
        ../../../source/tools/toolutil/pkg_genc.c
        ../../../source/tools/toolutil/pkg_gencmn.c
        ../../../source/tools/toolutil/ppucd.cpp
        ../../../source/tools/toolutil/flagparser.c
        ../../../source/tools/toolutil/filetools.cpp
        ../../../source/tools/toolutil/udbgutil.cpp
        ../../../source/tools/toolutil/dbgutil.cpp
        ../../../source/tools/toolutil/ucln_tu.c
    )

set(ICU_TOOLSUTILS_INCLUDE_DIRECTORIES
      ../../../source/common
      ../../../source/i18n
)

include_directories(${ICU_TOOLSUTILS_INCLUDE_DIRECTORIES})
add_library(${ICU_JAVA_TOOLUTIL_LIB} STATIC ${ICU_TOOLSUTILS_SOURCES})
