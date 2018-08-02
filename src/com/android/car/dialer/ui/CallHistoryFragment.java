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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.widget.ListItemAdapter;
import androidx.car.widget.PagedListView;
import androidx.lifecycle.ViewModelProviders;

import com.android.car.dialer.R;
import com.android.car.dialer.ui.common.DialerBaseFragment;
import com.android.car.dialer.ui.viewmodel.CallHistoryViewModel;

public class CallHistoryFragment extends DialerBaseFragment {
    public static CallHistoryFragment newInstance() {
        return new CallHistoryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.call_list_fragment, container, false);
        PagedListView pagedListView = fragmentView.findViewById(R.id.list_view);
        CallHistoryListItemProvider callHistoryListItemProvider = new CallHistoryListItemProvider();
        ListItemAdapter adapter = new ListItemAdapter(getContext(), callHistoryListItemProvider);
        pagedListView.setAdapter(adapter);

        CallHistoryViewModel viewModel = ViewModelProviders.of(this).get(
                CallHistoryViewModel.class);

        viewModel.getCallHistory().observe(this,
                callHistoryItems -> {
                    callHistoryListItemProvider.setCallHistoryListItems(getContext(),
                            callHistoryItems);
                    adapter.notifyDataSetChanged();
                });

        return fragmentView;
    }
}
