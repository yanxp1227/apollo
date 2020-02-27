package com.ctrip.framework.apollo.portal.component.txtresolver;

import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;

import com.google.common.base.Strings;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * properties 配置解析器
 *
 * normal property file resolver.
 * update comment and blank item implement by create new item and delete old item.
 * update normal key/value item implement by update.
 */
@Component("propertyResolver")
public class PropertyResolver implements ConfigTextResolver {

  private static final String KV_SEPARATOR = "=";
  private static final String ITEM_SEPARATOR = "\n";

  @Override
  public ItemChangeSets resolve(long namespaceId, String configText, List<ItemDTO> baseItems) {

    // 创建 Item Map ，以 lineNum 为 键
    Map<Integer, ItemDTO> oldLineNumMapItem = BeanUtils.mapByKey("lineNum", baseItems);
    // 创建 Item Map ，以 key 为 键
    Map<String, ItemDTO> oldKeyMapItem = BeanUtils.mapByKey("key", baseItems);

    //移除注释和空行的配置项。
    //remove comment and blank item map.
    oldKeyMapItem.remove("");

    // 按照拆分 Property 配置
    String[] newItems = configText.split(ITEM_SEPARATOR);

    // 校验是否存在重复配置 Key 。若是，抛出 BadRequestException 异常
    if (isHasRepeatKey(newItems)) {
      throw new BadRequestException("config text has repeat key please check.");
    }

    // 创建 ItemChangeSets 对象，并解析配置文件到 ItemChangeSets 中。
    ItemChangeSets changeSets = new ItemChangeSets();
    Map<Integer, String> newLineNumMapItem = new HashMap<>();//use for delete blank and comment item
    int lineCounter = 1;
    for (String newItem : newItems) {
      newItem = newItem.trim();
      newLineNumMapItem.put(lineCounter, newItem);
      // 使用行号，获得已存在的 ItemDTO
      ItemDTO oldItemByLine = oldLineNumMapItem.get(lineCounter);

      //comment item 注释Item
      if (isCommentItem(newItem)) {

        handleCommentLine(namespaceId, oldItemByLine, newItem, lineCounter, changeSets);

        //blank item  空白 Item
      } else if (isBlankItem(newItem)) {

        handleBlankLine(namespaceId, oldItemByLine, lineCounter, changeSets);

        //normal item  普通 Item
      } else {
        handleNormalLine(namespaceId, oldKeyMapItem, newItem, lineCounter, changeSets);
      }
      // 行号计数 + 1
      lineCounter++;
    }

    // 删除注释和空行配置项
    deleteCommentAndBlankItem(oldLineNumMapItem, newLineNumMapItem, changeSets);
    // 删除普通配置项
    deleteNormalKVItem(oldKeyMapItem, changeSets);

    return changeSets;
  }

  private boolean isHasRepeatKey(String[] newItems) {
    Set<String> keys = new HashSet<>();
    // 记录行数，用于报错提示，无业务逻辑需要。
    int lineCounter = 1;
    // 计数
    int keyCount = 0;
    for (String item : newItems) {
      if (!isCommentItem(item) && !isBlankItem(item)) { // 排除注释和空行的配置项
        keyCount++;
        String[] kv = parseKeyValueFromItem(item);
        if (kv != null) {
          keys.add(kv[0].toLowerCase());
        } else {
          throw new BadRequestException("line:" + lineCounter + " key value must separate by '='");
        }
      }
      lineCounter++;
    }

    return keyCount > keys.size();
  }

  private String[] parseKeyValueFromItem(String item) {
    int kvSeparator = item.indexOf(KV_SEPARATOR);
    if (kvSeparator == -1) {
      return null;
    }

    String[] kv = new String[2];
    kv[0] = item.substring(0, kvSeparator).trim();
    kv[1] = item.substring(kvSeparator + 1, item.length()).trim();
    return kv;
  }

  private void handleCommentLine(Long namespaceId, ItemDTO oldItemByLine, String newItem, int lineCounter, ItemChangeSets changeSets) {
    String oldComment = oldItemByLine == null ? "" : oldItemByLine.getComment();
    // 创建注释 ItemDTO 到 ItemChangeSets 的新增项，若老的配置项不是注释或者不相等。另外，更新注释配置，通过删除 + 添加的方式。
    //create comment. implement update comment by delete old comment and create new comment
    if (!(isCommentItem(oldItemByLine) && newItem.equals(oldComment))) {
      changeSets.addCreateItem(buildCommentItem(0L, namespaceId, newItem, lineCounter));
    }
  }

