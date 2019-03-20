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

package de.adorsys.psd2.impl;

import de.adorsys.psd2.xs2a.core.consent.AspspConsentData;
import de.adorsys.psd2.xs2a.core.pis.PisExecutionRule;
import de.adorsys.psd2.xs2a.core.pis.TransactionStatus;
import de.adorsys.psd2.xs2a.spi.domain.SpiContextData;
import de.adorsys.psd2.xs2a.spi.domain.account.SpiAccountReference;
import de.adorsys.psd2.xs2a.spi.domain.authorisation.SpiScaConfirmation;
import de.adorsys.psd2.xs2a.spi.domain.code.SpiFrequencyCode;
import de.adorsys.psd2.xs2a.spi.domain.common.SpiAmount;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiAddress;
import de.adorsys.psd2.xs2a.spi.domain.payment.SpiPeriodicPayment;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiPaymentExecutionResponse;
import de.adorsys.psd2.xs2a.spi.domain.payment.response.SpiPeriodicPaymentInitiationResponse;
import de.adorsys.psd2.xs2a.spi.domain.response.SpiResponse;
import de.adorsys.psd2.xs2a.spi.service.PeriodicPaymentSpi;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;

@Slf4j
@Service
public class PeriodicPaymentSpiMockImpl implements PeriodicPaymentSpi {
    private static final String TEST_ASPSP_DATA = "Test aspsp data";

    @Override
    @NotNull
    public SpiResponse<SpiPeriodicPaymentInitiationResponse> initiatePayment(@NotNull SpiContextData contextData, @NotNull SpiPeriodicPayment payment, @NotNull AspspConsentData initialAspspConsentData) {
        log.info("PeriodicPaymentSpi#initiatePayment: contextData {}, spiPeriodicPayment {}, aspspConsentData {}", contextData, payment, initialAspspConsentData);
        SpiPeriodicPaymentInitiationResponse response = new SpiPeriodicPaymentInitiationResponse();
        response.setTransactionStatus(TransactionStatus.RCVD);
        response.setPaymentId("5c90de7ca690c806c59d0260");
        response.setAspspAccountId("11111-11111");

        return SpiResponse.<SpiPeriodicPaymentInitiationResponse>builder()
                   .aspspConsentData(initialAspspConsentData.respondWith(TEST_ASPSP_DATA.getBytes()))
                   .payload(response)
                   .success();
    }

    @Override
    @NotNull
    public SpiResponse<SpiPeriodicPayment> getPaymentById(@NotNull SpiContextData contextData, @NotNull SpiPeriodicPayment payment, @NotNull AspspConsentData aspspConsentData) {
        log.info("PeriodicPaymentSpi#getPaymentById: contextData {}, spiPeriodicPayment {}, aspspConsentData {}", contextData, payment, aspspConsentData);
        SpiPeriodicPayment spiPeriodicPayment = new SpiPeriodicPayment("sepa-credit-transfers");
        spiPeriodicPayment.setPaymentId("5c90de7ca690c806c59d0260");
        spiPeriodicPayment.setEndToEndIdentification("RI-1234567890");
        spiPeriodicPayment.setDebtorAccount(new SpiAccountReference("11111-11111", "DE89370400440532013000", null, null, null, null, Currency.getInstance("USD")));
        spiPeriodicPayment.setInstructedAmount(new SpiAmount(Currency.getInstance("USD"), new BigDecimal(1000)));
        spiPeriodicPayment.setCreditorAccount(new SpiAccountReference(null, "DE89370400440532013000", null, null, null, null, Currency.getInstance("USD")));
        spiPeriodicPayment.setCreditorAgent("FSDFSASGSGF");
        spiPeriodicPayment.setCreditorName("Telekom");
        spiPeriodicPayment.setCreditorAddress(new SpiAddress("Herrnstraße", "123-34", "Nürnberg", "90431", "DE"));
        spiPeriodicPayment.setRemittanceInformationUnstructured("Ref. Number TELEKOM-1222");
        spiPeriodicPayment.setRequestedExecutionDate(LocalDate.of(2020, Month.JANUARY, 1));
        spiPeriodicPayment.setRequestedExecutionTime(OffsetDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC));
        spiPeriodicPayment.setStartDate(LocalDate.of(2020, Month.JANUARY, 1));
        spiPeriodicPayment.setEndDate(LocalDate.of(2021, Month.FEBRUARY, 1));
        spiPeriodicPayment.setExecutionRule(PisExecutionRule.PRECEEDING);
        spiPeriodicPayment.setFrequency(SpiFrequencyCode.ANNUAL);
        spiPeriodicPayment.setPaymentStatus(TransactionStatus.RCVD);

        return SpiResponse.<SpiPeriodicPayment>builder()
                   .aspspConsentData(aspspConsentData)
                   .payload(spiPeriodicPayment)
                   .success();
    }

    @Override
    @NotNull
    public SpiResponse<TransactionStatus> getPaymentStatusById(@NotNull SpiContextData contextData, @NotNull SpiPeriodicPayment payment, @NotNull AspspConsentData aspspConsentData) {
        log.info("PeriodicPaymentSpi#getPaymentStatusById: contextData {}, spiPeriodicPayment {}, aspspConsentData {}", contextData, payment, aspspConsentData);

        return SpiResponse.<TransactionStatus>builder()
                   .aspspConsentData(aspspConsentData)
                   .payload(TransactionStatus.RCVD)
                   .success();
    }

    @Override
    @NotNull
    public SpiResponse<SpiPaymentExecutionResponse> executePaymentWithoutSca(@NotNull SpiContextData contextData, @NotNull SpiPeriodicPayment payment, @NotNull AspspConsentData aspspConsentData) {
        log.info("PeriodicPaymentSpi#executePaymentWithoutSca: contextData {}, spiPeriodicPayment {}, aspspConsentData {}", contextData, payment, aspspConsentData);

        return SpiResponse.<SpiPaymentExecutionResponse>builder()
                   .aspspConsentData(aspspConsentData)
                   .payload(new SpiPaymentExecutionResponse(TransactionStatus.ACCP))
                   .success();
    }

    @Override
    @NotNull
    public SpiResponse<SpiPaymentExecutionResponse> verifyScaAuthorisationAndExecutePayment(@NotNull SpiContextData contextData, @NotNull SpiScaConfirmation spiScaConfirmation, @NotNull SpiPeriodicPayment payment, @NotNull AspspConsentData aspspConsentData) {
        log.info("PeriodicPaymentSpi#verifyScaAuthorisationAndExecutePayment: contextData {}, spiScaConfirmation{}, spiPeriodicPayment {}, aspspConsentData {}", contextData, spiScaConfirmation, payment, aspspConsentData);

        return SpiResponse.<SpiPaymentExecutionResponse>builder()
                   .aspspConsentData(aspspConsentData)
                   .payload(new SpiPaymentExecutionResponse(TransactionStatus.ACCP))
                   .success();
    }
}
