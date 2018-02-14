set(XSLT_SOURCES
    libxslt/attributes.c
    libxslt/documents.c
    libxslt/extensions.c
    libxslt/extra.c
    libxslt/functions.c
    libxslt/imports.c
    libxslt/keys.c
    libxslt/namespaces.c
    libxslt/numbers.c
    libxslt/pattern.c
    libxslt/preproc.c
    libxslt/security.c
    libxslt/templates.c
    libxslt/transform.c
    libxslt/variables.c
    libxslt/xslt.c
    libxslt/xsltlocale.c
    libxslt/xsltutils.c
    libxslt/attrvt.c
)

# XSLT_COMPILE_OPTIONS are taken from Chromium.
if (WIN32)
    set(XSLT_PLATFORM_INCLUDE_DIRECTORY win32)
    set(XSLT_COMPILE_OPTIONS
        "/wd4267"  # size_t to int.

        # TODO(brucedawson): http://crbug.com/554200 4311 is a VS
        # 2015 64-bit warning for pointer truncation
        "/wd4311"
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
    PUBLIC "."
    PRIVATE "${XSLT_PLATFORM_INCLUDE_DIRECTORY}"
)

if (XSLT_COMPILE_OPTIONS)
    target_compile_options(XSLTJava PRIVATE ${XSLT_COMPILE_OPTIONS})
endif ()

target_compile_definitions(XSLTJava PUBLIC "LIBXSLT_STATIC")
