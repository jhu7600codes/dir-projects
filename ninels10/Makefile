TARGET := iphone:clang:10.3:10.0
ARCHS = armv7 arm64
INSTALL_TARGET_PROCESSES = SpringBoard

include $(THEOS)/makefiles/common.mk

TWEAK_NAME = NineLS10
NineLS10_FILES = Tweak.x
NineLS10_CFLAGS = -fobjc-arc
NineLS10_FRAMEWORKS = UIKit AudioToolbox
NineLS10_PRIVATE_FRAMEWORKS = MediaRemote

include $(THEOS_MAKE_PATH)/tweak.mk

SUBPROJECTS += prefs
include $(THEOS_MAKE_PATH)/aggregate.mk
