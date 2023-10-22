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
import com.devicehive.dao.IcomponentDao;
import com.devicehive.model.Icomponent;
import com.devicehive.model.User;
import com.devicehive.vo.IcomponentVO;
import com.devicehive.vo.IcomponentWithUsersAndDevicesVO;
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
public class IcomponentDaoRdbmsImpl extends RdbmsGenericDao implements IcomponentDao {

    @Override
    public List<IcomponentVO> findByName(String name) {
        List<Icomponent> result = createNamedQuery(Icomponent.class, "Icomponent.findByName", Optional.of(CacheConfig.get()))
                .setParameter("name", name)
                .getResultList();
        Stream<IcomponentVO> objectStream = result.stream().map(Icomponent::convertIcomponent);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public void persist(IcomponentVO newIcomponent) {
        Icomponent icomponent = Icomponent.convert(newIcomponent);
        super.persist(icomponent);
        newIcomponent.setId(icomponent.getId());
    }

    @Override
    public List<IcomponentWithUsersAndDevicesVO> getIcomponentsByIdsAndUsers(Long idForFiltering, Set<Long> icomponentIds, Set<Long> permittedIcomponents) {
        TypedQuery<Icomponent> query = createNamedQuery(Icomponent.class, "Icomponent.getIcomponentsByIdsAndUsers",
                Optional.of(CacheConfig.get()))
                .setParameter("userId", idForFiltering)
                .setParameter("icomponentIds", icomponentIds)
                .setParameter("permittedIcomponents", permittedIcomponents);
        List<Icomponent> result = query.getResultList();
        Stream<IcomponentWithUsersAndDevicesVO> objectStream = result.stream().map(Icomponent::convertWithDevicesAndUsers);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public int deleteById(long id) {
        return createNamedQuery("Icomponent.deleteById", Optional.empty())
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public IcomponentVO find(Long icomponentId) {
        Icomponent icomponent = find(Icomponent.class, icomponentId);
        return icomponent != null ? Icomponent.convertIcomponent(icomponent) : null;
    }

    @Override
    public IcomponentVO merge(IcomponentVO existing) {
        Icomponent icomponent = find(Icomponent.class, existing.getId());
        icomponent.setName(existing.getName());
        icomponent.setDescription(existing.getDescription());
        icomponent.setEntityVersion(existing.getEntityVersion());
        super.merge(icomponent);
        return existing;
    }

    @Override
    public void assignToIcomponent(IcomponentVO icomponent, UserVO user) {
        assert icomponent != null && icomponent.getId() != null;
        assert user != null && user.getId() != null;
        Icomponent existing = find(Icomponent.class, icomponent.getId());
        User userReference = reference(User.class, user.getId());
        if (existing.getUsers() == null) {
            existing.setUsers(new HashSet<>());
        }
        existing.getUsers().add(userReference);
        super.merge(existing);
    }

    @Override
    public List<IcomponentVO> list(String name, String namePattern, String sortField, boolean sortOrderAsc, Integer take, Integer skip, Optional<HivePrincipal> principal) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Icomponent> criteria = cb.createQuery(Icomponent.class);
        Root<Icomponent> from = criteria.from(Icomponent.class);

        Predicate[] nameAndPrincipalPredicates = CriteriaHelper.icomponentListPredicates(cb, from, ofNullable(name), ofNullable(namePattern), principal);
        criteria.where(nameAndPrincipalPredicates);

        CriteriaHelper.order(cb, criteria, from, ofNullable(sortField), sortOrderAsc);

        TypedQuery<Icomponent> query = createQuery(criteria);
        cacheQuery(query, of(CacheConfig.refresh()));
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);
        List<Icomponent> result = query.getResultList();
        Stream<IcomponentVO> objectStream = result.stream().map(Icomponent::convertIcomponent);
        return objectStream.collect(Collectors.toList());
    }

    @Override
    public long count(String name, String namePattern, Optional<HivePrincipal> principal) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<Long> criteria = cb.createQuery(Long.class);
        Root<Icomponent> from = criteria.from(Icomponent.class);

        Predicate[] nameAndPrincipalPredicates = CriteriaHelper.icomponentListPredicates(cb, from,
                ofNullable(name), ofNullable(namePattern), principal);

        criteria.where(nameAndPrincipalPredicates);
        criteria.select(cb.count(from));
        return count(criteria);
    }

    @Override
    public List<IcomponentVO> listAll() {
        return createNamedQuery(Icomponent.class, "Icomponent.findAll", of(CacheConfig.refresh()))
                .getResultList().stream()
                .map(Icomponent::convertIcomponent).collect(Collectors.toList());
    }

    @Override
    public Optional<IcomponentVO> findFirstByName(String name) {
        return findByName(name).stream().findFirst();
    }

    @Override
    public Optional<IcomponentWithUsersAndDevicesVO> findWithUsers(long icomponentId) {
        List<Icomponent> icomponents = createNamedQuery(Icomponent.class, "Icomponent.findWithUsers", Optional.of(CacheConfig.refresh()))
                .setParameter("id", icomponentId)
                .getResultList();
        return icomponents.isEmpty() ? Optional.empty() : Optional.ofNullable(Icomponent.convertWithDevicesAndUsers(icomponents.get(0)));
    }

    @Override
    public Optional<IcomponentVO> findDefault(Set<Long> icomponentIds) {
        return createNamedQuery(Icomponent.class, "Icomponent.findOrderedByIdWithPermission", Optional.of(CacheConfig.refresh()))
                .setParameter("permittedIcomponents", icomponentIds)
                .getResultList().stream()
                .findFirst()
                .map(Icomponent::convertIcomponent);
    }
}
