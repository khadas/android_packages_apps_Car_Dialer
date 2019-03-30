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

package com.android.car.dialer.ui.favorite;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.dialer.R;
import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.common.DialerBaseFragment;
import com.android.car.dialer.ui.common.PhoneNumberListAdapter;
import com.android.car.telephony.common.Contact;
import com.android.car.telephony.common.PhoneNumber;
import com.android.car.telephony.common.TelecomUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of favorite contacts.
 */
public class FavoriteFragment extends DialerBaseFragment {
    private static final String TAG = "CD.FavoriteFrag";

    public static FavoriteFragment newInstance() {
        return new FavoriteFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        L.d(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.favorite_fragment, container, false);
        RecyclerView listView = view.findViewById(R.id.list_view);
        int numOfColumn = getContext().getResources().getInteger(
                R.integer.favorite_fragment_grid_column);
        listView.setLayoutManager(
                new GridLayoutManager(getContext(), numOfColumn));
        listView.addItemDecoration(new ItemSpacingDecoration());
        listView.setItemAnimator(null);

        FavoriteAdapter adapter = new FavoriteAdapter();

        FavoriteViewModel favoriteViewModel = ViewModelProviders.of(this).get(
                FavoriteViewModel.class);
        LiveData<List<Contact>> favoriteContacts = favoriteViewModel.getFavoriteContacts();
        adapter.setOnListItemClickedListener(this::onItemClicked);
        favoriteContacts.observe(this, adapter::setFavoriteContacts);

        listView.setAdapter(adapter);
        return view;
    }

    private void onItemClicked(Contact contact) {
        Context context = getContext();

        if (contact.hasPrimaryPhoneNumber()) {
            placeCall(contact.getPrimaryPhoneNumber(), false);
            return;
        }

        List<PhoneNumber> contactPhoneNumbers = contact.getNumbers();
        if (contactPhoneNumbers.isEmpty()) {
            L.w(TAG, "contact %s doesn't have any phone number", contact.getDisplayName());
            return;
        }

        if (contactPhoneNumbers.size() == 1) {
            placeCall(contactPhoneNumbers.get(0), false);
        } else if (contactPhoneNumbers.size() > 1) {
            final List<PhoneNumber> selectedPhoneNumber = new ArrayList<>();
            new AlertDialog.Builder(context)
                    .setTitle(R.string.select_number_dialog_title)
                    .setSingleChoiceItems(
                            new PhoneNumberListAdapter(context, contactPhoneNumbers),
                            -1,
                            ((dialog, which) -> {
                                selectedPhoneNumber.clear();
                                selectedPhoneNumber.add(contactPhoneNumbers.get(which));
                            }))
                    .setNeutralButton(R.string.select_number_dialog_just_once_button,
                            (dialog, which) -> {
                                if (!selectedPhoneNumber.isEmpty()) {
                                    placeCall(selectedPhoneNumber.get(0), false);
                                }
                            })
                    .setPositiveButton(R.string.select_number_dialog_always_button,
                            (dialog, which) -> {
                                if (!selectedPhoneNumber.isEmpty()) {
                                    placeCall(selectedPhoneNumber.get(0), true);
                                }
                            })
                    .show();
        }
    }

    private void placeCall(PhoneNumber number, boolean setAsPrimary) {
        UiCallManager.get().placeCall(number.getRawNumber());
        if (setAsPrimary) {
            TelecomUtils.setAsPrimaryPhoneNumber(getContext(), number);
        }
    }

    private class ItemSpacingDecoration extends RecyclerView.ItemDecoration {

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int carPadding1 =
                    FavoriteFragment.this.getContext().getResources().getDimensionPixelOffset(
                            R.dimen.favorite_card_space);

            int leftPadding = 0;
            int rightPadding = 0;
            if (parent.getChildAdapterPosition(view) % 2 == 0) {
                rightPadding = carPadding1;
            } else {
                leftPadding = carPadding1;
            }

            outRect.set(leftPadding, carPadding1, rightPadding, carPadding1);
        }
    }
}
