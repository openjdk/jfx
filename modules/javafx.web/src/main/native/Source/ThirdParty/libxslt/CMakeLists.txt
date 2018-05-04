set(XSLT_SOURCES
    src/libxslt/attributes.c
    src/libxslt/documents.c
    src/libxslt/extensions.c
    src/libxslt/extra.c
    src/libxslt/functions.c
    src/libxslt/imports.c
    src/libxslt/keys.c
    src/libxslt/namespaces.c
    src/libxslt/numbers.c
    src/libxslt/pattern.c
    src/libxslt/preproc.c
    src/libxslt/security.c
    src/libxslt/templates.c
    src/libxslt/transform.c
    src/libxslt/variables.c
    src/libxslt/xslt.c
    src/libxslt/xsltlocale.c
    src/libxslt/xsltutils.c
    src/libxslt/attrvt.c
)

# XSLT_COMPILE_OPTIONS are taken from Chromium.
if (WIN32)
    set(XSLT_PLATFORM_INCLUDE_DIRECTORY win32)
    set(XSLT_COMPILE_OPTIONS
        "/wd4267"  # size_t to int.

        # TODO(brucedawson): http://crbug.com/554200 4311 is a VS
        # 2015 64-bit warning for pointer truncation
        "/wd4311"

        # security.c: from 'const char *' to 'LPCWSTR'
        "/wd4133"
    )
elseif (APPLE)
    set(XSLT_PLATFORM_INCLUDE_DIRECTORY mac)
    set(XSLT_COMPILE_OPTIONS
        # numbers.c, xsltlocale.c, functions.c
        "-Wno-unused-parameter"

        # libxslt stores a char[3] in a `const unsigned char*`.
        "-Wno-pointer-sign"

        # xsltDefaultRegion and xsltCalibrateTimestamps are only
        # used with certain preprocessor defines set.
        "-Wno-unused-function"
        "-Wno-unused-variable"
     )
elseif (UNIX AND NOT APPLE)
    set(XSLT_PLATFORM_INCLUDE_DIRECTORY linux)
    set(XSLT_COMPILE_OPTIONS
        # TODO:
        # Following warnings are seen only with GCC 4.9.4. Remove once
        # we move to latest GCC
        "-Wno-unused-function"
        "-Wno-unused-parameter"
        "-Wno-unused-variable"
        "-Wno-unused-but-set-variable"
        "-Wno-suggest-attribute=format"
        "-Wno-sign-compare"
    )
endif ()

add_library(XSLTJava STATIC ${XSLT_SOURCES})

target_link_libraries(XSLTJava PUBLIC XMLJava)

target_include_directories(XSLTJava
    PUBLIC "src"
    PRIVATE "${XSLT_PLATFORM_INCLUDE_DIRECTORY}"
)

if (XSLT_COMPILE_OPTIONS)
    target_compile_options(XSLTJava PRIVATE ${XSLT_COMPILE_OPTIONS})
endif ()

target_compile_definitions(XSLTJava PUBLIC "LIBXSLT_STATIC")
