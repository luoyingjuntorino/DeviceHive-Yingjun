package com.devicehive.messages.handler.dao.count;


/*
 * #%L
 * DeviceHive Backend Logic
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

import com.devicehive.dao.IcomponentDao;
import com.devicehive.model.rpc.CountIcomponentRequest;
import com.devicehive.model.rpc.CountResponse;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.server.RequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CountIcomponentHandler implements RequestHandler {

    private IcomponentDao icomponentDao;

    @Autowired
    public void setIcomponentDao(IcomponentDao icomponentDao) {
        this.icomponentDao = icomponentDao;
    }

    @Override
    public Response handle(Request request) {

        final CountIcomponentRequest req = (CountIcomponentRequest) request.getBody();

        final long count = icomponentDao.count(req.getName(), req.getNamePattern(), req.getPrincipal());

        final CountResponse countResponse = new CountResponse(count);

        return Response.newBuilder()
                .withBody(countResponse)
                .buildSuccess();
    }
}
