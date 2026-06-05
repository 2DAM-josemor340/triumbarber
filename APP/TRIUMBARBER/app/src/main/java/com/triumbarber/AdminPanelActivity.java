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
        tvContador = findViewById(R.id.tvContadorCitas);
        tvTituloEstado = findViewById(R.id.tvTituloAdmin);
        rvAdmin = findViewById(R.id.rvAdmin);        

        rvAdmin.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CitaAdapter(listaCitasMostrada, this);
        adapter.setModoAdmin(true);
        rvAdmin.setAdapter(adapter);

        findViewById(R.id.layoutFerminAdmin).setOnClickListener(v -> aplicarFiltro("Fermín"));
        findViewById(R.id.layoutJosemariaAdmin).setOnClickListener(v -> aplicarFiltro("Josemaría"));
        findViewById(R.id.btnVerTodasAdmin).setOnClickListener(v -> aplicarFiltro(null));

        findViewById(R.id.btnCerrarSesionAdmin).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        obtenerNombreAdmin();

        findViewById(R.id.btnBloquearAgendaAdmin).setOnClickListener(v -> mostrarDialogoBloqueo());

        findViewById(R.id.btnCitaTelefonicaAdmin).setOnClickListener(v -> {
            Intent intent = new Intent(this, CitaTelefonicaActivity.class);
            startActivity(intent);
        });
        
        cargarCitasDesdeFirestore();
    }

    private void obtenerNombreAdmin() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid != null) {
            db.collection("usuarios").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    adminNombre = documentSnapshot.getString("nombre");
                }
            });
        }
    }

    private void mostrarDialogoBloqueo() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dpd = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String fechaSeleccionada = String.format("%02d/%02d/%04d", dayOfMonth, (month + 1), year);
            verificarEstadoDia(fechaSeleccionada);
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dpd.setTitle("Selecciona el día");
        dpd.show();
    }

    private void verificarEstadoDia(String fecha) {
        String docId = fecha.replace("/", "-") + "_" + adminNombre;
        db.collection("bloqueos").document(docId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                mostrarOpcionDesbloqueo(fecha, docId);
            } else {
                confirmarBloqueo(fecha);
            }
        });
    }

    private void mostrarOpcionDesbloqueo(String fecha, String docId) {
        new AlertDialog.Builder(this)
                .setTitle("Día Bloqueado")
                .setMessage("El día " + fecha + " ya está bloqueado para ti. ¿Quieres desbloquearlo?")
                .setPositiveButton("Desbloquear", (dialog, which) -> {
                    db.collection("bloqueos").document(docId).delete()
                            .addOnSuccessListener(a -> Toast.makeText(this, "Día desbloqueado con éxito", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Error al desbloquear", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void confirmarBloqueo(String fecha) {
        new AlertDialog.Builder(this)
                .setTitle("Bloquear Día")
                .setMessage("¿Estás seguro de que quieres bloquear el día " + fecha + " para tu agenda (" + adminNombre + ")? Los clientes no podrán pedirte cita este día.")
                .setPositiveButton("Bloquear", (dialog, which) -> {
                    Map<String, Object> bloqueo = new HashMap<>();
                    bloqueo.put("fecha", fecha);
                    bloqueo.put("barbero", adminNombre);
                    bloqueo.put("motivo", "Bloqueado por " + adminNombre);

                    String docId = fecha.replace("/", "-") + "_" + adminNombre;
                    db.collection("bloqueos").document(docId).set(bloqueo)
                            .addOnSuccessListener(a -> Toast.makeText(this, "Día bloqueado con éxito", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Error al bloquear día", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void filtrarListaLocal() {
        listaCitasMostrada.clear();

        for (Cita cita : listaCitasOriginal) {
            boolean coincideBarbero = (barberoFiltrado == null || cita.getBarbero().equals(barberoFiltrado));
            boolean esPendiente = cita.getEstado() != null && !cita.getEstado().equalsIgnoreCase("HECHO");
            if (coincideBarbero && esPendiente) {
                listaCitasMostrada.add(cita);
            }
        }
        adapter.notifyDataSetChanged();
        tvContador.setText("Total: " + listaCitasMostrada.size());
    }

    private void aplicarFiltro(String barbero) {
        this.barberoFiltrado = barbero;
        tvTituloEstado.setText(barbero == null ? "TODAS LAS CITAS" : "CITAS: " + barbero.toUpperCase());
        filtrarListaLocal();
    }

    private void cargarCitasDesdeFirestore() {
        db.collection("citas")
                .orderBy("fecha", Query.Direction.ASCENDING)
                .orderBy("hora", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaCitasOriginal.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Cita cita = document.toObject(Cita.class);
                        cita.setId(document.getId());
                        listaCitasOriginal.add(cita);
                    }
                    filtrarListaLocal();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_ERROR", e.getMessage());
                    Toast.makeText(AdminPanelActivity.this, "Error de carga. Revisa los índices de Firebase.", Toast.LENGTH_LONG).show();
                });
    }


    @Override
    public void onHechoClick(int position) {
        Cita cita = listaCitasMostrada.get(position);
        db.collection("citas").document(cita.getId())
                .update("estado", "HECHO")
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cita marcada como HECHO", Toast.LENGTH_SHORT).show();
                    cargarCitasDesdeFirestore();
                });
    }

    @Override
    public void onAnularClick(int position) {
        Cita cita = listaCitasMostrada.get(position);

        new AlertDialog.Builder(this)
                .setTitle("Anular Cita")
                .setMessage("¿Estás seguro de que deseas eliminar la cita de " + cita.getClienteNombre() + "?")
                .setPositiveButton("Confirmar", (dialog, which) -> {

                    db.collection("citas").document(cita.getId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Cita anulada correctamente", Toast.LENGTH_SHORT).show();

                                cargarCitasDesdeFirestore();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error al anular la cita", Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onModificarClick(int position) {
    }

}