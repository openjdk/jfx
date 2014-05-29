
# Environment
MKDIR=mkdir
CP=cp
CCADMIN=CCadmin
SHELL=/bin/bash # NOTE: bash-ism present in this makefile

# NOTE: the following three variables are passed in from
# the javafx-iio/build-*.xml files
DIST_DIR        = ../dist
BUILD_DIR       = ../build

BUILD_HEADERS   = $(BUILD_DIR)/include

PTY_SRC         = .

VPATH           = $(PTY_SRC)

SYSTEM_UNAME := $(shell uname)
IS_WINDOWS = false


ifeq ($(SYSTEM_UNAME), Linux)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = /home/java2d/jdk/jdk1.6.0_10
	endif
	OBJ_SUFFIX  = o
	OUTPUT_FLAG = -o # trailing blank required
	PTY_LIB	= $(DIST_DIR)/libpty.so

	COMMON_PARAMS = -fno-strict-aliasing -fPIC -W -Wall  -Wno-unused -Wno-parentheses -fno-omit-frame-pointer
	LINK		= $(CC)
	LINK_PARAMS	= $(COMMON_PARAMS) -shared -o $(PTY_LIB) $(EXTRA_LDFLAGS) -lX11
	CC_PARAMS	= -O2 -ffast-math -Fd$(BUILD_DIR) $(COMMON_PARAMS) -I$(PTY_SRC) -I$(BUILD_HEADERS) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux -c $(EXTRA_CFLAGS)
endif


INCLUDES        = \
	-I$(JDK_HOME)/include \
        -I./build/jheaders \
        -I$(PTY_SRC)


# The following should automatically include all .c files in the src/ folder.
OBJS		:= $(patsubst $(PTY_SRC)/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(PTY_SRC)/*.c))
# Can manually add more files here if needed.
OBJS		+= \

all: .pre-build $(PTY_LIB)

$(PTY_LIB) : $(OBJS)
	$(LINK) $(LINK_PARAMS) $^ ;
	@if [ "$(MT)" != "" ]; then \
		$(MT) -nologo -manifest $@.manifest "-outputresource:$@;2" || \
		rm -f $(PTY_LIB);					      \
	fi;

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(PTY_SRC)/%.c
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

.pre-build:
	@mkdir -p $(BUILD_DIR) $(DIST_DIR)

# clean
clean:
	rm -rf $(BUILD_DIR) $(DIST_DIR)

# clobber
clobber:
	rm -rf $(BUILD_DIR) $(DIST_DIR)

