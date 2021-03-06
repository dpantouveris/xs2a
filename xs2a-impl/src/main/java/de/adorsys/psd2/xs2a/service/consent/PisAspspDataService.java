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

package de.adorsys.psd2.xs2a.service.consent;

import de.adorsys.psd2.consent.api.AspspDataService;
import de.adorsys.psd2.consent.api.service.PisCommonPaymentServiceEncrypted;
import de.adorsys.psd2.xs2a.core.consent.AspspConsentData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PisAspspDataService {
    private final PisCommonPaymentServiceEncrypted pisCommonPaymentServiceEncrypted;
    private final AspspDataService aspspDataService;

    public AspspConsentData getAspspConsentData(String paymentId) {
        return aspspDataService.readAspspConsentData(paymentId)
                   .orElseGet(() -> new AspspConsentData(null, paymentId));
    }

    public void updateAspspConsentData(AspspConsentData consentData) {
        aspspDataService.updateAspspConsentData(consentData);
    }

    public String getInternalPaymentIdByEncryptedString(String encryptedId) {
        return pisCommonPaymentServiceEncrypted.getDecryptedId(encryptedId).orElse(null);
    }
}
