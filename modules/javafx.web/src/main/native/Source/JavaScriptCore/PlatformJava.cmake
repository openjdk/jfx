# set(JavaScriptCore_OUTPUT_NAME javascriptcorejava-${WEBKITJAVA_API_VERSION})
set(JavaScriptCore_LIBRARY_TYPE STATIC)
add_definitions(-DSTATICALLY_LINKED_WITH_WTF)
# configure_file(javascriptcorejava.pc.in ${CMAKE_BINARY_DIR}/Source/JavaScriptCore/javascriptcorejava-${WEBKITJAVA_API_VERSION}.pc @ONLY)
# configure_file(JavaScriptCore.gir.in ${CMAKE_BINARY_DIR}/JavaScriptCore-${WEBKITJAVA_API_VERSION}.gir @ONLY)

# add_custom_command(
#     OUTPUT ${CMAKE_BINARY_DIR}/JavaScriptCore-${WEBKITJAVA_API_VERSION}.typelib
#     DEPENDS ${CMAKE_BINARY_DIR}/JavaScriptCore-${WEBKITJAVA_API_VERSION}.gir
#     COMMAND ${INTROSPECTION_COMPILER} ${CMAKE_BINARY_DIR}/JavaScriptCore-${WEBKITJAVA_API_VERSION}.gir -o ${CMAKE_BINARY_DIR}/JavaScriptCore-${WEBKITJAVA_API_VERSION}.typelib
# )

# ADD_TYPELIB(${CMAKE_BINARY_DIR}/JavaScriptCore-${WEBKITJAVA_API_VERSION}.typelib)

# install(FILES "${CMAKE_BINARY_DIR}/Source/JavaScriptCore/javascriptcorejava-${WEBKITJAVA_API_VERSION}.pc"
#         DESTINATION "${LIB_INSTALL_DIR}/pkgconfig"
# )

# #//XXX: remove?
# install(FILES API/JavaScript.h
#               API/JSBase.h
#               API/JSContextRef.h
#               API/JSObjectRef.h
#               API/JSStringRef.h
#               API/JSValueRef.h
#               API/WebKitAvailability.h
#         DESTINATION "${WEBKITGTK_HEADER_INSTALL_DIR}/JavaScriptCore"
# )

# if (ENABLE_INTROSPECTION)
#     install(FILES ${CMAKE_BINARY_DIR}/JavaScriptCore-${WEBKITJAVA_API_VERSION}.gir
#             DESTINATION ${INTROSPECTION_INSTALL_GIRDIR}
#     )
#     install(FILES ${CMAKE_BINARY_DIR}/JavaScriptCore-${WEBKITJAVA_API_VERSION}.typelib
#             DESTINATION ${INTROSPECTION_INSTALL_TYPELIBDIR}
#     )
# endif ()
message(STATUS "#### ####= LIB_INSTALL_DIR  ${LIB_INSTALL_DIR}")
message(STATUS "#### ####= WEBKITJAVA_API_VERSION  ${WEBKITJAVA_API_VERSION}")
message(STATUS "#### ####= JavaScriptCore_LIBRARIES  ${JavaScriptCore_LIBRARIES}")
list(APPEND JavaScriptCore_LIBRARIES
	${ICU_LIBRARIES}
)
message(STATUS "#### ####= JavaScriptCore_LIBRARIES  ${JavaScriptCore_LIBRARIES}")

message(STATUS "#### ####= ICU_LIBRARIES  ${ICU_LIBRARIES}")
message(STATUS "#### ####= ICU_DATA_LIBRARIES  ${ICU_DATA_LIBRARIES}")
message(STATUS "#### ####= ICU_I38N_LIBRARIES  ${ICU_I18N_LIBRARIES}")

# target_link_libraries(JavaScriptCore ${JavaScriptCore_LIBRARIES}) #//XXX remove


#//XXX are these needed? 
list(APPEND JavaScriptCore_LUT_FILES
	runtime/ArrayPrototype.cpp
	runtime/MathObject.cpp
	runtime/NamePrototype.cpp
	runtime/RegExpObject.cpp
)
message(STATUS "+++++++++++++++++++++ append JavaScriptCore_LUT_FILES ${JavaScriptCore_LUT_FILES}")

list(APPEND JavaScriptCore_INCLUDE_DIRECTORIES
    ${ICU_INCLUDE_DIRS}
    ${CMAKE_BINARY_DIR}/../../gensrc/headers
    ${WTF_DIR}/wtf
    ${CMAKE_SOURCE_DIR}/Source/WebCore/platform
    ${JAVA_INCLUDE_PATH}
    ${JAVA_INCLUDE_PATH2}
)
if (APPLE)
    list(APPEND JavaScriptCore_INCLUDE_DIRECTORIES
        "${JAVASCRIPTCORE_DIR}/icu"
    )
    find_library(COREFOUNDATION_LIBRARY CoreFoundation)
    list(APPEND JavaScriptCore_LIBRARIES
        ${COREFOUNDATION_LIBRARY}
        libicucore.dylib
    )
endif()

list(APPEND JavaScriptCore_LIBRARIES
    ${JAVA_JVM_LIBRARY}
#     ${GLIB_LIBRARIES}
)

list(APPEND JavaScriptCore_SYSTEM_INCLUDE_DIRECTORIES
	${JDK_INCLUDE_DIRS} #//XXX
#     ${GLIB_INCLUDE_DIRS}
)

list(APPEND JavaScriptCore_SOURCES
	runtime/WatchdogJava.cpp
    # ${WTF_DIR}/wtf/java/MainThreadJava.cpp
	# ${WTF_DIR}/wtf/java/StringJava.cpp
)

message(STATUS "#### ####========  JavaScriptCore_LIBRARIES  ${JavaScriptCore_LIBRARIES}")
message(STATUS "#### ####========  JavaScriptCore_INCLUDE_DIRECTORIES  ${JavaScriptCore_INCLUDE_DIRECTORIES}")
message(STATUS "#### ####========= JavaScriptCore_SYSTEM_INCLUDE_DIRECTORIES  ${JavaScriptCore_SYSTEM_INCLUDE_DIRECTORIES}")
message(STATUS "#### ####========= JavaScriptCore_SOURCES  ${JavaScriptCore_SOURCES}")
message(STATUS "#### ####========= JavaScriptCore_RUNTIME_SOURCES  ${JavaScriptCore_RUNTIME_SOURCES}")
