package com.example.pocket;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.pocket.data.model.Photo;
import com.example.pocket.utils.SharedPrefManager;
import com.example.pocket.viewmodel.FeedViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MemoriesFragment extends Fragment {
    private final List<Photo> photos = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_memories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RecyclerView memoriesList = view.findViewById(R.id.memories_list);
        View emptyState = view.findViewById(R.id.memories_empty_state);
        CircleImageView avatar = view.findViewById(R.id.memories_profile_avatar);
        MemoriesAdapter adapter = new MemoriesAdapter(this::openViewer);
        memoriesList.setLayoutManager(new LinearLayoutManager(requireContext()));
        memoriesList.setAdapter(adapter);

        Glide.with(this)
                .load(SharedPrefManager.getInstance(requireContext()).getAvatarUrl())
                .placeholder(R.drawable.avatar_placeholder)
                .error(R.drawable.avatar_placeholder)
                .into(avatar);
        avatar.setOnClickListener(clicked -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).openProfile();
            }
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            emptyState.setVisibility(View.VISIBLE);
            memoriesList.setVisibility(View.GONE);
            return;
        }

        FeedViewModel viewModel = new ViewModelProvider(this).get(FeedViewModel.class);
        viewModel.getTimelinePhotos().observe(getViewLifecycleOwner(), nextPhotos -> {
            photos.clear();
            if (nextPhotos != null) {
                for (Photo photo : nextPhotos) {
                    if (photo.getCreatedAt() != null && user.getUid().equals(photo.getSenderId())) {
                        photos.add(photo);
                    }
                }
            }
            adapter.submitPhotos(photos);
            boolean empty = photos.isEmpty();
            emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
            memoriesList.setVisibility(empty ? View.GONE : View.VISIBLE);
        });
        viewModel.loadTimeline(user.getUid());
    }

    private void openViewer(@NonNull Photo selectedPhoto) {
        int selectedIndex = 0;
        for (int index = 0; index < photos.size(); index++) {
            if (photos.get(index).getId() != null
                    && photos.get(index).getId().equals(selectedPhoto.getId())) {
                selectedIndex = index;
                break;
            }
        }
        MemoriesViewerDialogFragment.newInstance(new ArrayList<>(photos), selectedIndex)
                .show(getParentFragmentManager(), "memories_viewer");
    }
}
