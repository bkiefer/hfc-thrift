# - Try to find TECS
# Once done this will define awesome stuff:
#   TECS_FOUND    if found
#   TECS_LIBRARIES        libraries for linking
#   TECS_INCLUDE_DIR      path to the include folder for tecs
#
#   SET ${TECS_HOME} to look for a specific path
#   SET ${TECS_USE_STATIC_LIBS} find libraries for static linking
#   SET ${TECS_VERSION_MAJOR} to look for a tecs version with major
#   @author Yannick Körber

if(NOT TECS_VERSION_MAJOR)
  set(TECS_VERSION_MAJOR 2)
  message("TECS_VERSION_MAJOR not set. using ${TECS_VERSION_MAJOR}")
endif(NOT TECS_VERSION_MAJOR)

if (TECS_LIBRARIES AND TECS_INCLUDE_DIR)
  # cached
  set(TECS_FOUND TRUE)
else (TECS_LIBRARIES AND TECS_INCLUDE_DIR)
  #prefer path TECS_HOME for include
  find_path(TECS_INCLUDE_DIR NAMES tecs-${TECS_VERSION_MAJOR}
    PATHS ${TECS_HOME}/include
    NO_DEFAULT_PATH
  )
  find_path(TECS_INCLUDE_DIR NAMES tecs-${TECS_VERSION_MAJOR})

  if(${TECS_USE_STATIC_LIBS})
    set(TECS_LIB_NAMES libtecs.${TECS_VERSION_MAJOR}.a libtecs.${TECS_VERSION_MAJOR}.lib)
  else(${TECS_USE_STATIC_LIBS})
    set(TECS_LIB_NAMES libtecs.${TECS_VERSION_MAJOR}.dylib libtecs.so.${TECS_VERSION_MAJOR} libtecs.${TECS_VERSION_MAJOR}.dll)
  endif(${TECS_USE_STATIC_LIBS})

  #prefer path TECS_HOME for lib
  find_library(TECS_LIBRARIES NAMES ${TECS_LIB_NAMES}
    PATHS ${TECS_HOME}/lib
    NO_DEFAULT_PATH
  )
  find_library(TECS_LIBRARIES NAMES ${TECS_LIB_NAMES})


include(FindPackageHandleStandardArgs)
find_package_handle_standard_args(TECS TECS_INCLUDE_DIR TECS_LIBRARIES)
# advanced view only
mark_as_advanced(TECS_INCLUDE_DIR TECS_LIBRARIES)
endif (TECS_LIBRARIES AND TECS_INCLUDE_DIR)
