package com.medisync.diabo.fragment;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.medisync.diabo.R;

public class GuestLimitDialog extends DialogFragment {

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, android.R.style.Theme_Translucent_NoTitleBar);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_guest_limit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnSignUp = view.findViewById(R.id.btn_sign_up);
        Button btnSignIn = view.findViewById(R.id.btn_sign_in);
        TextView tvContinue = view.findViewById(R.id.tv_continue_limited);

        if (btnSignUp != null) {
            btnSignUp.setOnClickListener(v -> {
                dismiss();
                // Navigate to authentication
                try {
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    navController.navigate(R.id.authenticationFragment);
                } catch (Exception e) {
                    // Fallback if navigation fails
                }
            });
        }

        if (btnSignIn != null) {
            btnSignIn.setOnClickListener(v -> {
                dismiss();
                try {
                    NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                    navController.navigate(R.id.authenticationFragment);
                } catch (Exception e) {
                    // Fallback if navigation fails
                }
            });
        }

        if (tvContinue != null) {
            tvContinue.setOnClickListener(v -> dismiss());
        }
    }
}
