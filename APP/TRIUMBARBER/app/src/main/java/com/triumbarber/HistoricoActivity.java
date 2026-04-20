package com.triumbarber;

import android.os.Bundle;
import android.util.Log;
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

public class HistoricoActivity extends AppCompatActivity {

    private RecyclerView rvHistorico;
    private CitaAdapter adapter;
    private List<Cita> listaCompleta = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        db = FirebaseFirestore.getInstance();
        rvHistorico = findViewById(R.id.rvHistorico);
        rvHistorico.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CitaAdapter(listaCompleta, null);
        rvHistorico.setAdapter(adapter);

        findViewById(R.id.btnVolverHistorico).setOnClickListener(v -> finish());

        cargarTodoElHistorial();
    }

    private void cargarTodoElHistorial() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        db.collection("citas")
                .whereEqualTo("clienteId", uid)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .orderBy("hora", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    listaCompleta.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Cita cita = doc.toObject(Cita.class);
                        cita.setId(doc.getId());
                        listaCompleta.add(cita);
                    }
                    adapter.notifyDataSetChanged();
                    if (listaCompleta.isEmpty()) {
                        Toast.makeText(this, "No tienes citas registradas", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "Error: " + e.getMessage());
                    Toast.makeText(this, "Error de carga. Verifica los índices de Firebase.", Toast.LENGTH_LONG).show();
                });
    }
}