include(ExternalProject)

set (ICU_DATA_BUILD_COMMAND bash icudata_wrapper.sh ${CMAKE_BINARY_DIR} ${CMAKE_BUILD_TYPE} ${ICU_JAVA_DATA_LIB} ${CMAKE_C_FLAGS})

if (APPLE)
    # Fixed in ICU 59.1 http://bugs.icu-project.org/trac/ticket/12643
    string (REPLACE "-std=c++1y" "" CMAKE_CXX_FLAGS ${CMAKE_CXX_FLAGS})
    # Fixed in ICU 56.1 http://bugs.icu-project.org/trac/ticket/11435
    string (REPLACE "-fno-rtti" "" CMAKE_CXX_FLAGS ${CMAKE_CXX_FLAGS})
    list(APPEND ICU_DATA_BUILD_COMMAND ${CMAKE_CXX_FLAGS} ${CMAKE_OSX_DEPLOYMENT_TARGET})
else ()
    list(APPEND ICU_DATA_BUILD_COMMAND ${CMAKE_CXX_FLAGS})
endif ()

add_custom_target(icudatagen ALL
                  COMMAND ${ICU_DATA_BUILD_COMMAND}
                  DEPENDS pkgdata
                  BYPRODUCTS ${ICU_JAVA_DATA_LIB}
                  WORKING_DIRECTORY ${CMAKE_CURRENT_SOURCE_DIR}
                  )

add_library(${ICU_JAVA_ICU_DATA_LIB_NAME} STATIC IMPORTED)

set_target_properties(${ICU_JAVA_ICU_DATA_LIB_NAME} PROPERTIES IMPORTED_LOCATION ${ICU_JAVA_DATA_LIB})
