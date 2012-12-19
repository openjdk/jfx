#
#  There exist several targets which are by default empty and which can be 
#  used for execution of your targets. These targets are usually executed 
#  before and after some main targets. They are: 
#
#     .build-pre:              called before 'build' target
#     .build-post:             called after 'build' target
#     .clean-pre:              called before 'clean' target
#     .clean-post:             called after 'clean' target
#     .clobber-pre:            called before 'clobber' target
#     .clobber-post:           called after 'clobber' target
#     .all-pre:                called before 'all' target
#     .all-post:               called after 'all' target
#     .help-pre:                called before 'help' target
#     .help-post:               called after 'help' target
#
#  Targets beginning with '.' are not intended to be called on their own.
#
#  Main targets can be executed directly, and they are:
#  
#     build                    build a specific configuration
#     clean                    remove built files from a configuration
#     clobber                  remove all built files
#     all                      build all configurations
#     help                     print help mesage
#  
#  Targets .build-impl, .clean-impl, .clobber-impl, .all-impl, and
#  .help-impl are implemented in nbproject/makefile-impl.mk.
#
# NOCDDL


# Environment 
MKDIR=mkdir
CP=cp
CCADMIN=CCadmin
RANLIB=ranlib
LINK=link.exe
CC=cl.exe
MT=mt.exe
RSC=rc.exe

ifndef DIST_DIR
	DIST_DIR	= dist
endif
ifndef BUILD_DIR
	BUILD_DIR	= build
endif

WIN_LAUNCHER = $(DIST_DIR)/javafxpackager.exe
OBJS = $(BUILD_DIR)/main.obj

LINK_PARAMS	+= -out:$(WIN_LAUNCHER) -nologo /opt:REF /incremental:no 
LINK_PARAMS     += /manifest kernel32.lib advapi32.lib
CC_PARAMS	+= -nologo -MT -Fd$(BUILD_DIR)/ -GS -W3 -EHsc -DWIN32 
CC_PARAMS       += -D_LITTLE_ENDIAN -DWIN32_LEAN_AND_MEAN 
CC_PARAMS       += -D_WIN32_WINDOWS=0x0500 -D_WIN32_WINNT=0x0500 -I. 
CC_PARAMS       += -I$(BUILD_DIR) -I$(JDK_HOME)/include 
CC_PARAMS       += -I$(JDK_HOME)/include/win32 -I$(DXSDK_PATH)/Include -c
RC_FLAGS        = /l 0x409 /r /I$(BUILD_DIR) -d "JFX_DVERSION=$(JFX_DVERSION)" 
RC_FLAGS        += -d "JFX_VERSION=$(JFX_VERSION)" -d NDEBUG

JFX_VERSION=$(FX_MAJOR_VERSION),$(FX_MINOR_VERSION),$(FX_UPDATE_VERSION),${FX_BUILD_NUMBER}
JFX_DVERSION=$(FX_MAJOR_VERSION).$(FX_MINOR_VERSION).$(FX_UPDATE_VERSION).${FX_BUILD_NUMBER}

ifeq ($(CONF), Release)
	CC_PARAMS += -Zi -O2
else
	LINK_PARAMS += /debug
	CC_PARAMS += -Zi -Od -DDEBUG
endif

# build
build: .build-pre $(WIN_LAUNCHER) .build-post

$(WIN_LAUNCHER) : $(OBJS)
	$(RSC) $(RC_FLAGS) -fo$(BUILD_DIR)/javafxpackager.res javafxpackager.rc
	$(LINK) $(LINK_PARAMS) $(BUILD_DIR)/javafxpackager.res $^
	$(MT) -manifest $(BUILD_DIR)/javafxpackager.manifest -outputresource:$@\;#1

$(BUILD_DIR)/%.obj : %.cpp  $(INCLUDES)
	$(CC) $(CC_PARAMS) -Fo$@ $<

.build-pre:
	@mkdir -p $(BUILD_DIR) $(DIST_DIR)

.build-post:
# Add your post 'build' code here...


# clean
clean: .clean-pre .clean-post
	rm -rf $(BUILD_DIR) $(DIST_DIR) 

.clean-pre:
# Add your pre 'clean' code here...

.clean-post:
# Add your post 'clean' code here...


# clobber
clobber: .clobber-pre .clobber-impl .clobber-post

.clobber-pre:
# Add your pre 'clobber' code here...

.clobber-post:
# Add your post 'clobber' code here...


# all
all: .all-pre .all-impl .all-post

.all-pre:
# Add your pre 'all' code here...

.all-post:
# Add your post 'all' code here...


# help
help: .help-pre .help-impl .help-post

.help-pre:
# Add your pre 'help' code here...

.help-post:
# Add your post 'help' code here...



# include project implementation makefile
include nbproject/Makefile-impl.mk
