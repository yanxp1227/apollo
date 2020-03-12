package com.ctrip.framework.apollo.core.schedule;

/**
 * 定时策略接口。在 Apollo 中，用于执行失败，计算下一次执行的延迟时间
 *
 * Schedule policy
 * @author Jason Song(song_s@ctrip.com)
 */
public interface SchedulePolicy {
  /**
   * 执行失败
   *
   * @return 下次执行延迟
   */
  long fail();

  /**
   * 执行成功
   */
  void success();
}
