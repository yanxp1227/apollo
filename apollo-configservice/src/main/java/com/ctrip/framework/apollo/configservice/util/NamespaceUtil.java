package com.ctrip.framework.apollo.configservice.util;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.configservice.service.AppNamespaceServiceWithCache;
import org.springframework.stereotype.Component;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Component
public class NamespaceUtil {

  private final AppNamespaceServiceWithCache appNamespaceServiceWithCache;

  public NamespaceUtil(final AppNamespaceServiceWithCache appNamespaceServiceWithCache) {
    this.appNamespaceServiceWithCache = appNamespaceServiceWithCache;
  }

  public String filterNamespaceName(String namespaceName) {
    if (namespaceName.toLowerCase().endsWith(".properties")) {
      int dotIndex = namespaceName.lastIndexOf(".");
      return namespaceName.substring(0, dotIndex);
    }

    return namespaceName;
  }

  /**
   * 从本地缓存中获取AppNamespace的名字
   * @param appId
   * @param namespaceName
   * @return
   */
  public String normalizeNamespace(String appId, String namespaceName) {
    // 获得 App 下的 AppNamespace 对象
    AppNamespace appNamespace = appNamespaceServiceWithCache.findByAppIdAndNamespace(appId, namespaceName);
    if (appNamespace != null) {
      return appNamespace.getName();
    }

    // 获取不到，说明该 Namespace 可能是关联的
    appNamespace = appNamespaceServiceWithCache.findPublicNamespaceByName(namespaceName);
    if (appNamespace != null) {
      return appNamespace.getName();
    }
    //都查询不到，直接返回。为什么呢？
    // 因为 AppNamespaceServiceWithCache 是基于缓存实现，可能对应的 AppNamespace 暂未缓存到内存中
    return namespaceName;
  }
}
