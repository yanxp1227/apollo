package com.ctrip.framework.apollo.portal.listener;

import com.ctrip.framework.apollo.common.dto.AppDTO;
import com.ctrip.framework.apollo.common.dto.AppNamespaceDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.tracer.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CreationListener {

  private static Logger logger = LoggerFactory.getLogger(CreationListener.class);

  private final PortalSettings portalSettings;
  private final AdminServiceAPI.AppAPI appAPI;
  private final AdminServiceAPI.NamespaceAPI namespaceAPI;

  public CreationListener(
      final PortalSettings portalSettings,
      final AdminServiceAPI.AppAPI appAPI,
      final AdminServiceAPI.NamespaceAPI namespaceAPI) {
    this.portalSettings = portalSettings;
    this.appAPI = appAPI;
    this.namespaceAPI = namespaceAPI;
  }

  @EventListener
  public void onAppCreationEvent(AppCreationEvent event) {
    // 将APP对象转成 AppDTO对象，source的属性值为null不转换到AppDTO的属性中去
    AppDTO appDTO = BeanUtils.transform(AppDTO.class, event.getApp());
    //获取有效的 Env 数组 <Pb1>
    List<Env> envs = portalSettings.getActiveEnvs();
    // 循环Env数组，调用 Admin Service 的 API ，创建 App 对象。 <Pb2>
    for (Env env : envs) {
      try {
        appAPI.createApp(env, appDTO);
      } catch (Throwable e) {
        logger.error("Create app failed. appId = {}, env = {})", appDTO.getAppId(), env, e);
        Tracer.logError(String.format("Create app failed. appId = %s, env = %s", appDTO.getAppId(), env), e);
      }
    }
  }

  @EventListener
  public void onAppNamespaceCreationEvent(AppNamespaceCreationEvent event) {
    //将 AppNamespace 转成 AppNamespaceDTO对象
    AppNamespaceDTO appNamespace = BeanUtils.transform(AppNamespaceDTO.class, event.getAppNamespace());
    //获取有效对的 Env 数组  <Pb>
    List<Env> envs = portalSettings.getActiveEnvs();
    //循环 Env 数组,调用对应的Admin service 的API,创建AppNamespace对象
    for (Env env : envs) {
      try {
        namespaceAPI.createAppNamespace(env, appNamespace);
      } catch (Throwable e) {
        logger.error("Create appNamespace failed. appId = {}, env = {}", appNamespace.getAppId(), env, e);
        Tracer.logError(String.format("Create appNamespace failed. appId = %s, env = %s", appNamespace.getAppId(), env), e);
      }
    }
  }

}
