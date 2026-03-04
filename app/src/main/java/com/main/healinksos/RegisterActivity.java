package com.main.healinksos;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RegisterActivity extends AppCompatActivity {
    private final OkHttpClient client = new OkHttpClient();

    private AutoCompleteTextView autoSangre;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 1. Referencias a la UI
        EditText etNombre = findViewById(R.id.etNombre);
        EditText etEmail = findViewById(R.id.etEmailReg);
        EditText etEdad = findViewById(R.id.etEdad);
        autoSangre = findViewById(R.id.autoCompleteSangre); // nuevo XML
        EditText etAlergias = findViewById(R.id.etAlergias);
        EditText etHistorial = findViewById(R.id.etHistorial);
        EditText etPassword = findViewById(R.id.etRegPass);
        Button btnRegistrar = findViewById(R.id.btnRegister);
        Button btnVolver = findViewById(R.id.btnBack);

        // Configuración del Dropdown
        String[] tiposSangre = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                tiposSangre
        );
        autoSangre.setAdapter(adapter);

        btnRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Captura de datos
                String nom = etNombre.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String edadTxt = etEdad.getText().toString().trim();
                String sangre = autoSangre.getText().toString(); // Cambio aquí: .getText()
                String alergias = etAlergias.getText().toString().trim();
                String historial = etHistorial.getText().toString().trim();
                String pass = etPassword.getText().toString().trim();

                // VALIDACIONE

                // Formato de Correo
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(RegisterActivity.this, "Formato de correo inválido", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Campos vacíos
                if(nom.isEmpty() || email.isEmpty() || pass.isEmpty() || edadTxt.isEmpty() || sangre.isEmpty()) {
                    Toast.makeText(RegisterActivity.this, "Por favor llena los campos obligatorios", Toast.LENGTH_SHORT).show();
                    return;
                }

                if(pass.length()<8){
                    Toast.makeText(RegisterActivity.this, "La contraseña debe tener al menos 8 caracteres", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Envío a la API
                enviarDatosAPI(nom, email, edadTxt, sangre, alergias, historial, pass);
            }
        });

        btnVolver.setOnClickListener(v -> finish());
    }

    private void enviarDatosAPI(String nombre, String email, String edad, String sangre, String alergias, String historial, String password) {
        String url = "http://0.0.0.0:3000/registrar"; // PONER IP

        JSONObject json = new JSONObject();
        try {
            json.put("nombre", nombre);
            json.put("email", email);
            json.put("edad", Integer.parseInt(edad));
            json.put("sangre", sangre);
            json.put("alergias", alergias);
            json.put("historial", historial);
            json.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(RegisterActivity.this, "Error de red AWS: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    runOnUiThread(() -> {
                        Toast.makeText(RegisterActivity.this, "¡Perfil HeaLink Creado!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                } else {
                    runOnUiThread(() ->
                            Toast.makeText(RegisterActivity.this, "Error: El correo ya existe", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }
}