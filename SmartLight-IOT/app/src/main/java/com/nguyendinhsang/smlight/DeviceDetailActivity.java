package com.nguyendinhsang.smlight;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class DeviceDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);

        TextView tvName = findViewById(R.id.tvDeviceName);
        ImageView ivIcon = findViewById(R.id.ivDeviceIcon);

        // Nhận dữ liệu từ Intent
        Intent intent = getIntent();
        String name = intent.getStringExtra("device_name");
        int iconResId = intent.getIntExtra("device_icon", R.drawable.ic_device);

        tvName.setText(name);
        ivIcon.setImageResource(iconResId);
    }
}