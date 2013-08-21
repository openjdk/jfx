CC      = gcc -O2 
JDK     = /opt/java

HDRS    = iolib.h map.h os.h
SRCS    = trace.c agent.c iolib.c os-linux.c
SRCS2   = retrace.c iolib.c enums.c map.c os-linux.c


PLATFORM := $(shell if [ -x /opt/vc ]; then echo RASPBERRY; else echo BEAGLE; fi)

ifeq ($(PLATFORM),RASPBERRY)
CFLAGS += -DRASPBERRYPI=1 -I/opt/vc/include -I/opt/vc/include/interface/vcos/pthreads -L/opt/vc/lib
DEST = armv6hf-vfp
endif

ifeq ($(PLATFORM),BEAGLE)
DEST = armv6-vfp
endif

TARGETS = $(DEST)/gltrace.so $(DEST)/gltrace

default: all

all: $(DEST) $(TARGETS)

$(DEST):
	if [ ! -x $(DEST) ]; then mkdir -m 755 -p $(DEST); fi

#
#    gltrace.so
#

HDRS    = iolib.h map.h os.h
DYNLIB_SRCS = trace.c agent.c iolib.c os-linux.c

$(DEST)/gltrace.so: $(HDRS) $(DYNLIB_SRCS)
	$(CC) $(CFLAGS) -shared -fPIC \
	-I$(JDK)/include -I$(JDK)/include/linux \
	-ldl -lEGL -lGLESv2 \
	-o $@ $(DYNLIB_SRCS) 

#
#    gltrace
#

GLTRACE_SRCS = retrace.c iolib.c enums.c map.c os-linux.c

$(DEST)/gltrace: $(HDRS) $(GLTRACE_SRCS)
	$(CC) $(CFLAGS) -o $@ $(GLTRACE_SRCS) -lEGL -lGLESv2 -lrt -lpng

#
#    examples
#

JAVA    = $(JDK)/bin/java
JVMOPTS =
JAVAFX  = /opt/javafx/lib/ext/jfxrt.jar
JFXOPTS =

RUNJFX  = $(JAVA) $(JVMOPTS) $(JFXOPTS)

TEST    =
ARGS    =

trace:
	export LD_PRELOAD=$(DEST)/gltrace.so; \
	$(RUNJFX) -cp "$(JAVAFX):$(JAR)" $(TEST) $(ARGS)

replay:
	$(DEST)/gltrace -replay

dump:
	$(DEST)/gltrace -print

interactive:
	$(DEST)/gltrace

clean:
	rm -rf $(TARGETS)
