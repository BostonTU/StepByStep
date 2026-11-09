package com.example.stepbystep;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

/**
 * RecyclerView Adapter for displaying Activity history items.
 */
public class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ActivityItem item);
    }

    public interface OnItemDeleteListener {
        void onItemDelete(ActivityItem item, int position);
    }

    private List<ActivityItem> activityList;
    private OnItemClickListener clickListener;
    private OnItemDeleteListener deleteListener;

    public ActivityAdapter(List<ActivityItem> activityList, OnItemClickListener clickListener, OnItemDeleteListener deleteListener) {
        this.activityList = activityList;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    public void updateData(List<ActivityItem> newList) {
        this.activityList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ActivityItem item = activityList.get(position);

        holder.tvDateTime.setText(item.getDateTime());
        holder.tvSteps.setText(String.format(Locale.getDefault(), "%d ก้าว", item.getSteps()));
        holder.tvShakes.setText(String.format(Locale.getDefault(), "%d ครั้ง", item.getShakes()));
        holder.tvDuration.setText(item.getFormattedDuration());

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onItemClick(item);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) {
                int pos = holder.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) {
                    deleteListener.onItemDelete(item, pos);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return activityList != null ? activityList.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDateTime, tvSteps, tvShakes, tvDuration;
        ImageView btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tvItemDateTime);
            tvSteps = itemView.findViewById(R.id.tvItemSteps);
            tvShakes = itemView.findViewById(R.id.tvItemShakes);
            tvDuration = itemView.findViewById(R.id.tvItemDuration);
            btnDelete = itemView.findViewById(R.id.btnItemDelete);
        }
    }
}
