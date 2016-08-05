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
	${OBJECTDIR}/_ext/839896833/Exports.o \
	${OBJECTDIR}/_ext/839896833/FilePath.o \
	${OBJECTDIR}/_ext/839896833/GenericPlatform.o \
	${OBJECTDIR}/_ext/839896833/Helpers.o \
	${OBJECTDIR}/_ext/839896833/IniFile.o \
	${OBJECTDIR}/_ext/839896833/Java.o \
	${OBJECTDIR}/_ext/839896833/JavaUserPreferences.o \
	${OBJECTDIR}/_ext/839896833/JavaVirtualMachine.o \
	${OBJECTDIR}/_ext/839896833/LinuxPlatform.o \
	${OBJECTDIR}/_ext/839896833/Lock.o \
	${OBJECTDIR}/_ext/839896833/Macros.o \
	${OBJECTDIR}/_ext/839896833/Messages.o \
	${OBJECTDIR}/_ext/839896833/Package.o \
	${OBJECTDIR}/_ext/839896833/Platform.o \
	${OBJECTDIR}/_ext/839896833/PlatformString.o \
	${OBJECTDIR}/_ext/839896833/PlatformThread.o \
	${OBJECTDIR}/_ext/839896833/PosixPlatform.o \
	${OBJECTDIR}/_ext/839896833/PropertyFile.o \
	${OBJECTDIR}/_ext/839896833/WindowsPlatform.o \
	${OBJECTDIR}/_ext/839896833/main.o


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
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libpackager.${CND_DLIB_EXT}

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libpackager.${CND_DLIB_EXT}: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.cc} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libpackager.${CND_DLIB_EXT} ${OBJECTFILES} ${LDLIBSOPTIONS} -shared -fPIC

${OBJECTDIR}/_ext/839896833/Exports.o: ../../../../library/common/Exports.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/Exports.o ../../../../library/common/Exports.cpp

${OBJECTDIR}/_ext/839896833/FilePath.o: ../../../../library/common/FilePath.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/FilePath.o ../../../../library/common/FilePath.cpp

${OBJECTDIR}/_ext/839896833/GenericPlatform.o: ../../../../library/common/GenericPlatform.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/GenericPlatform.o ../../../../library/common/GenericPlatform.cpp

${OBJECTDIR}/_ext/839896833/Helpers.o: ../../../../library/common/Helpers.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/Helpers.o ../../../../library/common/Helpers.cpp

${OBJECTDIR}/_ext/839896833/IniFile.o: ../../../../library/common/IniFile.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/IniFile.o ../../../../library/common/IniFile.cpp

${OBJECTDIR}/_ext/839896833/Java.o: ../../../../library/common/Java.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/Java.o ../../../../library/common/Java.cpp

${OBJECTDIR}/_ext/839896833/JavaUserPreferences.o: ../../../../library/common/JavaUserPreferences.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/JavaUserPreferences.o ../../../../library/common/JavaUserPreferences.cpp

${OBJECTDIR}/_ext/839896833/JavaVirtualMachine.o: ../../../../library/common/JavaVirtualMachine.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/JavaVirtualMachine.o ../../../../library/common/JavaVirtualMachine.cpp

${OBJECTDIR}/_ext/839896833/LinuxPlatform.o: ../../../../library/common/LinuxPlatform.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/LinuxPlatform.o ../../../../library/common/LinuxPlatform.cpp

${OBJECTDIR}/_ext/839896833/Lock.o: ../../../../library/common/Lock.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/Lock.o ../../../../library/common/Lock.cpp

${OBJECTDIR}/_ext/839896833/Macros.o: ../../../../library/common/Macros.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/Macros.o ../../../../library/common/Macros.cpp

${OBJECTDIR}/_ext/839896833/Messages.o: ../../../../library/common/Messages.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/Messages.o ../../../../library/common/Messages.cpp

${OBJECTDIR}/_ext/839896833/Package.o: ../../../../library/common/Package.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/Package.o ../../../../library/common/Package.cpp

${OBJECTDIR}/_ext/839896833/Platform.o: ../../../../library/common/Platform.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/Platform.o ../../../../library/common/Platform.cpp

${OBJECTDIR}/_ext/839896833/PlatformString.o: ../../../../library/common/PlatformString.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/PlatformString.o ../../../../library/common/PlatformString.cpp

${OBJECTDIR}/_ext/839896833/PlatformThread.o: ../../../../library/common/PlatformThread.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/PlatformThread.o ../../../../library/common/PlatformThread.cpp

${OBJECTDIR}/_ext/839896833/PosixPlatform.o: ../../../../library/common/PosixPlatform.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/PosixPlatform.o ../../../../library/common/PosixPlatform.cpp

${OBJECTDIR}/_ext/839896833/PropertyFile.o: ../../../../library/common/PropertyFile.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/PropertyFile.o ../../../../library/common/PropertyFile.cpp

${OBJECTDIR}/_ext/839896833/WindowsPlatform.o: ../../../../library/common/WindowsPlatform.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/WindowsPlatform.o ../../../../library/common/WindowsPlatform.cpp

${OBJECTDIR}/_ext/839896833/main.o: ../../../../library/common/main.cpp 
	${MKDIR} -p ${OBJECTDIR}/_ext/839896833
	${RM} "$@.d"
	$(COMPILE.cc) -O2 -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/839896833/main.o ../../../../library/common/main.cpp

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libpackager.${CND_DLIB_EXT}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
