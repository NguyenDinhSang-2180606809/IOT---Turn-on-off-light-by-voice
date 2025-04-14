package com.nguyendinhsang.smlight;

// ✅ Thêm những dòng này:
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    BottomNavigationView bottomNavigationView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        bottomNavigationView = findViewById(R.id.bottomNavMenu);
        bottomNavigationView.setOnItemSelectedListener(this);
        loadFragment(new HomeFragment(), R.id.nav_fragment);

    }
    private void loadFragment(Fragment fragment, int layout) {
        FragmentManager fm = getSupportFragmentManager(); // ✅ dùng Support version
        FragmentTransaction fragmentTransaction = fm.beginTransaction();
        fragmentTransaction.replace(layout, fragment);
        fragmentTransaction.commit();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.homeMenu) {
            HomeFragment fragment = new HomeFragment();
            loadFragment(new HomeFragment(), R.id.nav_fragment);
        }
        return false;
    }
}

