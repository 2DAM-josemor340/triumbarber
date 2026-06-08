package com.triumbarber;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminPanelActivity extends AppCompatActivity implements CitaAdapter.OnCitaListener {

    private RecyclerView rvAdmin;
    private CitaAdapter adapter;
    private List<Cita> listaCitasOriginal = new ArrayList<>();
    private List<Cita> listaCitasMostrada = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView tvContador, tvTituloEstado;
    private String barberoFiltrado = null;
    private String adminNombre = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adminpanel);

        db = FirebaseFirestore.getInstance();

        rvAdmin = findViewById(R.id.rvAdmin);
        tvContador = findViewById(R.id.tvContadorCitas);
        tvTituloEstado = findViewById(R.id.tvTituloAdmin);

        rvAdmin.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CitaAdapter(listaCitasMostrada, this);
        adapter.setModoAdmin(true);
        rvAdmin.setAdapter(adapter);

        findViewById(R.id.btnVerTodasAdmin).setOnClickListener(v -> { barberoFiltrado = null; tvTituloEstado.setText("Todas las citas"); filtrarListaLocal(); });
        findViewById(R.id.btnFiltroFermin).setOnClickListener(v -> { barberoFiltrado = "Fermín"; tvTituloEstado.setText("Agenda de Fermín"); filtrarListaLocal(); });
        findViewById(R.id.btnFiltroJosemaria).setOnClickListener(v -> { barberoFiltrado = "Josemaría"; tvTituloEstado.setText("Agenda de Josemaría"); filtrarListaLocal(); });

        findViewById(R.id.btnCerrarSesionAdmin).setOnClickListener(v -> { FirebaseAuth.getInstance().signOut(); startActivity(new Intent(this, LoginActivity.class)); finish(); });
        findViewById(R.id.btnCitaTelefonicaAdmin).setOnClickListener(v -> startActivity(new Intent(this, CitaTelefonicaActivity.class)));
        findViewById(R.id.btnBloquearAgendaAdmin).setOnClickListener(v -> mostrarDialogoBloqueo());

        recuperarNombreAdmin();
        cargarCitasDesdeFirestore();
    }

    private void recuperarNombreAdmin() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("usuarios").document(uid).get().addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    adminNombre = doc.getString("nombre");
                }
            });
        }
    }

    private void cargarCitasDesdeFirestore() {
        db.collection("citas")
                .orderBy("fecha", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaCitasOriginal.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Cita cita = doc.toObject(Cita.class);
                        cita.setId(doc.getId());
                        listaCitasOriginal.add(cita);
                    }
                    filtrarListaLocal();
                })
                .addOnFailureListener(e -> Log.e("FIRESTORE", "Error al leer documentos", e));
    }

    private void filtrarListaLocal() {
        listaCitasMostrada.clear();

        listaCitasOriginal.sort((c1, c2) -> c1.getHora().compareTo(c2.getHora()));

        for (Cita cita : listaCitasOriginal) {
            boolean coincideBarbero = (barberoFiltrado == null || cita.getBarbero().equals(barberoFiltrado));
            boolean esPendiente = cita.getEstado() != null && !cita.getEstado().equalsIgnoreCase("HECHO");

            if (coincideBarbero && esPendiente) {
                boolean yaAgrupada = false;
                for (Cita mostrada : listaCitasMostrada) {
                    if (mostrada.getClienteId().equals(cita.getClienteId()) &&
                            mostrada.getFecha().equals(cita.getFecha()) &&
                            mostrada.getServicio().equals(cita.getServicio())) {

                        yaAgrupada = true;
                        break;
                    }
                }

                if (!yaAgrupada) {
                    listaCitasMostrada.add(cita);
                }
            }
        }
        adapter.notifyDataSetChanged();
        tvContador.setText("Total: " + listaCitasMostrada.size());
    }

    private void mostrarDialogoBloqueo() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            month = month + 1;
            String fechaStr = year + "/" + (month < 10 ? "0" + month : month) + "/" + (dayOfMonth < 10 ? "0" + dayOfMonth : dayOfMonth);
            verificarEstadoDia(fechaStr);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }

    private void verificarEstadoDia(String fechaStr) {
        String docId = fechaStr.replace("/", "-");
        db.collection("bloqueos").document(docId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                new AlertDialog.Builder(this)
                        .setTitle("Desbloquear Jornada")
                        .setMessage("Este día ya está bloqueado por " + doc.getString("bloqueadoPor") + " para " + doc.getString("barbero") + ". ¿Deseas rehabilitarlo?")
                        .setPositiveButton("Habilitar", (d, w) -> db.collection("bloqueos").document(docId).delete().addOnSuccessListener(a -> Toast.makeText(this, "Jornada habilitada", Toast.LENGTH_SHORT).show()))
                        .setNegativeButton("Cancelar", null).show();
            } else {
                String[] profesionales = {"Fermín", "Josemaría"};
                new AlertDialog.Builder(this)
                        .setTitle("Selecciona Profesional")
                        .setItems(profesionales, (d, position) -> {
                            Map<String, Object> b = new HashMap<>();
                            b.put("barbero", profesionales[position]);
                            b.put("fecha", fechaStr);
                            b.put("bloqueadoPor", adminNombre);
                            db.collection("bloqueos").document(docId).set(b).addOnSuccessListener(a -> Toast.makeText(this, "Día bloqueado con éxito", Toast.LENGTH_SHORT).show());
                        }).show();
            }
        });
    }

    @Override
    public void onHechoClick(int position) {
        Cita cita = listaCitasMostrada.get(position);
        db.collection("citas").document(cita.getId()).update("estado", "HECHO")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Servicio completado", Toast.LENGTH_SHORT).show();
                    cargarCitasDesdeFirestore();
                });
    }

    @Override
    public void onAnularClick(int position) {
        Cita cita = listaCitasMostrada.get(position);
        new AlertDialog.Builder(this)
                .setTitle("Anular Cita")
                .setMessage("¿Deseas eliminar la cita de " + cita.getClienteNombre() + "?")
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    db.collection("citas").document(cita.getId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Cita anulada correctamente", Toast.LENGTH_SHORT).show();
                                cargarCitasDesdeFirestore();
                            });
                })
                .setNegativeButton("Cancelar", null).show();
    }

    @Override
    public void onModificarClick(int position) {
        Cita cita = listaCitasMostrada.get(position);
        Intent intent = new Intent(this, SeleccionBarberoActivity.class);
        intent.putExtra("MODO_EDICION", true);
        intent.putExtra("CITA_ID", cita.getId());
        startActivity(intent);
    }
}