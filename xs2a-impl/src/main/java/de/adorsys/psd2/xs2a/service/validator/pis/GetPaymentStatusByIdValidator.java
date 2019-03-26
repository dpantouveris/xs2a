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

import de.adorsys.psd2.xs2a.service.validator.GetCommonPaymentByIdResponseValidator;
import de.adorsys.psd2.xs2a.service.validator.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Validator to be used for validating get payment status by ID request according to some business rules
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GetPaymentStatusByIdValidator extends AbstractPisTppValidator<GetPaymentStatusByIdPO> {
    private final GetCommonPaymentByIdResponseValidator getCommonPaymentByIdResponseValidator;

    /**
     * Validates get payment status by ID request by checking whether:
     * <ul>
     * <li>given payment's type and product are valid for the payment</li>
     * </ul>
     *
     * @param paymentObject payment information object
     * @return valid result if the payment is valid, invalid result with appropriate error otherwise
     */
    @Override
    protected ValidationResult executeBusinessValidation(GetPaymentStatusByIdPO paymentObject) {
        ValidationResult getCommonPaymentValidationResult =
            getCommonPaymentByIdResponseValidator.validateRequest(paymentObject.getPisCommonPaymentResponse(),
                                                                  paymentObject.getPaymentType(),
                                                                  paymentObject.getPaymentProduct());
        if (getCommonPaymentValidationResult.isNotValid()) {
            return getCommonPaymentValidationResult;
        }

        return ValidationResult.valid();
    }
}
