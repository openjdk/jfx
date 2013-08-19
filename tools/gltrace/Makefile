CC      = gcc -O2 
JDK     = /opt/java

HDRS    = iolib.h map.h
SRCS    = trace.c agent.c iolib.c
SRCS2   = retrace.c iolib.c enums.c map.c

default: all

PLATFORM := $(shell if [ -x /opt/vc ]; then echo RASPBERRY; else echo BEAGLE; fi)

ifeq ($(PLATFORM),RASPBERRY)
CFLAGS += -DRASPBERRYPI=1 -I/opt/vc/include -I/opt/vc/include/interface/vcos/pthreads -L/opt/vc/lib
DEST = armv6hf-vfp
endif

ifeq ($(PLATFORM),BEAGLE)
DEST = armv6-vfp
endif

TARGETS = $(DEST)/ogltrace.so $(DEST)/ogltrace

all: $(DEST) $(TARGETS)

$(DEST):
        if [ ! -x $(DEST) ]; then mkdir -m 755 -p $(DEST); fi

$(DEST)/ogltrace.so: $(HDRS) $(SRCS)
        $(CC) $(CFLAGS) -shared -fPIC \
        -I$(JDK)/include -I$(JDK)/include/linux \
        -ldl -lEGL -lGLESv2 \
        -o $@ $(SRCS) 

$(DEST)/ogltrace: $(HDRS) $(SRCS2)
        $(CC) $(CFLAGS) -o $@ $(SRCS2) -lEGL -lGLESv2 -lrt -lpng

#
#    Examples
#

JAVA    = $(JDK)/bin/java
JVMOPTS =
JAVAFX  = /opt/javafx/lib/ext/jfxrt.jar
JFXOPTS =

RUNJFX  = $(JAVA) $(JVMOPTS) $(JFXOPTS)

TEST    =
ARGS    =

trace:
        export LD_PRELOAD=$(DEST)/ogltrace.so; \
        $(RUNJFX) -cp "$(JAVAFX):$(JAR)" $(TEST) $(ARGS)

replay:
        $(DEST)/ogltrace -replay

dump:
        $(DEST)/ogltrace -print

interactive:
        $(DEST)/ogltrace

clean:
        rm -rf $(TARGETS)
