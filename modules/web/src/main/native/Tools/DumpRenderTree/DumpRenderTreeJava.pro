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

VPATH += \
    $$PWD \


INCLUDEPATH += \
    $$PWD \
    $$PWD/java \
    $$PWD/../../Source \
    $$PWD/../../Source/JavaScriptCore \
    $$PWD/../../Source/WTF/ \
    $$PWD/../../Source/WTF/wtf \
    $$PWD/../../Source/WTF/wtf/java \
    $$PWD/../../Source/JavaScriptCore/ForwardingHeaders \
    $$PWD/../../../build/javah

LIBS += -ljfxwebkit


win32-* {
    LIBS += -ladvapi32
}

mac* {
    LIBS += -lobjc -framework AppKit
}

HEADERS += \
    GCController.h \
    LayoutTestController.h \
    WorkQueue.h \
    WorkQueueItem.h \
    java/JavaEnv.h \

SOURCES += \
    ../../Source/WTF/wtf/CurrentTime.cpp \
    ../../Source/WTF/wtf/Assertions.cpp \
    ../../Source/WTF/wtf/FastMalloc.cpp \
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
