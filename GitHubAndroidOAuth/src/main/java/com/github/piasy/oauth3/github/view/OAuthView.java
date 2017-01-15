package com.github.piasy.oauth3.github.view;

import com.github.piasy.oauth3.github.model.GitHubUser;

/**
 * Created by Piasy{github.com/Piasy} on 26/12/2016.
 */

public interface OAuthView {
    void authSuccess(String token, GitHubUser user);

    void authFail(int code, String error);

    void codeArrived(OAuthResult result);
}
