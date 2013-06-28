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
# the prism-common/build-*.xml files
# DIST_DIR = dist
# BUILD_DIR = build
ifndef DIST_DIR
	DIST_DIR = dist
endif
ifndef BUILD_DIR
	BUILD_DIR = build
endif
ifndef PRISM_COM_CP
	PRISM_COM_CP = ../prism-common/build/classes
endif
ifndef JAVAFX_GEOM_CP
	JAVAFX_GEOM_CP = ../../rt/javafx-geom/build/classes
endif
ifndef CONF
	CONF = Release
endif

PRISM_COM_SRC = src
PRISM_COM_PKG = com.sun.prism.impl.shape
PRISM_COM_PKG_PATH = $(subst .,/,$(PRISM_COM_PKG))
PRISM_COM_JNI_PKG = $(subst .,_,$(PRISM_COM_PKG))
JNI_INCLUDES := $(patsubst $(PRISM_COM_CP)/$(PRISM_COM_PKG_PATH)/%.class,$(BUILD_DIR)/$(PRISM_COM_JNI_PKG)_%.h,$(PRISM_COM_CP)/$(PRISM_COM_PKG_PATH)/NativePiscesRasterizer.class)
INCLUDES := $(wildcard $(PRISM_COM_SRC)/*.h)
I_JAVAH_CLASSPATH = $(PRISM_COM_CP):$(JAVAFX_GEOM_CP)

SYSTEM_UNAME := $(shell uname)

ifneq (,$(findstring CYGWIN,$(SYSTEM_UNAME)))
	fix_path = $(shell cygpath $1 $2)

	ifndef JDK_HOME
		JDK_HOME = c:/Progra~2/Java/jdk1.6.0_32
	endif

	OBJ_SUFFIX = obj
	OUTPUT_FLAG = -Fo
	PRISM_COM_LIB = $(DIST_DIR)/prism-common.dll
	PRISM_COM_MAP = $(DIST_DIR)/prism-common.map
	PRISM_COM_PDB = $(DIST_DIR)/prism-common.pdb
	JAVAH_CLASSPATH := $(subst :,\;,$(I_JAVAH_CLASSPATH))

	LINK =  link.exe
	CC =  cl.exe

	LINK_PARAMS	= -dll -out:$(PRISM_COM_LIB) -pdb:$(PRISM_COM_PDB) -map:$(PRISM_COM_MAP) -nologo /manifest /opt:REF /incremental:no
	CC_PARAMS	= -nologo -MD /D_STATIC_CPPLIB -Fd$(BUILD_DIR)/ -W3 -DINLINE=__inline -DUNICODE -D_UNICODE -DLOGGING -DWIN32 -DIAL -D_LITTLE_ENDIAN -DWIN32_LEAN_AND_MEAN -I$(PRISM_COM_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/win32 -c
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
	PRISM_COM_LIB	= $(DIST_DIR)/libprism-common.so
	JAVAH_CLASSPATH := $(I_JAVAH_CLASSPATH)

	COMMON_PARAMS = -fno-strict-aliasing -fPIC -W -Wall  -Wno-unused -Wno-parentheses -fno-omit-frame-pointer
    ifeq ($(CC), parfait-gcc)
		LINK = parfait-gcc
	else
		CC = gcc
		LINK = gcc
	endif
	LINK_PARAMS	= $(COMMON_PARAMS) -shared -o $(PRISM_COM_LIB)
	CC_PARAMS	= -O2 -ffast-math -Fd$(BUILD_DIR) $(COMMON_PARAMS) -I$(PRISM_COM_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/solaris -c
endif

ifeq ($(SYSTEM_UNAME), Linux)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = /usr/lib/jvm/java-6-sun
	endif
	OBJ_SUFFIX = o
	OUTPUT_FLAG = -o # trailing blank required
	PRISM_COM_LIB = $(DIST_DIR)/libprism-common.so
	JAVAH_CLASSPATH := $(I_JAVAH_CLASSPATH)

	COMMON_PARAMS = -fno-strict-aliasing -fPIC -W -Wall  -Wno-unused -Wno-parentheses -fno-omit-frame-pointer -Werror=implicit-function-declaration
	LINK 		= $(CC)
	LINK_PARAMS	= $(COMMON_PARAMS) -shared -o $(PRISM_COM_LIB)
	CC_PARAMS	= -O2 -ffast-math -Fd$(BUILD_DIR) -DINLINE=inline $(COMMON_PARAMS) -I$(PRISM_COM_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/linux -c

endif

ifeq ($(SYSTEM_UNAME), Darwin)
	fix_path = $2

	ifndef JDK_HOME
		JDK_HOME = $(shell /usr/libexec/java_home)
	endif
	OBJ_SUFFIX = o
	OUTPUT_FLAG = -o # trailing blank required
	
	JAVAH_CLASSPATH := $(I_JAVAH_CLASSPATH)
	
	ifeq ($(IS_IOS),true)
		IOS_SDK_BASE := $(shell xcode-select -print-path)
		IOS_DEV = $(IOS_SDK_BASE)/Platforms/$(IOS_PLATFORM).platform/Developer
		IOS_SDK = $(IOS_DEV)/SDKs/$(IOS_PLATFORM)$(IOS_VERSION).sdk

		CC = $(IOS_DEV)/usr/bin/gcc
		LINK = libtool
	
		PRISM_COM_LIB = $(DIST_DIR)/libprism-common-$(IOS_ARCH).a

		ifeq ($(CONF),Debug)
			COMMON_PARAMS = -gdwarf-2
		endif
		LINK_PARAMS = -static -arch_only $(IOS_ARCH) -syslibroot $(IOS_SDK) $(COMMON_PARAMS) -framework Foundation -L$(IOS_SDK)/usr/lib -o $(PRISM_COM_LIB)
		CC_PARAMS   = -arch $(IOS_ARCH) -isysroot $(IOS_SDK) -miphoneos-version-min=5.0
		CC_PARAMS  += -fno-common -Wall -fno-strict-aliasing -fwrapv  -fpascal-strings -O3
		CC_PARAMS  += -Fd$(BUILD_DIR) -DINLINE=inline $(COMMON_PARAMS) -I$(PRISM_COM_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/darwin -c
	else
		PRISM_COM_LIB = $(DIST_DIR)/libprism-common.dylib

		ifeq ($(CC), parfait-gcc)
			COMMON_PARAMS = -mmacosx-version-min=10.4
		else
			COMMON_PARAMS = -mmacosx-version-min=10.4 -arch i386 -arch x86_64 #-arch ppc
		endif
		LINK = $(CC)

		LINK_PARAMS	= -dynamiclib $(COMMON_PARAMS) -o $(PRISM_COM_LIB)
		CC_PARAMS	= -O3 -Fd$(BUILD_DIR) -DINLINE=inline $(COMMON_PARAMS) -I$(PRISM_COM_SRC) -I$(BUILD_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/darwin -c
	endif
endif

OBJS		:= $(patsubst $(PRISM_COM_SRC)/%.c,$(BUILD_DIR)/%.$(OBJ_SUFFIX),$(wildcard $(PRISM_COM_SRC)/*.c))

JAVAH = $(call fix_path,-u,$(JDK_HOME)/bin/javah)


# build
build: .build-pre $(PRISM_COM_LIB) .build-post

# generate parfait report, make must be launched with 
# CC=parfait-gcc CXX=parfait-g++ CPP=parfait-g++ cc=parfait-gcc LINK=parfait-gcc
parfait: build
	parfait $(PRISM_COM_LIB).bc -g $(DIST_DIR)/parfait


$(PRISM_COM_LIB) : $(OBJS)
	$(LINK) $(LINK_PARAMS) $^

#$(BUILD_DIR)/%.$(OBJ_SUFFIX) : %.c $(JNI_INCLUDES) $(INCLUDES)
#	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/%.$(OBJ_SUFFIX) : $(PRISM_COM_SRC)/%.c $(JNI_INCLUDES) $(INCLUDES)
	$(CC) $(CC_PARAMS) $(OUTPUT_FLAG)$@ $<

$(BUILD_DIR)/$(PRISM_COM_JNI_PKG)_%.h : $(PRISM_COM_CP)/$(PRISM_COM_PKG_PATH)/%.class
	$(JAVAH) -force -o $@ -jni -classpath $(JAVAH_CLASSPATH) $(PRISM_COM_PKG).$*

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