  private void handleBlankLine(Long namespaceId, ItemDTO oldItem, int lineCounter, ItemChangeSets changeSets) {
    // 创建空行 ItemDTO 到 ItemChangeSets 的新增项，若老的不是空行。另外，更新空行配置，通过删除 + 添加的方式
    if (!isBlankItem(oldItem)) {
      changeSets.addCreateItem(buildBlankItem(0L, namespaceId, lineCounter));
    }
  }

  private void handleNormalLine(Long namespaceId, Map<String, ItemDTO> keyMapOldItem, String newItem,
                                int lineCounter, ItemChangeSets changeSets) {

    // 解析一行，生成 [key, value]
    String[] kv = parseKeyValueFromItem(newItem);

    if (kv == null) {
      throw new BadRequestException("line:" + lineCounter + " key value must separate by '='");
    }

    String newKey = kv[0];
    String newValue = kv[1].replace("\\n", "\n"); //handle user input \n
    // 获得老的 ItemDTO 对象
    ItemDTO oldItem = keyMapOldItem.get(newKey);
    // 不存在，则创建 ItemDTO 到 ItemChangeSets 的添加项
    if (oldItem == null) {//new item
      changeSets.addCreateItem(buildNormalItem(0L, namespaceId, newKey, newValue, "", lineCounter));
    // 如果值或者行号不相等，则创建 ItemDTO 到 ItemChangeSets 的修改项
    } else if (!newValue.equals(oldItem.getValue()) || lineCounter != oldItem.getLineNum()) {//update item
      changeSets.addUpdateItem(
          buildNormalItem(oldItem.getId(), namespaceId, newKey, newValue, oldItem.getComment(),
              lineCounter));
    }
    // 移除老的 ItemDTO 对象
    keyMapOldItem.remove(newKey);
  }

  private boolean isCommentItem(ItemDTO item) {
    return item != null && "".equals(item.getKey())
        && (item.getComment().startsWith("#") || item.getComment().startsWith("!"));
  }

  /**
   * 判断行内容是否为注释内容
   * @param line
   * @return
   */
  private boolean isCommentItem(String line) {
    return line != null && (line.startsWith("#") || line.startsWith("!"));
  }

  private boolean isBlankItem(ItemDTO item) {
    return item != null && "".equals(item.getKey()) && "".equals(item.getComment());
  }

  private boolean isBlankItem(String line) {
    return  Strings.nullToEmpty(line).trim().isEmpty();
  }

  private void deleteNormalKVItem(Map<String, ItemDTO> baseKeyMapItem, ItemChangeSets changeSets) {
    // 将剩余的配置项，添加到 ItemChangeSets 的删除项
    //surplus item is to be deleted
    for (Map.Entry<String, ItemDTO> entry : baseKeyMapItem.entrySet()) {
      changeSets.addDeleteItem(entry.getValue());
    }
  }

  private void deleteCommentAndBlankItem(Map<Integer, ItemDTO> oldLineNumMapItem,
                                         Map<Integer, String> newLineNumMapItem,
                                         ItemChangeSets changeSets) {

    for (Map.Entry<Integer, ItemDTO> entry : oldLineNumMapItem.entrySet()) {
      int lineNum = entry.getKey();
      ItemDTO oldItem = entry.getValue();
      String newItem = newLineNumMapItem.get(lineNum);

      // 添加到 ItemChangeSets 的删除项
      //1. old is blank by now is not
      //2.old is comment by now is not exist or modified
      if ((isBlankItem(oldItem) && !isBlankItem(newItem))  // 老的是空行配置项，新的不是空行配置项
          || isCommentItem(oldItem) && (newItem == null || !newItem.equals(oldItem.getComment()))) {  // 老的是注释配置项，新的不相等
        changeSets.addDeleteItem(oldItem);
      }
    }
  }

  private ItemDTO buildCommentItem(Long id, Long namespaceId, String comment, int lineNum) {
    return buildNormalItem(id, namespaceId, "", "", comment, lineNum);
  }

  private ItemDTO buildBlankItem(Long id, Long namespaceId, int lineNum) {
    return buildNormalItem(id, namespaceId, "", "", "", lineNum);
  }

  private ItemDTO buildNormalItem(Long id, Long namespaceId, String key, String value, String comment, int lineNum) {
    ItemDTO item = new ItemDTO(key, value, comment, lineNum);
    item.setId(id);
    item.setNamespaceId(namespaceId);
    return item;
  }
}
