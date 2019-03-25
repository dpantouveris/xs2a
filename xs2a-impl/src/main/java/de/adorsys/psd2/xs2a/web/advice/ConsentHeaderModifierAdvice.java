/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.psd2.xs2a.web.advice;

import de.adorsys.psd2.model.ConsentsResponse201;
import de.adorsys.psd2.xs2a.service.ScaApproachResolver;
import de.adorsys.psd2.xs2a.web.controller.ConsentController;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.util.Optional;

import static de.adorsys.psd2.xs2a.web.validator.constants.Xs2aMethodNameConstant.CREATE_CONSENT;

@ControllerAdvice(assignableTypes = {ConsentController.class})
public class ConsentHeaderModifierAdvice extends CommonHeaderModifierAdvice {

    public ConsentHeaderModifierAdvice(ScaApproachResolver scaApproachResolver) {
        super(scaApproachResolver);
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request, ServerHttpResponse response) {
        String methodName = returnType.getMethod().getName();
        if (CREATE_CONSENT.equals(methodName)) {
            response.getHeaders().add("Aspsp-Sca-Approach", scaApproachResolver.resolveScaApproach().name());
            if (!hasError(body, ConsentsResponse201.class)) {
                ConsentsResponse201 consentResponse = (ConsentsResponse201) body;
                response.getHeaders().add("Location", Optional.ofNullable(consentResponse.getLinks().get("self"))
                                                          .map(Object::toString)
                                                          .orElse(null));
            }
        } else if ("_startConsentAuthorisation".equals(methodName)
                       || "_updateConsentsPsuData".equals(methodName)) {
            response.getHeaders().add("Aspsp-Sca-Approach", scaApproachResolver.resolveScaApproach().name());
        }

        return body;
    }
}
