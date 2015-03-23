include($$PWD/../../Source/WebKitJava.pri)

TEMPLATE = lib
CONFIG += plugin

CONFIG(debug, debug|release) {
    B_MODE = Debug
} else {
    B_MODE = Release
}

TARGET = DumpRenderTreeJava
DESTDIR = ../../$$B_MODE/lib
QMAKE_LIBDIR += $$DESTDIR
OBJECTS_DIR = $$B_MODE/obj

QMAKE_MACOSX_DEPLOYMENT_TARGET = 10.8

mac*|linux* {
    QMAKE_CXXFLAGS += -std=c++11
}

*clang* {
    QMAKE_CXXFLAGS += -stdlib=libc++ -Wno-c++11-narrowing
}

VPATH += \
    $$PWD \

INCLUDEPATH += \
    $$PWD \
    $$PWD/java \
    $$PWD/../../Source \
    $$PWD/../../Source/JavaScriptCore \
    $$PWD/../../Source/JavaScriptCore/icu \
    $$PWD/../../Source/WTF/ \
    $$PWD/../../Source/WTF/wtf \
    $$PWD/../../Source/WTF/wtf/java \
    $$PWD/../../Source/JavaScriptCore/ \
    $$PWD/../../Source/JavaScriptCore/assembler \
    $$PWD/../../Source/JavaScriptCore/bytecode \
    $$PWD/../../Source/JavaScriptCore/bytecompiler \
    $$PWD/../../Source/JavaScriptCore/bindings \
    $$PWD/../../Source/JavaScriptCore/builtins \
    $$PWD/../../Source/JavaScriptCore/ftl \
    $$PWD/../../Source/JavaScriptCore/heap \
    $$PWD/../../Source/JavaScriptCore/dfg \
    $$PWD/../../Source/JavaScriptCore/debugger \
    $$PWD/../../Source/JavaScriptCore/disassembler \
    $$PWD/../../Source/JavaScriptCore/disassembler/udis86 \
    $$PWD/../../Source/JavaScriptCore/interpreter \
    $$PWD/../../Source/JavaScriptCore/inspector \
    $$PWD/../../Source/JavaScriptCore/jit \
    $$PWD/../../Source/JavaScriptCore/llint \
    $$PWD/../../Source/JavaScriptCore/parser \
    $$PWD/../../Source/JavaScriptCore/profiler \
    $$PWD/../../Source/JavaScriptCore/runtime \
    $$PWD/../../Source/JavaScriptCore/tools \
    $$PWD/../../Source/JavaScriptCore/yarr \
    $$PWD/../../Source/JavaScriptCore/API \
    $$PWD/../../Source/JavaScriptCore/ForwardingHeaders \
    ../../../generated-src/headers

LIBS += -ljfxwebkit

win32-* {
    LIBS += -ladvapi32
    INCLUDEPATH += \
        $(WEBKIT_OUTPUTDIR)/import/include/icu        
}

linux-* {
    INCLUDEPATH += \
        $(WEBKIT_OUTPUTDIR)/import/include
}

mac* {
    LIBS += -lc++ -lobjc -framework AppKit
}

HEADERS += \
    GCController.h \
    LayoutTestController.h \
    WorkQueue.h \
    WorkQueueItem.h \
    java/JavaEnv.h \

SOURCES += \
#    ../../Source/WTF/wtf/CurrentTime.cpp \
#    ../../Source/WTF/wtf/Assertions.cpp \
#    ../../Source/WTF/wtf/FastMalloc.cpp \
    GCController.cpp \
    TestRunner.cpp \
    WorkQueue.cpp \
    java/DumpRenderTree.cpp \
    java/GCControllerJava.cpp \
    java/JavaEnv.cpp \
    java/TestRunnerJava.cpp \
    java/WorkQueueItemJava.cpp \
    java/EventSender.cpp \

DEFINES += USE_SYSTEM_MALLOC=1
