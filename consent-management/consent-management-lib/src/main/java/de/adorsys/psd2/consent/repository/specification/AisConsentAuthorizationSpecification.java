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

package de.adorsys.psd2.consent.repository.specification;

import de.adorsys.psd2.consent.domain.account.AisConsentAuthorization;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;

import static de.adorsys.psd2.consent.repository.specification.EntityAttributeSpecificationProvider.provideSpecificationForEntityAttribute;

public class AisConsentAuthorizationSpecification {
    private static final String INSTANCE_ID_ATTRIBUTE = "instanceId";
    private static final String CONSENT_EXTERNAL_ID_ATTRIBUTE = "externalId";

    private AisConsentAuthorizationSpecification(){}

    public static Specification<AisConsentAuthorization> byExternalIdAndInstanceId(String externalId, String instanceId) {
        return Specifications.<AisConsentAuthorization>where(provideSpecificationForEntityAttribute(INSTANCE_ID_ATTRIBUTE, instanceId))
                   .and(provideSpecificationForEntityAttribute(CONSENT_EXTERNAL_ID_ATTRIBUTE, externalId));
    }
}
