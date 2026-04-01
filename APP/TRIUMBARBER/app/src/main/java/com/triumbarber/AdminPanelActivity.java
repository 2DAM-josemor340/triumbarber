package com.triumbarber;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;


public class AdminPanelActivity extends AppCompatActivity {

    private RecyclerView rvAdmin;
    private CitaAdapter adapter;
    private List<Cita> listaCitasOriginal = new ArrayList<>();
    private List<Cita> listaCitasMostrada = new ArrayList<>();
    private FirebaseFirestore db;
    private TextView tvTituloEstado;
    private String barberoFiltrado = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adminpanel);

        db = FirebaseFirestore.getInstance();
        tvTituloEstado = findViewById(R.id.tvTituloAdmin);
        rvAdmin = findViewById(R.id.rvAdmin);

        rvAdmin.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CitaAdapter(listaCitasMostrada);
        adapter.setModoAdmin(true);
        rvAdmin.setAdapter(adapter);

        findViewById(R.id.layoutFerminAdmin).setOnClickListener(v -> aplicarFiltro("Fermín"));
        findViewById(R.id.layoutJosemariaAdmin).setOnClickListener(v -> aplicarFiltro("Josemaría"));
        findViewById(R.id.btnVerTodasAdmin).setOnClickListener(v -> aplicarFiltro(null));

        cargarCitasDesdeFirestore();

    }

    private void filtrarListaLocal(String texto) {
        listaCitasMostrada.clear();
        String query = texto.toLowerCase().trim();

        for (Cita cita : listaCitasOriginal) {
            boolean coincideBarbero = (barberoFiltrado == null || cita.getBarbero().equals(barberoFiltrado));
            if (coincideBarbero) {
                String nombre = (cita.getClienteNombre() != null) ? cita.getClienteNombre().toLowerCase() : "";
                String email = (cita.getClienteEmail() != null) ? cita.getClienteEmail().toLowerCase() : "";
                if (query.isEmpty() || nombre.contains(query) || email.contains(query)) {
                    listaCitasMostrada.add(cita);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void aplicarFiltro(String barbero) {
        this.barberoFiltrado = barbero;
        tvTituloEstado.setText(barbero == null ? "TODAS LAS CITAS" : "CITAS: " + barbero.toUpperCase());
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
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE_ERROR", e.getMessage());
                    Toast.makeText(AdminPanelActivity.this, "Error de carga. Revisa los índices de Firebase.", Toast.LENGTH_LONG).show();
                });
    }

}