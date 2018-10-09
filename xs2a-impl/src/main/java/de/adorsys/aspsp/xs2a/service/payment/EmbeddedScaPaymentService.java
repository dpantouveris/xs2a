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

package de.adorsys.aspsp.xs2a.service.payment;

import de.adorsys.aspsp.xs2a.service.mapper.PaymentMapper;
import de.adorsys.aspsp.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiBulkPaymentMapper;
import de.adorsys.aspsp.xs2a.service.mapper.spi_xs2a_mappers.Xs2aToSpiPaymentMapper;
import de.adorsys.aspsp.xs2a.spi.service.PaymentSpi;
import de.adorsys.aspsp.xs2a.spi.service.v2.BulkPaymentSpi;
import de.adorsys.aspsp.xs2a.spi.service.v2.SinglePaymentSpi;
import org.springframework.stereotype.Service;

@Service
public class EmbeddedScaPaymentService extends RedirectAndEmbeddedPaymentService {
    public EmbeddedScaPaymentService(PaymentSpi paymentSpi, PaymentMapper paymentMapper, SinglePaymentSpi singlePaymentSpi, BulkPaymentSpi bulkPaymentSpi, Xs2aToSpiPaymentMapper xs2aToSpiPaymentMapper, Xs2aToSpiBulkPaymentMapper xs2aToSpiBulkPaymentMapper) {
        super(paymentSpi, paymentMapper, singlePaymentSpi, bulkPaymentSpi, xs2aToSpiPaymentMapper, xs2aToSpiBulkPaymentMapper);
    }
}
