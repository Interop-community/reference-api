/**
 *  * #%L
 *  *
 *  * %%
 *  * Copyright (C) 2014-2020 Healthcare Services Platform Consortium
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

package org.logicahealth.platform.api.proxy;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.ResponseExtractor;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class DefaultResponseExtractor extends LoggingObject implements ResponseExtractor<Void> {

    private HttpServletResponse httpResponse;
    private String[] headerNamesToCopy;

    public DefaultResponseExtractor(HttpServletResponse httpResponse, String... headerNamesToCopy) {
        this.httpResponse = httpResponse;
        this.headerNamesToCopy = headerNamesToCopy;
    }

    @Override
    public Void extractData(ClientHttpResponse response) throws IOException {
        copyHeaders(httpResponse, response);
        InputStream body = response.getBody();
        if (body != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Copying the client HTTP response body to the servlet HTTP response");
            }
            FileCopyUtils.copy(response.getBody(), httpResponse.getOutputStream());
        } else if (logger.isDebugEnabled()) {
            logger.debug("No body in the client HTTP response, so not copying anything to the servlet HTTP response");
        }

        httpResponse.setStatus(response.getRawStatusCode());

        return null;
    }

    protected void copyHeaders(HttpServletResponse httpResponse, ClientHttpResponse response) {
        if (headerNamesToCopy != null) {
            for (String name : headerNamesToCopy) {
                List<String> values = response.getHeaders().get(name);
                if (values != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(format("Setting servlet HTTP header '%s' to '%s'", name, values));
                    }
                    for (String value : values) {
                        httpResponse.addHeader(name, value);
                    }
                }
            }
        }
    }

}