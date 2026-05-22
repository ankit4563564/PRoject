package com.medisync.diabo.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medisync.diabo.databinding.ItemGoalPillBinding;
import java.util.List;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.ViewHolder> {

    private final List<Goal> goals;

    public static class Goal {
        public String title;
        public String value;
        public String color;

        public Goal(String title, String value, String color) {
            this.title = title;
            this.value = value;
            this.color = color;
        }
    }

    public GoalAdapter(List<Goal> goals) {
        this.goals = goals;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemGoalPillBinding binding = ItemGoalPillBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Goal goal = goals.get(position);
        holder.binding.tvGoalTitle.setText(goal.title);
        holder.binding.tvGoalValue.setText(goal.value);
        // Color mapping omitted for brevity, using default
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemGoalPillBinding binding;
        public ViewHolder(ItemGoalPillBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
