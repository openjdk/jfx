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
CCADMIN=CCadmin
RANLIB=ranlib
CC=gcc
CCC=g++
CXX=g++
FC=

# Macros
PLATFORM=Cygwin-Windows

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=build/Release/${PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/src/PiscesRenderer.o \
	${OBJECTDIR}/src/PiscesStrokeParameters.o \
	${OBJECTDIR}/src/PiscesPath.o \
	${OBJECTDIR}/src/JNIUtil.o \
	${OBJECTDIR}/src/PiscesBlit.o \
	${OBJECTDIR}/src/JTransform.o \
	${OBJECTDIR}/src/PiscesTransform.o \
	${OBJECTDIR}/src/JPiscesRenderer.o \
	${OBJECTDIR}/src/PiscesMath.o \
	${OBJECTDIR}/src/JJavaSurface.o \
	${OBJECTDIR}/src/PiscesPipelines.o \
	${OBJECTDIR}/src/JAbstractSurface.o \
	${OBJECTDIR}/src/PiscesLibrary.o \
	${OBJECTDIR}/src/PiscesUtil.o \
	${OBJECTDIR}/src/PiscesSysutils.o \
	${OBJECTDIR}/src/JNativeSurface.o

# C Compiler Flags
CFLAGS=

# CC Compiler Flags
CCFLAGS=
CXXFLAGS=

# Fortran Compiler Flags
FFLAGS=

# Link Libraries and Options
LDLIBSOPTIONS=

# Build Targets
.build-conf: ${BUILD_SUBPROJECTS}
	${MAKE}  -f nbproject/Makefile-Release.mk dist/Release/${PLATFORM}/libpisces-native.dll

dist/Release/${PLATFORM}/libpisces-native.dll: ${OBJECTFILES}
	${MKDIR} -p dist/Release/${PLATFORM}
	${LINK.c} -mno-cygwin -shared -o dist/Release/${PLATFORM}/libpisces-native.dll -fPIC ${OBJECTFILES} ${LDLIBSOPTIONS} 

${OBJECTDIR}/src/PiscesRenderer.o: src/PiscesRenderer.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/PiscesRenderer.o src/PiscesRenderer.c

${OBJECTDIR}/src/PiscesStrokeParameters.o: src/PiscesStrokeParameters.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/PiscesStrokeParameters.o src/PiscesStrokeParameters.c

${OBJECTDIR}/src/PiscesPath.o: src/PiscesPath.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/PiscesPath.o src/PiscesPath.c

${OBJECTDIR}/src/JNIUtil.o: src/JNIUtil.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/JNIUtil.o src/JNIUtil.c

${OBJECTDIR}/src/PiscesBlit.o: src/PiscesBlit.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/PiscesBlit.o src/PiscesBlit.c

${OBJECTDIR}/src/JTransform.o: src/JTransform.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/JTransform.o src/JTransform.c

${OBJECTDIR}/src/PiscesTransform.o: src/PiscesTransform.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/PiscesTransform.o src/PiscesTransform.c

${OBJECTDIR}/src/JPiscesRenderer.o: src/JPiscesRenderer.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/JPiscesRenderer.o src/JPiscesRenderer.c

${OBJECTDIR}/src/PiscesMath.o: src/PiscesMath.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/PiscesMath.o src/PiscesMath.c

${OBJECTDIR}/src/JJavaSurface.o: src/JJavaSurface.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/JJavaSurface.o src/JJavaSurface.c

${OBJECTDIR}/src/PiscesPipelines.o: src/PiscesPipelines.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/PiscesPipelines.o src/PiscesPipelines.c

${OBJECTDIR}/src/JAbstractSurface.o: src/JAbstractSurface.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/JAbstractSurface.o src/JAbstractSurface.c

${OBJECTDIR}/src/PiscesLibrary.o: src/PiscesLibrary.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/PiscesLibrary.o src/PiscesLibrary.c

${OBJECTDIR}/src/PiscesUtil.o: src/PiscesUtil.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/PiscesUtil.o src/PiscesUtil.c

${OBJECTDIR}/src/PiscesSysutils.o: src/PiscesSysutils.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/PiscesSysutils.o src/PiscesSysutils.c

${OBJECTDIR}/src/JNativeSurface.o: src/JNativeSurface.c 
	${MKDIR} -p ${OBJECTDIR}/src
	${RM} $@.d
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/src/JNativeSurface.o src/JNativeSurface.c

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf:
	${RM} -r build/Release
	${RM} dist/Release/${PLATFORM}/libpisces-native.dll

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
