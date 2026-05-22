package com.example.a7san7issa.ui.admin;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.a7san7issa.R;
import com.example.a7san7issa.api.ApiClient;
import com.example.a7san7issa.api.ApiService;
import com.example.a7san7issa.utils.TokenManager;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.PercentFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminStatsFragment extends Fragment {

    private TextView tvTotalUsers, tvTotalDocs;
    private PieChart pieChartFilieres, pieChartPopularDocs, pieChartMatieres;
    private ApiService apiService;

    // Palette moderne
    private final int[] CHART_COLORS = new int[]{
            Color.parseColor("#22C55E"), Color.parseColor("#3B82F6"),
            Color.parseColor("#F59E0B"), Color.parseColor("#EC4899"),
            Color.parseColor("#8B5CF6"), Color.parseColor("#06B6D4"),
            Color.parseColor("#F97316"), Color.parseColor("#14B8A6"),
            Color.parseColor("#A855F7"), Color.parseColor("#EF4444")
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_admin_stats, container, false);

        TokenManager tokenManager = new TokenManager(getContext());
        apiService = ApiClient.getClient(tokenManager).create(ApiService.class);

        tvTotalUsers = v.findViewById(R.id.tvTotalUsers);
        tvTotalDocs = v.findViewById(R.id.tvTotalDocs);
        pieChartFilieres = v.findViewById(R.id.pieChartFilieres);
        pieChartPopularDocs = v.findViewById(R.id.pieChartPopularDocs);
        pieChartMatieres = v.findViewById(R.id.pieChartMatieres);

        setupPieChart(pieChartFilieres, null);
        setupPieChart(pieChartPopularDocs, "Documents populaires");
        setupPieChart(pieChartMatieres, "Matières populaires");

        loadStats();
        return v;
    }

    private void setupPieChart(PieChart chart, String centerText) {
        chart.setDrawHoleEnabled(true);
        chart.setHoleColor(Color.TRANSPARENT);
        chart.getDescription().setEnabled(false);
        chart.setUsePercentValues(true);
        if (centerText != null) chart.setCenterText(centerText);
        chart.setCenterTextSize(12f);
        chart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        chart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        chart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        chart.getLegend().setDrawInside(false);
    }

    private void loadStats() {
        apiService.getAdminStats().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> stats = response.body();
                    tvTotalUsers.setText(String.valueOf(stats.get("total_utilisateurs")));
                    tvTotalDocs.setText(String.valueOf(stats.get("total_documents")));

                    // 1. Filières (données simulées pour l'instant, à remplacer par un vrai endpoint)
                    List<PieEntry> filiereEntries = new ArrayList<>();
                    filiereEntries.add(new PieEntry(30f, "SM A"));
                    filiereEntries.add(new PieEntry(25f, "SM B"));
                    filiereEntries.add(new PieEntry(20f, "PC"));
                    filiereEntries.add(new PieEntry(15f, "SVT"));
                    filiereEntries.add(new PieEntry(10f, "ECO"));
                    fillPieChart(pieChartFilieres, filiereEntries);

                    // 2. Documents populaires
                    List<Map<String, Object>> popularDocs = (List<Map<String, Object>>) stats.get("documents_populaires");
                    if (popularDocs != null) {
                        List<PieEntry> docEntries = new ArrayList<>();
                        for (Map<String, Object> doc : popularDocs) {
                            int vues = ((Number) doc.get("nb_vues")).intValue();
                            String titre = (String) doc.get("titre");
                            docEntries.add(new PieEntry(vues, titre));
                        }
                        fillPieChart(pieChartPopularDocs, docEntries);
                    }

                    // 3. Matières populaires (appel au nouvel endpoint)
                    loadMatieresStats();
                } else {
                    Toast.makeText(getContext(), "Erreur stats", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(getContext(), "Erreur réseau", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadMatieresStats() {
        apiService.getStatsByMatiere().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call,
                                   Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<PieEntry> entries = new ArrayList<>();
                    for (Map<String, Object> item : response.body()) {
                        String matiere = (String) item.get("document__matiere");
                        int vues = ((Number) item.get("total_vues")).intValue();
                        entries.add(new PieEntry(vues, matiere));
                    }
                    fillPieChart(pieChartMatieres, entries);
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {}
        });
    }

    private void fillPieChart(PieChart chart, List<PieEntry> entries) {
        if (entries.isEmpty()) return;
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(CHART_COLORS);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);
        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(chart));
        chart.setData(data);
        chart.invalidate();
    }
}