cmake_minimum_required(VERSION 3.10)

if(NOT DEFINED CMAKE_TOOLCHAIN_FILE)
    if(DEFINED ENV{VITASDK})
        set(CMAKE_TOOLCHAIN_FILE "$ENV{VITASDK}/share/vita.toolchain.cmake" CACHE PATH "toolchain file")
    else()
        message(FATAL_ERROR "Please set the VITASDK env var, or pass -DCMAKE_TOOLCHAIN_FILE=$VITASDK/share/vita.toolchain.cmake")
    endif()
endif()

project(vita_taxi C)

include("${VITASDK}/share/vita.cmake" REQUIRED)

set(VITA_APP_NAME "Vita Taxi")
set(VITA_TITLEID  "VTAX00001")
set(VITA_VERSION  "01.00")

include_directories(include)

add_executable(vita_taxi
    src/main.c
    src/car.c
    src/world.c
    src/game.c
    src/input.c
    src/render.c
)

target_link_libraries(vita_taxi
    vita2d
    SceDisplay_stub
    SceGxm_stub
    SceSysmodule_stub
    SceCtrl_stub
    SceTouch_stub
    SceHid_stub
    SceCommonDialog_stub
    SceAppMgr_stub
    ScePgf_stub
    freetype
    png
    jpeg
    z
    m
    c
)

vita_create_self(vita_taxi.self vita_taxi)
vita_create_vpk(vita_taxi.vpk ${VITA_TITLEID} vita_taxi.self
    VERSION ${VITA_VERSION}
    NAME    ${VITA_APP_NAME}
)
