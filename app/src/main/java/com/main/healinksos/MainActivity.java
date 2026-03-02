package com.main.healinksos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPass = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvRegistro = findViewById(R.id.tvGoToRegister);

        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Ingresa tus credenciales", Toast.LENGTH_SHORT).show();
                return;
            }
            validarLoginConAPI(email, pass);
        });

        tvRegistro.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void validarLoginConAPI(String email, String password) {
        //PONER TU IP
        String url = "http://0.0.0.0:3000/login";

        JSONObject json = new JSONObject();
        try {
            json.put("email", email);
            json.put("password", password);
        } catch (Exception e) { e.printStackTrace(); }

        RequestBody body = RequestBody.create(json.toString(),
                MediaType.parse("application/json; charset=utf-8"));

        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error de red", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject resObj = new JSONObject(response.body().string());
                        int idUsuario = resObj.getInt("id");

                        runOnUiThread(() -> {
                            Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                            intent.putExtra("id_usuario", idUsuario);
                            startActivity(intent);
                            finish();
                        });
                    } catch (Exception e) { e.printStackTrace(); }
                } else {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Credenciales incorrectas", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }
}