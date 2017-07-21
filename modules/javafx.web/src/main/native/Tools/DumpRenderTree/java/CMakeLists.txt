set(WEBKIT_TESTRUNNER_SHARED_DIR "${TOOLS_DIR}/TestRunnerShared/")
set(WEBKIT_TESTRUNNER_UISCRIPTCONTEXT_DIR "${TOOLS_DIR}/TestRunnerShared/UIScriptContext")
set(DUMP_RENDER_TREE_BINDINGS_DIR "${TOOLS_DIR}/DumpRenderTree/Bindings")

file(MAKE_DIRECTORY ${DERIVED_SOURCES_DIR}/DumpRenderTree)

set(DumpRenderTree_SOURCES
    ../CyclicRedundancyCheck.cpp
    ../DumpRenderTreeCommon.cpp
    ../GCController.cpp
    ../TestRunner.cpp
    ../WorkQueue.cpp
    DumpRenderTree.cpp
    EventSender.cpp
    TestRunnerJava.cpp
    GCControllerJava.cpp
    JavaEnv.cpp
    WorkQueueItemJava.cpp
    ${WEBKIT_TESTRUNNER_UISCRIPTCONTEXT_DIR}/UIScriptContext.cpp
    ${WEBKIT_TESTRUNNER_UISCRIPTCONTEXT_DIR}/UIScriptController.cpp
    ${WEBKIT_TESTRUNNER_SHARED_DIR}/Bindings/JSWrapper.cpp
)

set(DumpRenderTree_LIBRARIES
    WebCore
    ${JAVA_JVM_LIBRARY}
    ${ICU_I18N_LIBRARIES}
    ${ICU_LIBRARIES}
)

set(DumpRenderTree_INCLUDE_DIRECTORIES
    ${DERIVED_SOURCES_DIR}/DumpRenderTree
    ${WEBKIT_TESTRUNNER_UISCRIPTCONTEXT_DIR}
    ${WEBKIT_TESTRUNNER_SHARED_DIR}/Bindings
    ${WEBCORE_DIR}
    ${WEBCORE_DIR}/testing/js
    ${WEBCORE_DIR}/platform
    ${JAVASCRIPTCORE_DIR}
    ${JAVASCRIPTCORE_DIR}/API
    ${JAVASCRIPTCORE_DIR}/assembler
    ${JAVASCRIPTCORE_DIR}/bytecode
    ${JAVASCRIPTCORE_DIR}/dfg
    ${JAVASCRIPTCORE_DIR}/disassembler
    ${JAVASCRIPTCORE_DIR}/heap
    ${JAVASCRIPTCORE_DIR}/interpreter
    ${JAVASCRIPTCORE_DIR}/jit
    ${JAVASCRIPTCORE_DIR}/llint
    ${JAVASCRIPTCORE_DIR}/parser
    ${JAVASCRIPTCORE_DIR}/profiler
    ${JAVASCRIPTCORE_DIR}/runtime
    ${JAVASCRIPTCORE_DIR}/ForwardingHeaders
    ${DERIVED_SOURCES_DIR}/ForwardingHeaders
    ${DERIVED_SOURCES_JAVASCRIPTCORE_DIR}
    ${TOOLS_DIR}/DumpRenderTree
    ${WTF_DIR}
    ${CMAKE_SOURCE_DIR}/Source
    ${CMAKE_BINARY_DIR}
    ${DERIVED_SOURCES_DIR}
    ${DERIVED_SOURCES_WEBCORE_DIR}
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
    ${ICU_INCLUDE_DIRS}
)

if (APPLE)
  list(APPEND DumpRenderTree_INCLUDE_DIRECTORIES
        ${WTF_DIR}/icu
    )
endif ()

set(DumpRenderTree_IDL_FILES
    "${WEBKIT_TESTRUNNER_UISCRIPTCONTEXT_DIR}/Bindings/UIScriptController.idl"
)

GENERATE_BINDINGS(DumpRenderTreeBindings
    OUTPUT_SOURCE DumpRenderTree_SOURCES
    INPUT_FILES ${DumpRenderTree_IDL_FILES}
    BASE_DIR ${DUMP_RENDER_TREE_BINDINGS_DIR}
    IDL_INCLUDES ../Bindings
    FEATURES ${FEATURE_DEFINES_WITH_SPACE_SEPARATOR}
    DESTINATION ${DERIVED_SOURCES_DIR}/DumpRenderTree
    GENERATOR DumpRenderTree)

WEBKIT_INCLUDE_CONFIG_FILES_IF_EXISTS()

include_directories(${DumpRenderTree_INCLUDE_DIRECTORIES})

add_library(DumpRenderTreeJava SHARED ${DumpRenderTree_SOURCES})
add_definitions(-DWEBCORE_EXPORT=WTF_IMPORT -DWEBCORE_TESTSUPPORT_EXPORT=WTF_IMPORT)
add_dependencies(DumpRenderTreeJava DumpRenderTreeBindings)

if (UNIX AND NOT APPLE)
  set_target_properties(DumpRenderTreeJava PROPERTIES LINK_FLAGS "-Wl,--no-undefined")
elseif (WIN32)
  # Workaround for MSBuild. It always creates Release|Debug folder
  # according to the build type on each target.
  install(PROGRAMS ${CMAKE_BINARY_DIR}/bin/DumpRenderTreeJava.dll DESTINATION ${CMAKE_BINARY_DIR}/lib OPTIONAL)
endif ()
set_target_properties(DumpRenderTreeJava PROPERTIES OUTPUT_NAME "DumpRenderTreeJava")
target_link_libraries(DumpRenderTreeJava ${DumpRenderTree_LIBRARIES})
