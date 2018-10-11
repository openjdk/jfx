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
	${OBJECTDIR}/_ext/1063624794/ImageLoader.o \
	${OBJECTDIR}/_ext/1063624794/com_sun_javafx_iio_ios_IosImageLoader.o \
	${OBJECTDIR}/_ext/1063624794/jni_utils.o \
	${OBJECTDIR}/_ext/1742650920/jpegloader.o \
	${OBJECTDIR}/_ext/1433180815/jcapimin.o \
	${OBJECTDIR}/_ext/1433180815/jcapistd.o \
	${OBJECTDIR}/_ext/1433180815/jccoefct.o \
	${OBJECTDIR}/_ext/1433180815/jccolor.o \
	${OBJECTDIR}/_ext/1433180815/jcdctmgr.o \
	${OBJECTDIR}/_ext/1433180815/jchuff.o \
	${OBJECTDIR}/_ext/1433180815/jcinit.o \
	${OBJECTDIR}/_ext/1433180815/jcmainct.o \
	${OBJECTDIR}/_ext/1433180815/jcmarker.o \
	${OBJECTDIR}/_ext/1433180815/jcmaster.o \
	${OBJECTDIR}/_ext/1433180815/jcomapi.o \
	${OBJECTDIR}/_ext/1433180815/jcparam.o \
	${OBJECTDIR}/_ext/1433180815/jcprepct.o \
	${OBJECTDIR}/_ext/1433180815/jcsample.o \
	${OBJECTDIR}/_ext/1433180815/jctrans.o \
	${OBJECTDIR}/_ext/1433180815/jdapimin.o \
	${OBJECTDIR}/_ext/1433180815/jdapistd.o \
	${OBJECTDIR}/_ext/1433180815/jdcoefct.o \
	${OBJECTDIR}/_ext/1433180815/jdcolor.o \
	${OBJECTDIR}/_ext/1433180815/jddctmgr.o \
	${OBJECTDIR}/_ext/1433180815/jdhuff.o \
	${OBJECTDIR}/_ext/1433180815/jdinput.o \
	${OBJECTDIR}/_ext/1433180815/jdmainct.o \
	${OBJECTDIR}/_ext/1433180815/jdmarker.o \
	${OBJECTDIR}/_ext/1433180815/jdmaster.o \
	${OBJECTDIR}/_ext/1433180815/jdmerge.o \
	${OBJECTDIR}/_ext/1433180815/jdpostct.o \
	${OBJECTDIR}/_ext/1433180815/jdsample.o \
	${OBJECTDIR}/_ext/1433180815/jdtrans.o \
	${OBJECTDIR}/_ext/1433180815/jerror.o \
	${OBJECTDIR}/_ext/1433180815/jfdctflt.o \
	${OBJECTDIR}/_ext/1433180815/jfdctfst.o \
	${OBJECTDIR}/_ext/1433180815/jfdctint.o \
	${OBJECTDIR}/_ext/1433180815/jidctflt.o \
	${OBJECTDIR}/_ext/1433180815/jidctfst.o \
	${OBJECTDIR}/_ext/1433180815/jidctint.o \
	${OBJECTDIR}/_ext/1433180815/jmemmgr.o \
	${OBJECTDIR}/_ext/1433180815/jmemnobs.o \
	${OBJECTDIR}/_ext/1433180815/jquant1.o \
	${OBJECTDIR}/_ext/1433180815/jquant2.o \
	${OBJECTDIR}/_ext/1433180815/jutils.o


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
	"${MAKE}"  -f nbproject/Makefile-${CND_CONF}.mk ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-iio.${CND_DLIB_EXT}

${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-iio.${CND_DLIB_EXT}: ${OBJECTFILES}
	${MKDIR} -p ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}
	${LINK.c} -o ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-iio.${CND_DLIB_EXT} ${OBJECTFILES} ${LDLIBSOPTIONS} -shared -fPIC

${OBJECTDIR}/_ext/1063624794/ImageLoader.o: ../../modules/javafx.graphics/src/main/native-iio/ios/ImageLoader.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1063624794
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1063624794/ImageLoader.o ../../modules/javafx.graphics/src/main/native-iio/ios/ImageLoader.m

${OBJECTDIR}/_ext/1063624794/com_sun_javafx_iio_ios_IosImageLoader.o: ../../modules/javafx.graphics/src/main/native-iio/ios/com_sun_javafx_iio_ios_IosImageLoader.m 
	${MKDIR} -p ${OBJECTDIR}/_ext/1063624794
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1063624794/com_sun_javafx_iio_ios_IosImageLoader.o ../../modules/javafx.graphics/src/main/native-iio/ios/com_sun_javafx_iio_ios_IosImageLoader.m

${OBJECTDIR}/_ext/1063624794/jni_utils.o: ../../modules/javafx.graphics/src/main/native-iio/ios/jni_utils.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1063624794
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1063624794/jni_utils.o ../../modules/javafx.graphics/src/main/native-iio/ios/jni_utils.c

${OBJECTDIR}/_ext/1742650920/jpegloader.o: ../../modules/javafx.graphics/src/main/native-iio/jpegloader.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1742650920
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1742650920/jpegloader.o ../../modules/javafx.graphics/src/main/native-iio/jpegloader.c

${OBJECTDIR}/_ext/1433180815/jcapimin.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcapimin.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jcapimin.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcapimin.c

${OBJECTDIR}/_ext/1433180815/jcapistd.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcapistd.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jcapistd.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcapistd.c

