package com.example.sensoryapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {
    private Set<String> selectedUnderstimOptions = new HashSet<>();
    private Set<String> selectedOverstimOptions = new HashSet<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.editUnderstimPreferences).setOnClickListener(v -> {
            showUnderstimPreferencesDialog();
        });

        findViewById(R.id.editOverstimPreferences).setOnClickListener(v -> {
            showOverstimPreferencesDialog();
        });

        MaterialToolbar appBar = findViewById(R.id.topAppBar);
        appBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, MainActivity.class);
                startActivity(intent);
            }
        });
    }


    private void showUnderstimPreferencesDialog() {
        LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.understim_location_choices_dialog, null);

        CheckBox cafeOption = dialogView.findViewById(R.id.cafeOption);
        CheckBox restaurantOption = dialogView.findViewById(R.id.restaurantOption);
        CheckBox libraryOption = dialogView.findViewById(R.id.libraryOption);
        CheckBox parkOption = dialogView.findViewById(R.id.parkOption);

        cafeOption.setChecked(selectedUnderstimOptions.contains("Cafe"));
        restaurantOption.setChecked(selectedUnderstimOptions.contains("Restaurant"));
        libraryOption.setChecked(selectedUnderstimOptions.contains("Library"));
        parkOption.setChecked(selectedUnderstimOptions.contains("Park"));

        new AlertDialog.Builder(this)
                .setTitle("Choose Options")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    StringBuilder selectedOptions = new StringBuilder();
                    selectedUnderstimOptions.clear();
                    if (cafeOption.isChecked()) {
                        selectedOptions.append("Cafe, ");
                        selectedUnderstimOptions.add("Cafe");
                    }
                    if (restaurantOption.isChecked()) {
                        selectedOptions.append("Restaurant, ");
                        selectedUnderstimOptions.add("Restaurant");
                    }
                    if (libraryOption.isChecked()) {
                        selectedOptions.append("Library, ");
                        selectedUnderstimOptions.add("Library");
                    }
                    if (parkOption.isChecked()) {
                        selectedOptions.append("Park, ");
                        selectedUnderstimOptions.add("Park");
                    }
                    if (selectedOptions.length() > 0) {
                        selectedOptions.setLength(selectedOptions.length() - 2);
                    }
                    ((TextView)findViewById(R.id.understimPreferences)).setText(selectedOptions);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void showOverstimPreferencesDialog() {
        LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.overstim_location_choices_dialog, null);

        CheckBox bowlingOption = dialogView.findViewById(R.id.bowlingOption);
        CheckBox restaurantOption = dialogView.findViewById(R.id.restaurantOption);
        CheckBox gymOption = dialogView.findViewById(R.id.gymOption);
        CheckBox parkOption = dialogView.findViewById(R.id.parkOption);

        bowlingOption.setChecked(selectedOverstimOptions.contains("Bowling"));
        restaurantOption.setChecked(selectedOverstimOptions.contains("Restaurant"));
        gymOption.setChecked(selectedOverstimOptions.contains("Gym"));
        parkOption.setChecked(selectedOverstimOptions.contains("Park"));

        new AlertDialog.Builder(this)
                .setTitle("Choose Options")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    StringBuilder selectedOptions = new StringBuilder();
                    selectedOverstimOptions.clear();
                    if (bowlingOption.isChecked()) {
                        selectedOptions.append("Bowling, ");
                        selectedOverstimOptions.add("Bowling");
                    }
                    if (restaurantOption.isChecked()) {
                        selectedOptions.append("Restaurant, ");
                        selectedOverstimOptions.add("Restaurant");
                    }
                    if (gymOption.isChecked()) {
                        selectedOptions.append("Gym, ");
                        selectedOverstimOptions.add("Gym");
                    }
                    if (parkOption.isChecked()) {
                        selectedOptions.append("Park, ");
                        selectedOverstimOptions.add("Park");
                    }
                    if (selectedOptions.length() > 0) {
                        selectedOptions.setLength(selectedOptions.length() - 2);
                    }
                    ((TextView)findViewById(R.id.overstimPreferences)).setText(selectedOptions);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }
}