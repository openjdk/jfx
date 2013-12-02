#
# glib-lite Makefile
#
LINK = link.exe

GLIB_VERSION = 2.28.8
export GLIB_BASE_DIR = 3rd_party/glib/glib-$(GLIB_VERSION)

GLIB_TARGET = libglib-$(GLIB_VERSION).lib
GMODULE_TARGET = libmodule-$(GLIB_VERSION).lib
GOBJECT_TARGET = libgobject-$(GLIB_VERSION).lib
GTHREAD_TARGET = libgthread-$(GLIB_VERSION).lib
MODULES = $(GLIB_TARGET) $(GMODULE_TARGET) $(GOBJECT_TARGET) $(GTHREAD_TARGET)

export BUILD_DIR = $(shell cygpath -u "$(OUTPUT_DIR)/$(BUILD_TYPE)")
DEF_DIR   = ../../../$(GLIB_BASE_DIR)/build/win32/vs100

TARGET = $(BUILD_DIR)/$(BASE_NAME).dll

MANIFEST   = $(shell cygpath -ma "$(BUILD_DIR)/$(BASE_NAME).manifest")
IMPLIB     = $(shell cygpath -ma "$(BUILD_DIR)/$(BASE_NAME).lib")
export PDB = $(shell cygpath -ma "$(BUILD_DIR)/$(BASE_NAME).pdb")

SYSTEM_LIBS = Ws2_32.lib \
              kernel32.lib \
              user32.lib \
              shell32.lib \
              advapi32.lib \
              ole32.lib

LDFLAGS = -out:$(shell cygpath -ma $(TARGET)) -nologo -incremental:no -libpath:$(shell cygpath -ma $(BUILD_DIR)) -dll $(SYSTEM_LIBS) \
          -manifest -manifestfile:$(MANIFEST) -manifestuac:"level='asInvoker' uiAccess='false'" \
          -subsystem:windows -tlbid:1 -dynamicbase -nxcompat -errorreport:queue -implib:$(IMPLIB)

ifeq ($(BUILD_TYPE), Release)
    LDFLAGS += -def:$(DEF_DIR)/glib-lite.def -opt:ref -opt:icf
else # debug
    LDFLAGS += -debug -def:$(DEF_DIR)/glib-liteD.def -pdb:$(shell cygpath -ma $(PDB))
endif

ifeq ($(ARCH), x32)
    LDFLAGS += -MACHINE:x86
else
    LDFLAGS += -MACHINE:x64
endif

LIBS = $(addprefix $(BUILD_DIR)/,$(MODULES))

.PHONY: default list

default: $(TARGET)

$(TARGET): $(LIBS)
	$(LINK) $(LDFLAGS) $(shell cygpath -ma $(RESOURCE)) $(foreach lib,$(LIBS),$(shell cygpath -ma $(lib))) 

$(BUILD_DIR)/$(GLIB_TARGET):
	$(MAKE) TARGET=$(BUILD_DIR)/$(GLIB_TARGET) -f Makefile.glib

$(BUILD_DIR)/$(GMODULE_TARGET):
	$(MAKE) TARGET=$(BUILD_DIR)/$(GMODULE_TARGET) -f Makefile.gmodule

$(BUILD_DIR)/$(GOBJECT_TARGET):
	$(MAKE) TARGET=$(BUILD_DIR)/$(GOBJECT_TARGET) -f Makefile.gobject

$(BUILD_DIR)/$(GTHREAD_TARGET):
	$(MAKE) TARGET=$(BUILD_DIR)/$(GTHREAD_TARGET) -f Makefile.gthread
