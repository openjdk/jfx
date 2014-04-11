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
CND_CONF=Debug
CND_DISTDIR=dist
CND_BUILDDIR=build

# Include project Makefile
include Makefile

# Object Directory
OBJECTDIR=${CND_BUILDDIR}/${CND_CONF}/${CND_PLATFORM}

# Object Files
OBJECTFILES= \
	${OBJECTDIR}/_ext/1812479850/MacFontFinder.o \
	${OBJECTDIR}/_ext/1812479850/coretext.o \
	${OBJECTDIR}/_ext/1812479850/dfontdecoder.o \
	${OBJECTDIR}/_ext/1812479850/directwrite.o \
	${OBJECTDIR}/_ext/1812479850/fontpath.o \
	${OBJECTDIR}/_ext/1812479850/fontpath_linux.o


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
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-font.${CND_DLIB_EXT}

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-font.${CND_DLIB_EXT}: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.cc} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-font.${CND_DLIB_EXT} ${OBJECTFILES} ${LDLIBSOPTIONS} -shared -fPIC

${OBJECTDIR}/_ext/1812479850/MacFontFinder.o: ../../modules/graphics/src/main/native-font/MacFontFinder.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1812479850
	${RM} "$@.d"
	$(COMPILE.c) -g -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1812479850/MacFontFinder.o ../../modules/graphics/src/main/native-font/MacFontFinder.c

${OBJECTDIR}/_ext/1812479850/coretext.o: ../../modules/graphics/src/main/native-font/coretext.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1812479850
	${RM} "$@.d"
	$(COMPILE.c) -g -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1812479850/coretext.o ../../modules/graphics/src/main/native-font/coretext.c

${OBJECTDIR}/_ext/1812479850/dfontdecoder.o: ../../modules/graphics/src/main/native-font/dfontdecoder.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1812479850
	${RM} "$@.d"
	$(COMPILE.c) -g -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1812479850/dfontdecoder.o ../../modules/graphics/src/main/native-font/dfontdecoder.c

${OBJECTDIR}/_ext/1812479850/directwrite.o: ../../modules/graphics/src/main/native-font/directwrite.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/1812479850
	${RM} "$@.d"
	$(COMPILE.cc) -g -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1812479850/directwrite.o ../../modules/graphics/src/main/native-font/directwrite.cpp

${OBJECTDIR}/_ext/1812479850/fontpath.o: ../../modules/graphics/src/main/native-font/fontpath.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1812479850
	${RM} "$@.d"
	$(COMPILE.c) -g -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1812479850/fontpath.o ../../modules/graphics/src/main/native-font/fontpath.c

${OBJECTDIR}/_ext/1812479850/fontpath_linux.o: ../../modules/graphics/src/main/native-font/fontpath_linux.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1812479850
	${RM} "$@.d"
	$(COMPILE.c) -g -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1812479850/fontpath_linux.o ../../modules/graphics/src/main/native-font/fontpath_linux.c

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-font.${CND_DLIB_EXT}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
