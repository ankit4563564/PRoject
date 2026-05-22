package com.medisync.diabo.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.medisync.diabo.R;
import com.medisync.diabo.ReportUploadActivity;
import com.medisync.diabo.databinding.FragmentMainBinding;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initial setup: load DashboardFragment
        getChildFragmentManager().beginTransaction()
                .replace(R.id.main_content_container, new DashboardFragment())
                .commit();
        updateFabVisibility(R.id.nav_dashboard);

        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_dashboard) {
                selectedFragment = new DashboardFragment();
            } else if (itemId == R.id.nav_chat) {
                selectedFragment = new ChatFragment();
            } else if (itemId == R.id.nav_timeline) {
                selectedFragment = new TimelineFragment();
            } else if (itemId == R.id.nav_doctor) {
                selectedFragment = new DoctorFragment();
            }

            if (selectedFragment != null) {
                getChildFragmentManager().beginTransaction()
                        .setCustomAnimations(R.anim.fade_in_tab, R.anim.fade_out_tab, R.anim.fade_in_tab, R.anim.fade_out_tab)
                        .replace(R.id.main_content_container, selectedFragment)
                        .commit();
                updateFabVisibility(itemId);
                return true;
            }
            return false;
        });

        binding.fabScan.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), ReportUploadActivity.class);
            startActivity(intent);
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }

    private void updateFabVisibility(int itemId) {
        // Hide scan FAB on chat so it doesn't cover the send button.
        if (itemId == R.id.nav_chat) {
            binding.fabScan.setVisibility(View.GONE);
        } else {
            binding.fabScan.setVisibility(View.VISIBLE);
        }
    }

    public void selectTab(int itemId) {
        if (binding != null && binding.bottomNavigation != null) {
            binding.bottomNavigation.setSelectedItemId(itemId);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
