#!/bin/bash

glib-genmarshal --prefix=source_marshal --header marshal.in > marshal.h
glib-genmarshal --prefix=source_marshal --body marshal.in > marshal.c
