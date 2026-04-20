package com.triumbarber;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import java.util.List;

public class AdminPanelActivity extends AppCompatActivity implements CitaAdapter.OnCitaListener {

    private RecyclerView rvAdmin;
    private CitaAdapter adapter;
    private List<Cita> listaCitas = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView tvContador, tvTituloEstado;
    private String barberoFiltrado = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adminpanel);

        db = FirebaseFirestore.getInstance();
        tvContador = findViewById(R.id.tvContadorCitas);
        tvTituloEstado = findViewById(R.id.tvTituloAdmin);
        rvAdmin = findViewById(R.id.rvAdmin);

        rvAdmin.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CitaAdapter(listaCitas, this);
        rvAdmin.setAdapter(adapter);

        findViewById(R.id.layoutFerminAdmin).setOnClickListener(v -> aplicarFiltro("Fermín"));
        findViewById(R.id.layoutJosemariaAdmin).setOnClickListener(v -> aplicarFiltro("Josemaría"));
        findViewById(R.id.btnVerTodasAdmin).setOnClickListener(v -> aplicarFiltro(null));

        findViewById(R.id.btnCerrarSesionAdmin).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        cargarCitasDesdeFirestore();
    }

    private void aplicarFiltro(String barbero) {
        this.barberoFiltrado = barbero;
        tvTituloEstado.setText(barbero == null ? "TODAS LAS CITAS" : "CITAS: " + barbero.toUpperCase());
        cargarCitasDesdeFirestore();
    }

    private void cargarCitasDesdeFirestore() {
        db.collection("citas")
                .orderBy("fecha", Query.Direction.ASCENDING)
                .orderBy("hora", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaCitas.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Cita cita = document.toObject(Cita.class);
                        cita.setId(document.getId());

                        if (barberoFiltrado == null || cita.getBarbero().equals(barberoFiltrado)) {
                            listaCitas.add(cita);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    tvContador.setText("Total: " + listaCitas.size());
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_ERROR", e.getMessage());
                    Toast.makeText(AdminPanelActivity.this, "Error de orden/carga. Revisa el Logcat para el link del índice.", Toast.LENGTH_LONG).show();
                });
    }

    @Override
    public void onAnularClick(int position) {
        Cita cita = listaCitas.get(position);

        LinearLayout layoutDialogo = new LinearLayout(this);
        layoutDialogo.setOrientation(LinearLayout.VERTICAL);
        layoutDialogo.setPadding(40, 20, 40, 20);

        final EditText etMotivo = new EditText(this);
        etMotivo.setHint("Motivo de la anulación");

        final EditText etPropuesta = new EditText(this);
        etPropuesta.setHint("Propuesta de nueva fecha (ej: Mañana 11:00)");

        layoutDialogo.addView(etMotivo);
        layoutDialogo.addView(etPropuesta);

        new AlertDialog.Builder(this)
                .setTitle("Anular Cita")
                .setMessage("Se enviará un correo a " + cita.getClienteNombre())
                .setView(layoutDialogo)
                .setPositiveButton("Confirmar", (dialog, which) -> {
                    String motivo = etMotivo.getText().toString();
                    String propuesta = etPropuesta.getText().toString();

                    db.collection("citas").document(cita.getId()).delete()
                            .addOnSuccessListener(aVoid -> {
                                enviarNotificacionCliente(cita, "ANULADA", motivo, propuesta);
                                cargarCitasDesdeFirestore();
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onModificarClick(int position) {
        Cita cita = listaCitas.get(position);
        Intent intent = new Intent(this, CalendarioActivity.class);
        intent.putExtra("CITA_ID", cita.getId());
        intent.putExtra("BARBERO_SELECCIONADO", cita.getBarbero());
        intent.putExtra("CLIENTE_EMAIL", cita.getClienteEmail());
        intent.putExtra("CITA_OBJETO", cita);
        intent.putExtra("MODO_ADMIN", true);
        startActivity(intent);
    }

    private void enviarNotificacionCliente(Cita cita, String accion, String motivo, String propuesta) {
        String cuerpoEmail = "Hola " + cita.getClienteNombre() + ",\n\n" +
                "Tu cita ha sido " + accion + ".\n" +
                "Motivo: " + motivo + "\n" +
                "Te proponemos: " + propuesta + "\n\n" +
                "Saludos, TriumBarber.";

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + cita.getClienteEmail()));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Aviso de Cita TriumBarber");
        intent.putExtra(Intent.EXTRA_TEXT, cuerpoEmail);

        try {
            startActivity(Intent.createChooser(intent, "Notificando al cliente..."));
        } catch (Exception e) {
            Toast.makeText(this, "No hay apps de correo instaladas", Toast.LENGTH_SHORT).show();
        }
    }
}