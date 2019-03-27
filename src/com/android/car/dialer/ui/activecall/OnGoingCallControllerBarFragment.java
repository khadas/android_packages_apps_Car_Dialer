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

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.telecom.Call;
import android.telecom.CallAudioState;
import android.telecom.CallAudioState.CallAudioRoute;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.car.apps.common.FabDrawable;
import com.android.car.dialer.R;
import com.android.car.dialer.log.L;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.view.VerticalListDividerDecoration;

import java.util.List;

/**
 * A Fragment of the bar which controls on going call. Its host or parent Fragment is expected to
 * implement {@link OnGoingCallControllerBarCallback}.
 */
public class OnGoingCallControllerBarFragment extends Fragment {
    private static String TAG = "CDialer.OngoingCallCtlFrg";
    private static final String CALL_STATE = "CALL_STATE";

    private AlertDialog mAudioRouteSelectionDialog;
    private ImageView mAudioRouteButton;
    private LiveData<Call> mCallLiveData;
    private int mCallState;

    public static OnGoingCallControllerBarFragment newInstance() {
        return new OnGoingCallControllerBarFragment();
    }

    public static OnGoingCallControllerBarFragment newInstance(int callState) {
        OnGoingCallControllerBarFragment onGoingCallControllerBarFragment =
                new OnGoingCallControllerBarFragment();
        Bundle args = new Bundle();
        args.putInt(CALL_STATE, callState);
        onGoingCallControllerBarFragment.setArguments(args);
        return onGoingCallControllerBarFragment;
    }

    /**
     * Callback for control bar buttons.
     */
    public interface OnGoingCallControllerBarCallback {
        void onOpenDialpad();

        void onCloseDialpad();
    }

