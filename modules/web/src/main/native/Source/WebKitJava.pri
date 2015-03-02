BASE_DIR = $$PWD

CONFIG -= qt warn_on
CONFIG += warn_off

CONFIG(release, debug|release) {
    DEFINES *= NDEBUG
}

DEFINES += \
    BUILDING_JAVA__ \
    USE_SYSTEM_MALLOC \
    STATICALLY_LINKED_WITH_WebCore \
    STATICALLY_LINKED_WITH_JavaScriptCore \
    STATICALLY_LINKED_WITH_WTF

*-cross-* {
    INCLUDEPATH += $$(WK_INCLUDE)
    LIBPATH += $$(WK_LIB)
    QMAKE_CXXFLAGS += $$(WK_CFLAGS)
    QMAKE_LFLAGS += $$(WK_LFLAGS)
}

*-g++* {
    QMAKE_CXXFLAGS_RELEASE -= -O2 -O3
    QMAKE_CXXFLAGS_RELEASE += -O3
    QMAKE_CXXFLAGS -= -Wreturn-type -fno-strict-aliasing
    QMAKE_CXXFLAGS += -Wreturn-type -fno-strict-aliasing
}

mac*|linux* {
    QMAKE_CXXFLAGS -= -std=c++11
    QMAKE_CXXFLAGS += -std=c++11
}

INCLUDEPATH += $(JAVA_HOME)/include

linux-*|solaris-* {
    contains(DEFINES, ICU_UNICODE) {
        QMAKE_CXXFLAGS += $$system(icu-config --cppflags)
    }
    DEFINES += HAVE_STDINT_H
    LIBS += -lpthread

    linux-* {
        INCLUDEPATH += \
		    $(JAVA_HOME)/include/linux \
		    $(WEBKIT_OUTPUTDIR)/import/include
    }

    solaris-* {
        INCLUDEPATH += $(JAVA_HOME)/include/solaris
        solaris-cc {
            QMAKE_CXXFLAGS += -library=stlport4
            QMAKE_LFLAGS += -library=stlport4
#           stdxx4 may be used instead of stlport but is new in Sun Studio (in 2009) and actually not shipped yet
#           QMAKE_CXXFLAGS += -library=stdcxx4
#           QMAKE_LFLAGS += -library=stdcxx4
        }
   }
}

mac* {
    QMAKE_CXXFLAGS_X86_64 -= -mmacosx-version-min=10.5
    QMAKE_CXXFLAGS_X86_64 += -mmacosx-version-min=10.7
    contains(DEFINES, ICU_UNICODE=1) {
        LIBS += $$system(icu-config --ldflags) # the output contains '\n' symbol, qmake replaces it with "\c" string
        LIBS -= \c                             # a workaround for the above issue
    }
    INCLUDEPATH += $(JAVA_HOME)/include/darwin
}

CONFIG(release, debug|release) {
    linux-*|solaris-g++* {
        QMAKE_LFLAGS += -Xlinker -s
    }
    solaris-cc {
        QMAKE_CXXFLAGS += -xspace
        QMAKE_LFLAGS += -s
    }
    macx-g++ {
        QMAKE_LFLAGS += -dead_strip -Xlinker -x
    }
}

win32-* {
    INCLUDEPATH += $$BASE_DIR/JavaScriptCore/os-win32

    # static icu libraries on windows
    QMAKE_CXXFLAGS += -DU_STATIC_IMPLEMENTATION
    QMAKE_CFLAGS += -DU_STATIC_IMPLEMENTATION

    INCLUDEPATH += \
        $(WEBKIT_OUTPUTDIR)/import/include \
        $(JAVA_HOME)/include/win32
    LIBS += \
        -L$(WEBKIT_OUTPUTDIR)/import/lib \
        -lwinmm

    QMAKE_CXXFLAGS -= -Zc:wchar_t-
    QMAKE_CXXFLAGS += -w34100 -w34189 -wd4291 -wd4344 -wd4996 -MP4 -Zc:wchar_t #/D _STATIC_CPPLIB
#    QMAKE_CXXFLAGS_RELEASE += /O2 - QMAKE does it by default
    QMAKE_LFLAGS += /MAP

    DEFINES += \
        NOMINMAX # disable min/max macro defines in Windows.h
}

QMAKE_EXTRA_TARGETS += generated_files
