package org.hspconsortium.platform.api.interceptor;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.hspconsortium.platform.api.security.TenantInfoRequestMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.common.exceptions.UnauthorizedUserException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UserSecurityandAnalyticsInterceptor extends InterceptorAdapter {

    @Value("${hspc.platform.api.sandboxManagerApi.url}")
    private String sandboxManagerApiUrl;

    @Value("${hspc.platform.api.sandboxManagerApi.transactionPath}")
    private String transactionPath;

    @Autowired
    private TenantInfoRequestMatcher tenantInfoRequestMatcher;

    @Override
    public boolean outgoingResponse(RequestDetails theRequestDetails, ResponseDetails theResponseDetails, HttpServletRequest theServletRequest, HttpServletResponse theServletResponse) throws AuthenticationException {

        Integer responseCode = theResponseDetails.getResponseCode();
        handleCallToSandboxAPI(theServletRequest, theServletRequest, responseCode);
        return true;
    }

    @Override
    public boolean handleException(RequestDetails theRequestDetails, BaseServerResponseException theException, HttpServletRequest theServletRequest, HttpServletResponse theServletResponse) {
        Integer responseCode = theException.getStatusCode();
        // TODO: find better solution to the if statement below
        // This if statement is so a second call isn't made to the sandbox manager api because an exception was thrown earlier
        if (!theException.getCause().getMessage().equals("User does not have privileges to this sandbox.")) {
            handleCallToSandboxAPI(theServletRequest, theServletRequest, responseCode);
        }
        return true;
    }

    private void handleCallToSandboxAPI(HttpServletRequest request, HttpServletRequest servletRequest, Integer responseCode) {
        checkIfUserIsAuthorized(request, servletRequest, responseCode);

    }

    private void checkIfUserIsAuthorized(HttpServletRequest request, HttpServletRequest servletRequest, Integer responseCode) {
        String urlPath = request.getRequestURL().toString();
        String[] parameters = urlPath.split("/");
        String domain = parameters[2];
        String tenant = parameters[3];
        String secured = parameters[4];
        String resource = parameters[5];

        if (secured.equals("open")) {
            checkIfOpenEndpointIsAllowed(tenant);
            secured = "false";
        }  else {
            secured = "true";
        }
        if (!resource.equals("metadata")) {
            try {
                String authHeader = request.getHeader("Authorization");
                HttpClient httpclient = HttpClients.createDefault();
                HttpPost httppost = new HttpPost(sandboxManagerApiUrl + transactionPath);
                String body = "{" +
                        "\"url\": \"" + urlPath + "\"," +
                        "\"domain\": \"" + domain + "\"," +
                        "\"resource\": \"" + resource + "\"," +
                        "\"method\": \"" + request.getMethod() + "\"," +
                        "\"response_code\": \"" + responseCode + "\"," +
                        "\"ip_address\": \"" + getRemoteAddress(servletRequest) + "\"," +
                        "\"tenant\": \"" + tenant + "\"," +
                        "\"secured\": \"" + secured + "\"" +
                        "}";
                httppost.setEntity(new StringEntity(body, ContentType.APPLICATION_JSON));
                httppost.setHeader("Authorization", authHeader);
                httppost.setHeader("Content-type", "application/json");
                HttpResponse returned = httpclient.execute(httppost);
                if (returned.getStatusLine().getStatusCode() == 401) {
                    throw new Exception();
                }
            } catch (Exception e) {
                throw new UnauthorizedUserException("User does not have privileges to this sandbox.");
            }
        }
    }

    private void checkIfOpenEndpointIsAllowed(String tenant) {
        Set<String> openTenants = tenantInfoRequestMatcher.getOpenTeamIds();
        if(!openTenants.contains(tenant)) {
            throw new RuntimeException("Sandbox " + tenant + " does not have an open endpoint.");
        }
    }

    private String getRemoteAddress(HttpServletRequest httpServletRequest) {
        String remoteAddress = httpServletRequest.getHeader("X-FORWARDED-FOR");
        if (StringUtils.isEmpty(remoteAddress)) {
            remoteAddress = httpServletRequest.getRemoteAddr();
        }
        return remoteAddress;
    }


}
