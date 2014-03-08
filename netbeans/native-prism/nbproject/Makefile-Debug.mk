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
CND_PLATFORM=GNU-MacOSX
CND_DLIB_EXT=dylib
CND_CONF=Debug
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/_ext/342980646/Curve.o \
	${OBJECTDIR}/_ext/342980646/Dasher.o \
	${OBJECTDIR}/_ext/342980646/Helpers.o \
	${OBJECTDIR}/_ext/342980646/NativePiscesRasterizer.o \
	${OBJECTDIR}/_ext/342980646/Renderer.o \
	${OBJECTDIR}/_ext/342980646/Stroker.o \
	${OBJECTDIR}/_ext/342980646/Transformer.o


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
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism.${CND_DLIB_EXT}

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism.${CND_DLIB_EXT}: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.c} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism.${CND_DLIB_EXT} ${OBJECTFILES} ${LDLIBSOPTIONS} -dynamiclib -install_name libnative-prism.${CND_DLIB_EXT} -fPIC

${OBJECTDIR}/_ext/342980646/Curve.o: ../../modules/graphics/src/main/native-prism/Curve.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/342980646
	${RM} $@.d
	$(COMPILE.c) -g -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/342980646/Curve.o ../../modules/graphics/src/main/native-prism/Curve.c

${OBJECTDIR}/_ext/342980646/Dasher.o: ../../modules/graphics/src/main/native-prism/Dasher.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/342980646
	${RM} $@.d
	$(COMPILE.c) -g -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/342980646/Dasher.o ../../modules/graphics/src/main/native-prism/Dasher.c

${OBJECTDIR}/_ext/342980646/Helpers.o: ../../modules/graphics/src/main/native-prism/Helpers.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/342980646
	${RM} $@.d
	$(COMPILE.c) -g -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/342980646/Helpers.o ../../modules/graphics/src/main/native-prism/Helpers.c

${OBJECTDIR}/_ext/342980646/NativePiscesRasterizer.o: ../../modules/graphics/src/main/native-prism/NativePiscesRasterizer.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/342980646
	${RM} $@.d
	$(COMPILE.c) -g -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/342980646/NativePiscesRasterizer.o ../../modules/graphics/src/main/native-prism/NativePiscesRasterizer.c

${OBJECTDIR}/_ext/342980646/Renderer.o: ../../modules/graphics/src/main/native-prism/Renderer.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/342980646
	${RM} $@.d
	$(COMPILE.c) -g -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/342980646/Renderer.o ../../modules/graphics/src/main/native-prism/Renderer.c

${OBJECTDIR}/_ext/342980646/Stroker.o: ../../modules/graphics/src/main/native-prism/Stroker.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/342980646
	${RM} $@.d
	$(COMPILE.c) -g -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/342980646/Stroker.o ../../modules/graphics/src/main/native-prism/Stroker.c

${OBJECTDIR}/_ext/342980646/Transformer.o: ../../modules/graphics/src/main/native-prism/Transformer.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/342980646
	${RM} $@.d
	$(COMPILE.c) -g -fPIC  -MMD -MP -MF $@.d -o ${OBJECTDIR}/_ext/342980646/Transformer.o ../../modules/graphics/src/main/native-prism/Transformer.c

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-prism.${CND_DLIB_EXT}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
