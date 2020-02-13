package com.ctrip.framework.apollo.biz.repository;


import com.ctrip.framework.apollo.biz.entity.Cluster;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

/**
 * 提供给 Admin Service 和 Config Service 的数据访问层
 */
public interface ClusterRepository extends PagingAndSortingRepository<Cluster, Long> {

  List<Cluster> findByAppIdAndParentClusterId(String appId, Long parentClusterId);

  List<Cluster> findByAppId(String appId);

  Cluster findByAppIdAndName(String appId, String name);

  List<Cluster> findByParentClusterId(Long parentClusterId);
}
