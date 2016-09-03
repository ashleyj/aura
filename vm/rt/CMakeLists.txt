project(rt)

add_subdirectory(aura)
add_subdirectory(android)

add_library(aura-rt SHARED ${AURA_RT_CORE_OBJECTS} ${ANDROID_OBJECTS})
set_target_properties(aura-rt PROPERTIES SUFFIX "${LIB_SUFFIX}")
install(TARGETS aura-rt DESTINATION ${INSTALL_DIR})
