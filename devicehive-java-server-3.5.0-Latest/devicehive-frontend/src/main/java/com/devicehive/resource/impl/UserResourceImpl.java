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
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.response.UserIexperimentResponse;
import com.devicehive.model.response.UserIcomponentResponse;
import com.devicehive.model.response.UserNetworkResponse;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.resource.UserResource;
import com.devicehive.resource.util.ResponseFactory;
import com.devicehive.service.BaseIexperimentService;
import com.devicehive.service.IexperimentService;
import com.devicehive.service.BaseIcomponentService;
import com.devicehive.service.IcomponentService;
import com.devicehive.service.UserService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Objects;

import static com.devicehive.configuration.Constants.ID;
import static com.devicehive.configuration.Constants.LOGIN;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.IEXPERIMENTS_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.ICOMPONENTS_LISTED;
import static javax.ws.rs.core.Response.Status.*;

@Service
public class UserResourceImpl implements UserResource {

    private static final Logger logger = LoggerFactory.getLogger(UserResourceImpl.class);

    private final UserService userService;
    private final IexperimentService iexperimentService;
    private final IcomponentService icomponentService;
    private final HiveValidator hiveValidator;

    @Autowired
    public UserResourceImpl(UserService userService, IexperimentService iexperimentService, IcomponentService icomponentService, HiveValidator hiveValidator) {
        this.userService = userService;
        this.iexperimentService = iexperimentService;
        this.icomponentService = icomponentService;
        this.hiveValidator = hiveValidator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void list(String login, String loginPattern, Integer role, Integer status, String sortField,
            String sortOrder, Integer take, Integer skip, @Suspended final AsyncResponse asyncResponse) {

        if (sortField != null && !ID.equalsIgnoreCase(sortField) && !LOGIN.equalsIgnoreCase(sortField)) {
            final Response response = ResponseFactory.response(BAD_REQUEST,
                    new ErrorResponse(BAD_REQUEST.getStatusCode(),
                            Messages.INVALID_REQUEST_PARAMETERS));
            asyncResponse.resume(response);
        } else {
            if (sortField != null) {
                sortField = sortField.toLowerCase();
            }

            userService.list(login, loginPattern, role, status, sortField, sortOrder, take, skip)
                    .thenApply(users -> {
                        logger.debug("User list request proceed successfully");

                        return ResponseFactory.response(OK, users, JsonPolicyDef.Policy.USERS_LISTED);
                    }).thenAccept(asyncResponse::resume);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void count(String login, String loginPattern, Integer role, Integer status, AsyncResponse asyncResponse) {
        logger.debug("User count requested");

        userService.count(login, loginPattern, role, status)
                .thenApply(count -> {
                    logger.debug("User count request proceed successfully");
                    return ResponseFactory.response(OK, count, JsonPolicyDef.Policy.USERS_LISTED);
                }).thenAccept(asyncResponse::resume);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getUser(Long userId) {
        UserVO currentLoggedInUser = findCurrentUserFromAuthContext();

        UserWithNetworkVO fetchedUser = null;

        if (currentLoggedInUser != null && currentLoggedInUser.getRole() == UserRole.ADMIN) {
            fetchedUser = userService.findUserWithNetworks(userId);
        } else if (currentLoggedInUser != null && currentLoggedInUser.getRole() == UserRole.CLIENT && Objects.equals(currentLoggedInUser.getId(), userId)) {
            fetchedUser = userService.findUserWithNetworks(currentLoggedInUser.getId());
        } else {
            return ResponseFactory.response(FORBIDDEN,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.USER_NOT_FOUND, userId)));
        }

        if (fetchedUser == null) {
            logger.error("Can't get user with id {}: user not found", userId);
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.USER_NOT_FOUND, userId)));
        }

