package com.devicehive.resource.impl;

/*
 * #%L
 * DeviceHive Java Server Common business logic
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

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.ErrorResponse;
import com.devicehive.model.updates.IcomponentUpdate;
import com.devicehive.resource.IcomponentResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.BaseIcomponentService;
import com.devicehive.service.IcomponentService;
import com.devicehive.vo.IcomponentVO;
import com.devicehive.vo.IcomponentWithUsersAndDevicesVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static com.devicehive.configuration.Constants.ID;
import static com.devicehive.configuration.Constants.NAME;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.ICOMPONENTS_LISTED;
import static javax.ws.rs.core.Response.Status.*;

@Service
public class IcomponentResourceImpl implements IcomponentResource {

    private static final Logger logger = LoggerFactory.getLogger(IcomponentResourceImpl.class);

    private final IcomponentService icomponentService;

    @Autowired
    public IcomponentResourceImpl(IcomponentService icomponentService) {
        this.icomponentService = icomponentService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void list(String name, String namePattern, String sortField, String sortOrder, Integer take, Integer skip,
                     @Suspended final AsyncResponse asyncResponse) {

        logger.debug("Icomponent list requested");

        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !NAME.equalsIgnoreCase(sortField)) {
            logger.error("Unable to proceed icomponent list request. Invalid sortField");
            final Response response = ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
            asyncResponse.resume(response);
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!principal.areAllIcomponentsAvailable() && (principal.getIcomponentIds() == null || principal.getIcomponentIds().isEmpty())) {
            logger.warn("Unable to get list for empty icomponents");
            final Response response = ResponseFactory.response(OK, Collections.<IcomponentVO>emptyList(), ICOMPONENTS_LISTED);
            asyncResponse.resume(response);
        } else {
            icomponentService.list(name, namePattern, sortField, sortOrder, take, skip, principal)
                    .thenApply(icomponents -> {
                        logger.debug("Icomponent list request proceed successfully.");
                        return ResponseFactory.response(OK, icomponents, ICOMPONENTS_LISTED);
                    }).thenAccept(asyncResponse::resume);
        }
    }

    @Override
    public void count(String name, String namePattern, AsyncResponse asyncResponse) {
        logger.debug("Icomponent count requested");
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        icomponentService.count(name, namePattern, principal)
                .thenApply(count -> {
                    logger.debug("Icomponent count request proceed successfully.");
                    return ResponseFactory.response(OK, count, ICOMPONENTS_LISTED);
                }).thenAccept(asyncResponse::resume);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response get(long id) {
        logger.debug("Icomponent get requested.");
        IcomponentWithUsersAndDevicesVO existing = icomponentService.getWithDevices(id);
        if (existing == null) {
            logger.error("Icomponent with id =  {} does not exists", id);
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.ICOMPONENT_NOT_FOUND, id)));
        }
        return ResponseFactory.response(OK, existing, JsonPolicyDef.Policy.ICOMPONENT_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insert(IcomponentUpdate icomponent) {
        logger.debug("Icomponent insert requested");
        IcomponentVO result = icomponentService.create(icomponent.convertTo());
        logger.debug("New icomponent has been created");
        return ResponseFactory.response(CREATED, result, JsonPolicyDef.Policy.ICOMPONENT_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response update(IcomponentUpdate icomponentToUpdate, long id) {
        logger.debug("Icomponent update requested. Id : {}", id);
        icomponentService.update(id, icomponentToUpdate);
        logger.debug("Icomponent has been updated successfully. Id : {}", id);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response delete(long id, boolean force) {
        logger.debug("Icomponent delete requested");
        boolean isDeleted = icomponentService.delete(id, force);
        if (!isDeleted) {
            logger.error(String.format(Messages.ICOMPONENT_NOT_FOUND, id));
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.ICOMPONENT_NOT_FOUND, id)));
        }
        logger.debug("Icomponent with id = {} does not exists any more.", id);
        return ResponseFactory.response(NO_CONTENT);
    }
}