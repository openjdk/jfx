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

# NOTE: the following variables are passed in from
# the prism-d3d/build.xml file
ifndef DIST_DIR
	DIST_DIR	= dist
endif
ifndef BUILD_DIR
	BUILD_DIR	= build
endif
ifndef PRISM_D3D_CLASS_DIR
	PRISM_D3D_CLASS_DIR	= ../prism-d3d/build/classes
endif
ifndef PRISM_DEP_CP
	PRISM_DEP_CP    = ../prism-ps/build/classes\;../prism-common/build/classes\;../javafx-geom/build/classes\;../glass/glass/build/classes
endif
ifndef FXC_ARCH
	FXC_ARCH	= x86
endif
ifndef CONF
	CONF		= Release
endif

SRC_DIR         = src
PRISM_LIB	= $(DIST_DIR)/prism-d3d.dll
PRISM_MAP	= $(DIST_DIR)/prism-d3d.map
PRISM_PDB	= $(DIST_DIR)/prism-d3d.pdb
PRISM_PKG	= com.sun.prism.d3d
PRISM_PKG_PATH = $(subst .,/,$(PRISM_PKG))
PRISM_JNI_PKG = $(subst .,_,$(PRISM_PKG))

SRC_HLSL_3D_DIR = $(SRC_DIR)/hlsl
BUILD_HLSL_3D_DIR = $(BUILD_DIR)/hlsl

JNI_INCLUDES	=						\
	 $(BUILD_DIR)/$(PRISM_JNI_PKG)_D3DContext.h		\
	 $(BUILD_DIR)/$(PRISM_JNI_PKG)_D3DGraphics.h		\
	 $(BUILD_DIR)/$(PRISM_JNI_PKG)_D3DPipeline.h		\
	 $(BUILD_DIR)/$(PRISM_JNI_PKG)_D3DResourceFactory.h	\
	 $(BUILD_DIR)/$(PRISM_JNI_PKG)_D3DShader.h		\
	 $(BUILD_DIR)/$(PRISM_JNI_PKG)_D3DSwapChain.h		\
	 $(BUILD_DIR)/$(PRISM_JNI_PKG)_D3DVertexBuffer.h

INCLUDES	=					\
	$(SRC_DIR)/D3DBadHardware.h			\
	$(SRC_DIR)/D3DContext.h				\
	$(SRC_DIR)/D3DPipeline.h			\
	$(SRC_DIR)/D3DPipelineManager.h			\
	$(SRC_DIR)/D3DResourceManager.h			\
	$(SRC_DIR)/TextureUploader.h			\
	$(SRC_DIR)/Trace.h				\
	$(SRC_DIR)/interface.h				\
	$(SRC_DIR)/D3DLight.h				\
	$(SRC_DIR)/D3DMesh.h				\
	$(SRC_DIR)/D3DMeshView.h			\
	$(SRC_DIR)/D3DPhongMaterial.h			\
	$(SRC_DIR)/D3DPhongShader.h

HLSL_3D_INCLUDES	=				\
	$(SRC_HLSL_3D_DIR)/devColor.h			\
	$(SRC_HLSL_3D_DIR)/psConstants.h		\
	$(SRC_HLSL_3D_DIR)/psMath.h			\
	$(SRC_HLSL_3D_DIR)/psConstants.h		\
	$(SRC_HLSL_3D_DIR)/vs2ps.h			\
	$(SRC_HLSL_3D_DIR)/vsConstants.h		\
	$(SRC_HLSL_3D_DIR)/vsDecl.h			\
	$(SRC_HLSL_3D_DIR)/vsMath.h

VS_SRC		= $(SRC_DIR)/PassThroughVS.hlsl
VS_INCLUDE	= $(BUILD_DIR)/PassThroughVS.h

PS_3D_SRC	= $(SRC_HLSL_3D_DIR)/Mtl1PS.hlsl
PS_3D_INCLUDE	= $(BUILD_HLSL_3D_DIR)/Mtl1PS.h

VS_3D_SRC	= $(SRC_HLSL_3D_DIR)/Mtl1VS.hlsl
VS_3D_INCLUDE	= $(BUILD_HLSL_3D_DIR)/Mtl1VS_Obj.h

OBJS		=					\
	$(BUILD_DIR)/D3DGraphics.obj			\
	$(BUILD_DIR)/D3DPipeline.obj			\
	$(BUILD_DIR)/D3DPipelineManager.obj		\
	$(BUILD_DIR)/D3DResourceManager.obj		\
	$(BUILD_DIR)/D3DResourceFactory.obj		\
	$(BUILD_DIR)/D3DContext.obj			\
	$(BUILD_DIR)/D3DContextInit.obj			\
	$(BUILD_DIR)/D3DShader.obj			\
	$(BUILD_DIR)/TextureUploader.obj		\
	$(BUILD_DIR)/D3DLight.obj			\
	$(BUILD_DIR)/D3DMesh.obj			\
	$(BUILD_DIR)/D3DMeshView.obj			\
	$(BUILD_DIR)/D3DPhongMaterial.obj		\
	$(BUILD_DIR)/D3DPhongShader.obj			\
	$(BUILD_DIR)/D3DPhongShaderGen.obj		\
	$(BUILD_DIR)/Trace.obj

JAVAH	= $(shell cygpath -u $(JDK_HOME)/bin/javah)
FXC	= $(shell cygpath -u $(DXSDK_PATH)/utilities/bin/$(FXC_ARCH)/fxc)

LINK	=  link.exe
CC	=  cl.exe

