package com.devicehive.auth;

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

import com.devicehive.service.BaseDeviceService;
import com.devicehive.vo.DeviceVO;
import com.devicehive.vo.PluginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class JwtCheckPermissionsHelper {

    private final BaseDeviceService deviceService;

    @Autowired
    public JwtCheckPermissionsHelper(BaseDeviceService deviceService) {
        this.deviceService = deviceService;
    }

    public boolean checkPermissions(
            HivePrincipal hivePrincipal,
            HiveAction action,
            Object targetDomainObject) {

        Set<HiveAction> permittedActions = hivePrincipal.getActions();
        return checkActionAllowed(action, permittedActions)
                && checkNetworksAllowed(hivePrincipal, action, targetDomainObject)
                && checkIexperimentsAllowed(hivePrincipal, action, targetDomainObject)
                && checkIcomponentsAllowed(hivePrincipal, action, targetDomainObject)
                && checkDeviceIdsAllowed(hivePrincipal, targetDomainObject);
    }

    // TODO - verify permission-checking logic
    private boolean checkActionAllowed(HiveAction hiveAction, Set<HiveAction> permissions) {
        boolean result = false;
        if (permissions != null) result = permissions.contains(hiveAction);
        return result;
    }

    private boolean checkNetworksAllowed(HivePrincipal principal, HiveAction action, Object targetDomainObject) {
        if (principal.areAllNetworksAvailable()) return true;
        else if (targetDomainObject instanceof Long && action.getValue().contains("Network")) {
            return principal.getNetworkIds() != null && principal.getNetworkIds().contains(targetDomainObject);
        }
        return true;
    }

    private boolean checkIexperimentsAllowed(HivePrincipal principal, HiveAction action, Object targetDomainObject) {
        if (principal.areAllIexperimentsAvailable()) return true;
        else if (targetDomainObject instanceof Long && action.getValue().contains("Iexperiment")) {
            return principal.getIexperimentIds() != null && principal.getIexperimentIds().contains(targetDomainObject);
        }
        return true;
    }

    private boolean checkIcomponentsAllowed(HivePrincipal principal, HiveAction action, Object targetDomainObject) {
        if (principal.areAllIcomponentsAvailable()) return true;
        else if (targetDomainObject instanceof Long && action.getValue().contains("Icomponent")) {
            return principal.getIcomponentIds() != null && principal.getIcomponentIds().contains(targetDomainObject);
        }
        return true;
    }

    private boolean checkDeviceIdsAllowed(HivePrincipal principal, Object targetDomainObject) {

        if (targetDomainObject instanceof String) {
            final PluginVO plugin = principal.getPlugin();
            if (plugin != null && plugin.getTopicName() != null) {
                return plugin.getTopicName().equals(targetDomainObject);
            }

            if (principal.areAllIexperimentsAvailable() && principal.areAllNetworksAvailable() && principal.areAllIcomponentsAvailable()) {
                return true;
            }

            final Set<Long> networks = principal.getNetworkIds();
            final Set<Long> iexperiments = principal.getIexperimentIds();
            final Set<Long> icomponents = principal.getIcomponentIds();
            DeviceVO device = deviceService.findByIdWithPermissionsCheck((String) targetDomainObject, principal);
            //TODO: add combinations of logic about icomponent
            if (device == null) {
                return false;
            }
            // if (principal.areAllNetworksAvailable() && iexperiments != null) {
            //     return iexperiments.contains(device.getIexperimentId());
            // }
            // if (principal.areAllIexperimentsAvailable() && networks != null) {
            //     return networks.contains(device.getNetworkId());
            // }
            //..........................................below codes had modified
            if (principal.areAllIcomponentsAvailable() && iexperiments != null && networks != null) {
                return iexperiments.contains(device.getIexperimentId()) && networks.contains(device.getNetworkId());
            }
            if (principal.areAllIcomponentsAvailable() && principal.areAllIexperimentsAvailable() && networks != null) {
                return networks.contains(device.getNetworkId());
            }
            if (principal.areAllIcomponentsAvailable() && principal.areAllNetworksAvailable() && iexperiments != null) {
                return iexperiments.contains(device.getIexperimentId());
            }
            if (principal.areAllIexperimentsAvailable() && icomponents != null && networks != null) {
                return networks.contains(device.getNetworkId()) && icomponents.contains(device.getIcomponentId());
            }
            if (principal.areAllNetworksAvailable() && icomponents != null && iexperiments != null) {
                return iexperiments.contains(device.getIexperimentId()) && icomponents.contains(device.getIcomponentId());
            }
            if (principal.areAllNetworksAvailable() && principal.areAllIexperimentsAvailable() && icomponents != null) {
                return icomponents.contains(device.getIcomponentId());
            }
            //..........................................above codes had modified
            if (networks != null && iexperiments != null && icomponents != null) {
                return networks.contains(device.getNetworkId()) && iexperiments.contains(device.getIexperimentId()) && icomponents.contains(device.getIcomponentId());
            }

            return false;
        }

        return true;
    }
}
