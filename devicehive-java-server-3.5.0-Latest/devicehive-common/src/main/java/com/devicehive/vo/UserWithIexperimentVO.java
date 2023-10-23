package com.devicehive.vo;

/*
 * #%L
 * DeviceHive Common Module
 * %%
 * Copyright (C) 2016 - 2017 DataArt
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

import com.devicehive.json.strategies.JsonPolicyDef;

import java.util.HashSet;
import java.util.Set;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;

public class UserWithIexperimentVO extends UserVO {

    @JsonPolicyDef({USER_PUBLISHED})
    private Set<IexperimentVO> iexperiments;

    public Set<IexperimentVO> getIexperiments() {
        return iexperiments;
    }

    public void setIexperiments(Set<IexperimentVO> iexperiments) {
        this.iexperiments = iexperiments;
    }

    public static UserWithIexperimentVO fromUserVO(UserVO dc) {
        UserWithIexperimentVO vo = null;
        if (dc != null) {
            vo = new UserWithIexperimentVO();
            vo.setData(dc.getData());
            vo.setId(dc.getId());
            vo.setData(dc.getData());
            vo.setLastLogin(dc.getLastLogin());
            vo.setLogin(dc.getLogin());
            vo.setLoginAttempts(dc.getLoginAttempts());
            vo.setIexperiments(new HashSet<>());
            vo.setPasswordHash(dc.getPasswordHash());
            vo.setPasswordSalt(dc.getPasswordSalt());
            vo.setRole(dc.getRole());
            vo.setStatus(dc.getStatus());
            vo.setIntroReviewed(dc.getIntroReviewed());
            vo.setAllIexperimentsAvailable(dc.getAllIexperimentsAvailable());
        }

        return vo;
    }

}
