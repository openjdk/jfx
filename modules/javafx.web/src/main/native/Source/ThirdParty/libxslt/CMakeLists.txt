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

set(XSLT_INCLUDE_DIRECTORIES
      .
      win32
      ${THIRDPARTY_DIR}/libxml/src/include
      ${THIRDPARTY_DIR}/libxml/win32/include
)

include_directories(${XSLT_INCLUDE_DIRECTORIES})
add_library(XSLTJava STATIC ${XSLT_SOURCES})
set_target_properties(XSLTJava PROPERTIES COMPILE_DEFINITIONS "LIBXSLT_STATIC")
