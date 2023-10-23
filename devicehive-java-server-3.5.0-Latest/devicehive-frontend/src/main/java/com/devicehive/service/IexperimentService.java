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
import com.devicehive.dao.IexperimentDao;
import com.devicehive.exceptions.ActionNotAllowedException;
import com.devicehive.exceptions.HiveException;
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.response.EntityCountResponse;
import com.devicehive.model.rpc.*;
import com.devicehive.model.updates.IexperimentUpdate;
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
public class IexperimentService extends BaseIexperimentService {

    private static final Logger logger = LoggerFactory.getLogger(IexperimentService.class);

    private final HiveValidator hiveValidator;

    @Autowired
    public IexperimentService(HiveValidator hiveValidator,
                             IexperimentDao iexperimentDao,
                             RpcClient rpcClient) {
        super(iexperimentDao, rpcClient);
        this.hiveValidator = hiveValidator;
    }

    @Transactional
    public boolean delete(long id, boolean force) {
        logger.trace("About to execute named query \"Iexperiment.deleteById\" for ");
        IexperimentWithUsersAndDevicesVO iexperiment = getWithDevices(id);
        if (!force && iexperiment != null && !iexperiment.getDevices().isEmpty()) {
            logger.warn("Failed to delete non-empty iexperiment with id {}", id);
            String deviceIds = iexperiment.getDevices().stream().map(DeviceVO::getDeviceId).collect(Collectors.joining(", "));
            throw new HiveException(String.format(Messages.IEXPERIMENT_DELETION_NOT_ALLOWED, deviceIds), SC_BAD_REQUEST);
        }
        int result = iexperimentDao.deleteById(id);
        logger.debug("Deleted {} rows from Iexperiment table", result);
        return result > 0;
    }

    @Transactional
    public IexperimentVO create(IexperimentVO newIexperiment) {
        hiveValidator.validate(newIexperiment);
        logger.debug("Creating iexperiment {}", newIexperiment);
        if (newIexperiment.getId() != null) {
            logger.error("Can't create iexperiment entity with id={} specified", newIexperiment.getId());
            throw new IllegalParametersException(Messages.ID_NOT_ALLOWED);
        }
        List<IexperimentVO> existing = iexperimentDao.findByName(newIexperiment.getName());
        if (!existing.isEmpty()) {
            logger.error("Iexperiment with name {} already exists", newIexperiment.getName());
            throw new ActionNotAllowedException(Messages.DUPLICATE_IEXPERIMENT);
        }
        iexperimentDao.persist(newIexperiment);
        logger.info("Entity {} created successfully", newIexperiment);
        return newIexperiment;
    }

    @Transactional
    public IexperimentVO update(@NotNull Long iexperimentId, IexperimentUpdate iexperimentUpdate) {
        IexperimentVO existing = iexperimentDao.find(iexperimentId);
        if (existing == null) {
            throw new NoSuchElementException(String.format(Messages.IEXPERIMENT_NOT_FOUND, iexperimentId));
        }
        if (iexperimentUpdate.getName().isPresent()) {
            existing.setName(iexperimentUpdate.getName().get());
        }
        if (iexperimentUpdate.getDescription().isPresent()) {
            existing.setDescription(iexperimentUpdate.getDescription().get());
        }
        hiveValidator.validate(existing);

        return iexperimentDao.merge(existing);
    }

    public CompletableFuture<List<IexperimentVO>> listAll() {
        final ListIexperimentRequest request = new ListIexperimentRequest();

        return list(request);
    }

    public CompletableFuture<EntityCountResponse> count(String name, String namePattern, HivePrincipal principal) {
        Optional<HivePrincipal> principalOpt = ofNullable(principal);
        CountIexperimentRequest countIexperimentRequest = new CountIexperimentRequest(name, namePattern, principalOpt);

        return count(countIexperimentRequest);
    }

    public CompletableFuture<EntityCountResponse> count(CountIexperimentRequest countIexperimentRequest) {
        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request
                .newBuilder()
                .withBody(countIexperimentRequest)
                .build(), new ResponseConsumer(future));

        return future.thenApply(response -> new EntityCountResponse((CountResponse)response.getBody()));
    }

    @Transactional
    public IexperimentVO verifyIexperiment(Optional<IexperimentVO> iexperimentNullable) {
        //case iexperiment is not defined
        if (iexperimentNullable == null || iexperimentNullable.orElse(null) == null) {
            return null;
        }
        IexperimentVO iexperiment = iexperimentNullable.get();

        Optional<IexperimentVO> storedOpt = findIexperimentByIdOrName(iexperiment);
        if (storedOpt.isPresent()) {
            return storedOpt.get();
        }

        throw new NoSuchElementException(String.format(Messages.IEXPERIMENT_NOT_FOUND, iexperiment.getId()));
    }

    @Transactional
    public IexperimentVO createOrUpdateIexperimentByUser(Optional<IexperimentVO> iexperimentNullable, UserVO user) {
        //case iexperiment is not defined
        if (iexperimentNullable == null || iexperimentNullable.orElse(null) == null) {
            return null;
        }

        IexperimentVO iexperiment = iexperimentNullable.orElse(null);

        Optional<IexperimentVO> storedOpt = findIexperimentByIdOrName(iexperiment);
        if (storedOpt.isPresent()) {
            return storedOpt.get();
        } else {
            if (iexperiment.getId() != null) {
                throw new IllegalParametersException(Messages.INVALID_REQUEST_PARAMETERS);
            }
            if (user.isAdmin()) {
                IexperimentWithUsersAndDevicesVO newIexperiment = new IexperimentWithUsersAndDevicesVO(iexperiment);
                iexperimentDao.persist(newIexperiment);
                iexperiment.setId(newIexperiment.getId());
            } else {
                throw new ActionNotAllowedException(Messages.IEXPERIMENT_CREATION_NOT_ALLOWED);
            }
            return iexperiment;
        }
    }

    @Transactional
    public Long findDefaultIexperiment(Set<Long> iexperimentIds) {
        return iexperimentDao.findDefault(iexperimentIds)
                .map(IexperimentVO::getId)
                .orElseThrow(() -> new ActionNotAllowedException(Messages.NO_ACCESS_TO_IEXPERIMENT));
    }

    @Transactional
    public IexperimentVO createOrUpdateIexperimentByUser(UserVO user) {
        IexperimentVO iexperimentVO = new IexperimentVO();
        iexperimentVO.setName(user.getLogin());
        iexperimentVO.setDescription(String.format("User %s default iexperiment", user.getLogin()));
        return createOrUpdateIexperimentByUser(Optional.ofNullable(iexperimentVO), user);
    }

    public boolean isIexperimentExists(Long iexperimentId) {
        return ofNullable(iexperimentId)
                .map(id -> iexperimentDao.find(id) != null)
                .orElse(false);
    }

    private Optional<IexperimentVO> findIexperimentByIdOrName(IexperimentVO iexperiment) {
        return ofNullable(iexperiment.getId())
                .map(id -> ofNullable(iexperimentDao.find(id)))
                .orElseGet(() -> iexperimentDao.findFirstByName(iexperiment.getName()));
    }
}
