
UNAME_S         := $(shell uname -s)

ifeq ($(UNAME_S),Darwin)
include Makefile.macosx
endif

ifeq ($(UNAME_S),Linux)
include Makefile.linux
endif
