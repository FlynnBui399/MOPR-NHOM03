package com.example.fonos.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.fonos.R;
import com.example.fonos.auth.LoginActivity;
import com.example.fonos.auth.RegisterActivity;

public class ProfileFragment extends Fragment {

    // TODO: Thay bằng logic kiểm tra đăng nhập thực tế của bạn
    // Ví dụ: dùng SharedPreferences hoặc Firebase Auth
    private boolean isLoggedIn() {
        return false; // Tạm thời luôn là chưa đăng nhập
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        Button btnLogin   = view.findViewById(R.id.btnLogin);
        Button btnRegister = view.findViewById(R.id.btnRegister);
        Button btnLogout  = view.findViewById(R.id.btnLogout);

        if (isLoggedIn()) {
            // Đã đăng nhập: ẩn login/register, hiện logout
            btnLogin.setVisibility(View.GONE);
            btnRegister.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
        } else {
            // Chưa đăng nhập: hiện login/register, ẩn logout
            btnLogin.setVisibility(View.VISIBLE);
            btnRegister.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.GONE);
        }

        btnLogin.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
        });

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), RegisterActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            // TODO: Thêm logic đăng xuất (Firebase signOut, xóa SharedPreferences...)
            // Ví dụ: FirebaseAuth.getInstance().signOut();
        });

        return view;
    }
}