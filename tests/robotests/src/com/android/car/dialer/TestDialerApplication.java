/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.android.car.dialer;

import static org.robolectric.Shadows.shadowOf;
import static org.mockito.Mockito.mock;

import android.app.Application;
import android.content.ComponentName;

import com.android.car.dialer.telecom.InCallServiceImpl;
import com.android.car.dialer.telecom.UiCallManager;

/** Robolectric runtime application for Dialer. Must be Test + application class name. */
public class TestDialerApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public void initUiCallManager() {
        shadowOf(this).setComponentNameAndServiceForBindService(
                new ComponentName(this, InCallServiceImpl.class),
                mock(InCallServiceImpl.LocalBinder.class));
        UiCallManager.init(this);
    }

}
