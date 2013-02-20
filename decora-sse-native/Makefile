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
MKDIR		= mkdir
CP		= cp
CCADMIN		= CCadmin
RANLIB		= ranlib

ifndef DIST_DIR
	DIST_DIR	= dist
endif
ifndef BUILD_DIR
	BUILD_DIR	= build
endif
ifndef DECORA_SSE_CLASS_DIR
	DECORA_SSE_CLASS_DIR	= ../decora-sse/build/classes
endif
ifndef DECORA_DEP_CP
	DECORA_DEP_CP   = ../decora-runtime/build/classes:../../rt/javafx-geom/build/classes
endif
ifndef CONF
	CONF		= Release
endif

DECORA_GEN_CPP	= build/gensrc
DECORA_PKG	= com.sun.scenario.effect.impl.sw.sse
DECORA_PKG_PATH = $(subst .,/,$(DECORA_PKG))
DECORA_JNI_PKG = $(subst .,_,$(DECORA_PKG))

JNI_INCLUDES	:= $(patsubst $(DECORA_SSE_CLASS_DIR)/$(DECORA_PKG_PATH)/%.class,$(BUILD_DIR)/$(DECORA_JNI_PKG)_%.h,$(wildcard $(DECORA_SSE_CLASS_DIR)/$(DECORA_PKG_PATH)/*.class))
I_JAVAH_CLASSPATH = $(DECORA_SSE_CLASS_DIR):$(DECORA_DEP_CP)

ifneq (,$(findstring CYGWIN,$(shell uname)))
	fix_path = $(shell cygpath $1 $2)
        JAVAH_CLASSPATH = $(subst :,\;,$(I_JAVAH_CLASSPATH))
	OBJ_SUFFIX  = obj
	OUTPUT_FLAG = -Fo
	DECORA_LIB	= $(DIST_DIR)/decora-sse.dll
	DECORA_MAP	= $(DIST_DIR)/decora-sse.map
	DECORA_PDB	= $(DIST_DIR)/decora-sse.pdb

	LINK		=  link.exe
	CC		=  cl.exe

	LINK_PARAMS	= -dll -out:$(DECORA_LIB) -pdb:$(DECORA_PDB) -map:$(DECORA_MAP) -nologo /manifest /opt:REF /incremental:no
	CC_PARAMS	= -nologo -MD /D_STATIC_CPPLIB -Fd$(BUILD_DIR)/ -W3 -EHsc -DUNICODE -D_UNICODE -DLOGGING -DWIN32 -DIAL -D_LITTLE_ENDIAN -DWIN32_LEAN_AND_MEAN -I. -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/win32 -c
	ifeq ($(CONF), Release)
		CC_PARAMS += -Ox /arch:SSE /fp:fast
	else
		LINK_PARAMS += /debug
		CC_PARAMS += -Zi -Od -DDEBUG -arch:SSE
	endif
else
	fix_path = $2
        JAVAH_CLASSPATH = $(I_JAVAH_CLASSPATH)

	ifndef JDK_HOME
		JDK_HOME = $(shell /usr/libexec/java_home)
	endif
	OBJ_SUFFIX  = o
	OUTPUT_FLAG = -o 
	DECORA_LIB	= $(DIST_DIR)/libdecora-sse.dylib

        ifeq ($(CC), parfait-gcc)
		COMMON_PARAMS = -mmacosx-version-min=10.4
		LINK = parfait-g++
	else
		COMMON_PARAMS = -mmacosx-version-min=10.4 -arch i386 -arch x86_64 #-arch ppc
		LINK = g++
	endif
	LINK_PARAMS	= -dynamiclib $(COMMON_PARAMS) -o $(DECORA_LIB)
	CC_PARAMS	= -O1 -ffast-math -Fd$(BUILD_DIR) $(COMMON_PARAMS) -I. -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/darwin -c
endif

OBJS		:= $(patsubst $(DECORA_GEN_CPP)/%.cc,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(DECORA_GEN_CPP)/*.cc))
OBJS		+= \
	$(BUILD_DIR)/SSEUtils.$(OBJ_SUFFIX) \
	$(BUILD_DIR)/SSEBoxBlurPeer.$(OBJ_SUFFIX) \
	$(BUILD_DIR)/SSEBoxShadowPeer.$(OBJ_SUFFIX) \
	$(BUILD_DIR)/SSELinearConvolvePeer.$(OBJ_SUFFIX) \
	$(BUILD_DIR)/SSELinearConvolveShadowPeer.$(OBJ_SUFFIX)

JAVAH	= $(call fix_path,-u,$(JDK_HOME)/bin/javah)


# build
build: .build-pre $(DECORA_LIB) .build-post

# generate parfait report, make must be launched with 
# CC=parfait-gcc CXX=parfait-g++ CPP=parfait-g++ cc=parfait-gcc LINK=parfait-g++
parfait: build
	parfait $(DECORA_LIB).bc -g $(DIST_DIR)/parfait

$(DECORA_LIB) : $(OBJS)
	$(LINK) $(LINK_PARAMS) $^

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : %.cc $(JNI_INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(DECORA_GEN_CPP)/%.cc $(JNI_INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/$(DECORA_JNI_PKG)_%.h : $(DECORA_SSE_CLASS_DIR)/$(DECORA_PKG_PATH)/%.class
	$(JAVAH) -o $@ -jni -classpath $(JAVAH_CLASSPATH) $(DECORA_PKG).$*

sanity:
	@if [ ! -d $(JDK_HOME) ]; then \
		echo "ERROR: path to Java 6 SDK: $(JDK_HOME) is not found."; \
		echo "Override the location on the command line"; \
		echo "using ALT_BOOTDIR variable."; \
		echo "Sanity check failed."; \
		exit 1; \
	fi;

.build-pre: sanity
	@mkdir -p $(BUILD_DIR) $(DIST_DIR)

.build-post:
# Add your post 'build' code here...


# clean
#clean: .clean-pre .clean-impl .clean-post
clean: .clean-pre .clean-post
	rm -rf $(BUILD_DIR) $(DIST_DIR)

.clean-pre:
# Add your pre 'clean' code here...

.clean-post:
# Add your post 'clean' code here...


# clobber
#clobber: .clobber-pre .clobber-impl .clobber-post
clobber: .clobber-pre .clobber-post
	rm -rf $(BUILD_DIR) $(DIST_DIR)

.clobber-pre:

.clobber-post:
# Add your post 'clobber' code here...


# all
#all: .all-pre .all-impl .all-post
all: .all-pre build .all-post

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

.PRECIOUS: $(JNI_INCLUDES)

# include project implementation makefile
include nbproject/Makefile-impl.mk
