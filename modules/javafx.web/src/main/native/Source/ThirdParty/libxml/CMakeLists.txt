set(XML_SOURCES
     # src/c14n.c
     # src/catalog.c
     src/buf.c
     src/chvalid.c
     # src/debugXML.c
     src/dict.c
     # src/DOCBparser.c
     src/encoding.c
     src/entities.c
     src/error.c
     src/globals.c
     src/hash.c
     src/HTMLparser.c
     src/HTMLtree.c
     # src/legacy.c
     src/list.c
     # src/nanoftp.c
     # src/nanohttp.c
     src/parser.c
     src/parserInternals.c
     src/pattern.c
     # src/relaxng.c
     src/SAX2.c
     # src/SAX.c
     # src/schematron.c
     src/threads.c
     src/tree.c
     src/uri.c
     src/valid.c
     # src/xinclude.c
     # src/xlink.c
     src/xmlIO.c
     src/xmlmemory.c
     src/xmlreader.c
     # src/xmlregexp.c
     # src/xmlmodule.c
     src/xmlsave.c
     # src/xmlschemas.c
     # src/xmlschemastypes.c
     # src/xmlunicode.c
     src/xmlwriter.c
     src/xpath.c
     # src/xpointer.c
     src/xmlstring.c
)

set(LIBXML2_LIBRARIES XMLJava)

if (WIN32)
    set(XML_PLATFORM_INCLUDE_DIRECTORY win32)
    set(XML_COMPILE_OPTIONS
        "/wd4018"  # Signed/unsigned mismatch in comparison.
        "/wd4267"  # TODO(brucedawson): http://crbug.com/554200 fix C4267
        "/wd4311"  # and C4311 warnings.
    )
elseif (APPLE)
    set(XML_PLATFORM_INCLUDE_DIRECTORY mac)
    set(XML_COMPILE_OPTIONS
        # encoding.c cast from `const unsigned char*` to `unsigned short*`
        "-Wno-cast-align"

        # xmlIO.c, __MVS__ define is not set
        "-Wno-undef"

        # xmlIO.c
        "-Wno-unused-parameter"

        # Following flags are taken from Chromium libxml BUILD.gn
        # libxml passes `const unsigned char*` through `const char*`.
        "-Wno-pointer-sign"

        # pattern.c and uri.c both have an intentional `for (...);` /
        # `while(...);` loop. I submitted a patch to move the `'` to its own
        # line, but until that's landed suppress the warning:
        "-Wno-empty-body"

        # debugXML.c compares array 'arg' to NULL.
        "-Wno-tautological-pointer-compare"

        # threads.c attempts to forward declare a pthread_equal which doesn't
        # match the prototype in pthreads.h
        "-Wno-ignored-attributes"

        # libxml casts from int to long to void*.
        "-Wno-int-to-void-pointer-cast"

        # libxml passes a volatile LPCRITICAL_SECTION* to a function expecting
        # a void* volatile*.
        "-Wno-incompatible-pointer-types"

        # trio_is_special_quantity and trio_is_negative are only
        # used with certain preprocessor defines set.
        "-Wno-unused-function"

        # Comparison between xmlElementType and xmlXPathTypeVal.
        # TODO(hans): See if we can fix upstream (http://crbug.com/763944).
        "-Wno-enum-compare"
      )
elseif (UNIX AND NOT APPLE)
    set(XML_PLATFORM_INCLUDE_DIRECTORY linux)
    set(XML_COMPILE_OPTIONS
        # TODO:
        # Following warnings are seen only with GCC 4.9.4. Remove once
        # we move to latest GCC
        "-Wno-unused-function"
        "-Wno-unused-parameter"
        "-Wno-unused-variable"
        "-Wno-unused-but-set-variable"
        "-Wno-suggest-attribute=format"
        "-Wno-sign-compare"
        "-Wno-enum-compare"

        # xmlIO.c, __MVS__ define is not set
        "-Wno-undef"

        # Following flags are taken from Chromium libxml BUILD.gn
        # gcc spits out a bunch of warnings about passing too many arguments to
        # __xmlSimpleError.
        "-Wno-format-extra-args"
    )
endif ()

if (UNIX)
    set(XML_DEFINES "_REENTRANT")
endif ()

add_library(XMLJava STATIC ${XML_SOURCES})

if (XML_DEFINES)
    target_compile_definitions(XMLJava PRIVATE ${XML_DEFINES})
endif ()

target_compile_definitions(XMLJava PUBLIC "LIBXML_STATIC")
target_compile_options(XMLJava PRIVATE ${XML_COMPILE_OPTIONS})
target_link_libraries(XMLJava PUBLIC icuuc icudata)
# Export to other moduels who uses libxml
target_include_directories(XMLJava
    PUBLIC "src/include"
    PUBLIC "${XML_PLATFORM_INCLUDE_DIRECTORY}/include"
    PRIVATE "${XML_PLATFORM_INCLUDE_DIRECTORY}"
)
