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

    // IP del servidor (POR AHORA ES CON RED LOCAL)
    private final String SERVER_URL = "http://0.0.0.0:3000";

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

        // --- BOTONES PRINCIPALES ---
        MaterialButton btnSOS = findViewById(R.id.btnSOS);
        btnSOS.setOnClickListener(v -> {
            dynamoSimulator.enviarDatosSimulados(idUsuarioActual, true);
            Toast.makeText(this, "¡ALERTA SOS ENVIADA A AWS!", Toast.LENGTH_LONG).show();
        });

        // BOTÓN: Añadir Contactos
        findViewById(R.id.btnAddContact).setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddContactActivity.class);
            intent.putExtra("id_usuario", idUsuarioActual);
            startActivity(intent);
        });

        // BOTÓN NUEVO: Añadir Medicinas (Se agrega al layout_meds_content)
        MaterialButton btnAddMed = findViewById(R.id.btnAddMed);
        btnAddMed.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, AddMedicineActivity.class);
            intent.putExtra("id_usuario", idUsuarioActual);
            startActivity(intent);
        });

        // --- NAVEGACIÓN INFERIOR ---
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
                obtenerMedicamentosSQL(); // <--- LLAMADA INTEGRADA
            }
            return true;
        });

        handler.post(monitoreoLoop);
    }

    // --- MÓDULO DE CONTACTOS ---
    private void obtenerContactosSQL() {
        String url = SERVER_URL + "/contactos/" + idUsuarioActual;
        client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        final JSONArray array = new JSONArray(response.body().string());
                        runOnUiThread(() -> {
                            LinearLayout listContainer = findViewById(R.id.list_contacts_container);
                            listContainer.removeAllViews();
                            for (int i = 0; i < array.length(); i++) {
                                try { inflarContacto(array.getJSONObject(i), listContainer); } catch (JSONException e) { e.printStackTrace(); }
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
        int idContacto = obj.getInt("id_contacto"); // Asegúrate que el SQL devuelva el ID

        ((TextView)view.findViewById(R.id.tvNombreCont)).setText(obj.getString("nombre_contacto"));
        ((TextView)view.findViewById(R.id.tvTelCont)).setText(obj.getString("telefono"));
        ((TextView)view.findViewById(R.id.tvPrioridadIcon)).setText(obj.getString("prioridad"));

        // LÓGICA DE BORRADO
        view.findViewById(R.id.btnEliminarItem).setOnClickListener(v -> {
            eliminarRecursoAPI("/contactos/" + idContacto, this::obtenerContactosSQL);
        });

        container.addView(view);
    }

    // --- MÓDULO DE MEDICINAS (NUEVO) ---
    private void obtenerMedicamentosSQL() {
        String url = SERVER_URL + "/medicamentos/" + idUsuarioActual;
        client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        final JSONArray array = new JSONArray(response.body().string());
                        runOnUiThread(() -> {
                            LinearLayout container = findViewById(R.id.list_meds_container);
                            container.removeAllViews();
                            for (int i = 0; i < array.length(); i++) {
                                try { inflarMedicina(array.getJSONObject(i), container); } catch (JSONException e) { e.printStackTrace(); }
                            }
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            @Override public void onFailure(Call call, IOException e) {}
        });
    }

    private void inflarMedicina(JSONObject obj, LinearLayout container) throws JSONException {
        View view = getLayoutInflater().inflate(R.layout.item_medicinas, null);
        int idMed = obj.getInt("id_medicina");
        String nombre = obj.getString("nombre_farmaco");

        ((TextView)view.findViewById(R.id.tvNombreMedItem)).setText(nombre);
        ((TextView)view.findViewById(R.id.tvInfoMedItem)).setText(obj.getString("dosis") + " - Cada " + obj.getString("frecuencia_horas") + "h");

        // LÓGICA DE BORRADO
        view.findViewById(R.id.btnEliminarItem).setOnClickListener(v -> {
            eliminarRecursoAPI("/medicamentos/" + idMed, this::obtenerMedicamentosSQL);
        });

        container.addView(view);
    }

    // --- LÓGICA DE MAPA ---
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
    private void eliminarRecursoAPI(String endpoint, Runnable onSuccess) {
        Request request = new Request.Builder()
                .url(SERVER_URL + endpoint)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(DashboardActivity.this, "Eliminado con éxito", Toast.LENGTH_SHORT).show();
                        onSuccess.run(); // Refresca la lista automáticamente
                    });
                }
            }
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(DashboardActivity.this, "Error al eliminar", Toast.LENGTH_SHORT).show());
            }
        });
    }
}