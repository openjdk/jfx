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
CND_PLATFORM=GNU-Linux-x86
CND_DLIB_EXT=so
CND_CONF=Release
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/_ext/9180873/JAbstractSurface.o \
	${OBJECTDIR}/_ext/9180873/JJavaSurface.o \
	${OBJECTDIR}/_ext/9180873/JNIUtil.o \
	${OBJECTDIR}/_ext/9180873/JNativeSurface.o \
	${OBJECTDIR}/_ext/9180873/JPiscesRenderer.o \
	${OBJECTDIR}/_ext/9180873/JTransform.o \
	${OBJECTDIR}/_ext/9180873/PiscesBlit.o \
	${OBJECTDIR}/_ext/9180873/PiscesMath.o \
	${OBJECTDIR}/_ext/9180873/PiscesPaint.o \
	${OBJECTDIR}/_ext/9180873/PiscesSysutils.o \
	${OBJECTDIR}/_ext/9180873/PiscesTransform.o \
	${OBJECTDIR}/_ext/9180873/PiscesUtil.o


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
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism-sw.${CND_DLIB_EXT}

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism-sw.${CND_DLIB_EXT}: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.c} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism-sw.${CND_DLIB_EXT} ${OBJECTFILES} ${LDLIBSOPTIONS} -shared -fPIC

${OBJECTDIR}/_ext/9180873/JAbstractSurface.o: ../../modules/graphics/src/main/native-prism-sw/JAbstractSurface.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/9180873
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/9180873/JAbstractSurface.o ../../modules/graphics/src/main/native-prism-sw/JAbstractSurface.c

${OBJECTDIR}/_ext/9180873/JJavaSurface.o: ../../modules/graphics/src/main/native-prism-sw/JJavaSurface.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/9180873
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/9180873/JJavaSurface.o ../../modules/graphics/src/main/native-prism-sw/JJavaSurface.c

${OBJECTDIR}/_ext/9180873/JNIUtil.o: ../../modules/graphics/src/main/native-prism-sw/JNIUtil.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/9180873
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/9180873/JNIUtil.o ../../modules/graphics/src/main/native-prism-sw/JNIUtil.c

${OBJECTDIR}/_ext/9180873/JNativeSurface.o: ../../modules/graphics/src/main/native-prism-sw/JNativeSurface.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/9180873
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/9180873/JNativeSurface.o ../../modules/graphics/src/main/native-prism-sw/JNativeSurface.c

${OBJECTDIR}/_ext/9180873/JPiscesRenderer.o: ../../modules/graphics/src/main/native-prism-sw/JPiscesRenderer.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/9180873
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/9180873/JPiscesRenderer.o ../../modules/graphics/src/main/native-prism-sw/JPiscesRenderer.c

${OBJECTDIR}/_ext/9180873/JTransform.o: ../../modules/graphics/src/main/native-prism-sw/JTransform.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/9180873
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/9180873/JTransform.o ../../modules/graphics/src/main/native-prism-sw/JTransform.c

${OBJECTDIR}/_ext/9180873/PiscesBlit.o: ../../modules/graphics/src/main/native-prism-sw/PiscesBlit.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/9180873
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/9180873/PiscesBlit.o ../../modules/graphics/src/main/native-prism-sw/PiscesBlit.c

${OBJECTDIR}/_ext/9180873/PiscesMath.o: ../../modules/graphics/src/main/native-prism-sw/PiscesMath.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/9180873
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/9180873/PiscesMath.o ../../modules/graphics/src/main/native-prism-sw/PiscesMath.c

${OBJECTDIR}/_ext/9180873/PiscesPaint.o: ../../modules/graphics/src/main/native-prism-sw/PiscesPaint.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/9180873
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/9180873/PiscesPaint.o ../../modules/graphics/src/main/native-prism-sw/PiscesPaint.c

${OBJECTDIR}/_ext/9180873/PiscesSysutils.o: ../../modules/graphics/src/main/native-prism-sw/PiscesSysutils.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/9180873
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/9180873/PiscesSysutils.o ../../modules/graphics/src/main/native-prism-sw/PiscesSysutils.c

${OBJECTDIR}/_ext/9180873/PiscesTransform.o: ../../modules/graphics/src/main/native-prism-sw/PiscesTransform.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/9180873
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/9180873/PiscesTransform.o ../../modules/graphics/src/main/native-prism-sw/PiscesTransform.c

${OBJECTDIR}/_ext/9180873/PiscesUtil.o: ../../modules/graphics/src/main/native-prism-sw/PiscesUtil.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/9180873
	${RM} "$@.d"
	$(COMPILE.c) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/9180873/PiscesUtil.o ../../modules/graphics/src/main/native-prism-sw/PiscesUtil.c

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism-sw.${CND_DLIB_EXT}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
