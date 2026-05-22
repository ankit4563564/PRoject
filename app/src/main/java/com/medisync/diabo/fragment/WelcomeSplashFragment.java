package com.medisync.diabo.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.medisync.diabo.R;

public class WelcomeSplashFragment extends Fragment {

    private final Handler handler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        View container = view.findViewById(R.id.welcome_container);
        View allSet = view.findViewById(R.id.tv_all_set);
        View welcome = view.findViewById(R.id.tv_welcome);
        container.setAlpha(0f);
        container.setScaleX(0.97f);
        container.setScaleY(0.97f);
        container.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(420)
                .start();
        allSet.setAlpha(0f);
        welcome.setAlpha(0f);
        allSet.animate().alpha(1f).setDuration(300).setStartDelay(120).start();
        welcome.animate().alpha(1f).setDuration(320).setStartDelay(260).start();
        handler.postDelayed(() -> {
            container.animate().alpha(0f).setDuration(240).withEndAction(() ->
                    {
                        if (!isAdded()) return;
                        try {
                            androidx.navigation.NavController navController = Navigation.findNavController(view);
                            if (navController.getCurrentDestination() != null
                                    && navController.getCurrentDestination().getId() == R.id.welcomeSplashFragment) {
                                navController.navigate(R.id.action_welcome_to_main);
                            }
                        } catch (Exception ignored) {
                        }
                    }).start();
        }, 1200);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}
