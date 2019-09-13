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

package com.android.car.dialer.ui.contact;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.apps.common.BackgroundImageView;
import com.android.car.apps.common.LetterTileDrawable;
import com.android.car.apps.common.util.ViewUtils;
import com.android.car.dialer.R;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.view.ContactAvatarOutputlineProvider;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.PhoneNumber;
import com.android.car.telephony.common.TelecomUtils;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;

/** ViewHolder for {@link ContactDetailsFragment}. */
class ContactDetailsViewHolder extends RecyclerView.ViewHolder {
    // Applies to all
    @NonNull
    private final TextView mTitle;

    // Applies to header
    @Nullable
    private final ImageView mAvatarView;
    @Nullable
    private final BackgroundImageView mBackgroundImageView;

    // Applies to phone number items
    @Nullable
    private final TextView mText;
    @Nullable
    private final View mCallActionView;
    @Nullable
    private final View mFavoriteActionView;

    @NonNull
    private final ContactDetailsAdapter.PhoneNumberPresenter mPhoneNumberPresenter;

    ContactDetailsViewHolder(
            View v,
            @NonNull ContactDetailsAdapter.PhoneNumberPresenter phoneNumberPresenter) {
        super(v);
        mCallActionView = v.findViewById(R.id.call_action_id);
        mFavoriteActionView = v.findViewById(R.id.contact_details_favorite_button);
        mTitle = v.findViewById(R.id.title);
        mText = v.findViewById(R.id.text);
        mAvatarView = v.findViewById(R.id.avatar);
        if (mAvatarView != null) {
            mAvatarView.setOutlineProvider(ContactAvatarOutputlineProvider.get());
        }
        mBackgroundImageView = v.findViewById(R.id.background_image);

        mPhoneNumberPresenter = phoneNumberPresenter;
    }

    public void bind(Context context, Contact contact) {
        if (contact == null) {
            ViewUtils.setText(mTitle, R.string.error_contact_deleted);
            LetterTileDrawable letterTile = TelecomUtils.createLetterTile(context, null);
            if (mAvatarView != null) {
                mAvatarView.setImageDrawable(letterTile);
            }
            if (mBackgroundImageView != null) {
                mBackgroundImageView.setAlpha(context.getResources().getFloat(
                        R.dimen.config_background_image_error_alpha));
                mBackgroundImageView.setBackgroundColor(letterTile.getColor());
            }
            return;
        }

        ViewUtils.setText(mTitle, contact.getDisplayName());

        if (mAvatarView == null && mBackgroundImageView == null) {
            return;
        }

        LetterTileDrawable letterTile = TelecomUtils.createLetterTile(context,
                contact.getDisplayName());
        Glide.with(context)
                .load(contact.getAvatarUri())
                .apply(new RequestOptions().centerCrop().error(letterTile))
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(Drawable resource,
                            Transition<? super Drawable> glideAnimation) {
                        if (mAvatarView != null) {
                            mAvatarView.setImageDrawable(resource);
                        }
                        if (mBackgroundImageView != null) {
                            mBackgroundImageView.setAlpha(context.getResources().getFloat(
                                    R.dimen.config_background_image_alpha));
                            mBackgroundImageView.setBackgroundDrawable(resource);
                        }
                    }

                    @Override
                    public void onLoadFailed(Drawable errorDrawable) {
                        if (mAvatarView != null) {
                            mAvatarView.setImageDrawable(letterTile);
                        }
                        if (mBackgroundImageView != null) {
                            mBackgroundImageView.setAlpha(context.getResources().getFloat(
                                    R.dimen.config_background_image_error_alpha));
                            mBackgroundImageView.setBackgroundColor(letterTile.getColor());
                        }
                    }
                });
    }

    public void bind(Context context, Contact contact, PhoneNumber phoneNumber) {

        mTitle.setText(phoneNumber.getRawNumber());

        // Present the phone number type.
        CharSequence readableLabel = phoneNumber.getReadableLabel(context.getResources());
        if (phoneNumber.isPrimary()) {
            mText.setText(context.getString(R.string.primary_number_description, readableLabel));
            mText.setTextAppearance(R.style.TextAppearance_DefaultNumberLabel);
        } else {
            mText.setText(readableLabel);
            mText.setTextAppearance(R.style.TextAppearance_ContactDetailsListSubtitle);
        }

        mCallActionView.setOnClickListener(
                v -> UiCallManager.get().placeCall(phoneNumber.getRawNumber()));
        mFavoriteActionView.setActivated(phoneNumber.isFavorite());
        mFavoriteActionView.setOnClickListener(v -> {
            mPhoneNumberPresenter.onClick(contact, phoneNumber);
            mFavoriteActionView.setActivated(!mFavoriteActionView.isActivated());
        });
    }
}
