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

# NOTE: the following variables are passed in from
# the pisces/build-*.xml files
ifndef DIST_DIR
	DIST_DIR = dist
endif
ifndef BUILD_DIR
	BUILD_DIR = build
endif
ifndef PRISM-SW_CP
	PRISM-SW_CP = ../prism-sw/build/classes
endif
ifndef CONF
	CONF = Release
endif

PRISM-SW_SRC = src
PRISM-SW_INCLUDE = include
PISCES_PKG = com.sun.pisces
PISCES_PKG_PATH = $(subst .,/,$(PISCES_PKG))
PISCES_JNI_PKG = $(subst .,_,$(PISCES_PKG))
JNI_INCLUDES := $(patsubst $(PRISM-SW_CP)/$(PISCES_PKG_PATH)/%.class,$(BUILD_DIR)/$(PISCES_JNI_PKG)_%.h,$(wildcard $(PRISM-SW_CP)/$(PISCES_PKG_PATH)/*.class))
INCLUDES = \
	$(PRISM-SW_INCLUDE)/JAbstractSurface.h   \
	$(PRISM-SW_INCLUDE)/JNIUtil.h            \
	$(PRISM-SW_INCLUDE)/JPiscesRenderer.h	   \
	$(PRISM-SW_INCLUDE)/JTransform.h		   \
	$(PRISM-SW_INCLUDE)/PiscesBlit.h		   \
	$(PRISM-SW_INCLUDE)/PiscesDefs.h		   \
	$(PRISM-SW_INCLUDE)/PiscesMath.h	       \
	$(PRISM-SW_INCLUDE)/PiscesPaint.h	       \
	$(PRISM-SW_INCLUDE)/PiscesRenderer.h	   \
	$(PRISM-SW_INCLUDE)/PiscesRenderer.inl   \
	$(PRISM-SW_INCLUDE)/PiscesSurface.h	   \
	$(PRISM-SW_INCLUDE)/PiscesSurface.inl	   \
	$(PRISM-SW_INCLUDE)/PiscesSysutils.h	   \
	$(PRISM-SW_INCLUDE)/PiscesTransform.h	   \
	$(PRISM-SW_INCLUDE)/PiscesUtil.h

SYSTEM_UNAME := $(shell uname)

ifneq (,$(findstring CYGWIN,$(SYSTEM_UNAME)))
	fix_path = $(shell cygpath $1 $2)

	ifndef JDK_HOME
		JDK_HOME = c:/jdk1.6.0
	endif

	OBJ_SUFFIX = obj
	OUTPUT_FLAG = -Fo
	PRISM-SW_LIB = $(DIST_DIR)/prism-sw.dll
	PRISM-SW_MAP = $(DIST_DIR)/prism-sw.map
	PRISM-SW_PDB = $(DIST_DIR)/prism-sw.pdb

	LINK =  link.exe
	CC =  cl.exe

	LINK_PARAMS	= -dll -out:$(PRISM-SW_LIB) -pdb:$(PRISM-SW_PDB) -map:$(PRISM-SW_MAP) -nologo /manifest /opt:REF /incremental:no
	CC_PARAMS	= -nologo -MD /D_STATIC_CPPLIB -Fd$(BUILD_DIR)/ -W3 -DINLINE=__inline -DUNICODE -D_UNICODE -DLOGGING -DWIN32 -DIAL -D_LITTLE_ENDIAN -DWIN32_LEAN_AND_MEAN -I$(PRISM-SW_SRC) -I$(PRISM-SW_INCLUDE) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/win32 -c
	ifeq ($(CDECL_WORKAROUND), true)
		# NOTE: Forcing __cdecl calling convention by making any use of __stdcall be ignored. JDK->cvm compatibility issue
		CC_PARAMS	+= /D__stdcall=
	endif
	ifeq ($(CONF), Release)
		CC_PARAMS += -Ox
	else
		LINK_PARAMS += /debug
		CC_PARAMS += -Zi -Od -DDEBUG
	endif
endif

ifeq ($(SYSTEM_UNAME), SunOS)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = /java/re/jdk/1.6.0_10/latest/binaries/solaris-i586
	endif
	OBJ_SUFFIX  = o
	OUTPUT_FLAG = -o # trailing blank required
	PRISM-SW_LIB	= $(DIST_DIR)/libprism-sw.so

	COMMON_PARAMS = -fno-strict-aliasing -fPIC -W -Wall  -Wno-unused -Wno-parentheses -fno-omit-frame-pointer
    ifeq ($(CC), parfait-gcc)
		LINK = parfait-gcc
	else
		CC = gcc
		LINK = gcc
	endif
	LINK_PARAMS	= $(COMMON_PARAMS) -shared -o $(PRISM-SW_LIB)
	CC_PARAMS	= -O2 -ffast-math -Fd$(BUILD_DIR) $(COMMON_PARAMS) -I$(PRISM-SW_SRC) -I$(PRISM-SW_INCLUDE) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/solaris -c
endif

ifeq ($(SYSTEM_UNAME), Linux)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = /usr/lib/jvm/java-6-sun
	endif
	OBJ_SUFFIX = o
	OUTPUT_FLAG = -o # trailing blank required
	PRISM-SW_LIB = $(DIST_DIR)/libprism-sw.so

	COMMON_PARAMS = -fno-strict-aliasing -fPIC -W -Wall  -Wno-unused -Wno-parentheses -fno-omit-frame-pointer -Werror=implicit-function-declaration
	LINK 		= $(CC)
	LINK_PARAMS	= $(COMMON_PARAMS) -shared -o $(PRISM-SW_LIB)
	CC_PARAMS	= -O2 -ffast-math -Fd$(BUILD_DIR) -DINLINE=inline $(COMMON_PARAMS) -I$(PRISM-SW_SRC) -I$(PRISM-SW_INCLUDE) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux -c

endif

ifeq ($(SYSTEM_UNAME), Darwin)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = $(shell /usr/libexec/java_home)
	endif
	OBJ_SUFFIX = o
	OUTPUT_FLAG = -o # trailing blank required
	PRISM-SW_LIB = $(DIST_DIR)/libprism-sw.dylib

    ifeq ($(CC), parfait-gcc)
		COMMON_PARAMS = -mmacosx-version-min=10.4
	else
		COMMON_PARAMS = -mmacosx-version-min=10.4 -arch i386 -arch x86_64 #-arch ppc
	endif
	LINK = $(CC)

	LINK_PARAMS	= -dynamiclib $(COMMON_PARAMS) -o $(PRISM-SW_LIB)
	CC_PARAMS	= -O3 -Fd$(BUILD_DIR) -DINLINE=inline $(COMMON_PARAMS) -I$(PRISM-SW_SRC) -I$(PRISM-SW_INCLUDE) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/darwin -c
endif

OBJS		:= $(patsubst $(PRISM-SW_SRC)/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(PRISM-SW_SRC)/*.c))
OBJS		+= \
	$(BUILD_DIR)/PiscesBlit.$(OBJ_SUFFIX) \
	$(BUILD_DIR)/PiscesMath.$(OBJ_SUFFIX) \
	$(BUILD_DIR)/PiscesTransform.$(OBJ_SUFFIX) \
	$(BUILD_DIR)/PiscesUtil.$(OBJ_SUFFIX)

JAVAH = $(call fix_path,-u,$(JDK_HOME)/bin/javah)


# build
build: .build-pre $(PRISM-SW_LIB) .build-post

# generate parfait report, make must be launched with 
# CC=parfait-gcc CXX=parfait-g++ CPP=parfait-g++ cc=parfait-gcc LINK=parfait-gcc
parfait: build
	parfait $(PISCES_LIB).bc -g $(DIST_DIR)/parfait


$(PRISM-SW_LIB) : $(OBJS)
	$(LINK) $(LINK_PARAMS) $^

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : %.c $(JNI_INCLUDES) $(INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(PRISM-SW_SRC)/%.c $(JNI_INCLUDES) $(INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/$(PISCES_JNI_PKG)_%.h : $(PRISM-SW_CP)/$(PISCES_PKG_PATH)/%.class
	$(JAVAH) -force -o $@ -jni -classpath $(PRISM-SW_CP) $(PISCES_PKG).$*

sanity:
	@if [ ! -d "$(JDK_HOME)" ]; then \
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
