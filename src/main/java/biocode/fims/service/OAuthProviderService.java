package biocode.fims.service;

import biocode.fims.models.*;
import biocode.fims.repositories.OAuthClientRepository;
import biocode.fims.repositories.OAuthNonceRepository;
import biocode.fims.repositories.OAuthTokenRepository;
import biocode.fims.utils.StringGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static java.util.concurrent.TimeUnit.*;

/**
 * Service class for handling oAuth2 interactions
 */
@Service
public class OAuthProviderService {
    private static long NONCE_EXPIRATION_INTEVAL = MILLISECONDS.convert(10, MINUTES);
    public static long ACCESS_TOKEN_EXPIRATION_INTEVAL = SECONDS.convert(1, HOURS);
    private static long REFRESH_TOKEN_EXPIRATION_INTEVAL = SECONDS.convert(1, DAYS);

    private final OAuthClientRepository oAuthClientRepository;
    private final OAuthTokenRepository oAuthTokenRepository;
    private final OAuthNonceRepository oAuthNonceRepository;
    private final UserService userService;

    @Autowired
    public OAuthProviderService(OAuthClientRepository oAuthClientRepository, OAuthTokenRepository oAuthTokenRepository,
                                OAuthNonceRepository oAuthNonceRepository, UserService userService) {
        this.oAuthClientRepository = oAuthClientRepository;
        this.oAuthTokenRepository = oAuthTokenRepository;
        this.oAuthNonceRepository = oAuthNonceRepository;
        this.userService = userService;
    }

    /**
     * verify that the given code was issued for the same client id that is trying to exchange the code for an access
     * token
     *
     * @param clientId
     * @param code
     * @param redirectUri
     * @return
     */
    @Transactional(readOnly = true)
    public OAuthNonce getValidNonce(String clientId, String code, String redirectUri) {
        return oAuthNonceRepository.getOAuthNonce(clientId, code, redirectUri, NONCE_EXPIRATION_INTEVAL);
    }

    @Transactional(readOnly = true)
    public User getUser(String accessToken, String userEntityGraph) {
        return oAuthTokenRepository.getUser(accessToken, ACCESS_TOKEN_EXPIRATION_INTEVAL, userEntityGraph);
    }

    @Transactional(readOnly = true)
    public OAuthToken getOAuthToken(String refreshToken) {
        OAuthToken oAuthToken = oAuthTokenRepository.getOAuthToken(refreshToken, REFRESH_TOKEN_EXPIRATION_INTEVAL);

        if (oAuthToken != null && userService.userBelongsToInstanceProject(oAuthToken.getUser())) {
            return oAuthToken;
        }

        return null;
    }

    public OAuthToken generateToken(OAuthToken expiredOAuthToken) {
        OAuthToken newOAuthToken = generateToken(expiredOAuthToken.getoAuthClient(), expiredOAuthToken.getUser(), null);

        // refresh tokens are only good once, so delete the old access token so the refresh token can no longer be used
        oAuthTokenRepository.delete(expiredOAuthToken);
        return newOAuthToken;
    }

    public OAuthToken generateToken(OAuthClient oAuthClient, String state, OAuthNonce oAuthNonce) {
        User user = oAuthNonce.getUser();
        oAuthNonceRepository.delete(oAuthNonce);

        return generateToken(oAuthClient, user, state);
    }


    public OAuthToken generateToken(OAuthClient oAuthClient, User user, String state) {
        String token = StringGenerator.generateString(20);
        String refreshToken = StringGenerator.generateString(20);
        OAuthToken oAuthToken = new OAuthToken(token, refreshToken, oAuthClient, user);
        oAuthToken.setState(state);

        oAuthTokenRepository.save(oAuthToken);
        return oAuthToken;
    }

    @Transactional(readOnly = true)
    public OAuthClient getOAuthClient(String clientId, String clientSecret) {
        if (StringUtils.isEmpty(clientSecret)) {
            return oAuthClientRepository.findOneByClientId(clientId);
        } else {
            return oAuthClientRepository.findOneByClientIdAndClientSecret(clientId, clientSecret);
        }
    }

    public OAuthNonce generateNonce(OAuthClient oAuthClient, User user, String redirectUrl) {
        String code = StringGenerator.generateString(20);
        OAuthNonce oAuthNonce = new OAuthNonce(code, redirectUrl, user, oAuthClient);
        oAuthNonceRepository.save(oAuthNonce);
        return oAuthNonce;
    }

    public OAuthClient createOAuthClient(String callback) {
        OAuthClient oAuthClient = new OAuthClient.OAuthClientBuilder(callback).build();

        oAuthClientRepository.save(oAuthClient);
        return oAuthClient;
    }
}
