package utils;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.common.collect.ImmutableList;
import imports.Config;
import imports.RequestFields;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

public class TokenUtils {

    private static final String PUBLIC_RSA_KEY_URL = String.format(
        "https://cognito-idp.us-east-1.amazonaws.com/%s/.well-known/jwks.json", Config.COGNITO_USER_POOL);

    private static final List<String> LIVE_FUNCTIONS = ImmutableList.of("ProxyEndpoint");
    private static final String EMULATED_ACTIVE_USER_KEY = "EMULATED_ACTIVE_USER";
    private static final String cognitoClaim = "cognito:username";

    public static String getActiveUserFromRequest(APIGatewayProxyRequestEvent request, Context context)
        throws JwkException, MalformedURLException {
        String functionName = context.getFunctionName();

        if (!LIVE_FUNCTIONS.contains(functionName)) {
            String emulatedActiveUser = System.getenv(EMULATED_ACTIVE_USER_KEY);

            if (emulatedActiveUser != null) {
                return emulatedActiveUser;
            }
        }

        Map<String, String> headers = request.getHeaders();
        String authorization = headers.get(RequestFields.AUTHORIZATION);

        String token = authorization.substring(RequestFields.BEARER.length());

        DecodedJWT jwt = JWT.decode(token);

        JwkProvider provider = new UrlJwkProvider(new URL(PUBLIC_RSA_KEY_URL));
        Jwk jwk = provider.get(jwt.getKeyId());

        Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

        algorithm.verify(jwt);

        return jwt.getClaim(cognitoClaim).asString();
    }

}
