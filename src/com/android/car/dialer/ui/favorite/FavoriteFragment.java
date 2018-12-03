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

import android.annotation.Nullable;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.car.widget.PagedListView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.dialer.R;
import com.android.car.dialer.entity.Contact;
import com.android.car.dialer.entity.PhoneNumber;
import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.TelecomUtils;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.common.DialerBaseFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains a list of favorite contacts.
 */
public class FavoriteFragment extends DialerBaseFragment {
    private static final String TAG = "CD.FavoriteFrag";

    private static final String KEY_MAX_CLICKS = "max_clicks";
    private static final int DEFAULT_MAX_CLICKS = 6;

    public static FavoriteFragment newInstance() {
        return new FavoriteFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        L.d(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.favorite_fragment, container, false);
        PagedListView listView = view.findViewById(R.id.list_view);
        int numOfColumn = getContext().getResources().getInteger(
                R.integer.favorite_fragment_grid_column);
        listView.getRecyclerView().setLayoutManager(
                new GridLayoutManager(getContext(), numOfColumn));
        listView.getRecyclerView().addItemDecoration(new ItemSpacingDecoration());
        listView.getRecyclerView().setItemAnimator(null);
        listView.setMaxPages(getMaxPages());

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
        UiCallManager.get().placeCall(number.getNumber());
        if (setAsPrimary) {
            TelecomUtils.setAsPrimaryPhoneNumber(getContext(), number);
        }
    }

    private int getMaxPages() {
        // Maximum number of forward acting clicks the user can perform
        Bundle args = getArguments();
        int maxClicks = args == null
                ? DEFAULT_MAX_CLICKS
                : args.getInt(KEY_MAX_CLICKS, DEFAULT_MAX_CLICKS);
        // We want to show one fewer page than max clicks to allow clicking on an item,
        // but, the first page is "free" since it doesn't take any clicks to show
        final int maxPages = maxClicks < 0 ? -1 : maxClicks;
        L.v(TAG, "Max clicks: %s, Max pages: %s", maxClicks, maxPages);
        return maxPages;
    }

    private class ItemSpacingDecoration extends RecyclerView.ItemDecoration {

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int carPadding1 =
                    FavoriteFragment.this.getContext().getResources().getDimensionPixelOffset(
                            R.dimen.car_padding_1);

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

    private static class PhoneNumberListAdapter extends ArrayAdapter<PhoneNumber> {
        private final Context mContext;

        public PhoneNumberListAdapter(Context context, List<PhoneNumber> phoneNumbers) {
            super(context, R.layout.phone_number_list_item, R.id.phone_number, phoneNumbers);
            mContext = context;
        }

        @Override
        public View getView(int position, @Nullable View convertView,
                @android.annotation.NonNull ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            PhoneNumber phoneNumber = getItem(position);
            if (phoneNumber == null) {
                return view;
            }
            TextView phoneNumberView = view.findViewById(R.id.phone_number);
            phoneNumberView.setText(phoneNumber.getNumber());
            TextView phoneNumberDescriptionView = view.findViewById(R.id.phone_number_description);
            phoneNumberDescriptionView.setText(
                    phoneNumber.getReadableLabel(mContext.getResources()));
            return view;
        }
    }

    @StringRes
    @Override
    protected int getActionBarTitleRes() {
        return R.string.favorites_title;
    }
}
