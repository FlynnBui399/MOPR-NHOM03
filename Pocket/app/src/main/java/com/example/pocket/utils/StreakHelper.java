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
            long streakCount = 0;
            boolean userAPostedToday = false;
            boolean userBPostedToday = false;
            Timestamp lastUpdated = null;

            boolean isUserA = senderUid.compareTo(receiverUid) < 0;

            if (doc.exists()) {
                Long sc = doc.getLong("streakCount");
                if (sc != null) streakCount = sc;
                Boolean aPosted = doc.getBoolean("userAPostedToday");
                if (aPosted != null) userAPostedToday = aPosted;
                Boolean bPosted = doc.getBoolean("userBPostedToday");
                if (bPosted != null) userBPostedToday = bPosted;
                lastUpdated = doc.getTimestamp("lastUpdated");

                if (lastUpdated != null) {
                    long diffMs = System.currentTimeMillis() - lastUpdated.toDate().getTime();
                    if (diffMs > 48 * 60 * 60 * 1000L) {
                        if (!userAPostedToday || !userBPostedToday) {
                            streakCount = 0;
                            userAPostedToday = false;
                            userBPostedToday = false;
                        }
                    }
                }
            }

            if (isUserA) {
                userAPostedToday = true;
            } else {
                userBPostedToday = true;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("userAPostedToday", userAPostedToday);
            updates.put("userBPostedToday", userBPostedToday);

            if (userAPostedToday && userBPostedToday) {
                boolean isNewDay = true;
                if (streakCount > 0 && lastUpdated != null) {
                    Calendar calLast = Calendar.getInstance();
                    calLast.setTime(lastUpdated.toDate());
                    Calendar calNow = Calendar.getInstance();
                    
                    isNewDay = calLast.get(Calendar.YEAR) != calNow.get(Calendar.YEAR)
                            || calLast.get(Calendar.DAY_OF_YEAR) != calNow.get(Calendar.DAY_OF_YEAR);
                }
                if (isNewDay) {
                    streakCount++;
                    updates.put("streakCount", streakCount);
                    updates.put("userAPostedToday", false);
                    updates.put("userBPostedToday", false);
                    updates.put("lastUpdated", FieldValue.serverTimestamp());
                } else {
                    updates.put("streakCount", streakCount);
                }
            } else {
                updates.put("streakCount", streakCount);
                if (lastUpdated == null) {
                    updates.put("lastUpdated", FieldValue.serverTimestamp());
                }
            }

            if (!doc.exists()) {
                transaction.set(ref, updates);
            } else {
                transaction.update(ref, updates);
            }
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
                } else {
                    onUpdate.accept(0L);
                }
            });
    }
}
