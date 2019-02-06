package ie.ianduffy.keycloak.ext.auth.magiclink;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailSenderProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public class MagicLinkFormAuthenticator extends AbstractUsernameFormAuthenticator implements Authenticator {

    @Override
    public void action(AuthenticationFlowContext context) {
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        String email = formData.getFirst("email");

        UserModel user = context.getSession().users().getUserByEmail(email, context.getRealm());
        if (user == null) {
            // Register user
            user = context.getSession().users().addUser(context.getRealm(), email);
            user.setEnabled(true);
            user.setUsername(email);
            user.setEmail(email);
            user.setEmailVerified(true);
            user.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
        }

        String key = KeycloakModelUtils.generateId();
        context.getAuthenticationSession().setAuthNote("email-key", key);

        String link = KeycloakUriBuilder.fromUri(context.getRefreshExecutionUrl()).queryParam("key", key).build().toString();
        String body = "<a href=\"" + link + "\">Click to login</a>";
        try {
            context.getSession().getProvider(EmailSenderProvider.class).send(context.getRealm().getSmtpConfig(), user, "Login link", null, body);
        } catch (EmailException e) {
            e.printStackTrace();
        }

        context.setUser(user);
        context.challenge(context.form().createForm("view-email.ftl"));
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        String sessionKey = context.getAuthenticationSession().getAuthNote("email-key");
        if (sessionKey != null) {
            String requestKey = context.getHttpRequest().getUri().getQueryParameters().getFirst("key");
            if (requestKey != null) {
                if (requestKey.equals(sessionKey)) {
                    context.success();
                } else {
                    context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
                }
            } else {
                context.challenge(context.form().createForm("view-email.ftl"));
            }
        } else {
            context.challenge(context.form().createForm("login-email-only.ftl"));
        }
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }

}
