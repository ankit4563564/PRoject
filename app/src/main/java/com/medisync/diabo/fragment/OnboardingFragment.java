package com.medisync.diabo.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import com.medisync.diabo.R;
import com.medisync.diabo.databinding.FragmentOnboardingBinding;
import com.medisync.diabo.db.AppDatabase;
import com.medisync.diabo.model.UserProfile;

public class OnboardingFragment extends Fragment {

    private FragmentOnboardingBinding binding;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private UserProfile savedProfile;
    private int slideIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentOnboardingBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        AppDatabase db = AppDatabase.getInstance(requireContext());
        db.appDao().getUserProfile().observe(getViewLifecycleOwner(), userProfile -> savedProfile = userProfile);

        // Initial states
        binding.iconContainer.setScaleX(0.5f);
        binding.iconContainer.setScaleY(0.5f);
        binding.iconContainer.setAlpha(0f);
        binding.btnContinue.setVisibility(View.INVISIBLE);

        setupFeatures();
        
        // Hide features initially
        binding.feature1.getRoot().setVisibility(View.INVISIBLE);
        binding.feature2.getRoot().setVisibility(View.INVISIBLE);
        binding.feature3.getRoot().setVisibility(View.INVISIBLE);
        binding.feature4.getRoot().setVisibility(View.INVISIBLE);

        startAnimations();

        binding.btnContinue.setOnClickListener(v -> {
            if (slideIndex < 2) {
                slideIndex++;
                applySlide();
                startAnimations();
                return;
            }
            NavController navController = Navigation.findNavController(v);
            try {
                if (navController.getCurrentDestination() == null
                        || navController.getCurrentDestination().getId() != R.id.onboardingFragment) {
                    return;
                }
                if (savedProfile != null) {
                    navController.navigate(R.id.action_onboarding_to_main);
                } else {
                    navController.navigate(R.id.action_onboarding_to_auth);
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void setupFeatures() {
        applySlide();
    }

    private void applySlide() {
        if (binding == null) return;
        if (slideIndex == 0) {
            binding.title.setText("Your diabetes care, organized from day one");
            binding.badge.setText("WELCOME");
            binding.btnContinue.setText("Next");
        } else if (slideIndex == 1) {
            binding.title.setText("Upload reports and understand what changed");
            binding.badge.setText("REPORTS + TIMELINE");
            binding.btnContinue.setText("Next");
        } else {
            binding.title.setText("Chat, log, and share with more confidence");
            binding.badge.setText("DAILY CARE");
            binding.btnContinue.setText("Get Started");
        }

        binding.feature1.featureTitle.setText("Start with a clean health overview");
        binding.feature2.featureTitle.setText("Upload reports and extract key values");
        binding.feature3.featureTitle.setText("Chat with your diabetes assistant");
        binding.feature4.featureTitle.setText("Share PDF reports with care teams");
        if (slideIndex == 1) {
            binding.feature1.featureTitle.setText("Analyze labs and medications from reports");
            binding.feature2.featureTitle.setText("Review organ-wise health timeline");
            binding.feature3.featureTitle.setText("Open report history and details");
            binding.feature4.featureTitle.setText("Generate PDF summaries when needed");
        } else if (slideIndex == 2) {
            binding.feature1.featureTitle.setText("Log glucose manually");
            binding.feature2.featureTitle.setText("Set medication reminders");
            binding.feature3.featureTitle.setText("Keep chat history on device");
            binding.feature4.featureTitle.setText("Edit profile and manage privacy");
        }
    }

    private void startAnimations() {
        if (binding == null) return;
        // Icon animation
        binding.iconContainer.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(1.0f)
                .setDuration(520)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Staggered feature animations
        handler.postDelayed(() -> { if (binding != null) animateFeatureRow(binding.feature1.getRoot()); }, 260);
        handler.postDelayed(() -> { if (binding != null) animateFeatureRow(binding.feature2.getRoot()); }, 500);
        handler.postDelayed(() -> { if (binding != null) animateFeatureRow(binding.feature3.getRoot()); }, 740);
        handler.postDelayed(() -> { if (binding != null) animateFeatureRow(binding.feature4.getRoot()); }, 980);

        // Continue button animation
        handler.postDelayed(() -> {
            if (binding == null) return;
            binding.btnContinue.setVisibility(View.VISIBLE);
            AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
            TranslateAnimation translate = new TranslateAnimation(0, 0, 40, 0);
            alpha.setDuration(420);
            translate.setDuration(420);
            binding.btnContinue.startAnimation(alpha);
            binding.btnContinue.startAnimation(translate);
            binding.btnContinue.animate().scaleX(1.03f).scaleY(1.03f).setDuration(200).withEndAction(() -> {
                if (binding != null) binding.btnContinue.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
            }).start();
        }, 1280);
    }

    private void animateFeatureRow(View row) {
        row.setVisibility(View.VISIBLE);
        AlphaAnimation alpha = new AlphaAnimation(0f, 1f);
        TranslateAnimation translate = new TranslateAnimation(-40, 0, 0, 0);
        alpha.setDuration(360);
        translate.setDuration(360);
        row.startAnimation(alpha);
        row.startAnimation(translate);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        handler.removeCallbacksAndMessages(null);
    }
}
