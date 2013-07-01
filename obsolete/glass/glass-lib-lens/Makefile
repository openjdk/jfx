
# Environment 
MKDIR=mkdir
CP=cp
CCADMIN=CCadmin
SHELL=/bin/bash # NOTE: bash-ism present in this makefile

MAT_SRC         := src

VPATH           := $(MAT_SRC)

SYSTEM_UNAME := $(shell uname)
IS_WINDOWS = false


ifeq ($(SYSTEM_UNAME), Linux)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = /home/java2d/jdk/jdk1.6.0_10
	endif
	OBJ_SUFFIX  = o
	OUTPUT_FLAG = -o # trailing blank required
	MAT_LIB_FB	= $(DIST_DIR)/libglass-lens-fb.so
	MAT_LIB_DFB	= $(DIST_DIR)/libglass-lens-dfb.so
	MAT_LIB_EGLFB	= $(DIST_DIR)/libglass-lens-eglfb.so
	MAT_LIB_HEADLESS = $(DIST_DIR)/libglass-lens-headless.so

	COMMON_PARAMS     = -fno-strict-aliasing -fPIC -W -Wall  -Wno-unused -Wno-parentheses -fno-omit-frame-pointer \
                -Wl,--no-undefined -Werror
	LINK		  = $(CC)
	LINK_PARAMS_FB	  = $(COMMON_PARAMS) -shared -o $(MAT_LIB_FB) $(EXTRA_LDFLAGS)
	LINK_PARAMS_EGLFB = $(COMMON_PARAMS) -shared -o $(MAT_LIB_EGLFB) $(EXTRA_LDFLAGS)
	LINK_PARAMS_DFB	  = $(COMMON_PARAMS) -shared -o $(MAT_LIB_DFB) $(EXTRA_LDFLAGS)
	LINK_PARAMS_HEADLESS = $(COMMON_PARAMS) -shared -o $(MAT_LIB_HEADLESS) $(EXTRA_LDFLAGS)
	CC_PARAMS	  = -O2 -ffast-math -Fd$(BUILD_DIR) $(COMMON_PARAMS) -I$(MAT_SRC) -I$(BUILD_HEADERS) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux -c $(EXTRA_CFLAGS)
endif


########### Common Files ##############

# The following should automatically include all .c files in the src/ folder.
OBJS := $(patsubst $(MAT_SRC)/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(MAT_SRC)/*.c))
# Window Manager files + robot
OBJS += $(patsubst $(MAT_SRC)/wm/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(MAT_SRC)/wm/*.c))

#filter udev input and  cursor when building with headless
ifeq (,$(findstring headless,$(MAKECMDGOALS)))
    #udev input
    OBJS        += $(BUILD_DIR)/udevInput.$(OBJ_SUFFIX)
    #cursor 
    OBJS += $(patsubst $(MAT_SRC)/cursor/fbCursor/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(MAT_SRC)/cursor/fbCursor/*.c))
else
    #use NULL cursor for headless
    OBJS += $(patsubst $(MAT_SRC)/cursor/nullCursor/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(MAT_SRC)/cursor/nullCursor/*.c))
endif



#VNC support
ifneq (,$(findstring USE_RFB,$(EXTRA_CFLAGS)))
OBJS += $(BUILD_DIR)/rfb.$(OBJ_SUFFIX)
endif

# set screens

#FB screen
OBJS_FB   += $(BUILD_DIR)/fb/fbdevScreen.$(OBJ_SUFFIX)

#dfb screen
OBJS_DFB += $(BUILD_DIR)/dfb/dfbScreen.$(OBJ_SUFFIX)

#eglfb screen
ifeq (,$(findstring EGL_X11_FB_CONTAINER,$(EXTRA_CFLAGS)))
#use FB screen
OBJS_EGLFB      += $(BUILD_DIR)/eglfb/fbdevScreen.$(OBJ_SUFFIX)
else
#use X11 container as screen
OBJS_EGLFB      += $(BUILD_DIR)/eglfb/x11ContainerScreen.$(OBJ_SUFFIX)
endif

#headless screen
OBJS_HEADLESS      += $(BUILD_DIR)/headless/headlessScreen.$(OBJ_SUFFIX)

########### targets and rules
all: .pre-build $(MAT_LIB_DFB) $(MAT_LIB_HEADLESS)

fb: .pre-build $(MAT_LIB_FB)

dfb: .pre-build $(MAT_LIB_DFB)

eglfb: .pre-build $(MAT_LIB_EGLFB)

headless: .pre-build $(MAT_LIB_HEADLESS)

$(MAT_LIB_FB) : $(OBJS) $(OBJS_FB)
	$(LINK) $^ $(LINK_PARAMS_FB) ;
	@if [ "$(MT)" != "" ]; then \
		$(MT) -nologo -manifest $@.manifest "-outputresource:$@;2" || \
		rm -f $(MAT_LIB);					      \
	fi;

$(MAT_LIB_DFB) : $(OBJS) $(OBJS_DFB)
	$(LINK) $^ $(LINK_PARAMS_DFB) ;
	@if [ "$(MT)" != "" ]; then \
		$(MT) -nologo -manifest $@.manifest "-outputresource:$@;2" || \
		rm -f $(MAT_LIB);					      \
	fi;

$(MAT_LIB_EGLFB) : $(OBJS) $(OBJS_EGLFB)
	$(LINK) $^ $(LINK_PARAMS_EGLFB) ;
	@if [ "$(MT)" != "" ]; then \
		$(MT) -nologo -manifest $@.manifest "-outputresource:$@;2" || \
		rm -f $(MAT_LIB);					      \
	fi;

$(MAT_LIB_HEADLESS) : $(OBJS) $(OBJS_HEADLESS)
	$(LINK) $^ $(LINK_PARAMS_HEADLESS) ;
	@if [ "$(MT)" != "" ]; then \
		$(MT) -nologo -manifest $@.manifest "-outputresource:$@;2" || \
		rm -f $(MAT_LIB);					      \
	fi;


#Common
$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(MAT_SRC)/%.c 
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(MAT_SRC)/input/udev/%.c 
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(MAT_SRC)/lensRFB/%.c 
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(MAT_SRC)/input/x11Container/%.c 
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<
	
$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(MAT_SRC)/wm/%.c
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(MAT_SRC)/cursor/fbCursor/%.c
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(MAT_SRC)/cursor/nullCursor/%.c
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

	
#DFB
	
$(BUILD_DIR)/dfb/%.$(OBJ_SUFFIX) : $(MAT_SRC)/wm/screen/%.c
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

#FB

$(BUILD_DIR)/fb/%.$(OBJ_SUFFIX) : $(MAT_SRC)/wm/screen/%.c
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

#EGLFB

$(BUILD_DIR)/eglfb/%.$(OBJ_SUFFIX) : $(MAT_SRC)/wm/screen/%.c
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

#HEADLESS

$(BUILD_DIR)/headless/%.$(OBJ_SUFFIX) : $(MAT_SRC)/wm/screen/%.c
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<


.pre-build:
	@mkdir -p $(BUILD_DIR) $(DIST_DIR) \
            $(BUILD_DIR)/eglfb \
            $(BUILD_DIR)/fb \
            $(BUILD_DIR)/dfb \
            $(BUILD_DIR)/headless \

# clean
clean: 
	rm -rf $(BUILD_DIR) $(DIST_DIR)

# clobber
clobber: 
	rm -rf $(BUILD_DIR) $(DIST_DIR)

