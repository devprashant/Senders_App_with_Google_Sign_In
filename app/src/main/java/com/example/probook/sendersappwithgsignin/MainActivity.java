package com.example.probook.sendersappwithgsignin;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener {

    LocationManager lm;
    LocationListener locationListener;
    PendingIntent pendingIntent;
    private TextView txtLatititude;
    private TextView txtLongitude;
    private EditText etZoom;
    private static final String PROX_ALERT_INTENT = "com.eaxmple.mau";

    //Firebase stuff
    private static final int RC_SIGN_IN = 9001;
    private static final String TAG = "MainActivity";
    private GoogleApiClient mGoogleApiClient;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mdatabase;
    private String userId;
    private long MIN_TIME_TO_UPDATE = 1;
    private float MIN_DISTANCE_TO_UPDATE = 0;
    private boolean userRegistered = false;


    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            txtLatititude.setText(String.valueOf(location.getLatitude()));
            txtLongitude.setText(String.valueOf(location.getLongitude()));

            String zoomValue = etZoom.getText().toString();
            //Vehicle Location
            VehicleLocation vehicleLocation = new VehicleLocation();
            vehicleLocation.setLatitude(String.valueOf(location.getLatitude()));
            vehicleLocation.setLongitude(String.valueOf(location.getLongitude()));

            // Vehicle Device Stats
            DeviceStats deviceStats = new DeviceStats(getBaseContext());

            if (userRegistered) {
                // Vehcile location update
                mdatabase.getReference().child("vehicles").child(userId).child("location")
                        .setValue(vehicleLocation)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "location updated");
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                ((TextView) findViewById(R.id.tvLoginError)).setText("Firebase vehicle location update failure: " + e.toString());
                            }
                        });

                // Vehicle Device stats update
                mdatabase.getReference().child("vehicles").child(userId).child("device_stats")
                        .setValue(deviceStats)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "stats updated");
                                }
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                ((TextView) findViewById(R.id.tvLoginError)).setText("Firebase vehicle stats update failure: " + e.toString());
                            }
                        });
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtLongitude = (TextView) findViewById(R.id.txtLong);
        txtLatititude = (TextView) findViewById(R.id.txtLat);
        etZoom = (EditText) findViewById(R.id.etZoom);

        // -- use the locationmanager class to obtain location data --
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        //
        mAuth = FirebaseAuth.getInstance();
        mdatabase = FirebaseDatabase.getInstance();

        mAuthListener = new FirebaseAuth.AuthStateListener(){

            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if(user != null){
                    ((TextView)findViewById(R.id.tvLoginStatus)).setText("Device Registered");
                    findViewById(R.id.btnSignout).setEnabled(true);
                    findViewById(R.id.btnRevokeAccess).setEnabled(true);
                    userRegistered = true;
                    writeNewUserToDB(user);
                } else {
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                    findViewById(R.id.btnSignout).setEnabled(false);
                    findViewById(R.id.btnRevokeAccess).setEnabled(false);
                    userRegistered = false;
                    ((TextView)findViewById(R.id.tvLoginStatus)).setText("Device not registered");
                }
            }
        };


        // Customize SignIn Button
        SignInButton signInButton = (SignInButton) findViewById(R.id.sign_in_button);
        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setScopes(gso.getScopeArray());

        findViewById(R.id.sign_in_button).setOnClickListener(this);
        findViewById(R.id.btnSignout).setOnClickListener(this);
        findViewById(R.id.btnRevokeAccess).setOnClickListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();

        locationListener = new MyLocationListener();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_TO_UPDATE, MIN_DISTANCE_TO_UPDATE, locationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        lm.removeUpdates(locationListener);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mAuthListener != null){
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void writeNewUserToDB(FirebaseUser user) {
        User newuser = new User(user.getDisplayName(), user.getEmail());
        userId = user.getUid();
        mdatabase.getReference().child("vehicles").child(user.getUid()).child("metadata")
                .setValue(newuser)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()){
                            Log.d(TAG, "vehicle added");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        ((TextView)findViewById(R.id.tvLoginError)).setText("Firebase new vehicle add failure: " + e.toString());
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        ((TextView)findViewById(R.id.tvLoginError)).setText("Connection Failed From Google Api Client");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                ((TextView)findViewById(R.id.tvLoginError)).setText("");
                signIn();
                break;
            case R.id.btnSignout:
                signOut();
                break;
            case R.id.btnRevokeAccess:
                try {
                    userRegistered = false;
                    Thread.sleep(3000); // Sleep app for 3 seconds so that update delivery to server stops before attempting to delete.
                    revokeAccess();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    //==============================================Revoke Access=========================================
    private void revokeAccess() {

        // Steps;
        // 1. Delete user data from database.
        // 2. Delete user auth data from auth dashboard.
        // 3. Revoke user acess from google sigin api.
        final FirebaseUser userToDelete = FirebaseAuth.getInstance().getCurrentUser();

        // 1.Delete user data from database
        // Remember , data can only be deleted in database if it follow the defined rules.
        mdatabase.getReference().child("vehicles").child(userToDelete.getUid())
                .setValue(null)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if(task.isSuccessful()) {
                            if(task.isSuccessful()){
                                Log.d(TAG, "user deleted from database");
                                // 2.Delete user from Dashboard

                                deleteUserFromAuthDashBoard(userToDelete);
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.d(TAG, "user delete failed: " + e);
                    }
                });
    }

    private void deleteUserFromAuthDashBoard(FirebaseUser userToDelete) {

        userToDelete.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    // 3.Revoke Access from Google SignIn API.
                    Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            ((TextView)findViewById(R.id.tvLoginStatus)).setText("Device Deregistered");
                            //findViewById(R.id.btnRevokeAccess).setEnabled(false);
                        }
                    });
                }
            }
        });
    }
    //==============================================/Revoke Access/=====================================
//================================================Sign Out==========================================
    private void signOut() {

        FirebaseAuth.getInstance().signOut();
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                ((TextView)findViewById(R.id.tvLoginStatus)).setText("Device Signed out");
            }
        });
    }
    //===============================================/Sign Out/=========================================
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN){
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if(result.isSuccess()){
            GoogleSignInAccount account = result.getSignInAccount();
            firebaseAuthWithGoogle(account);
        } else {
            ((TextView)findViewById(R.id.tvLoginError)).setText("Google Sign In Error:" + result.toString());
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){
                            ((TextView)findViewById(R.id.tvLoginError)).setText("Firebase Sign In Error: " + task.getException().toString());
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
                            @Override
                            public void onResult(@NonNull Status status) {
                                ((TextView)findViewById(R.id.tvLoginStatus)).setText("Please Login Again");
                            }
                        });
                    }
                });
    }
}
