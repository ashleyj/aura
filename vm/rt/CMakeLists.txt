project(rt)

add_subdirectory(aura)
add_subdirectory(android)

add_library(aura-rt STATIC ${AURA_RT_CORE_OBJECTS} ${ANDROID_OBJECTS})
set_target_properties(aura-rt PROPERTIES SUFFIX "${LIB_SUFFIX}")
merge_static_lib_object_files(aura-rt 'Java_*' 'rvmRT*')
install(TARGETS aura-rt DESTINATION ${INSTALL_DIR})
