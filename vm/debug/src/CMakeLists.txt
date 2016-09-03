project(debug)

include_directories(../../core/include ${CMAKE_BINARY_DIR}/gc/include)

set(SRC
  debug.c 
)

add_library(aura-debug SHARED ${SRC})

set_target_properties(aura-debug PROPERTIES SUFFIX "${LIB_SUFFIX}")
install(TARGETS aura-debug DESTINATION ${INSTALL_DIR})
