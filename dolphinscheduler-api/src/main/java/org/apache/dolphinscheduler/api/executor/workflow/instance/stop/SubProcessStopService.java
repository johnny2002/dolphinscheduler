package org.apache.dolphinscheduler.api.executor.workflow.instance.stop;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.enums.CommandType;
import org.apache.dolphinscheduler.common.enums.WorkflowExecutionStatus;
import org.apache.dolphinscheduler.dao.entity.ProcessInstance;
import org.apache.dolphinscheduler.dao.repository.ProcessInstanceDao;
import org.apache.dolphinscheduler.extract.base.client.SingletonJdkDynamicRpcClientProxyFactory;
import org.apache.dolphinscheduler.extract.master.ITaskInstanceExecutionEventListener;
import org.apache.dolphinscheduler.extract.master.transportor.WorkflowInstanceStateChangeEvent;
import org.apache.dolphinscheduler.service.process.ProcessService;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class SubProcessStopService {

    private final ProcessService processService;
    private final ProcessInstanceDao processInstanceDao;

    public SubProcessStopService(ProcessService processService, ProcessInstanceDao processInstanceDao) {
        this.processService = processService;
        this.processInstanceDao = processInstanceDao;
    }

    /**
     * 递归停止所有子流程
     */
    public void stopAllSubProcesses(Integer parentProcessId) {
        stopSubProcessesRecursively(parentProcessId, 0);
    }

    private void stopSubProcessesRecursively(Integer parentProcessId, int depth) {
        String indent = StringUtils.repeat("  ", depth);

        List<ProcessInstance> subProcesses = processService.queryRunningSubProcessByParentId(parentProcessId);

        if (subProcesses.isEmpty()) {
            log.debug("{}No sub-processes found for parent {}", indent, parentProcessId);
            return;
        }

        log.info("{}Found {} sub-processes for parent {}", indent, subProcesses.size(), parentProcessId);

        for (ProcessInstance subProcess : subProcesses) {
            if (subProcess.getState().isRunning()) {
                try {
                    log.info("{}Stopping sub-process [{}] (ID: {})",
                            indent, subProcess.getName(), subProcess.getId());

                    // 先递归停止子流程的子流程
                    stopSubProcessesRecursively(subProcess.getId(), depth + 1);

                    // 停止当前子流程
                    stopSubProcess(subProcess);

                } catch (Exception e) {
                    log.error("{}Failed to stop sub-process {}", indent, subProcess.getName(), e);
                }
            }
        }
    }

    private void stopSubProcess(ProcessInstance subProcess) {
        try {
            // 设置状态
            subProcess.setCommandType(CommandType.STOP);
            subProcess.addHistoryCmd(CommandType.STOP);
            subProcess.setStateWithDesc(WorkflowExecutionStatus.READY_STOP,
                    "Stopped by parent process");

            // 更新数据库
            if (processInstanceDao.updateById(subProcess)) {
                // 发送停止事件到Master
                sendStopEventToMaster(subProcess);
                log.info("Successfully stopped sub-process [{}]", subProcess.getName());
            } else {
                log.error("Failed to update sub-process status: {}", subProcess.getName());
            }
        } catch (Exception e) {
            log.error("Exception while stopping sub-process {}", subProcess.getName(), e);
            throw e;
        }
    }

    private void sendStopEventToMaster(ProcessInstance processInstance) {
        try {
            ITaskInstanceExecutionEventListener eventListener =
                    SingletonJdkDynamicRpcClientProxyFactory.getProxyClient(processInstance.getHost(),
                            ITaskInstanceExecutionEventListener.class);
            eventListener.onWorkflowInstanceInstanceStateChange(
                    new WorkflowInstanceStateChangeEvent(
                            processInstance.getId(), 0, processInstance.getState(),
                            processInstance.getId(), 0));
        } catch (Exception e) {
            log.error("Failed to send stop event to master for process {}",
                    processInstance.getName(), e);
            throw new RuntimeException("Failed to send stop event to master", e);
        }
    }
}
