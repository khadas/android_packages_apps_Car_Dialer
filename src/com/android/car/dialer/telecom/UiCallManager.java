/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.car.dialer.telecom;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadsetClient;
import android.bluetooth.BluetoothHeadsetClientCall;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.IBinder;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.CallAudioState.CallAudioRoute;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.car.dialer.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The entry point for all interactions between UI and telecom.
 */
public class UiCallManager {
    private static String TAG = "Em.TelecomMgr";

    private static final String HFP_CLIENT_CONNECTION_SERVICE_CLASS_NAME
            = "com.android.bluetooth.hfpclient.connserv.HfpClientConnectionService";
    // Rate limit how often you can place outgoing calls.
    private static final long MIN_TIME_BETWEEN_CALLS_MS = 3000;
    private static final List<Integer> sCallStateRank = new ArrayList<>();
    private static UiCallManager sUiCallManager;

    static {
        // States should be added from lowest rank to highest
        sCallStateRank.add(Call.STATE_DISCONNECTED);
        sCallStateRank.add(Call.STATE_DISCONNECTING);
        sCallStateRank.add(Call.STATE_NEW);
        sCallStateRank.add(Call.STATE_CONNECTING);
        sCallStateRank.add(Call.STATE_SELECT_PHONE_ACCOUNT);
        sCallStateRank.add(Call.STATE_HOLDING);
        sCallStateRank.add(Call.STATE_ACTIVE);
        sCallStateRank.add(Call.STATE_DIALING);
        sCallStateRank.add(Call.STATE_RINGING);
    }

    private Context mContext;
    private long mLastPlacedCallTimeMs;

    private TelecomManager mTelecomManager;
    private InCallServiceImpl mInCallService;
    private BluetoothHeadsetClient mBluetoothHeadsetClient;
    private final Map<UiCall, Call> mCallMapping = new HashMap<>();

    /**
     * Initialized a globally accessible {@link UiCallManager} which can be retrieved by
     * {@link #get}. If this function is called a second time before calling {@link #tearDown()},
     * an exception will be thrown.
     *
     * @param applicationContext Application context.
     */
    public static UiCallManager init(Context applicationContext) {
        if (sUiCallManager == null) {
            sUiCallManager = new UiCallManager(applicationContext);
        } else {
            throw new IllegalStateException("UiCallManager has been initialized.");
        }
        return sUiCallManager;
    }

    /**
     * Gets the global {@link UiCallManager} instance. Make sure
     * {@link #init(Context)} is called before calling this method.
     */
    public static UiCallManager get() {
        if (sUiCallManager == null) {
            throw new IllegalStateException(
                    "Call UiCallManager.init(Context) before calling this function");
        }
        return sUiCallManager;
    }

