package com.android.car.dialer.ui.activecall;

import android.os.Bundle;
import android.telecom.Call;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.android.car.dialer.R;

public class RingingCallControllerBarFragment extends Fragment {

    private Call mActiveCall;

    public static RingingCallControllerBarFragment newInstance() {
        return new RingingCallControllerBarFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.ringing_call_controller_bar_fragment,
                container, false);

        fragmentView.findViewById(R.id.answer_call_button).setOnClickListener((v) -> answerCall());
        fragmentView.findViewById(R.id.answer_call_text).setOnClickListener((v) -> answerCall());
        fragmentView.findViewById(R.id.end_call_button).setOnClickListener((v) -> declineCall());
        fragmentView.findViewById(R.id.end_call_text).setOnClickListener((v) -> declineCall());

        InCallViewModel inCallViewModel = ViewModelProviders.of(getParentFragment()).get(
                InCallViewModel.class);
        mActiveCall = inCallViewModel.getPrimaryCall().getValue();
        return fragmentView;
    }

    private void answerCall() {
        if (mActiveCall != null) {
            mActiveCall.answer(/* videoState= */0);
        }
    }

    private void declineCall() {
        if (mActiveCall != null) {
            mActiveCall.reject(/* rejectWithMessage= */false, /* textMessage= */null);
        }
    }
}
