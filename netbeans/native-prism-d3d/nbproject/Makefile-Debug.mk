#
# Generated Makefile - do not edit!
#
# Edit the Makefile in the project folder instead (../Makefile). Each target
# has a -pre and a -post target defined where you can add customized code.
#
# This makefile implements configuration specific macros and targets.


# Environment
MKDIR=mkdir
CP=cp
GREP=grep
NM=nm
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc
CCC=g++
CXX=g++
FC=gfortran
AS=as

# Macros
CND_PLATFORM=Cygwin_4.x-Windows
CND_DLIB_EXT=dll
CND_CONF=Debug
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/_ext/284623486/D3DContext.o \
	${OBJECTDIR}/_ext/284623486/D3DContextInit.o \
	${OBJECTDIR}/_ext/284623486/D3DGraphics.o \
	${OBJECTDIR}/_ext/284623486/D3DLight.o \
	${OBJECTDIR}/_ext/284623486/D3DMesh.o \
	${OBJECTDIR}/_ext/284623486/D3DMeshView.o \
	${OBJECTDIR}/_ext/284623486/D3DPhongMaterial.o \
	${OBJECTDIR}/_ext/284623486/D3DPhongShader.o \
	${OBJECTDIR}/_ext/284623486/D3DPhongShaderGen.o \
	${OBJECTDIR}/_ext/284623486/D3DPipeline.o \
	${OBJECTDIR}/_ext/284623486/D3DPipelineManager.o \
	${OBJECTDIR}/_ext/284623486/D3DResourceFactory.o \
	${OBJECTDIR}/_ext/284623486/D3DResourceManager.o \
	${OBJECTDIR}/_ext/284623486/D3DShader.o \
	${OBJECTDIR}/_ext/284623486/D3DWindow.o \
	${OBJECTDIR}/_ext/284623486/TextureUploader.o \
	${OBJECTDIR}/_ext/284623486/Trace.o


# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=
CXXFLAGS=

# Fortran Compiler Flags
FFLAGS=

# Assembler Flags
ASFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism-d3d.${CND_DLIB_EXT}

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism-d3d.${CND_DLIB_EXT}: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.cc} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism-d3d.${CND_DLIB_EXT} ${OBJECTFILES} ${LDLIBSOPTIONS} -shared

${OBJECTDIR}/_ext/284623486/D3DContext.o: ../../modules/graphics/src/main/native-prism-d3d/D3DContext.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DContext.o ../../modules/graphics/src/main/native-prism-d3d/D3DContext.cc

${OBJECTDIR}/_ext/284623486/D3DContextInit.o: ../../modules/graphics/src/main/native-prism-d3d/D3DContextInit.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DContextInit.o ../../modules/graphics/src/main/native-prism-d3d/D3DContextInit.cc

${OBJECTDIR}/_ext/284623486/D3DGraphics.o: ../../modules/graphics/src/main/native-prism-d3d/D3DGraphics.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DGraphics.o ../../modules/graphics/src/main/native-prism-d3d/D3DGraphics.cc

${OBJECTDIR}/_ext/284623486/D3DLight.o: ../../modules/graphics/src/main/native-prism-d3d/D3DLight.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DLight.o ../../modules/graphics/src/main/native-prism-d3d/D3DLight.cc

${OBJECTDIR}/_ext/284623486/D3DMesh.o: ../../modules/graphics/src/main/native-prism-d3d/D3DMesh.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DMesh.o ../../modules/graphics/src/main/native-prism-d3d/D3DMesh.cc

${OBJECTDIR}/_ext/284623486/D3DMeshView.o: ../../modules/graphics/src/main/native-prism-d3d/D3DMeshView.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DMeshView.o ../../modules/graphics/src/main/native-prism-d3d/D3DMeshView.cc

${OBJECTDIR}/_ext/284623486/D3DPhongMaterial.o: ../../modules/graphics/src/main/native-prism-d3d/D3DPhongMaterial.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DPhongMaterial.o ../../modules/graphics/src/main/native-prism-d3d/D3DPhongMaterial.cc

${OBJECTDIR}/_ext/284623486/D3DPhongShader.o: ../../modules/graphics/src/main/native-prism-d3d/D3DPhongShader.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DPhongShader.o ../../modules/graphics/src/main/native-prism-d3d/D3DPhongShader.cc

${OBJECTDIR}/_ext/284623486/D3DPhongShaderGen.o: ../../modules/graphics/src/main/native-prism-d3d/D3DPhongShaderGen.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DPhongShaderGen.o ../../modules/graphics/src/main/native-prism-d3d/D3DPhongShaderGen.cc

${OBJECTDIR}/_ext/284623486/D3DPipeline.o: ../../modules/graphics/src/main/native-prism-d3d/D3DPipeline.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DPipeline.o ../../modules/graphics/src/main/native-prism-d3d/D3DPipeline.cc

${OBJECTDIR}/_ext/284623486/D3DPipelineManager.o: ../../modules/graphics/src/main/native-prism-d3d/D3DPipelineManager.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DPipelineManager.o ../../modules/graphics/src/main/native-prism-d3d/D3DPipelineManager.cc

${OBJECTDIR}/_ext/284623486/D3DResourceFactory.o: ../../modules/graphics/src/main/native-prism-d3d/D3DResourceFactory.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DResourceFactory.o ../../modules/graphics/src/main/native-prism-d3d/D3DResourceFactory.cc

${OBJECTDIR}/_ext/284623486/D3DResourceManager.o: ../../modules/graphics/src/main/native-prism-d3d/D3DResourceManager.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DResourceManager.o ../../modules/graphics/src/main/native-prism-d3d/D3DResourceManager.cc

${OBJECTDIR}/_ext/284623486/D3DShader.o: ../../modules/graphics/src/main/native-prism-d3d/D3DShader.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DShader.o ../../modules/graphics/src/main/native-prism-d3d/D3DShader.cc

${OBJECTDIR}/_ext/284623486/D3DWindow.o: ../../modules/graphics/src/main/native-prism-d3d/D3DWindow.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/D3DWindow.o ../../modules/graphics/src/main/native-prism-d3d/D3DWindow.cc

${OBJECTDIR}/_ext/284623486/TextureUploader.o: ../../modules/graphics/src/main/native-prism-d3d/TextureUploader.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/TextureUploader.o ../../modules/graphics/src/main/native-prism-d3d/TextureUploader.cc

${OBJECTDIR}/_ext/284623486/Trace.o: ../../modules/graphics/src/main/native-prism-d3d/Trace.cc 
	${MKDIR} -p ${OBJECTDIR}/_ext/284623486
	${RM} "$@.d"
	$(COMPILE.cc) -g -I../../modules/graphics/build/generated-src/headers/prismD3D/win -I../../modules/graphics/build/headers/PrismD3D  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/284623486/Trace.o ../../modules/graphics/src/main/native-prism-d3d/Trace.cc

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism-d3d.${CND_DLIB_EXT}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
