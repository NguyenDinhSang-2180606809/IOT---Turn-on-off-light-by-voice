package com.nguyendinhsang.smlight;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    private static final int REQUEST_ADD_ROOM = 1001;
    private static final String PREFS_NAME = "room_prefs";
    private static final String KEY_ROOMS = "room_list";

    private List<String> roomList = new ArrayList<>();
    private RoomAdapter roomAdapter;
    private List<DeviceModel> deviceList = new ArrayList<>();
    private DeviceAdapter deviceAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadRoomList(); // Load từ SharedPreferences
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // RecyclerView danh sách phòng
        RecyclerView roomRecycler = view.findViewById(R.id.roomRecyclerView);
        roomRecycler.setLayoutManager(new GridLayoutManager(getContext(), 3));
        roomAdapter = new RoomAdapter(roomList);
        roomRecycler.setAdapter(roomAdapter);

        // Nút Thêm phòng
        ImageView btnAddRoom = view.findViewById(R.id.btnAddRoom);
        btnAddRoom.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddRoomActivity.class);
            startActivityForResult(intent, REQUEST_ADD_ROOM);
        });

        // Nút Thêm thiết bị
        loadDeviceList();

        RecyclerView deviceRecycler = view.findViewById(R.id.deviceRecyclerView);
        deviceRecycler.setLayoutManager(new GridLayoutManager(getContext(), 2));
        deviceAdapter = new DeviceAdapter(
                deviceList,
                device -> {
                    // Chuyển sang LedControlActivity (dùng topic cố định)
                    Intent intent = new Intent(getActivity(), LedControlActivity.class);
                    intent.putExtra("device_name", device.getName());
                    intent.putExtra("device_icon", device.getIconResId());
                    startActivity(intent);
                },
                position -> {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Xoá thiết bị")
                            .setMessage("Bạn có chắc muốn xoá thiết bị này?")
                            .setPositiveButton("Xoá", (dialog, which) -> {
                                deviceList.remove(position);
                                deviceAdapter.notifyItemRemoved(position);
                                saveDeviceList();
                            })
                            .setNegativeButton("Huỷ", null)
                            .show();
                }
        );

        deviceRecycler.setAdapter(deviceAdapter);

        View.OnClickListener onClickAddDevice = v -> {
            Intent intent = new Intent(getActivity(), ProvisionDevice.class);
            startActivity(intent);
        };

        ImageView btnAddDevice = view.findViewById(R.id.btnAddDevice);
        btnAddDevice.setOnClickListener(onClickAddDevice);

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ADD_ROOM && resultCode == Activity.RESULT_OK) {
            String newRoom = data.getStringExtra("room_name");
            if (newRoom != null && !newRoom.trim().isEmpty()) {
                roomList.add(newRoom);
                roomAdapter.notifyItemInserted(roomList.size() - 1);
                saveRoomList();
            }
        }
    }

    private void saveRoomList() {
        SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Set<String> set = new HashSet<>(roomList);
        editor.putStringSet(KEY_ROOMS, set);
        editor.apply();
    }

    private void loadRoomList() {
        SharedPreferences prefs = getActivity() != null ?
                getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) : null;
        if (prefs != null) {
            Set<String> set = prefs.getStringSet(KEY_ROOMS, null);
            if (set != null) {
                roomList.clear();
                roomList.addAll(set);
            } else {
                roomList.add("Phòng khách");
                roomList.add("Phòng bếp");
                roomList.add("Phòng ngủ");
            }
        }
    }

    private void saveDeviceList() {
        SharedPreferences prefs = getActivity().getSharedPreferences("device_prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = gson.toJson(deviceList);
        editor.putString("device_list", json);
        editor.apply();
    }

    private void loadDeviceList() {
        SharedPreferences prefs = getActivity().getSharedPreferences("device_prefs", Context.MODE_PRIVATE);
        String json = prefs.getString("device_list", "[]");
        Type type = new TypeToken<List<DeviceModel>>(){}.getType();
        List<DeviceModel> loadedList = new Gson().fromJson(json, type);

        deviceList.clear();
        if (loadedList != null) {
            deviceList.addAll(loadedList);
        }
    }
}