        return ResponseFactory.response(OK, fetchedUser, JsonPolicyDef.Policy.USER_PUBLISHED);
    }

    @Override
    public Response getCurrent() {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long id = principal.getUser().getId();
        UserVO currentUser = userService.findUserWithNetworks(id);

        if (currentUser == null) {
            return ResponseFactory.response(CONFLICT, new ErrorResponse(CONFLICT.getStatusCode(), Messages.CAN_NOT_GET_CURRENT_USER));
        }

        return ResponseFactory.response(OK, currentUser, JsonPolicyDef.Policy.USER_PUBLISHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response insertUser(UserUpdate userToCreate) {
        hiveValidator.validate(userToCreate);
        String password = userToCreate.getPassword().orElse(null);
        UserVO created = userService.createUser(userToCreate.convertTo(), password);
        return ResponseFactory.response(CREATED, created, JsonPolicyDef.Policy.USER_SUBMITTED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response updateUser(UserUpdate user, Long userId) {
        UserVO curUser = findCurrentUserFromAuthContext();
        userService.updateUser(userId, user, curUser);
        return ResponseFactory.response(NO_CONTENT);
    }

    @Override
    public Response updateCurrentUser(UserUpdate user) {
        UserVO curUser = findCurrentUserFromAuthContext();
        userService.updateUser(curUser.getId(), user, curUser);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response deleteUser(long userId) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UserVO currentUser = null;
        if (principal.getUser() != null) {
            currentUser = principal.getUser();
        }

        if (currentUser != null && currentUser.getId().equals(userId)) {
            logger.debug("Rejected removing current user");
            ErrorResponse errorResponseEntity = new ErrorResponse(FORBIDDEN.getStatusCode(),
                    Messages.CANT_DELETE_CURRENT_USER_KEY);
            return ResponseFactory.response(FORBIDDEN, errorResponseEntity);
        }
        boolean isDeleted = userService.deleteUser(userId);
        if (!isDeleted) {
            logger.error(String.format(Messages.USER_NOT_FOUND, userId));
            return ResponseFactory.response(NOT_FOUND,
                    new ErrorResponse(NOT_FOUND.getStatusCode(), String.format(Messages.USER_NOT_FOUND, userId)));
        }
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getNetwork(long id, long networkId) {
        UserWithNetworkVO existingUser = userService.findUserWithNetworks(id);
        if (existingUser == null) {
            logger.error("Can't get network with id {}: user {} not found", networkId, id);
            ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.USER_NOT_FOUND, id));
            return ResponseFactory.response(NOT_FOUND, errorResponseEntity);
        }
        for (NetworkVO network : existingUser.getNetworks()) {
            if (network.getId() == networkId) {
                return ResponseFactory.response(OK, UserNetworkResponse.fromNetwork(network), JsonPolicyDef.Policy.NETWORKS_LISTED);
            }
        }
        ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                String.format(Messages.USER_NETWORK_NOT_FOUND, networkId, id));
        return ResponseFactory.response(NOT_FOUND, errorResponseEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response assignNetwork(long id, long networkId) {
        userService.assignNetwork(id, networkId);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response unassignNetwork(long id, long networkId) {
        userService.unassignNetwork(id, networkId);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getIexperiment(long id, long iexperimentId) {
        UserWithIexperimentVO existingUser = userService.findUserWithIexperiment(id);
        if (existingUser == null) {
            logger.error("Can't get iexperiment with id {}: user {} not found", iexperimentId, id);
            ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.USER_NOT_FOUND, id));
            return ResponseFactory.response(NOT_FOUND, errorResponseEntity);
        }

        if (existingUser.getAllIexperimentsAvailable()) {
            IexperimentVO iexperimentVO = iexperimentService.getWithDevices(iexperimentId);
            if (iexperimentVO != null) {
                return ResponseFactory.response(OK, UserIexperimentResponse.fromIexperiment(iexperimentVO), JsonPolicyDef.Policy.IEXPERIMENTS_LISTED);
            }
        }

        for (IexperimentVO iexperiment : existingUser.getIexperiments()) {
            if (iexperiment.getId() == iexperimentId) {
                return ResponseFactory.response(OK, UserIexperimentResponse.fromIexperiment(iexperiment), JsonPolicyDef.Policy.IEXPERIMENTS_LISTED);
            }
        }
        ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                String.format(Messages.USER_IEXPERIMENT_NOT_FOUND, iexperimentId, id));
        return ResponseFactory.response(NOT_FOUND, errorResponseEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getIexperiments(long id, @Suspended final AsyncResponse asyncResponse) {
        UserWithIexperimentVO existingUser = userService.findUserWithIexperiment(id);
        if (existingUser == null) {
            logger.error("Can't get iexperiments for user with id {}: user not found", id);
            ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.USER_NOT_FOUND, id));
            asyncResponse.resume(ResponseFactory.response(NOT_FOUND, errorResponseEntity));
        } else {
            if (existingUser.getAllIexperimentsAvailable()) {
                iexperimentService.listAll().thenApply(iexperimentVOS -> {
                    logger.debug("User list request proceed successfully");
                    return ResponseFactory.response(OK, iexperimentVOS, JsonPolicyDef.Policy.IEXPERIMENTS_LISTED);
                }).thenAccept(asyncResponse::resume);
            } else if (!existingUser.getAllIexperimentsAvailable() && (existingUser.getIexperiments() == null || existingUser.getIexperiments().isEmpty())) {
                logger.warn("Unable to get list for empty iexperiments");
                asyncResponse.resume(ResponseFactory.response(OK, Collections.<IexperimentVO>emptyList(), IEXPERIMENTS_LISTED));
            } else {
                asyncResponse.resume(ResponseFactory.response(OK, existingUser.getIexperiments(), JsonPolicyDef.Policy.IEXPERIMENTS_LISTED));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response assignIexperiment(long id, long iexperimentId) {
        userService.assignIexperiment(id, iexperimentId);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response unassignIexperiment(long id, long iexperimentId) {
        userService.unassignIexperiment(id, iexperimentId);
        return ResponseFactory.response(NO_CONTENT);
    }

    @Override
    public Response allowAllIexperiments(long id) {
        userService.allowAllIexperiments(id);
        return ResponseFactory.response(NO_CONTENT);
    }

    @Override
    public Response disallowAllIexperiments(long id) {
        userService.disallowAllIexperiments(id);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response getIcomponent(long id, long icomponentId) {
        UserWithIcomponentVO existingUser = userService.findUserWithIcomponent(id);
        if (existingUser == null) {
            logger.error("Can't get icomponent with id {}: user {} not found", icomponentId, id);
            ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.USER_NOT_FOUND, id));
            return ResponseFactory.response(NOT_FOUND, errorResponseEntity);
        }

        if (existingUser.getAllIcomponentsAvailable()) {
            IcomponentVO icomponentVO = icomponentService.getWithDevices(icomponentId);
            if (icomponentVO != null) {
                return ResponseFactory.response(OK, UserIcomponentResponse.fromIcomponent(icomponentVO), JsonPolicyDef.Policy.ICOMPONENTS_LISTED);
            }
        }

        for (IcomponentVO icomponent : existingUser.getIcomponents()) {
            if (icomponent.getId() == icomponentId) {
                return ResponseFactory.response(OK, UserIcomponentResponse.fromIcomponent(icomponent), JsonPolicyDef.Policy.ICOMPONENTS_LISTED);
            }
        }
        ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                String.format(Messages.USER_ICOMPONENT_NOT_FOUND, icomponentId, id));
        return ResponseFactory.response(NOT_FOUND, errorResponseEntity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getIcomponents(long id, @Suspended final AsyncResponse asyncResponse) {
        UserWithIcomponentVO existingUser = userService.findUserWithIcomponent(id);
        if (existingUser == null) {
            logger.error("Can't get icomponents for user with id {}: user not found", id);
            ErrorResponse errorResponseEntity = new ErrorResponse(NOT_FOUND.getStatusCode(),
                    String.format(Messages.USER_NOT_FOUND, id));
            asyncResponse.resume(ResponseFactory.response(NOT_FOUND, errorResponseEntity));
        } else {
            if (existingUser.getAllIcomponentsAvailable()) {
                icomponentService.listAll().thenApply(icomponentVOS -> {
                    logger.debug("User list request proceed successfully");
                    return ResponseFactory.response(OK, icomponentVOS, JsonPolicyDef.Policy.ICOMPONENTS_LISTED);
                }).thenAccept(asyncResponse::resume);
            } else if (!existingUser.getAllIcomponentsAvailable() && (existingUser.getIcomponents() == null || existingUser.getIcomponents().isEmpty())) {
                logger.warn("Unable to get list for empty icomponents");
                asyncResponse.resume(ResponseFactory.response(OK, Collections.<IcomponentVO>emptyList(), ICOMPONENTS_LISTED));
            } else {
                asyncResponse.resume(ResponseFactory.response(OK, existingUser.getIcomponents(), JsonPolicyDef.Policy.ICOMPONENTS_LISTED));
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response assignIcomponent(long id, long icomponentId) {
        userService.assignIcomponent(id, icomponentId);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response unassignIcomponent(long id, long icomponentId) {
        userService.unassignIcomponent(id, icomponentId);
        return ResponseFactory.response(NO_CONTENT);
    }

    @Override
    public Response allowAllIcomponents(long id) {
        userService.allowAllIcomponents(id);
        return ResponseFactory.response(NO_CONTENT);
    }

    @Override
    public Response disallowAllIcomponents(long id) {
        userService.disallowAllIcomponents(id);
        return ResponseFactory.response(NO_CONTENT);
    }

    /**
     * Finds current user from authentication context, handling token
     * authorisation schemes.
     *
     * @return user object or null
     */
    private UserVO findCurrentUserFromAuthContext() {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getUser();
    }

}
