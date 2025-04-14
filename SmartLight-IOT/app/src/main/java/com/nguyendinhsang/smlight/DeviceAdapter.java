package com.nguyendinhsang.smlight;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private final List<DeviceModel> deviceList;

    public interface OnDeviceLongClickListener {
        void onDeviceLongClick(int position);
    }

    private final OnDeviceLongClickListener longClickListener;
    private final OnDeviceClickListener clickListener;

    public interface OnDeviceClickListener {
        void onDeviceClick(DeviceModel device);
    }

    public DeviceAdapter(List<DeviceModel> deviceList,
                         OnDeviceClickListener clickListener,
                         OnDeviceLongClickListener longClickListener) {
        this.deviceList = deviceList;
        this.clickListener = clickListener;
        this.longClickListener = longClickListener;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        DeviceModel device = deviceList.get(position);
        holder.tvDeviceName.setText(device.getName());
        holder.imgDeviceIcon.setImageResource(device.getIconResId());

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onDeviceLongClick(position);
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onDeviceClick(deviceList.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView tvDeviceName;
        ImageView imgDeviceIcon;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            imgDeviceIcon = itemView.findViewById(R.id.imgDeviceIcon);
        }
    }
}
