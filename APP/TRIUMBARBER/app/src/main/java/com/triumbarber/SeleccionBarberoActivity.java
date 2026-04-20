package com.triumbarber;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class SeleccionBarberoActivity extends AppCompatActivity implements CitaAdapter.OnCitaListener {

    private RecyclerView rvCitasPendientes;
    private CitaAdapter adapter;
    private List<Cita> listaCitas = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccionbarbero);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        findViewById(R.id.btnFermin).setOnClickListener(v -> abrirCalendario("Fermín"));
        findViewById(R.id.btnJosemaria).setOnClickListener(v -> abrirCalendario("Josemaría"));

        findViewById(R.id.btnVerHistorico).setOnClickListener(v -> {
            startActivity(new Intent(this, HistoricoActivity.class));
        });

        findViewById(R.id.btnCerrarSesion).setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        rvCitasPendientes = findViewById(R.id.rvCitasUsuario);
        rvCitasPendientes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CitaAdapter(listaCitas, this);
        rvCitasPendientes.setAdapter(adapter);

        cargarCitasPendientes();
    }

    private void abrirCalendario(String barbero) {
        Intent intent = new Intent(this, CalendarioActivity.class);
        intent.putExtra("BARBERO_SELECCIONADO", barbero);
        startActivity(intent);
    }

    private void cargarCitasPendientes() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("citas")
                .whereEqualTo("clienteId", uid)
                .orderBy("fecha", Query.Direction.ASCENDING)
                .orderBy("hora", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaCitas.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Cita cita = doc.toObject(Cita.class);
                        cita.setId(doc.getId());
                        listaCitas.add(cita);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error al cargar citas pendientes", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarCitasPendientes();
    }

    @Override
    public void onAnularClick(int position) {
        Cita cita = listaCitas.get(position);
        db.collection("citas").document(cita.getId()).delete().addOnSuccessListener(a -> {
            Toast.makeText(this, "Cita anulada correctamente", Toast.LENGTH_SHORT).show();
            cargarCitasPendientes();
        });
    }

    @Override
    public void onModificarClick(int position) {
        Cita cita = listaCitas.get(position);
        Intent intent = new Intent(this, CalendarioActivity.class);
        intent.putExtra("CITA_ID", cita.getId());
        intent.putExtra("BARBERO_SELECCIONADO", cita.getBarbero());
        intent.putExtra("CITA_OBJETO", cita);
        startActivity(intent);
    }
}