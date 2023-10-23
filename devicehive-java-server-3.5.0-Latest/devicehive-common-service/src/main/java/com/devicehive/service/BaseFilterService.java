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
import com.devicehive.exceptions.HiveException;
import com.devicehive.model.enums.UserRole;
import com.devicehive.model.eventbus.Filter;
import com.devicehive.model.rpc.ListIexperimentRequest;
import com.devicehive.model.rpc.ListIcomponentRequest;
import com.devicehive.model.rpc.ListNetworkRequest;
import com.devicehive.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.devicehive.configuration.Messages.ACCESS_DENIED;
import static com.devicehive.configuration.Messages.IEXPERIMENTS_NOT_FOUND;
import static com.devicehive.configuration.Messages.ICOMPONENTS_NOT_FOUND;
import static com.devicehive.configuration.Messages.NETWORKS_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

@Component
public class BaseFilterService {
    private static final Logger logger = LoggerFactory.getLogger(BaseFilterService.class);

    private final BaseDeviceService deviceService;
    private final BaseNetworkService networkService;
    private final BaseIexperimentService iexperimentService;
    private final BaseIcomponentService icomponentService;

    @Autowired
    public BaseFilterService(BaseDeviceService deviceService,
                             BaseNetworkService networkService,
                             BaseIexperimentService iexperimentService,
                             BaseIcomponentService icomponentService) {
        this.deviceService = deviceService;
        this.networkService = networkService;
        this.iexperimentService = iexperimentService;
        this.icomponentService = icomponentService;
    }

    public Set<Filter> getFilterList(String deviceId,
                                     Set<Long> networks,
                                     Set<Long> iexperiments,
                                     Set<Long> icomponents,
                                     String eventName,
                                     Set<String> names,
                                     HiveAuthentication authentication) {
        final HivePrincipal principal = (HivePrincipal) authentication.getPrincipal();

        if (networks != null && !networks.isEmpty()) {
            Set<NetworkWithUsersAndDevicesVO> actualNetworks = networks.stream().map(networkService::getWithDevices
            ).filter(Objects::nonNull).collect(Collectors.toSet());
            if (actualNetworks.size() != networks.size()) {
                if (UserRole.CLIENT.equals(principal.getUser().getRole())) {
                    throw new HiveException(ACCESS_DENIED, SC_FORBIDDEN);
                }
                throw new HiveException(String.format(NETWORKS_NOT_FOUND, networks), SC_NOT_FOUND);
            }
        } else {
            networks = principal.getNetworkIds();
        }
        if (iexperiments != null && !iexperiments.isEmpty()) {
            Set<IexperimentWithUsersAndDevicesVO> actualIexperiments = iexperiments.stream()
                    .map(iexperimentService::getWithDevices).filter(Objects::nonNull).collect(Collectors.toSet());
            if (actualIexperiments.size() != iexperiments.size()) {
                if (UserRole.CLIENT.equals(principal.getUser().getRole())) {
                    throw new HiveException(ACCESS_DENIED, SC_FORBIDDEN);
                }
                throw new HiveException(String.format(IEXPERIMENTS_NOT_FOUND, iexperiments), SC_NOT_FOUND);
            }
        } else {
            iexperiments = principal.getIexperimentIds();
        }

        if (icomponents != null && !icomponents.isEmpty()) {
            Set<IcomponentWithUsersAndDevicesVO> actualIcomponents = icomponents.stream()
                    .map(icomponentService::getWithDevices).filter(Objects::nonNull).collect(Collectors.toSet());
            if (actualIcomponents.size() != icomponents.size()) {
                if (UserRole.CLIENT.equals(principal.getUser().getRole())) {
                    throw new HiveException(ACCESS_DENIED, SC_FORBIDDEN);
                }
                throw new HiveException(String.format(ICOMPONENTS_NOT_FOUND, icomponents), SC_NOT_FOUND);
            }
        } else {
            icomponents = principal.getIcomponentIds();
        }

        if ((networks != null && !networks.isEmpty() || principal.areAllNetworksAvailable())
                && (iexperiments != null && !iexperiments.isEmpty() || principal.areAllIexperimentsAvailable())
                && (icomponents != null && !icomponents.isEmpty() || principal.areAllIcomponentsAvailable())) {
            Set<Filter> filters;
            if (deviceId != null) {
                DeviceVO device = deviceService.findByIdWithPermissionsCheckIfExists(deviceId, principal);
                if (names != null) {
                    filters = names.stream().map(name ->
                            new Filter(device.getNetworkId(), device.getIexperimentId(), device.getIcomponentId(), deviceId, eventName, name))
                            .collect(Collectors.toSet());
                } else {
                    filters = Collections.singleton(new Filter(device.getNetworkId(), device.getIexperimentId(), device.getIcomponentId(), deviceId, eventName, null));
                }
            } else {
                if (networks == null && iexperiments == null && icomponents == null) {
                    if (names != null) {
                        filters = names.stream().map(name ->
                                new Filter(null, null, null, null, eventName, name))
                                .collect(Collectors.toSet());
                    } else {
                        filters = Collections.singleton(new Filter(null, null, null, null, eventName, null));
                    }
                } else {
                    if (networks == null) {
                        ListNetworkRequest listNetworkRequest = new ListNetworkRequest();
                        listNetworkRequest.setPrincipal(Optional.of(principal));
                        networks = networkService.list(listNetworkRequest).join()
                                .stream().map(NetworkVO::getId).collect(Collectors.toSet());
                    }
                    if (iexperiments == null) {
                        ListIexperimentRequest listIexperimentRequest = new ListIexperimentRequest();
                        listIexperimentRequest.setPrincipal(Optional.of(principal));
                        iexperiments = iexperimentService.list(listIexperimentRequest).join()
                                .stream().map(IexperimentVO::getId).collect(Collectors.toSet());
                    }
                    if (icomponents == null) {
                        ListIcomponentRequest listIcomponentRequest = new ListIcomponentRequest();
                        listIcomponentRequest.setPrincipal(Optional.of(principal));
                        icomponents = icomponentService.list(listIcomponentRequest).join()
                                .stream().map(IcomponentVO::getId).collect(Collectors.toSet());
                    }
                    final Set<Long> finalIexperiments = iexperiments;
                    final Set<Long> finalIcomponents = icomponents;
                    filters = networks.stream()
                            .flatMap(network -> finalIexperiments.stream()
                            .flatMap(iexperiment -> finalIcomponents.stream()
                            .flatMap(icomponent ->{
                                if (names != null && !names.isEmpty()) {
                                    return names.stream().map(name ->
                                            new Filter(network, iexperiment, icomponent, null, eventName, name)
                                    );
                                } else {
                                    return Stream.of(new Filter(network, iexperiment, icomponent, null, eventName, null));
                                }
                            })))
                            .collect(Collectors.toSet());
                }
            }

            return filters;
        } else {
            logger.warn("Filters set is empty for userId {}", principal.getUser().getId());
            return Collections.emptySet();
        }
    }
}
