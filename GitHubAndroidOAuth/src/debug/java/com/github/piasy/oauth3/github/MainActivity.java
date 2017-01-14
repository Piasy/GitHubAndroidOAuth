/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.piasy.oauth3.github;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;
import com.github.piasy.oauth3.github.model.GitHubUser;
import onactivityresult.ActivityResult;
import onactivityresult.Extra;
import onactivityresult.ExtraString;
import onactivityresult.OnActivityResult;

public class MainActivity extends AppCompatActivity {

    private GitHubOAuth mGitHubOAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGitHubOAuth = GitHubOAuth.builder()
                .clientId("YOUR_CLIENT_ID")
                .clientSecret("YOUR_CLIENT_SECRET")
                .scope("YOUR_SCOPE")
                .redirectUrl("YOUR_REDIRECT_URL")
                .debug(true)
                .build();

        findViewById(R.id.mBtnStart).setOnClickListener(
                v -> mGitHubOAuth.authorize(MainActivity.this));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        ActivityResult.onResult(requestCode, resultCode, data).into(this);
    }

    @OnActivityResult(requestCode = GitHubOAuth.OAUTH_REQ, resultCodes = RESULT_OK)
    public void onAuthSuccess(@ExtraString(name = GitHubOAuth.RESULT_KEY_TOKEN) String token,
            @Extra(name = GitHubOAuth.RESULT_KEY_USER) GitHubUser user) {
        Log.d(GitHubOAuth.TAG, "onSuccess " + token + ", " + user);
        Toast.makeText(this, "onSuccess " + token, Toast.LENGTH_SHORT).show();
    }

    @OnActivityResult(requestCode = GitHubOAuth.OAUTH_REQ, resultCodes = RESULT_CANCELED)
    public void onAuthFail(@ExtraString(name = GitHubOAuth.RESULT_KEY_ERROR) String error) {
        Toast.makeText(this, "onFail " + error, Toast.LENGTH_SHORT).show();
    }
}
