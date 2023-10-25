package com.devicehive.service.security.jwt;

/*
 * #%L
 * DeviceHive Frontend Logic
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

import com.devicehive.configuration.Messages;
import com.devicehive.exceptions.HiveException;
import com.devicehive.security.jwt.JwtUserPayload;
import com.devicehive.service.BaseUserService;
import com.devicehive.util.HiveValidator;
import com.devicehive.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;
import java.util.HashSet;
import java.util.Set;

import static com.devicehive.auth.HiveAction.ANY;
import static com.devicehive.auth.HiveAction.getClientHiveActions;
import static com.devicehive.auth.HiveAction.getIdSet;

@Component
public class JwtTokenService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    private final BaseUserService userService;
    private final JwtClientService tokenService;
    private final HiveValidator hiveValidator;

    @Autowired
    public JwtTokenService(BaseUserService userService, JwtClientService tokenService, HiveValidator hiveValidator) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.hiveValidator = hiveValidator;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public JwtTokenVO createJwtToken(@NotNull final JwtRequestVO request) {
        hiveValidator.validate(request);
        if (StringUtils.isBlank(request.getLogin()) || StringUtils.isEmpty(request.getPassword())) {
            logger.error(Messages.INVALID_AUTH_REQUEST_PARAMETERS);
            throw new HiveException(Messages.INVALID_AUTH_REQUEST_PARAMETERS, Response.Status.BAD_REQUEST.getStatusCode());
        }
        final UserVO user = userService.getActiveUser(request.getLogin(), request.getPassword());
        return createJwtToken(user);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public JwtTokenVO createJwtToken(@NotNull UserVO user) {
        Set<String> networkIds = new HashSet<>();
        Set<String> iexperimentIds = new HashSet<>();
        Set<String> icomponentIds = new HashSet<>();
        Set<Integer> actions = new HashSet<>();
        if (user.isAdmin()) {
            networkIds.add("*");
            iexperimentIds.add("*");
            icomponentIds.add("*");
            actions.add(ANY.getId());
        } else {
            UserWithNetworkVO userWithNetwork = userService.findUserWithNetworks(user.getId());
            UserWithIexperimentVO userWithIexperiment = userService.findUserWithIexperiment(user.getId());
            UserWithIcomponentVO userWithIcomponent = userService.findUserWithIcomponent(user.getId());
//          TODO: check if needed
            userService.refreshUserLoginData(user);

            Set<NetworkVO> networks = userWithNetwork.getNetworks();
            Set<IexperimentVO> iexperiments = userWithIexperiment.getIexperiments();
            Set<IcomponentVO> icomponents = userWithIcomponent.getIcomponents();
            if (!networks.isEmpty()) {
                networks.forEach(network -> {
                    networkIds.add(network.getId().toString());
                });
            }
            if (userWithIexperiment.getAllIexperimentsAvailable()) {
                iexperimentIds.add("*");
            } 
            if (!iexperiments.isEmpty()) { 
                iexperiments.forEach(iexperiment -> {
                    iexperimentIds.add(iexperiment.getId().toString());
                    });
            }
            if (userWithIcomponent.getAllIcomponentsAvailable()) {
                icomponentIds.add("*");
            } 
            if (!icomponents.isEmpty()) {
                icomponents.forEach(icomponent -> {
                    icomponentIds.add(icomponent.getId().toString());
                    });
            }
            
            actions = getIdSet(getClientHiveActions());
        }

        JwtTokenVO tokenVO = new JwtTokenVO();
        JwtUserPayload accessPayload = JwtUserPayload.newBuilder()
                .withUserId(user.getId())
                .withActions(actions)
                .withNetworkIds(networkIds)
                .withIexperimentIds(iexperimentIds)
                .withIcomponentIds(icomponentIds)
                .buildPayload();

        JwtUserPayload refreshPayload = JwtUserPayload.newBuilder().withPayload(accessPayload)
                .buildPayload();

        tokenVO.setAccessToken(tokenService.generateJwtAccessToken(accessPayload, false));
        tokenVO.setRefreshToken(tokenService.generateJwtRefreshToken(refreshPayload, false));
        return tokenVO;
    }
}
