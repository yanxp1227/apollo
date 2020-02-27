package com.ctrip.framework.apollo.biz.utils;

import com.ctrip.framework.apollo.biz.entity.Item;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.springframework.beans.BeanUtils;

/**
 * 配置变更内容构建起
 */
public class ConfigChangeContentBuilder {

  private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create();

  /**
   * 创建Item集合
   */
  private List<Item> createItems = new LinkedList<>();
  /**
   * 更新Item集合
   */
  private List<ItemPair> updateItems = new LinkedList<>();
  /**
   * 删除Item集合
   */
  private List<Item> deleteItems = new LinkedList<>();


  /**
   * 添加 Item 到集合createItems 中
   * @param item
   * @return
   */
  public ConfigChangeContentBuilder createItem(Item item) {
    if (!StringUtils.isEmpty(item.getKey())){
      createItems.add(cloneItem(item));
    }
    return this;
  }

  /**
   * 创建ItemPair并添加到集合updateItems中
   * @param oldItem
   * @param newItem
   * @return
   */
  public ConfigChangeContentBuilder updateItem(Item oldItem, Item newItem) {
    if (!oldItem.getValue().equals(newItem.getValue())){
      ItemPair itemPair = new ItemPair(cloneItem(oldItem), cloneItem(newItem));
      updateItems.add(itemPair);
    }
    return this;
  }

  /**
   * 将Item添加到集合deleteItems中
   * @param item
   * @return
   */
  public ConfigChangeContentBuilder deleteItem(Item item) {
    if (!StringUtils.isEmpty(item.getKey())) {
      deleteItems.add(cloneItem(item));
    }
    return this;
  }

  /**
   * 判断是否有变化。当且仅当有变化才记录 Commit
   * @return
   */
  public boolean hasContent(){
    return !createItems.isEmpty() || !updateItems.isEmpty() || !deleteItems.isEmpty();
  }

  /**
   * 构建 Item 变化的 JSON 字符串
   * @return
   */
  public String build() {
    //因为事务第一段提交并没有更新时间,所以build时统一更新
    Date now = new Date();

    for (Item item : createItems) {
      item.setDataChangeLastModifiedTime(now);
    }

    for (ItemPair item : updateItems) {
      item.newItem.setDataChangeLastModifiedTime(now);
    }

    for (Item item : deleteItems) {
      item.setDataChangeLastModifiedTime(now);
    }
    return gson.toJson(this);
  }

  static class ItemPair {

    Item oldItem; //老
    Item newItem; //新

    public ItemPair(Item oldItem, Item newItem) {
      this.oldItem = oldItem;
      this.newItem = newItem;
    }
  }

  /**
   * 克隆Item对象,防止在build方法中属性被修改
   *
   * @param source
   * @return
   */
  Item cloneItem(Item source) {
    Item target = new Item();

    BeanUtils.copyProperties(source, target);

    return target;
  }

  public static ConfigChangeContentBuilder convertJsonString(String content) {
    return gson.fromJson(content, ConfigChangeContentBuilder.class);
  }

  public List<Item> getCreateItems() {
    return createItems;
  }

  public List<ItemPair> getUpdateItems() {
    return updateItems;
  }

  public List<Item> getDeleteItems() {
    return deleteItems;
  }
}
