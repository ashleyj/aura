project(aura-rt-core)
cmake_minimum_required(VERSION 2.8.8)

include_directories(../../core/include ${CMAKE_BINARY_DIR}/gc/include)

set(SRC
  rt.c
  reflection_helpers.c 
  java_lang_Class.c 
  java_lang_ClassCache.c 
  java_lang_Object.c 
  java_lang_Thread.c 
  java_lang_Throwable.c 
  java_lang_Runtime.c 
  java_lang_String.c 
  java_lang_System.c 
  java_lang_VMClassLoader.c 
  java_lang_ref_Reference.c 
  java_lang_reflect_AccessibleObject.c 
  java_lang_reflect_Array.c 
  java_lang_reflect_Constructor.c 
  java_lang_reflect_Field.c 
  java_lang_reflect_Method.c 
  java_lang_reflect_Proxy.c 
  java_net_NetworkInterface.c 
  aura_rt_Signals.c 
  aura_rt_VM.c 
  aura_rt_bro_Dl.c 
  sun_misc_Unsafe.c 
)

if(DARWIN)
  add_definitions(-DNOUNCRYPT)
  set(OBJC_SRC darwin.m)
  set(SRC ${SRC} ${OBJC_SRC})
  set(OBJC_FLAGS "-x objective-c")
  if(IOS)
    set(OBJC_FLAGS "${OBJC_FLAGS} -fobjc-abi-version=2")
  endif()
  set_property(SOURCE ${OBJC_SRC} PROPERTY LANGUAGE C)
  set_property(SOURCE ${OBJC_SRC} PROPERTY COMPILE_FLAGS ${OBJC_FLAGS})
endif()

add_library(aura-rt-core OBJECT ${SRC})
set(AURA_RT_CORE_OBJECTS $<TARGET_OBJECTS:aura-rt-core> PARENT_SCOPE)

