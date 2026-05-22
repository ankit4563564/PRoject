package com.medisync.diabo.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medisync.diabo.R;
import com.medisync.diabo.databinding.ItemCalendarDayBinding;
import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.ViewHolder> {

    private final List<Day> days;
    private int selectedPosition = 0;

    public static class Day {
        public String number;
        public String name;
        public boolean hasData;

        public Day(String number, String name, boolean hasData) {
            this.number = number;
            this.name = name;
            this.hasData = hasData;
        }
    }

    public CalendarAdapter(List<Day> days) {
        this.days = days;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCalendarDayBinding binding = ItemCalendarDayBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Day day = days.get(position);
        holder.binding.tvDayNumber.setText(day.number);
        holder.binding.tvDayName.setText(day.name);
        holder.binding.dotIndicator.setVisibility(day.hasData ? android.view.View.VISIBLE : android.view.View.INVISIBLE);

        if (selectedPosition == position) {
            holder.binding.dayContainer.setBackgroundResource(R.drawable.button_gradient);
            holder.binding.tvDayNumber.setTextColor(android.graphics.Color.WHITE);
            holder.binding.tvDayName.setTextColor(android.graphics.Color.WHITE);
        } else {
            holder.binding.dayContainer.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            holder.binding.tvDayNumber.setTextColor(android.graphics.Color.BLACK);
            holder.binding.tvDayName.setTextColor(android.graphics.Color.GRAY);
        }

        holder.itemView.setOnClickListener(v -> {
            int previous = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previous);
            notifyItemChanged(selectedPosition);
        });
    }

    @Override
    public int getItemCount() {
        return days.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemCalendarDayBinding binding;
        public ViewHolder(ItemCalendarDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
