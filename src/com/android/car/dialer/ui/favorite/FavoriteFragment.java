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

import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.dialer.R;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.common.DialerListBaseFragment;
import com.android.car.dialer.ui.common.DialerUtils;
import com.android.car.telephony.common.Contact;

import java.util.List;

/** Contains a list of favorite contacts. */
public class FavoriteFragment extends DialerListBaseFragment {
    private static final String TAG = "CD.FavoriteFrag";

    public static FavoriteFragment newInstance() {
        return new FavoriteFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        getRecyclerView().addItemDecoration(new ItemSpacingDecoration());
        getRecyclerView().setItemAnimator(null);

        FavoriteAdapter adapter = new FavoriteAdapter();

        FavoriteViewModel favoriteViewModel = ViewModelProviders.of(this).get(
                FavoriteViewModel.class);
        LiveData<List<Contact>> favoriteContacts = favoriteViewModel.getFavoriteContacts();
        adapter.setOnListItemClickedListener(this::onItemClicked);
        favoriteContacts.observe(this, adapter::setFavoriteContacts);

        getRecyclerView().setAdapter(adapter);
    }

    @NonNull
    @Override
    protected RecyclerView.LayoutManager createLayoutManager() {
        int numOfColumn = getContext().getResources().getInteger(
                R.integer.favorite_fragment_grid_column);
        return new GridLayoutManager(getContext(), numOfColumn);
    }

    private void onItemClicked(Contact contact) {
        DialerUtils.promptForPrimaryNumber(getContext(), contact, (phoneNumber, always) ->
                UiCallManager.get().placeCall(phoneNumber.getRawNumber()));
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
