#
# MacOS libffi Makefile
#

ifeq ($(ARCH), arm64)
    MARCH = aarch64
else
    MARCH = x86
endif
BUILD_DIR = $(OUTPUT_DIR)/$(BUILD_TYPE)
TARGET_DIRS = $(BUILD_DIR)

SRCBASE_DIR = ../../../3rd_party/libffi
OBJBASE_DIR = $(BUILD_DIR)/obj/libffi

DIRLIST = src src/$(MARCH)

TARGET_NAME = lib$(BASE_NAME).a
TARGET = $(BUILD_DIR)/$(TARGET_NAME)

CFLAGS += -c \
         -Werror=implicit-function-declaration \
         -fPIC \
         -DGSTREAMER_LITE

ifeq ($(ARCH), arm64)
    CFLAGS += -DAARCH64
else
    CFLAGS += -DX86_64
endif

ifeq ($(BUILD_TYPE), Release)
    CFLAGS += -Os
else
    CFLAGS += -O0 -g -Wall
endif

# INCLUDES = -I$(SRCBASE_DIR)/include -I$(SRCBASE_DIR)/include/mac/aarch64 -I$(SRCBASE_DIR)/msvc_build/aarch64/aarch64_include
INCLUDES = -I$(SRCBASE_DIR)/src/$(MARCH) -I$(SRCBASE_DIR)/include -I$(SRCBASE_DIR)/include/mac/$(MARCH)

C_SOURCES = src/closures.c \
	   src/java_raw_api.c \
	   src/prep_cif.c \
	   src/raw_api.c \
	   src/types.c \
           src/$(MARCH)/ffi.c

ifneq ($(ARCH), arm64)
    C_SOURCES += src/$(MARCH)/ffi64.c src/$(MARCH)/ffiw64.c
endif

ifeq ($(ARCH), arm64)
    ASM_SOURCES = src/$(MARCH)/sysv.S
else
    ASM_SOURCES = src/$(MARCH)/unix64.S \
                  src/$(MARCH)/win64.S
endif


OBJ_DIRS = $(addprefix $(OBJBASE_DIR)/,$(DIRLIST))
OBJECTS  = $(patsubst %.c,$(OBJBASE_DIR)/%.o,$(C_SOURCES))\
	   $(patsubst %.S,$(OBJBASE_DIR)/%.o,$(ASM_SOURCES))

.PHONY: default list

default: $(TARGET)

$(OBJBASE_DIR)/%.o: $(SRCBASE_DIR)/%.c
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(OBJBASE_DIR)/%.o: $(SRCBASE_DIR)/%.S
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

$(OBJECTS): | $(OBJ_DIRS) $(TARGET_DIRS)

$(OBJ_DIRS):
	mkdir -p $(OBJ_DIRS)

$(TARGET_DIRS):
	mkdir -p $(TARGET_DIRS)

$(TARGET): $(OBJECTS)
	$(AR) -static $(OBJECTS) -arch_only $(ARCH) -o $@