    private OnGoingCallControllerBarCallback mOnGoingCallControllerBarCallback;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getParentFragment() != null
                && getParentFragment() instanceof OnGoingCallControllerBarCallback) {
            mOnGoingCallControllerBarCallback =
                    (OnGoingCallControllerBarCallback) getParentFragment();
        } else if (getHost() instanceof OnGoingCallControllerBarCallback) {
            mOnGoingCallControllerBarCallback = (OnGoingCallControllerBarCallback) getHost();
        }

        View dialogView = LayoutInflater.from(getContext()).inflate(
                R.layout.audio_route_switch_dialog, null, false);
        RecyclerView list = dialogView.findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(getContext()));
        list.addItemDecoration(new VerticalListDividerDecoration(getContext(), true));

        List<Integer> availableRoutes = UiCallManager.get().getSupportedAudioRoute();

        mAudioRouteSelectionDialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        mAudioRouteSelectionDialog.getWindow().setBackgroundDrawableResource(
                android.R.color.transpare‌​nt);
        list.setAdapter(new AudioRouteListAdapter(getContext(), availableRoutes));

        InCallViewModel inCallViewModel = ViewModelProviders.of(getParentFragment()).get(
                InCallViewModel.class);
        mCallLiveData = inCallViewModel.getPrimaryCall();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.on_going_call_controller_bar_fragment,
                container, false);
        if (getArguments() != null) {
            mCallState = getArguments().getInt(CALL_STATE);
        }
        fragmentView.findViewById(R.id.mute_button).setOnClickListener((v) -> {
            if (mOnGoingCallControllerBarCallback == null) {
                return;
            }
            if (v.isActivated()) {
                v.setActivated(false);
                onUnmuteMic();
            } else {
                v.setActivated(true);
                onMuteMic();
            }
        });

        View dialPadButton = fragmentView.findViewById(R.id.toggle_dialpad_button);
        dialPadButton.setOnClickListener((v) -> {
            if (mOnGoingCallControllerBarCallback == null) {
                return;
            }
            if (v.isActivated()) {
                v.setActivated(false);
                mOnGoingCallControllerBarCallback.onCloseDialpad();
            } else {
                v.setActivated(true);
                mOnGoingCallControllerBarCallback.onOpenDialpad();
            }
        });
        dialPadButton.setActivated(false);
        mOnGoingCallControllerBarCallback.onCloseDialpad();

        ImageView endCallButton = fragmentView.findViewById(R.id.end_call_button);
        FabDrawable answerCallDrawable = new FabDrawable(getContext());
        answerCallDrawable.setFabAndStrokeColor(getContext().getColor(R.color.phone_end_call));
        endCallButton.setBackground(answerCallDrawable);
        endCallButton.setOnClickListener((v) -> {
            if (mOnGoingCallControllerBarCallback == null) {
                return;
            }
            onEndCall();
        });

        List<Integer> audioRoutes = UiCallManager.get().getSupportedAudioRoute();
        mAudioRouteButton = fragmentView.findViewById(R.id.voice_channel_button);
        if (audioRoutes.size() > 1) {
            fragmentView.findViewById(R.id.voice_channel_chevron).setVisibility(View.VISIBLE);
            mAudioRouteButton.setOnClickListener(
                    (v) -> mAudioRouteSelectionDialog.show());
        } else {
            fragmentView.findViewById(R.id.voice_channel_chevron).setVisibility(View.GONE);
        }

        ImageView pauseButton = fragmentView.findViewById(R.id.pause_button);
        pauseButton.setOnClickListener((v) -> {
            if (mOnGoingCallControllerBarCallback == null) {
                return;
            }

            if (mCallState == Call.STATE_ACTIVE) {
                onHoldCall();
            } else if (mCallState == Call.STATE_HOLDING) {
                onUnholdCall();
            } else {
                L.i(TAG, "Pause button is clicked while call in %s state", mCallState);
            }
        });
        setButtonEnabled(pauseButton);

        return fragmentView;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mAudioRouteSelectionDialog.isShowing()) {
            mAudioRouteSelectionDialog.dismiss();
        }
    }

    /**
     * Set the call state and change the view for the pause button accordingly
     */
    public void setCallState(int callState) {
        L.d(TAG, "Call State: %s", callState);
        mCallState = callState;
        ImageView pauseButton = getView().findViewById(R.id.pause_button);
        setButtonEnabled(pauseButton);
    }

    private void onMuteMic() {
        UiCallManager.get().setMuted(true);
    }

    private void onUnmuteMic() {
        UiCallManager.get().setMuted(false);
    }

    private void onHoldCall() {
        if (mCallLiveData.getValue() != null) {
            mCallLiveData.getValue().hold();
        }
    }

    private void onUnholdCall() {
        if (mCallLiveData.getValue() != null) {
            mCallLiveData.getValue().unhold();
        }
    }

    private void onVoiceOutputChannelChanged(@CallAudioRoute int audioRoute) {
        UiCallManager.get().setAudioRoute(audioRoute);
        mAudioRouteSelectionDialog.dismiss();
        mAudioRouteButton.setImageResource(getAudioRouteIconRes(audioRoute));
    }

    private void onEndCall() {
        if (mCallLiveData.getValue() != null) {
            mCallLiveData.getValue().disconnect();
        }
    }

    private int getAudioRouteIconRes(@CallAudioRoute int audioRoute) {
        switch (audioRoute) {
            case CallAudioState.ROUTE_WIRED_HEADSET:
            case CallAudioState.ROUTE_EARPIECE:
                return R.drawable.ic_smartphone;
            case CallAudioState.ROUTE_BLUETOOTH:
                return R.drawable.ic_bluetooth;
            case CallAudioState.ROUTE_SPEAKER:
                return R.drawable.ic_speaker_phone;
            default:
                L.w(TAG, "Unknown audio route: %s", audioRoute);
                return -1;
        }
    }

    private int getAudioRouteLabelRes(@CallAudioRoute int audioRoute) {
        switch (audioRoute) {
            case CallAudioState.ROUTE_WIRED_HEADSET:
            case CallAudioState.ROUTE_EARPIECE:
                return R.string.audio_route_handset;
            case CallAudioState.ROUTE_BLUETOOTH:
                return R.string.audio_route_vehicle;
            case CallAudioState.ROUTE_SPEAKER:
                return R.string.audio_route_phone_speaker;
            default:
                L.w(TAG, "Unknown audio route: %s", audioRoute);
                return -1;
        }
    }

    private class AudioRouteListAdapter extends
            RecyclerView.Adapter<AudioRouteItemViewHolder> {
        private List<Integer> mSupportedRoutes;
        private Context mContext;

        public AudioRouteListAdapter(Context context, List<Integer> supportedRoutes) {
            mSupportedRoutes = supportedRoutes;
            mContext = context;
            if (mSupportedRoutes.contains(CallAudioState.ROUTE_EARPIECE)
                    && mSupportedRoutes.contains(CallAudioState.ROUTE_WIRED_HEADSET)) {
                // Keep either ROUTE_EARPIECE or ROUTE_WIRED_HEADSET, but not both of them.
                mSupportedRoutes.remove(CallAudioState.ROUTE_WIRED_HEADSET);
            }
        }

        @Override
        public AudioRouteItemViewHolder onCreateViewHolder(ViewGroup container, int position) {
            View listItemView = LayoutInflater.from(mContext).inflate(
                    R.layout.audio_route_list_item, container, false);
            return new AudioRouteItemViewHolder(listItemView);
        }

        @Override
        public void onBindViewHolder(AudioRouteItemViewHolder viewHolder, int position) {
            int audioRoute = mSupportedRoutes.get(position);
            viewHolder.mBody.setText(mContext.getString(getAudioRouteLabelRes(audioRoute)));
            viewHolder.mIcon.setImageResource(getAudioRouteIconRes(audioRoute));
            viewHolder.itemView.setOnClickListener((v) -> onVoiceOutputChannelChanged(audioRoute));
        }

        @Override
        public int getItemCount() {
            return mSupportedRoutes.size();
        }
    }

    private static class AudioRouteItemViewHolder extends RecyclerView.ViewHolder {
        public final ImageView mIcon;
        public final TextView mBody;

        public AudioRouteItemViewHolder(View itemView) {
            super(itemView);
            mIcon = itemView.findViewById(R.id.icon);
            mBody = itemView.findViewById(R.id.body);
        }
    }

    private void setButtonEnabled(View button) {
        if (mCallState == Call.STATE_HOLDING) {
            button.setEnabled(true);
            button.setActivated(true);
        } else if (mCallState == Call.STATE_ACTIVE) {
            button.setEnabled(true);
            button.setActivated(false);
        } else {
            button.setEnabled(false);
        }
    }
}
