include($$PWD/../WebKitJava.pri)

GENERATED_SOURCES_DIR = generated
OBJECTS_DIR = obj
SOURCE_DIR = $$BASE_DIR
DESTDIR = ../lib
QMAKE_LIBDIR += $$DESTDIR

CHK_FILE_EXISTS = test -f
CHK_DIR_EXISTS = test -d

mac*|linux* {
    INC_OPT=-include
}
win* {
    INC_OPT=/FI
}
QMAKE_CXXFLAGS += $$INC_OPT $$PWD/runtime/JSExportMacros.h  # todo tav remove when building w/ pch

mac* {
    DEFINES += \
        WTF_USE_CF=1
}

mac* {
    # on macosx, we do not have icu headeres for system libraries,
    # so a snapshot of icu headers is used.
    INCLUDEPATH += \
        $$PWD/icu \
	$$PWD/../WebCore/icu
}

INCLUDEPATH += \
    $$GENERATED_SOURCES_DIR \
    $$PWD/../WTF \
    $$PWD/runtime \
    $$PWD/parser \
    $$PWD/builtins \
    $$PWD/heap \
    $$PWD/bytecode \
    $$PWD/dfg \
    $$PWD/jit \
    $$PWD/assembler \
    $$PWD/disassembler \
    $$PWD/llint \
    $$PWD/profiler \
    $$PWD/interpreter \
    $$PWD/API

FEATURE_DEFINES += \
    ENABLE_PROMISES=1 # tav todo revise

DEFINES += \
    BUILDING_JavaScriptCore \
    BUILDING_WTF \
    BUILD_WEBKIT \
    ICU_UNICODE=1 \
    $$FEATURE_DEFINES

mac*|linux* {
    DEFINES += \
        JS_EXPORT_PRIVATE # tav todo delete when building w/ pch
}

LUT_FILES += \
    runtime/ArrayConstructor.cpp \
    runtime/ArrayPrototype.cpp \
    runtime/BooleanPrototype.cpp \
    runtime/DateConstructor.cpp \
    runtime/DatePrototype.cpp \
    runtime/ErrorPrototype.cpp \
    runtime/JSDataViewPrototype.cpp \
    runtime/JSGlobalObject.cpp \
    runtime/JSONObject.cpp \
    runtime/JSPromiseConstructor.cpp \
    runtime/JSPromisePrototype.cpp \
    runtime/MathObject.cpp \
    runtime/NamePrototype.cpp \
    runtime/NumberConstructor.cpp \
    runtime/NumberPrototype.cpp \
    runtime/ObjectConstructor.cpp \
    runtime/RegExpConstructor.cpp \
    runtime/RegExpObject.cpp \
    runtime/RegExpPrototype.cpp \
    runtime/StringConstructor.cpp \

KEYWORDLUT_FILES += \
    parser/Keywords.table

JIT_STUB_FILES += \
    jit/JITStubs.cpp

BUILTINS_FILES += \
    builtins/Array.prototype.js

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
#GENERATORS += rvctstubs

# GENERATOR 2-B: JIT Stub functions for MSVC
msvcstubs.output = Generated${QMAKE_FILE_BASE}_MSVC.asm
msvcstubs.script = $$PWD/create_jit_stubs
msvcstubs.commands = perl -i $$msvcstubs.script --prefix MSVC ${QMAKE_FILE_NAME} > ${QMAKE_FILE_OUT}
msvcstubs.depends = ${QMAKE_FILE_NAME}
msvcstubs.input = JIT_STUB_FILES
msvcstubs.CONFIG += no_link
#GENERATORS += msvcstubs

builtins.output = JSCBuiltins.cpp
builtins.input = BUILTINS_FILES
builtins.script = $$PWD/generate-js-builtins
#builtins.commands = python $$builtins.script ${QMAKE_FILE_NAME} ${QMAKE_FILE_OUT}
builtins.commands = python $$builtins.script $$PWD/builtins/Array.prototype.js ${QMAKE_FILE_OUT}
builtins.extra_sources = JSCBuiltins.cpp
builtins.depends = $${INSPECTOR_JS_GEN_FILES}
GENERATORS += builtins

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

INSPECTOR_JSON_INPUT = $$PWD/inspector/protocol
inspectorJSON.output = InspectorJS.json
inspectorJSON.input = INSPECTOR_JSON_INPUT
inspectorJSON.script = $$PWD/inspector/scripts/generate-combined-inspector-json.py
inspectorJSON.commands = python $$inspectorJSON.script ${QMAKE_FILE_NAME} > ${QMAKE_FILE_OUT}
inspectorJSON.depends = ${QMAKE_FILE_NAME}
GENERATORS += inspectorJSON

