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

package de.adorsys.psd2.xs2a.service.validator.pis;

import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.service.validator.BusinessValidator;
import de.adorsys.psd2.xs2a.service.validator.TppInfoProvider;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import de.adorsys.psd2.xs2a.service.validator.tpp.PisTppInfoValidator;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Common validator for validating TPP in payments and executing request-specific business validation afterwards.
 * Should be used for all PIS-related requests after payment initiation.
 *
 * @param <T> type of object to be checked
 */
@Slf4j
@Component
public abstract class AbstractPisTppValidator<T extends TppInfoProvider> implements BusinessValidator<T> {
    private PisTppInfoValidator pisTppInfoValidator;

    @Override
    public ValidationResult validate(@NotNull T object) {
        TppInfo tppInfoInPayment = object.getTppInfo();
        ValidationResult tppValidationResult = pisTppInfoValidator.validateTpp(tppInfoInPayment);
        if (tppValidationResult.isNotValid()) {
            return tppValidationResult;
        }

        return executeBusinessValidation(object);
    }

    /**
     * Executes request-specific business validation
     *
     * @param paymentObject payment object to be validated
     * @return valid result if the object is valid, invalid result with appropriate error otherwise
     */
    protected abstract ValidationResult executeBusinessValidation(T paymentObject);

    @Autowired
    public void setPisTppInfoValidator(PisTppInfoValidator pisTppInfoValidator) {
        this.pisTppInfoValidator = pisTppInfoValidator;
    }
}
