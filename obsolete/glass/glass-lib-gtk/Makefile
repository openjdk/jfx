
# Environment 
MKDIR=mkdir
CP=cp
CCADMIN=CCadmin
SHELL=/bin/bash # NOTE: bash-ism present in this makefile

BUILD_HEADERS   = $(BUILD_DIR)/include

GLASS_SRC       = src

VPATH           = $(GLASS_SRC)

SYSTEM_UNAME := $(shell uname)
IS_WINDOWS = false

ifeq ($(SYSTEM_UNAME), Linux)
	fix_path = $2

	OBJ_SUFFIX  = o
	OUTPUT_FLAG = -o # trailing blank required
	GLASS_LIB   = $(DIST_DIR)/libglass.so

	COMMON_PARAMS = -fno-strict-aliasing -fPIC -W -Wall  -Wno-unused -Wno-parentheses -fno-omit-frame-pointer -Werror=implicit-function-declaration
	LINK		= $(CXX)
	GTK_VERSION     = 2.0
#	GTK_VERSION     = 3.0
	PKGCONFIG_PATH=      # empty path, use the system default
	PKG_CONFIG_LIBRARIES = gtk+-$(GTK_VERSION) gthread-2.0 xtst
	LINK_PARAMS	= $(COMMON_PARAMS) -shared -o $(GLASS_LIB) $(EXTRA_LDFLAGS) `$(PKGCONFIG_PATH)pkg-config --libs $(PKG_CONFIG_LIBRARIES)`
	CXX_PARAMS = -Wnon-virtual-dtor -Woverloaded-virtual -std=c++0x
ifdef DEBUG
	CC_PARAMS = -ggdb -DVERBOSE
else
	CC_PARAMS = -O2 -ffast-math
endif
	CC_PARAMS += -Fd$(BUILD_DIR) $(COMMON_PARAMS) -I$(GLASS_SRC) -I$(BUILD_HEADERS) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux `$(PKGCONFIG_PATH)pkg-config --cflags $(PKG_CONFIG_LIBRARIES)` -c $(EXTRA_CFLAGS)
endif


INCLUDES        = \
	-I$(JDK_HOME)/include \
        -I./build/jheaders \
        -I$(GLASS_SRC)


# The following should automatically include all .cpp files in the src/ folder.
OBJS_cpp	:= $(patsubst $(GLASS_SRC)/%.cpp,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(GLASS_SRC)/*.cpp))
# Can manually add more files here if needed.
OBJS		:= $(OBJS_cpp)

all: .pre-build $(GLASS_LIB)

$(GLASS_LIB) : $(OBJS) 
	$(LINK) $^ $(LINK_PARAMS);
	@if [ "$(MT)" != "" ]; then \
		$(MT) -nologo -manifest $@.manifest "-outputresource:$@;2" || \
		rm -f $(GLASS_LIB);					      \
	fi;

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(GLASS_SRC)/%.c 
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(GLASS_SRC)/%.cpp
	$(CXX) $(CC_PARAMS) $(CXX_PARAMS) $(OUTPUT_FLAG)$@ $<

.pre-build:
	@mkdir -p $(BUILD_DIR) $(DIST_DIR)

# clean
clean: 
	rm -rf $(BUILD_DIR) $(DIST_DIR)

# clobber
clobber: 
	rm -rf $(BUILD_DIR) $(DIST_DIR)
