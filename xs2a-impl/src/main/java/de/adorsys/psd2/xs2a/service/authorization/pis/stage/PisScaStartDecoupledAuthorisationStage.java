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

package de.adorsys.psd2.xs2a.service.authorization.pis.stage;

import de.adorsys.psd2.consent.api.pis.authorisation.GetPisAuthorisationResponse;
import de.adorsys.psd2.xs2a.core.consent.AspspConsentData;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataRequest;
import de.adorsys.psd2.xs2a.domain.consent.pis.Xs2aUpdatePisCommonPaymentPsuDataResponse;
import de.adorsys.psd2.xs2a.service.consent.PisAspspDataService;
import de.adorsys.psd2.xs2a.service.context.SpiContextDataProvider;
import de.adorsys.psd2.xs2a.service.mapper.consent.CmsToXs2aPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.psd2.ServiceType;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.*;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorisationDecoupledScaResponse;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiAuthorisationStatus;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.PaymentAuthorisationSpi;
import de.adorsys.psd2.xs2a.spi.service.SpiPayment;
import org.springframework.stereotype.Service;

import static de.adorsys.psd2.xs2a.core.sca.ScaStatus.SCAMETHODSELECTED;

@Service("PIS_DECOUPLED_STARTED")
public class PisScaStartDecoupledAuthorisationStage extends PisScaStage<Xs2aUpdatePisCommonPaymentPsuDataRequest, GetPisAuthorisationResponse, Xs2aUpdatePisCommonPaymentPsuDataResponse> {
    private final PaymentAuthorisationSpi paymentAuthorisationSpi;
    private final PisAspspDataService pisAspspDataService;
    private final SpiContextDataProvider spiContextDataProvider;
    private final Xs2aToSpiPsuDataMapper xs2aToSpiPsuDataMapper;
    private final SpiErrorMapper spiErrorMapper;

    public PisScaStartDecoupledAuthorisationStage(CmsToXs2aPaymentMapper cmsToXs2aPaymentMapper, Xs2aToSpiPeriodicPaymentMapper xs2aToSpiPeriodicPaymentMapper, Xs2aToSpiSinglePaymentMapper xs2aToSpiSinglePaymentMapper, Xs2aToSpiBulkPaymentMapper xs2aToSpiBulkPaymentMapper, PaymentAuthorisationSpi paymentAuthorisationSpi, PisAspspDataService pisAspspDataService, SpiContextDataProvider spiContextDataProvider, Xs2aToSpiPsuDataMapper xs2aToSpiPsuDataMapper, SpiErrorMapper spiErrorMapper) {
        super(cmsToXs2aPaymentMapper, xs2aToSpiPeriodicPaymentMapper, xs2aToSpiSinglePaymentMapper, xs2aToSpiBulkPaymentMapper);
        this.paymentAuthorisationSpi = paymentAuthorisationSpi;
        this.pisAspspDataService = pisAspspDataService;
        this.spiContextDataProvider = spiContextDataProvider;
        this.xs2aToSpiPsuDataMapper = xs2aToSpiPsuDataMapper;
        this.spiErrorMapper = spiErrorMapper;
    }

    @Override
    public Xs2aUpdatePisCommonPaymentPsuDataResponse apply(Xs2aUpdatePisCommonPaymentPsuDataRequest request, GetPisAuthorisationResponse pisAuthorisationResponse) {
        PaymentType paymentType = pisAuthorisationResponse.getPaymentType();
        String paymentProduct = pisAuthorisationResponse.getPaymentProduct();
        SpiPayment payment = mapToSpiPayment(pisAuthorisationResponse, paymentType, paymentProduct);

        AspspConsentData aspspConsentData = pisAspspDataService.getAspspConsentData(request.getPaymentId());

        SpiPsuData psuData = xs2aToSpiPsuDataMapper.mapToSpiPsuData(request.getPsuData());
        SpiContextData contextData = spiContextDataProvider.provideWithPsuIdData(request.getPsuData());

        SpiResponse<SpiAuthorisationStatus> authPsuResponse = paymentAuthorisationSpi.authorisePsu(contextData, psuData, request.getPassword(), payment, aspspConsentData);
        aspspConsentData = authPsuResponse.getAspspConsentData();
        pisAspspDataService.updateAspspConsentData(aspspConsentData);

        if (authPsuResponse.hasError()) {
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(spiErrorMapper.mapToErrorHolder(authPsuResponse, ServiceType.PIS));
        }

        SpiResponse<SpiAuthorisationDecoupledScaResponse> spiResponse = paymentAuthorisationSpi.startScaDecoupled(contextData, request.getAuthorisationId(), null, payment, aspspConsentData);

        if (spiResponse.hasError()) {
            return new Xs2aUpdatePisCommonPaymentPsuDataResponse(spiErrorMapper.mapToErrorHolder(spiResponse, ServiceType.PIS));
        }

        Xs2aUpdatePisCommonPaymentPsuDataResponse response = new Xs2aUpdatePisCommonPaymentPsuDataResponse(SCAMETHODSELECTED);
        response.setPsuMessage(spiResponse.getPayload().getPsuMessage());
        return response;
    }
}
