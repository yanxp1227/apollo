package com.ctrip.framework.apollo.configservice.wrapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;

/**
 * DeferredResult 包装器，封装 DeferredResult 的公用方法
 * @author Jason Song(song_s@ctrip.com)
 */
public class DeferredResultWrapper implements Comparable<DeferredResultWrapper> {

  /**
   * 未修改时的 ResponseEntity 响应，使用 302 状态码。
   */
  private static final ResponseEntity<List<ApolloConfigNotification>>
      NOT_MODIFIED_RESPONSE_LIST = new ResponseEntity<>(HttpStatus.NOT_MODIFIED);

  /**
   * 归一化和原始的 Namespace 的名字的 Map
   * key:数据库NamespaceName   value:客户端传过来的NamespaceName
   *
   * 作用:客户端在填写 Namespace 时，写错了名字的大小写。在 Config Service 中，会进行归一化“修复”，方便逻辑的统一编写。
   *      但是，最终返回给客户端需要“还原”回原始( original )的 Namespace 的名字，避免客户端无法识别
   */
  private Map<String, String> normalizedNamespaceNameToOriginalNamespaceName;
  /**
   * 响应的 DeferredResult 对象,
   */
  private DeferredResult<ResponseEntity<List<ApolloConfigNotification>>> result;


  /**
   * 在构造器中初始化对象并指定给超时时间
   *
   * @param timeoutInMilli 超时时间
   */
  public DeferredResultWrapper(long timeoutInMilli) {
    result = new DeferredResult<>(timeoutInMilli, NOT_MODIFIED_RESPONSE_LIST);
  }

  public void recordNamespaceNameNormalizedResult(String originalNamespaceName, String normalizedNamespaceName) {
    if (normalizedNamespaceNameToOriginalNamespaceName == null) {
      normalizedNamespaceNameToOriginalNamespaceName = Maps.newHashMap();
    }
    // 添加到 `normalizedNamespaceNameToOriginalNamespaceName` 中
    normalizedNamespaceNameToOriginalNamespaceName.put(normalizedNamespaceName, originalNamespaceName);
  }


  public void onTimeout(Runnable timeoutCallback) {
    //DeferredResult超过指定超时时间后会执行
    result.onTimeout(timeoutCallback);
  }

  public void onCompletion(Runnable completionCallback) {
    //DeferredResult的结束事件(Timeout和setResult时会执行)
    result.onCompletion(completionCallback);
  }


  public void setResult(ApolloConfigNotification notification) {
    setResult(Lists.newArrayList(notification));
  }

  /**
   * The namespace name is used as a key in client side, so we have to return the original one instead of the correct one
   */
  public void setResult(List<ApolloConfigNotification> notifications) {
    // 恢复被归一化的 Namespace 的名字为原始的 Namespace 的名字
    if (normalizedNamespaceNameToOriginalNamespaceName != null) {
      notifications.stream().filter(notification -> normalizedNamespaceNameToOriginalNamespaceName.containsKey
          (notification.getNamespaceName())).forEach(notification -> notification.setNamespaceName(
              normalizedNamespaceNameToOriginalNamespaceName.get(notification.getNamespaceName())));
    }
    // 设置结果，并使用 200 状态码。
    result.setResult(new ResponseEntity<>(notifications, HttpStatus.OK));
  }

  public DeferredResult<ResponseEntity<List<ApolloConfigNotification>>> getResult() {
    return result;
  }

  @Override
  public int compareTo(@NonNull DeferredResultWrapper deferredResultWrapper) {
    return Integer.compare(this.hashCode(), deferredResultWrapper.hashCode());
  }
}
