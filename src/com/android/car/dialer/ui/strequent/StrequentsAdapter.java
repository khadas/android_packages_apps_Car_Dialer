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

package com.android.car.dialer.ui.strequent;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.car.widget.PagedListView;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.dialer.CallLogViewHolder;
import com.android.car.dialer.ContactEntry;
import com.android.car.dialer.R;
import com.android.car.dialer.entity.PhoneCallLog;
import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.TelecomUtils;
import com.android.car.dialer.ui.common.entity.UiCallLog;

import java.util.List;

/**
 * Adapter class for binding strequent and last call.
 */
public class StrequentsAdapter extends RecyclerView.Adapter<CallLogViewHolder>
        implements PagedListView.ItemCap {
    private static final String TAG = "CD.StrequentAdapter";

    // The possible view types in this adapter.
    private static final int VIEW_TYPE_LASTCALL = 1;
    private static final int VIEW_TYPE_STREQUENT = 2;
    /**
     * The max number of call records shown for a single combined log.
     */
    private static final int MAX_NUM_CALL_RECORDS = 3;

    private final Context mContext;
    private int mMaxItems = -1;
    private List<ContactEntry> mStrequentList;
    private UiCallLog mLastCall;
    private StrequentsListener<CallLogViewHolder> mStrequentsListener;

    public interface StrequentsListener<T> {
        /** Notified when a row corresponding an individual Contact (not group) was clicked. */
        void onContactClicked(T viewHolder);
    }

    public StrequentsAdapter(Context context) {
        mContext = context;
    }

    public void setStrequentsListener(@Nullable StrequentsListener<CallLogViewHolder> listener) {
        mStrequentsListener = listener;
    }

    /** Sets the last call. */
    public void setLastCall(@Nullable UiCallLog lastCall) {
        if (mLastCall != null) {
            notifyItemChanged(0);
        } else {
            notifyDataSetChanged();
        }
        mLastCall = lastCall;
    }

    /** Sets the strequent list. */
    public void setStrequentList(List<ContactEntry> strequentList) {
        mStrequentList = strequentList;
        notifyDataSetChanged();
    }

    @Override
    public void setMaxItems(int maxItems) {
        mMaxItems = maxItems;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0 && mLastCall != null) {
            return VIEW_TYPE_LASTCALL;
        } else {
            return VIEW_TYPE_STREQUENT;
        }
    }

    @Override
    public int getItemCount() {
        int itemCount = mStrequentList == null ? 0 : mStrequentList.size();
        itemCount += mLastCall == null ? 0 : 1;

        return mMaxItems >= 0 ? Math.min(mMaxItems, itemCount) : itemCount;
    }

    @Override
    public CallLogViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.call_log_list_item_card, parent, false);
        return new CallLogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final CallLogViewHolder viewHolder, int position) {
        switch (viewHolder.getItemViewType()) {
            case VIEW_TYPE_LASTCALL:
                onBindLastCallRow(viewHolder);
                break;
            case VIEW_TYPE_STREQUENT:
            default:
                int positionIntoData = position;

                // If there is last call data, then decrement the position so there is not an out of
                // bounds error on the mData.
                if (mLastCall != null) {
                    positionIntoData--;
                }

                onBindStrequentView(viewHolder, mStrequentList.get(positionIntoData));
                viewHolder.callType.setVisibility(View.VISIBLE);
        }
    }

    private void onViewClicked(CallLogViewHolder viewHolder) {
        if (mStrequentsListener != null) {
            mStrequentsListener.onContactClicked(viewHolder);
        }
    }

    @Override
    public void onViewDetachedFromWindow(CallLogViewHolder holder) {
        holder.itemView.setOnFocusChangeListener(null);
    }

    /**
     * Binds the views in the entry to the data of last call.
     *
     * @param viewHolder the view holder corresponding to this entry
     */
    private void onBindLastCallRow(final CallLogViewHolder viewHolder) {
        if (mLastCall == null) {
            return;
        }

        viewHolder.itemView.setOnClickListener(v -> onViewClicked(viewHolder));

        viewHolder.title.setText(mLastCall.getTitle());
        viewHolder.text.setText(mLastCall.getText());
        viewHolder.itemView.setTag(mLastCall.getNumber());
        viewHolder.smallIcon.setVisibility(View.GONE);
        viewHolder.callTypeIconsView.clear();
        viewHolder.callTypeIconsView.setVisibility(View.VISIBLE);
        List<PhoneCallLog.Record> records = mLastCall.getCallRecords(MAX_NUM_CALL_RECORDS);
        for (PhoneCallLog.Record record : records) {
            viewHolder.callTypeIconsView.add(record.getCallType());
        }

        TelecomUtils.setContactBitmapAsync(mContext, viewHolder.icon, mLastCall.getTitle(),
                mLastCall.getNumber());
    }

    /**
     * Bind view function for frequent call row.
     */
    private void onBindStrequentView(final CallLogViewHolder viewHolder, final ContactEntry entry) {
        viewHolder.itemView.setOnClickListener(v -> onViewClicked(viewHolder));

        final String number = entry.getNumber();
        String secondaryText = "";
        if (!entry.isVoicemail()) {
            secondaryText = String.valueOf(TelecomUtils.getTypeFromNumber(mContext, number));
        }

        viewHolder.text.setText(secondaryText);
        viewHolder.itemView.setTag(number);
        viewHolder.callTypeIconsView.clear();

        String displayName = entry.getDisplayName();
        viewHolder.title.setText(displayName);

        TelecomUtils.setContactBitmapAsync(mContext, viewHolder.icon, displayName, number);

        if (entry.isStarred()) {
            viewHolder.smallIcon.setVisibility(View.VISIBLE);
            final int iconColor = mContext.getColor(android.R.color.white);
            viewHolder.smallIcon.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
            viewHolder.smallIcon.setImageResource(R.drawable.ic_favorite);
        } else {
            viewHolder.smallIcon.setVisibility(View.GONE);
        }
    }
}
