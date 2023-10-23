package com.devicehive.model.response;

/*
 * #%L
 * DeviceHive Common Dao Interfaces
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

import com.devicehive.json.strategies.JsonPolicyDef;
import com.devicehive.model.HiveEntity;
import com.devicehive.vo.IexperimentVO;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.IEXPERIMENTS_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;

//TODO: javadoc
public class UserIexperimentResponse implements HiveEntity {

    private static final long serialVersionUID = 4328590211197574009L;
    @SerializedName("iexperiment")
    @JsonPolicyDef({USER_PUBLISHED, IEXPERIMENTS_LISTED})
    private IexperimentVO iexperiment;

    public static UserIexperimentResponse fromIexperiment(IexperimentVO iexperiment) {
        UserIexperimentResponse result = new UserIexperimentResponse();
        result.setIexperiment(iexperiment);
        return result;
    }

    public IexperimentVO getIexperiment() {
        return iexperiment;
    }

    public void setIexperiment(IexperimentVO iexperiment) {
        this.iexperiment = iexperiment;
    }
}
