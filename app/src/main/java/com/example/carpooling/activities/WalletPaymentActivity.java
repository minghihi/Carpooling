package com.example.carpooling.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.example.carpooling.R;

public class WalletPaymentActivity extends AppCompatActivity {

    private RadioGroup rgOnlineOptions;
    private AppCompatButton btnConfirmWallet;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet_payment);

        rgOnlineOptions = findViewById(R.id.rgOnlineOptions);
        btnConfirmWallet = findViewById(R.id.btnConfirmWallet);
        btnBack = findViewById(R.id.btnBack);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnConfirmWallet != null) {
            btnConfirmWallet.setOnClickListener(v -> {
                Toast.makeText(this, "Tính năng thanh toán trực tuyến này hiện chưa hỗ trợ!", Toast.LENGTH_LONG).show();
            });
        }
    }
}