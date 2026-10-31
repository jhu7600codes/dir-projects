TARGET := iphone:clang:10.3:10.0
ARCHS = armv7 arm64
INSTALL_TARGET_PROCESSES = SpringBoard

include $(THEOS)/makefiles/common.mk

TWEAK_NAME = NineUnlock10
NineUnlock10_FILES = Tweak.x
NineUnlock10_CFLAGS = -fobjc-arc
NineUnlock10_FRAMEWORKS = UIKit AudioToolbox

include $(THEOS_MAKE_PATH)/tweak.mk

SUBPROJECTS += prefs
include $(THEOS_MAKE_PATH)/aggregate.mk
