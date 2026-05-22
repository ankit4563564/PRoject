package com.medisync.diabo.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.medisync.diabo.R;
import com.medisync.diabo.databinding.ActivityAuthenticationBinding;

public class AuthenticationFragment extends Fragment {

    private ActivityAuthenticationBinding binding;
    private boolean isSignUp = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = ActivityAuthenticationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bindInputState();
        applyTapScale(binding.btnLogin);
        applyTapScale(binding.btnGuest);
        applyTapScale(binding.tabLogin);
        applyTapScale(binding.tabSignup);
        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            binding.btnLogin.setEnabled(false);
            binding.btnGuest.setEnabled(false);
            binding.pbLoading.setVisibility(View.VISIBLE);

            // Save user type
            requireContext().getSharedPreferences("diabo_prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_guest", false)
                    .apply();

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && binding != null) {
                    try {
                        NavController navController = Navigation.findNavController(view);
                        if (navController.getCurrentDestination() != null
                                && navController.getCurrentDestination().getId() == R.id.authenticationFragment) {
                            navController.navigate(R.id.action_auth_to_setup);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }, 1500);
        });
        binding.btnGuest.setOnClickListener(v -> {
            binding.btnLogin.setEnabled(false);
            binding.btnGuest.setEnabled(false);
            binding.pbLoading.setVisibility(View.VISIBLE);

            // Save user type
            requireContext().getSharedPreferences("diabo_prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("is_guest", true)
                    .apply();

            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded() && binding != null) {
                    try {
                        NavController navController = Navigation.findNavController(view);
                        if (navController.getCurrentDestination() != null
                                && navController.getCurrentDestination().getId() == R.id.authenticationFragment) {
                            navController.navigate(R.id.action_auth_to_setup);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }, 1500);
        });
        binding.tvSwitchAuth.setOnClickListener(v -> {
            setAuthMode(!isSignUp);
        });

        binding.tabLogin.setOnClickListener(v -> setAuthMode(false));
        binding.tabSignup.setOnClickListener(v -> setAuthMode(true));
        setAuthMode(false);
    }

    private void bindInputState() {
        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updatePrimaryButtonState();
            }
        };
        binding.etEmail.addTextChangedListener(watcher);
        binding.etPassword.addTextChangedListener(watcher);
    }

    private void setAuthMode(boolean signUpMode) {
        isSignUp = signUpMode;
        if (signUpMode) {
            binding.authTitle.setText("Welcome Back");
            binding.btnLogin.setText("Create Account");
            binding.tvSwitchAuth.setText("By creating account, you agree to our Terms");
            binding.tabLogin.setBackground(null);
            binding.tabSignup.setBackgroundResource(R.drawable.button_gradient);
            binding.tabLogin.setTextColor(requireContext().getColor(R.color.graySubtext));
            binding.tabSignup.setTextColor(requireContext().getColor(R.color.white));
        } else {
            binding.authTitle.setText("Welcome Back");
            binding.btnLogin.setText("Log In");
            binding.tvSwitchAuth.setText("By creating account, you agree to our Terms");
            binding.tabLogin.setBackgroundResource(R.drawable.button_gradient);
            binding.tabSignup.setBackgroundResource(R.drawable.edittext_bg);
            binding.tabLogin.setTextColor(requireContext().getColor(R.color.white));
            binding.tabSignup.setTextColor(requireContext().getColor(R.color.graySubtext));
        }
        updatePrimaryButtonState();
    }

    private void updatePrimaryButtonState() {
        String email = binding.etEmail.getText() == null ? "" : binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText() == null ? "" : binding.etPassword.getText().toString().trim();
        boolean enabled = !email.isEmpty() && password.length() >= 3;
        binding.btnLogin.setEnabled(enabled);
        binding.btnLogin.animate().alpha(enabled ? 1f : 0.55f).setDuration(160).start();
    }

    private void applyTapScale(View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
                v.animate().scaleX(0.98f).scaleY(0.98f).setDuration(70).start();
            } else if (event.getAction() == android.view.MotionEvent.ACTION_UP || event.getAction() == android.view.MotionEvent.ACTION_CANCEL) {
                v.animate().scaleX(1f).scaleY(1f).setDuration(90).start();
            }
            return false;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
