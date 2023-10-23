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
import com.devicehive.model.updates.IexperimentUpdate;
import com.devicehive.resource.IexperimentResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.BaseIexperimentService;
import com.devicehive.service.IexperimentService;
import com.devicehive.vo.IexperimentVO;
import com.devicehive.vo.IexperimentWithUsersAndDevicesVO;
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
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.IEXPERIMENTS_LISTED;
import static javax.ws.rs.core.Response.Status.*;

@Service
public class IexperimentResourceImpl implements IexperimentResource {

    private static final Logger logger = LoggerFactory.getLogger(IexperimentResourceImpl.class);

    private final IexperimentService iexperimentService;

    @Autowired
    public IexperimentResourceImpl(IexperimentService iexperimentService) {
        this.iexperimentService = iexperimentService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void list(String name, String namePattern, String sortField, String sortOrder, Integer take, Integer skip,
                     @Suspended final AsyncResponse asyncResponse) {

        logger.debug("Iexperiment list requested");

        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !NAME.equalsIgnoreCase(sortField)) {
            logger.error("Unable to proceed iexperiment list request. Invalid sortField");
            final Response response = ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
            asyncResponse.resume(response);
        } else if (sortField != null) {
            sortField = sortField.toLowerCase();
        }
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!principal.areAllIexperimentsAvailable() && (principal.getIexperimentIds() == null || principal.getIexperimentIds().isEmpty())) {
            logger.warn("Unable to get list for empty iexperiments");
            final Response response = ResponseFactory.response(OK, Collections.<IexperimentVO>emptyList(), IEXPERIMENTS_LISTED);
            asyncResponse.resume(response);
        } else {
            iexperimentService.list(name, namePattern, sortField, sortOrder, take, skip, principal)
                    .thenApply(iexperiments -> {
                        logger.debug("Iexperiment list request proceed successfully.");
                        return ResponseFactory.response(OK, iexperiments, IEXPERIMENTS_LISTED);
                    }).thenAccept(asyncResponse::resume);
        }
    }

    @Override
    public void count(String name, String namePattern, AsyncResponse asyncResponse) {
        logger.debug("Iexperiment count requested");
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        iexperimentService.count(name, namePattern, principal)
                .thenApply(count -> {
                    logger.debug("Iexperiment count request proceed successfully.");
                    return ResponseFactory.response(OK, count, IEXPERIMENTS_LISTED);
                }).thenAccept(asyncResponse::resume);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response get(long id) {
        logger.debug("Iexperiment get requested.");
        IexperimentWithUsersAndDevicesVO existing = iexperimentService.getWithDevices(id);
        if (existing == null) {
            logger.error("Iexperiment with id =  {} does not exists", id);
            return ResponseFactory.response(Response.Status.NOT_FOUND, new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.IEXPERIMENT_NOT_FOUND, id)));
        }
        return ResponseFactory.response(OK, existing, JsonPolicyDef.Policy.IEXPERIMENT_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insert(IexperimentUpdate iexperiment) {
        logger.debug("Iexperiment insert requested");
        IexperimentVO result = iexperimentService.create(iexperiment.convertTo());
        logger.debug("New iexperiment has been created");
        return ResponseFactory.response(CREATED, result, JsonPolicyDef.Policy.IEXPERIMENT_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response update(IexperimentUpdate iexperimentToUpdate, long id) {
        logger.debug("Iexperiment update requested. Id : {}", id);
        iexperimentService.update(id, iexperimentToUpdate);
        logger.debug("Iexperiment has been updated successfully. Id : {}", id);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response delete(long id, boolean force) {
        logger.debug("Iexperiment delete requested");
        boolean isDeleted = iexperimentService.delete(id, force);
        if (!isDeleted) {
            logger.error(String.format(Messages.IEXPERIMENT_NOT_FOUND, id));
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.IEXPERIMENT_NOT_FOUND, id)));
        }
        logger.debug("Iexperiment with id = {} does not exists any more.", id);
        return ResponseFactory.response(NO_CONTENT);
    }
}