LINK_PARAMS	= -dll -out:$(PRISM_LIB) -pdb:$(PRISM_PDB) -map:$(PRISM_MAP) -nologo /manifest /opt:REF /incremental:no user32.lib
CC_PARAMS	+= -nologo -Fd$(BUILD_DIR)/ -W3 -DUNICODE -D_UNICODE -DWIN32 -D_LITTLE_ENDIAN -D_STATIC_CPPLIB -I$(BUILD_DIR) -I$(SRC_DIR) -I$(JDK_HOME)/include -I$(JDK_HOME)/include/win32 -I$(DXSDK_PATH)/Include -c
ifeq ($(CONF), Release)
	CC_PARAMS += -Zi -O2 -Oi -MD
else
	LINK_PARAMS += /debug
	CC_PARAMS += -Zi -Od -DDEBUG -MDd
endif

ifeq ($(NO_PERF_CONT), true)
	CC_PARAMS += -DNO_PERF_COUNTERS
endif


# build
build: .build-pre $(PRISM_LIB) .build-post

$(PRISM_LIB) : $(OBJS)
	$(LINK) $(LINK_PARAMS) $^

$(BUILD_DIR)/%.obj : $(SRC_DIR)/%.cc $(JNI_INCLUDES) $(VS_INCLUDE) $(VS_3D_INCLUDE) $(PS_3D_INCLUDE) $(INCLUDES)
	$(CC) $(CC_PARAMS) -Fo$@ $<

$(VS_INCLUDE) : $(VS_SRC)
	$(FXC) /nologo /T vs_3_0 /Fh $(VS_INCLUDE) /E passThrough $(VS_SRC)

$(PS_3D_INCLUDE) : $(PS_3D_SRC) $(HLSL_3D_INCLUDES)
	$(FXC) /nologo /T ps_2_0 /Fh $(PS_3D_INCLUDE) /DSpec=0 /DSType=0 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_i.h /DSpec=0 /DSType=0 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s1n.h /DSpec=1 /DSType=0 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s2n.h /DSpec=2 /DSType=0 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s3n.h /DSpec=3 /DSType=0 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s1a.h /DSpec=1 /DSType=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s2a.h /DSpec=2 /DSType=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s3a.h /DSpec=3 /DSType=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s1s.h /DSpec=1 /DSType=2 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s2s.h /DSpec=2 /DSType=2 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s3s.h /DSpec=3 /DSType=2 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b1n.h /DSpec=1 /DSType=0 /DBump=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b2n.h /DSpec=2 /DSType=0 /DBump=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b3n.h /DSpec=3 /DSType=0 /DBump=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b1a.h /DSpec=1 /DSType=1 /DBump=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b2a.h /DSpec=2 /DSType=1 /DBump=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b3a.h /DSpec=3 /DSType=1 /DBump=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b1s.h /DSpec=1 /DSType=2 /DBump=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b2s.h /DSpec=2 /DSType=2 /DBump=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b3s.h /DSpec=3 /DSType=2 /DBump=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s1ni.h /DSpec=1 /DSType=0 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s2ni.h /DSpec=2 /DSType=0 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s3ni.h /DSpec=3 /DSType=0 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s1ai.h /DSpec=1 /DSType=1 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s2ai.h /DSpec=2 /DSType=1 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s3ai.h /DSpec=3 /DSType=1 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s1si.h /DSpec=1 /DSType=2 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s2si.h /DSpec=2 /DSType=2 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_s3si.h /DSpec=3 /DSType=2 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b1ni.h /DSpec=1 /DSType=0 /DBump=1 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b2ni.h /DSpec=2 /DSType=0 /DBump=1 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b3ni.h /DSpec=3 /DSType=0 /DBump=1 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b1ai.h /DSpec=1 /DSType=1 /DBump=1 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b2ai.h /DSpec=2 /DSType=1 /DBump=1 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b3ai.h /DSpec=3 /DSType=1 /DBump=1 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b1si.h /DSpec=1 /DSType=2 /DBump=1 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b2si.h /DSpec=2 /DSType=2 /DBump=1 /DIllumMap=1 $(PS_3D_SRC)
	$(FXC) /nologo /T ps_2_0 /Fh $(BUILD_HLSL_3D_DIR)/Mtl1PS_b3si.h /DSpec=3 /DSType=2 /DBump=1 /DIllumMap=1 $(PS_3D_SRC)

$(VS_3D_INCLUDE) : $(VS_3D_SRC) $(HLSL_3D_INCLUDES)
	$(FXC) /nologo /T vs_2_0 /Fh $(VS_3D_INCLUDE) /DVertexType=ObjVertex $(VS_3D_SRC)

$(BUILD_DIR)/$(PRISM_JNI_PKG)_%.h : $(PRISM_D3D_CLASS_DIR)/$(PRISM_PKG_PATH)/%.class
	 $(JAVAH) -force -o $@ -jni -classpath "$(PRISM_D3D_CLASS_DIR)\;$(PRISM_DEP_CP)" $(PRISM_PKG).$*

sanity:
	@if [ ! -d $(JDK_HOME) ]; then \
		echo "ERROR: path to Java 6 SDK: $(JDK_HOME) is not found."; \
		echo "Override the location on the command line"; \
		echo "using ALT_BOOTDIR variable."; \
		echo "Sanity check failed."; \
		exit 1; \
	fi;
	@if [ ! -d $(DXSDK_PATH) ]; then \
		echo "ERROR: path to DirectX 9 SDK: $(DXSDK_PATH) is not found."; \
		echo "Override the location on the command line"; \
		echo "using ALT_DXSDK_PATH variable.";  \
		echo "Sanity check failed."; \
		exit 1; \
	fi;

.build-pre: sanity
	@mkdir -p $(BUILD_DIR) $(BUILD_HLSL_3D_DIR) $(DIST_DIR)

.build-post:


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
	rm -rf $(BUILD_DIR) $(DIST_DIST)

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
