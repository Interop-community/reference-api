/**
 *  * #%L
 *  *
 *  * %%
 *  * Copyright (C) 2014-2019 Healthcare Services Platform Consortium
 *  * %%
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  * #L%
 */

package org.hspconsortium.platform.api.interceptor;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.api.server.ResponseDetails;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import org.hl7.fhir.instance.model.Encounter;
import org.hl7.fhir.instance.model.Reference;
import org.hl7.fhir.instance.model.StringType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hspconsortium.platform.api.authorization.ScopeBasedAuthorizationParams;
import org.hspconsortium.platform.api.authorization.SmartScope;
import org.hspconsortium.platform.api.oauth2.HspcOAuth2Authentication;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.lang.reflect.*;

import static ch.qos.logback.core.joran.util.beans.BeanUtil.isGetter;


@Component
public class ScopeBasedAuthorizationInterceptor extends InterceptorAdapter {

    public static final String LAUNCH_CONTEXT_PATIENT_PARAM_NAME = "patient";

    @Autowired
    private ScopeBasedAuthorizationParams scopeBasedAuthorizationParams;

    @Override
    public boolean incomingRequestPostProcessed(RequestDetails theRequestDetails, HttpServletRequest theRequest, HttpServletResponse theResponse) throws AuthenticationException {

        // Authorization filtering only applies to searching a particular type
        if (theRequestDetails.getRestOperationType() != RestOperationTypeEnum.SEARCH_TYPE) {
            if (theRequestDetails.getRestOperationType() != RestOperationTypeEnum.VREAD)
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // if the user is not authenticated, we can't do any authorization
        if (authentication == null || !(authentication instanceof HspcOAuth2Authentication)) {
            return true;
        }

        HspcOAuth2Authentication hspcOAuth2Authentication = (HspcOAuth2Authentication) authentication;


        Set<SmartScope> smartScopes = getSmartScopes(hspcOAuth2Authentication);

        // we currently treat a user as if it has access to the entire system, so if a user scope exists we don't do
        // any further authorization filtering
        for (SmartScope smartScope : smartScopes) {
            if (smartScope.isUserScope())
                return true;
        }

        String scope_patientId;
        String patientId = "";
        for (SmartScope smartScope : smartScopes) {
            if (smartScope.isPatientScope()) {
                scope_patientId = hspcOAuth2Authentication.getLaunchContextParams().get(LAUNCH_CONTEXT_PATIENT_PARAM_NAME);
                filterToPatientScope(scope_patientId, theRequestDetails);

                String uri = theRequest.getRequestURI();
                String queryString = theRequest.getQueryString();

                if (uri.contains("Patient/")) {
                    patientId = uri.substring(uri.indexOf("Patient") + 8);
                } else if (uri.contains("Patient") && queryString != null && queryString.contains("_id=")) {
                    patientId = queryString.substring(queryString.indexOf("_id=") + 4);
                } else if (queryString != null && queryString.contains("patient") && !queryString.contains("&_count")) {
                    patientId = queryString.substring(queryString.indexOf("patient") + 8);
                } else if (queryString != null && queryString.contains("patient") && queryString.contains("&_count")) {
                    patientId = queryString.substring(queryString.indexOf("patient") + 8, queryString.indexOf("&_count"));
                }

                if (!patientId.isEmpty()) {
                    if (!scope_patientId.equals(patientId)) {
                        return false;
                    }
                }

                return true;
            }

        }

        return true;
    }

    @Override
    public boolean outgoingResponse(RequestDetails theRequestDetails, ResponseDetails theResponseDetails, HttpServletRequest theServletRequest, HttpServletResponse theServletResponse) throws AuthenticationException {

        IBaseResource resource = theResponseDetails.getResponseResource();
        String[] resourceList = {"Encounter", "MedicationRequest", "MedicationOrder"};
        String patientId = "";
        String patientId2 = "";
        try {
            if (Arrays.stream(resourceList).anyMatch(resource.getIdElement().getResourceType()::equals)) {
//                patientId = ((Reference)((Encounter) theResponseDetails.getResponseResource()).getPatient()).getReference();

                Method[] resourceMethods = resource.getClass().getMethods();
                for (Method method: resourceMethods) {
                    if (method.getName().equalsIgnoreCase("getSubject")) {
                        Class referenceClass = resource.getClass().getMethod("getSubject").getReturnType();
                        Object newObj = Class.forName(referenceClass.getName()).cast(resource.getClass().getMethod("getSubject").invoke(resource));

                        Method m  = newObj.getClass().getDeclaredMethod("getReference");
                        m.setAccessible(true);
                        Object o = m.invoke(referenceClass.newInstance());
                        System.out.println((String) o);

                        patientId2 = "a";

//                        Method[] referenceMethods = referenceClass.getDeclaredMethods();
//                        for (Method method2: referenceMethods) {
//                            if (method2.getName().equalsIgnoreCase("getReference")) {
//                                Method m = referenceClass.getMethod("getReference");
//                                m.setAccessible(true);
//                                Object o = m.invoke(referenceClass.newInstance());
//                                System.out.println((String) o);
//                                patientId2 = "";
//
//                            }
//                        }


//                        Field[] fields1 = referenceClass.getDeclaredFields();
//                        for (Field field: fields1) {
//                            if (field.getName().equalsIgnoreCase("reference")) {
//                                field.setAccessible(true);
////                                field.get(new StringType());
//                                System.out.println(field.get(referenceClass.newInstance()));
//                            }
//                        }
//                        Field f = fields1.getClass().getDeclaredField("reference");
//
//                        System.out.println(f.get(returnedClass));
//                        Field field = returnedClass.getField("reference");
//                        Object fieldValue = field.get("reference");
//                        patientId2 = resource.getClass().getField("SP_SUBJECT").toString();

                    }

                }
//                resource.getClass().getMethod("getSubject");
            }
        } catch (Exception e) {
            e.printStackTrace();

        }

        return true;
    }





//    @Override
//    public boolean incomingRequestPreProcessed(HttpServletRequest theRequest, HttpServletResponse theResponse) {
//
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        HspcOAuth2Authentication hspcOAuth2Authentication = (HspcOAuth2Authentication) authentication;
//
//        Set<SmartScope> smartScopes = getSmartScopes(hspcOAuth2Authentication);
//
//        for (SmartScope smartScope : smartScopes) {
//            if (smartScope.isPatientScope()) {
//                String scope_patientId = hspcOAuth2Authentication.getLaunchContextParams().get(LAUNCH_CONTEXT_PATIENT_PARAM_NAME);
//                String uri = theRequest.getRequestURI();
//                String queryString = theRequest.getQueryString();
//                String patientId = "";
//                if (uri.contains("Patient/")) {
//                    patientId = uri.substring(uri.indexOf("Patient") + 8);
//                } else if (uri.contains("Patient") && queryString.contains("_id=")) {
//                    patientId = queryString.substring(queryString.indexOf("_id=") + 4);
//                } else if (queryString != null && queryString.contains("patient") && !queryString.contains("&_count")) {
//                    patientId = queryString.substring(queryString.indexOf("patient") + 8);
//                } else if (queryString != null && queryString.contains("patient") && queryString.contains("&_count")) {
//                    patientId = queryString.substring(queryString.indexOf("patient") + 8, queryString.indexOf("&_count"));
//                }
//
//                if (!patientId.isEmpty()) {
//                    if (!scope_patientId.equals(patientId)) {
//                        throw new SecurityException("Patient: " + patientId + " is not in the selected patient scope.");
//                    }
//                }
//                return true;
//            }
//        }
//
//        return true;
//    }


    private void filterToPatientScope(String patientId, RequestDetails requestDetails) {
        if (patientId == null) {
            throw new SecurityException("For patient scope, a launch_context parameter indicating the in-context" +
                    " patient is required, but none was found.");
        }

        String scopeParam = scopeBasedAuthorizationParams.getParamForResource(requestDetails.getResourceName());

        if (scopeParam == null) {
            // https://www.hl7.org/fhir/compartment-patient.html
            // if we get here, the resource being accessed is one described as "...never in [the patient] compartment"
            return;
        }

        Map<String, String[]> requestParams = requestDetails.getParameters();
        String[] existingScopeParamValue = requestParams.get(scopeParam);

        if (existingScopeParamValue == null) {
            // parameter doesn't exist with name 'scopeParam'
            requestDetails.addParameter(scopeParam, new String[]{patientId});
        } else if (!valueAlreadyInParameter(existingScopeParamValue, patientId)) {
            // parameter exists, but is different than the current patientId
            requestDetails.addParameter(scopeParam, addValueToStringArray(existingScopeParamValue, patientId));
        }
    }

    private String[] addValueToStringArray(String[] stringArray, String newValue) {
        String[] newArray = new String[stringArray.length + 1];

        for (int x = 0; x < newArray.length - 1; x++) {
            newArray[x] = stringArray[x];
        }

        newArray[newArray.length - 1] = newValue;

        return newArray;
    }

    private boolean valueAlreadyInParameter(String[] existingScopeParamValue, String valueToSearch) {
        for (String anExistingScopeParamValue : existingScopeParamValue) {
            if (valueToSearch.equals(anExistingScopeParamValue))
                return true;
        }
        return false;
    }

    /**
     * Scopes are stored as strings in the authentication object. Take those out and add them to the "SmartScope"
     * wrapper which adds some convenience methods for extracting meaning from the scope.
     */
    private Set<SmartScope> getSmartScopes(HspcOAuth2Authentication hspcOAuth2Authentication) {

        Set<SmartScope> scopes = new HashSet<>();

        for (String scope : hspcOAuth2Authentication.getOAuth2Request().getScope()) {
            scopes.add(new SmartScope(scope));
        }

        return scopes;
    }

}
