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
import com.devicehive.exceptions.IllegalParametersException;
import com.devicehive.model.response.EntityCountResponse;
import com.devicehive.model.rpc.CountIcomponentRequest;
import com.devicehive.model.rpc.CountResponse;
import com.devicehive.model.rpc.ListIcomponentRequest;
import com.devicehive.model.rpc.ListIcomponentResponse;
import com.devicehive.model.updates.IcomponentUpdate;
import com.devicehive.service.helpers.ResponseConsumer;
import com.devicehive.shim.api.Request;
import com.devicehive.shim.api.Response;
import com.devicehive.shim.api.client.RpcClient;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.IcomponentVO;
import com.devicehive.vo.IcomponentWithUsersAndDevicesVO;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.UserVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Component
public class BaseIcomponentService {
    private static final Logger logger = LoggerFactory.getLogger(BaseIcomponentService.class);

    protected final IcomponentDao icomponentDao;
    protected final RpcClient rpcClient;

    @Autowired
    public BaseIcomponentService(IcomponentDao icomponentDao,
                                 RpcClient rpcClient) {
        this.icomponentDao = icomponentDao;
        this.rpcClient = rpcClient;

    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public IcomponentWithUsersAndDevicesVO getWithDevices(@NotNull Long icomponentId) {
        HivePrincipal principal = (HivePrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Set<Long> permittedIcomponents = principal.getIcomponentIds();
        Set<Long> permittedNetworks = principal.getNetworkIds();

        Optional<IcomponentWithUsersAndDevicesVO> result = of(principal)
                .flatMap(pr -> {
                    if (pr.getUser() != null) {
                        return of(pr.getUser());
                    } else {
                        return empty();
                    }
                }).flatMap(user -> {
                    Long idForFiltering = user.isAdmin() ? null : user.getId();
                    if (user.getAllIcomponentsAvailable()) {
                        idForFiltering = null;
                    }
                    List<IcomponentWithUsersAndDevicesVO> found = icomponentDao.getIcomponentsByIdsAndUsers(idForFiltering,
                            Collections.singleton(icomponentId), permittedIcomponents);
                    return found.stream().findFirst();
                }).map(icomponent -> {
                    if (permittedNetworks != null && !permittedNetworks.isEmpty()) {
                        Set<DeviceVO> allowed = icomponent.getDevices().stream()
                                .filter(device -> permittedNetworks.contains(device.getNetworkId()))
                                .collect(Collectors.toSet());
                                icomponent.setDevices(allowed);
                    }
                    return icomponent;
                });

        return result.orElse(null);
    }

    public CompletableFuture<List<IcomponentVO>> list(String name,
                                                      String namePattern,
                                                      String sortField,
                                                      String sortOrder,
                                                      Integer take,
                                                      Integer skip,
                                                      HivePrincipal principal) {
        Optional<HivePrincipal> principalOpt = ofNullable(principal);

        ListIcomponentRequest request = new ListIcomponentRequest();
        request.setName(name);
        request.setNamePattern(namePattern);
        request.setSortField(sortField);
        request.setSortOrder(sortOrder);
        request.setTake(take);
        request.setSkip(skip);
        request.setPrincipal(principalOpt);

        return list(request);
    }

    public CompletableFuture<List<IcomponentVO>> list(ListIcomponentRequest request) {
        CompletableFuture<Response> future = new CompletableFuture<>();

        rpcClient.call(Request.newBuilder().withBody(request).build(), new ResponseConsumer(future));

        return future.thenApply(r -> ((ListIcomponentResponse) r.getBody()).getIcomponents());
    }
}
