package com.devicehive.model.eventbus;

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

import com.hazelcast.nio.serialization.Portable;
import com.hazelcast.nio.serialization.PortableReader;
import com.hazelcast.nio.serialization.PortableWriter;

import java.io.IOException;
import java.util.Objects;
import java.util.StringJoiner;

public class Filter implements Portable {

    public static final int FACTORY_ID = 1;
    public static final int CLASS_ID = 4;

    private Long networkId;

    private Long iexperimentId;

    private Long icomponentId;

    private String deviceId;

    private String eventName;

    private String name;

    public Filter() {

    }

    public Filter(Long networkId, Long iexperimentId, Long icomponentId, String deviceId, String eventName, String name) {
        this.networkId = networkId;
        this.iexperimentId = iexperimentId;
        this.icomponentId = icomponentId;
        this.deviceId = deviceId;
        this.eventName = eventName;
        this.name = name;
    }

    public Long getNetworkId() {
        return networkId;
    }

    public void setNetworkId(Long networkId) {
        this.networkId = networkId;
    }

    public Long getIexperimentId() {
        return iexperimentId;
    }

    public void setIexperimentId(Long iexperimentId) {
        this.iexperimentId = iexperimentId;
    }

    public Long getIcomponentId() {
        return icomponentId;
    }

    public void setIcomponentId(Long icomponentId) {
        this.icomponentId = icomponentId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstKey() {
        StringJoiner joiner = new StringJoiner(",");

        joiner.add(networkId != null ? networkId.toString() : "*")
                .add(iexperimentId != null ? iexperimentId.toString() : "*")
                .add(icomponentId != null ? icomponentId.toString() : "*")
                .add(deviceId != null ? deviceId : "*");

        return joiner.toString();
    }

    public String getDeviceIgnoredFirstKey() {
        StringJoiner joiner = new StringJoiner(",");

        joiner.add(networkId != null ? networkId.toString() : "*")
                .add(iexperimentId != null ? iexperimentId.toString() : "*")
                .add(icomponentId != null ? icomponentId.toString() : "*")
                .add("*");

        return joiner.toString();
    }

    public String getSecondKey() {
        StringJoiner joiner = new StringJoiner(",");

        joiner.add(eventName != null ? eventName : "*")
                .add(name != null ? name : "*");

        return joiner.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Filter)) return false;
        Filter that = (Filter) o;
        return Objects.equals(networkId, that.networkId) &&
                Objects.equals(iexperimentId, that.iexperimentId) &&
                Objects.equals(icomponentId, that.icomponentId) &&
                Objects.equals(deviceId, that.deviceId) &&
                Objects.equals(eventName, that.eventName) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId, iexperimentId, icomponentId, deviceId, eventName, name);
    }

    @Override
    public String toString() {
        return "Filter{" +
                "networkId=" + networkId +
                ", iexperimentId=" + iexperimentId +
                ", icomponentId=" + icomponentId +
                ", deviceId=" + deviceId +
                ", eventName=" + eventName +
                ", name=" + name +
                '}';
    }

    @Override
    public int getFactoryId() {
        return FACTORY_ID;
    }

    @Override
    public int getClassId() {
        return CLASS_ID;
    }

    @Override
    public void writePortable(PortableWriter writer) throws IOException {
        writer.writeLong("networkId", Objects.nonNull(networkId) ? networkId : 0);
        writer.writeLong("iexperimentId", Objects.nonNull(iexperimentId) ? iexperimentId : 0);
        writer.writeLong("icomponentId", Objects.nonNull(icomponentId) ? icomponentId : 0);
        writer.writeUTF("deviceId", deviceId);
        writer.writeUTF("eventName", eventName);
        writer.writeUTF("name", name);
    }

    @Override
    public void readPortable(PortableReader reader) throws IOException {
        networkId = reader.readLong("networkId");
        iexperimentId = reader.readLong("iexperimentId");
        icomponentId = reader.readLong("icomponentId");
        deviceId = reader.readUTF("deviceId");
        eventName = reader.readUTF("eventName");
        name = reader.readUTF("name");
    }
}
