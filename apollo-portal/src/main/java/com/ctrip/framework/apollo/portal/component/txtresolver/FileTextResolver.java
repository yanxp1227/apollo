package com.ctrip.framework.apollo.portal.component.txtresolver;

import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.utils.StringUtils;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 文件配置文本解析器
 * 适用于 yaml、yml、json、xml 格式
 */
@Component("fileTextResolver")
public class FileTextResolver implements ConfigTextResolver {


  @Override
  public ItemChangeSets resolve(long namespaceId, String configText, List<ItemDTO> baseItems) {
    ItemChangeSets changeSets = new ItemChangeSets();
    // 原有Item为空且配置文本为空，不进行修改
    if (CollectionUtils.isEmpty(baseItems) && StringUtils.isEmpty(configText)) {
      return changeSets;
    }
    if (CollectionUtils.isEmpty(baseItems)) {
      // 不存在已有配置，创建 ItemDTO 到 ItemChangeSets 新增项
      changeSets.addCreateItem(createItem(namespaceId, 0, configText));
    } else {
      // 已存在配置，创建 ItemDTO 到 ItemChangeSets 修改项
      ItemDTO beforeItem = baseItems.get(0);
      if (!configText.equals(beforeItem.getValue())) {//update
        changeSets.addUpdateItem(createItem(namespaceId, beforeItem.getId(), configText));
      }
    }

    return changeSets;
  }

  private ItemDTO createItem(long namespaceId, long itemId, String value) {
    ItemDTO item = new ItemDTO();
    item.setId(itemId);
    item.setNamespaceId(namespaceId);
    item.setValue(value);
    item.setLineNum(1);
    item.setKey(ConfigConsts.CONFIG_FILE_CONTENT_KEY);
    return item;
  }
}
