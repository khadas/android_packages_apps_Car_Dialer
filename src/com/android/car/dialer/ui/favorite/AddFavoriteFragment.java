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

package com.android.car.dialer.ui.favorite;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Toast;

import com.android.car.dialer.R;
import com.android.car.dialer.livedata.ContactDetailsLiveData;
import com.android.car.dialer.ui.common.FavoritePhoneNumberListAdapter;
import com.android.car.dialer.ui.search.ContactResultsFragment;
import com.android.car.telephony.common.PhoneNumber;

import java.util.HashSet;
import java.util.Set;

/** A fragment that allows the user to search for and select favorite phone numbers */
public class AddFavoriteFragment extends ContactResultsFragment {

    /** Creates a new instance of AddFavoriteFragment */
    public static AddFavoriteFragment newInstance() {
        return new AddFavoriteFragment();
    }

    private ContactDetailsLiveData mContactDetails;
    private AlertDialog mCurrentDialog;
    private FavoritePhoneNumberListAdapter mDialogAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDialogAdapter = new FavoritePhoneNumberListAdapter(getContext());

        Set<PhoneNumber> selectedNumbers = new HashSet<>();
        mCurrentDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.select_number_dialog_title)
                .setAdapter(mDialogAdapter,
                        null)
                .setNegativeButton(R.string.cancel_add_favorites_dialog,
                        null)
                .setPositiveButton(R.string.confirm_add_favorites_dialog,
                        (d, which) -> {
                            String result = "Not yet implemented, but you picked ";
                            for (PhoneNumber number : selectedNumbers) {
                                result += number.getNumber() + ", ";
                            }
                            Toast.makeText(getContext(), result, Toast.LENGTH_LONG).show();
                            getFragmentManager().popBackStackImmediate();
                        })
                .create();

        mCurrentDialog.getListView().setItemsCanFocus(false);
        mCurrentDialog.getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mCurrentDialog.getListView().setOnItemClickListener((parent, view2, position, id) -> {
            PhoneNumber number = mDialogAdapter.getItem(position);
            if (view2.isActivated()) {
                selectedNumbers.add(number);
            } else {
                selectedNumbers.remove(number);
            }
        });
    }

    @Override
    public void onShowContactDetail(Uri contactLookupUri) {
        if (mContactDetails != null) {
            mContactDetails.removeObservers(this);
        }

        mContactDetails = new ContactDetailsLiveData(getContext(), contactLookupUri);
        mContactDetails.observe(this, (contact) -> {
            if (contact == null) {
                mCurrentDialog.dismiss();
                return;
            }

            mDialogAdapter.setPhoneNumbers(contact.getNumbers());
            mCurrentDialog.show();
        });
    }
}