${OBJECTDIR}/_ext/1433180815/jccoefct.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jccoefct.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jccoefct.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jccoefct.c

${OBJECTDIR}/_ext/1433180815/jccolor.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jccolor.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jccolor.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jccolor.c

${OBJECTDIR}/_ext/1433180815/jcdctmgr.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcdctmgr.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jcdctmgr.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcdctmgr.c

${OBJECTDIR}/_ext/1433180815/jchuff.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jchuff.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jchuff.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jchuff.c

${OBJECTDIR}/_ext/1433180815/jcinit.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcinit.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jcinit.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcinit.c

${OBJECTDIR}/_ext/1433180815/jcmainct.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcmainct.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jcmainct.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcmainct.c

${OBJECTDIR}/_ext/1433180815/jcmarker.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcmarker.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jcmarker.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcmarker.c

${OBJECTDIR}/_ext/1433180815/jcmaster.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcmaster.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jcmaster.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcmaster.c

${OBJECTDIR}/_ext/1433180815/jcomapi.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcomapi.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jcomapi.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcomapi.c

${OBJECTDIR}/_ext/1433180815/jcparam.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcparam.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jcparam.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcparam.c

${OBJECTDIR}/_ext/1433180815/jcprepct.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcprepct.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jcprepct.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcprepct.c

${OBJECTDIR}/_ext/1433180815/jcsample.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcsample.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jcsample.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jcsample.c

${OBJECTDIR}/_ext/1433180815/jctrans.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jctrans.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jctrans.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jctrans.c

${OBJECTDIR}/_ext/1433180815/jdapimin.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdapimin.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jdapimin.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdapimin.c

${OBJECTDIR}/_ext/1433180815/jdapistd.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdapistd.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jdapistd.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdapistd.c

${OBJECTDIR}/_ext/1433180815/jdcoefct.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdcoefct.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jdcoefct.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdcoefct.c

${OBJECTDIR}/_ext/1433180815/jdcolor.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdcolor.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jdcolor.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdcolor.c

${OBJECTDIR}/_ext/1433180815/jddctmgr.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jddctmgr.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jddctmgr.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jddctmgr.c

${OBJECTDIR}/_ext/1433180815/jdhuff.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdhuff.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jdhuff.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdhuff.c

${OBJECTDIR}/_ext/1433180815/jdinput.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdinput.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jdinput.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdinput.c

${OBJECTDIR}/_ext/1433180815/jdmainct.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdmainct.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jdmainct.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdmainct.c

${OBJECTDIR}/_ext/1433180815/jdmarker.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdmarker.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jdmarker.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdmarker.c

${OBJECTDIR}/_ext/1433180815/jdmaster.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdmaster.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jdmaster.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdmaster.c

${OBJECTDIR}/_ext/1433180815/jdmerge.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdmerge.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jdmerge.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdmerge.c

${OBJECTDIR}/_ext/1433180815/jdpostct.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdpostct.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jdpostct.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdpostct.c

${OBJECTDIR}/_ext/1433180815/jdsample.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdsample.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jdsample.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdsample.c

${OBJECTDIR}/_ext/1433180815/jdtrans.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdtrans.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jdtrans.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jdtrans.c

${OBJECTDIR}/_ext/1433180815/jerror.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jerror.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jerror.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jerror.c

${OBJECTDIR}/_ext/1433180815/jfdctflt.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jfdctflt.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jfdctflt.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jfdctflt.c

${OBJECTDIR}/_ext/1433180815/jfdctfst.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jfdctfst.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jfdctfst.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jfdctfst.c

${OBJECTDIR}/_ext/1433180815/jfdctint.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jfdctint.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jfdctint.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jfdctint.c

${OBJECTDIR}/_ext/1433180815/jidctflt.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jidctflt.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jidctflt.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jidctflt.c

${OBJECTDIR}/_ext/1433180815/jidctfst.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jidctfst.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jidctfst.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jidctfst.c

${OBJECTDIR}/_ext/1433180815/jidctint.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jidctint.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jidctint.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jidctint.c

${OBJECTDIR}/_ext/1433180815/jmemmgr.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jmemmgr.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jmemmgr.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jmemmgr.c

${OBJECTDIR}/_ext/1433180815/jmemnobs.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jmemnobs.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jmemnobs.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jmemnobs.c

${OBJECTDIR}/_ext/1433180815/jquant1.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jquant1.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jquant1.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jquant1.c

${OBJECTDIR}/_ext/1433180815/jquant2.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jquant2.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jquant2.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jquant2.c

${OBJECTDIR}/_ext/1433180815/jutils.o: ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jutils.c 
	${MKDIR} -p ${OBJECTDIR}/_ext/1433180815
	${RM} "$@.d"
	$(COMPILE.c) -g -I../../modules/javafx.graphics/build/gensrc/headers/iio/win -fPIC  -MMD -MP -MF "$@.d" -o ${OBJECTDIR}/_ext/1433180815/jutils.o ../../modules/javafx.graphics/src/main/native-iio/libjpeg/jutils.c

# Subprojects
.build-subprojects:

# Clean Targets
.clean-conf: ${CLEAN_SUBPROJECTS}
	${RM} -r ${CND_BUILDDIR}/${CND_CONF}
	${RM} ${CND_DISTDIR}/${CND_CONF}/${CND_PLATFORM}/libnative-iio.${CND_DLIB_EXT}

# Subprojects
.clean-subprojects:

# Enable dependency checking
.dep.inc: .depcheck-impl

include .dep.inc
