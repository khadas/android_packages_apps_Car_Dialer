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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Represents a single call on UI. It is an abstraction of {@code android.telecom.Call}.
 *
 * <p> Deprecated. Use CallDetailLiveData and CallStateLiveData instead.
 */
@Deprecated
public class UiCall {
    private final Call mCall;

    public UiCall(@NonNull Call call) {
        mCall = call;
    }

    /**
     * @see Call#getState()
     */
    public int getState() {
        return mCall.getState();
    }

    /**
     * Returns true if the current call has a parent call.
     */
    public boolean hasParent() {
        return mCall.getParent() != null;
    }

    /**
     * Returns true if the current call has a child call.
     */
    public boolean hasChildren() {
        return !mCall.getChildren().isEmpty();
    }

    /**
     * Returns the phone number of this call.
     */
    public String getNumber() {
        Call.Details details = mCall.getDetails();
        GatewayInfo gatewayInfo = mCall.getDetails().getGatewayInfo();
        String number;
        if (gatewayInfo != null) {
            number = gatewayInfo.getOriginalAddress().getSchemeSpecificPart();
        } else if (details.getHandle() != null) {
            number = details.getHandle().getSchemeSpecificPart();
        } else {
            number = "";
        }
        return number;
    }

    /**
     * Returns the disconnect reason.
     */
    @Nullable
    public CharSequence getDisconnectCause() {
        Call.Details details = mCall.getDetails();
        DisconnectCause cause =
                details == null ? null : details.getDisconnectCause();
        return cause == null ? null : cause.getLabel();
    }

    /**
     * @see GatewayInfo#getOriginalAddress()
     */
    @Nullable
    public Uri getGatewayInfoOriginalAddress() {
        GatewayInfo gatewayInfo = mCall.getDetails().getGatewayInfo();
        return gatewayInfo == null ? null : gatewayInfo.getOriginalAddress();
    }

    /**
     * @see Call.Details#getConnectTimeMillis()
     */
    public long getConnectTimeMillis() {
        Call.Details details = mCall.getDetails();
        if (details != null) {
            return details.getConnectTimeMillis();
        } else {
            return 0;
        }
    }

    /**
     * Returns the {@link Call} represented by this UiCall.
     */
    public Call getTelecomCall() {
        return mCall;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof UiCall) {
            return mCall.equals(((UiCall) obj).mCall);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mCall.hashCode();
    }
}
