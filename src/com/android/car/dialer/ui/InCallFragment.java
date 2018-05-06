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
package com.android.car.dialer.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.car.dialer.BitmapWorkerTask;
import com.android.car.dialer.DialerFragment;
import com.android.car.dialer.R;
import com.android.car.dialer.telecom.TelecomUtils;
import com.android.car.dialer.telecom.UiCall;
import com.android.car.dialer.telecom.UiCallManager;

/**
 * A fragment that displays information about an on-going call with options to hang up.
 */
public class InCallFragment extends Fragment implements
        OnGoingCallControllerBarFragment.OnGoingCallControllerBarCallback {

    private Fragment mDialerFragment;
    private View mUserProfileContainerView;
    private View mDialerFragmentContainer;

    public static InCallFragment newInstance() {
        return new InCallFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.in_call_fragment, container, false);
        mUserProfileContainerView = fragmentView.findViewById(R.id.user_profile_container);
        mDialerFragmentContainer = fragmentView.findViewById(R.id.dialer_container);
        mDialerFragment = new DialerFragment();

        Fragment onGoingCallControllerBarFragment = OnGoingCallControllerBarFragment.newInstance();

        getChildFragmentManager().beginTransaction()
                .replace(R.id.controller_bar_container, onGoingCallControllerBarFragment)
                .commit();
        bindUserProfileView(fragmentView.findViewById(R.id.user_profile_container));
        return fragmentView;
    }

    @Override
    public void onOpenDialpad() {
        mDialerFragment = new DialerFragment();
        getChildFragmentManager().beginTransaction()
                .replace(R.id.dialer_container, mDialerFragment)
                .commit();
        mDialerFragmentContainer.setVisibility(View.VISIBLE);
        mUserProfileContainerView.setVisibility(View.GONE);
    }

    @Override
    public void onCloseDialpad() {
        getFragmentManager().beginTransaction()
                .remove(mDialerFragment)
                .commit();
        mDialerFragmentContainer.setVisibility(View.GONE);
        mUserProfileContainerView.setVisibility(View.VISIBLE);
    }

    private void bindUserProfileView(View container) {
        UiCall primaryCall = UiCallManager.get().getPrimaryCall();
        if (primaryCall == null) {
            return;
        }
        String number = primaryCall.getNumber();
        ImageView avatar = container.findViewById(R.id.avatar);
        BitmapWorkerTask.BitmapRunnable runnable = new BitmapWorkerTask.BitmapRunnable() {
            @Override
            public void run() {
                if (mBitmap != null) {
                    Resources r = getResources();
                    avatar.setImageDrawable(new CircleBitmapDrawable(r, mBitmap));
                    avatar.setImageBitmap(mBitmap);
                    avatar.clearColorFilter();
                } else {
                    avatar.setImageResource(R.drawable.logo_avatar);
                    avatar.setImageResource(R.drawable.ic_avatar_bg);
                }
            }
        };
        BitmapWorkerTask.loadBitmap(getContext().getContentResolver(), avatar, number, runnable);
        TextView nameView = container.findViewById(R.id.title);
        nameView.setText(TelecomUtils.getDisplayName(getContext(), primaryCall));
    }
}
