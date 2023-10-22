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

import com.devicehive.auth.HiveAuthentication;
import com.devicehive.auth.HivePrincipal;
import com.devicehive.configuration.Messages;
import com.devicehive.dao.IcomponentDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.response.EntityCountResponse;
import com.devicehive.model.rpc.*;
import com.devicehive.model.updates.IcomponentUpdate;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Optional.*;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

@Component
public class IcomponentService extends BaseIcomponentService {

    private static final Logger logger = LoggerFactory.getLogger(IcomponentService.class);

    private final HiveValidator hiveValidator;

    @Autowired
    public IcomponentService(HiveValidator hiveValidator,
    IcomponentDao icomponentDao,
                             RpcClient rpcClient) {
        super(icomponentDao, rpcClient);
        this.hiveValidator = hiveValidator;
    }

    @Transactional
    public boolean delete(long id, boolean force) {
        logger.trace("About to execute named query \"Icomponent.deleteById\" for ");
        IcomponentWithUsersAndDevicesVO icomponent = getWithDevices(id);
        if (!force && icomponent != null && !icomponent.getDevices().isEmpty()) {
            logger.warn("Failed to delete non-empty icomponent with id {}", id);
            String deviceIds = icomponent.getDevices().stream().map(DeviceVO::getDeviceId).collect(Collectors.joining(", "));
            throw new HiveException(String.format(Messages.ICOMPONENT_DELETION_NOT_ALLOWED, deviceIds), SC_BAD_REQUEST);
        }
        int result = icomponentDao.deleteById(id);
        logger.debug("Deleted {} rows from Icomponent table", result);
        return result > 0;
    }

    @Transactional
    public IcomponentVO create(IcomponentVO newIcomponent) {
        hiveValidator.validate(newIcomponent);
        logger.debug("Creating icomponent {}", newIcomponent);
        if (newIcomponent.getId() != null) {
            logger.error("Can't create icomponent entity with id={} specified", newIcomponent.getId());
            throw new IllegalParametersException(Messages.ID_NOT_ALLOWED);
        }
        List<IcomponentVO> existing = icomponentDao.findByName(newIcomponent.getName());
        if (!existing.isEmpty()) {
            logger.error("Icomponent with name {} already exists", newIcomponent.getName());
            throw new ActionNotAllowedException(Messages.DUPLICATE_ICOMPONENT);
        }
        icomponentDao.persist(newIcomponent);
        logger.info("Entity {} created successfully", newIcomponent);
        return newIcomponent;
    }

    @Transactional
    public IcomponentVO update(@NotNull Long icomponentId, IcomponentUpdate icomponentUpdate) {
        IcomponentVO existing = icomponentDao.find(icomponentId);
        if (existing == null) {
            throw new NoSuchElementException(String.format(Messages.ICOMPONENT_NOT_FOUND, icomponentId));
        }
        if (icomponentUpdate.getName().isPresent()) {
            existing.setName(icomponentUpdate.getName().get());
        }
        if (icomponentUpdate.getDescription().isPresent()) {
            existing.setDescription(icomponentUpdate.getDescription().get());
        }
        hiveValidator.validate(existing);

        return icomponentDao.merge(existing);
    }

    public CompletableFuture<List<IcomponentVO>> listAll() {
        final ListIcomponentRequest request = new ListIcomponentRequest();

        return list(request);
    }

    public CompletableFuture<EntityCountResponse> count(String name, String namePattern, HivePrincipal principal) {
        Optional<HivePrincipal> principalOpt = ofNullable(principal);
        CountIcomponentRequest countIcomponentRequest = new CountIcomponentRequest(name, namePattern, principalOpt);

        return count(countIcomponentRequest);
    }

    public CompletableFuture<EntityCountResponse> count(CountIcomponentRequest countIcomponentRequest) {
        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(countIcomponentRequest)
                .build(), new ResponseConsumer(future));

        return future.thenApply(response -> new EntityCountResponse((CountResponse)response.getBody()));
    }

    @Transactional
    public IcomponentVO verifyIcomponent(Optional<IcomponentVO> icomponentNullable) {
        //case icomponent is not defined
        if (icomponentNullable == null || icomponentNullable.orElse(null) == null) {
            return null;
        }
        IcomponentVO icomponent = icomponentNullable.get();

        Optional<IcomponentVO> storedOpt = findIcomponentByIdOrName(icomponent);
        if (storedOpt.isPresent()) {
            return storedOpt.get();
        }

        throw new NoSuchElementException(String.format(Messages.ICOMPONENT_NOT_FOUND, icomponent.getId()));
    }

    @Transactional
    public IcomponentVO createOrUpdateIcomponentByUser(Optional<IcomponentVO> icomponentNullable, UserVO user) {
        //case icomponent is not defined
        if (icomponentNullable == null || icomponentNullable.orElse(null) == null) {
            return null;
        }

        IcomponentVO icomponent = icomponentNullable.orElse(null);

        Optional<IcomponentVO> storedOpt = findIcomponentByIdOrName(icomponent);
        if (storedOpt.isPresent()) {
            return storedOpt.get();
        } else {
            if (icomponent.getId() != null) {
                throw new IllegalParametersException(Messages.INVALID_REQUEST_PARAMETERS);
            }
            if (user.isAdmin()) {
                IcomponentWithUsersAndDevicesVO newIcomponent = new IcomponentWithUsersAndDevicesVO(icomponent);
                icomponentDao.persist(newIcomponent);
                icomponent.setId(newIcomponent.getId());
            } else {
                throw new ActionNotAllowedException(Messages.ICOMPONENT_CREATION_NOT_ALLOWED);
            }
            return icomponent;
        }
    }

    @Transactional
    public Long findDefaultIcomponent(Set<Long> icomponentIds) {
        return icomponentDao.findDefault(icomponentIds)
                .map(IcomponentVO::getId)
                .orElseThrow(() -> new ActionNotAllowedException(Messages.NO_ACCESS_TO_ICOMPONENT));
    }

    @Transactional
    public IcomponentVO createOrUpdateIcomponentByUser(UserVO user) {
        IcomponentVO icomponentVO = new IcomponentVO();
        icomponentVO.setName(user.getLogin());
        icomponentVO.setDescription(String.format("User %s default icomponent", user.getLogin()));
        return createOrUpdateIcomponentByUser(Optional.ofNullable(icomponentVO), user);
    }

    public boolean isIcomponentExists(Long icomponentId) {
        return ofNullable(icomponentId)
                .map(id -> icomponentDao.find(id) != null)
                .orElse(false);
    }

    private Optional<IcomponentVO> findIcomponentByIdOrName(IcomponentVO icomponent) {
        return ofNullable(icomponent.getId())
                .map(id -> ofNullable(icomponentDao.find(id)))
                .orElseGet(() -> icomponentDao.findFirstByName(icomponent.getName()));
    }
}
