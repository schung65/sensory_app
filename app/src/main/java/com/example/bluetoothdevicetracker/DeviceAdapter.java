package com.example.bluetoothdevicetracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {

    private List<String> deviceList;

    public DeviceAdapter(List<String> deviceList) {
        this.deviceList = deviceList;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        String deviceInfo = deviceList.get(position);
        holder.deviceTextView.setText(deviceInfo);
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceTextView;

        public DeviceViewHolder(View itemView) {
            super(itemView);
            deviceTextView = itemView.findViewById(R.id.deviceName);
        }
    }
}

