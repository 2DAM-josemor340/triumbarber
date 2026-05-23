package com.triumbarber;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfirmacionActivity extends AppCompatActivity {

    private TextView tvResumen, tvNotaLabel;
    private EditText etNotas;
    private Button btnConfirmar;
    private String barbero, fecha, hora, servicio, citaIdExistente;
    private boolean modoTelefonico;
    private String telNombre, telApellidos, telDni, telTelefono, telEmail;

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
        servicio = getIntent().getStringExtra("SERVICIO");
        citaIdExistente = getIntent().getStringExtra("CITA_ID");
        modoTelefonico = getIntent().getBooleanExtra("MODO_TELEFONICO", false);

        if (modoTelefonico) {
            telNombre = getIntent().getStringExtra("TEL_NOMBRE");
            telApellidos = getIntent().getStringExtra("TEL_APELLIDOS");
            telDni = getIntent().getStringExtra("TEL_DNI");
            telTelefono = getIntent().getStringExtra("TEL_TELEFONO");
            telEmail = getIntent().getStringExtra("TEL_EMAIL");
        }

        tvResumen = findViewById(R.id.tvResumenCita);
        tvNotaLabel = findViewById(R.id.tvNotaLabel);
        etNotas = findViewById(R.id.etNotas);
        btnConfirmar = findViewById(R.id.btnConfirmarFinal);

        actualizarResumen("");

        etNotas.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String notaStr = s.toString().trim();
                actualizarResumen(notaStr);
                
                if (notaStr.isEmpty()) {
                    tvNotaLabel.setText("Nota: -");
                } else {
                    tvNotaLabel.setText("Nota: " + notaStr);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnConfirmar.setOnClickListener(v -> guardarCitaCompleta());
    }

    private void actualizarResumen(String nota) {
        String resumen = "Barbero: " + barbero + "\n" +
                "Servicio: " + servicio + "\n" +
                "Fecha: " + fecha + "\n" +
                "Hora: " + hora;
        
        if (!nota.isEmpty()) {
            resumen += "\nNota: " + nota;
        }
        
        tvResumen.setText(resumen);
    }

    private void guardarCitaCompleta() {
        List<String> bloquesAReservar = calcularBloquesAReservar();
        String notas = etNotas.getText().toString().trim();

        if (modoTelefonico) {
            for (String h : bloquesAReservar) {
                Map<String, Object> c = new HashMap<>();
                c.put("barbero", barbero);
                c.put("fecha", fecha);
                c.put("hora", h);
                c.put("servicio", servicio);
                c.put("clienteId", "TELEFONICA_" + telDni);
                c.put("clienteNombre", telNombre);
                c.put("clienteApellidos", telApellidos);
                c.put("clienteDni", telDni);
                c.put("clienteTelefono", telTelefono);
                c.put("clienteEmail", telEmail);
                c.put("estado", "Pendiente");
                c.put("observaciones", notas);

                db.collection("citas").add(c);
            }
            Toast.makeText(this, "Cita telefónica guardada con éxito", Toast.LENGTH_SHORT).show();
            volverAInicio();
            return;
        }

        db.collection("usuarios").document(mAuth.getUid()).get().addOnSuccessListener(doc -> {
            for (String h : bloquesAReservar) {
                Map<String, Object> c = new HashMap<>();
                c.put("barbero", barbero);
                c.put("fecha", fecha);
                c.put("hora", h);
                c.put("servicio", servicio);
                c.put("clienteId", mAuth.getUid());
                c.put("clienteNombre", doc.getString("nombre"));
                c.put("clienteApellidos", doc.getString("apellidos"));
                c.put("clienteEmail", mAuth.getCurrentUser().getEmail());
                c.put("clienteTelefono", doc.getString("telefono"));
                c.put("estado", "Pendiente");
                c.put("observaciones", notas);

                if (citaIdExistente != null && h.equals(hora)) {
                    db.collection("citas").document(citaIdExistente).update(c);
                } else {
                    db.collection("citas").add(c);
                }
            }
            Toast.makeText(this, "Cita reservada con éxito", Toast.LENGTH_SHORT).show();
            volverAInicio();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show();
        });
    }

    private void volverAInicio() {
        Intent intent;
        if (modoTelefonico) {
            intent = new Intent(this, AdminPanelActivity.class);
        } else {
            intent = new Intent(this, SeleccionBarberoActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private List<String> calcularBloquesAReservar() {
        List<String> bloques = new ArrayList<>();
        List<String> horasBase = obtenerHorasBase();
        int index = horasBase.indexOf(hora);

        if ("Corte".equals(servicio) || "Barba".equals(servicio)) {
            bloques.add(hora);
        } else if ("Corte+Barba".equals(servicio)) {
            bloques.add(hora);
            if (index + 1 < horasBase.size()) bloques.add(horasBase.get(index + 1));
        } else if ("Decoloracion".equals(servicio)) {
            bloques.add(hora);
            if (index + 1 < horasBase.size()) bloques.add(horasBase.get(index + 1));
            if (index + 4 < horasBase.size()) bloques.add(horasBase.get(index + 4));
            if (index + 5 < horasBase.size()) bloques.add(horasBase.get(index + 5));
        }
        return bloques;
    }

    private List<String> obtenerHorasBase() {
        List<String> horasBase = new ArrayList<>();

        int diaSemana = Calendar.MONDAY;
        try {
            String[] partes = fecha.split("/");
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(partes[0]), Integer.parseInt(partes[1]) - 1, Integer.parseInt(partes[2]));
            diaSemana = cal.get(Calendar.DAY_OF_WEEK);
        } catch (Exception e) {}

        for (int h = 10; h <= 13; h++) {
            horasBase.add(h + ":00");
            horasBase.add(h + ":30");
        }
        if (diaSemana != Calendar.SATURDAY) {
            for (int h = 16; h <= 19; h++) {
                horasBase.add(h + ":00");
                horasBase.add(h + ":30");
            }
        }
        return horasBase;
    }
}
