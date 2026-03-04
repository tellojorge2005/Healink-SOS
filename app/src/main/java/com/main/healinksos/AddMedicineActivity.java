package com.main.healinksos;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AddMedicineActivity extends AppCompatActivity {

    private final OkHttpClient client = new OkHttpClient();
    private int idUsuario;
    private String horaFormatoSQL = "00:00:00";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        idUsuario = getIntent().getIntExtra("id_usuario", 0);

        // Referencias a la interfaz de usuario
        TextInputEditText etNombre = findViewById(R.id.etNombreMed);
        TextInputEditText etDosis = findViewById(R.id.etDosisMed);
        TextInputEditText etFreq = findViewById(R.id.etFreqMed);
        TextView tvHora = findViewById(R.id.tvHoraMed);
        MaterialButton btnHora = findViewById(R.id.btnPickTimeMed);
        MaterialButton btnGuardar = findViewById(R.id.btnGuardarMed);

        // Selector de hora para Hora_Programada
        btnHora.setOnClickListener(v -> {
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                horaFormatoSQL = String.format("%02d:%02d:00", hourOfDay, minute);
                tvHora.setText("Hora: " + String.format("%02d:%02d", hourOfDay, minute));
            }, 12, 0, true).show();
        });

        btnGuardar.setOnClickListener(v -> {
            String nom = etNombre.getText().toString().trim();
            String dos = etDosis.getText().toString().trim();
            String freq = etFreq.getText().toString().trim();

            if (nom.isEmpty() || freq.isEmpty()) {
                Toast.makeText(this, "Llena los campos obligatorios", Toast.LENGTH_SHORT).show();
                return;
            }

            enviarMedicinaAPI(nom, dos, Integer.parseInt(freq));
        });

        findViewById(R.id.btnCancelarMed).setOnClickListener(v -> finish());
    }

    private void enviarMedicinaAPI(String nombre, String dosis, int frecuencia) {
        // IP de servidor (POR AHORA ES CON RED LOCAL)
        String url = "http://0.0.0.0:3000/medicamentos";

        JSONObject json = new JSONObject();
        try {
            // Keys sincronizados
            json.put("id_usuario", idUsuario);
            json.put("nombre", nombre);
            json.put("dosis", dosis);
            json.put("hora", horaFormatoSQL);
            json.put("frecuencia", frecuencia);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(AddMedicineActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddMedicineActivity.this, "¡Medicina Guardada!", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    });
                }
            }
        });
    }
}