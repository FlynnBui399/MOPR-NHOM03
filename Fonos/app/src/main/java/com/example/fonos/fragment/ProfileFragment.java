package com.example.fonos.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fonos.R;
import com.example.fonos.auth.LoginActivity;
import com.example.fonos.auth.RegisterActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private TextView tvProfileName, tvProfileEmail;
    private Button btnLogin, btnRegister, btnLogout;

    private boolean isLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);
        btnLogin   = view.findViewById(R.id.btnLogin);
        btnRegister = view.findViewById(R.id.btnRegister);
        btnLogout  = view.findViewById(R.id.btnLogout);

        View btnSettings = view.findViewById(R.id.btnSettings);
        View btnHistory = view.findViewById(R.id.btnHistory);
        View btnDownloads = view.findViewById(R.id.btnDownloads);
        View btnHelp = view.findViewById(R.id.btnHelp);
        View btnAbout = view.findViewById(R.id.btnAbout);

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> Toast.makeText(getContext(), "Dang mo Cai dat...", Toast.LENGTH_SHORT).show());
        }
        if (btnHistory != null) {
            btnHistory.setOnClickListener(v -> Toast.makeText(getContext(), "Lich su nghe dang trong", Toast.LENGTH_SHORT).show());
        }
        if (btnDownloads != null) {
            btnDownloads.setOnClickListener(v -> Toast.makeText(getContext(), "Chua co sach nao duoc tai xuong", Toast.LENGTH_SHORT).show());
        }
        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> Toast.makeText(getContext(), "Dang ket noi den trung tam ho tro...", Toast.LENGTH_SHORT).show());
        }
        if (btnAbout != null) {
            btnAbout.setOnClickListener(v -> Toast.makeText(getContext(), "Fonos - Phien ban 1.0.0", Toast.LENGTH_SHORT).show());
        }

        updateUI();

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RegisterActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            updateUI();
        });

        return view;
    }

    private void updateUI() {
        if (isLoggedIn()) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                String name = user.getDisplayName();
                tvProfileName.setText(name != null && !name.isEmpty() ? name : "Fonos User");
                tvProfileEmail.setText(user.getEmail());
            }
            btnLogin.setVisibility(View.GONE);
            btnRegister.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
        } else {
            tvProfileName.setText(getString(R.string.profile_guest));
            tvProfileEmail.setText(getString(R.string.profile_guest_email));
            btnLogin.setVisibility(View.VISIBLE);
            btnRegister.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.GONE);
        }
    }
}