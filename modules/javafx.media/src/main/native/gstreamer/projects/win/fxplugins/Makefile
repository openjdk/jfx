#
# fxplugins Makefile
#

CC   = cl.exe
LINKER = link.exe

BASECLASSES_TARGET = baseclasses.lib
MODULES = $(BASECLASSES_TARGET)

export BUILD_DIR = $(shell cygpath -u "$(OUTPUT_DIR)/$(BUILD_TYPE)")
TARGET = $(BUILD_DIR)/$(BASE_NAME).dll

MANIFEST   = $(shell cygpath -ma "$(BUILD_DIR)/$(BASE_NAME).manifest")
IMPLIB     = $(shell cygpath -ma "$(BUILD_DIR)/$(BASE_NAME).lib")
export PDB = $(shell cygpath -ma "$(BUILD_DIR)/$(BASE_NAME).pdb")

SYSTEM_LIBS = glib-lite.lib \
              gstreamer-lite.lib \
              winmm.lib \
              strmiids.lib \
              kernel32.lib \
              user32.lib \
              shell32.lib \
              advapi32.lib \
              ole32.lib \
              oleaut32.lib \
              strmiids.lib

LDFLAGS = -out:$(shell cygpath -ma $(TARGET)) -nologo -incremental:no -libpath:$(shell cygpath -ma $(BUILD_DIR)) -dll $(SYSTEM_LIBS) \
          -nodefaultlib:libcmt -manifest -manifestfile:$(MANIFEST) -manifestuac:"level='asInvoker' uiAccess='false'" -implib:$(IMPLIB) \
          -subsystem:windows -tlbid:1 -dynamicbase -nxcompat -errorreport:queue

OBJBASE_DIR = $(BUILD_DIR)/obj/plugins
SRCBASE_DIR = ../../../plugins

GLIB_DIR        = ../../../3rd_party/glib
GSTREAMER_DIR   = ../../../gstreamer-lite
BASECLASSES_DIR = ../../../3rd_party/baseclasses

DIRLIST = dshowwrapper \
          javasource \
          progressbuffer \
          progressbuffer/win32

DEP_DIRS = $(BUILD_DIR) $(addprefix $(OBJBASE_DIR)/,$(DIRLIST))

C_SOURCES = javasource/javasource.c \
            javasource/marshal.c \
            progressbuffer/progressbuffer.c \
            progressbuffer/win32/filecache.c \
            progressbuffer/hlsprogressbuffer.c \
            fxplugins.c

CPP_SOURCES = dshowwrapper/Allocator.cpp \
              dshowwrapper/dshowwrapper.cpp \
              dshowwrapper/Sink.cpp \
              dshowwrapper/Src.cpp

COMPILER_FLAGS = -nologo -W3 -WX- -EHsc -GS -fp:precise \
                 -Zc:wchar_t -Zc:forScope- -analyze- -errorReport:queue -Gm-

INCLUDES =  $(addprefix -I$(SRCBASE_DIR)/,$(DIRLIST)) \
            -I$(SRCBASE_DIR) \
            -I$(GLIB_DIR) \
            -I$(GLIB_DIR)/glib \
            -I$(GLIB_DIR)/gmodule \
            -I$(GLIB_DIR)/build/win32/vs100 \
            -I$(GSTREAMER_DIR)/gstreamer \
            -I$(GSTREAMER_DIR)/gstreamer/libs \
            -I$(GSTREAMER_DIR)/gst-plugins-base/gst-libs \
            -I$(BASECLASSES_DIR)

CFLAGS =   -DWIN32 \
           -D_WINDOWS \
           -D_USRDLL \
           -DENABLE_PULL_MODE=1 \
           -DENABLE_SOURCE_SEEKING=1 \
           -DGSTREAMER_LITE \
           -DGST_REMOVE_DEPRECATED \
           -DGST_REMOVE_DISABLED \
           -DGST_DISABLE_GST_DEBUG \
           -DGST_DISABLE_LOADSAVE \
           -DG_DISABLE_DEPRECATED \
           -DG_DISABLE_ASSERT \
           -DG_DISABLE_CHECKS \
           -D_WINDLL \
           -D_MBCS \
           -DINITGUID \
	   $(INCLUDES) \
           $(COMPILER_FLAGS)

OBJECTS = $(patsubst %.cpp,$(OBJBASE_DIR)/%.obj,$(CPP_SOURCES)) $(patsubst %.c,$(OBJBASE_DIR)/%.obj,$(C_SOURCES))

ifeq ($(BUILD_TYPE), Release)
    CFLAGS += -DNDEBUG -Oy -MD -Gy -GF
    LDFLAGS += -opt:ref -opt:icf
    O1FLAGS = -O1
else # debug
    LDFLAGS += -debug -pdb:$(PDB)
    CFLAGS += -D_DEBUG -Oy- -RTC1 -MDd -DENABLE_VISUAL_STUDIO_MEMORY_LEAKS_DETECTION -wd"4018" -wd"4244" -wd"4274" -Zi -Fd$(PDB)
    O1FLAGS = -Od
endif

ifeq ($(ARCH), x32)
    LDFLAGS += -safeseh -MACHINE:x86
else
    CFLAGS += -D_WIN64
    LDFLAGS += -MACHINE:x64
endif

LIBS = $(addprefix $(BUILD_DIR)/,$(MODULES))

export BASECLASSES_DIR

.PHONY: default $(MODULES) list

default: $(MODULES)
	$(MAKE) $(TARGET)

$(TARGET): $(OBJECTS) $(LIBS)
	$(LINKER) $(LDFLAGS) $(shell cygpath -ma "$(RESOURCE)") $(foreach object,$(OBJECTS),$(shell cygpath -ma $(object))) $(foreach lib,$(LIBS),$(shell cygpath -ma $(lib)))

$(OBJECTS): | $(DEP_DIRS)

$(DEP_DIRS):
	@mkdir -p $(DEP_DIRS)

$(OBJBASE_DIR)/%.obj: $(SRCBASE_DIR)/%.cpp
	$(CC) $(CFLAGS) $(O1FLAGS) -TP -c -Fo$(shell cygpath -ma $@) $<

$(OBJBASE_DIR)/%.obj: $(SRCBASE_DIR)/%.c
	$(CC) $(CFLAGS) $(O1FLAGS) -TC -c -Fo$(shell cygpath -ma $@) $<

$(BASECLASSES_TARGET):
	$(MAKE) TARGET=$(BUILD_DIR)/$(BASECLASSES_TARGET) -f Makefile.BaseClasses
