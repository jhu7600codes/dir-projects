TARGET := iphone:clang:latest:10.0
ARCHS = arm64 armv7
INSTALL_TARGET_PROCESSES = SpringBoard

include $(THEOS)/makefiles/common.mk

TWEAK_NAME = SlideToUnlock
SlideToUnlock_FILES = Tweak.x
SlideToUnlock_CFLAGS = -fobjc-arc

include $(THEOS_MAKE_PATH)/tweak.mk
