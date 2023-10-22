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
import com.devicehive.model.rpc.CountIcomponentRequest;
import com.devicehive.model.rpc.ListIcomponentRequest;
import com.devicehive.model.updates.IcomponentUpdate;
import com.devicehive.service.BaseIcomponentService;
import com.devicehive.service.IcomponentService;
import com.devicehive.vo.IcomponentVO;
import com.devicehive.vo.IcomponentWithUsersAndDevicesVO;
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
public class IcomponentHandlers {
    private static final Logger logger = LoggerFactory.getLogger(IcomponentHandlers.class);

    private final IcomponentService icomponentService;
    private final WebSocketClientHandler webSocketClientHandler;
    private final Gson gson;

    @Autowired
    public IcomponentHandlers(IcomponentService icomponentService, WebSocketClientHandler webSocketClientHandler, Gson gson) {
        this.icomponentService = icomponentService;
        this.webSocketClientHandler = webSocketClientHandler;
        this.gson = gson;
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_ICOMPONENT')")
    public void processIcomponentList(JsonObject request, WebSocketSession session) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ListIcomponentRequest listIcomponentRequest = ListIcomponentRequest.createListIcomponentRequest(request);
        listIcomponentRequest.setPrincipal(Optional.ofNullable(principal));

        String sortField = Optional.ofNullable(listIcomponentRequest.getSortField()).map(String::toLowerCase).orElse(null);
        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !NAME.equalsIgnoreCase(sortField)) {
            logger.error("Unable to proceed icomponent list request. Invalid sortField");
            throw new HiveException(Messages.INVALID_REQUEST_PARAMETERS, BAD_REQUEST.getStatusCode());
        }

        WebSocketResponse response = new WebSocketResponse();
        if (!principal.areAllIcomponentsAvailable() && (principal.getIcomponentIds() == null || principal.getIcomponentIds().isEmpty())) {
            logger.warn("Unable to get list for empty icomponents");
            response.addValue(ICOMPONENTS, Collections.<IcomponentVO>emptyList(), ICOMPONENTS_LISTED);
            webSocketClientHandler.sendMessage(request, response, session);
        } else {
            icomponentService.list(listIcomponentRequest)
                    .thenAccept(icomponents -> {
                        logger.debug("Icomponent list request proceed successfully.");
                        response.addValue(ICOMPONENTS, icomponents, ICOMPONENTS_LISTED);
                        webSocketClientHandler.sendMessage(request, response, session);
                    });
        }
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'GET_ICOMPONENT')")
    public void processIcomponentCount(JsonObject request, WebSocketSession session) {
        CountIcomponentRequest countIcomponentRequest = CountIcomponentRequest.createCountIcomponentRequest(request);

        WebSocketResponse response = new WebSocketResponse();
        icomponentService.count(countIcomponentRequest)
                .thenAccept(count -> {
                    logger.debug("Icomponent count request proceed successfully.");
                    response.addValue(COUNT, count.getCount(), null);
                    webSocketClientHandler.sendMessage(request, response, session);
                });
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#icomponentId, 'GET_ICOMPONENT')")
    public void processIcomponentGet(Long icomponentId, JsonObject request, WebSocketSession session) {
        logger.debug("Icomponent get requested.");
        if (icomponentId == null) {
            logger.error(Messages.ICOMPONENT_ID_REQUIRED);
            throw new HiveException(Messages.ICOMPONENT_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }

        IcomponentWithUsersAndDevicesVO existing = icomponentService.getWithDevices(icomponentId);
        if (existing == null) {
            logger.error(String.format(Messages.ICOMPONENT_NOT_FOUND, icomponentId));
            throw new HiveException(String.format(Messages.ICOMPONENT_NOT_FOUND, icomponentId), NOT_FOUND.getStatusCode());
        }

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(ICOMPONENT, existing, ICOMPONENT_PUBLISHED);
        webSocketClientHandler.sendMessage(request, response, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(null, 'MANAGE_ICOMPONENT')")
    public void processIcomponentInsert(JsonObject request, WebSocketSession session) {
        logger.debug("Icomponent insert requested");
        IcomponentVO icomponent = gson.fromJson(request.get(ICOMPONENT), IcomponentVO.class);
        if (icomponent == null) {
            logger.error(Messages.ICOMPONENT_REQUIRED);
            throw new HiveException(Messages.ICOMPONENT_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        IcomponentVO result = icomponentService.create(icomponent);
        logger.debug("New icomponent has been created");

        WebSocketResponse response = new WebSocketResponse();
        response.addValue(ICOMPONENT, result, ICOMPONENT_SUBMITTED);
        webSocketClientHandler.sendMessage(request, response, session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#icomponentId, 'MANAGE_ICOMPONENT')")
    public void processIcomponentUpdate(Long icomponentId, JsonObject request, WebSocketSession session) {
        IcomponentUpdate icomponentToUpdate = gson.fromJson(request.get(ICOMPONENT), IcomponentUpdate.class);
        logger.debug("Icomponent update requested. Id : {}", icomponentId);
        if (icomponentId == null) {
            logger.error(Messages.ICOMPONENT_ID_REQUIRED);
            throw new HiveException(Messages.ICOMPONENT_ID_REQUIRED, BAD_REQUEST.getStatusCode());
        }
        icomponentService.update(icomponentId, icomponentToUpdate);
        logger.debug("Icomponent has been updated successfully. Id : {}", icomponentId);
        webSocketClientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

    @HiveWebsocketAuth
    @PreAuthorize("isAuthenticated() and hasPermission(#icomponentId, 'MANAGE_ICOMPONENT')")
    public void processIcomponentDelete(Long icomponentId, JsonObject request, WebSocketSession session) {
        logger.debug("Icomponent delete requested");
        boolean force = Optional.ofNullable(gson.fromJson(request.get(FORCE), Boolean.class)).orElse(false);
        boolean isDeleted = icomponentService.delete(icomponentId, force);
        if (!isDeleted) {
            logger.error(String.format(Messages.ICOMPONENT_NOT_FOUND, icomponentId));
            throw new HiveException(String.format(Messages.ICOMPONENT_NOT_FOUND, icomponentId), NOT_FOUND.getStatusCode());
        }
        logger.debug("Icomponent with id = {} does not exists any more.", icomponentId);
        webSocketClientHandler.sendMessage(request, new WebSocketResponse(), session);
    }

}
