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

package de.adorsys.psd2.xs2a.web.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.discovery.ServiceTypeDiscoveryService;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorMapperContainer;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceTypeToErrorTypeMapper;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.web.validator.ConsentControllerHeadersValidationService;
import de.adorsys.psd2.xs2a.web.validator.PaymentControllerHeadersValidationService;
import de.adorsys.psd2.xs2a.web.validator.common.PsuIpAddressvalidationService;
import de.adorsys.psd2.xs2a.web.validator.common.XRequestIdValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static de.adorsys.psd2.xs2a.domain.MessageErrorCode.FORMAT_ERROR;
import static de.adorsys.psd2.xs2a.domain.TppMessageInformation.of;
import static de.adorsys.psd2.xs2a.web.validator.constants.Xs2aMethodNameConstant.CREATE_CONSENT;
import static de.adorsys.psd2.xs2a.web.validator.constants.Xs2aMethodNameConstant.INITIATE_PAYMENT;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeaderValidationInterceptor extends HandlerInterceptorAdapter {
    private final ServiceTypeDiscoveryService serviceTypeDiscoveryService;
    private final ServiceTypeToErrorTypeMapper errorTypeMapper;
    private final ErrorMapperContainer errorMapperContainer;
    private final ObjectMapper objectMapper;
    private final XRequestIdValidationService xRequestIdValidationService;
    private final PsuIpAddressvalidationService psuIpAddressvalidationService;
    private final PaymentControllerHeadersValidationService paymentControllerHeadersValidationService;
    private final ConsentControllerHeadersValidationService consentControllerHeadersValidationService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        return isRequestValid(request, response, handler);
    }

    private boolean isRequestValid(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

        ValidationResult xRequestIdValidationResult = xRequestIdValidationService.validateXRequestId(request);

        if (xRequestIdValidationResult.isNotValid()) {
            return buildError(response, xRequestIdValidationResult);
        }

        return validateSpecificMethods(request, response, handler);
    }

    private boolean validateSpecificMethods(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {

        if (isSpecifiedMethodCalled(handler, INITIATE_PAYMENT)) {
            ValidationResult psuIpAddressValidationResult = psuIpAddressvalidationService.validatePsuIdAddress(request);
            if (psuIpAddressValidationResult.isNotValid()) {
                return buildError(response, psuIpAddressValidationResult);
            }

            ValidationResult tppRedirectValidationResult = paymentControllerHeadersValidationService.validateInitiatePayment();
            if (tppRedirectValidationResult.isNotValid()) {
                return buildError(response, tppRedirectValidationResult);
            }
        }

        if (isSpecifiedMethodCalled(handler, CREATE_CONSENT)) {
            ValidationResult cretaeConsentValidationResult = consentControllerHeadersValidationService.validateCreateConsent();
            if (cretaeConsentValidationResult.isNotValid()) {
                return buildError(response, cretaeConsentValidationResult);
            }
        }

        return true;
    }

    private boolean isSpecifiedMethodCalled(Object handler, String methodName) {
        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            return handlerMethod.getMethod().getName().equals(methodName);
        }
        return false;
    }

    private boolean buildError(HttpServletResponse response, ValidationResult validationResult) throws IOException {
        response.resetBuffer();
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Type", "application/json");

        Optional<MessageError> optionalMessageError = Optional.ofNullable(validationResult.getMessageError());

        if (optionalMessageError.isPresent()) {
            TppMessageInformation tppMessageInformation = optionalMessageError.get().getTppMessage();
            response.setStatus(tppMessageInformation.getMessageErrorCode().getCode());
            response.getWriter().write(objectMapper.writeValueAsString(createError(Collections.singleton(tppMessageInformation.getText()))));
        } else {
            response.setStatus(FORMAT_ERROR.getCode());
            response.getWriter().write(objectMapper.writeValueAsString(createError(Collections.singleton("MessageError is empty"))));
        }

        response.flushBuffer();
        return false;
    }

    private Object createError(Collection<String> errorMessages) {
        MessageError messageError = getMessageError(errorMessages);
        return Optional.ofNullable(errorMapperContainer.getErrorBody(messageError))
                   .map(ErrorMapperContainer.ErrorBody::getBody)
                   .orElse(null);
    }

    private MessageError getMessageError(Collection<String> errorMessages) {
        ErrorType errorType = errorTypeMapper.mapToErrorType(serviceTypeDiscoveryService.getServiceType(), FORMAT_ERROR.getCode());

        TppMessageInformation[] tppMessages = errorMessages.stream()
                                                  .map(e -> of(FORMAT_ERROR, e))
                                                  .toArray(TppMessageInformation[]::new);

        return new MessageError(errorType, tppMessages);
    }
}
