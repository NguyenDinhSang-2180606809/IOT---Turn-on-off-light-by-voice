package com.nguyendinhsang.smlight;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class AddRoomActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_room);

        EditText etRoomName = findViewById(R.id.etRoomName);
        Button btnAdd = findViewById(R.id.btnAddRoom);

        btnAdd.setOnClickListener(v -> {
            String roomName = etRoomName.getText().toString().trim();
            if (roomName.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên phòng", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent resultIntent = new Intent();
            resultIntent.putExtra("room_name", roomName);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}
