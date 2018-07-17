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

import android.net.Uri;
import android.telecom.Call;
import android.telecom.DisconnectCause;
import android.telecom.GatewayInfo;

/**
 * Represents a single call on UI. It is an abstraction of {@code android.telecom.Call}.
 */
public class UiCall {
    /**
     *  the next value of a monotonic increasing id.
     */
    private static int sNextCarPhoneCallId = 0;

    private final int mId;
    private final Call mCall;

    private int mState;
    private boolean mHasParent;
    private String mNumber;
    private CharSequence mDisconnectCause;
    private boolean mHasChildren;
    private Uri mGatewayInfoOriginalAddress;
    private long connectTimeMillis;

    private UiCall(int id, Call call) {
        mId = id;
        mCall = call;
    }

    public int getId() {
        return mId;
    }

    public int getState() {
        return mState;
    }

    public void setState(int state) {
        mState = state;
    }

    public boolean hasParent() {
        return mHasParent;
    }

    public void setHasParent(boolean hasParent) {
        mHasParent = hasParent;
    }

    public void setHasChildren(boolean hasChildren) {
        mHasChildren = hasChildren;
    }

    public boolean hasChildren() {
        return mHasChildren;
    }

    public String getNumber() {
        return mNumber;
    }

    public void setNumber(String number) {
        mNumber = number;
    }

    public CharSequence getDisconnectCause() {
        return mDisconnectCause;
    }

    public void setDisconnectCause(CharSequence disconnectCause) {
        mDisconnectCause = disconnectCause;
    }

    public Uri getGatewayInfoOriginalAddress() {
        return mGatewayInfoOriginalAddress;
    }

    public void setGatewayInfoOriginalAddress(Uri gatewayInfoOriginalAddress) {
        mGatewayInfoOriginalAddress = gatewayInfoOriginalAddress;
    }

    public long getConnectTimeMillis() {
        return connectTimeMillis;
    }

    public void setConnectTimeMillis(long connectTimeMillis) {
        this.connectTimeMillis = connectTimeMillis;
    }

    /**
     * Returns the {@link Call} represented by this UiCall.
     */
    public Call getTelecomCall() {
        return mCall;
    }

    /**
     * Creates a UiCall from {@param telecomCall}.
     *
     * @returns an empty default {@link UiCall} if the given call doesn't contains any call details.
     */
    public static UiCall createFromTelecomCall(Call telecomCall) {
        return updateFromTelecomCall(new UiCall(sNextCarPhoneCallId++, telecomCall), telecomCall);
    }

    /**
     * Updates the uiCall with a {@link Call}.
     */
    // TODO: read states from telecomCall dynamically instead of read and set values before hand.
    public static UiCall updateFromTelecomCall(UiCall uiCall, Call telecomCall) {
        uiCall.setState(telecomCall.getState());
        uiCall.setHasChildren(!telecomCall.getChildren().isEmpty());
        uiCall.setHasParent(telecomCall.getParent() != null);

        Call.Details details = telecomCall.getDetails();
        if (details == null) {
            return uiCall;
        }

        uiCall.setConnectTimeMillis(details.getConnectTimeMillis());

        DisconnectCause cause = details.getDisconnectCause();
        uiCall.setDisconnectCause(cause == null ? null : cause.getLabel());

        GatewayInfo gatewayInfo = details.getGatewayInfo();
        uiCall.setGatewayInfoOriginalAddress(
                gatewayInfo == null ? null : gatewayInfo.getOriginalAddress());

        String number = "";
        if (gatewayInfo != null) {
            number = gatewayInfo.getOriginalAddress().getSchemeSpecificPart();
        } else if (details.getHandle() != null) {
            number = details.getHandle().getSchemeSpecificPart();
        }
        uiCall.setNumber(number);

        return uiCall;
    }
}
