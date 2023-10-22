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
import com.devicehive.vo.IcomponentVO;
import com.google.gson.annotations.SerializedName;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.ICOMPONENTS_LISTED;
import static com.devicehive.json.strategies.JsonPolicyDef.Policy.USER_PUBLISHED;

//TODO: javadoc
public class UserIcomponentResponse implements HiveEntity {

    // private static final long serialVersionUID = 4328590211197574009L;
    private static final long serialVersionUID = 4320590211197574009L;
    
    @SerializedName("icomponent")
    @JsonPolicyDef({USER_PUBLISHED, ICOMPONENTS_LISTED})
    private IcomponentVO icomponent;

    public static UserIcomponentResponse fromIcomponent(IcomponentVO icomponent) {
        UserIcomponentResponse result = new UserIcomponentResponse();
        result.setIcomponent(icomponent);
        return result;
    }

    public IcomponentVO getIcomponent() {
        return icomponent;
    }

    public void setIcomponent(IcomponentVO icomponent) {
        this.icomponent = icomponent;
    }
}
