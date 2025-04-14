package com.nguyendinhsang.smlight;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.budiyev.android.codescanner.CodeScanner;
import com.budiyev.android.codescanner.CodeScannerView;
import com.espressif.provisioning.ESPConstants;
import com.espressif.provisioning.ESPDevice;
import com.espressif.provisioning.ESPProvisionManager;
import com.espressif.provisioning.WiFiAccessPoint;
import com.espressif.provisioning.listeners.BleScanListener;
import com.espressif.provisioning.listeners.ProvisionListener;
import com.espressif.provisioning.listeners.QRCodeScanListener;
import com.espressif.provisioning.listeners.WiFiScanListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ProvisionDevice extends AppCompatActivity {

    private static final String TAG = "ProvisionActivity";
    private static final int REQUEST_BLUETOOTH_PERMISSION = 103;

    private ESPProvisionManager provisionManager;
    private ESPDevice espDevice;
    private CodeScanner codeScanner;
    private CodeScannerView scannerView;

    private Button btnQrScan, btnManualSetup, btnScanNetworks, btnProvision;
    private RadioButton rbBLE, rbSoftAP;
    private EditText etDeviceName, etPop, etSSID, etPassword;
    private ListView listWifiNetworks;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private boolean deviceFound = false;
    private ArrayList<String> wifiNetworks = new ArrayList<>();
    private ArrayAdapter<String> wifiAdapter;
    private String selectedSSID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provision);

        initViews();
        setupListeners();

        provisionManager = ESPProvisionManager.getInstance(getApplicationContext());
        checkPermissions();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Provision Device");
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void initViews() {
        scannerView = findViewById(R.id.scanner_view);
        btnQrScan = findViewById(R.id.btn_qr_scan);
        btnManualSetup = findViewById(R.id.btn_manual_setup);
        btnScanNetworks = findViewById(R.id.btn_scan_networks);
        btnProvision = findViewById(R.id.btn_provision);
        rbBLE = findViewById(R.id.rb_ble);
        rbSoftAP = findViewById(R.id.rb_softap);
        etDeviceName = findViewById(R.id.et_device_name);
        etPop = findViewById(R.id.et_pop);
        etSSID = findViewById(R.id.et_ssid);
        etPassword = findViewById(R.id.et_password);
        listWifiNetworks = findViewById(R.id.list_wifi_networks);
        progressBar = findViewById(R.id.progress_bar);
        tvStatus = findViewById(R.id.tv_status);

        wifiAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiNetworks);
        listWifiNetworks.setAdapter(wifiAdapter);

        codeScanner = new CodeScanner(this, scannerView);
    }

    private void setupListeners() {
        btnQrScan.setOnClickListener(v -> startQrCodeScan());

        btnManualSetup.setOnClickListener(v -> setupManually());

        btnScanNetworks.setOnClickListener(v -> {
            if (espDevice != null) {
                scanWifiNetworks();
            } else {
                showToast("Device not connected. Please connect first.");
            }
        });

        btnProvision.setOnClickListener(v -> {
            if (espDevice != null) {
                String ssid = etSSID.getText().toString();
                String password = etPassword.getText().toString();

                if (ssid.isEmpty()) {
                    showToast("Please enter or select a WiFi SSID");
                    return;
                }

                provisionDevice(ssid, password);
            } else {
                showToast("Device not connected. Please connect first.");
            }
        });

        listWifiNetworks.setOnItemClickListener((parent, view, position, id) -> {
            selectedSSID = wifiNetworks.get(position);
            etSSID.setText(selectedSSID);
        });
    }

    private void checkPermissions() {
        List<String> permissions = new ArrayList<>();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }

        if (!permissions.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), REQUEST_BLUETOOTH_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSION) {
            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    Log.e(TAG, "Quyền " + permissions[i] + " bị từ chối");
                }
            }
            if (allGranted) {
                Log.d(TAG, "Tất cả quyền đã được cấp");
            } else {
                showToast("Cần quyền Bluetooth để kết nối thiết bị");
            }
        }
    }

    private void startQrCodeScan() {
        scannerView.setVisibility(View.VISIBLE);
        updateStatus("Scanning QR Code...");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            checkPermissions();
            return;
        }

        provisionManager.scanQRCode(codeScanner, new QRCodeScanListener() {
            @Override
            public void qrCodeScanned() {}

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    scannerView.setVisibility(View.GONE);
                    updateStatus("QR Code scanning failed: " + e.getMessage());
                });
            }

            @Override
            public void onFailure(Exception e, String strErr) {
                runOnUiThread(() -> {
                    scannerView.setVisibility(View.GONE);
                    updateStatus("QR Code scanning failed: " + e.getMessage());
                });
            }

            @Override
            public void deviceDetected(ESPDevice device) {
                runOnUiThread(() -> {
                    scannerView.setVisibility(View.GONE);
                    espDevice = device;
                    updateStatus("QR Code scanned successfully. Device: " + device.getDeviceName());
                    if (device.getTransportType() == ESPConstants.TransportType.TRANSPORT_BLE) {
                        showDeviceConnectDialog();
                    } else if (device.getTransportType() == ESPConstants.TransportType.TRANSPORT_SOFTAP) {
                        connectToSoftApDevice();
                    }
                });
            }
        });
    }

    private void showDeviceConnectDialog() {
        deviceFound = false;
        progressBar.setVisibility(View.VISIBLE);
        updateStatus("Searching for BLE devices...");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
            checkPermissions();
            return;
        }

        provisionManager.searchBleEspDevices("PROV_", new BleScanListener() {
            @Override
            public void scanStartFailed() {
                runOnUiThread(() -> {
                    showToast("BLE scan thất bại");
                    progressBar.setVisibility(View.GONE);
                });
            }

            @Override
            public void onPeripheralFound(BluetoothDevice device, ScanResult scanResult) {
                runOnUiThread(() -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            ActivityCompat.checkSelfPermission(ProvisionDevice.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        checkPermissions();
                        return;
                    }
                    String deviceName = device.getName();
                    if (deviceName != null && deviceName.equals(espDevice.getDeviceName())) {
                        deviceFound = true;
                        progressBar.setVisibility(View.GONE);
                        List<ParcelUuid> uuids = scanResult.getScanRecord().getServiceUuids();
                        String serviceUuid = (uuids != null && !uuids.isEmpty()) ? uuids.get(0).toString() : null;
                        connectToBleDevice(device, serviceUuid);
                        Log.d(TAG, "Device Found: " + deviceName + ", MAC: " + device.getAddress());
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("BLE scan failed: " + e.getMessage());
                });
            }

            @Override
            public void scanCompleted() {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    Log.d(TAG, "Scan completed, deviceFound: " + deviceFound);
                    if (!deviceFound) {
                        updateStatus("Device not found");
                        showAlertDialog("Device not found", "Would you like to manually select a device?",
                                (dialog, which) -> showBleDeviceListDialog(),
                                (dialog, which) -> dialog.dismiss());
                    } else {
                        updateStatus("Device found and connected");
                    }
                });
            }
        });
    }

    private void connectToBleDevice(BluetoothDevice device, String serviceUuid) {
        progressBar.setVisibility(View.VISIBLE);
        updateStatus("Connecting to BLE device...");

        try {
            espDevice.connectBLEDevice(device, serviceUuid);
            String pop = etPop.getText().toString();
            if (!pop.isEmpty()) {
                espDevice.setProofOfPossession(pop);
            }
            updateStatus("Connected to device: " + espDevice.getDeviceName());
            progressBar.setVisibility(View.GONE);
            showToast("Kết nối BLE thành công!");
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            updateStatus("Failed to connect: " + e.getMessage());
            showToast("Lỗi kết nối: " + e.getMessage());
        }
    }

    private void connectToSoftApDevice() {
        progressBar.setVisibility(View.VISIBLE);
        updateStatus("Connecting to SoftAP device...");

        try {
            espDevice.connectWiFiDevice();
            String pop = etPop.getText().toString();
            if (!pop.isEmpty()) {
                espDevice.setProofOfPossession(pop);
            }
            updateStatus("Connected to device: " + espDevice.getDeviceName());
            progressBar.setVisibility(View.GONE);
        } catch (Exception e) {
            progressBar.setVisibility(View.GONE);
            updateStatus("Failed to connect: " + e.getMessage());
        }
    }

    private void setupManually() {
        ESPConstants.TransportType transportType = rbBLE.isChecked()
                ? ESPConstants.TransportType.TRANSPORT_BLE
                : ESPConstants.TransportType.TRANSPORT_SOFTAP;

        ESPConstants.SecurityType securityType = ESPConstants.SecurityType.SECURITY_2;

        espDevice = provisionManager.createESPDevice(transportType, securityType);

        String deviceName = etDeviceName.getText().toString();
        String pop = etPop.getText().toString();

        if (!pop.isEmpty()) {
            espDevice.setProofOfPossession(pop);
        }

        if (transportType == ESPConstants.TransportType.TRANSPORT_BLE) {
            showDeviceConnectDialog();
        } else {
            connectToSoftApDevice();
        }
    }

    private void scanWifiNetworks() {
        progressBar.setVisibility(View.VISIBLE);
        updateStatus("Scanning WiFi networks...");
        wifiNetworks.clear();

        espDevice.scanNetworks(new WiFiScanListener() {
            @Override
            public void onWifiListReceived(ArrayList<WiFiAccessPoint> wifiList) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    wifiNetworks.clear();
                    for (WiFiAccessPoint ap : wifiList) {
                        wifiNetworks.add(ap.getWifiName());
                    }
                    wifiAdapter.notifyDataSetChanged();
                    updateStatus("WiFi scan completed. Found " + wifiList.size() + " networks.");
                });
            }

            @Override
            public void onWiFiScanFailed(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("WiFi scan failed: " + e.getMessage());
                });
            }
        });
    }

    private void provisionDevice(String ssid, String password) {
        progressBar.setVisibility(View.VISIBLE);
        updateStatus("Provisioning device...");

        espDevice.provision(ssid, password, new ProvisionListener() {
            @Override
            public void createSessionFailed(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("Failed to create session: " + e.getMessage());
                });
            }

            @Override
            public void wifiConfigSent() {
                runOnUiThread(() -> {
                    updateStatus("WiFi configuration sent to device");
                });
            }

            @Override
            public void wifiConfigFailed(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("WiFi configuration failed: " + e.getMessage());
                });
            }

            @Override
            public void wifiConfigApplied() {
                runOnUiThread(() -> {
                    updateStatus("WiFi configuration applied");
                });
            }

            @Override
            public void wifiConfigApplyFailed(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("Failed to apply WiFi configuration: " + e.getMessage());
                });
            }

            @Override
            public void provisioningFailedFromDevice(ESPConstants.ProvisionFailureReason failureReason) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("Provisioning failed: " + failureReason);
                });
            }

            @Override
            public void deviceProvisioningSuccess() {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("Device provisioned successfully!");
                    showAddDeviceDialog(); // Hiển thị dialog để thêm thiết bị
                });
            }

            @Override
            public void onProvisioningFailed(Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    updateStatus("Provisioning failed: " + e.getMessage());
                });
            }
        });
    }

    private void showAddDeviceDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_device, null);
        EditText etDeviceNameDialog = dialogView.findViewById(R.id.etDeviceName);
        RadioGroup iconGroup = dialogView.findViewById(R.id.iconRadioGroup);
        Spinner spinnerRoom = dialogView.findViewById(R.id.spinnerRoom);

        ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, loadRoomList());
        spinnerRoom.setAdapter(roomAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Thêm thiết bị")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = etDeviceNameDialog.getText().toString().trim();
                    int iconResId = R.drawable.ic_device; // Default icon

                    int checkedId = iconGroup.getCheckedRadioButtonId();
                    if (checkedId == R.id.rbLight) {
                        iconResId = R.drawable.ic_light;
                    } else if (checkedId == R.id.rbFan) {
                        iconResId = R.drawable.ic_fan;
                    } else if (checkedId == R.id.rbSensor) {
                        iconResId = R.drawable.ic_sensor;
                    }

                    if (!name.isEmpty()) {
                        String selectedRoom = spinnerRoom.getSelectedItem().toString();
                        DeviceModel newDevice = new DeviceModel(name, iconResId, selectedRoom);
                        saveDevice(newDevice); // Lưu vào SharedPreferences

                        // Gửi dữ liệu về MainActivity
                        Intent intent = new Intent(ProvisionDevice.this, MainActivity.class);
                        intent.putExtra("device_name", name);
                        intent.putExtra("device_icon", iconResId);
                        intent.putExtra("device_room", selectedRoom);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();

                        Toast.makeText(this, "Thiết bị đã được lưu!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Vui lòng nhập tên thiết bị", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void saveDevice(DeviceModel device) {
        SharedPreferences prefs = getSharedPreferences("device_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = prefs.getString("device_list", "[]");
        Type type = new TypeToken<List<DeviceModel>>() {}.getType();
        List<DeviceModel> deviceList = gson.fromJson(json, type);

        deviceList.add(device);

        String updatedJson = gson.toJson(deviceList);
        editor.putString("device_list", updatedJson);
        editor.apply();
    }

    private List<String> loadRoomList() {
        SharedPreferences prefs = getSharedPreferences("room_prefs", MODE_PRIVATE);
        Set<String> set = prefs.getStringSet("room_list", null);
        List<String> roomList = new ArrayList<>();

        if (set != null) {
            roomList.addAll(set);
        } else {
            roomList.add("Phòng khách");
            roomList.add("Phòng ngủ");
            roomList.add("Phòng bếp");
        }

        return roomList;
    }

    private void updateStatus(String message) {
        Log.d(TAG, message);
        tvStatus.setText(message);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void showAlertDialog(String title, String message,
                                 android.content.DialogInterface.OnClickListener positiveListener,
                                 android.content.DialogInterface.OnClickListener negativeListener) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", positiveListener)
                .setNegativeButton("No", negativeListener)
                .show();
    }

    private void showBleDeviceListDialog() {
        showToast("This is where you would show a list of BLE devices");
    }

    @Override
    protected void onResume() {
        super.onResume();
        codeScanner.startPreview();
    }

    @Override
    protected void onPause() {
        codeScanner.releaseResources();
        super.onPause();
    }
}