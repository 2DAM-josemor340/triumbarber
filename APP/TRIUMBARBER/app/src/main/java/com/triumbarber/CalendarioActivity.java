package com.triumbarber;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.GridView;
import android.widget.TextView;
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
    private TextView tvTituloBarbero;
    private String barberoSeleccionado, citaIdExistente, servicioSeleccionado;
    private String fechaSeleccionada = "", horaSeleccionada = "";
    private boolean modoTelefonico = false;
    private String telNombre, telApellidos, telDni, telTelefono, telEmail;
    private int diaSemanaSeleccionado;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final List<String> horasOcupadas = new ArrayList<>();
    private final Set<String> festivos2026 = new HashSet<>();
    private final Map<String, String> bloqueosAdmin = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendario);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        calendarView = findViewById(R.id.calendarView);
        gridHoras = findViewById(R.id.gridHoras);
        tvTituloBarbero = findViewById(R.id.tvTituloBarbero);

        barberoSeleccionado = getIntent().getStringExtra("BARBERO_SELECCIONADO");
        servicioSeleccionado = getIntent().getStringExtra("SERVICIO_SELECCIONADO");
        citaIdExistente = getIntent().getStringExtra("CITA_ID");

        if (barberoSeleccionado != null) {
            tvTituloBarbero.setText("Cita con: " + barberoSeleccionado);
        }

        calendarView.setMinDate(System.currentTimeMillis() - 1000);
        cargarFestivos2026();
        cargarBloqueosAdmin();

        calendarView.setOnDateChangeListener((v, year, month, dayOfMonth) -> {
            fechaSeleccionada = String.format("%04d/%02d/%02d", year, (month + 1), dayOfMonth);
            validarDiaSeleccionado(year, month, dayOfMonth);
        });

        modoTelefonico = getIntent().getBooleanExtra("MODO_TELEFONICO", false);
        if (modoTelefonico) {
            telNombre = getIntent().getStringExtra("TEL_NOMBRE");
            telApellidos = getIntent().getStringExtra("TEL_APELLIDOS");
            telDni = getIntent().getStringExtra("TEL_DNI");
            telTelefono = getIntent().getStringExtra("TEL_TELEFONO");
            telEmail = getIntent().getStringExtra("TEL_EMAIL");
        }

        findViewById(R.id.btnConfirmarCita).setOnClickListener(v -> validarYGuardarCita());
    }

    private void cargarBloqueosAdmin() {
        db.collection("bloqueos").get().addOnSuccessListener(queryDocumentSnapshots -> {
            bloqueosAdmin.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String fecha = doc.getString("fecha");
                String barbero = doc.getString("barbero");
                if (fecha != null) {
                    if (barbero == null || barbero.isEmpty()) {
                        bloqueosAdmin.put(fecha + "_TODOS", "TODOS");
                    } else {
                        bloqueosAdmin.put(fecha + "_" + barbero, barbero);
                    }
                }
            }
            if (!fechaSeleccionada.isEmpty()) {
                validarDiaSeleccionadoActual();
            }
        });
    }

    private void validarDiaSeleccionadoActual() {
        String[] partes = fechaSeleccionada.split("/");
        if (partes.length == 3) {
            int y = Integer.parseInt(partes[0]);
            int m = Integer.parseInt(partes[1]) - 1;
            int d = Integer.parseInt(partes[2]);
            validarDiaSeleccionado(y, m, d);
        }
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
        } else if (bloqueosAdmin.containsKey(fechaSeleccionada + "_TODOS") || 
                   bloqueosAdmin.containsKey(fechaSeleccionada + "_" + barberoSeleccionado)) {
            bloquearDia("El barbero " + barberoSeleccionado + " no está disponible este día.");
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
            if (esHoraDisponibleSegunServicio(h, horasBase)) {
                horasMostrables.add(h);
            }
        }

        if (horasMostrables.isEmpty()) {
            bloquearDia("No hay horas disponibles para " + servicioSeleccionado);
        } else {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_hora, R.id.tvHoraItem, horasMostrables);
            gridHoras.setAdapter(adapter);
            gridHoras.setOnItemClickListener((p, v, pos, id) -> {
                horaSeleccionada = horasMostrables.get(pos);
                Toast.makeText(this, "Seleccionado: " + horaSeleccionada + " (" + servicioSeleccionado + ")", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private boolean esHoraDisponibleSegunServicio(String horaInicio, List<String> horasBase) {
        if (horasOcupadas.contains(horaInicio)) return false;

        int index = horasBase.indexOf(horaInicio);

        if ("Corte".equals(servicioSeleccionado) || "Barba".equals(servicioSeleccionado)) {
            return true;
        } else if ("Corte+Barba".equals(servicioSeleccionado)) {
            if (index + 1 >= horasBase.size()) return false;
            String siguienteHora = horasBase.get(index + 1);
            return !horasOcupadas.contains(siguienteHora);
        } else if ("Decoloracion".equals(servicioSeleccionado)) {
            if (index + 1 >= horasBase.size()) return false;
            if (horasOcupadas.contains(horasBase.get(index + 1))) return false;
            if (index + 5 >= horasBase.size()) return false;
            if (horasOcupadas.contains(horasBase.get(index + 4))) return false;
            if (horasOcupadas.contains(horasBase.get(index + 5))) return false;

            return true;
        }
        return true;
    }

    private void validarYGuardarCita() {
        if (fechaSeleccionada.isEmpty()) {
            Toast.makeText(this, "Selecciona una fecha en el calendario", Toast.LENGTH_SHORT).show();
            return;
        }
        if (horaSeleccionada.isEmpty()) {
            Toast.makeText(this, "Selecciona una hora", Toast.LENGTH_SHORT).show();
            return;
        }

        if (modoTelefonico) {
            ejecutarGuardado();
            return;
        }

        db.collection("citas")
                .whereEqualTo("clienteId", mAuth.getUid())
                .whereEqualTo("fecha", fechaSeleccionada)
                .get().addOnCompleteListener(taskUser -> {
                    if (taskUser.isSuccessful() && taskUser.getResult() != null) {
                        boolean yaTieneCitaEseDia = false;
                        for (QueryDocumentSnapshot doc : taskUser.getResult()) {
                            if (citaIdExistente == null || !doc.getId().equals(citaIdExistente)) {
                                yaTieneCitaEseDia = true;
                                break;
                            }
                        }

                        if (yaTieneCitaEseDia) {
                            Toast.makeText(this, "Ya tienes una cita reservada para este día. Solo se permite una por día.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        db.collection("citas")
                                .whereEqualTo("barbero", barberoSeleccionado)
                                .whereEqualTo("fecha", fechaSeleccionada)
                                .whereEqualTo("hora", horaSeleccionada)
                                .get().addOnCompleteListener(taskBarber -> {
                                    if (taskBarber.isSuccessful() && taskBarber.getResult() != null) {
                                        boolean ocupada = false;
                                        for (QueryDocumentSnapshot doc : taskBarber.getResult()) {
                                            if (citaIdExistente == null || !doc.getId().equals(citaIdExistente)) {
                                                ocupada = true;
                                                break;
                                            }
                                        }
                                        if (ocupada) {
                                            Toast.makeText(this, "¡Error! Esta hora se acaba de ocupar con este barbero.", Toast.LENGTH_LONG).show();
                                            consultarDisponibilidadBarbero(fechaSeleccionada);
                                        } else {
                                            ejecutarGuardado();
                                        }
                                    }
                                });
                    }
                });
    }

    private void ejecutarGuardado() {
        Intent intent = new Intent(this, ConfirmacionActivity.class);
        intent.putExtra("BARBERO", barberoSeleccionado);
        intent.putExtra("FECHA", fechaSeleccionada);
        intent.putExtra("HORA", horaSeleccionada);
        intent.putExtra("SERVICIO", servicioSeleccionado);
        intent.putExtra("CITA_ID", citaIdExistente);

        if (modoTelefonico) {
            intent.putExtra("MODO_TELEFONICO", true);
            intent.putExtra("TEL_NOMBRE", telNombre);
            intent.putExtra("TEL_APELLIDOS", telApellidos);
            intent.putExtra("TEL_DNI", telDni);
            intent.putExtra("TEL_TELEFONO", telTelefono);
            intent.putExtra("TEL_EMAIL", telEmail);
        }

        startActivity(intent);
    }

    private void bloquearDia(String mensaje) {
        gridHoras.setAdapter(null);
        horaSeleccionada = "";
        Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show();
    }
}
