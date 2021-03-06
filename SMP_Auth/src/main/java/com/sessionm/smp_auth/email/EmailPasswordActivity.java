package com.sessionm.smp_auth.email;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sessionm.api.SessionMError;
import com.sessionm.api.identity.IdentityListener;
import com.sessionm.api.identity.IdentityManager;
import com.sessionm.api.identity.UserListener;
import com.sessionm.api.identity.UserManager;
import com.sessionm.api.identity.data.SMPUser;
import com.sessionm.api.identity.data.SMPUserCreate;
import com.sessionm.smp_auth.BaseActivity;
import com.sessionm.smp_auth.R;
import com.sessionm.smp_auth.UserDetailsActivity;

import java.util.Set;

/**
 * Demonstrates the usage of the SMP SDK to create/authorize a user with email and password.
 */
public class EmailPasswordActivity extends BaseActivity implements
        View.OnClickListener {

    private static final String TAG = "EmailPassword";

    private TextView mStatusTextView;
    private TextView mDetailTextView;
    private TextView mAuthCodeTextView;
    private EditText mEmailField;
    private EditText mPasswordField;

    private IdentityManager identityManager;
    private IdentityListener identityListener;
    private UserManager userManager;
    private UserListener userListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emailpassword);

        // Views
        mStatusTextView = (TextView) findViewById(R.id.status);
        mDetailTextView = (TextView) findViewById(R.id.detail);
        mAuthCodeTextView = (TextView) findViewById(R.id.auth_code);
        mEmailField = (EditText) findViewById(R.id.field_email);
        mPasswordField = (EditText) findViewById(R.id.field_password);

        // Buttons
        findViewById(R.id.email_sign_in_button).setOnClickListener(this);
        findViewById(R.id.logged_in_auth_code).setOnClickListener(this);
        findViewById(R.id.email_create_account_button).setOnClickListener(this);
        findViewById(R.id.sign_out_button).setOnClickListener(this);

        findViewById(R.id.logged_in_view_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(EmailPasswordActivity.this, UserDetailsActivity.class));
            }
        });

        identityManager = IdentityManager.getInstance();
        userManager = UserManager.getInstance();

        identityListener = new IdentityListener() {
            @Override
            public void onAuthStateUpdated(IdentityManager.AuthState authState) {
                hideProgressDialog();
            }

            @Override
            public void onFailure(SessionMError sessionMError) {
                hideProgressDialog();
                Toast.makeText(EmailPasswordActivity.this, sessionMError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        userListener = new UserListener() {
            @Override
            public void onUserUpdated(SMPUser smpUser, Set<String> set) {
                if (smpUser != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + smpUser.getID());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                updateUI(smpUser);
            }

            @Override
            public void onFailure(SessionMError sessionMError) {
                Toast.makeText(EmailPasswordActivity.this, sessionMError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();
        identityManager.setListener(identityListener);
        userManager.setListener(userListener);
    }

    private void createAccount(String email, String password) {
        Log.d(TAG, "createAccount:" + email);
        if (!validateForm()) {
            return;
        }

        showProgressDialog();

        SMPUserCreate.Builder builder = new SMPUserCreate.Builder(email, password).lastName("LastName");
        SessionMError error = identityManager.createUser(builder.build());
        if (error != null) {
            hideProgressDialog();
            Toast.makeText(EmailPasswordActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void signIn(String email, String password) {
        Log.d(TAG, "signIn:" + email);
        if (!validateForm()) {
            return;
        }

        showProgressDialog();

        SessionMError error = identityManager.authenticateUser(email, password);
        if (error != null) {
            hideProgressDialog();
            Toast.makeText(EmailPasswordActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void requestAuthCode() {
        identityManager.setAuthCodeListener(new IdentityManager.AuthCodeListener() {
            @Override
            public void onAuthCodeRequested(final String authCode) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAuthCodeTextView.setText(authCode);
                    }
                });
            }

            @Override
            public void onFailure(SessionMError error) {

            }
        });
        SessionMError error = identityManager.requestAuthCode(null);
        if (error != null)
            Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
    }

    private void signOut() {
        identityManager.logOutUser();
        updateUI(null);
    }

    private boolean validateForm() {
        boolean valid = true;

        String email = mEmailField.getText().toString();
        if (TextUtils.isEmpty(email)) {
            mEmailField.setError("Required.");
            valid = false;
        } else {
            mEmailField.setError(null);
        }

        String password = mPasswordField.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError("Required.");
            valid = false;
        } else {
            mPasswordField.setError(null);
        }

        return valid;
    }

    private void updateUI(SMPUser smpUser) {
        hideProgressDialog();
        if (smpUser != null) {
            mStatusTextView.setText(getString(R.string.emailpassword_status_fmt, smpUser.getEmail()));
            mDetailTextView.setText(getString(R.string.smp_status_fmt, smpUser.getID()));
            mAuthCodeTextView.setText("Auth Code");

            findViewById(R.id.email_password_buttons).setVisibility(View.GONE);
            findViewById(R.id.email_password_fields).setVisibility(View.GONE);
            findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
            findViewById(R.id.logged_in_view_profile).setVisibility(View.VISIBLE);
            findViewById(R.id.logged_in_auth_code).setVisibility(View.VISIBLE);
        } else {
            mStatusTextView.setText(R.string.logged_out);
            mDetailTextView.setText(null);
            mAuthCodeTextView.setText(null);

            findViewById(R.id.email_password_buttons).setVisibility(View.VISIBLE);
            findViewById(R.id.email_password_fields).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_button).setVisibility(View.GONE);
            findViewById(R.id.logged_in_view_profile).setVisibility(View.GONE);
        }
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.email_create_account_button) {
            createAccount(mEmailField.getText().toString(), mPasswordField.getText().toString());
        } else if (i == R.id.email_sign_in_button) {
            signIn(mEmailField.getText().toString(), mPasswordField.getText().toString());
        } else if (i == R.id.logged_in_auth_code) {
            requestAuthCode();
        } else if (i == R.id.sign_out_button) {
            signOut();
        }
    }
}
