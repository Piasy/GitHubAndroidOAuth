package com.github.piasy.oauth3.example;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import com.github.piasy.oauth3.github.GitHubOAuth;
import com.github.piasy.oauth3.github.model.GitHubUser;

public class MainActivity extends AppCompatActivity implements GitHubOAuth.Listener {

    private GitHubOAuth mGitHubOAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGitHubOAuth = new GitHubOAuth.Builder()
                .clientId("YOUR_CLIENT_ID")
                .clientSecret("YOUR_CLIENT_SECRET")
                .scope("YOUR_SCOPE")
                .redirectUrl("YOUR_REDIRECT_URL")
                .listener(this)
                .build();

        findViewById(R.id.mBtnStart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGitHubOAuth.authorize(getSupportFragmentManager());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGitHubOAuth.destroy();
    }

    @Override
    public void onSuccess(String token, GitHubUser user) {
        Log.d(GitHubOAuth.TAG, "onSuccess " + token + ", " + user);
        Toast.makeText(this, "onSuccess " + token, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFail(String error) {
        Toast.makeText(this, "onFail " + error, Toast.LENGTH_SHORT).show();
    }
}
