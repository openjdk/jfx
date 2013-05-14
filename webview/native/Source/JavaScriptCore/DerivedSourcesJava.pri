include($$PWD/../WebKitJava.pri)

GENERATED_SOURCES_DIR = generated
OBJECTS_DIR = obj
SOURCE_DIR = $$BASE_DIR
DESTDIR = ../lib
QMAKE_LIBDIR += $$DESTDIR

DEFINES += \
    BUILDING_JavaScriptCore \
    BUILDING_WTF \
    BUILD_WEBKIT \

LUT_FILES += \
    runtime/ArrayConstructor.cpp \
    runtime/ArrayPrototype.cpp \
    runtime/BooleanPrototype.cpp \
    runtime/DateConstructor.cpp \
    runtime/DatePrototype.cpp \
    runtime/ErrorPrototype.cpp \
    runtime/JSGlobalObject.cpp \
    runtime/JSONObject.cpp \
    runtime/MathObject.cpp \
    runtime/NamePrototype.cpp \
    runtime/NumberConstructor.cpp \
    runtime/NumberPrototype.cpp \
    runtime/ObjectConstructor.cpp \
    runtime/ObjectPrototype.cpp \
    runtime/RegExpConstructor.cpp \
    runtime/RegExpObject.cpp \
    runtime/RegExpPrototype.cpp \
    runtime/StringConstructor.cpp \
    runtime/StringPrototype.cpp \

KEYWORDLUT_FILES += \
    parser/Keywords.table

JIT_STUB_FILES += \
    jit/JITStubs.cpp

# GENERATOR 1-A: LUT creator
lut.output = ${QMAKE_FILE_BASE}.lut.h
lut.input = LUT_FILES
lut.script = $$PWD/create_hash_table
lut.commands = perl $$lut.script ${QMAKE_FILE_NAME} -i > ${QMAKE_FILE_OUT}
lut.depends = ${QMAKE_FILE_NAME}
GENERATORS += lut

# GENERATOR 1-B: particular LUT creator (for 1 file only)
keywordlut.output = Lexer.lut.h
keywordlut.input = KEYWORDLUT_FILES
keywordlut.script = $$PWD/create_hash_table
keywordlut.commands = perl $$keywordlut.script ${QMAKE_FILE_NAME} -i > ${QMAKE_FILE_OUT}
keywordlut.depends = ${QMAKE_FILE_NAME}
GENERATORS += keywordlut

# GENERATOR 2-A: JIT Stub functions for RVCT
rvctstubs.output = Generated${QMAKE_FILE_BASE}_RVCT.h
rvctstubs.script = $$PWD/create_jit_stubs
rvctstubs.commands = perl -i $$rvctstubs.script --prefix RVCT ${QMAKE_FILE_NAME} > ${QMAKE_FILE_OUT}
rvctstubs.depends = ${QMAKE_FILE_NAME}
rvctstubs.input = JIT_STUB_FILES
rvctstubs.CONFIG += no_link
GENERATORS += rvctstubs

# GENERATOR 2-B: JIT Stub functions for MSVC
msvcstubs.output = Generated${QMAKE_FILE_BASE}_MSVC.asm
msvcstubs.script = $$PWD/create_jit_stubs
msvcstubs.commands = perl -i $$msvcstubs.script --prefix MSVC ${QMAKE_FILE_NAME} > ${QMAKE_FILE_OUT}
msvcstubs.depends = ${QMAKE_FILE_NAME}
msvcstubs.input = JIT_STUB_FILES
msvcstubs.CONFIG += no_link
GENERATORS += msvcstubs

#GENERATOR: "RegExpJitTables.h": tables used by Yarr
retgen.output = RegExpJitTables.h
retgen.script = $$PWD/create_regex_tables
retgen.input = retgen.script
retgen.commands = python $$retgen.script > ${QMAKE_FILE_OUT}
GENERATORS += retgen

#GENERATOR: "KeywordLookup.h": decision tree used by the lexer
klgen.output = KeywordLookup.h
klgen.script = $$PWD/KeywordLookupGenerator.py
klgen.input = KEYWORDLUT_FILES
klgen.commands = python $$klgen.script ${QMAKE_FILE_NAME} > ${QMAKE_FILE_OUT}
GENERATORS += klgen

defineTest(prependEach) {
    unset(variable)
    unset(prefix)

    variable = $$1
    prefix = $$2

    original_values = $$unique($$variable)

    for(value, original_values) {
        values += $${prefix}$${value}
    }

    eval($$variable = $$values)
    export($$variable)

    return(true)
}


for(generator, GENERATORS) {
    eval($${generator}.CONFIG = target_predeps no_link)
    eval($${generator}.dependency_type = TYPE_C)

    isEmpty($${generator}.output_function) {
        prependEach($${generator}.output, $${GENERATED_SOURCES_DIR}/)
    }

    script = $$eval($${generator}.script)
    eval($${generator}.depends += $$script)

    !isEmpty($${generator}.input) {
        # Compiler-style generator
        QMAKE_EXTRA_COMPILERS += $${generator}
        DEFAULT_TARGETS += compiler_$${generator}_make_all
    } else {
        # Regular target generator
        QMAKE_EXTRA_TARGETS += $${generator}
        DEFAULT_TARGETS += $${generator}
    }

    generated_files.depends += compiler_$${generator}_make_all
}
