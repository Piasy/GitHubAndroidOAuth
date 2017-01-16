# GitHubAndroidOAuth
OAuth for GitHub in Android.

[ ![Download](https://api.bintray.com/packages/piasy/maven/GitHubAndroidOAuth/images/download.svg) ](https://bintray.com/piasy/maven/GitHubAndroidOAuth/_latestVersion)

[安卓基础：Activity/Fragment 销毁与重建](http://blog.piasy.com/2017/01/15/Android-Basics-Activity-Fragment-Kill-and-Recreate)。

![2017011551325GitHubOAuth_state_machine_normal.jpg](https://imgs.babits.top/2017011551325GitHubOAuth_state_machine_normal.jpg)

## Usage

### Dependency

``` gradle
allprojects {
    repositories {
        maven {
            url  "http://dl.bintray.com/piasy/maven"
        }
    }
}

compile 'com.github.piasy:GitHubAndroidOAuth:1.2.0'
```

### Java

``` java
mGitHubOAuth = GitHubOAuth.builder()
        .clientId("YOUR_CLIENT_ID")
        .clientSecret("YOUR_CLIENT_SECRET")
        .scope("YOUR_SCOPE")
        .redirectUrl("YOUR_REDIRECT_URL")
        .debug(true) // to see more log
        .build();

// `this` is Activity or Fragment (either support or not)
mGitHubOAuth.authorize(this);

// receive result in onActivityResult, you can use OnActivityResult to reduce boilerplate code
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
public void onAuthFail(@ExtraInt(name = GitHubOAuth.RESULT_KEY_ERROR_CODE) int errorCode
        , @ExtraString(name = GitHubOAuth.RESULT_KEY_ERROR) String error) {
    Toast.makeText(this, "onFail " + errorCode + ", " + error, Toast.LENGTH_SHORT).show();
}
```
