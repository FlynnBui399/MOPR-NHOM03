package com.example.pocket.utils;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import java.util.*;
import java.util.function.Consumer;

public class StreakHelper {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private static String getStreakId(String uid1, String uid2) {
        return uid1.compareTo(uid2) < 0 ? uid1 + "_" + uid2 : uid2 + "_" + uid1;
    }

    /** Call this immediately after a photo is successfully uploaded */
    public static void updateStreak(String senderUid, String receiverUid) {
        String streakId = getStreakId(senderUid, receiverUid);
        DocumentReference ref = db.collection("streaks").document(streakId);

        db.runTransaction(transaction -> {
            DocumentSnapshot doc = transaction.get(ref);
            Map<String, Object> data = new HashMap<>();

            if (!doc.exists()) {
                data.put("participants", Arrays.asList(senderUid, receiverUid));
                data.put("streakCount", 1L);
                data.put("lastPhotoAt", FieldValue.serverTimestamp());
                data.put(senderUid + "_sent", true);
                data.put(receiverUid + "_sent", false);
                transaction.set(ref, data);
                return null;
            }

            Timestamp last = doc.getTimestamp("lastPhotoAt");
            long hoursAgo = last != null
                ? (System.currentTimeMillis() - last.toDate().getTime()) / 3_600_000L : 999L;
            long current = doc.getLong("streakCount") != null ? doc.getLong("streakCount") : 0L;
            boolean otherSent = Boolean.TRUE.equals(doc.getBoolean(receiverUid + "_sent"));

            if (hoursAgo > 48) {
                data.put("streakCount", 1L);
                data.put(senderUid + "_sent", true);
                data.put(receiverUid + "_sent", false);
            } else if (otherSent) {
                data.put("streakCount", current + 1);
                data.put(senderUid + "_sent", false);
                data.put(receiverUid + "_sent", false);
            } else {
                data.put(senderUid + "_sent", true);
            }
            data.put("lastPhotoAt", FieldValue.serverTimestamp());
            transaction.update(ref, data);
            return null;
        });
    }

    /** Listen for streak count changes in real-time */
    public static ListenerRegistration listenStreak(String uid1, String uid2,
            Consumer<Long> onUpdate) {
        return db.collection("streaks").document(getStreakId(uid1, uid2))
            .addSnapshotListener((doc, e) -> {
                if (doc != null && doc.exists()) {
                    Long count = doc.getLong("streakCount");
                    onUpdate.accept(count != null ? count : 0L);
                }
            });
    }
}
