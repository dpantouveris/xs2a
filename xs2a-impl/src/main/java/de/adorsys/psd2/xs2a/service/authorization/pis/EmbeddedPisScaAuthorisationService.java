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

package de.adorsys.psd2.xs2a.service.authorization.pis;

import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.profile.ScaApproach;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aAuthorisationSubResources;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aCreatePisAuthorisationResponse;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aCreatePisCancellationAuthorisationResponse;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aPaymentCancellationAuthorisationSubResource;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.mapper.consent.Xs2aPisCommonPaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmbeddedPisScaAuthorisationService implements PisScaAuthorisationService {
    private final PisAuthorisationService authorisationService;
    private final Xs2aPisCommonPaymentMapper pisCommonPaymentMapper;

    @Override
    public Optional<Xs2aCreatePisAuthorisationResponse> createCommonPaymentAuthorisation(String paymentId, PaymentType paymentType, PsuIdData psuData) {
        return pisCommonPaymentMapper.mapToXsa2CreatePisAuthorisationResponse(authorisationService.createPisAuthorisation(paymentId, psuData), paymentType);
    }

    @Override
    public Xs2aUpdatePisCommonPaymentPsuDataResponse updateCommonPaymentPsuData(Xs2aUpdatePisCommonPaymentPsuDataRequest request) {
        return authorisationService.updatePisAuthorisation(request, getScaApproachServiceType());
    }

    @Override
    public Optional<Xs2aCreatePisCancellationAuthorisationResponse> createCommonPaymentCancellationAuthorisation(String paymentId, PaymentType paymentType, PsuIdData psuData) {
        return pisCommonPaymentMapper.mapToXs2aCreatePisCancellationAuthorisationResponse(authorisationService.createPisAuthorisationCancellation(paymentId, psuData), paymentType);
    }

    @Override
    public Optional<Xs2aPaymentCancellationAuthorisationSubResource> getCancellationAuthorisationSubResources(String paymentId) {
        return authorisationService.getCancellationAuthorisationSubResources(paymentId)
                   .map(Xs2aPaymentCancellationAuthorisationSubResource::new);
    }

    @Override
    public Xs2aUpdatePisCommonPaymentPsuDataResponse updateCommonPaymentCancellationPsuData(Xs2aUpdatePisCommonPaymentPsuDataRequest request) {
        return authorisationService.updatePisCancellationAuthorisation(request);
    }

    @Override
    public Optional<Xs2aAuthorisationSubResources> getAuthorisationSubResources(String paymentId) {
        return authorisationService.getAuthorisationSubResources(paymentId)
                   .map(Xs2aAuthorisationSubResources::new);
    }

    @Override
    public Optional<ScaStatus> getAuthorisationScaStatus(String paymentId, String authorisationId) {
        return authorisationService.getAuthorisationScaStatus(paymentId, authorisationId);
    }

    @Override
    public Optional<ScaStatus> getCancellationAuthorisationScaStatus(String paymentId, String cancellationId) {
        return authorisationService.getCancellationAuthorisationScaStatus(paymentId, cancellationId);
    }

    @Override
    public ScaApproach getScaApproachServiceType() {
        return ScaApproach.EMBEDDED;
    }
}
