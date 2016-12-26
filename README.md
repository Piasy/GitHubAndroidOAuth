# GitHubAndroidOAuth
OAuth for GitHub in Android.

[ ![Download](https://api.bintray.com/packages/piasy/maven/GitHubAndroidOAuth/images/download.svg) ](https://bintray.com/piasy/maven/GitHubAndroidOAuth/_latestVersion)

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

compile 'com.github.piasy:GitHubAndroidOAuth:1.0.0'
```

### Java

``` java
mGitHubOAuth = new GitHubOAuth.Builder()
        .clientId("YOUR_CLIENT_ID")
        .clientSecret("YOUR_CLIENT_SECRET")
        .scope("YOUR_SCOPE")
        .redirectUrl("YOUR_REDIRECT_URL")
        .listener(this)
        .build();

mGitHubOAuth.authorize(getSupportFragmentManager());

@Override
public void onSuccess(String token) {
    Toast.makeText(this, "onSuccess " + token, Toast.LENGTH_SHORT).show();
}

@Override
public void onFail(String error) {
    Toast.makeText(this, "onFail " + error, Toast.LENGTH_SHORT).show();
}
```
