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