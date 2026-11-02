package com.example.carpooling.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.carpooling.R;
import com.example.carpooling.activities.BookingHistoryActivity;
import com.example.carpooling.activities.DriverActivity;
import com.example.carpooling.activities.MainActivity;
import com.example.carpooling.activities.ProfileActivity;

public class BottomNavHelper {

    public static void setupNav(Activity activity, String userRole, int currentTab) {
        View bottomBar = activity.findViewById(R.id.bottomBar);
        if (bottomBar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(bottomBar, (v, insets) -> {
                int bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
                v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottomInset);
                return insets;
            });
        }

        LinearLayout navHome = activity.findViewById(R.id.navHome);
        LinearLayout navHistory = activity.findViewById(R.id.navHistory);
        LinearLayout navProfile = activity.findViewById(R.id.navProfile);

        ImageView iconHome = activity.findViewById(R.id.iconHome);
        TextView textHome = activity.findViewById(R.id.textHome);
        ImageView iconHistory = activity.findViewById(R.id.iconHistory);
        TextView textHistory = activity.findViewById(R.id.textHistory);
        ImageView iconProfile = activity.findViewById(R.id.iconProfile);
        TextView textProfile = activity.findViewById(R.id.textProfile);

        int activeColor = activity.getResources().getColor(android.R.color.holo_orange_dark);
        int inactiveColor = activity.getResources().getColor(android.R.color.darker_gray);

        if (iconHome != null) iconHome.setColorFilter(inactiveColor);
        if (textHome != null) textHome.setTextColor(inactiveColor);
        if (iconHistory != null) iconHistory.setColorFilter(inactiveColor);
        if (textHistory != null) textHistory.setTextColor(inactiveColor);
        if (iconProfile != null) iconProfile.setColorFilter(inactiveColor);
        if (textProfile != null) textProfile.setTextColor(inactiveColor);

        if (currentTab == 1) {
            if (iconHome != null) iconHome.setColorFilter(activeColor);
            if (textHome != null) textHome.setTextColor(activeColor);
        } else if (currentTab == 2) {
            if (iconHistory != null) iconHistory.setColorFilter(activeColor);
            if (textHistory != null) textHistory.setTextColor(activeColor);
        } else if (currentTab == 3) {
            if (iconProfile != null) iconProfile.setColorFilter(activeColor);
            if (textProfile != null) textProfile.setTextColor(activeColor);
        }

        if (navHome != null) {
            navHome.setOnClickListener(v -> {
                if (currentTab == 1) return;
                Intent intent = "DRIVER".equals(userRole) ?
                        new Intent(activity, DriverActivity.class) :
                        new Intent(activity, MainActivity.class);
                intent.putExtra("USER_ROLE", userRole);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);

                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                activity.finish();
            });
        }

        if (navHistory != null) {
            navHistory.setOnClickListener(v -> {
                if (currentTab == 2) return;
                Intent intent = new Intent(activity, BookingHistoryActivity.class);
                intent.putExtra("USER_ROLE", userRole);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);

                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                activity.finish();
            });
        }

        if (navProfile != null) {
            navProfile.setOnClickListener(v -> {
                if (currentTab == 3) return;
                Intent intent = new Intent(activity, ProfileActivity.class);
                intent.putExtra("USER_ROLE", userRole);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                activity.startActivity(intent);

                activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                activity.finish();
            });
        }
    }
}