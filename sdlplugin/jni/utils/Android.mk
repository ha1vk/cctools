LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := ccsdlplugin

LOCAL_SRC_FILES := utils.c

LOCAL_CFLAGS := -O2 -Wall
LOCAL_LDLIBS := -lz -llog

include $(BUILD_SHARED_LIBRARY)
