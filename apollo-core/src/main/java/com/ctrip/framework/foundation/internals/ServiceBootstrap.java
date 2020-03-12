package com.ctrip.framework.foundation.internals;

import com.ctrip.framework.apollo.core.spi.Ordered;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class ServiceBootstrap {

  /**
   * 返回指定类型的第一个实例
   *
   * @param clazz
   * @param <S>
   * @return
   */
  public static <S> S loadFirst(Class<S> clazz) {
    //获取所有实例
    Iterator<S> iterator = loadAll(clazz);
    // 目录/META-INF/services/{clazzName} 下没有找到该类型的文件则抛出 IllegalStateException异常
    if (!iterator.hasNext()) {
      throw new IllegalStateException(String.format(
          "No implementation defined in /META-INF/services/%s, please check whether the file exists and has the right implementation class!",
          clazz.getName()));
    }
    //返回第一个实例
    return iterator.next();
  }

  /**
   * 指定类型创建配置文件中所有的实例
   *
   * @param clazz
   * @param <S>
   * @return
   */
  public static <S> Iterator<S> loadAll(Class<S> clazz) {
    // 加载 /META-INF/services/{clazzName} 文件内填写的所有类并创建对象
    ServiceLoader<S> loader = ServiceLoader.load(clazz);
    // 获取 /META-INF/services/{clazzName} 填写的所有类并返回对应的集合
    return loader.iterator();
  }

  /**
   * 返回order排序后的对象集合
   *
   * @param clazz
   * @param <S>
   * @return
   */
  public static <S extends Ordered> List<S> loadAllOrdered(Class<S> clazz) {
    Iterator<S> iterator = loadAll(clazz);
    // 目录/META-INF/services/{clazzName} 下没有找到该类型的文件则抛出 IllegalStateException异常
    if (!iterator.hasNext()) {
      throw new IllegalStateException(String.format(
          "No implementation defined in /META-INF/services/%s, please check whether the file exists and has the right implementation class!",
          clazz.getName()));
    }

    List<S> candidates = Lists.newArrayList(iterator);
    Collections.sort(candidates, new Comparator<S>() {
      @Override
      public int compare(S o1, S o2) {
        // the smaller order has higher priority
        // 顺序越小优先级越高
        return Integer.compare(o1.getOrder(), o2.getOrder());
      }
    });

    return candidates;
  }

  /**
   * 返回 优先级越高的 实例(Order最小)
   * @param clazz
   * @param <S>
   * @return
   */
  public static <S extends Ordered> S loadPrimary(Class<S> clazz) {
    List<S> candidates = loadAllOrdered(clazz);

    return candidates.get(0);
  }
}
