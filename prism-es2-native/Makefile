
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
# the prism-es2/build-*.xml files
ifndef DIST_DIR
	DIST_DIR	= dist
endif
ifndef BUILD_DIR
	BUILD_DIR	= build
endif
ifndef PRISMES2_CLASS_DIR
	PRISMES2_CLASS_DIR	= ../prism-es2/build/classes
endif
ifndef PRISM_DEP_CP
	PRISM_DEP_CP    = ../../rt/prism-common/build/classes
endif

I_JAVAH_CLASSPATH = $(PRISMES2_CLASS_DIR):$(PRISM_DEP_CP)

ifndef CONF
	CONF		= Release
endif

PRISMES2_SRC	= src
PRISMES2_OBJS = $(patsubst $(PRISMES2_SRC)/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(PRISMES2_SRC)/*.c))

OGL_INCLUDE_PATH = $(PRISMES2_SRC)
OGL_INCLUDES = $(OGL_INCLUDE_PATH)/*.h $(OGL_INCLUDE_PATH)/*/*.h

PRISMES2_GL2_PKG	= com.sun.prism.es2
PRISMES2_GL2_PKG_PATH = $(subst .,/,$(PRISMES2_GL2_PKG))
PRISMES2_JNI_GL2_PKG = $(subst .,_,$(PRISMES2_GL2_PKG))
JNI_GL2_INCLUDES	:= $(patsubst $(PRISMES2_CLASS_DIR)/$(PRISMES2_GL2_PKG_PATH)/%.class,$(BUILD_DIR)/$(PRISMES2_JNI_GL2_PKG)_%.h,$(wildcard $(PRISMES2_CLASS_DIR)/$(PRISMES2_GL2_PKG_PATH)/GL*.class))

SYSTEM_UNAME := $(shell uname)

ifneq (,$(findstring CYGWIN,$(SYSTEM_UNAME)))
	fix_path = $(shell cygpath $1 $2)

	ifndef JDK_HOME
		JDK_HOME = c:/jdk1.6.0
	endif

	OBJ_SUFFIX  = obj
	OUTPUT_FLAG = -Fo
	PRISMES2_LIB	= $(DIST_DIR)/prism-es2.dll
	PRISMES2_MAP	= $(DIST_DIR)/prism-es2.map
	PRISMES2_PDB	= $(DIST_DIR)/prism-es2.pdb

	PRISMES2_NATIVE_SRC = $(PRISMES2_SRC)/windows
	PRISMES2_NATIVE_OBJS = $(patsubst $(PRISMES2_NATIVE_SRC)/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(PRISMES2_NATIVE_SRC)/*.c))
	INCLUDES	= \
		$(OGL_INCLUDES)

	ifndef PRISMES2_PLATFORM_CLASS_DIR
	    PRISMES2_PLATFORM_CLASS_DIR = ../prism-es2-win/build/classes
	endif
	PRISMES2_PLATFORM_PKG	= com.sun.prism.es2
	PRISMES2_PLATFORM_PKG_PATH = $(subst .,/,$(PRISMES2_PLATFORM_PKG))
	PRISMES2_JNI_PLATFORM_PKG = $(subst .,_,$(PRISMES2_PLATFORM_PKG))
	JNI_PLATFORM_INCLUDES	:= $(patsubst $(PRISMES2_PLATFORM_CLASS_DIR)/$(PRISMES2_PLATFORM_PKG_PATH)/%.class,$(BUILD_DIR)/$(PRISMES2_JNI_PLATFORM_PKG)_%.h,$(wildcard $(PRISMES2_PLATFORM_CLASS_DIR)/$(PRISMES2_PLATFORM_PKG_PATH)/*.class))

        JAVAH_CLASSPATH = $(subst :,\;,$(I_JAVAH_CLASSPATH))
	JAVAH_PLATFORM_CLASSPATH = $(PRISMES2_PLATFORM_CLASS_DIR)\;$(JAVAH_CLASSPATH)

	JNI_INCLUDES := $(JNI_GL2_INCLUDES) $(JNI_PLATFORM_INCLUDES)
	LINK		=  link.exe
	CC		=  cl.exe

	LINK_PARAMS	= -dll -out:$(PRISMES2_LIB) -pdb:$(PRISMES2_PDB) -map:$(PRISMES2_MAP) /nologo /manifest /OPT:REF,ICF /incremental:no /SUBSYSTEM:WINDOWS opengl32.lib gdi32.lib user32.lib kernel32.lib
	CC_PARAMS	+= /nologo /D_STATIC_CPPLIB /DWIN32 /D_LITTLE_ENDIAN -DUNICODE -D_UNICODE/DNDEBUG -Fd$(BUILD_DIR)/ /W3 /Ob1 /GF /Gy /GS -I$(OGL_INCLUDE_PATH) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/win32 -c
	ifeq ($(CONF), Release)
		CC_PARAMS += -Ox -MD
	else
		LINK_PARAMS += /debug
		CC_PARAMS += -Zi -Od -DDEBUG -MDd
	endif
endif

ifeq ($(SYSTEM_UNAME), SunOS)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = /java/re/jdk/1.6.0_10/latest/binaries/solaris-i586
	endif
	OBJ_SUFFIX  = o
	OUTPUT_FLAG = -o # trailing blank required
	PRISMES2_LIB	= $(DIST_DIR)/libprism-es2.so

	PRISMES2_NATIVE_SRC = $(PRISMES2_SRC)/x11
	PRISMES2_NATIVE_OBJS = $(patsubst $(PRISMES2_NATIVE_SRC)/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(PRISMES2_NATIVE_SRC)/*.c))
	INCLUDES	= \
		$(OGL_INCLUDES)

	ifndef PRISMES2_PLATFORM_CLASS_DIR
	    PRISMES2_PLATFORM_CLASS_DIR = ../prism-es2-x11/build/classes
	endif
	PRISMES2_PLATFORM_PKG	= com.sun.prism.es2
	PRISMES2_PLATFORM_PKG_PATH = $(subst .,/,$(PRISMES2_PLATFORM_PKG))
	PRISMES2_JNI_PLATFORM_PKG = $(subst .,_,$(PRISMES2_PLATFORM_PKG))
	JNI_PLATFORM_INCLUDES	:= $(patsubst $(PRISMES2_PLATFORM_CLASS_DIR)/$(PRISMES2_PLATFORM_PKG_PATH)/%.class,$(BUILD_DIR)/$(PRISMES2_JNI_PLATFORM_PKG)_%.h,$(wildcard $(PRISMES2_PLATFORM_CLASS_DIR)/$(PRISMES2_PLATFORM_PKG_PATH)/*.class))

        JAVAH_CLASSPATH = $(I_JAVAH_CLASSPATH)
	JAVAH_PLATFORM_CLASSPATH = $(PRISMES2_PLATFORM_CLASS_DIR):$(JAVAH_CLASSPATH)

	JNI_INCLUDES := $(JNI_GL2_INCLUDES) $(JNI_PLATFORM_INCLUDES)

	COMMON_PARAMS = -fno-strict-aliasing -fPIC -W -Wall  -Wno-unused -Wno-parentheses -fno-omit-frame-pointer
        ifeq ($(CC), parfait-gcc)
		LINK = parfait-gcc
	else
		CC = gcc
		LINK = gcc
	endif
	LINK_PARAMS	= $(COMMON_PARAMS)  -L/usr/lib/amd64 -lX11 -lGL -shared -o $(PRISMES2_LIB)
	CC_PARAMS	= -DSOLARIS -O2 -ffast-math -Fd$(BUILD_DIR) $(COMMON_PARAMS) -I$(OGL_INCLUDE_PATH) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/solaris -c
endif

ifeq ($(SYSTEM_UNAME), Linux)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = /home/java2d/jdk/jdk1.6.0_10
	endif
	OBJ_SUFFIX  = o
	OUTPUT_FLAG = -o # trailing blank required
	PRISMES2_LIB	= $(DIST_DIR)/libprism-es2.so

	PRISMES2_NATIVE_SRC = $(PRISMES2_SRC)/x11
	PRISMES2_NATIVE_OBJS = $(patsubst $(PRISMES2_NATIVE_SRC)/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(PRISMES2_NATIVE_SRC)/*.c))
	INCLUDES	= \
		$(OGL_INCLUDES)

	ifndef PRISMES2_PLATFORM_CLASS_DIR
	    PRISMES2_PLATFORM_CLASS_DIR = ../prism-es2-x11/build/classes
	endif
	PRISMES2_PLATFORM_PKG	= com.sun.prism.es2
	PRISMES2_PLATFORM_PKG_PATH = $(subst .,/,$(PRISMES2_PLATFORM_PKG))
	PRISMES2_JNI_PLATFORM_PKG = $(subst .,_,$(PRISMES2_PLATFORM_PKG))
	JNI_PLATFORM_INCLUDES	:= $(patsubst $(PRISMES2_PLATFORM_CLASS_DIR)/$(PRISMES2_PLATFORM_PKG_PATH)/%.class,$(BUILD_DIR)/$(PRISMES2_JNI_PLATFORM_PKG)_%.h,$(wildcard $(PRISMES2_PLATFORM_CLASS_DIR)/$(PRISMES2_PLATFORM_PKG_PATH)/*.class))

        JAVAH_CLASSPATH = $(I_JAVAH_CLASSPATH)
	JAVAH_PLATFORM_CLASSPATH = $(PRISMES2_PLATFORM_CLASS_DIR):$(JAVAH_CLASSPATH)

	JNI_INCLUDES := $(JNI_GL2_INCLUDES) $(JNI_PLATFORM_INCLUDES)

	COMMON_PARAMS = -fno-strict-aliasing -fPIC -W -Wall  -Wno-unused -Wno-parentheses -fno-omit-frame-pointer -Wl,--no-as-needed
	LINK		= $(CC)
	PRISMES2_LINK_LIBS := -lX11 -lXxf86vm -lGL
	LINK_PARAMS	= $(COMMON_PARAMS)  $(PRISMES2_LINK_LIBS) -shared -o $(PRISMES2_LIB)
	CC_PARAMS	= -DLINUX -O2 -ffast-math -Fd$(BUILD_DIR) $(COMMON_PARAMS) -I$(OGL_INCLUDE_PATH) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux $(EXTRA_CFLAGS) -c

endif

ifeq ($(SYSTEM_UNAME), Darwin)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = $(shell /usr/libexec/java_home)
	endif

	ifeq ($(IS_IOS),true)
		IOS_SDK_BASE := $(shell xcode-select -print-path)
		IOS_DEV = $(IOS_SDK_BASE)/Platforms/$(IOS_PLATFORM).platform/Developer
		IOS_SDK = $(IOS_DEV)/SDKs/$(IOS_PLATFORM)$(IOS_VERSION).sdk

		CC = $(IOS_DEV)/usr/bin/gcc
		LINK = libtool

		OBJ_SUFFIX  = o
		OUTPUT_FLAG = -o # trailing blank required
		PRISMES2_LIB	= $(DIST_DIR)/libprism-es2-$(IOS_ARCH).a

		PRISMES2_NATIVE_SRC = $(PRISMES2_SRC)/ios
		PRISMES2_NATIVE_OBJS = $(patsubst $(PRISMES2_NATIVE_SRC)/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(PRISMES2_NATIVE_SRC)/*.c)) \
			$(patsubst $(PRISMES2_NATIVE_SRC)/%.m,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(PRISMES2_NATIVE_SRC)/*.m))
		INCLUDES	= \
			$(OGL_INCLUDES)

		ifndef PRISMES2_PLATFORM_CLASS_DIR
		    PRISMES2_PLATFORM_CLASS_DIR = ../prism-es2-ios/build/classes
		endif
		PRISMES2_PLATFORM_PKG	= com.sun.prism.es2
		PRISMES2_PLATFORM_PKG_PATH = $(subst .,/,$(PRISMES2_PLATFORM_PKG))
		PRISMES2_JNI_PLATFORM_PKG = $(subst .,_,$(PRISMES2_PLATFORM_PKG))
		JNI_PLATFORM_INCLUDES	:= $(patsubst $(PRISMES2_PLATFORM_CLASS_DIR)/$(PRISMES2_PLATFORM_PKG_PATH)/%.class,$(BUILD_DIR)/$(PRISMES2_JNI_PLATFORM_PKG)_%.h,$(wildcard $(PRISMES2_PLATFORM_CLASS_DIR)/$(PRISMES2_PLATFORM_PKG_PATH)/*.class))

	        JAVAH_CLASSPATH = $(I_JAVAH_CLASSPATH)
		JAVAH_PLATFORM_CLASSPATH = $(PRISMES2_PLATFORM_CLASS_DIR):$(JAVAH_CLASSPATH)

		JNI_INCLUDES := $(JNI_GL2_INCLUDES) $(JNI_PLATFORM_INCLUDES)

		ifeq ($(CONF),Debug)
			COMMON_PARAMS = -gdwarf-2
		endif
		LINK_PARAMS = -static -arch_only $(IOS_ARCH) -syslibroot $(IOS_SDK) $(COMMON_PARAMS) -framework Foundation -framework CoreGraphics -framework UIKit -framework QuartzCore -framework OpenGLES -L$(IOS_SDK)/usr/lib -o $(PRISMES2_LIB)
		CC_PARAMS   = -arch $(IOS_ARCH) -isysroot $(IOS_SDK) -miphoneos-version-min=5.0
		CC_PARAMS  += -fno-common -Wall -fno-strict-aliasing -fwrapv -fpascal-strings -fobjc-abi-version=2 -fobjc-legacy-dispatch 
		CC_PARAMS  += -Fd$(BUILD_DIR) $(COMMON_PARAMS) -I$(OGL_INCLUDE_PATH) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/darwin -c 
	else
		OBJ_SUFFIX  = o
		OUTPUT_FLAG = -o # trailing blank required
		PRISMES2_LIB	= $(DIST_DIR)/libprism-es2.dylib

		PRISMES2_NATIVE_SRC = $(PRISMES2_SRC)/macosx
		PRISMES2_NATIVE_OBJS = $(patsubst $(PRISMES2_NATIVE_SRC)/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(PRISMES2_NATIVE_SRC)/*.c)) \
			$(patsubst $(PRISMES2_NATIVE_SRC)/%.m,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(PRISMES2_NATIVE_SRC)/*.m))
		INCLUDES	= \
			$(OGL_INCLUDES)

		ifndef PRISMES2_PLATFORM_CLASS_DIR
		    PRISMES2_PLATFORM_CLASS_DIR = ../prism-es2-mac/build/classes
		endif
		PRISMES2_PLATFORM_PKG	= com.sun.prism.es2
		PRISMES2_PLATFORM_PKG_PATH = $(subst .,/,$(PRISMES2_PLATFORM_PKG))
		PRISMES2_JNI_PLATFORM_PKG = $(subst .,_,$(PRISMES2_PLATFORM_PKG))
		JNI_PLATFORM_INCLUDES	:= $(patsubst $(PRISMES2_PLATFORM_CLASS_DIR)/$(PRISMES2_PLATFORM_PKG_PATH)/%.class,$(BUILD_DIR)/$(PRISMES2_JNI_PLATFORM_PKG)_%.h,$(wildcard $(PRISMES2_PLATFORM_CLASS_DIR)/$(PRISMES2_PLATFORM_PKG_PATH)/*.class))

	        JAVAH_CLASSPATH = $(I_JAVAH_CLASSPATH)
		JAVAH_PLATFORM_CLASSPATH = $(PRISMES2_PLATFORM_CLASS_DIR):$(JAVAH_CLASSPATH)

		JNI_INCLUDES := $(JNI_GL2_INCLUDES) $(JNI_PLATFORM_INCLUDES)

	        ifeq ($(CC), parfait-gcc)
			COMMON_PARAMS = -mmacosx-version-min=10.4
		else
			COMMON_PARAMS = -mmacosx-version-min=10.4 -arch i386 -arch x86_64 #-arch ppc
		endif
		LINK		= $(CC)
		LINK_PARAMS	=  -framework Cocoa -framework OpenGL -dynamiclib $(COMMON_PARAMS) -o $(PRISMES2_LIB)
		CC_PARAMS	= -DMACOSX -Fd$(BUILD_DIR) $(COMMON_PARAMS) -I$(OGL_INCLUDE_PATH) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/darwin -c
	endif
endif

OBJS		:= $(PRISMES2_OBJS) $(PRISMES2_NATIVE_OBJS)

JAVAH  = $(call fix_path,-u,$(JDK_HOME)/bin/javah)

# build
build: .build-pre $(PRISMES2_LIB) .build-post

# generate parfait report, make must be launched with
# CC=parfait-gcc CXX=parfait-g++ CPP=parfait-g++ cc=parfait-gcc LINK=parfait-g++
parfait: build
	parfait $(PRISMES2_LIB).bc -g $(DIST_DIR)/parfait


$(PRISMES2_LIB) : $(OBJS)
	$(LINK) $(LINK_PARAMS) $^

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(PRISMES2_SRC)/%.c  $(JNI_INCLUDES) $(INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(PRISMES2_NATIVE_SRC)/%.c $(JNI_INCLUDES) $(INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(PRISMES2_NATIVE_SRC)/%.m $(JNI_INCLUDES) $(INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/$(PRISMES2_JNI_GL2_PKG)_%.h : $(PRISMES2_CLASS_DIR)/$(PRISMES2_GL2_PKG_PATH)/%.class
	$(JAVAH) -force -o $@ -jni -classpath $(JAVAH_CLASSPATH) $(PRISMES2_GL2_PKG).$*

$(BUILD_DIR)/$(PRISMES2_JNI_PLATFORM_PKG)_%.h : $(PRISMES2_PLATFORM_CLASS_DIR)/$(PRISMES2_PLATFORM_PKG_PATH)/%.class
	$(JAVAH) -force -o $@ -jni -classpath $(JAVAH_PLATFORM_CLASSPATH) $(PRISMES2_PLATFORM_PKG).$*


sanity:
	@if [ ! -d "$(JDK_HOME)" ]; then \
		echo "ERROR: path to Java 6 SDK: $(JDK_HOME) is not found."; \
		echo "Override the location on the command line"; \
		echo "using ALT_BOOTDIR variable."; \
		echo "Sanity check failed."; \
		exit 1; \
	fi;

.build-pre: sanity
	@echo build_pre JNI_INCLUDES = $(JNI_INCLUDES)
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
