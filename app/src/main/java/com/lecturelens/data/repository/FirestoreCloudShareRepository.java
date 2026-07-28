package com.lecturelens.data.repository;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lecturelens.domain.model.SharedNotesPacket;
import com.lecturelens.domain.repository.CloudShareRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FirestoreCloudShareRepository implements CloudShareRepository {

    private static final String COLLECTION = "shared_notes";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final FirebaseFirestore firestore;
    private final Random random = new Random();

    @Inject
    public FirestoreCloudShareRepository(@NonNull FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void publish(@NonNull SharedNotesPacket packet, @NonNull PublishCallback callback) {
        String code = packet.shareCode.isEmpty() ? newCode() : packet.shareCode.toUpperCase(Locale.US);
        Map<String, Object> data = new HashMap<>();
        data.put("title", packet.title);
        data.put("summary", packet.summary);
        data.put("keyTerms", packet.keyTerms);
        data.put("actionItems", packet.actionItems);
        data.put("transcript", packet.transcript);
        data.put("ownerEmail", packet.ownerEmail != null ? packet.ownerEmail : "");
        data.put("createdAtMs", packet.createdAtMs > 0L
                ? packet.createdAtMs
                : System.currentTimeMillis());

        firestore.collection(COLLECTION).document(code)
                .set(data)
                .addOnSuccessListener(unused -> callback.onPublished(code))
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    @Override
    public void fetchByCode(@NonNull String shareCode, @NonNull FetchCallback callback) {
        String code = shareCode.trim().toUpperCase(Locale.US);
        if (code.isEmpty()) {
            callback.onError("Enter a share code");
            return;
        }
        firestore.collection(COLLECTION).document(code)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap == null || !snap.exists()) {
                        callback.onError("No notes found for code " + code);
                        return;
                    }
                    callback.onFetched(fromSnapshot(code, snap));
                })
                .addOnFailureListener(e -> callback.onError(friendly(e)));
    }

    @NonNull
    private String newCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    @NonNull
    private static SharedNotesPacket fromSnapshot(@NonNull String code,
                                                  @NonNull DocumentSnapshot snap) {
        List<String> keyTerms = stringList(snap.get("keyTerms"));
        List<String> actionItems = stringList(snap.get("actionItems"));
        String title = stringOf(snap.getString("title"));
        String summary = stringOf(snap.getString("summary"));
        String transcript = stringOf(snap.getString("transcript"));
        String owner = snap.getString("ownerEmail");
        Long created = snap.getLong("createdAtMs");
        return new SharedNotesPacket(
                code,
                title,
                summary,
                keyTerms,
                actionItems,
                transcript,
                owner,
                created != null ? created : 0L);
    }

    @NonNull
    private static List<String> stringList(Object raw) {
        List<String> out = new ArrayList<>();
        if (!(raw instanceof List)) {
            return out;
        }
        for (Object item : (List<?>) raw) {
            if (item != null) {
                String s = item.toString().trim();
                if (!s.isEmpty()) {
                    out.add(s);
                }
            }
        }
        return out;
    }

    @NonNull
    private static String stringOf(String value) {
        return value != null ? value : "";
    }

    @NonNull
    private static String friendly(@NonNull Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.isEmpty()) {
            return "Cloud share failed. Check Firebase setup.";
        }
        if (msg.contains("PERMISSION_DENIED") || msg.toLowerCase(Locale.US).contains("permission")) {
            return "Firestore permission denied. Enable open rules for shared_notes (see Firebase console).";
        }
        if (msg.toLowerCase(Locale.US).contains("not been instantiated")
                || msg.toLowerCase(Locale.US).contains("firebaseapp")) {
            return "Firebase isn't configured. Add a matching google-services.json for com.lecturelens.";
        }
        return msg;
    }
}
