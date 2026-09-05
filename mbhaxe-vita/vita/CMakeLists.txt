# PS Vita build of MBHaxe (Marble Blast Ultra).
#
# Prerequisites (see VITA_PORT.md for the full writeup):
#   - vitasdk installed and $VITASDK set (https://vitasdk.org)
#   - vdpm packages: cmake, SDL2 (vita port), vitaGL, zlib, libpng, libjpeg-turbo,
#     libogg, libvorbis, vitashark or vitaShaRK if vitaGL needs shader
#     recompilation at runtime, and a build of hashlink (see hashlink/ below)
#   - `haxe compile-vita.hxml` already run from the repo root, producing
#     vita/native/marblegame.c (this file is generated, not checked in)
#
# This does NOT yet build: it's a starting point encoding what we know needs
# to link, not a verified-working build script. Expect to iterate on it.

cmake_minimum_required(VERSION 3.10)

if(NOT DEFINED CMAKE_TOOLCHAIN_FILE)
	if(DEFINED ENV{VITASDK})
		set(CMAKE_TOOLCHAIN_FILE "$ENV{VITASDK}/share/vita.toolchain.cmake" CACHE PATH "toolchain file")
	else()
		message(FATAL_ERROR "Please define VITASDK to point to your SDK path, or set CMAKE_TOOLCHAIN_FILE")
	endif()
endif()

project(mbhaxe_vita C)
include("${VITASDK}/share/vita.cmake" REQUIRED)

set(VITA_APP_NAME "Marble Blast Ultra")
set(VITA_TITLEID  "MBHAXE001")
set(VITA_VERSION  "01.00")

# hlc_main.c is the standard hashlink entry point (see native/, copied from
# your hashlink checkout, same as the desktop targets in .circleci/config.yml)
add_executable(marblegame
	native/marblegame.c
	native/hlc_main.c
	stubs/datachannel.c
)

target_include_directories(marblegame PRIVATE
	native
	$ENV{VITASDK}/arm-vita-eabi/include
	${HASHLINK_INCLUDE_DIR}
)

# Everything hashlink/heaps needs (ui, openal, fmt, sdl, uv, ssl) has to be
# built as *static* libraries for Vita rather than the .hdll shared modules
# used on desktop - vitasdk homebrew is one statically-linked .self, there's
# no dynamic loader for hdll-style modules. See VITA_PORT.md "native deps"
# section for the state of each of these.
target_link_libraries(marblegame
	hl                 # hashlink runtime, static build (see hashlink/ notes below)
	sdl                # hlsdl, built against the vitasdk SDL2 port
	fmt
	ui
	uv
	ssl
	# openal            # not yet available for Vita, see VITA_PORT.md "audio"
	SDL2
	vitaGL
	vitashark
	SceDisplay_stub
	SceCtrl_stub
	SceTouch_stub
	SceAudio_stub
	SceSysmodule_stub
	SceGxm_stub
	SceCommonDialog_stub
	SceAppMgr_stub
	SceAppUtil_stub
	m
)

vita_create_self(marblegame.self marblegame UNSAFE)

# Bundle data/ into the VPK's app0:/data so ResourceLoader's relative paths
# ("data/...") resolve unchanged - no ResourceLoader edits needed.
vita_create_vpk(marblegame.vpk ${VITA_TITLEID} marblegame.self
	VERSION ${VITA_VERSION}
	NAME "${VITA_APP_NAME}"
	FILE ${CMAKE_SOURCE_DIR}/../data data
)
