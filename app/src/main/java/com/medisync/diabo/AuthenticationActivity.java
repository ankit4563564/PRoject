package com.medisync.diabo;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.medisync.diabo.databinding.ActivityAuthenticationBinding;

public class AuthenticationActivity extends AppCompatActivity {

    private ActivityAuthenticationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthenticationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Mock Auth: Success for any valid looking email
                navigateToMain();
            }
        });

        binding.btnGuest.setOnClickListener(v -> navigateToMain());

        binding.tvSwitchAuth.setOnClickListener(v -> {
            // For presentation, just toggle title
            if (binding.authTitle.getText().equals("Welcome Back")) {
                binding.authTitle.setText("Join MediSync");
                binding.btnLogin.setText("Sign Up");
                binding.tvSwitchAuth.setText("Already have an account? Login");
            } else {
                binding.authTitle.setText("Welcome Back");
                binding.btnLogin.setText("Login");
                binding.tvSwitchAuth.setText("Don't have an account? Sign Up");
            }
        });
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
