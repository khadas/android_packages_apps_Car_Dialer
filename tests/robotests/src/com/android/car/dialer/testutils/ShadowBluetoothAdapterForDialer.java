/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.dialer.testutils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowApplication;

import java.util.HashMap;
import java.util.Map;

/**
 * Derived from {@link org.robolectric.shadows.ShadowBluetoothAdapter}
 *
 * Needed for Bluetooth related tests because the default ShadowBluetooth Adapter does not include
 * an implementation of setProfileConnectionState.
 */

// TODO: Remove this and use the default ShadowBluetoothAdapter once Robolectric is updated
@Implements(value = BluetoothAdapter.class)
public class ShadowBluetoothAdapterForDialer extends
        org.robolectric.shadows.ShadowBluetoothAdapter {

    private Map<Integer, Integer> profileConnectionStateData = new HashMap<>();

    @Implementation
    public static synchronized BluetoothAdapter getDefaultAdapter() {
        return (BluetoothAdapter) ShadowApplication.getInstance().getBluetoothAdapter();
    }

    /**
     * Returns the connection state for the given Bluetooth {@code profile}, defaulting to {@link
     * BluetoothProfile.STATE_DISCONNECTED} if the profile's connection state was never set.
     *
     * Set a Bluetooth profile's connection state via {@link #setProfileConnectionState(int, int)}.
     */
    @Implementation
    public int getProfileConnectionState(int profile) {
        Integer state = profileConnectionStateData.get(profile);
        if (state == null) {
            return BluetoothProfile.STATE_DISCONNECTED;
        }
        return state;
    }

    /**
     * Sets the connection state {@code state} for the given BLuetoothProfile {@code profile}
     */
    public void setProfileConnectionState(int profile, int state) {
        profileConnectionStateData.put(profile, state);
    }
}
