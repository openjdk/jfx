#
# gstreamer-lite Makefile
#

LINKER = link.exe

GLIB_VERSION = 2.56.1
export GLIB_DIR = ../../../3rd_party/glib/glib-$(GLIB_VERSION)

GSTREAMER_TARGET = libgstreamer.lib
GSTPLUGINS_TARGET = libgstplugins.lib
MODULES = $(GSTREAMER_TARGET) $(GSTPLUGINS_TARGET)

export BUILD_DIR = $(shell cygpath -u "$(OUTPUT_DIR)/$(BUILD_TYPE)")
DEF_DIR   = ..

TARGET = $(BUILD_DIR)/$(BASE_NAME).dll

MANIFEST   = $(shell cygpath -ma "$(BUILD_DIR)/$(BASE_NAME).manifest")
IMPLIB     = $(shell cygpath -ma "$(BUILD_DIR)/$(BASE_NAME).lib")
export PDB = $(shell cygpath -ma "$(BUILD_DIR)/$(BASE_NAME).pdb")

SYSTEM_LIBS = glib-lite.lib \
              Ws2_32.lib \
              kernel32.lib \
              user32.lib \
              shell32.lib \
              advapi32.lib \
              ole32.lib \
              DSound.lib

LDFLAGS = -out:$(shell cygpath -ma $(TARGET)) -nologo -incremental:no -libpath:$(shell cygpath -ma $(BUILD_DIR)) -dll $(SYSTEM_LIBS) \
          -manifest -manifestfile:$(MANIFEST) -manifestuac:"level='asInvoker' uiAccess='false'" \
          -subsystem:windows -tlbid:1 -dynamicbase -nxcompat -errorreport:queue -implib:$(IMPLIB) \
          -def:$(DEF_DIR)/gstreamer-lite.def

ifeq ($(BUILD_TYPE), Release)
    LDFLAGS +=  -opt:ref -opt:icf
else # debug
    LDFLAGS += -debug -pdb:$(PDB)
endif

ifeq ($(ARCH), x32)
    LDFLAGS += -safeseh -MACHINE:x86
else
    LDFLAGS += -MACHINE:x64
endif

LIBS = $(addprefix $(BUILD_DIR)/,$(MODULES))

.PHONY: default $(MODULES) list

default: $(MODULES)
	$(MAKE) $(TARGET)

$(TARGET): $(LIBS)
	$(LINKER) $(LDFLAGS) $(shell cygpath -ma "$(RESOURCE)") $(foreach lib,$(LIBS),$(shell cygpath -ma $(lib)))

$(GSTREAMER_TARGET):
	$(MAKE) TARGET=$(BUILD_DIR)/$(GSTREAMER_TARGET) -f Makefile.gstreamer

$(GSTPLUGINS_TARGET):
	$(MAKE) TARGET=$(BUILD_DIR)/$(GSTPLUGINS_TARGET) -f Makefile.gstplugins
