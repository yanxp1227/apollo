package com.ctrip.framework.apollo.biz.entity;

import com.google.common.base.MoreObjects;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Entity
@Table(name = "ReleaseMessage")
public class ReleaseMessage {

  /**
   * 编号
   */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "Id")
  private long id;

  /**
   * 消息内容，通过 {@link com.ctrip.framework.apollo.biz.utils.ReleaseMessageKeyGenerator#generate(String, String, String)} 方法生成。
   */
  @Column(name = "Message", nullable = false)
  private String message;
  /**
   * 最后更新时间
   */
  @Column(name = "DataChange_LastTime")
  private Date dataChangeLastModifiedTime;

  /** 保存时执行该方法*/
  @PrePersist
  protected void prePersist() {
    if (this.dataChangeLastModifiedTime == null) {
      dataChangeLastModifiedTime = new Date();
    }
  }

  public ReleaseMessage() {
  }

  public ReleaseMessage(String message) {
    this.message = message;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("id", id)
        .add("message", message)
        .add("dataChangeLastModifiedTime", dataChangeLastModifiedTime)
        .toString();
  }
}
