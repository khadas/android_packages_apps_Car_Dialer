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

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.telecom.Call;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.android.car.apps.common.BackgroundImageView;
import com.android.car.apps.common.LetterTileDrawable;
import com.android.car.dialer.R;
import com.android.car.dialer.log.L;
import com.android.car.dialer.ui.dialpad.DialpadFragment;
import com.android.car.telephony.common.CallDetail;
import com.android.car.telephony.common.TelecomUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

/**
 * A fragment that displays information about an on-going call with options to hang up.
 */
public class InCallFragment extends Fragment implements
        OnGoingCallControllerBarFragment.OnGoingCallControllerBarCallback {
    private static final String TAG = "CD.InCallFragment";
    private static final String TAG_CALL_RINGING = "CallStateRinging";
    private static final String TAG_CALL_OTHER = "CallStateOther";

    private Fragment mDialpadFragment;
    private View mUserProfileContainerView;
    private View mDialerFragmentContainer;
    private TextView mUserProfileBodyText;
    private BackgroundImageView mBackgroundImage;

    public static InCallFragment newInstance() {
        return new InCallFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.in_call_fragment, container, false);
        mUserProfileContainerView = fragmentView.findViewById(R.id.user_profile_container);
        mDialerFragmentContainer = fragmentView.findViewById(R.id.dialpad_container);
        mUserProfileBodyText = mUserProfileContainerView.findViewById(R.id.user_profile_body);
        mBackgroundImage = fragmentView.findViewById(R.id.background_image);
        mDialpadFragment = DialpadFragment.newInCallDialpad();

        InCallViewModel inCallViewModel = ViewModelProviders.of(getActivity()).get(
                InCallViewModel.class);

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
        mBackgroundImage.setDimmed(true);
    }

    @Override
    public void onCloseDialpad() {
        getChildFragmentManager().beginTransaction()
                .remove(mDialpadFragment)
                .commit();
        mDialerFragmentContainer.setVisibility(View.GONE);
        mUserProfileContainerView.setVisibility(View.VISIBLE);
        mBackgroundImage.setDimmed(false);
    }

    private void bindUserProfileView(@Nullable CallDetail callDetail) {
        L.i(TAG, "bindUserProfileView: %s", callDetail);
        if (callDetail == null) {
            return;
        }

        String number = callDetail.getNumber();
        Pair<String, Uri> displayNameAndAvatarUri = TelecomUtils.getDisplayNameAndAvatarUri(
                getContext(), number);

        TextView nameView = mUserProfileContainerView.findViewById(R.id.user_profile_title);
        nameView.setText(displayNameAndAvatarUri.first);

        TextView phoneNumberView
                = mUserProfileContainerView.findViewById(R.id.user_profile_phone_number);

        String phoneNumberLabel = TelecomUtils.getTypeFromNumber(getContext(), number).toString();
        if(!phoneNumberLabel.isEmpty()) {
            phoneNumberLabel += " ";
        }
        phoneNumberLabel += TelecomUtils.getFormattedNumber(getContext(), number);
        phoneNumberView.setText(phoneNumberLabel);

        ImageView avatar = mUserProfileContainerView.findViewById(R.id.user_profile_avatar);

        LetterTileDrawable letterTile = TelecomUtils.createLetterTile(
                getContext(),
                displayNameAndAvatarUri.first);

        Glide.with(getContext())
                .asBitmap()
                .load(displayNameAndAvatarUri.second)
                .apply(new RequestOptions().circleCrop().error(letterTile))
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource,
                            Transition<? super Bitmap> glideAnimation) {
                        // set showAnimation to false mostly because bindUserProfileView will be
                        // called several times, and we don't want the image to flicker
                        mBackgroundImage.setBackgroundImage(resource, false);
                        avatar.setImageBitmap(resource);
                    }

                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        mBackgroundImage.setBackgroundColor(letterTile.getColor());
                        avatar.setImageDrawable(letterTile);
                    }
                });
    }

    private void updateControllerBarFragment(@Nullable Integer callState) {
        L.i(TAG, "updateControllerBarFragment %s", callState);
        if (callState == null) {
            return;
        }

        if (callState == Call.STATE_RINGING) {
            Fragment controllerBarFragment = RingingCallControllerBarFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.controller_bar_container, controllerBarFragment, TAG_CALL_RINGING)
                    .commit();
            return;
        }
        Fragment controllerBarFragment = getChildFragmentManager().findFragmentByTag(
                TAG_CALL_OTHER);
        if (controllerBarFragment == null) {
            controllerBarFragment = OnGoingCallControllerBarFragment.newInstance(
                    callState.intValue());
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.controller_bar_container, controllerBarFragment, TAG_CALL_OTHER)
                    .commit();
        } else {
            ((OnGoingCallControllerBarFragment) controllerBarFragment).setCallState(
                    callState.intValue());
        }
    }

    private void updateBody(String text) {
        L.i(TAG, "updateBody: %s", text);
        mUserProfileBodyText.setText(text);
        mUserProfileBodyText.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
    }
}
