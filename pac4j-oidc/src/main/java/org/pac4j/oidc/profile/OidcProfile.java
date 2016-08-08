package org.pac4j.oidc.profile;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.profile.CommonProfile;
import org.pac4j.oidc.client.OidcClient;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>This class is the user profile for sites using OpenID Connect protocol.</p>
 * <p>It is returned by the {@link OidcClient}.</p>
 *
 * @author Michael Remond
 * @version 1.7.0
 */
public class OidcProfile<U extends JwtIdTokenProfile> extends CommonProfile implements Externalizable {

    private static final long serialVersionUID = -52855988661742374L;

    private transient static final String ACCESS_TOKEN = "access_token";
    private transient static final String ID_TOKEN = "id_token";
    private transient static final String REFRESH_TOKEN = "refresh_token";

    public OidcProfile() {
    }

    public void setAccessToken(BearerAccessToken accessToken) {
        addAttribute(ACCESS_TOKEN, accessToken);
    }

    public BearerAccessToken getAccessToken() {
        return (BearerAccessToken) getAttribute(ACCESS_TOKEN);
    }

    public String getIdTokenString() {
        return (String) getAttribute(ID_TOKEN);
    }

    public void setIdTokenString(String idTokenString) {
        addAttribute(ID_TOKEN, idTokenString);
    }

    public Optional<U> getIdToken() {
        if (getIdTokenString() != null) {
            try {
                final JWT jwt = JWTParser.parse(getIdTokenString());
                final CommonProfile profile = (CommonProfile) buildJwtIdTokenProfile();
                final JWTClaimsSet claims = jwt.getJWTClaimsSet();
                if (claims != null) {
                    final Map<String, Object> mapClaims = claims.getClaims();
                    for (final Map.Entry<String, Object> entry: mapClaims.entrySet()) {
                        final String key = entry.getKey();
                        final Object value = entry.getValue();
                        if (JwtIdTokenClaims.SUBJECT.equalsIgnoreCase(key)) {
                            profile.setId(value);
                        } else {
                            profile.addAttribute(key, value);
                        }
                    }
                }
                return Optional.of((U) profile);
            } catch (final ParseException e) {
                throw new TechnicalException(e);
            }
        }
        return Optional.empty();
    }

    protected U buildJwtIdTokenProfile() {
        return (U) new DefaultIdTokenProfile();
    }

    public String getRefreshTokenString() {
        return (String) getAttribute(REFRESH_TOKEN);
    }

    public void setRefreshTokenString(String refreshTokenString) {
        addAttribute(REFRESH_TOKEN, refreshTokenString);
    }

    @Override
    public void writeExternal(final ObjectOutput out) throws IOException {
        super.writeExternal(out);
        BearerAccessTokenBean bean = null; 
        if (getAccessToken() != null) {
            bean = BearerAccessTokenBean.toBean(getAccessToken());
        }
        out.writeObject(bean);
        out.writeObject(getIdTokenString());
        out.writeObject(getRefreshTokenString());
    }

    @Override
    public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        final BearerAccessTokenBean bean = (BearerAccessTokenBean) in.readObject();
        if (bean != null) {
            setAccessToken(BearerAccessTokenBean.fromBean(bean));
        }
        setIdTokenString((String) in.readObject());
        setRefreshTokenString((String) in.readObject());
    }

    @Override
    public void clearSensitiveData() {
        removeAttribute(ACCESS_TOKEN);
        removeAttribute(ID_TOKEN);
        removeAttribute(REFRESH_TOKEN);
    }

    private static class BearerAccessTokenBean implements Serializable {
        private static final long serialVersionUID = 7726472295622796149L;
        private String value;
        private long lifetime;
        private List<String> scope;

        public BearerAccessTokenBean(final String value, final long lifetime,
                                     final List<String> scope) {
            this.value = value;
            this.lifetime = lifetime;
            this.scope = scope;
        }

        public static BearerAccessTokenBean toBean(final BearerAccessToken token) {
            final Scope scope = token.getScope();
            if (scope != null) {
                return new BearerAccessTokenBean(token.getValue(),
                        token.getLifetime(), scope.toStringList());
            } else {
                return new BearerAccessTokenBean(token.getValue(),
                        token.getLifetime(), null);
            }
        }

        public static BearerAccessToken fromBean(final BearerAccessTokenBean token) {
            return new BearerAccessToken(token.value, token.lifetime,
                    Scope.parse(token.scope));
        }
    }
}
