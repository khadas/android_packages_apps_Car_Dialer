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
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.apps.common.util.ViewUtils;
import com.android.car.dialer.R;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.common.DialerUtils;
import com.android.car.dialer.ui.view.ContactAvatarOutputlineProvider;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.PhoneNumber;
import com.android.car.telephony.common.TelecomUtils;

/** ViewHolder for {@link ContactDetailsFragment}. */
class ContactDetailsViewHolder extends RecyclerView.ViewHolder {
    // Applies to all
    @NonNull
    private final TextView mTitle;

    // Applies to header
    @Nullable
    private final ImageView mAvatar;
    @Nullable
    private final TextView mCallHeroButton;
    @Nullable
    private final TextView mTextHeroButton;

    // Applies to phone number items
    @Nullable
    private final TextView mText;
    @Nullable
    private final View mCallActionView;
    @Nullable
    private final View mSendTextActionView;

    ContactDetailsViewHolder(View v) {
        super(v);
        mCallActionView = v.findViewById(R.id.call_action_id);
        mCallHeroButton = v.findViewById(R.id.call_hero_button);
        mTextHeroButton = v.findViewById(R.id.text_hero_button);
        mSendTextActionView = v.findViewById(R.id.contact_details_text_button_icon);
        mTitle = v.findViewById(R.id.title);
        mText = v.findViewById(R.id.text);
        mAvatar = v.findViewById(R.id.avatar);
        if (mAvatar != null) {
            mAvatar.setOutlineProvider(ContactAvatarOutputlineProvider.get());
        }
    }

    public void bind(Context context, Contact contact) {
        TelecomUtils.setContactBitmapAsync(context, mAvatar, contact, null);

        if (contact == null) {
            mTitle.setText(R.string.error_contact_deleted);

            ViewUtils.setVisible(mCallHeroButton, false);
            ViewUtils.setVisible(mTextHeroButton, false);
            return;
        }

        mTitle.setText(contact.getDisplayName());

        final PhoneNumber primaryNumber = contact.getNumbers().size() == 1
                ? contact.getNumbers().get(0)
                : contact.getPrimaryPhoneNumber();

        if (primaryNumber != null) {
            CharSequence label = primaryNumber.getReadableLabel(context.getResources());

            String callButtonText = context.getString(
                    R.string.contact_details_call_number_button_with_label, label);
            String textButtonText = context.getString(
                    R.string.contact_details_text_number_button_with_label, label);

            setTextAndClickListener(mCallHeroButton, callButtonText,
                    v -> placeCall(primaryNumber));
            setTextAndClickListener(mTextHeroButton, textButtonText,
                    v -> sendText(context, primaryNumber));

        } else {
            setTextAndClickListener(mCallHeroButton, R.string.contact_details_call_number_button,
                    v -> DialerUtils.promptForPrimaryNumber(context, contact,
                            (phoneNumber, always) -> placeCall(phoneNumber)));

            setTextAndClickListener(mTextHeroButton, R.string.contact_details_text_number_button,
                    v -> DialerUtils.promptForPrimaryNumber(context, contact,
                            (phoneNumber, always) -> sendText(context, phoneNumber)));
        }

        ViewUtils.setVisible(mCallHeroButton, true);
        ViewUtils.setVisible(mTextHeroButton, true);
    }

    private void setTextAndClickListener(TextView view, String str, View.OnClickListener listener) {
        if (view != null) {
            view.setText(str);
            view.setOnClickListener(listener);
        }
    }

    private void setTextAndClickListener(TextView view, int strId, View.OnClickListener listener) {
        if (view != null) {
            setTextAndClickListener(view, view.getContext().getString(strId), listener);
        }
    }

    public void bind(Context context, PhoneNumber phoneNumber) {

        mTitle.setText(phoneNumber.getRawNumber());

        // Present the phone number type.
        CharSequence readableLabel = phoneNumber.getReadableLabel(context.getResources());
        if (phoneNumber.isPrimary()) {
            mText.setText(context.getString(R.string.primary_number_description, readableLabel));
        } else {
            mText.setText(readableLabel);
        }

        mCallActionView.setOnClickListener(v -> placeCall(phoneNumber));
        mSendTextActionView.setOnClickListener(v -> sendText(context, phoneNumber));
    }

    private void placeCall(PhoneNumber number) {
        UiCallManager.get().placeCall(number.getRawNumber());
    }

    private void sendText(Context context, PhoneNumber number) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("smsto:"));
        intent.setType("vnd.android-dir/mms-sms");
        intent.putExtra("address", number.getRawNumber());

        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context,
                    R.string.error_no_text_intent_handler,
                    Toast.LENGTH_LONG).show();
        }
    }
}
