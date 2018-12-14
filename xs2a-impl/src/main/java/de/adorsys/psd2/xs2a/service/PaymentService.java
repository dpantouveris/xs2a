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

package de.adorsys.psd2.xs2a.service;

import de.adorsys.psd2.consent.api.pis.PisPayment;
import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentResponse;
import de.adorsys.psd2.xs2a.config.factory.ReadPaymentFactory;
import de.adorsys.psd2.xs2a.config.factory.ReadPaymentStatusFactory;
import de.adorsys.psd2.xs2a.core.consent.AspspConsentData;
import de.adorsys.psd2.xs2a.core.event.EventType;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.ErrorHolder;
import de.adorsys.psd2.xs2a.domain.ResponseObject;
import de.adorsys.psd2.xs2a.domain.consent.Xs2aPisCommonPayment;
import de.adorsys.psd2.xs2a.domain.pis.*;
import de.adorsys.psd2.xs2a.exception.MessageError;
import de.adorsys.psd2.xs2a.service.consent.PisConsentDataService;
import de.adorsys.psd2.xs2a.service.consent.PisPsuDataService;
import de.adorsys.psd2.xs2a.service.consent.Xs2aPisConsentService;
import de.adorsys.psd2.xs2a.service.event.Xs2aEventService;
import de.adorsys.psd2.xs2a.service.mapper.consent.CmsToXs2aPaymentMapper;
import de.adorsys.psd2.xs2a.service.mapper.consent.Xs2aPisConsentMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiErrorMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.SpiToXs2aTransactionalStatusMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiPaymentInfoMapper;
import de.adorsys.psd2.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiPsuDataMapper;
import de.adorsys.psd2.xs2a.service.payment.*;
import de.adorsys.psd2.xs2a.service.profile.AspspProfileServiceWrapper;
import de.adorsys.psd2.xs2a.spi.domain.common.SpiTransactionStatus;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiPaymentInfo;
import de.adorsys.psd2.xs2a.spi.domain.psu.SpiPsuData;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.CommonPaymentSpi;
import de.adorsys.psd2.xs2a.spi.service.SpiPayment;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.adorsys.psd2.xs2a.core.profile.PaymentType.PERIODIC;
import static de.adorsys.psd2.xs2a.core.profile.PaymentType.SINGLE;
import static de.adorsys.psd2.xs2a.domain.MessageErrorCode.*;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {
    private final ReadPaymentFactory readPaymentFactory;
    private final ReadPaymentStatusFactory readPaymentStatusFactory;
    private final SpiPaymentFactory spiPaymentFactory;
    private final Xs2aPisConsentService pisConsentService;
    private final Xs2aUpdatePaymentStatusAfterSpiService updatePaymentStatusAfterSpiService;
    private final PisConsentDataService pisConsentDataService;
    private final PisPsuDataService pisPsuDataService;
    private final TppService tppService;
    private final CreateSinglePaymentService createSinglePaymentService;
    private final CreatePeriodicPaymentService createPeriodicPaymentService;
    private final CreateBulkPaymentService createBulkPaymentService;
    private final Xs2aPisConsentMapper xs2aPisConsentMapper;
    private final Xs2aToSpiPsuDataMapper psuDataMapper;
    private final CommonPaymentSpi commonPaymentSpi;
    private final SpiToXs2aTransactionalStatusMapper spiToXs2aTransactionalStatus;
    private final AspspProfileServiceWrapper profileService;
    private final CancelPaymentService cancelPaymentService;
    private final SpiErrorMapper spiErrorMapper;
    private final Xs2aEventService xs2aEventService;
    private final CreateCommonPaymentService createCommonPaymentService;
    private final ReadCommonPaymentService readCommonPaymentService;
    private final Xs2aToSpiPaymentInfoMapper xs2aToSpiPaymentInfoMapper;
    private final CmsToXs2aPaymentMapper cmsToXs2aPaymentMapper;

    /**
     * Initiates a payment though "payment service" corresponding service method
     *
     * @param payment                     Payment information
     * @param paymentInitiationParameters Parameters for payment initiation
     * @return Response containing information about created payment or corresponding error
     */
    public ResponseObject createPayment(Object payment, PaymentInitiationParameters paymentInitiationParameters) {
        xs2aEventService.recordTppRequest(EventType.PAYMENT_INITIATION_REQUEST_RECEIVED, payment);

        TppInfo tppInfo = tppService.getTppInfo();
        tppInfo.setTppRedirectUri(paymentInitiationParameters.getTppRedirectUri());
        Xs2aPisCommonPayment pisCommonPayment = xs2aPisConsentMapper.mapToXs2aPisCommonPayment(pisConsentService.createCommonPayment(paymentInitiationParameters, tppInfo), paymentInitiationParameters.getPsuData());
        if (StringUtils.isBlank(pisCommonPayment.getPaymentId())) {
            return ResponseObject.builder()
                       .fail(new MessageError(PAYMENT_FAILED))
                       .build();
        }

        if (isRawPaymentProduct(paymentInitiationParameters.getPaymentProduct())) {
            CommonPayment request = new CommonPayment();
            request.setPaymentType(paymentInitiationParameters.getPaymentType());
            request.setPaymentProduct(paymentInitiationParameters.getPaymentProduct());
            request.setPaymentData((byte[]) payment);
            request.setTppInfo(tppInfo);
            request.setPsuData(Collections.singletonList(paymentInitiationParameters.getPsuData()));

            return createCommonPaymentService.createPayment(request, paymentInitiationParameters, tppInfo, pisCommonPayment);
        }

        if (paymentInitiationParameters.getPaymentType() == SINGLE) {
            return createSinglePaymentService.createPayment((SinglePayment) payment, paymentInitiationParameters, tppInfo, pisCommonPayment);
        } else if (paymentInitiationParameters.getPaymentType() == PERIODIC) {
            return createPeriodicPaymentService.createPayment((PeriodicPayment) payment, paymentInitiationParameters, tppInfo, pisCommonPayment);
        } else {
            return createBulkPaymentService.createPayment((BulkPayment) payment, paymentInitiationParameters, tppInfo, pisCommonPayment);
        }
    }

    private boolean isRawPaymentProduct(String paymentProduct) {
        // TODO make correct value of method https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/533
        return paymentProduct.contains("pain.");
    }

    /**
     * Retrieves payment from ASPSP by its ASPSP identifier, product and payment type
     *
     * @param paymentType type of payment (payments, bulk-payments, periodic-payments)
     * @param paymentId   ASPSP identifier of the payment
     * @return Response containing information about payment or corresponding error
     */
    public ResponseObject getPaymentById(PaymentType paymentType, String paymentId) {
        xs2aEventService.recordPisTppRequest(paymentId, EventType.GET_PAYMENT_REQUEST_RECEIVED);
        AspspConsentData aspspConsentData = pisConsentDataService.getAspspConsentData(paymentId);
        Optional<PisCommonPaymentResponse> pisCommonPaymentOptional = pisConsentService.getPisCommonPaymentById(paymentId);

        if (!pisCommonPaymentOptional.isPresent()) {
            return ResponseObject.builder()
                       .fail(new MessageError(FORMAT_ERROR, "Payment not found"))
                       .build();
        }

        PisCommonPaymentResponse commonPaymentResponse = pisCommonPaymentOptional.get();

        PsuIdData psuData = pisPsuDataService.getPsuDataByPaymentId(paymentId);
        PaymentInformationResponse response;

        // TODO should be refactored https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/533
        if (commonPaymentResponse.getPaymentData() != null) {
            CommonPayment commonPayment = cmsToXs2aPaymentMapper.mapToXs2aCommonPayment(commonPaymentResponse);
            response = readCommonPaymentService.getPayment(commonPayment, psuData, aspspConsentData);
        } else {
            PisPayment pisPayment = getPisPaymentFromConsent(commonPaymentResponse);

            if (pisPayment == null) {
                return ResponseObject.builder()
                           .fail(new MessageError(FORMAT_ERROR, "Payment not found"))
                           .build();
            }

            ReadPaymentService<PaymentInformationResponse> readPaymentService = readPaymentFactory.getService(paymentType.getValue());
            response = readPaymentService.getPayment(pisPayment, commonPaymentResponse.getPaymentProduct(), psuData, aspspConsentData); //NOT USED IN 1.2
        }

        if (response.hasError()) {
            return ResponseObject.builder()
                       .fail(new MessageError(response.getErrorHolder().getErrorCode(), response.getErrorHolder().getMessage()))
                       .build();
        }
        return ResponseObject.builder()
                   .body(response.getPayment())
                   .build();
    }

    /**
     * Retrieves payment status from ASPSP
     *
     * @param paymentType The addressed payment category Single, Periodic or Bulk
     * @param paymentId   String representation of payment primary ASPSP identifier
     * @return Information about the status of a payment
     */
    public ResponseObject<TransactionStatus> getPaymentStatusById(PaymentType paymentType, String paymentId) {
        xs2aEventService.recordPisTppRequest(paymentId, EventType.GET_TRANSACTION_STATUS_REQUEST_RECEIVED);

        AspspConsentData aspspConsentData = pisConsentDataService.getAspspConsentData(paymentId);
        Optional<PisCommonPaymentResponse> pisCommonPaymentOptional = pisConsentService.getPisCommonPaymentById(paymentId);

        if (!pisCommonPaymentOptional.isPresent()) {
            return ResponseObject.<TransactionStatus>builder()
                       .fail(new MessageError(FORMAT_ERROR, "Payment not found"))
                       .build();
        }

        PsuIdData psuData = pisPsuDataService.getPsuDataByPaymentId(paymentId);
        PisCommonPaymentResponse pisCommonPaymentResponse = pisCommonPaymentOptional.get();
        SpiPsuData spiPsuData = psuDataMapper.mapToSpiPsuData(psuData);
        SpiResponse<SpiTransactionStatus> spiResponse;

        // TODO should be refactored https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/533
        if (pisCommonPaymentResponse.getPaymentData() != null) {
            CommonPayment commonPayment = cmsToXs2aPaymentMapper.mapToXs2aCommonPayment(pisCommonPaymentResponse);
            SpiPaymentInfo request = xs2aToSpiPaymentInfoMapper.mapToSpiPaymentInfo(commonPayment);
            spiResponse = commonPaymentSpi.getPaymentStatusById(spiPsuData, request, aspspConsentData);
        } else {
            PisPayment pisPayment = getPisPaymentFromConsent(pisCommonPaymentResponse);

            if (pisPayment == null) {
                return ResponseObject.<TransactionStatus>builder()
                           .fail(new MessageError(FORMAT_ERROR, "Payment not found"))
                           .build();
            }

            ReadPaymentStatusService readPaymentStatusService = readPaymentStatusFactory.getService(ReadPaymentStatusFactory.SERVICE_PREFIX + paymentType.getValue());
            spiResponse = readPaymentStatusService.readPaymentStatus(pisPayment, pisCommonPaymentResponse.getPaymentProduct(), spiPsuData, aspspConsentData);
        }

        pisConsentDataService.updateAspspConsentData(spiResponse.getAspspConsentData());

        if (spiResponse.hasError()) {
            ErrorHolder errorHolder = spiErrorMapper.mapToErrorHolder(spiResponse);
            return ResponseObject.<TransactionStatus>builder()
                       .fail(new MessageError(errorHolder.getErrorCode(), errorHolder.getMessage()))
                       .build();
        }

        TransactionStatus transactionStatus = spiToXs2aTransactionalStatus.mapToTransactionStatus(spiResponse.getPayload());

        if (transactionStatus == null) {
            return ResponseObject.<TransactionStatus>builder()
                       .fail(new MessageError(RESOURCE_UNKNOWN_403))
                       .build();
        }

        if (!updatePaymentStatusAfterSpiService.updatePaymentStatus(paymentId, transactionStatus)) {
            return ResponseObject.<TransactionStatus>builder()
                       .fail(new MessageError(FORMAT_ERROR, "Payment is finalised already, so its status cannot be changed"))
                       .build();
        }

        return ResponseObject.<TransactionStatus>builder().body(transactionStatus).build();
    }

    /**
     * Cancels payment by its ASPSP identifier and payment type
     *
     * @param paymentType type of payment (payments, bulk-payments, periodic-payments)
     * @param paymentId   ASPSP identifier of the payment
     * @return Response containing information about cancelled payment or corresponding error
     */
    public ResponseObject<CancelPaymentResponse> cancelPayment(PaymentType paymentType, String paymentId) {
        xs2aEventService.recordPisTppRequest(paymentId, EventType.PAYMENT_CANCELLATION_REQUEST_RECEIVED);

        AspspConsentData aspspConsentData = pisConsentDataService.getAspspConsentData(paymentId);
        Optional<PisCommonPaymentResponse> pisCommonPaymentOptional = pisConsentService.getPisCommonPaymentById(paymentId);

        if (!pisCommonPaymentOptional.isPresent()) {
            return ResponseObject.<CancelPaymentResponse>builder()
                       .fail(new MessageError(FORMAT_ERROR, "Consent not found"))
                       .build();
        }

        PisCommonPaymentResponse pisCommonPaymentResponse = pisCommonPaymentOptional.get();
        SpiPayment spiPayment = null;

        if (pisCommonPaymentResponse.getPaymentData() != null) {
            CommonPayment commonPayment = cmsToXs2aPaymentMapper.mapToXs2aCommonPayment(pisCommonPaymentResponse);
            spiPayment = xs2aToSpiPaymentInfoMapper.mapToSpiPaymentInfo(commonPayment);
        } else {
            PisPayment pisPayment = getPisPaymentFromConsent(pisCommonPaymentResponse);
            if (pisPayment == null) {
                return ResponseObject.<CancelPaymentResponse>builder()
                           .fail(new MessageError(FORMAT_ERROR, "Payment not found"))
                           .build();
            }

            Optional<? extends SpiPayment> spiPaymentOptional = spiPaymentFactory.createSpiPaymentByPaymentType(pisPayment, pisCommonPaymentResponse.getPaymentProduct(), paymentType);

            if (!spiPaymentOptional.isPresent()) {
                log.error("Unknown payment type: {}", paymentType);
                return ResponseObject.<CancelPaymentResponse>builder()
                           .fail(new MessageError(FORMAT_ERROR))
                           .build();
            }

            spiPayment = spiPaymentOptional.get();
        }

        Optional<PisCommonPaymentResponse> commonPayment = pisConsentService.getPisCommonPaymentById(paymentId);

        if (commonPayment.isPresent() && isFinalisedPayment(commonPayment.get())) {
            return ResponseObject.<CancelPaymentResponse>builder()
                       .fail(new MessageError(FORMAT_ERROR, "Payment is finalised already and cannot be cancelled"))
                       .build();
        }

        PsuIdData psuData = pisPsuDataService.getPsuDataByPaymentId(paymentId);
        SpiPsuData spiPsuData = psuDataMapper.mapToSpiPsuData(psuData);

        if (profileService.isPaymentCancellationAuthorizationMandated()) {
            return cancelPaymentService.initiatePaymentCancellation(spiPsuData, spiPayment);
        } else {
            ResponseObject<CancelPaymentResponse> cancellationResponse = cancelPaymentService.cancelPaymentWithoutAuthorisation(spiPsuData, spiPayment);
            pisConsentService.revokeConsentById(paymentId);
            return cancellationResponse;
        }
    }

    private boolean isFinalisedPayment(PisCommonPaymentResponse consent) {
        List<PisPayment> finalisedPayments = consent.getPayments().stream()
                                                 .filter(p -> p.getTransactionStatus().isFinalisedStatus())
                                                 .collect(Collectors.toList());

        return CollectionUtils.isNotEmpty(finalisedPayments);
    }

    private PisPayment getPisPaymentFromConsent(PisCommonPaymentResponse pisCommonPaymentResponse) {
        return Optional.of(pisCommonPaymentResponse)
                   .map(PisCommonPaymentResponse::getPayments)
                   .filter(CollectionUtils::isNotEmpty)
                   .map(payments -> payments.get(0))
                   .orElse(null);
    }
}
