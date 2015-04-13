# -------------------------------------------------------------------
# Project file for the LLIntOffsetsExtractor binary, used to generate
# derived sources for JavaScriptCore.
#
# See 'Tools/qmake/README' for an overview of the build system
# -------------------------------------------------------------------

TEMPLATE = app
TARGET = LLIntOffsetsExtractor
DESTDIR = Programs
CONFIG -= debug_and_release

QMAKE_MACOSX_DEPLOYMENT_TARGET = 10.8

CONFIG(release, debug|release) {
    DEFINES *= NDEBUG
}

mac*|linux* {
    QMAKE_CXXFLAGS += -std=c++11

    CONFIG -= app_bundle
}

mac* {
    INCLUDEPATH += \
        $(JAVA_HOME)/include/darwin
}

linux* {
    INCLUDEPATH += \
        $(JAVA_HOME)/include/linux \
        $(WEBKIT_OUTPUTDIR)/import/include
}

win* {
    INCLUDEPATH += \
        $(JAVA_HOME)/include/win32

    CONFIG -= windows
    CONFIG += console

    DEFINES += \
        NOMINMAX # disable min/max macro defines in Windows.h
}

*clang* {
    QMAKE_CXXFLAGS += -stdlib=libc++
}

debug_and_release {
    CONFIG += force_build_all
    CONFIG += build_all
}

DEFINES += \
    BUILDING_JAVA__ \
    BUILDING_JavaScriptCore \
    BUILDING_WTF \
    BUILD_WEBKIT \
    STATICALLY_LINKED_WITH_WebCore \
    STATICALLY_LINKED_WITH_JavaScriptCore \
    STATICALLY_LINKED_WITH_WTF \
    ICU_UNICODE=1 \
    ENABLE_PROMISES=1 \
    WTF_USE_EXPORT_MACROS=0 \
    USE_SYSTEM_MALLOC \
    JS_EXPORT_PRIVATE


mac* {
    # on macosx, we do not have icu headeres for system libraries,
    # so a snapshot of icu headers is used.
    INCLUDEPATH += $$PWD/icu  
}

INCLUDEPATH += \
    $$PWD/.. \
    $$PWD/../WTF \
    $$PWD \
    $$PWD/runtime \
    $$PWD/bytecode \
    $$PWD/heap \
    $$PWD/jit \
    $$PWD/assembler \
    $$PWD/disassembler \
    $$PWD/llint \
    $$PWD/profiler \
    $$PWD/parser \
    $$PWD/interpreter \
    $$PWD/API \
    $$PWD/dfg \
    $$PWD/debugger \
    $$PWD/ForwardingHeaders \
    $(WEBKIT_OUTPUTDIR)/import/include/icu \
    $(JAVA_HOME)/include

LLINT_DEPENDENCY = $$PWD/offlineasm/generate_offset_extractor.rb
INPUT_FILES = $$PWD/llint/LowLevelInterpreter.asm

llint.output = LLIntDesiredOffsets.h
llint.script = $$PWD/offlineasm/generate_offset_extractor.rb
llint.input = INPUT_FILES
llint.depends = $$LLINT_DEPENDENCY
llint.commands = ruby $$llint.script ${QMAKE_FILE_NAME} ${QMAKE_FILE_OUT}
llint.CONFIG += no_link
QMAKE_EXTRA_COMPILERS += llint

# Compilation of this file will automatically depend on LLIntDesiredOffsets.h
# due to qmake scanning the source file for header dependencies.
SOURCES = llint/LLIntOffsetsExtractor.cpp

