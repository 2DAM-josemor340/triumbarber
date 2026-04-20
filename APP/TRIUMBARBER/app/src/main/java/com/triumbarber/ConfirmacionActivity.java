package com.triumbarber;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ConfirmacionActivity extends AppCompatActivity {

    private TextView tvResumen;
    private EditText etNotas;
    private Button btnConfirmar;
    private String barbero, fecha, hora;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirmacion);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        barbero = getIntent().getStringExtra("BARBERO");
        fecha = getIntent().getStringExtra("FECHA");
        hora = getIntent().getStringExtra("HORA");

        tvResumen = findViewById(R.id.tvResumenCita);
        etNotas = findViewById(R.id.etNotas);
        btnConfirmar = findViewById(R.id.btnConfirmarFinal);

        String resumen = "Barbero: " + barbero + "\n" +
                "Fecha: " + fecha + "\n" +
                "Hora: " + hora;
        tvResumen.setText(resumen);

        btnConfirmar.setOnClickListener(v -> guardarCitaEnFirebase());
    }

    private void guardarCitaEnFirebase() {
        String uidUsuario = mAuth.getCurrentUser().getUid();
        String notas = etNotas.getText().toString().trim();

        Map<String, Object> cita = new HashMap<>();
        cita.put("id_usuario", uidUsuario);
        cita.put("barbero", barbero);
        cita.put("fecha", fecha);
        cita.put("hora", hora);
        cita.put("notas", notas);
        cita.put("estado", "pendiente");

        db.collection("citas")
                .add(cita)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(ConfirmacionActivity.this, "¡Cita reservada con éxito!", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(ConfirmacionActivity.this, SeleccionBarberoActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ConfirmacionActivity.this, "Error al reservar: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}