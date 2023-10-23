package com.devicehive.dao;

/*
 * #%L
 * DeviceHive Common Dao interfaces
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

import com.devicehive.auth.HivePrincipal;
import com.devicehive.vo.IexperimentVO;
import com.devicehive.vo.IexperimentWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IexperimentDao {
    List<IexperimentVO> findByName(String name);

    void persist(IexperimentVO newIexperiment);

    List<IexperimentWithUsersAndDevicesVO> getIexperimentsByIdsAndUsers(Long idForFiltering, Set<Long> singleton, Set<Long> permittedIexperiments);

    int deleteById(long id);

    IexperimentVO find(@NotNull Long iexperimentId);

    IexperimentVO merge(IexperimentVO existing);

    void assignToIexperiment(IexperimentVO iexperiment, UserVO user);

    List<IexperimentVO> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take,
                         Integer skip, Optional<HivePrincipal> principal);

    long count(String name, String namePattern, Optional<HivePrincipal> principal);

    List<IexperimentVO> listAll();

    Optional<IexperimentVO> findFirstByName(String name);

    Optional<IexperimentWithUsersAndDevicesVO> findWithUsers(@NotNull long iexperimentId);

    Optional<IexperimentVO> findDefault(Set<Long> iexperimentIds);
}
