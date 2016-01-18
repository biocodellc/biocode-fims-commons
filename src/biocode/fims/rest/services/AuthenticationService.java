package biocode.fims.rest.services;

import biocode.fims.auth.Authenticator;
import biocode.fims.auth.Authorizer;
import biocode.fims.auth.oauth2.OAuthProvider;
import biocode.fims.bcid.Database;
import biocode.fims.fimsExceptions.BadRequestException;
import biocode.fims.fimsExceptions.OAuthException;
import biocode.fims.fimsExceptions.ServerErrorException;
import biocode.fims.rest.FimsService;
import biocode.fims.utils.ErrorInfo;
import biocode.fims.utils.QueryParams;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * REST interface for handling user authentication
 */
@Path("authenticationService")
public class AuthenticationService extends FimsService {
    @Context
    private HttpServletRequest request;
    private static Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    /**
     * Service to log a user into the biocode-fims system
     *
     * @param usr
     * @param pass
     * @param return_to the url to return to after login
     *
     * @throws IOException
     */
    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(@FormParam("username") String usr,
                          @FormParam("password") String pass,
                          @QueryParam("return_to") String return_to,
                          @Context HttpServletResponse res) {

        if (!usr.isEmpty() && !pass.isEmpty()) {
            Authenticator authenticator = new Authenticator();
            Boolean isAuthenticated;

            // Verify that the entered and stored passwords match
            isAuthenticated = authenticator.login(usr, pass);

            if (isAuthenticated) {
                // Place the user in the session
                session.setAttribute("username", usr);
                Database db = new Database();
                session.setAttribute("userId", db.getUserId(usr));
                db.close();

                Authorizer myAuthorizer = new Authorizer();

                // Check if the user is an admin for any projects
                if (myAuthorizer.userProjectAdmin(usr)) {
                    session.setAttribute("projectAdmin", true);
                }

                myAuthorizer.close();

                // Check if the user has created their own password, if they are just using the temporary password, inform the user to change their password
                if (!authenticator.userSetPass(usr)) {
                    // don't need authenticator anymore
                    authenticator.close();

                    return Response.ok("{\"url\": \"" + appRoot + "secure/profile.jsp?error=Update Your Password" +
                            new QueryParams().getQueryParams(request.getParameterMap(), false) + "\"}")
                            .build();
                } else {
                    // don't need authenticator anymore
                    authenticator.close();
                }

                // Redirect to return_to uri if provided
                if (return_to != null) {

                    // check to see if oAuthLogin is in the session and set to true is so.
                    Object oAuthLogin = session.getAttribute("oAuthLogin");
                    if (oAuthLogin != null) {
                        session.setAttribute("oAuthLogin", true);
                    }

                    return Response.ok("{\"url\": \"" + return_to +
                            new QueryParams().getQueryParams(request.getParameterMap(), true) + "\"}")
                            .build();
                } else {
                    return Response.ok("{\"url\": \"" + appRoot + "index.jsp\"}").build();
                }
            }
            // stored and entered passwords don't match, invalidate the session to be sure that a user is not in the session
            else {
                session.invalidate();
                authenticator.close();
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

        OAuthProvider p = new OAuthProvider();

        if (redirectURL == null) {
            String callback = null;
            try {
                callback = p.getCallback(clientId);
            } catch (OAuthException e) {
                logger.warn("OAuthException retrieving callback for OAUTH clientID {}", clientId, e);
            }

            if (callback != null) {
                try {
                    p.close();
                    return Response.status(302).location(new URI(callback + "?error=invalid_request")).build();
                } catch (URISyntaxException e) {
                    logger.warn("Malformed callback URI for oAuth client {} and callback {}", clientId, callback);
                }
            }
            p.close();
            throw new BadRequestException("invalid_request");
        }

        if (clientId == null || !p.validClientId(clientId)) {
            redirectURL += "?error=unauthorized_client";
            try {
                p.close();
                return Response.status(302).location(new URI(redirectURL)).build();
            } catch (URISyntaxException e) {
                p.close();
                throw new BadRequestException("invalid_request", "invalid redirect_uri provided");
            }
        }

        if (username == null || !oAuthLogin) {
            session.setAttribute("oAuthLogin", "false");
            // need the user to login
            try {
                p.close();
                return Response.status(Response.Status.TEMPORARY_REDIRECT)
                        .location(new URI(appRoot + "login.jsp?return_to=/id/authenticationService/oauth/authorize?"
                                    + request.getQueryString()))
                        .build();
            } catch (URISyntaxException e) {
                p.close();
                throw new ServerErrorException(e);
            }
        }
        //TODO ask user if they want to share profile information with requesting party
        String code = p.generateCode(clientId, redirectURL, username.toString());
        p.close();

        // no longer need oAuthLogin session attribute
        session.removeAttribute("oAuthLogin");

        redirectURL += "?code=" + code;

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
     * @param redirectURL
     * @param state
     *
     * @return
     */
    @POST
    @Path("/oauth/access_token")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response access_token(@FormParam("code") String code,
                                 @FormParam("client_id") String clientId,
                                 @FormParam("client_secret") String clientSecret,
                                 @FormParam("redirect_uri") String redirectURL,
                                 @FormParam("grant_type") @DefaultValue("authorization_code") String grantType,
                                 @FormParam("username") String username,
                                 @FormParam("password") String password,
                                 @FormParam("state") String state) {
        OAuthProvider p = new OAuthProvider();
        JSONObject accessToken;

        if (clientId == null || clientSecret == null || !p.validateClient(clientId, clientSecret)) {
            p.close();
            throw new BadRequestException("invalid_client");
        }

        if (grantType.equalsIgnoreCase("authorization_code")) {
            if (redirectURL == null) {
                throw new BadRequestException("invalid_request", "redirect_uri is null");
            }

            if (code == null || !p.validateCode(clientId, code, redirectURL)) {
                p.close();
                throw new BadRequestException("invalid_grant", "Either code was null or the code doesn't match the " +
                        "clientId or the redirect_uri didn't match the redirect_uri sent with the authorization_code request");
            }
            accessToken = p.generateToken(clientId, state, code);
        } else if (grantType.equalsIgnoreCase("password")) {
            Authenticator authenticator = new Authenticator();
            if (username == null || password == null || !authenticator.login(username, password)) {
                throw new BadRequestException("the supplied username and/or password are incorrect", "invalid_request");
            }

            accessToken = p.generateToken(clientId, username);
        } else {
            throw new BadRequestException("unsupported_grant_type", "invalid grant_type was requested");
        }
        p.close();

        return Response.ok(accessToken.toJSONString())
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .build();
    }

    /**
     * Service for an oAuth client app to exchange a refresh token for a valid access token.
     *
     * @param clientId
     * @param clientSecret
     * @param refreshToken
     *
     * @return
     */
    @POST
    @Path("/oauth/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response refresh(@FormParam("client_id") String clientId,
                            @FormParam("client_secret") String clientSecret,
                            @FormParam("refresh_token") String refreshToken) {
        OAuthProvider p = new OAuthProvider();

        if (clientId == null || clientSecret == null || !p.validateClient(clientId, clientSecret)) {
            p.close();
            throw new BadRequestException("invalid_client");
        }

        if (refreshToken == null || !p.validateRefreshToken(refreshToken)) {
            p.close();
            throw new BadRequestException("invalid_grant", "refresh_token is invalid");
        }

        JSONObject accessToken = p.generateToken(refreshToken);

        // refresh tokens are only good once, so delete the old access token so the refresh token can no longer be used
        p.deleteAccessToken(refreshToken);
        p.close();

        return Response.ok(accessToken.toJSONString())
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
        try {
            return Response.seeOther(new URI(appRoot + "index.jsp")).build();
        } catch (URISyntaxException e) {
            throw new ServerErrorException(e);
        }
    }
}
