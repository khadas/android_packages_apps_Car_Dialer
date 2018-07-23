package com.android.car.dialer.ui.activecall;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.car.dialer.R;
import com.android.car.dialer.telecom.UiCall;
import com.android.car.dialer.telecom.UiCallManager;
import com.android.car.dialer.ui.common.DialerBaseFragment;

public class RingingCallControllerBarFragment extends DialerBaseFragment {

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

        return fragmentView;
    }

    private void answerCall() {
        UiCallManager uiCallManager = UiCallManager.get();
        UiCall primaryCall = uiCallManager.getPrimaryCall();
        uiCallManager.answerCall(primaryCall);
    }

    private void declineCall() {
        UiCallManager uiCallManager = UiCallManager.get();
        UiCall primaryCall = uiCallManager.getPrimaryCall();
        uiCallManager.rejectCall(primaryCall, false, null);
    }
}
