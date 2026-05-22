package com.medisync.diabo.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.medisync.diabo.databinding.ItemVitalBentoBinding;
import java.util.List;

public class VitalAdapter extends RecyclerView.Adapter<VitalAdapter.ViewHolder> {

    private final List<Vital> vitals;

    public static class Vital {
        public String name;
        public String value;
        public String status;
        public int iconResId;

        public Vital(String name, String value, String status, int iconResId) {
            this.name = name;
            this.value = value;
            this.status = status;
            this.iconResId = iconResId;
        }
    }

    public VitalAdapter(List<Vital> vitals) {
        this.vitals = vitals;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemVitalBentoBinding binding = ItemVitalBentoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Vital vital = vitals.get(position);
        holder.binding.tvVitalName.setText(vital.name);
        holder.binding.tvVitalValue.setText(vital.value);
        holder.binding.tvVitalStatus.setText(vital.status);
        holder.binding.ivVitalIcon.setImageResource(vital.iconResId);
    }

    @Override
    public int getItemCount() {
        return vitals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemVitalBentoBinding binding;
        public ViewHolder(ItemVitalBentoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
