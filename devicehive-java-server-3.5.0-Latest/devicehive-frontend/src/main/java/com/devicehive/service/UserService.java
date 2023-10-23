package com.devicehive.service;

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

import com.devicehive.configuration.Constants;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.IexperimentDao;
import com.devicehive.dao.IcomponentDao;
import com.devicehive.dao.NetworkDao;
import com.devicehive.dao.UserDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.enums.UserStatus;
import com.devicehive.model.response.EntityCountResponse;
import com.devicehive.model.rpc.*;
import com.devicehive.model.updates.UserUpdate;
import com.devicehive.service.configuration.ConfigurationService;
import com.devicehive.service.helpers.PasswordProcessor;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.service.time.TimestampService;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * This class serves all requests to database from controller.
 */
@Component
public class UserService extends BaseUserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final String PASSWORD_REGEXP = "^.{6,128}$";

    private final IexperimentDao iexperimentDao;
    private final IcomponentDao icomponentDao;
    private final RpcClient rpcClient;

    private BaseNetworkService networkService;

    @Autowired
    public UserService(PasswordProcessor passwordService,
                       NetworkDao networkDao,
                       IexperimentDao iexperimentDao,
                       IcomponentDao icomponentDao,
                       UserDao userDao,
                       TimestampService timestampService,
                       ConfigurationService configurationService,
                       HiveValidator hiveValidator,
                       RpcClient rpcClient) {
        super(passwordService, userDao, networkDao, timestampService, configurationService, hiveValidator);
        this.iexperimentDao = iexperimentDao;
        this.icomponentDao = icomponentDao;
        this.rpcClient = rpcClient;
    }

    @Autowired
    public void setNetworkService(BaseNetworkService networkService) {
        this.networkService = networkService;
    }

    /**
     * Tries to authenticate with given credentials
     *
     * @return User object if authentication is successful or null if not
     */
    @Transactional(noRollbackFor = ActionNotAllowedException.class)
    public UserVO authenticate(String login, String password) {
        Optional<UserVO> userOpt = userDao.findByName(login);
        if (!userOpt.isPresent()) {
            return null;
        }
        return checkPassword(userOpt.get(), password)
                .orElseThrow(() -> new ActionNotAllowedException(String.format(Messages.INCORRECT_CREDENTIALS, login)));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserVO updateUser(@NotNull Long id, UserUpdate userToUpdate, UserVO curUser) {
        UserVO existing = userDao.find(id);

        if (existing == null) {
            logger.error("Can't update user with id {}: user not found", id);
            throw new NoSuchElementException(String.format(Messages.USER_NOT_FOUND, id));
        }

        if (userToUpdate == null) {
            return existing;
        }

        final boolean isClient = UserRole.CLIENT.equals(curUser.getRole());
        if (isClient) {
            if (userToUpdate.getLogin().isPresent() ||
                    userToUpdate.getStatus().isPresent() ||
                    userToUpdate.getRole().isPresent()) {
                logger.error("Can't update user with id {}: users with the 'client' role not allowed to change their " +
                        "login, status or role", id);
                throw new HiveException(Messages.ADMIN_PERMISSIONS_REQUIRED, FORBIDDEN.getStatusCode());
            }
        }

        if (userToUpdate.getLogin().isPresent()) {
            final String newLogin = StringUtils.trim(userToUpdate.getLogin().orElse(null));
            Optional<UserVO> withSuchLogin = userDao.findByName(newLogin);

            if (withSuchLogin.isPresent() && !withSuchLogin.get().getId().equals(id)) {
                throw new ActionNotAllowedException(Messages.DUPLICATE_LOGIN);
            }
            existing.setLogin(newLogin);
        }

        final Optional<String> newPassword = userToUpdate.getPassword();
        if (newPassword.isPresent() && StringUtils.isNotEmpty(newPassword.get())) {
            final String password = newPassword.get();
            if (StringUtils.isEmpty(password) || !password.matches(PASSWORD_REGEXP)) {
                logger.error("Can't update user with id {}: password required", id);
                throw new IllegalParametersException(Messages.PASSWORD_VALIDATION_FAILED);
            }
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(password, salt);
            existing.setPasswordSalt(salt);
            existing.setPasswordHash(hash);
        }

        if (userToUpdate.getRoleEnum() != null) {
            existing.setRole(userToUpdate.getRoleEnum());
        }

        if (userToUpdate.getStatusEnum() != null) {
            existing.setStatus(userToUpdate.getStatusEnum());
        }

        existing.setData(userToUpdate.getData().orElse(null));
        
        if (userToUpdate.getIntroReviewed().isPresent()) {
            existing.setIntroReviewed(userToUpdate.getIntroReviewed().get());
        }

        hiveValidator.validate(existing);
        return userDao.merge(existing);
    }

    /**
     * Allows user access to given iexperiment
     *
     * @param userId id of user
     * @param iexperimentId id of iexperiment
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void assignIexperiment(@NotNull long userId, @NotNull long iexperimentId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't assign iexperiment with id {}: user {} not found", iexperimentId, userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        if (existingUser.getAllIexperimentsAvailable()) {
            throw new HiveException(String.format(Messages.IEXPERIMENT_ASSIGNMENT_NOT_ALLOWED, userId), FORBIDDEN.getStatusCode());
        }
        IexperimentWithUsersAndDevicesVO existingIexperiment = iexperimentDao.findWithUsers(iexperimentId).orElse(null);
        if (Objects.isNull(existingIexperiment)) {
            throw new HiveException(String.format(Messages.IEXPERIMENT_NOT_FOUND, iexperimentId), NOT_FOUND.getStatusCode());
        }

        iexperimentDao.assignToIexperiment(existingIexperiment, existingUser);
    }

    /**
     * Revokes user access to given iexperiment
     *
     * @param userId id of user
     * @param iexperimentId id of iexperiment
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void unassignIexperiment(@NotNull long userId, @NotNull long iexperimentId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't unassign iexperiment with id {}: user {} not found", iexperimentId, userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        if (existingUser.getAllIexperimentsAvailable()) {
            throw new HiveException(String.format(Messages.IEXPERIMENT_ASSIGNMENT_NOT_ALLOWED, userId), FORBIDDEN.getStatusCode());
        }
        IexperimentVO existingIexperiment = iexperimentDao.find(iexperimentId);
        if (existingIexperiment == null) {
            logger.error("Can't unassign user with id {}: iexperiment {} not found", userId, iexperimentId);
            throw new HiveException(String.format(Messages.IEXPERIMENT_NOT_FOUND, iexperimentId), NOT_FOUND.getStatusCode());
        }
        userDao.unassignIexperiment(existingUser, iexperimentId);
    }

    @Transactional
    public UserVO allowAllIexperiments(@NotNull long userId) {
        UserWithIexperimentVO existingUser = userDao.getWithIexperimentById(userId);
        if (existingUser == null) {
            logger.error("Can't allow all iexperiments: user {} not found", userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        return userDao.allowAllIexperiments(existingUser);
    }

    @Transactional
    public UserVO disallowAllIexperiments(@NotNull long userId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't disallow all iexperiments: user {} not found", userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        return userDao.disallowAllIexperiments(existingUser);
    }

    /**
     * Allows user access to given icomponent
     *
     * @param userId id of user
     * @param icomponentId id of icomponent
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void assignIcomponent(@NotNull long userId, @NotNull long icomponentId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't assign icomponent with id {}: user {} not found", icomponentId, userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        if (existingUser.getAllIcomponentsAvailable()) {
            throw new HiveException(String.format(Messages.ICOMPONENT_ASSIGNMENT_NOT_ALLOWED, userId), FORBIDDEN.getStatusCode());
        }
        IcomponentWithUsersAndDevicesVO existingIcomponent = icomponentDao.findWithUsers(icomponentId).orElse(null);
        if (Objects.isNull(existingIcomponent)) {
            throw new HiveException(String.format(Messages.ICOMPONENT_NOT_FOUND, icomponentId), NOT_FOUND.getStatusCode());
        }

        icomponentDao.assignToIcomponent(existingIcomponent, existingUser);
    }

    /**
     * Revokes user access to given icomponent
     *
     * @param userId id of user
     * @param icomponentId id of icomponent
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void unassignIcomponent(@NotNull long userId, @NotNull long icomponentId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't unassign icomponent with id {}: user {} not found", icomponentId, userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        if (existingUser.getAllIcomponentsAvailable()) {
            throw new HiveException(String.format(Messages.ICOMPONENT_ASSIGNMENT_NOT_ALLOWED, userId), FORBIDDEN.getStatusCode());
        }
        IcomponentVO existingIcomponent = icomponentDao.find(icomponentId);
        if (existingIcomponent == null) {
            logger.error("Can't unassign user with id {}: icomponent {} not found", userId, icomponentId);
            throw new HiveException(String.format(Messages.ICOMPONENT_NOT_FOUND, icomponentId), NOT_FOUND.getStatusCode());
        }
        userDao.unassignIcomponent(existingUser, icomponentId);
    }

    @Transactional
    public UserVO allowAllIcomponents(@NotNull long userId) {
        UserWithIcomponentVO existingUser = userDao.getWithIcomponentById(userId);
        if (existingUser == null) {
            logger.error("Can't allow all icomponents: user {} not found", userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        return userDao.allowAllIcomponents(existingUser);
    }

    @Transactional
    public UserVO disallowAllIcomponents(@NotNull long userId) {
        UserVO existingUser = userDao.find(userId);
        if (existingUser == null) {
            logger.error("Can't disallow all icomponents: user {} not found", userId);
            throw new HiveException(String.format(Messages.USER_NOT_FOUND, userId), NOT_FOUND.getStatusCode());
        }
        return userDao.disallowAllIcomponents(existingUser);
    }

    public CompletableFuture<List<UserVO>> list(ListUserRequest request) {
        return list(request.getLogin(), request.getLoginPattern(), request.getRole(), request.getStatus(), request.getSortField(),
                request.getSortOrder(), request.getTake(), request.getSkip());
    }

    public CompletableFuture<List<UserVO>> list(String login, String loginPattern, Integer role, Integer status, String sortField,
            String sortOrder, Integer take, Integer skip) {
        ListUserRequest request = new ListUserRequest();
        request.setLogin(login);
        request.setLoginPattern(loginPattern);
        request.setRole(role);
        request.setStatus(status);
        request.setSortField(sortField);
        request.setSortOrder(sortOrder);
        request.setTake(take);
        request.setSkip(skip);

        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(request)
                .build(), new ResponseConsumer(future));

        return future.thenApply(r -> ((ListUserResponse) r.getBody()).getUsers());
    }

    public CompletableFuture<EntityCountResponse> count(String login, String loginPattern, Integer role, Integer status) {
        CountUserRequest countUserRequest = new CountUserRequest(login, loginPattern, role, status);

        return count(countUserRequest);
    }

    public CompletableFuture<EntityCountResponse> count(CountUserRequest countUserRequest) {
        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(countUserRequest)
                .build(), new ResponseConsumer(future));

        return future.thenApply(response -> new EntityCountResponse((CountResponse)response.getBody()));
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserVO createUser(@NotNull UserVO user, String password) {
        hiveValidator.validate(user);
        if (user.getId() != null) {
            throw new IllegalParametersException(Messages.ID_NOT_ALLOWED);
        }
        if (user.getRole() == null ) {
            throw new IllegalParametersException(Messages.INVALID_USER_ROLE);
        }
        if (user.getStatus() == null) {
            user.setStatus(UserStatus.ACTIVE);
        }
        final String userLogin = StringUtils.trim(user.getLogin());
        user.setLogin(userLogin);
        Optional<UserVO> existing = userDao.findByName(user.getLogin());
        if (existing.isPresent()) {
            throw new ActionNotAllowedException(Messages.DUPLICATE_LOGIN);
        }
        if (StringUtils.isNotEmpty(password) && password.matches(PASSWORD_REGEXP)) {
            String salt = passwordService.generateSalt();
            String hash = passwordService.hashPassword(password, salt);
            user.setPasswordSalt(salt);
            user.setPasswordHash(hash);
        } else {
            throw new IllegalParametersException(Messages.PASSWORD_VALIDATION_FAILED);
        }
        user.setLoginAttempts(Constants.INITIAL_LOGIN_ATTEMPTS);
        if (user.getIntroReviewed() == null) {
            user.setIntroReviewed(false);
        }

        if (user.getAllIexperimentsAvailable() == null) {
            user.setAllIexperimentsAvailable(true);
        }
        if (user.getAllIcomponentsAvailable() == null) {
            user.setAllIcomponentsAvailable(true);
        }
        userDao.persist(user);
        return user;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserWithNetworkVO createUserWithNetwork(UserVO convertTo, String password) {
        hiveValidator.validate(convertTo);
        UserVO createdUser = createUser(convertTo, password);
        NetworkVO createdNetwork = networkService.createOrUpdateNetworkByUser(createdUser);
        UserWithNetworkVO result = UserWithNetworkVO.fromUserVO(createdUser);
        result.getNetworks().add(createdNetwork);
        return result;
    }

    /**
     * Deletes user by id. deletion is cascade
     *
     * @param id user id
     * @return true in case of success, false otherwise
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public boolean deleteUser(long id) {
        int result = userDao.deleteById(id);
        return result > 0;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToDevice(UserVO user, String deviceId) {
        if (!user.isAdmin()) {
            long count = userDao.hasAccessToDevice(user, deviceId);
            return count > 0;
        }
        return true;
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public boolean hasAccessToNetwork(UserVO user, NetworkVO network) {
        if (!user.isAdmin()) {
            long count = userDao.hasAccessToNetwork(user, network);
            return count > 0;
        }
        return true;
    }

}
