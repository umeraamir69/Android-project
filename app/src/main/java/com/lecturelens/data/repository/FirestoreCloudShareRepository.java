package com.lecturelens.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lecturelens.domain.model.SharedHandout;
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
        data.put("ownerName", packet.ownerName != null ? packet.ownerName : "");
        data.put("university", packet.university != null ? packet.university : "");
        data.put("professor", packet.professor != null ? packet.professor : "");
        data.put("handouts", handoutsToMaps(packet.handouts));
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
        if (code.length() != CODE_LENGTH) {
            callback.onError("Share codes are exactly " + CODE_LENGTH + " characters");
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

    /** Public so ExportLectureUseCase can pre-allocate a code before uploading files. */
    @NonNull
    public String allocateShareCode() {
        return newCode();
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
    private static List<Map<String, Object>> handoutsToMaps(@NonNull List<SharedHandout> handouts) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SharedHandout h : handouts) {
            Map<String, Object> map = new HashMap<>();
            map.put("displayName", h.displayName);
            map.put("mimeType", h.mimeType);
            map.put("extractedText", h.extractedText);
            map.put("downloadUrl", h.downloadUrl);
            out.add(map);
        }
        return out;
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
                snap.getString("ownerName"),
                snap.getString("university"),
                snap.getString("professor"),
                handoutsFrom(snap.get("handouts")),
                created != null ? created : 0L);
    }

    @NonNull
    private static List<SharedHandout> handoutsFrom(@Nullable Object raw) {
        List<SharedHandout> out = new ArrayList<>();
        if (!(raw instanceof List)) {
            return out;
        }
        for (Object item : (List<?>) raw) {
            if (!(item instanceof Map)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) item;
            out.add(new SharedHandout(
                    stringObj(map.get("displayName")),
                    stringObj(map.get("mimeType")),
                    stringObj(map.get("extractedText")),
                    stringObj(map.get("downloadUrl"))));
        }
        return out;
    }

    @NonNull
    private static String stringObj(@Nullable Object value) {
        return value != null ? value.toString() : "";
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
            return "Firestore/Storage permission denied. Allow shared_notes + shared/ Storage writes (see Firebase console).";
        }
        if (msg.toLowerCase(Locale.US).contains("not been instantiated")
                || msg.toLowerCase(Locale.US).contains("firebaseapp")) {
            return "Firebase isn't configured. Add a matching google-services.json for com.lecturelens.";
        }
        return msg;
    }
}
