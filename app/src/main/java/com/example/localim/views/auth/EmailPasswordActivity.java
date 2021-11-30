package com.example.localim.views.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.localim.views.MainActivity;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Objects;

import com.example.localim.R;

public class EmailPasswordActivity extends AppCompatActivity implements View.OnClickListener {

	private static final HashMap<String,Integer> ERRORS =  new HashMap<>();
	static {
		ERRORS.put("We have blocked all requests from this device due to unusual activity. Try again later. [ Access to this account has been temporarily disabled due to many failed login attempts. You can immediately restore it by resetting your password or you can try again later. ]",R.string.firebase_too_manny_attempts);
		ERRORS.put("A network error (such as timeout, interrupted connection or unreachable host) has occurred.", R.string.firebase_network_error);
		ERRORS.put("There is no user record corresponding to this identifier. The user may have been deleted.",R.string.firebase_no_mail);
		ERRORS.put("The given password is invalid. [ Password should be at least 6 characters ]", R.string.firebase_password_not_enough);
		ERRORS.put("The password is invalid or the user does not have a password.", R.string.firebase_invalid_password);
		ERRORS.put("The email address is badly formatted.", R.string.firebase_bad_email_address);
		ERRORS.put("The email address is already in use by another account.", R.string.firebase_address_already_used);
	}

	private static final String TAG = "EmailPasswordActivity";
	private EditText etMail, etPassword;
	private FirebaseAuth mAuth;
	private FirebaseAuth.AuthStateListener mAuthListener;
	private TextView tvProfile;
	private TextInputLayout mLayoutEmail, mLayoutPassword;

	/**
	 * Initialize the auth form and add a FirebaseAuth Listener to the activity
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_emailpassword);

		tvProfile = findViewById(R.id.profile);
		etMail = findViewById(R.id.et_email);
		etPassword = findViewById(R.id.et_password);
		mLayoutEmail = findViewById(R.id.layout_email);
		mLayoutPassword = findViewById(R.id.layout_password);

		findViewById(R.id.email_sign_in_button).setOnClickListener(this);
		findViewById(R.id.email_create_account_button).setOnClickListener(this);
		findViewById(R.id.sign_out_button).setOnClickListener(this);
		findViewById(R.id.verify_button).setOnClickListener(this);

		mAuth = FirebaseAuth.getInstance();
		mAuthListener = firebaseAuth -> {
			FirebaseUser user = firebaseAuth.getCurrentUser();
			if (user != null) {
				Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
			} else {
				Log.d(TAG, "onAuthStateChanged:signed_out");
			}
			updateUI(user);
		};
	}

	@Override
	public void onStart() {
		super.onStart();
		mAuth.addAuthStateListener(mAuthListener);
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mAuthListener != null) {
			mAuth.removeAuthStateListener(mAuthListener);
		}
	}

	@SuppressLint("NonConstantResourceId")
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.email_create_account_button:
				createAccount(etMail.getText().toString(), etPassword.getText().toString());
				break;
			case R.id.email_sign_in_button:
				signIn(etMail.getText().toString(), etPassword.getText().toString());
				break;
			case R.id.sign_out_button:
				signOut();
				break;
			case R.id.verify_button:
				findViewById(R.id.verify_button).setEnabled(false);
				final FirebaseUser firebaseUser = mAuth.getCurrentUser();
				Objects.requireNonNull(firebaseUser).sendEmailVerification().addOnCompleteListener(this, task -> {
					if (task.isSuccessful()) {
						Toast.makeText(
								EmailPasswordActivity.this, "Mail de vérification envoyé à " + firebaseUser.getEmail(), Toast.LENGTH_LONG
						).show();
					} else {
						Toast.makeText(EmailPasswordActivity.this, Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
					}
					findViewById(R.id.verify_button).setEnabled(true);
				});
				break;
		}
	}

	/**
	 * Create an account for a new user.
	 * If email and password fields are incorrectly filled, the user is invited to retry.
	 * A new user is then created in the Firebase Database at the child "/users"
	 *
	 * @param  email       Email taken by user
	 * @param  password    Password taken by user
	 */
	private void createAccount(String email, String password) {
		if (invalidateForm()) {
			return;
		}
		mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
			if (!task.isSuccessful()) {
				tvProfile.setTextColor(Color.RED);
				String error = task.getException().getMessage();
				if(ERRORS.containsKey(task.getException().getMessage())) {
					tvProfile.setText(ERRORS.get(error));
				} else {
					tvProfile.setText(error);
				}
			} else {
				tvProfile.setTextColor(Color.DKGRAY);
				Intent intent = new Intent(this, MainActivity.class);
				try {
					startActivity(intent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Connect the account of a user to the application.
	 * If not successful, an error in French (thanks to the HashMap class attribute) is returned
	 * If successful, user is redirected to the main application
	 *
	 * @param  email       Email taken by user
	 * @param  password    Password taken by user
	 */
	private void signIn(String email, String password) {
		if (invalidateForm()) {
			return;
		}
		mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
			Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());
			if (!task.isSuccessful()) {
				tvProfile.setTextColor(Color.RED);
				String error = Objects.requireNonNull(task.getException()).getMessage();
				if(ERRORS.containsKey(task.getException().getMessage())) {
					tvProfile.setText(ERRORS.get(error));
				} else {
					tvProfile.setText(error);
				}
			} else {
				tvProfile.setTextColor(Color.DKGRAY);
				tvProfile.setTextColor(Color.DKGRAY);
				Intent intent = new Intent(this, MainActivity.class);
				try {
					startActivity(intent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Disconnect the account of a user to the application.
	 */
	private void signOut() {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(R.string.logout);
		alert.setCancelable(false);
		alert.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
			mAuth.signOut();
			updateUI(null);
		});
		alert.setNegativeButton(R.string.cancel, (dialogInterface, i) -> dialogInterface.dismiss());
		alert.show();
	}

	/**
	 * Check if the two auth fields are not empty.
	 * If yes, return true
	 * If not, return false
	 */
	private boolean invalidateForm() {
		if (TextUtils.isEmpty(etMail.getText().toString())) {
			mLayoutEmail.setError(getResources().getString(R.string.required));
			return true;
		}
		if (TextUtils.isEmpty(etPassword.getText().toString())) {
			mLayoutPassword.setError(getResources().getString(R.string.required));
			return true;
		}
		mLayoutEmail.setError(null);
		mLayoutPassword.setError(null);
		return false;
	}

	private void updateUI(FirebaseUser user) {
		if (user != null) {
			if (!user.isEmailVerified()){
				tvProfile.append(getResources().getString(R.string.verification));
				findViewById(R.id.verify_button).setVisibility(View.VISIBLE);
			} else {
				tvProfile.append(getResources().getString(R.string.successful_authentification));
				findViewById(R.id.verify_button).setVisibility(View.GONE);
			}
			tvProfile.append("\n\n\n\n");
			tvProfile.append("Adresse mail : " + user.getEmail());
			tvProfile.append("\n\n");
			tvProfile.append("Firebase ID: " + user.getUid());

			findViewById(R.id.email_password_buttons).setVisibility(View.GONE);
			findViewById(R.id.email_password_fields).setVisibility(View.GONE);
			findViewById(R.id.description_inscription).setVisibility(View.GONE);
			findViewById(R.id.signout_zone).setVisibility(View.VISIBLE);
		} else {
			tvProfile.setText(null);
			findViewById(R.id.email_password_buttons).setVisibility(View.VISIBLE);
			findViewById(R.id.email_password_fields).setVisibility(View.VISIBLE);
			findViewById(R.id.signout_zone).setVisibility(View.GONE);
		}
	}
}