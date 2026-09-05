# Building the real game for Vita

This is the actual command sequence that produced a fully linked
`marblegame.elf` and a real `.vpk` in-session — not aspirational, this ran.
Nothing here has been run on real hardware; see `VITA_PORT.md` for what
that means and what to check first.

## 0. Toolchain

A vitasdk cross toolchain, built from source per
[vitasdk/buildscripts](https://github.com/vitasdk/buildscripts). See
"toolchain gotchas" in `VITA_PORT.md` for two build-environment bugs you'll
likely hit (a locale bug extracting gcc's tarball, and a `gcc-final-target-libs`
step that has to be built explicitly). `$VITASDK` below is wherever you
installed it; put `$VITASDK/bin` on `PATH`.

Also: `haxe` and `neko` (4.3.3 tested; `apt install haxe neko` on Debian/
Ubuntu works and is much less hassle than haxe.org's installer here).

## 1. Rendering stack (vitaGL + friends)

In dependency order — each `make`/`cmake --install` puts its output
straight into `$VITASDK/arm-vita-eabi/{include,lib}`:

```sh
# taihen (headers/libs only, no build)
curl -LO https://github.com/yifanlu/taiHEN/releases/download/v0.11/taihen.tar.gz
tar xf taihen.tar.gz
cp -r include/* $VITASDK/arm-vita-eabi/include/
cp -r lib/* $VITASDK/arm-vita-eabi/lib/

# SceShaccCgExt (needs taihen)
git clone https://github.com/bythos14/SceShaccCgExt
cd SceShaccCgExt && mkdir build && cd build
cmake .. -DCMAKE_TOOLCHAIN_FILE=$VITASDK/share/vita.toolchain.cmake \
  -DCMAKE_C_FLAGS="-std=gnu11 -Wno-error=implicit-function-declaration -Wno-error=int-conversion -Wno-error=incompatible-pointer-types" \
  -DCMAKE_CXX_FLAGS="-std=gnu++11 -Wno-error=implicit-function-declaration -Wno-error=int-conversion -Wno-error=incompatible-pointer-types"
make -j4 && make install
cd ../..

# math-neon
git clone https://github.com/Rinnegatamante/math-neon
cd math-neon && make -j4
cp libmathneon.a $VITASDK/arm-vita-eabi/lib/
cp source/math_neon.h $VITASDK/arm-vita-eabi/include/
cd ..

# vitaShaRK (master, not the pinned v1.5 tag - vitaGL's HEAD needs a newer
# API than that tag has, specifically shark_set_shader_association_path)
git clone https://github.com/Rinnegatamante/vitaShaRK
cd vitaShaRK && make -j4
cp libvitashark.a $VITASDK/arm-vita-eabi/lib/
cp source/vitashark.h $VITASDK/arm-vita-eabi/include/
cd ..

# vitaGL
git clone https://github.com/Rinnegatamante/vitaGL
cd vitaGL && make NO_DEBUG=1 -j4
cp libvitaGL.a $VITASDK/arm-vita-eabi/lib/
cp source/vitaGL.h $VITASDK/arm-vita-eabi/include/
cd ..

# SDL2, patched with vitaGL's video backend
git clone --branch release-2.32.8 https://github.com/libsdl-org/SDL SDL2
cd SDL2
curl -O https://raw.githubusercontent.com/vitasdk/packages/master/sdl2_vitagl/vitagl-backend.patch
patch -p1 < vitagl-backend.patch
mkdir build && cd build
cmake .. -DCMAKE_TOOLCHAIN_FILE=$VITASDK/share/vita.toolchain.cmake \
  -DCMAKE_INSTALL_PREFIX=$VITASDK/arm-vita-eabi -DCMAKE_BUILD_TYPE=Release \
  -DSDL_TEST=OFF -DVIDEO_VITA_VGL=ON \
  -DCMAKE_C_FLAGS="-std=gnu11 -Wno-error=implicit-function-declaration -Wno-error=int-conversion -Wno-error=incompatible-pointer-types" \
  -DCMAKE_CXX_FLAGS="-std=gnu++11 -Wno-error=implicit-function-declaration -Wno-error=int-conversion -Wno-error=incompatible-pointer-types"
make -j4 && make install
# CMAKE_INSTALL_PREFIX above already points at arm-vita-eabi, unlike the
# mistake made in-session (installed to $VITASDK/ directly, had to copy
# SDL2/ and libSDL2.a over by hand afterward - use the prefix above and
# skip that step)
cd ../..
```

## 2. Audio (openal-soft + Vita backend)

```sh
git clone --branch openal-soft-1.19.1 https://github.com/kcat/openal-soft
curl -O https://raw.githubusercontent.com/isage/openal-soft/master/openal-soft-1.19.1-vita-1.patch
cd openal-soft
patch -p1 < ../openal-soft-1.19.1-vita-1.patch
mkdir build && cd build
cmake .. -DCMAKE_TOOLCHAIN_FILE=$VITASDK/share/vita.toolchain.cmake \
  -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=$VITASDK/arm-vita-eabi \
  -DCMAKE_C_FLAGS="-std=gnu11 -Wno-error=implicit-function-declaration -Wno-error=int-conversion -Wno-error=incompatible-pointer-types" \
  -DCMAKE_CXX_FLAGS="-std=gnu++11 -Wno-error=implicit-function-declaration -Wno-error=int-conversion -Wno-error=incompatible-pointer-types"
make -j4 && make install
```

Confirm the configure log says `Building OpenAL with support for the
following backends: VITA, WaveFile, Null` - if it falls back to just
WaveFile/Null the patch didn't apply.

## 3. Image/audio codecs (zlib, libpng, libjpeg-turbo, libogg, libvorbis)

```sh
# zlib
curl -LO https://github.com/madler/zlib/releases/download/v1.3.2/zlib-1.3.2.tar.xz
tar xf zlib-1.3.2.tar.xz && cd zlib-1.3.2
curl -O https://raw.githubusercontent.com/vitasdk/packages/master/zlib/zlib-no-pic.diff
patch -p1 < zlib-no-pic.diff
CC=arm-vita-eabi-gcc AR=arm-vita-eabi-ar RANLIB=arm-vita-eabi-ranlib \
  ./configure --prefix=$VITASDK/arm-vita-eabi
make -j4
cp libz.a $VITASDK/arm-vita-eabi/lib/
cp zlib.h zconf.h $VITASDK/arm-vita-eabi/include/
cd ..

# libpng (needs zlib)
git clone --branch v1.6.58 https://github.com/pnggroup/libpng
cd libpng
curl -O https://raw.githubusercontent.com/vitasdk/packages/master/libpng/libpng.patch
patch -p1 < libpng.patch
mkdir build && cd build
cmake .. -DCMAKE_TOOLCHAIN_FILE=$VITASDK/share/vita.toolchain.cmake \
  -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=$VITASDK/arm-vita-eabi \
  -DPNG_ARM_NEON=on -DPNG_SHARED=OFF -DSKIP_INSTALL_EXECUTABLES=ON \
  -DCMAKE_C_FLAGS="-std=gnu11 -Wno-error=implicit-function-declaration -Wno-error=int-conversion" \
  -DCMAKE_CXX_FLAGS="-std=gnu++11 -Wno-error=implicit-function-declaration -Wno-error=int-conversion"
make -j4 && make install
cd ../..

# libjpeg-turbo
curl -LO https://github.com/libjpeg-turbo/libjpeg-turbo/releases/download/3.2.0/libjpeg-turbo-3.2.0.tar.gz
tar xf libjpeg-turbo-3.2.0.tar.gz && cd libjpeg-turbo-3.2.0
mkdir build && cd build
cmake .. -DCMAKE_TOOLCHAIN_FILE=$VITASDK/share/vita.toolchain.cmake \
  -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=$VITASDK/arm-vita-eabi \
  -DENABLE_SHARED=FALSE -DWITH_SIMD=FALSE -DWITH_TOOLS=FALSE
make -j4 && make install
cd ../..

# libogg
curl -LO https://downloads.xiph.org/releases/ogg/libogg-1.3.6.tar.xz
tar xf libogg-1.3.6.tar.xz && cd libogg-1.3.6
mkdir build && cd build
cmake .. -DCMAKE_TOOLCHAIN_FILE=$VITASDK/share/vita.toolchain.cmake \
  -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=$VITASDK/arm-vita-eabi -DINSTALL_DOCS=OFF \
  -DCMAKE_C_FLAGS="-std=gnu11 -Wno-error=implicit-function-declaration -Wno-error=int-conversion"
make -j4 && make install
cd ../..

# libvorbis (needs libogg)
curl -LO http://downloads.xiph.org/releases/vorbis/libvorbis-1.3.7.tar.gz
tar xf libvorbis-1.3.7.tar.gz && cd libvorbis-1.3.7
mkdir build && cd build
cmake .. -DCMAKE_TOOLCHAIN_FILE=$VITASDK/share/vita.toolchain.cmake \
  -DCMAKE_BUILD_TYPE=Release -DCMAKE_INSTALL_PREFIX=$VITASDK/arm-vita-eabi \
  -DCMAKE_C_FLAGS="-std=gnu11 -Wno-error=implicit-function-declaration -Wno-error=int-conversion" \
  -DCMAKE_CXX_FLAGS="-std=gnu++11 -Wno-error=implicit-function-declaration -Wno-error=int-conversion"
make -j4 && make install
cd ../..
```

## 4. hashlink runtime + native modules

Set up haxelibs pointing at `hashlink/` in this repo (or RandomityGuy's
upstream fork, same source) - `heaps` and `datachannel` install via
`haxelib git`/`haxelib dev`, `hlsdl`/`hlopenal`/`hashlink` (the base
haxelib) via `haxelib dev` pointing at `hashlink/libs/sdl`,
`hashlink/libs/openal`, `hashlink/other/haxelib` respectively (see
`../hello-test/README.md` for the exact commands - the haxelib setup is
identical, this just adds `heaps`/`datachannel`/`colyseus-websocket`).

Build `libhl.a` from `hashlink/src/{gc.c,std/*.c}` +
`hashlink/include/pcre/*.c` exactly as in `hello-test/README.md`, but drop
`-DHL_CONSOLE` (never needed) and keep `-DHAVE_CONFIG_H -DPCRE2_CODE_UNIT_WIDTH=16` for the pcre sources.

```sh
HL=hashlink   # this repo's vendored+patched copy
FLAGS="-std=gnu11 -Wno-error=implicit-function-declaration -Wno-error=int-conversion -Wno-error=incompatible-pointer-types -D_GNU_SOURCE -DHL_VITA"

# libhl (see hello-test/README.md step 2 - same thing, just also -Wno-error=...)

# hlsdl
arm-vita-eabi-gcc -c $FLAGS -I$HL/libs/sdl -I$HL/src $HL/libs/sdl/sdl.c -o hlsdl_sdl.o
arm-vita-eabi-gcc -c $FLAGS -I$HL/libs/sdl -I$HL/src $HL/libs/sdl/gl.c -o hlsdl_gl.o
arm-vita-eabi-ar rcs libhlsdl.a hlsdl_sdl.o hlsdl_gl.o

# hlopenal
arm-vita-eabi-gcc -c $FLAGS -I$HL/libs/openal -I$HL/src $HL/libs/openal/openal.c -o hlopenal.o
arm-vita-eabi-ar rcs libhlopenal.a hlopenal.o

# fmt (image/audio codecs) - note the extra include paths for the bundled
# single-header libs, and mikktspace.c itself (the header alone isn't enough)
arm-vita-eabi-gcc -c $FLAGS -I$HL/libs/fmt -I$HL/src -I$HL/include/minimp3 -I$HL/include/mikktspace $HL/libs/fmt/fmt.c -o fmt.o
arm-vita-eabi-gcc -c $FLAGS -I$HL/libs/fmt -I$HL/src $HL/libs/fmt/dxt.c -o fmt_dxt.o
arm-vita-eabi-gcc -c $FLAGS -I$HL/libs/fmt -I$HL/src $HL/libs/fmt/mikkt.c -o fmt_mikkt.o
arm-vita-eabi-gcc -c $FLAGS -I$HL/libs/fmt -I$HL/src $HL/libs/fmt/sha1.c -o fmt_sha1.o
arm-vita-eabi-gcc -c -std=gnu11 -Wno-error=implicit-function-declaration -Wno-error=int-conversion -DHL_VITA -I$HL/include/mikktspace $HL/include/mikktspace/mikktspace.c -o mikktspace_impl.o
arm-vita-eabi-ar rcs libfmt.a fmt.o fmt_dxt.o fmt_mikkt.o fmt_sha1.o mikktspace_impl.o

# ui (used as-is, no changes)
arm-vita-eabi-gcc -c -std=c11 -DHL_VITA -I$HL/src $HL/libs/ui/ui_stub.c -o ui_stub.o

# networking stubs (replace datachannel/uv/ssl entirely)
arm-vita-eabi-gcc -c -std=c11 -DHL_VITA -I$HL/src stubs/datachannel.c -o datachannel_stub.o
arm-vita-eabi-gcc -c -std=c11 -DHL_VITA -I$HL/src stubs/uv.c -o uv_stub.o
arm-vita-eabi-gcc -c -std=c11 -DHL_VITA -I$HL/src stubs/ssl.c -o ssl_stub.o
```

## 5. The game itself

```sh
haxe ../compile-vita.hxml   # from the mbhaxe-vita/ root; produces vita/native/marblegame.c
cd vita/native
cp $HL/src/hlc_main.c .
arm-vita-eabi-gcc -c -O1 -std=c11 -D_GNU_SOURCE -DHL_VITA -I. -I../../hashlink/src marblegame.c -o marblegame.o
# took ~4.5 minutes in-session - it's 953 generated .c files pulled into one
# translation unit via #include, ~730k lines total
```

## 6. Link (the part that actually worked)

Order matters (a static linker resolves left to right; `-ltaihen_stub`
has to come *after* `-lSceShaccCgExt`, which needs symbols from it - not
before). Use `g++`, not `gcc`, as the final link driver - vitaGL's C++
translation units (`texture_swizzler.cpp`, the shader preprocessor) need
libstdc++, which `g++` pulls in automatically and `gcc` won't.

```sh
arm-vita-eabi-g++ -Wl,-q marblegame.o \
  libhlsdl.a libhlopenal.a libfmt.a \
  datachannel_stub.o uv_stub.o ssl_stub.o ui_stub.o \
  libhl.a \
  -lSDL2 -lvitaGL -lvitashark -lmathneon -lopenal \
  -lpng16 -lturbojpeg -ljpeg -lvorbisfile -lvorbis -logg -lz \
  -lpthread -lm \
  -lSceShaccCg_stub -lSceShaccCgExt -lSceGxm_stub -lSceDisplay_stub \
  -lSceCtrl_stub -lSceTouch_stub -lSceAudio_stub -lSceAudioIn_stub \
  -lSceSysmodule_stub -lSceAppMgr_stub -lSceAppUtil_stub \
  -lSceCommonDialog_stub -lSceKernelDmacMgr_stub -lSceIme_stub \
  -lSceHid_stub -lSceMotion_stub -lScePower_stub -lSceProcessmgr_stub \
  -lSceIofilemgr_stub \
  -ltaihen_stub \
  -o marblegame.elf
```

This produced `marblegame.elf` with **zero undefined symbols** in-session.

## 7. Package

```sh
vita-elf-create marblegame.elf marblegame.velf
vita-make-fself -c marblegame.velf eboot.bin
vita-mksfoex -s TITLE_ID="MBHAXE001" "Marble Blast Ultra" param.sfo
vita-pack-vpk -s param.sfo -b eboot.bin --add ../../data=data marblegame.vpk
```

`--add ../../data=data` bundles the game's asset folder into the VPK at
`data/` next to `eboot.bin`, matching what `ResourceLoader.hx`'s
`TorqueFileSystem(".", null)` expects to find relative to the app's
working directory (`app0:`, where the VPK's contents land on install).

This is where this session stops: a real `marblegame.vpk`, never installed
on hardware. Whatever happens when you actually run it is the next real
data point - see `VITA_PORT.md`'s rendering section for what to expect and
check first.
