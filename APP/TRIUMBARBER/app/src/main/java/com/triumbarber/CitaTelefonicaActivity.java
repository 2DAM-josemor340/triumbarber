package com.triumbarber;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CitaTelefonicaActivity extends AppCompatActivity {

    private EditText etNombre, etApellidos, etDni, etTelefono, etEmail;
    private Button btnContinuar, btnVolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cita_telefonica);

        etNombre = findViewById(R.id.etNombreTel);
        etApellidos = findViewById(R.id.etApellidosTel);
        etDni = findViewById(R.id.etDNITel);
        etTelefono = findViewById(R.id.etTelefonoTel);
        etEmail = findViewById(R.id.etEmailTel);
        btnContinuar = findViewById(R.id.btnContinuarCitaTel);
        btnVolver = findViewById(R.id.btnVolverAdmin);

        btnVolver.setOnClickListener(v -> finish());

        btnContinuar.setOnClickListener(v -> {
            String nombre = etNombre.getText().toString().trim();
            String apellidos = etApellidos.getText().toString().trim();
            String dni = etDni.getText().toString().trim();
            String telefono = etTelefono.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (nombre.isEmpty() || apellidos.isEmpty() || dni.isEmpty() || telefono.isEmpty()) {
                Toast.makeText(this, "Rellena los campos obligatorios (Nombre, Apellidos, DNI, Teléfono)", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, SeleccionBarberoActivity.class);
            intent.putExtra("MODO_TELEFONICO", true);
            intent.putExtra("TEL_NOMBRE", nombre);
            intent.putExtra("TEL_APELLIDOS", apellidos);
            intent.putExtra("TEL_DNI", dni);
            intent.putExtra("TEL_TELEFONO", telefono);
            intent.putExtra("TEL_EMAIL", email);
            startActivity(intent);
        });
    }
}