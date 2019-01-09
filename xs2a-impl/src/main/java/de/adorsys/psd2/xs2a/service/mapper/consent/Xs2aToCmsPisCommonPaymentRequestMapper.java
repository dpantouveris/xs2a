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

package de.adorsys.psd2.xs2a.service.mapper.consent;

import de.adorsys.psd2.consent.api.CmsAddress;
import de.adorsys.psd2.consent.api.pis.CmsRemittance;
import de.adorsys.psd2.consent.api.pis.PisPayment;
import de.adorsys.psd2.consent.api.pis.proto.PisCommonPaymentRequest;
import de.adorsys.psd2.consent.api.pis.proto.PisPaymentInfo;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.core.profile.PaymentType;
import de.adorsys.psd2.xs2a.core.tpp.TppInfo;
import de.adorsys.psd2.xs2a.domain.address.Xs2aAddress;
import de.adorsys.psd2.xs2a.domain.address.Xs2aCountryCode;
import de.adorsys.psd2.xs2a.domain.code.Xs2aPurposeCode;
import de.adorsys.psd2.xs2a.domain.pis.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class Xs2aToCmsPisCommonPaymentRequestMapper {

    public PisCommonPaymentRequest mapToCmsPisCommonPaymentRequest(CommonPayment paymentInitRequest) {
        PisCommonPaymentRequest request = new PisCommonPaymentRequest();
        request.setPayments(Collections.emptyList());
        request.setPaymentInfo(mapToPisPaymentInfo(paymentInitRequest));
        request.setPaymentId(paymentInitRequest.getPaymentId());
        request.setPaymentType(paymentInitRequest.getPaymentType());
        request.setPaymentProduct(paymentInitRequest.getPaymentProduct());
        // TODO put real tppInfo data https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/406
        request.setTppInfo(new TppInfo());
        request.setPsuData(paymentInitRequest.getPsuDataList());
        return request;
    }

    public PisPaymentInfo mapToPisPaymentInfo(PaymentInitiationParameters paymentInitiationParameters, TppInfo tppInfo, TransactionStatus transactionStatus, String paymentId, byte[] paymentData) {
        PisPaymentInfo request = mapToPisPaymentInfo(paymentInitiationParameters, tppInfo, transactionStatus, paymentId);
        request.setPaymentData(paymentData);
        return request;
    }

    private PisPaymentInfo mapToPisPaymentInfo(CommonPayment paymentInitRequest) {
        return Optional.ofNullable(paymentInitRequest)
                   .map(dta -> {
                            PisPaymentInfo paymentInfo = new PisPaymentInfo();
                            paymentInfo.setPaymentId(dta.getPaymentId());
                            paymentInfo.setPaymentProduct(dta.getPaymentProduct());
                            paymentInfo.setPaymentType(dta.getPaymentType());
                            paymentInfo.setTransactionStatus(dta.getTransactionStatus());
                            paymentInfo.setPaymentData(dta.getPaymentData());
                            paymentInfo.setPsuDataList(dta.getPsuDataList());
                            paymentInfo.setTppInfo(dta.getTppInfo());
                            return paymentInfo;
                        }
                   )
                   .orElse(null);
    }

    public PisPaymentInfo mapToPisPaymentInfo(PaymentInitiationParameters paymentInitiationParameters, TppInfo tppInfo, TransactionStatus transactionStatus, String paymentId) {
        PisPaymentInfo request = new PisPaymentInfo();
        request.setPaymentProduct(paymentInitiationParameters.getPaymentProduct());
        request.setPaymentType(paymentInitiationParameters.getPaymentType());
        request.setTransactionStatus(transactionStatus);
        request.setPaymentData(null);
        request.setTppInfo(tppInfo);
        request.setPaymentId(paymentId);
        request.setPsuDataList(Collections.singletonList(paymentInitiationParameters.getPsuData()));
        return request;
    }

    public PisCommonPaymentRequest mapToCmsSinglePisCommonPaymentRequest(SinglePayment singlePayment, String paymentProduct) {
        PisCommonPaymentRequest request = new PisCommonPaymentRequest();
        request.setPayments(Collections.singletonList(mapToPisPaymentForSinglePayment(singlePayment)));
        request.setPaymentProduct(paymentProduct);
        // TODO put real tppInfo data https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/406
        request.setTppInfo(new TppInfo());
        return request;
    }

    public PisCommonPaymentRequest mapToCmsPeriodicPisCommonPaymentRequest(PeriodicPayment periodicPayment, String paymentProduct) {
        PisCommonPaymentRequest request = new PisCommonPaymentRequest();
        request.setPayments(Collections.singletonList(mapToPisPaymentForPeriodicPayment(periodicPayment)));
        request.setPaymentProduct(paymentProduct);
        // TODO put real tppInfo data https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/406
        request.setTppInfo(new TppInfo());
        return request;
    }

    public PisCommonPaymentRequest mapToCmsBulkPisCommonPaymentRequest(BulkPayment bulkPayment, String paymentProduct) {
        PisCommonPaymentRequest request = new PisCommonPaymentRequest();
        request.setPaymentId(bulkPayment.getPaymentId());
        request.setPayments(mapToListPisPayment(bulkPayment.getPayments()));
        request.setPaymentProduct(paymentProduct);
        request.setPaymentType(PaymentType.BULK);
        // TODO put real tppInfo data https://git.adorsys.de/adorsys/xs2a/aspsp-xs2a/issues/406
        request.setTppInfo(new TppInfo());
        return request;

    }

    private List<PisPayment> mapToListPisPayment(List<SinglePayment> payments) {
        return payments.stream()
                   .map(this::mapToPisPaymentForSinglePayment)
                   .collect(Collectors.toList());
    }

    private PisPayment mapToPisPaymentForSinglePayment(SinglePayment payment) {
        return Optional.ofNullable(payment)
                   .map(pmt -> {
                       PisPayment pisPayment = new PisPayment();

                       pisPayment.setPaymentId(pmt.getPaymentId());
                       pisPayment.setEndToEndIdentification(pmt.getEndToEndIdentification());
                       pisPayment.setDebtorAccount(pmt.getDebtorAccount());
                       pisPayment.setUltimateDebtor(pmt.getUltimateDebtor());
                       pisPayment.setCurrency(pmt.getInstructedAmount().getCurrency());
                       pisPayment.setAmount(new BigDecimal(pmt.getInstructedAmount().getAmount())); // todo remake amount type from String to BigDecimal
                       pisPayment.setCreditorAccount(pmt.getCreditorAccount());
                       pisPayment.setCreditorAgent(pmt.getCreditorAgent());
                       pisPayment.setCreditorName(pmt.getCreditorName());
                       pisPayment.setCreditorAddress(mapToCmsAddress(pmt.getCreditorAddress()));
                       pisPayment.setRemittanceInformationUnstructured(pmt.getRemittanceInformationUnstructured());
                       pisPayment.setRemittanceInformationStructured(mapToCmsRemittance(pmt.getRemittanceInformationStructured()));
                       pisPayment.setRequestedExecutionDate(pmt.getRequestedExecutionDate());
                       pisPayment.setRequestedExecutionTime(pmt.getRequestedExecutionTime());
                       pisPayment.setUltimateCreditor(pmt.getUltimateCreditor());
                       pisPayment.setPurposeCode(Optional.ofNullable(pmt.getPurposeCode())
                                                     .map(Xs2aPurposeCode::getCode)
                                                     .orElse(""));

                       return pisPayment;

                   }).orElse(null);
    }

    private PisPayment mapToPisPaymentForPeriodicPayment(PeriodicPayment payment) {
        return Optional.ofNullable(payment)
                   .map(pmt -> {
                       PisPayment pisPayment = new PisPayment();

                       pisPayment.setPaymentId(pmt.getPaymentId());
                       pisPayment.setEndToEndIdentification(pmt.getEndToEndIdentification());
                       pisPayment.setDebtorAccount(pmt.getDebtorAccount());
                       pisPayment.setUltimateDebtor(pmt.getUltimateDebtor());
                       pisPayment.setCurrency(pmt.getInstructedAmount().getCurrency());
                       pisPayment.setAmount(new BigDecimal(pmt.getInstructedAmount().getAmount())); // todo remake amount type from String to BigDecimal
                       pisPayment.setCreditorAccount(pmt.getCreditorAccount());
                       pisPayment.setCreditorAgent(pmt.getCreditorAgent());
                       pisPayment.setCreditorName(pmt.getCreditorName());
                       pisPayment.setCreditorAddress(mapToCmsAddress(pmt.getCreditorAddress()));
                       pisPayment.setRemittanceInformationUnstructured(pmt.getRemittanceInformationUnstructured());
                       pisPayment.setRemittanceInformationStructured(mapToCmsRemittance(pmt.getRemittanceInformationStructured()));
                       pisPayment.setRequestedExecutionDate(pmt.getRequestedExecutionDate());
                       pisPayment.setRequestedExecutionTime(pmt.getRequestedExecutionTime());
                       pisPayment.setUltimateCreditor(pmt.getUltimateCreditor());
                       pisPayment.setPurposeCode(Optional.ofNullable(pmt.getPurposeCode())
                                                     .map(Xs2aPurposeCode::getCode)
                                                     .orElse(""));
                       pisPayment.setStartDate(pmt.getStartDate());
                       pisPayment.setEndDate(pmt.getEndDate());
                       pisPayment.setExecutionRule(pmt.getExecutionRule());
                       pisPayment.setFrequency(pmt.getFrequency().name());
                       pisPayment.setDayOfExecution(pmt.getDayOfExecution());

                       return pisPayment;
                   }).orElse(null);
    }

    private CmsAddress mapToCmsAddress(Xs2aAddress address) {
        return Optional.ofNullable(address)
                   .map(adr -> {
                       CmsAddress cmsAddress = new CmsAddress();
                       cmsAddress.setStreet(adr.getStreet());
                       cmsAddress.setBuildingNumber(adr.getBuildingNumber());
                       cmsAddress.setCity(adr.getCity());
                       cmsAddress.setPostalCode(adr.getPostalCode());
                       cmsAddress.setCountry(Optional.ofNullable(adr.getCountry()).map(Xs2aCountryCode::getCode).orElse(null));
                       return cmsAddress;
                   }).orElseGet(CmsAddress::new);
    }

    private CmsRemittance mapToCmsRemittance(Remittance remittance) {
        return Optional.ofNullable(remittance)
                   .map(rm -> {
                       CmsRemittance cmsRemittance = new CmsRemittance();
                       cmsRemittance.setReference(rm.getReference());
                       cmsRemittance.setReferenceIssuer(rm.getReferenceIssuer());
                       cmsRemittance.setReferenceType(rm.getReferenceType());
                       return cmsRemittance;
                   })
                   .orElseGet(CmsRemittance::new);
    }
}