INSPECTOR_JS_CPP_FILES = \
    $${GENERATED_SOURCES_DIR}/InspectorJSFrontendDispatchers.cpp \
    $${GENERATED_SOURCES_DIR}/InspectorJSBackendDispatchers.cpp \
    $${GENERATED_SOURCES_DIR}/InspectorJSTypeBuilders.cpp

INSPECTOR_JS_H_FILES = \
    $${GENERATED_SOURCES_DIR}/InspectorJSFrontendDispatchers.h \
    $${GENERATED_SOURCES_DIR}/InspectorJSBackendDispatchers.h \
    $${GENERATED_SOURCES_DIR}/InspectorJSTypeBuilders.h

INSPECTOR_JS_GEN_FILES = \
    $${INSPECTOR_JS_CPP_FILES} \
    $${INSPECTOR_JS_H_FILES}

inspectorJS.input = INSPECTOR_JS_GEN_FILES
inspectorJS.output = ${QMAKE_FILE_NAME}
inspectorJS.output_no_prepend = true
inspectorJS.script = $$PWD/inspector/scripts/CodeGeneratorInspector.py
inspectorJS.commands = $$CHK_FILE_EXISTS ${QMAKE_FILE_NAME} || python $$inspectorJS.script $${GENERATED_SOURCES_DIR}/InspectorJS.json --output_h_dir $${GENERATED_SOURCES_DIR} --output_cpp_dir $${GENERATED_SOURCES_DIR} --output_js_dir $${GENERATED_SOURCES_DIR} --output_type JavaScript && cd $${GENERATED_SOURCES_DIR} && $$CHK_DIR_EXISTS inspector || $(MKDIR) inspector && $(COPY_FILE) InspectorJS*.h inspector
inspectorJS.extra_sources = $${INSPECTOR_JS_CPP_FILES}
inspectorJS.extra_sources_no_prepend = true
inspectorJS.depends = $${GENERATED_SOURCES_DIR}/InspectorJS.json
GENERATORS += inspectorJS

LLINT_DEPENDENCY = \
    $$PWD/llint/LowLevelInterpreter.asm \
    $$PWD/llint/LowLevelInterpreter32_64.asm \
    $$PWD/llint/LowLevelInterpreter64.asm \
    $$PWD/offlineasm/arm.rb \
    $$PWD/offlineasm/ast.rb \
    $$PWD/offlineasm/backends.rb \
    $$PWD/offlineasm/generate_offset_extractor.rb \
    $$PWD/offlineasm/instructions.rb \
    $$PWD/offlineasm/offsets.rb \
    $$PWD/offlineasm/opt.rb \
    $$PWD/offlineasm/parser.rb \
    $$PWD/offlineasm/registers.rb \
    $$PWD/offlineasm/self_hash.rb \
    $$PWD/offlineasm/settings.rb \
    $$PWD/offlineasm/transform.rb \
    $$PWD/offlineasm/x86.rb

win* {
    BIN_EXTENSION = .exe
}

LLINT_ASSEMBLER = $$PWD/llint/LowLevelInterpreter.asm
LLINT_FILES = Programs/LLIntOffsetsExtractor$$BIN_EXTENSION
llint.output = LLIntAssembly.h
llint.script = $$PWD/offlineasm/asm.rb
llint.input = LLINT_FILES
llint.depends = $$LLINT_DEPENDENCY
llint.commands = ruby $$llint.script $$LLINT_ASSEMBLER ${QMAKE_FILE_IN} ${QMAKE_FILE_OUT}
GENERATORS += llint

INJECTED_SCRIPT_FILES = $$PWD/inspector/InjectedScriptSource.js
injscript.output = InjectedScriptSource.h
injscript.input = INJECTED_SCRIPT_FILES
injscript.script = $$PWD/inspector/scripts/jsmin.py
injscript.commands = python $$injscript.script < $$PWD/inspector/InjectedScriptSource.js > $$GENERATED_SOURCES_DIR/InjectedScriptSource.min.js && perl $$PWD/inspector/scripts/xxd.pl InjectedScriptSource_js $$GENERATED_SOURCES_DIR/InjectedScriptSource.min.js ${QMAKE_FILE_OUT} && rm -f $$GENERATED_SOURCES_DIR/InjectedScriptSource.min.js
GENERATORS += injscript

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

    isEmpty($${generator}.output_no_prepend) {
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

    !isEmpty($${generator}.extra_objects) {
        prependEach($${generator}.extra_objects, $${OBJECTS_DIR}/)

        OBJECTS += $$eval($${generator}.extra_objects)
    } 

    !isEmpty($${generator}.extra_sources) {
        isEmpty($${generator}.extra_sources_no_prepend) {
            prependEach($${generator}.extra_sources, $${GENERATED_SOURCES_DIR}/)
        }
	SOURCES += $$eval($${generator}.extra_sources)
    }
}
