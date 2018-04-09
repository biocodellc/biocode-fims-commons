package biocode.fims.rest.services.rest;

import biocode.fims.application.config.FimsProperties;
import biocode.fims.auth.BasicAuthDecoder;
import biocode.fims.models.OAuthClient;
import biocode.fims.models.OAuthNonce;
import biocode.fims.models.OAuthToken;
import biocode.fims.models.User;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.rest.FimsService;
import biocode.fims.service.OAuthProviderService;
import biocode.fims.service.UserService;
import biocode.fims.utils.ErrorInfo;
import biocode.fims.utils.QueryParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * REST interface for handling user authentication
 * @exclude
 * @resourceTag Authentication
 */
public abstract class FimsAbstractAuthenticationController extends FimsService {
    @Context
    private HttpServletRequest request;
    private static Logger logger = LoggerFactory.getLogger(FimsAbstractAuthenticationController.class);

    protected final UserService userService;
    private final OAuthProviderService oAuthProviderService;

    @Autowired
    public FimsAbstractAuthenticationController(OAuthProviderService oAuthProviderService,
                                                UserService userService, FimsProperties props) {
        super(props);
        this.oAuthProviderService = oAuthProviderService;
        this.userService = userService;
    }

    /**
     * Service to log a user into the biocode-fims system
     *
     * @param usr
     * @param pass
     * @param return_to the url to return to after login
     * @throws IOException
     */

    @Deprecated
    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(@FormParam("username") String usr,
                          @FormParam("password") String pass,
                          @QueryParam("return_to") String returnTo,
                          @Context HttpServletResponse res) {

        if (!usr.isEmpty() && !pass.isEmpty()) {
            User user = userService.getUser(usr, pass);

            if (user != null) {
                // Place the user in the session
                session.setAttribute("user", user);

                // Check if the user is an admin for any projects
                if (userService.isAProjectAdmin(user, props.appRoot())) {
                    session.setAttribute("projectAdmin", true);
                }

                // Check if the user has created their own password, if they are just using the temporary password, inform the user to change their password
                if (!user.getHasSetPassword()) {
                    return Response.ok("{\"url\": \"" + props.appRoot() + "secure/profile?error=Update Your Password" +
                            new QueryParams().getQueryParams(request.getParameterMap(), false) + "\"}")
                            .build();
                }

                // Redirect to returnTo uri if provided
                if (returnTo != null) {

                    // check to see if oAuthLogin is in the session and set to true is so.
                    Object oAuthLogin = session.getAttribute("oAuthLogin");
                    if (oAuthLogin != null) {
                        session.setAttribute("oAuthLogin", true);
                    }

                    return Response.ok("{\"url\": \"" + returnTo +
                            new QueryParams().getQueryParams(request.getParameterMap(), true) + "\"}")
                            .build();
                } else {
                    return Response.ok("{\"url\": \"" + props.appRoot() + "\"}").build();
                }
            }
            // stored and entered passwords don't match, invalidate the session to be sure that a user is not in the session
            else {
                session.invalidate();
            }
        }

        return Response.status(400)
                .entity(new ErrorInfo("Bad Credentials", 400).toJSON())
                .build();
    }

    /**
     * Service for a client app to log a user into the system via oAuth.
     *
     * @param clientId
     * @param redirectURL
     * @param state
     */
    @GET
    @Path("/oauth/authorize")
    @Produces(MediaType.TEXT_HTML)
    public Response authorize(@QueryParam("client_id") String clientId,
                              @QueryParam("redirect_uri") String redirectURL,
                              @QueryParam("state") String state) {
        Object sessionoAuthLogin = session.getAttribute("oAuthLogin");
        Boolean oAuthLogin = false;

        // oAuthLogin is used to force the user to re-authenticate for oAuth
        if (sessionoAuthLogin != null && ((Boolean) sessionoAuthLogin)) {
            oAuthLogin = true;
        }

        OAuthClient oAuthClient = oAuthProviderService.getOAuthClient(clientId, null);
        if (oAuthClient == null) {
            if (redirectURL != null) {
                redirectURL += "?error=unauthorized_client";
                try {
                    return Response.status(302).location(new URI(redirectURL)).build();
                } catch (URISyntaxException ignored) {
                }
            }
            throw new BadRequestException("invalid_request", "invalid redirect_uri provided");
        }

        if (redirectURL == null) {
            try {
                return Response.status(302).location(new URI(oAuthClient.getCallback() + "?error=invalid_request")).build();
            } catch (URISyntaxException e) {
                logger.warn("Malformed callback URI for oAuth client {} and callback {}", clientId, oAuthClient.getCallback());
            }
            throw new BadRequestException("invalid_request");
        }

        if (userContext.getUser() == null || !oAuthLogin) {
            session.setAttribute("oAuthLogin", "false");
            // need the user to login
            try {
                return Response.status(Response.Status.TEMPORARY_REDIRECT)
                        .location(new URI(props.appRoot()+ "login?return_to=/id/authenticationService/oauth/authorize?"
                                + request.getQueryString()))
                        .build();
            } catch (URISyntaxException e) {
                throw new ServerErrorException(e);
            }
        }
        //TODO ask user if they want to share profile information with requesting party
        OAuthNonce oAuthNonce = oAuthProviderService.generateNonce(oAuthClient, userContext.getUser(), redirectURL);

        // no longer need oAuthLogin session attribute
        session.removeAttribute("oAuthLogin");

        redirectURL += "?code=" + oAuthNonce.getCode();

        if (state != null) {
            redirectURL += "&state=" + state;
        }
        try {
            return Response.status(302)
                    .location(new URI(redirectURL))
                    .build();
        } catch (URISyntaxException e) {
            throw new BadRequestException("invalid_request", "invalid redirect_uri provided");
        }
    }

