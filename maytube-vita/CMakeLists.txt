cmake_minimum_required(VERSION 3.10)

if(NOT DEFINED CMAKE_TOOLCHAIN_FILE AND DEFINED ENV{VITASDK})
  set(CMAKE_TOOLCHAIN_FILE "$ENV{VITASDK}/share/vita.toolchain.cmake" CACHE PATH "toolchain file")
endif()

project(maytube-vita C)
include("${VITASDK}/share/vita.cmake" REQUIRED)

set(CMAKE_C_STANDARD 11)
set(VITA_APP_NAME "maytube")
set(VITA_TITLEID "MAYTUBE01")
set(VITA_VERSION "01.00")

find_package(PkgConfig REQUIRED)
pkg_check_modules(SDL2 REQUIRED IMPORTED_TARGET sdl2)
pkg_check_modules(SDL2_TTF REQUIRED IMPORTED_TARGET SDL2_ttf)
pkg_check_modules(CURL REQUIRED IMPORTED_TARGET libcurl)
pkg_check_modules(AVFORMAT REQUIRED IMPORTED_TARGET libavformat)
pkg_check_modules(AVCODEC REQUIRED IMPORTED_TARGET libavcodec)
pkg_check_modules(AVUTIL REQUIRED IMPORTED_TARGET libavutil)
pkg_check_modules(SWSCALE REQUIRED IMPORTED_TARGET libswscale)
pkg_check_modules(SWRESAMPLE REQUIRED IMPORTED_TARGET libswresample)
# libavcodec's mp3lame encoder pulls in mpg123 (lame's own internal
# decoder half) but the .pc files for avcodec/mp3lame don't propagate it,
# so it has to be linked explicitly or resolution fails on this symbol set.
pkg_check_modules(MPG123 REQUIRED IMPORTED_TARGET libmpg123)

add_executable(maytube-vita
  src/main.c
  src/http.c
  src/sabr.c
  src/scrape.c
  src/fetch.c
  src/player.c
  src/keyboard.c
)

target_link_libraries(maytube-vita
  PkgConfig::SDL2_TTF
  PkgConfig::SDL2
  PkgConfig::AVFORMAT
  PkgConfig::AVCODEC
  PkgConfig::SWSCALE
  PkgConfig::SWRESAMPLE
  PkgConfig::AVUTIL
  PkgConfig::MPG123
  PkgConfig::CURL
)

vita_create_self(maytube-vita.self maytube-vita)
vita_create_vpk(maytube-vita.vpk ${VITA_TITLEID} maytube-vita.self
  VERSION ${VITA_VERSION}
  NAME ${VITA_APP_NAME}
  # Bundled so the app is usable straight after install with no manual
  # per-user file copy -- lands at app0:font.ttf, read by main.c's
  # FONT_PATH. See assets/DejaVuSans-LICENSE.txt for its license.
  FILE assets/font.ttf font.ttf
)
