package com.example.a7san7issa.ui.main;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.a7san7issa.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Arrays;
import java.util.List;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private PlacesClient placesClient;
    private FusedLocationProviderClient fusedLocationClient;
    private FloatingActionButton fabMyLocation;
    private static final int LOCATION_PERMISSION_REQUEST = 1;

    private final LatLng[] fallbackLibraries = {
            new LatLng(33.5748, -7.6091),
            new LatLng(33.5885, -7.6202),
            new LatLng(33.5780, -7.6160),
            new LatLng(33.5830, -7.6050),
            new LatLng(33.5600, -7.5900)
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_maps, container, false);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyCedyND3YzvUtGdIy7q9iQx7gxRA9g7OWs");
        }
        placesClient = Places.createClient(requireContext());

        fabMyLocation = v.findViewById(R.id.fab_my_location);
        if (fabMyLocation != null) {
            fabMyLocation.setOnClickListener(view -> centerOnMyLocation());
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        return v;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng casablanca = new LatLng(33.5731, -7.5898);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(casablanca, 12));

        enableMyLocationLayer();
        searchLibrariesNearby();
    }

    private void enableMyLocationLayer() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    private void centerOnMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null && mMap != null) {
                        LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 15));
                    } else {
                        Toast.makeText(getContext(), "Position actuelle indisponible", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void searchLibrariesNearby() {
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            addFallbackMarkers();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    LatLng searchCenter;
                    if (location != null) {
                        searchCenter = new LatLng(location.getLatitude(), location.getLongitude());
                    } else {
                        searchCenter = new LatLng(33.5731, -7.5898);
                    }

                    LatLng southWest = new LatLng(searchCenter.latitude - 0.05, searchCenter.longitude - 0.05);
                    LatLng northEast = new LatLng(searchCenter.latitude + 0.05, searchCenter.longitude + 0.05);
                    RectangularBounds bounds = RectangularBounds.newInstance(southWest, northEast);

                    List<Place.Field> placeFields = Arrays.asList(
                            Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);

                    SearchByTextRequest request = SearchByTextRequest.builder("bibliothèque", placeFields)
                            .setLocationBias(bounds)
                            .setMaxResultCount(10)
                            .build();

                    placesClient.searchByText(request)
                            .addOnSuccessListener(response -> {
                                mMap.clear();
                                for (Place place : response.getPlaces()) {
                                    LatLng loc = place.getLatLng();
                                    if (loc != null) {
                                        mMap.addMarker(new MarkerOptions()
                                                .position(loc)
                                                .title(place.getName())
                                                .snippet(place.getAddress()));
                                    }
                                }
                                if (response.getPlaces().isEmpty()) {
                                    Toast.makeText(getContext(), "Aucune bibliothèque trouvée", Toast.LENGTH_SHORT).show();
                                    addFallbackMarkers();
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), "Erreur API – affichage manuel", Toast.LENGTH_SHORT).show();
                                }
                                addFallbackMarkers();
                            });
                })
                .addOnFailureListener(e -> addFallbackMarkers());
    }

    private void addFallbackMarkers() {
        if (mMap == null) return;
        mMap.clear();
        String[] names = {
                "Biblio. Fondation Roi Abdul-Aziz",
                "Médiathèque Mosquée Hassan II",
                "Bibliothèque Al Saoud",
                "Institut Français Casablanca",
                "Biblio. Université Hassan II"
        };
        for (int i = 0; i < fallbackLibraries.length; i++) {
            mMap.addMarker(new MarkerOptions()
                    .position(fallbackLibraries[i])
                    .title(names[i]));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocationLayer();
                searchLibrariesNearby();
                centerOnMyLocation();
            } else {
                Toast.makeText(getContext(), "Permission de localisation refusée", Toast.LENGTH_SHORT).show();
                addFallbackMarkers();
            }
        }
    }
}