    /**
     * Service for a client app to exchange an oAuth code for an access token
     *
     * @param code
     * @param clientId
     * @param clientSecret
     * @param redirectUri
     * @param state
     * @return
     */
    @POST
    @Path("/oauth/accessToken")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response access_token(@FormParam("code") String code,
                                 @FormParam("client_id") String clientId,
                                 @FormParam("client_secret") String clientSecret,
                                 @FormParam("redirect_uri") String redirectUri,
                                 @FormParam("grant_type") @DefaultValue("authorization_code") String grantType,
                                 @FormParam("username") String username,
                                 @FormParam("password") String password,
                                 @FormParam("state") String state) {
        OAuthToken accessToken;

        if (clientId == null) {
            // check if the clientId is in the auth header
            String authHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
            String[] clientIdAndSecret = BasicAuthDecoder.decode(authHeader);

            if (clientIdAndSecret.length == 0)
                throw new BadRequestException("invalid_client");

            clientId = clientIdAndSecret[0];
            clientSecret = clientIdAndSecret[1];
        }
        OAuthClient oAuthClient = oAuthProviderService.getOAuthClient(clientId, clientSecret);

        if (oAuthClient == null) {
            throw new BadRequestException("invalid_client");
        }

        if (grantType.equalsIgnoreCase("authorization_code")) {

            if (redirectUri == null) {
                throw new BadRequestException("invalid_request", "redirect_uri is null");
            }

            OAuthNonce nonce = oAuthProviderService.getValidNonce(clientId, code, redirectUri);
            if (code == null || nonce == null) {
                throw new BadRequestException("invalid_grant", "Either code was null or the code doesn't match the " +
                        "clientId or the redirect_uri didn't match the redirect_uri sent with the authorization_code request");
            }
            accessToken = oAuthProviderService.generateToken(oAuthClient, state, nonce);
        } else if (grantType.equalsIgnoreCase("password")) {
            User user = userService.getUser(username, password);

            if (user == null) {
                throw new BadRequestException("the supplied username and/or password are incorrect", "invalid_request");
            }

            accessToken = oAuthProviderService.generateToken(oAuthClient, user, null);
        } else {
            throw new BadRequestException("unsupported_grant_type", "invalid grant_type was requested");
        }

        return Response.ok(accessToken)
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .build();
    }

    /**
     * Service for an oAuth client app to exchange a refresh token for a valid access token.
     *
     * @param clientId
     * @param refreshToken
     * @return
     */
    @POST
    @Path("/oauth/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response refresh(@FormParam("client_id") String clientId,
                            @FormParam("refresh_token") String refreshToken) {
        OAuthClient oAuthClient = oAuthProviderService.getOAuthClient(clientId, null);

        if (oAuthClient == null) {
            throw new BadRequestException("invalid_client");
        }

        OAuthToken expiredOAuthToken = oAuthProviderService.getOAuthToken(refreshToken);

        if (expiredOAuthToken == null) {
            throw new BadRequestException("invalid_grant", "refresh_token is invalid");
        }

        OAuthToken newOAuthToken = oAuthProviderService.generateToken(expiredOAuthToken);

        return Response.ok(newOAuthToken)
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .build();
    }

    /**
     * Rest service to log a user out of the fims system
     */
    @GET
    @Path("logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout() {
        // Invalidate the session
        session.invalidate();
        userContext.setUser(null);
        try {
            return Response.seeOther(new URI(props.appRoot())).build();
        } catch (URISyntaxException e) {
            throw new ServerErrorException(e);
        }
    }
}
