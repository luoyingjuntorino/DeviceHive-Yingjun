package com.devicehive.websockets.handlers;

/*
 * #%L
 * DeviceHive Frontend Logic
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.auth.websockets.HiveWebsocketAuth;
import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.messages.handler.WebSocketClientHandler;
import com.devicehive.model.rpc.CountIexperimentRequest;
import com.devicehive.model.rpc.ListIexperimentRequest;
import com.devicehive.model.updates.IexperimentUpdate;
import com.devicehive.service.BaseIexperimentService;
import com.devicehive.service.IexperimentService;
import com.devicehive.vo.IexperimentVO;
import com.devicehive.vo.IexperimentWithUsersAndDevicesVO;
import com.devicehive.websockets.converters.WebSocketResponse;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.Optional;

import static com.devicehive.configuration.Constants.*;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Component
public class IexperimentHandlers {
    private static final Logger logger = LoggerFactory.getLogger(IexperimentHandlers.class);

    private final IexperimentService iexperimentService;
    private final WebSocketClientHandler webSocketClientHandler;
    private final Gson gson;

    @Autowired
    public IexperimentHandlers(IexperimentService iexperimentService, WebSocketClientHandler webSocketClientHandler, Gson gson) {
        this.iexperimentService = iexperimentService;
        this.webSocketClientHandler = webSocketClientHandler;
        this.gson = gson;
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_IEXPERIMENT')")
    public void processIexperimentList(JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ListIexperimentRequest listIexperimentRequest = ListIexperimentRequest.createListIexperimentRequest(request);
        listIexperimentRequest.setPrincipal(Optional.ofNullable(principal));

        String sortField = Optional.ofNullable(listIexperimentRequest.getSortField()).map(String::toLowerCase).orElse(null);
        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !NAME.equalsIgnoreCase(sortField)) {
            logger.error("Unable to proceed iexperiment list request. Invalid sortField");
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, BAD_REQUEST.getStatusCode());
        }

        WebSocketResponse response = new WebSocketResponse();
        if (!principal.areAllIexperimentsAvailable() && (principal.getIexperimentIds() == null || principal.getIexperimentIds().isEmpty())) {
            logger.warn("Unable to get list for empty iexperiments");
            response.addValue(IEXPERIMENTS, Collections.<IexperimentVO>emptyList(), IEXPERIMENTS_LISTED);
            webSocketClientHandler.sendMessage(request, response, session);
        } else {
            iexperimentService.list(listIexperimentRequest)
                    .thenAccept(iexperiments -> {
                        logger.debug("Iexperiment list request proceed successfully.");
                        response.addValue(IEXPERIMENTS, iexperiments, IEXPERIMENTS_LISTED);
                        webSocketClientHandler.sendMessage(request, response, session);
                    });
        }
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_IEXPERIMENT')")
    public void processIexperimentCount(JsonObject request, WebSocketSession session) {
        CountIexperimentRequest countIexperimentRequest = CountIexperimentRequest.createCountIexperimentRequest(request);

        WebSocketResponse response = new WebSocketResponse();
        iexperimentService.count(countIexperimentRequest)
                .thenAccept(count -> {
                    logger.debug("Iexperiment count request proceed successfully.");
                    response.addValue(COUNT, count.getCount(), null);
                    webSocketClientHandler.sendMessage(request, response, session);
                });
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#iexperimentId, 'GET_IEXPERIMENT')")
    public void processIexperimentGet(Long iexperimentId, JsonObject request, WebSocketSession session) {
        logger.debug("Iexperiment get requested.");
        if (iexperimentId == null) {
            logger.error(Messages.IEXPERIMENT_ID_REQUIRED);
            throw new HiveException(Messages.IEXPERIMENT_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        IexperimentWithUsersAndDevicesVO existing = iexperimentService.getWithDevices(iexperimentId);
        if (existing == null) {
            logger.error(String.format(Messages.IEXPERIMENT_NOT_FOUND, iexperimentId));
            throw new HiveException(String.format(Messages.IEXPERIMENT_NOT_FOUND, iexperimentId), NOT_FOUND.getStatusCode());
        }

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(IEXPERIMENT, existing, IEXPERIMENT_PUBLISHED);
        webSocketClientHandler.sendMessage(request, response, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_IEXPERIMENT')")
    public void processIexperimentInsert(JsonObject request, WebSocketSession session) {
        logger.debug("Iexperiment insert requested");
        IexperimentVO iexperiment = gson.fromJson(request.get(IEXPERIMENT), IexperimentVO.class);
        if (iexperiment == null) {
            logger.error(Messages.IEXPERIMENT_REQUIRED);
            throw new HiveException(Messages.IEXPERIMENT_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        IexperimentVO result = iexperimentService.create(iexperiment);
        logger.debug("New iexperiment has been created");

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(IEXPERIMENT, result, IEXPERIMENT_SUBMITTED);
        webSocketClientHandler.sendMessage(request, response, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#iexperimentId, 'MANAGE_IEXPERIMENT')")
    public void processIexperimentUpdate(Long iexperimentId, JsonObject request, WebSocketSession session) {
        IexperimentUpdate iexperimentToUpdate = gson.fromJson(request.get(IEXPERIMENT), IexperimentUpdate.class);
        logger.debug("Iexperiment update requested. Id : {}", iexperimentId);
        if (iexperimentId == null) {
            logger.error(Messages.IEXPERIMENT_ID_REQUIRED);
            throw new HiveException(Messages.IEXPERIMENT_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        iexperimentService.update(iexperimentId, iexperimentToUpdate);
        logger.debug("Iexperiment has been updated successfully. Id : {}", iexperimentId);
        webSocketClientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#iexperimentId, 'MANAGE_IEXPERIMENT')")
    public void processIexperimentDelete(Long iexperimentId, JsonObject request, WebSocketSession session) {
        logger.debug("Iexperiment delete requested");
        boolean force = Optional.ofNullable(gson.fromJson(request.get(FORCE), Boolean.class)).orElse(false);
        boolean isDeleted = iexperimentService.delete(iexperimentId, force);
        if (!isDeleted) {
            logger.error(String.format(Messages.IEXPERIMENT_NOT_FOUND, iexperimentId));
            throw new HiveException(String.format(Messages.IEXPERIMENT_NOT_FOUND, iexperimentId), NOT_FOUND.getStatusCode());
        }
        logger.debug("Iexperiment with id = {} does not exists any more.", iexperimentId);
        webSocketClientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

}
