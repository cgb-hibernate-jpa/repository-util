package com.supcon.mare.repository.service.impl;

import com.supcon.mare.repository.constant.Constants;
import com.supcon.mare.repository.service.MyRepository;
import com.supcon.mare.repository.util.ReflectUtil;
import com.supcon.mare.repository.util.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import java.io.Serializable;
import java.util.*;

/**
 * @author: zhaoxu
 * @description: JPA通用功能扩展
 */
public class MyRepositoryImpl<T, ID extends Serializable>
        extends SimpleJpaRepository<T, ID> implements MyRepository<T, ID> {

    private ReflectUtil reflectUtil = new ReflectUtil();

    private EntityManager entityManager;

    private Class<T> clazz;

    @Autowired(required = false)
    public MyRepositoryImpl(JpaEntityInformation<T, ID> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.clazz = entityInformation.getJavaType();
        this.entityManager = entityManager;
    }

    @Override
    public Page<T> findByPage(Map<String, String> tableMap, List<String> excludeAttr, Map joinField, String sortAttr) {
        int current = Integer.valueOf(tableMap.get(Constants.CURRENT));
        int pageSize = Integer.valueOf(tableMap.get(Constants.PAGE_SIZE));

        Pageable pageable;
        if (!StringUtils.isEmpty(sortAttr)) {
            pageable = PageRequest.of(current - 1, pageSize, Utils.sortAttr(tableMap, sortAttr));
        } else {
            pageable = PageRequest.of(current - 1, pageSize);
        }

        Specification<T> specification = reflectUtil.createSpecification(tableMap, clazz, excludeAttr, joinField);
        return this.findAll(specification, pageable);
    }

    @Override
    public List<T> findByConditions(Map<String, String> tableMap, List<String> excludeAttr, Map joinField, String sortAttr) {
        Specification<T> specification = reflectUtil.createSpecification(tableMap, clazz, excludeAttr, joinField);

        if (!StringUtils.isEmpty(sortAttr)) {
            return this.findAll(specification, Utils.sortAttr(tableMap, sortAttr));
        } else {
            return this.findAll(specification);
        }

    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    public void deleteValid(String ids) {
        List<String> strings = Arrays.asList(ids.split(","));
        if (!CollectionUtils.isEmpty(strings)) {
            strings.stream().forEach(id -> {
                Object object = this.findById((ID) Long.valueOf(id)).get();
                reflectUtil.setValue(clazz, object, "valid", 0);
                reflectUtil.setValue(clazz, object, "gmtModified", Utils.getNowDate());
            });
        }
    }

    @Override
    public T findByAttr(String attr, String condition) {
        List<T> resultList = new ArrayList<>();
        List<String> excludeAttr = new ArrayList<>();

        Map<String, String> tableMap = new HashMap<>(4);
        tableMap.put(attr, condition);

        excludeAttr.add(attr);

        Specification<T> specification = reflectUtil.createSpecification(tableMap, clazz, excludeAttr, null);
        resultList.addAll(this.findAll(specification));

        if (!CollectionUtils.isEmpty(resultList)) {
            return resultList.get(0);
        } else {
            return null;
        }
    }
}
