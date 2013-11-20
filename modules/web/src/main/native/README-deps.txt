This file contains information on how to build third-party libraries that are needed for the native build to succeed and that are part of webview-deps import bundle.


LIBXML2 (Windows only)
======================

1. Download latest source bundle from
    ftp://xmlsoft.org/libxml2/LATEST_LIBXML2
2. Unpack the bundle and cd into win32 subdirectory
3. Consult the Readme.txt file
4. Open a command prompt and run the following command
    cscript configure.js compiler=msvc trio=no ftp=no http=no c14n=no catalog=no docb=no xptr=no xinclude=no iconv=no xml_debug=no regexps=no modules=no walker=no pattern=no valid=no sax1=no legacy=no schemas=no schematron=no
   This turns off unneeded features and makes library smaller
5. Set up compiler paths; usually done by running SetEnv.cmd
6. Run
    nmake /f Makefile.msvc
   The build will create libraries in the win32/bin.msvc directory. You need libxml2_a.lib (the static linkage library)


LIBXSLT (Windows only)
======================

0. Build the libxml2 library first (see the steps above)
1. Download latest source bundle from
    ftp://xmlsoft.org/libxml2/LATEST_LIBXSLT
2. Unpack the bundle and cd into win32 subdirectory
3. Consult the Readme.txt file
4. Open a command prompt and run the following command
    cscript configure.js compiler=msvc include=..\..\libxml2-XXX\include lib=..\..\libxml2-XXX\win32\bin.msvc xslt_debug=no debugger=no iconv=no crypto=no locale=no
   This turns off unneeded features and makes library smaller. The variables [include] and [lib] must refer to libxml2 build created at step 0
5. The generated Makefile.msvc contain OPT:NOWIN98 linker option that VS Express 2010 cannot handle. You can safely remove this whole line
6. Set up compiler paths; usually done by running SetEnv.cmd
7. Run
    nmake /f Makefile.msvc
   The build will create libraries in the win32/bin.msvc directory. You need libxslt_a.lib (the static linkage library)


LIBSQLITE3 (all platforms)
==========================

1. Download the latest sqlite-autoconf package from
    http://sqlite.org/download.html
2. Unpack the bundle
3. From the top level directory of the unpacked bundle, run either
    CFLAGS='-m32 -fPIC' ./configure --disable-readline --disable-threadsafe --disable-dynamic-extensions
   for 32-bit build, or
    CFLAGS='-m64 -fPIC' ./configure --disable-readline --disable-threadsafe --disable-dynamic-extensions
   for 64-bit build. This turns off unneeded features and makes library smaller
4. Run make (nmake on Windows). Libraries will be created in the .libs subdirectory. You need the static linkage library
