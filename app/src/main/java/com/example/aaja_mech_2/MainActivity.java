package com.example.aaja_mech_2;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.aaja_mech_2.Common.Common;
import com.example.aaja_mech_2.Model.RiderModel;
import com.firebase.ui.auth.AuthMethodPickerLayout;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;

public class MainActivity extends AppCompatActivity {


    //Variables
    Animation topAnim, bottomAnim;
    ImageView image;
    TextView logo_1, logo_2;

    private final static int LOGIN_REQUEST_CODE = 7171; //
    private List<AuthUI.IdpConfig> providers;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener listener;


    @BindView(R.id.progress_bar)
    ProgressBar progressBar;

    FirebaseDatabase database;
    DatabaseReference riderInfoRef;

    @Override
    protected void onStart(){
        super.onStart();
        delaySplashScreen();
    }

    private void delaySplashScreen() {
        progressBar.setVisibility(View.VISIBLE);
        Completable.timer(3, TimeUnit.SECONDS,
                AndroidSchedulers.mainThread())
                .subscribe(() ->

                        firebaseAuth.addAuthStateListener(listener)
                        );
    }

    @Override
    protected void onStop(){
        if(firebaseAuth != null && listener != null)
            firebaseAuth.removeAuthStateListener(listener);
        super.onStop();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Animations
        topAnim = AnimationUtils.loadAnimation(this,R.anim.top_animation);
        bottomAnim = AnimationUtils.loadAnimation(this, R.anim.bottom_animation);

        //Hooks
        image = findViewById(R.id.imageView);
        logo_1 = findViewById(R.id.textView1);
        logo_2 = findViewById(R.id.textView2);

        image.setAnimation(topAnim);
        logo_1.setAnimation(bottomAnim);
        logo_2.setAnimation(bottomAnim);

        init();



    }

    private void init() {
        ButterKnife.bind(this);

        database = FirebaseDatabase.getInstance();
        riderInfoRef = database.getReference(Common.RIDER_INFO_REFENCE);

        providers = Arrays.asList(
                new AuthUI.IdpConfig.PhoneBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build()
                );

        firebaseAuth = FirebaseAuth.getInstance();
        listener = myFirebaseAuth ->{
          FirebaseUser user =myFirebaseAuth.getCurrentUser();
          if(user != null){

              checkUserFromFirebase();
          }
          else{

              showLoginLayout();
          }
        };
    }

    private void showLoginLayout() {
        AuthMethodPickerLayout authMethodPickerLayout = new AuthMethodPickerLayout
                .Builder(R.layout.layout_sign_in)
                .setPhoneButtonId(R.id.btn_phone_sign_in)
                .setGoogleButtonId(R.id.btn_google_sign_in)
                .build();
        startActivityForResult(AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAuthMethodPickerLayout(authMethodPickerLayout)
        .setIsSmartLockEnabled(false)
        .setTheme(R.style.LoginTheme)
        .setAvailableProviders(providers)
        .build(),LOGIN_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == LOGIN_REQUEST_CODE){

            IdpResponse response =IdpResponse.fromResultIntent(data);
            if(resultCode == RESULT_OK){
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            }
            else{
                Toast.makeText(this, response.getError().getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkUserFromFirebase() {
        riderInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if(snapshot.exists()){
                            RiderModel riderModel = snapshot.getValue(RiderModel.class);
                            goToHomeActivity(riderModel);

                        }
                        else{
                            showRegisterLayout();
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(MainActivity.this,error.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void showRegisterLayout() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);
        View itemView = LayoutInflater.from(this).inflate(R.layout.layout_register, null);

        TextInputEditText edt_first_name = (TextInputEditText)itemView.findViewById(R.id.edt_first_name);
        TextInputEditText edt_last_name = (TextInputEditText)itemView.findViewById(R.id.edt_last_name);
        TextInputEditText edt_phone_number = (TextInputEditText)itemView.findViewById(R.id.edt_phone_number);

        Button btn_continue = (Button)itemView.findViewById(R.id.btn_register);

        //Set data
        if(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber() != null &&
                !TextUtils.isEmpty(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber()))
        edt_phone_number.setText(FirebaseAuth.getInstance().getCurrentUser().getPhoneNumber());

        //Set View
        builder.setView(itemView);
        AlertDialog dialog = builder.create();
        dialog.show();
        btn_continue.setOnClickListener(view -> {
                    if(TextUtils.isEmpty(edt_first_name.getText().toString()))
                    {
                        Toast.makeText(this, "Please enter first name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    else if(TextUtils.isEmpty(edt_last_name.getText().toString())){
                        Toast.makeText(this, "Please enter last name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    else if(TextUtils.isEmpty(edt_phone_number.getText().toString())){
                        Toast.makeText(this, "Please enter Phone Number", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    else
                    {
                        RiderModel model = new RiderModel();
                        model.setFirstName(edt_first_name.getText().toString());
                        model.setFirstName(edt_last_name.getText().toString());
                        model.setFirstName(edt_phone_number.getText().toString());


                        riderInfoRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                .setValue(model)
                                .addOnFailureListener(e ->
                                        {
                                            dialog.dismiss();
                                            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                        )
                                .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Register Successfully", Toast.LENGTH_SHORT).show();
                                        dialog.dismiss();
                                        goToHomeActivity(model);
                                });

                    }

        });

    }

    private void goToHomeActivity(RiderModel riderModel) {
        Common.currentRider = riderModel; // Please, dont forget it  !!!
        startActivity(new Intent(this, HomeActivity.class));
        finish();;
    }


}