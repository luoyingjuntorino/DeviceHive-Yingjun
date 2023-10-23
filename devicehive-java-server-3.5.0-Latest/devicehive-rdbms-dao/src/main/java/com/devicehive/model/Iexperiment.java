package com.devicehive.model;

/*
 * #%L
 * DeviceHive Dao RDBMS Implementation
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
import com.devicehive.vo.*;
import com.google.gson.annotations.SerializedName;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static com.devicehive.json.strategies.JsonPolicyDef.Policy.*;

@Entity
@Table(name = "iexperiment")
@NamedQueries({
        @NamedQuery(name = "Iexperiment.findAll", query = "select t from Iexperiment t"),
        @NamedQuery(name = "Iexperiment.findByName", query = "select t from Iexperiment t where t.name = :name"),
        @NamedQuery(name = "Iexperiment.findWithUsers", query = "select t from Iexperiment t left join fetch t.users where t.id = :id"),
        @NamedQuery(name = "Iexperiment.findOrderedByIdWithPermission", query = "select t from Iexperiment t " +
                "where (t.id in :permittedIexperiments or :permittedIexperiments is null) order by t.id"),
        @NamedQuery(name = "Iexperiment.deleteById", query = "delete from Iexperiment t where t.id = :id"),
        @NamedQuery(name = "Iexperiment.getWithDevices", query = "select t from Iexperiment t left join fetch t.devices where t.id = :id"),
        @NamedQuery(name = "Iexperiment.getIexperimentsByIdsAndUsers", query = "select t from Iexperiment t left outer join t.users u left join fetch t.devices d " +
                "where t.id in :iexperimentIds and (u.id = :userId or :userId is null) and (t.id in :permittedIexperiments or :permittedIexperiments is null)")
})
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Iexperiment implements HiveEntity {

    // private static final long serialVersionUID = -4534503697839217385L;
    private static final long serialVersionUID = -4534583697839217385L;

    @SerializedName("id")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonPolicyDef({DEVICE_PUBLISHED, USER_PUBLISHED, IEXPERIMENTS_LISTED, IEXPERIMENT_PUBLISHED, IEXPERIMENT_SUBMITTED})
    private Long id;

    @SerializedName("name")
    @Column
    @NotNull(message = "name field cannot be null.")
    @Size(min = 1, max = 128, message = "Field cannot be empty. The length of name should not be more than 128 " +
            "symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, IEXPERIMENTS_LISTED, IEXPERIMENT_PUBLISHED})
    private String name;

    @SerializedName("description")
    @Column
    @Size(max = 128, message = "The length of description should not be more than 128 symbols.")
    @JsonPolicyDef({DEVICE_PUBLISHED, DEVICE_SUBMITTED, USER_PUBLISHED, IEXPERIMENTS_LISTED, IEXPERIMENT_PUBLISHED})
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_iexperiment", joinColumns = {@JoinColumn(name = "iexperiment_id", nullable = false,
            updatable = false)},
            inverseJoinColumns = {@JoinColumn(name = "user_id", nullable = false, updatable = false)})
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<User> users;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "iexperiment")
    @JsonPolicyDef({IEXPERIMENT_PUBLISHED})
    @org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<Device> devices;

    @Version
    @Column(name = "entity_version")
    private Long entityVersion;

    public Set<Device> getDevices() {
        return devices;
    }

    public void setDevices(Set<Device> devices) {
        this.devices = devices;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    public Long getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(Long entityVersion) {
        this.entityVersion = entityVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Iexperiment iexperiment = (Iexperiment) o;

        return !(id != null ? !id.equals(iexperiment.id) : iexperiment.id != null);

    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Iexperiment{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }

    public static IexperimentVO convertIexperiment(Iexperiment iexperiment) {
        if (iexperiment != null) {
            IexperimentVO vo = new IexperimentVO();
            vo.setId(iexperiment.getId());
            vo.setName(iexperiment.getName());
            vo.setDescription(iexperiment.getDescription());
            vo.setEntityVersion(iexperiment.getEntityVersion());
            return vo;
        }
        return null;
    }

    public static IexperimentWithUsersAndDevicesVO convertWithDevicesAndUsers(Iexperiment iexperiment) {
        if (iexperiment != null) {
            IexperimentVO vo1 = convertIexperiment(iexperiment);
            IexperimentWithUsersAndDevicesVO vo = new IexperimentWithUsersAndDevicesVO(vo1);
            if (iexperiment.getUsers() != null) {
                vo.setUsers(iexperiment.getUsers().stream().map(User::convertToVo).collect(Collectors.toSet()));
            } else {
                vo.setUsers(Collections.emptySet());
            }
            if (iexperiment.getDevices() != null) {
                Set<DeviceVO> deviceList = iexperiment.getDevices().stream().map(Device::convertToVo).collect(Collectors.toSet());
                vo.setDevices(deviceList);
            } else {
                vo.setDevices(Collections.emptySet());
            }
            return vo;
        }
        return null;
    }

    public static Iexperiment convert(IexperimentVO vo) {
        if (vo != null) {
            Iexperiment iexperiment = new Iexperiment();
            iexperiment.setId(vo.getId());
            iexperiment.setName(vo.getName());
            iexperiment.setDescription(vo.getDescription());
            iexperiment.setEntityVersion(vo.getEntityVersion());
            return iexperiment;
        }
        return null;
    }
}
