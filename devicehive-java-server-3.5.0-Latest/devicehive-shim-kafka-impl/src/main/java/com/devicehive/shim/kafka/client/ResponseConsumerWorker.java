package com.devicehive.shim.kafka.client;

/*
 * #%L
 * DeviceHive Shim Kafka Implementation
 * %%
 * Copyright (C) 2016 DataArt
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.devicehive.api.RequestResponseMatcher;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.kafka.ConsumerWorker;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.util.concurrent.CountDownLatch;


public class ResponseConsumerWorker extends ConsumerWorker<Response> {

    private RequestResponseMatcher responseMatcher;

    public ResponseConsumerWorker(String topic, RequestResponseMatcher responseMatcher,
                                  KafkaConsumer<String, Response> consumer, CountDownLatch latch) {
        super(topic, consumer, latch);
        this.responseMatcher = responseMatcher;
    }

    @Override
    public void process(ConsumerRecord<String, Response> record) {
        responseMatcher.offerResponse(record.value());
    }
}
