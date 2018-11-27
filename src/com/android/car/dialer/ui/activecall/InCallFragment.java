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

package com.android.car.dialer.ui.activecall;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telecom.Call;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.android.car.dialer.R;
import com.android.car.dialer.entity.CallDetail;
import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.TelecomUtils;
import com.android.car.dialer.ui.common.DialerBaseFragment;
import com.android.car.dialer.ui.dialpad.DialpadFragment;

/**
 * A fragment that displays information about an on-going call with options to hang up.
 */
public class InCallFragment extends DialerBaseFragment implements
        OnGoingCallControllerBarFragment.OnGoingCallControllerBarCallback {
    private static final String TAG = "CD.InCallFragment";

    private Fragment mDialpadFragment;
    private View mUserProfileContainerView;
    private View mDialerFragmentContainer;
    private TextView mUserProfileBodyText;

    public static InCallFragment newInstance() {
        return new InCallFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.in_call_fragment, container, false);
        mUserProfileContainerView = fragmentView.findViewById(R.id.user_profile_container);
        mDialerFragmentContainer = fragmentView.findViewById(R.id.dialpad_container);
        mUserProfileBodyText = mUserProfileContainerView.findViewById(R.id.body);
        mDialpadFragment = DialpadFragment.newInCallDialpad();

        InCallViewModel inCallViewModel = ViewModelProviders.of(this).get(InCallViewModel.class);

        inCallViewModel.getPrimaryCallDetail().observe(this, this::bindUserProfileView);
        inCallViewModel.getPrimaryCallState().observe(this, this::updateControllerBarFragment);
        inCallViewModel.getCallStateDescription().observe(this, this::updateBody);
        return fragmentView;
    }

    @Override
    public void onOpenDialpad() {
        getChildFragmentManager().beginTransaction()
                .replace(R.id.dialpad_container, mDialpadFragment)
                .commit();
        mDialerFragmentContainer.setVisibility(View.VISIBLE);
        mUserProfileContainerView.setVisibility(View.GONE);
    }

    @Override
    public void onCloseDialpad() {
        getChildFragmentManager().beginTransaction()
                .remove(mDialpadFragment)
                .commit();
        mDialerFragmentContainer.setVisibility(View.GONE);
        mUserProfileContainerView.setVisibility(View.VISIBLE);
    }

    private void bindUserProfileView(@Nullable CallDetail callDetail) {
        L.i(TAG, "bindUserProfileView: %s", callDetail);
        if (callDetail == null) {
            return;
        }

        String number = callDetail.getNumber();
        String displayName = TelecomUtils.getDisplayName(getContext(), number);

        TextView nameView = mUserProfileContainerView.findViewById(R.id.title);
        nameView.setText(displayName);

        ImageView avatar = mUserProfileContainerView.findViewById(R.id.avatar);
        TelecomUtils.setContactBitmapAsync(getContext(), avatar, displayName, number);
    }

    private void updateControllerBarFragment(@Nullable Integer callState) {
        L.i(TAG, "updateControllerBarFragment %s", callState);
        if (callState == null) {
            return;
        }

        Fragment controllerBarFragment;
        if (callState == Call.STATE_RINGING) {
            controllerBarFragment = RingingCallControllerBarFragment.newInstance();
        } else {
            controllerBarFragment = OnGoingCallControllerBarFragment.newInstance();
        }

        getChildFragmentManager().beginTransaction()
                .replace(R.id.controller_bar_container, controllerBarFragment)
                .commit();
    }

    private void updateBody(String text) {
        L.i(TAG, "updateBody: %s", text);
        mUserProfileBodyText.setText(text);
        mUserProfileBodyText.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }

    @StringRes
    @Override
    protected int getActionBarTitleRes() {
        return R.string.in_call_title;
    }
}
