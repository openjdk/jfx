TEMPLATE = subdirs
CONFIG += ordered

include(WebKitJava.pri)

SUBDIRS += \
    LLIntOffsetsExtractor \
    javascriptcore \
    webcore \
    dumprendertree


LLIntOffsetsExtractor.file = JavaScriptCore/LLIntOffsetsExtractor.pro
LLIntOffsetsExtractor.makefile = Makefile.LLIntOffsetsExtractor

javascriptcore.file = JavaScriptCore/JavaScriptCoreJava.pro
webcore.file = WebCore/WebCoreJava.pro
webcore.depend = javascriptcore
dumprendertree.file = ../Tools/DumpRenderTree/DumpRenderTreeJava.pro
dumprendertree.depend = webcore

# win32-* {
#
# SUBDIRS += plugings
# plugings.file = WebKit/java/plugin/PluginsJava.pro
# plugings.depend = webcore
#
# }
