/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.engine.impl.persistence.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.flowable.engine.common.impl.persistence.entity.data.DataManager;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.history.HistoricTaskInstance;
import org.flowable.engine.impl.HistoricTaskInstanceQueryImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.history.HistoryLevel;
import org.flowable.engine.impl.persistence.entity.data.HistoricTaskInstanceDataManager;
import org.flowable.engine.impl.util.Flowable5Util;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class HistoricTaskInstanceEntityManagerImpl extends AbstractEntityManager<HistoricTaskInstanceEntity> implements HistoricTaskInstanceEntityManager {

  protected HistoricTaskInstanceDataManager historicTaskInstanceDataManager;
  
  public HistoricTaskInstanceEntityManagerImpl(ProcessEngineConfigurationImpl processEngineConfiguration, HistoricTaskInstanceDataManager historicTaskInstanceDataManager) {
    super(processEngineConfiguration);
    this.historicTaskInstanceDataManager = historicTaskInstanceDataManager;
  }
  
  @Override
  protected DataManager<HistoricTaskInstanceEntity> getDataManager() {
      return historicTaskInstanceDataManager;
  }
  
  @Override
  public HistoricTaskInstanceEntity create(TaskEntity task, ExecutionEntity execution) {
    return historicTaskInstanceDataManager.create(task, execution);
  }
  
  @Override
  public void deleteHistoricTaskInstancesByProcessInstanceId(String processInstanceId) {
    if (getHistoryManager().isHistoryLevelAtLeast(HistoryLevel.AUDIT)) {
      List<HistoricTaskInstanceEntity> taskInstances = historicTaskInstanceDataManager.findHistoricTaskInstanceByProcessInstanceId(processInstanceId); 
      for (HistoricTaskInstanceEntity historicTaskInstanceEntity : taskInstances) {
        delete(historicTaskInstanceEntity.getId()); // Needs to be by id (since that method is overridden, see below !)
      }
    }
  }

  @Override
  public long findHistoricTaskInstanceCountByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
    if (getHistoryManager().isHistoryEnabled()) {
      return historicTaskInstanceDataManager.findHistoricTaskInstanceCountByQueryCriteria(historicTaskInstanceQuery);
    }
    return 0;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<HistoricTaskInstance> findHistoricTaskInstancesByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
    if (getHistoryManager().isHistoryEnabled()) {
      return historicTaskInstanceDataManager.findHistoricTaskInstancesByQueryCriteria(historicTaskInstanceQuery);
    }
    return Collections.EMPTY_LIST;
  }

  @Override
  @SuppressWarnings("unchecked")
  public List<HistoricTaskInstance> findHistoricTaskInstancesAndVariablesByQueryCriteria(HistoricTaskInstanceQueryImpl historicTaskInstanceQuery) {
    if (getHistoryManager().isHistoryEnabled()) {
     return historicTaskInstanceDataManager.findHistoricTaskInstancesAndVariablesByQueryCriteria(historicTaskInstanceQuery);
    }
    return Collections.EMPTY_LIST;
  }

  @Override
  public void delete(String id) {
    if (getHistoryManager().isHistoryEnabled()) {
      HistoricTaskInstanceEntity historicTaskInstance = findById(id);
      if (historicTaskInstance != null) {
        
        if (historicTaskInstance.getProcessDefinitionId() != null 
            && Flowable5Util.isFlowable5ProcessDefinitionId(getCommandContext(), historicTaskInstance.getProcessDefinitionId())) {
          Flowable5CompatibilityHandler compatibilityHandler = Flowable5Util.getFlowable5CompatibilityHandler(); 
          compatibilityHandler.deleteHistoricTask(id);
          return;
        }
        
        List<HistoricTaskInstanceEntity> subTasks = historicTaskInstanceDataManager.findHistoricTasksByParentTaskId(historicTaskInstance.getId());
        for (HistoricTaskInstance subTask: subTasks) {
          delete(subTask.getId());
        }

        getHistoricDetailEntityManager().deleteHistoricDetailsByTaskId(id);
        getHistoricVariableInstanceEntityManager().deleteHistoricVariableInstancesByTaskId(id);
        getCommentEntityManager().deleteCommentsByTaskId(id);
        getAttachmentEntityManager().deleteAttachmentsByTaskId(id);
        getHistoricIdentityLinkEntityManager().deleteHistoricIdentityLinksByTaskId(id);
        
        delete(historicTaskInstance);
      }
    }
  }

  @Override
  public List<HistoricTaskInstance> findHistoricTaskInstancesByNativeQuery(Map<String, Object> parameterMap, int firstResult, int maxResults) {
    return historicTaskInstanceDataManager.findHistoricTaskInstancesByNativeQuery(parameterMap, firstResult, maxResults);
  }

  @Override
  public long findHistoricTaskInstanceCountByNativeQuery(Map<String, Object> parameterMap) {
    return historicTaskInstanceDataManager.findHistoricTaskInstanceCountByNativeQuery(parameterMap);
  }

  public HistoricTaskInstanceDataManager getHistoricTaskInstanceDataManager() {
    return historicTaskInstanceDataManager;
  }

  public void setHistoricTaskInstanceDataManager(HistoricTaskInstanceDataManager historicTaskInstanceDataManager) {
    this.historicTaskInstanceDataManager = historicTaskInstanceDataManager;
  }
  
}
