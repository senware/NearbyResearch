package edu.temple.nearbyresearch;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.Manifest;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    static final String SERVICE_ID = "edu.temple.nearbyresearch.SERVICE_ID";
    private ArrayList<String> userList;
    private ArrayList<String> connections;
    private RecyclerView recyclerView;
    private NearbyAdapter connectionAdapter;
    private String username;

    private ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    isGranted -> {
                boolean allGranted = true;
                for (String k : isGranted.keySet()) {
                    if(allGranted && !isGranted.get(k)) {
                        allGranted = false;
                    }
                }
                if(allGranted)
                    Toast.makeText(this, "Permissions Granted", Toast.LENGTH_LONG).show();
                else {
                    Toast.makeText(this, "Permissions NOT Granted", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.connection_list);
        userList = new ArrayList<>();
        connections = new ArrayList<>();
        connectionAdapter = new NearbyAdapter(userList);
        recyclerView.setAdapter(connectionAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (!checkAllPermissions()) {
            getUserPermission();
        }

        EditText usernameEditText = findViewById(R.id.username_edit_text);
        username = "";
        usernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                username = s.toString();
            }
        });

        Button discoverButton = findViewById(R.id.discover_button);
        discoverButton.setOnClickListener(v -> {
            startDiscovery();
            startAdvertising();
        });
    }



    private boolean checkAllPermissions() {
        return ContextCompat.checkSelfPermission(this, "ALL_PERMISSIONS") == PackageManager.PERMISSION_GRANTED;
    }

    private void getUserPermission() {
        requestPermissionLauncher.launch(new String[] {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        });
    }

    private void startAdvertising() {
        AdvertisingOptions advertisingOptions =
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        Nearby.getConnectionsClient(this)
                .startAdvertising(
                        username, SERVICE_ID, connectionLifecycleCallback, advertisingOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Toast.makeText(this, "Now advertising", Toast.LENGTH_LONG).show();
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Toast.makeText(this, "Could not start advertising", Toast.LENGTH_LONG).show();
                        });
    }

    private final ConnectionLifecycleCallback connectionLifecycleCallback = new ConnectionLifecycleCallback() {

        @Override
        public void onConnectionInitiated(@NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {

            Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endpointId, new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
                    String data = new String(payload.asBytes());
                    connections.add(endpointId);
                    userList.add(data);
                    connectionAdapter.notifyItemInserted(userList.indexOf(data));
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

                }
            });
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution connectionResolution) {
            Payload toSend = Payload.fromBytes(username.getBytes());
            Nearby.getConnectionsClient(getApplicationContext()).sendPayload(endpointId, toSend);
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            int pos = connections.indexOf(endpointId);
            userList.remove(pos);
            connections.remove(pos);
            connectionAdapter.notifyItemRemoved(pos);
        }
    };

    private void startDiscovery() {
        DiscoveryOptions discoveryOptions =
                new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build();
        Nearby.getConnectionsClient(this)
                .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener(
                        (Void unused) -> {
                            Toast.makeText(this, "Discovery initiated", Toast.LENGTH_LONG).show();
                        })
                .addOnFailureListener(
                        (Exception e) -> {
                            Toast.makeText(this, "Unable to start discovery", Toast.LENGTH_LONG).show();
                        });
    }

    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                    Nearby.getConnectionsClient(getApplicationContext())
                            .requestConnection(username, endpointId, connectionLifecycleCallback)
                            .addOnSuccessListener(
                                    (Void unused) -> {

                                    })
                            .addOnFailureListener(
                                    (Exception e) -> {

                                    });
                }

                @Override
                public void onEndpointLost(@NonNull String s) {

                }
            };
}