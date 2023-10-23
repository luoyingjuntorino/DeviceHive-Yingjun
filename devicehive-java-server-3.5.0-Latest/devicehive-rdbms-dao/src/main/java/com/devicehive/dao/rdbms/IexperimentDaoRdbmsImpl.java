package com.devicehive.dao.rdbms;

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

import com.devicehive.auth.HivePrincipal;
import com.devicehive.dao.IexperimentDao;
import com.devicehive.model.Iexperiment;
import com.devicehive.model.User;
import com.devicehive.vo.IexperimentVO;
import com.devicehive.vo.IexperimentWithUsersAndDevicesVO;
import com.devicehive.vo.UserVO;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Repository
public class IexperimentDaoRdbmsImpl extends RdbmsGenericDao implements IexperimentDao {

    @Override
    public List<IexperimentVO> findByName(String name) {
        List<Iexperiment> result = createNamedQuery(Iexperiment.class, "Iexperiment.findByName", Optional.of(CacheConfig.get()))
                .setParameter("name", name)
                .getResultList();
        Stream<IexperimentVO> objectStream = result.stream().map(Iexperiment::convertIexperiment);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public void persist(IexperimentVO newIexperiment) {
        Iexperiment iexperiment = Iexperiment.convert(newIexperiment);
        super.persist(iexperiment);
        newIexperiment.setId(iexperiment.getId());
    }

    @Override
    public List<IexperimentWithUsersAndDevicesVO> getIexperimentsByIdsAndUsers(Long idForFiltering, Set<Long> iexperimentIds, Set<Long> permittedIexperiments) {
        TypedQuery<Iexperiment> query = createNamedQuery(Iexperiment.class, "Iexperiment.getIexperimentsByIdsAndUsers",
                Optional.of(CacheConfig.get()))
                .setParameter("userId", idForFiltering)
                .setParameter("iexperimentIds", iexperimentIds)
                .setParameter("permittedIexperiments", permittedIexperiments);
        List<Iexperiment> result = query.getResultList();
        Stream<IexperimentWithUsersAndDevicesVO> objectStream = result.stream().map(Iexperiment::convertWithDevicesAndUsers);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public int deleteById(long id) {
        return createNamedQuery("Iexperiment.deleteById", Optional.empty())
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public IexperimentVO find(Long iexperimentId) {
        Iexperiment iexperiment = find(Iexperiment.class, iexperimentId);
        return iexperiment != null ? Iexperiment.convertIexperiment(iexperiment) : null;
    }

    @Override
    public IexperimentVO merge(IexperimentVO existing) {
        Iexperiment iexperiment = find(Iexperiment.class, existing.getId());
        iexperiment.setName(existing.getName());
        iexperiment.setDescription(existing.getDescription());
        iexperiment.setEntityVersion(existing.getEntityVersion());
        super.merge(iexperiment);
        return existing;
    }

    @Override
    public void assignToIexperiment(IexperimentVO iexperiment, UserVO user) {
        assert iexperiment != null && iexperiment.getId() != null;
        assert user != null && user.getId() != null;
        Iexperiment existing = find(Iexperiment.class, iexperiment.getId());
        User userReference = reference(User.class, user.getId());
        if (existing.getUsers() == null) {
            existing.setUsers(new HashSet<>());
        }
        existing.getUsers().add(userReference);
        super.merge(existing);
    }

    @Override
    public List<IexperimentVO> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take, Integer skip, Optional<HivePrincipal> principal) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Iexperiment> criteria = cb.createQuery(Iexperiment.class);
        Root<Iexperiment> from = criteria.from(Iexperiment.class);

        Predicate[] nameAndPrincipalPredicates = CriteriaHelper.iexperimentListPredicates(cb, from, ofNullable(name), ofNullable(namePattern), principal);
        criteria.where(nameAndPrincipalPredicates);

        CriteriaHelper.order(cb, criteria, from, ofNullable(sortField), sortOrderAsc);

        TypedQuery<Iexperiment> query = createQuery(criteria);
        cacheQuery(query, of(CacheConfig.refresh()));
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);
        List<Iexperiment> result = query.getResultList();
        Stream<IexperimentVO> objectStream = result.stream().map(Iexperiment::convertIexperiment);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public long count(String name, String namePattern, Optional<HivePrincipal> principal) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
        Root<Iexperiment> from = criteria.from(Iexperiment.class);

        Predicate[] nameAndPrincipalPredicates = CriteriaHelper.iexperimentListPredicates(cb, from,
                ofNullable(name), ofNullable(namePattern), principal);

        criteria.where(nameAndPrincipalPredicates);
        criteria.select(cb.count(from));
        return count(criteria);
    }

    @Override
    public List<IexperimentVO> listAll() {
        return createNamedQuery(Iexperiment.class, "Iexperiment.findAll", of(CacheConfig.refresh()))
                .getResultList().stream()
                .map(Iexperiment::convertIexperiment).collect(Collectors.toList());
    }

    @Override
    public Optional<IexperimentVO> findFirstByName(String name) {
        return findByName(name).stream().findFirst();
    }

    @Override
    public Optional<IexperimentWithUsersAndDevicesVO> findWithUsers(long iexperimentId) {
        List<Iexperiment> iexperiments = createNamedQuery(Iexperiment.class, "Iexperiment.findWithUsers", Optional.of(CacheConfig.refresh()))
                .setParameter("id", iexperimentId)
                .getResultList();
        return iexperiments.isEmpty() ? Optional.empty() : Optional.ofNullable(Iexperiment.convertWithDevicesAndUsers(iexperiments.get(0)));
    }

    @Override
    public Optional<IexperimentVO> findDefault(Set<Long> iexperimentIds) {
        return createNamedQuery(Iexperiment.class, "Iexperiment.findOrderedByIdWithPermission", Optional.of(CacheConfig.refresh()))
                .setParameter("permittedIexperiments", iexperimentIds)
                .getResultList().stream()
                .findFirst()
                .map(Iexperiment::convertIexperiment);
    }
}
