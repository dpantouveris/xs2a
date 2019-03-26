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

package de.adorsys.psd2.xs2a.service.validator.tpp;

import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.TppMessageInformation;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ErrorType;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import static de.adorsys.psd2.xs2a.domain.MessageErrorCode.UNAUTHORIZED;

@Component
@RequiredArgsConstructor
public class PisTppInfoValidator {
    private final TppInfoValidationService tppInfoValidationService;

    /**
     * Validates the TPP object contained in the payment by checking whether it matches current TPP
     *
     * @param tppInfoInPayment TPP Info object contained in the payment
     * @return validation result
     */
    public ValidationResult validateTpp(@Nullable TppInfo tppInfoInPayment) {
        if (tppInfoValidationService.differsFromTppInRequest(tppInfoInPayment)) {
            return ValidationResult.invalid(ErrorType.PIS_401, TppMessageInformation.of(UNAUTHORIZED, "TPP certificate doesn’t match the initial request"));
        }

        return ValidationResult.valid();
    }
}