    private UiCallManager(Context context) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "SetUp");
        }

        mContext = context;

        mTelecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);
        Intent intent = new Intent(context, InCallServiceImpl.class);
        intent.setAction(InCallServiceImpl.ACTION_LOCAL_BIND);
        context.bindService(intent, mInCallServiceConnection, Context.BIND_AUTO_CREATE);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            adapter.getProfileProxy(mContext, new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    if (profile == BluetoothProfile.HEADSET_CLIENT) {
                        mBluetoothHeadsetClient = (BluetoothHeadsetClient) proxy;
                    }
                }

                @Override
                public void onServiceDisconnected(int profile) {
                }
            }, BluetoothProfile.HEADSET_CLIENT);
        }
    }

    private final ServiceConnection mInCallServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onServiceConnected: " + name + ", service: " + binder);
            }
            mInCallService = ((InCallServiceImpl.LocalBinder) binder).getService();
            mInCallService.registerCallback(mInCallServiceCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onServiceDisconnected: " + name);
            }
            mInCallService.unregisterCallback(mInCallServiceCallback);
        }

        private InCallServiceImpl.Callback mInCallServiceCallback =
                new InCallServiceImpl.Callback() {
                    @Override
                    public void onTelecomCallAdded(Call telecomCall) {
                        doTelecomCallAdded(telecomCall);
                    }

                    @Override
                    public void onTelecomCallRemoved(Call telecomCall) {
                        doTelecomCallRemoved(telecomCall);
                    }

                    @Override
                    public void onCallAudioStateChanged(CallAudioState audioState) {
                    }
                };
    };

    /**
     * Tears down the {@link UiCallManager}. Calling this function will null out the global
     * accessible {@link UiCallManager} instance. Remember to re-initialize the
     * {@link UiCallManager}.
     */
    public void tearDown() {
        if (mInCallService != null) {
            mContext.unbindService(mInCallServiceConnection);
            mInCallService = null;
        }
        mCallMapping.clear();
        // Clear out the mContext reference to avoid memory leak.
        mContext = null;
        sUiCallManager = null;
    }

    protected void placeCall(String number) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "placeCall: " + number);
        }
        Uri uri = Uri.fromParts("tel", number, null);
        Log.d(TAG, "android.telecom.TelecomManager#placeCall: " + uri);
        mTelecomManager.placeCall(uri, null);
    }

    public void answerCall(UiCall uiCall) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "answerCall: " + uiCall);
        }

        Call telecomCall = mCallMapping.get(uiCall);
        if (telecomCall != null) {
            telecomCall.answer(0);
        }
    }

    public void rejectCall(UiCall uiCall, boolean rejectWithMessage, String textMessage) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "rejectCall: " + uiCall + ", rejectWithMessage: " + rejectWithMessage
                    + "textMessage: " + textMessage);
        }

        Call telecomCall = mCallMapping.get(uiCall);
        if (telecomCall != null) {
            telecomCall.reject(rejectWithMessage, textMessage);
        }
    }

    public void disconnectCall(UiCall uiCall) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "disconnectCall: " + uiCall);
        }

        Call telecomCall = mCallMapping.get(uiCall);
        if (telecomCall != null) {
            telecomCall.disconnect();
        }
    }

    public List<UiCall> getCalls() {
        return new ArrayList<>(mCallMapping.keySet());
    }

    public boolean getMuted() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "getMuted");
        }
        if (mInCallService == null) {
            return false;
        }
        CallAudioState audioState = mInCallService.getCallAudioState();
        return audioState != null && audioState.isMuted();
    }

    public void setMuted(boolean muted) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "setMuted: " + muted);
        }
        if (mInCallService == null) {
            return;
        }
        mInCallService.setMuted(muted);
    }

    public int getSupportedAudioRouteMask() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "getSupportedAudioRouteMask");
        }

        CallAudioState audioState = getCallAudioStateOrNull();
        return audioState != null ? audioState.getSupportedRouteMask() : 0;
    }

    public List<Integer> getSupportedAudioRoute() {
        List<Integer> audioRouteList = new ArrayList<>();

        boolean isBluetoothPhoneCall = isBluetoothCall();
        if (isBluetoothPhoneCall) {
            // if this is bluetooth phone call, we can only select audio route between vehicle
            // and phone.
            // Vehicle speaker route.
            audioRouteList.add(CallAudioState.ROUTE_BLUETOOTH);
            // Headset route.
            audioRouteList.add(CallAudioState.ROUTE_EARPIECE);
        } else {
            // Most likely we are making phone call with on board SIM card.
            int supportedAudioRouteMask = getSupportedAudioRouteMask();

            if ((supportedAudioRouteMask & CallAudioState.ROUTE_EARPIECE) != 0) {
                audioRouteList.add(CallAudioState.ROUTE_EARPIECE);
            } else if ((supportedAudioRouteMask & CallAudioState.ROUTE_BLUETOOTH) != 0) {
                audioRouteList.add(CallAudioState.ROUTE_BLUETOOTH);
            } else if ((supportedAudioRouteMask & CallAudioState.ROUTE_WIRED_HEADSET) != 0) {
                audioRouteList.add(CallAudioState.ROUTE_WIRED_HEADSET);
            } else if ((supportedAudioRouteMask & CallAudioState.ROUTE_SPEAKER) != 0) {
                audioRouteList.add(CallAudioState.ROUTE_SPEAKER);
            }
        }

        return audioRouteList;
    }

    public boolean isBluetoothCall() {
        PhoneAccountHandle phoneAccountHandle =
                mTelecomManager.getUserSelectedOutgoingPhoneAccount();
        if (phoneAccountHandle != null && phoneAccountHandle.getComponentName() != null) {
            return HFP_CLIENT_CONNECTION_SERVICE_CLASS_NAME.equals(
                    phoneAccountHandle.getComponentName().getClassName());
        } else {
            return false;
        }
    }

    public int getAudioRoute() {
        CallAudioState audioState = getCallAudioStateOrNull();
        int audioRoute = audioState != null ? audioState.getRoute() : 0;
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "getAudioRoute " + audioRoute);
        }
        return audioRoute;
    }

    /**
     * Re-route the audio out phone of the ongoing phone call.
     */
    public void setAudioRoute(@CallAudioRoute int audioRoute) {
        if (mBluetoothHeadsetClient != null && isBluetoothCall()) {
            for (BluetoothDevice device : mBluetoothHeadsetClient.getConnectedDevices()) {
                List<BluetoothHeadsetClientCall> currentCalls =
                        mBluetoothHeadsetClient.getCurrentCalls(device);
                if (currentCalls != null && !currentCalls.isEmpty()) {
                    if (audioRoute == CallAudioState.ROUTE_BLUETOOTH) {
                        mBluetoothHeadsetClient.connectAudio(device);
                    } else if ((audioRoute & CallAudioState.ROUTE_WIRED_OR_EARPIECE) != 0) {
                        mBluetoothHeadsetClient.disconnectAudio(device);
                    }
                }
            }
        }
        // TODO: Implement routing audio if current call is not a bluetooth call.
    }

    public void holdCall(UiCall uiCall) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "holdCall: " + uiCall);
        }

        Call telecomCall = mCallMapping.get(uiCall);
        if (telecomCall != null) {
            telecomCall.hold();
        }
    }

    public void unholdCall(UiCall uiCall) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "unholdCall: " + uiCall);
        }

        Call telecomCall = mCallMapping.get(uiCall);
        if (telecomCall != null) {
            telecomCall.unhold();
        }
    }

    public void playDtmfTone(UiCall uiCall, char digit) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "playDtmfTone: call: " + uiCall + ", digit: " + digit);
        }

        Call telecomCall = mCallMapping.get(uiCall);
        if (telecomCall != null) {
            telecomCall.playDtmfTone(digit);
        }
    }

    public void stopDtmfTone(UiCall uiCall) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "stopDtmfTone: call: " + uiCall);
        }

        Call telecomCall = mCallMapping.get(uiCall);
        if (telecomCall != null) {
            telecomCall.stopDtmfTone();
        }
    }

    public void postDialContinue(UiCall uiCall, boolean proceed) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "postDialContinue: call: " + uiCall + ", proceed: " + proceed);
        }

        Call telecomCall = mCallMapping.get(uiCall);
        if (telecomCall != null) {
            telecomCall.postDialContinue(proceed);
        }
    }

    public void conference(UiCall uiCall, UiCall otherUiCall) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "conference: call: " + uiCall + ", otherCall: " + otherUiCall);
        }

        Call telecomCall = mCallMapping.get(uiCall);
        Call otherTelecomCall = mCallMapping.get(otherUiCall);
        if (telecomCall != null) {
            telecomCall.conference(otherTelecomCall);
        }
    }

    public void splitFromConference(UiCall uiCall) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "splitFromConference: call: " + uiCall);
        }

        Call telecomCall = mCallMapping.get(uiCall);
        if (telecomCall != null) {
            telecomCall.splitFromConference();
        }
    }

    private UiCall doTelecomCallAdded(final Call telecomCall) {
        Log.d(TAG, "doTelecomCallAdded: " + telecomCall);
        return getOrCreateCallContainer(telecomCall);
    }

    private void doTelecomCallRemoved(Call telecomCall) {
        mCallMapping.remove(getOrCreateCallContainer(telecomCall));
    }

    private UiCall getOrCreateCallContainer(Call telecomCall) {
        for (Map.Entry<UiCall, Call> entry : mCallMapping.entrySet()) {
            if (entry.getValue() == telecomCall) {
                return entry.getKey();
            }
        }

        UiCall uiCall = new UiCall(telecomCall);
        mCallMapping.put(uiCall, telecomCall);
        return uiCall;
    }

    private CallAudioState getCallAudioStateOrNull() {
        return mInCallService != null ? mInCallService.getCallAudioState() : null;
    }

    /** Returns a first call that matches at least one provided call state */
    public UiCall getCallWithState(int... callStates) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "getCallWithState: " + callStates);
        }
        for (UiCall call : getCalls()) {
            for (int callState : callStates) {
                if (call.getState() == callState) {
                    return call;
                }
            }
        }
        return null;
    }

    public UiCall getPrimaryCall() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "getPrimaryCall");
        }
        List<UiCall> calls = getCalls();
        if (calls.isEmpty()) {
            return null;
        }

        Collections.sort(calls, getCallComparator());
        UiCall uiCall = calls.get(0);
        if (uiCall.hasParent()) {
            return null;
        }
        return uiCall;
    }

    public UiCall getSecondaryCall() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "getSecondaryCall");
        }
        List<UiCall> calls = getCalls();
        if (calls.size() < 2) {
            return null;
        }

        Collections.sort(calls, getCallComparator());
        UiCall uiCall = calls.get(1);
        if (uiCall.hasParent()) {
            return null;
        }
        return uiCall;
    }

    public static final int CAN_PLACE_CALL_RESULT_OK = 0;
    public static final int CAN_PLACE_CALL_RESULT_NETWORK_UNAVAILABLE = 1;
    public static final int CAN_PLACE_CALL_RESULT_HFP_UNAVAILABLE = 2;
    public static final int CAN_PLACE_CALL_RESULT_AIRPLANE_MODE = 3;

    public int getCanPlaceCallStatus(String number, boolean bluetoothRequired) {
        // TODO(b/26191392): figure out the logic for projected and embedded modes
        return CAN_PLACE_CALL_RESULT_OK;
    }

    public String getFailToPlaceCallMessage(int canPlaceCallResult) {
        switch (canPlaceCallResult) {
            case CAN_PLACE_CALL_RESULT_OK:
                return "";
            case CAN_PLACE_CALL_RESULT_HFP_UNAVAILABLE:
                return mContext.getString(R.string.error_no_hfp);
            case CAN_PLACE_CALL_RESULT_AIRPLANE_MODE:
                return mContext.getString(R.string.error_airplane_mode);
            case CAN_PLACE_CALL_RESULT_NETWORK_UNAVAILABLE:
            default:
                return mContext.getString(R.string.error_network_not_available);
        }
    }

    /** Places call only if there's no outgoing call right now */
    public void safePlaceCall(String number, boolean bluetoothRequired) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "safePlaceCall: " + number);
        }

        int placeCallStatus = getCanPlaceCallStatus(number, bluetoothRequired);
        if (placeCallStatus != CAN_PLACE_CALL_RESULT_OK) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Unable to place a call: " + placeCallStatus);
            }
            return;
        }

        UiCall outgoingCall = getCallWithState(
                Call.STATE_CONNECTING, Call.STATE_NEW, Call.STATE_DIALING);
        if (outgoingCall == null) {
            long now = Calendar.getInstance().getTimeInMillis();
            if (now - mLastPlacedCallTimeMs > MIN_TIME_BETWEEN_CALLS_MS) {
                placeCall(number);
                mLastPlacedCallTimeMs = now;
            } else {
                if (Log.isLoggable(TAG, Log.INFO)) {
                    Log.i(TAG, "You have to wait " + MIN_TIME_BETWEEN_CALLS_MS
                            + "ms between making calls");
                }
            }
        }
    }

    public void callVoicemail() {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "callVoicemail");
        }

        String voicemailNumber = TelecomUtils.getVoicemailNumber(mContext);
        if (TextUtils.isEmpty(voicemailNumber)) {
            Log.w(TAG, "Unable to get voicemail number.");
            return;
        }
        safePlaceCall(voicemailNumber, false);
    }

    private static Comparator<UiCall> getCallComparator() {
        return new Comparator<UiCall>() {
            @Override
            public int compare(UiCall call, UiCall otherCall) {
                if (call.hasParent() && !otherCall.hasParent()) {
                    return 1;
                } else if (!call.hasParent() && otherCall.hasParent()) {
                    return -1;
                }
                int carCallRank = sCallStateRank.indexOf(call.getState());
                int otherCarCallRank = sCallStateRank.indexOf(otherCall.getState());

                return otherCarCallRank - carCallRank;
            }
        };
    }
}
