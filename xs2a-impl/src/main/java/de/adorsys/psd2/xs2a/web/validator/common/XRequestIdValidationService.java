/*
 * Copyright 2018-2019 adorsys GmbH & Co KG
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

package de.adorsys.psd2.xs2a.web.validator.common;

import de.adorsys.psd2.xs2a.domain.MessageErrorCode;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

import static de.adorsys.psd2.xs2a.web.validator.constants.Xs2aHeaderConstant.X_REQUEST_ID;


/**
 * Service to be used to validate 'X-Request-ID' header in all REST calls.
 */
@Service
@RequiredArgsConstructor
public class XRequestIdValidationService {

    private static final String UUID_REGEX = "^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$";

    /**
     * Validates the 'X-Request-ID' header for non-null state and correct structure.
     *
     * @param request HttpServletRequest
     * @return ValidationResult instance with error code and text (if error occurs)
     */
    public ValidationResult validateXRequestId(HttpServletRequest request) {

        String xRequestId = request.getHeader(X_REQUEST_ID);

        if (Objects.isNull(xRequestId)) {
            return ValidationResult.invalid(ErrorType.COMMON_400, TppMessageInformation.of(MessageErrorCode.FORMAT_ERROR, "'X-Request-ID' may not be null"));
        }

        if (isNonValid(xRequestId)) {
            return ValidationResult.invalid(ErrorType.COMMON_400, TppMessageInformation.of(MessageErrorCode.FORMAT_ERROR, "'X-Request-ID' has to be represented by standard 36-char UUID representation"));
        }

        return ValidationResult.valid();
    }

    private boolean isNonValid(String xRequestId) {
        return !xRequestId.matches(UUID_REGEX);
    }
}
