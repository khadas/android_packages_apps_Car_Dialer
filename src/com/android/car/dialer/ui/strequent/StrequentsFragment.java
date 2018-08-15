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
package com.android.car.dialer.ui.strequent;

import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.car.widget.PagedListView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.dialer.ContactEntry;
import com.android.car.dialer.R;
import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.common.DialerBaseFragment;
import com.android.car.dialer.ui.common.entity.UiCallLog;

import java.util.List;

/**
 * Contains a list of contacts.
 */
public class StrequentsFragment extends DialerBaseFragment {
    private static final String TAG = "CD.StrequentsFrag";

    private static final String KEY_MAX_CLICKS = "max_clicks";
    private static final int DEFAULT_MAX_CLICKS = 6;

    public static StrequentsFragment newInstance() {
        return new StrequentsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        L.d(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.strequents_fragment, container, false);
        PagedListView listView = view.findViewById(R.id.list_view);
        int numOfColumn = getContext().getResources().getInteger(
                R.integer.favorite_fragment_grid_column);
        listView.getRecyclerView().setLayoutManager(
                new GridLayoutManager(getContext(), numOfColumn));
        listView.getRecyclerView().addItemDecoration(new ItemSpacingDecoration());
        listView.getRecyclerView().setItemAnimator(null);
        listView.setMaxPages(getMaxPages());

        StrequentsAdapter adapter = new StrequentsAdapter(getContext());
        adapter.setStrequentsListener(viewHolder -> {
            L.d(TAG, "onContactedClicked");
            UiCallManager.get().safePlaceCall((String) viewHolder.itemView.getTag(), false);
        });

        StrequentViewModel strequentViewModel = ViewModelProviders.of(this).get(
                StrequentViewModel.class);
        LiveData<List<ContactEntry>> strequentList = strequentViewModel.getStrequents();
        LiveData<UiCallLog> lastCall = strequentViewModel.getLastCall();
        adapter.setStrequentList(strequentList.getValue());
        adapter.setLastCall(lastCall.getValue());

        strequentList.observe(this, adapter::setStrequentList);
        lastCall.observe(this, adapter::setLastCall);

        listView.setAdapter(adapter);
        return view;
    }

    @Override
    protected Drawable getFullScreenBackgroundColor() {
        return new ColorDrawable(getContext().getColor(R.color.phone_theme_secondary));
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
        L.v(TAG, "Max clicks: " + maxClicks + ", Max pages: " + maxPages);
        return maxPages;
    }

    private class ItemSpacingDecoration extends RecyclerView.ItemDecoration {

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            int carPadding1 =
                    StrequentsFragment.this.getContext().getResources().getDimensionPixelOffset(
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
}
