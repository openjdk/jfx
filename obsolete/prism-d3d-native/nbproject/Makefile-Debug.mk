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
CC=cc.exe
CCC=g++
CXX=g++
FC=gfortran
AS=as

# Macros
CND_PLATFORM=Cygwin-Windows
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
	${OBJECTDIR}/src/D3DPhongShaderGen.o \
	${OBJECTDIR}/src/D3DPhongShader.o \
	${OBJECTDIR}/src/D3DShader.o \
	${OBJECTDIR}/src/D3DMeshView.o \
	${OBJECTDIR}/src/D3DPipeline.o \
	${OBJECTDIR}/src/Trace.o \
	${OBJECTDIR}/src/D3DResourceManager.o \
	${OBJECTDIR}/src/TextureUploader.o \
	${OBJECTDIR}/src/D3DLight.o \
	${OBJECTDIR}/src/D3DPhongMaterial.o \
	${OBJECTDIR}/src/D3DContext.o \
	${OBJECTDIR}/src/D3DMesh.o \
	${OBJECTDIR}/src/D3DResourceFactory.o \
	${OBJECTDIR}/src/D3DPipelineManager.o \
	${OBJECTDIR}/src/D3DGraphics.o


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
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk dist/newt-d3d.dll

dist/newt-d3d.dll: ${OBJECTFILES}
	${MKDIR} -p dist
	g++.exe -mno-cygwin -shared -o dist/newt-d3d.dll ${OBJECTFILES} ${LDLIBSOPTIONS} 

${OBJECTDIR}/src/D3DPhongShaderGen.o: src/D3DPhongShaderGen.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/D3DPhongShaderGen.o src/D3DPhongShaderGen.cc

${OBJECTDIR}/src/D3DPhongShader.o: src/D3DPhongShader.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/D3DPhongShader.o src/D3DPhongShader.cc

${OBJECTDIR}/src/D3DShader.o: src/D3DShader.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/D3DShader.o src/D3DShader.cc

${OBJECTDIR}/src/D3DMeshView.o: src/D3DMeshView.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/D3DMeshView.o src/D3DMeshView.cc

${OBJECTDIR}/src/D3DPipeline.o: src/D3DPipeline.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/D3DPipeline.o src/D3DPipeline.cc

${OBJECTDIR}/src/Trace.o: src/Trace.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/Trace.o src/Trace.cc

${OBJECTDIR}/src/D3DResourceManager.o: src/D3DResourceManager.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/D3DResourceManager.o src/D3DResourceManager.cc

${OBJECTDIR}/src/TextureUploader.o: src/TextureUploader.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/TextureUploader.o src/TextureUploader.cc

${OBJECTDIR}/src/D3DLight.o: src/D3DLight.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/D3DLight.o src/D3DLight.cc

${OBJECTDIR}/src/D3DPhongMaterial.o: src/D3DPhongMaterial.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/D3DPhongMaterial.o src/D3DPhongMaterial.cc

${OBJECTDIR}/src/D3DContext.o: src/D3DContext.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/D3DContext.o src/D3DContext.cc

${OBJECTDIR}/src/D3DMesh.o: src/D3DMesh.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/D3DMesh.o src/D3DMesh.cc

${OBJECTDIR}/src/D3DResourceFactory.o: src/D3DResourceFactory.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/D3DResourceFactory.o src/D3DResourceFactory.cc

${OBJECTDIR}/src/D3DPipelineManager.o: src/D3DPipelineManager.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/D3DPipelineManager.o src/D3DPipelineManager.cc

${OBJECTDIR}/src/D3DGraphics.o: src/D3DGraphics.cc 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.cc) -g -Ibuild -Ibuild/3d -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include -I../../../../../../Program\ Files\ \(x86\)/Java/jdk1.7.0_11/include/win32 -I../../../../../../Program\ Files\ \(x86\)/Microsoft\ DirectX\ SDK\ \(June\ 2010\)/Include  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/D3DGraphics.o src/D3DGraphics.cc

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} dist/newt-d3d.dll

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
