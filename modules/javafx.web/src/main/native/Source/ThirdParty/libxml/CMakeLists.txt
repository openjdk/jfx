set(XML_SOURCES
     # src/c14n.c
     # src/catalog.c
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
     src/nanoftp.c
     src/nanohttp.c
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
     src/xmlunicode.c
     src/xmlwriter.c
     src/xpath.c
     # src/xpointer.c
     src/xmlstring.c
)

set(XML_INCLUDE_DIRECTORIES
      win32
      win32/include
      src/include
)

include_directories(${XML_INCLUDE_DIRECTORIES})
add_library(XMLJava STATIC ${XML_SOURCES})
set_target_properties(XMLJava PROPERTIES COMPILE_DEFINITIONS "LIBXML_STATIC")
