package com.main.healinksos;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DashboardActivity extends AppCompatActivity {

    private DynamoSimulator dynamoSimulator;
    private int idUsuarioActual;
    private TextView tvBpm;
    private MapView mapView;
    private Marker userMarker;
    private Handler handler = new Handler();
    private boolean monitoreoActivo = true;
    private final OkHttpClient client = new OkHttpClient();

    private LinearLayout layoutHome, layoutContacts, layoutMeds;

    private Runnable monitoreoLoop = new Runnable() {
        @Override
        public void run() {
            if (monitoreoActivo && idUsuarioActual > 0) {
                dynamoSimulator.enviarDatosSimulados(idUsuarioActual, false);
                dynamoSimulator.obtenerUltimoDato(idUsuarioActual, (bpm, lat, lng) -> {
                    runOnUiThread(() -> {
                        tvBpm.setText(bpm);
                        actualizarMapa(Double.parseDouble(lat), Double.parseDouble(lng));
                    });
                });
                handler.postDelayed(this, 10000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        Configuration.getInstance().setOsmdroidTileCache(getCacheDir());

        setContentView(R.layout.activity_dashboard);

        idUsuarioActual = getIntent().getIntExtra("id_usuario", 0);
        dynamoSimulator = new DynamoSimulator();
        tvBpm = findViewById(R.id.tvBPMValue);

        layoutHome = findViewById(R.id.layout_home_content);
        layoutContacts = findViewById(R.id.layout_contacts_content);
        layoutMeds = findViewById(R.id.layout_meds_content);

        solicitarPermisos();
        configurarMapa();

        // Botón SOS
        MaterialButton btnSOS = findViewById(R.id.btnSOS);
        btnSOS.setOnClickListener(v -> {
            dynamoSimulator.enviarDatosSimulados(idUsuarioActual, true);
            Toast.makeText(this, "¡ALERTA SOS ENVIADA A AWS!", Toast.LENGTH_LONG).show();
        });

        // BOTÓN: Abrir Formulario de Contactos
        MaterialButton btnAddContact = findViewById(R.id.btnAddContact);
        btnAddContact.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddContactActivity.class);
            intent.putExtra("id_usuario", idUsuarioActual);
            startActivity(intent);
        });

        BottomNavigationView nav = findViewById(R.id.bottom_navigation);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            layoutHome.setVisibility(View.GONE);
            layoutContacts.setVisibility(View.GONE);
            layoutMeds.setVisibility(View.GONE);

            if (id == R.id.nav_home) {
                layoutHome.setVisibility(View.VISIBLE);
            } else if (id == R.id.nav_contacts) {
                layoutContacts.setVisibility(View.VISIBLE);
                obtenerContactosSQL();
            } else if (id == R.id.nav_meds) {
                layoutMeds.setVisibility(View.VISIBLE);
            }
            return true;
        });

        handler.post(monitoreoLoop);
    }

    private void obtenerContactosSQL() {
        String url = "http://0.0.0.0:3000/contactos/" + idUsuarioActual; // SE añade IP

        client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        final String responseData = response.body().string();
                        final JSONArray array = new JSONArray(responseData);
                        runOnUiThread(() -> {
                            try {
                                LinearLayout listContainer = findViewById(R.id.list_contacts_container);
                                listContainer.removeAllViews();
                                for (int i = 0; i < array.length(); i++) {
                                    JSONObject obj = array.getJSONObject(i);
                                    inflarContacto(obj, listContainer);
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call call, IOException e) {}
        });
    }

    private void inflarContacto(JSONObject obj, LinearLayout container) throws JSONException {
        View view = getLayoutInflater().inflate(R.layout.item_contacto, null);
        TextView nombre = view.findViewById(R.id.tvNombreCont);
        TextView tel = view.findViewById(R.id.tvTelCont);
        TextView prio = view.findViewById(R.id.tvPrioridadIcon);

        nombre.setText(obj.getString("nombre_contacto"));
        tel.setText(obj.getString("telefono"));
        prio.setText(obj.getString("prioridad"));
        container.addView(view);
    }

    private void configurarMapa() {
        mapView = findViewById(R.id.mapView);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(17.0);
        userMarker = new Marker(mapView);
        userMarker.setTitle("Paciente HeaLink");
        mapView.getOverlays().add(userMarker);
    }

    private void actualizarMapa(double lat, double lng) {
        if (mapView != null) {
            GeoPoint point = new GeoPoint(lat, lng);
            userMarker.setPosition(point);
            mapView.getController().animateTo(point);
            mapView.invalidate();
        }
    }

    private void solicitarPermisos() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 100);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        monitoreoActivo = false;
        handler.removeCallbacks(monitoreoLoop);
    }
}