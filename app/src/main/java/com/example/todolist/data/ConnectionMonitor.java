package com.example.todolist.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * One app-wide view of "is the phone connected to the Realtime Database server right now". It is driven by
 * the special {@code .info/connected} location, which Firebase maintains itself and which is false until the
 * first successful connection, so it also tells apart "slow" from "this address never works".
 */
public final class ConnectionMonitor {

    private static final MutableLiveData<Boolean> CONNECTED = new MutableLiveData<>(false);
    private static volatile boolean connectedNow;
    private static boolean started;

    private ConnectionMonitor() {
    }

    /** Idempotent; call once at app start, after persistence has been configured. */
    public static synchronized void start() {
        if (started) return;
        started = true;
        try {
            FirebaseDatabase.getInstance().getReference(".info/connected")
                    .addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            boolean value = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                            connectedNow = value;
                            CONNECTED.postValue(value);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            // The connection indicator is not protected by rules; nothing to do.
                        }
                    });
        } catch (RuntimeException e) {
            started = false; // e.g. the database address in google-services.json is unusable: try again later
        }
    }

    public static LiveData<Boolean> connected() {
        return CONNECTED;
    }

    public static boolean isConnected() {
        return connectedNow;
    }

    /** Drops and re-opens the connection; helps after a network change or a VPN switch. */
    public static void reconnect() {
        try {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            database.goOffline();
            database.goOnline();
        } catch (RuntimeException ignored) {
            // Nothing sensible to do: the banner keeps telling the user the state.
        }
    }
}
