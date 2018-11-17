#############################################################
# Car Dialer Robolectric test target.                    #
#############################################################
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := CarDialerRoboTests

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_JAVA_RESOURCE_DIRS := config

# Include the testing libraries
LOCAL_JAVA_LIBRARIES := \
    robolectric_android-all-stub \
    Robolectric_all-target \
    mockito-robolectric-prebuilt \
    truth-prebuilt

LOCAL_INSTRUMENTATION_FOR := CarDialerApp

LOCAL_MODULE_TAGS := optional

include $(BUILD_STATIC_JAVA_LIBRARY)

#############################################################
# Car Dialer runner target to run the previous target.   #
#############################################################
include $(CLEAR_VARS)

LOCAL_MODULE := RunCarDialerRoboTests

LOCAL_JAVA_LIBRARIES := \
    CarDialerRoboTests \
    robolectric_android-all-stub \
    Robolectric_all-target \
    mockito-robolectric-prebuilt \
    truth-prebuilt

LOCAL_TEST_PACKAGE := CarDialerApp

LOCAL_INSTRUMENT_SOURCE_DIRS := $(dir $(LOCAL_PATH))../src

include external/robolectric-shadows/run_robotests.mk
