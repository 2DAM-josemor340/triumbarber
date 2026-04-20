package com.triumbarber;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.GridView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class CalendarioActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private GridView gridHoras;
    private String barberoSeleccionado, citaIdExistente, motivoCambio;
    private String fechaSeleccionada = "", horaSeleccionada = "";
    private int diaSemanaSeleccionado;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final List<String> horasOcupadas = new ArrayList<>();
    private final Set<String> festivos2026 = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        calendarView = findViewById(R.id.calendarView);
        gridHoras = findViewById(R.id.gridHoras);

        barberoSeleccionado = getIntent().getStringExtra("BARBERO_SELECCIONADO");
        citaIdExistente = getIntent().getStringExtra("CITA_ID");
        motivoCambio = getIntent().getStringExtra("MOTIVO_CAMBIO");

        calendarView.setMinDate(System.currentTimeMillis() - 1000);
        cargarFestivos2026();

        calendarView.setOnDateChangeListener((v, year, month, dayOfMonth) -> {
            fechaSeleccionada = String.format("%04d/%02d/%02d", year, (month + 1), dayOfMonth);
            validarDiaSeleccionado(year, month, dayOfMonth);
        });

        findViewById(R.id.btnConfirmarCita).setOnClickListener(v -> validarYGuardarCita());
    }

    private void cargarFestivos2026() {
        festivos2026.add("2026/01/01");
        festivos2026.add("2026/01/06");
        festivos2026.add("2026/05/01");
        festivos2026.add("2026/10/12");
        festivos2026.add("2026/12/25");
    }

    private void validarDiaSeleccionado(int y, int m, int d) {
        Calendar cal = Calendar.getInstance();
        cal.set(y, m, d);
        diaSemanaSeleccionado = cal.get(Calendar.DAY_OF_WEEK);

        if (diaSemanaSeleccionado == Calendar.SUNDAY) {
            bloquearDia("Cerrado los domingos.");
        } else if (festivos2026.contains(fechaSeleccionada)) {
            bloquearDia("Día festivo nacional.");
        } else {
            consultarDisponibilidadBarbero(fechaSeleccionada);
        }
    }

    private void consultarDisponibilidadBarbero(String fecha) {
        db.collection("citas")
                .whereEqualTo("barbero", barberoSeleccionado)
                .whereEqualTo("fecha", fecha)
                .get().addOnCompleteListener(task -> {
                    horasOcupadas.clear();
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            if (citaIdExistente != null && doc.getId().equals(citaIdExistente)) continue;
                            horasOcupadas.add(doc.getString("hora"));
                        }
                    }
                    generarHorarioComercial();
                });
    }

    private void generarHorarioComercial() {
        List<String> horasBase = new ArrayList<>();
        for (int h = 10; h <= 13; h++) {
            horasBase.add(h + ":00");
            horasBase.add(h + ":30");
        }
        if (diaSemanaSeleccionado != Calendar.SATURDAY) {
            for (int h = 16; h <= 19; h++) {
                horasBase.add(h + ":00");
                horasBase.add(h + ":30");
            }
        }

        List<String> horasMostrables = new ArrayList<>();
        for (String h : horasBase) {
            if (!horasOcupadas.contains(h)) horasMostrables.add(h);
        }

        if (horasMostrables.isEmpty()) {
            bloquearDia("No hay horas disponibles.");
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_hora, R.id.tvHoraItem, horasMostrables);
            gridHoras.setAdapter(adapter);
            gridHoras.setOnItemClickListener((p, v, pos, id) -> {
                horaSeleccionada = horasMostrables.get(pos);
                Toast.makeText(this, "Seleccionado: " + horaSeleccionada, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void validarYGuardarCita() {
        if (horaSeleccionada.isEmpty()) {
            Toast.makeText(this, "Selecciona una hora", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("citas")
                .whereEqualTo("barbero", barberoSeleccionado)
                .whereEqualTo("fecha", fechaSeleccionada)
                .whereEqualTo("hora", horaSeleccionada)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean ocupada = false;
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            if (citaIdExistente == null || !doc.getId().equals(citaIdExistente)) {
                                ocupada = true;
                                break;
                            }
                        }
                        if (ocupada) {
                            Toast.makeText(this, "¡Error! Esta hora se acaba de ocupar.", Toast.LENGTH_LONG).show();
                            consultarDisponibilidadBarbero(fechaSeleccionada);
                        } else {
                            ejecutarGuardado();
                        }
                    }
                });
    }

    private void ejecutarGuardado() {
        db.collection("usuarios").document(mAuth.getUid()).get().addOnSuccessListener(doc -> {
            Map<String, Object> c = new HashMap<>();
            c.put("barbero", barberoSeleccionado);
            c.put("fecha", fechaSeleccionada);
            c.put("hora", horaSeleccionada);
            c.put("clienteId", mAuth.getUid());
            c.put("clienteNombre", doc.getString("nombre"));
            c.put("clienteEmail", mAuth.getCurrentUser().getEmail());
            c.put("estado", "Pendiente");

            if (citaIdExistente != null) {
                db.collection("citas").document(citaIdExistente).update(c).addOnSuccessListener(a -> finish());
            } else {
                db.collection("citas").add(c).addOnSuccessListener(a -> finish());
            }
        });
    }

    private void bloquearDia(String mensaje) {
        gridHoras.setAdapter(null);
        horaSeleccionada = "";
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }
}