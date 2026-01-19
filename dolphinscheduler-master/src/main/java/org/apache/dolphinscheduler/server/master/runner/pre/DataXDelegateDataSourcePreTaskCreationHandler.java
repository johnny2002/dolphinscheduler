package org.apache.dolphinscheduler.server.master.runner.pre;

import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.dao.entity.TaskInstance;
import org.apache.dolphinscheduler.plugin.task.datax.DataxParameters;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

@Component
public class DataXDelegateDataSourcePreTaskCreationHandler extends AbstractDelegateDataSourcePreTaskCreationHandler {

    @Override
    public boolean isSupport(TaskInstance taskInstance) {
        return Objects.equals(taskInstance.getTaskType(), "DATAX");
    }

    @Override
    protected String rebuildTaskParams(TaskInstance taskInstance, Map<String, Object> env) {
        DataxParameters taskParams = JSONUtils.parseObject(taskInstance.getTaskParams(), DataxParameters.class);
        if (taskParams == null || taskParams.getDsType() == null) {
            return taskInstance.getTaskParams();
        }
        if (Objects.equals(taskParams.getDsType(), "DELEGATE")) {
            DataSource dataSource = this.getDelegateDatasource(taskParams.getDataSource(), env);
            taskParams.setDataSource(dataSource.getId());
            taskParams.setDsType(dataSource.getType().name());
        }
        if (Objects.equals(taskParams.getDtType(), "DELEGATE")) {
            DataSource dataSource = this.getDelegateDatasource(taskParams.getDataTarget(), env);
            taskParams.setDataTarget(dataSource.getId());
            taskParams.setDtType(dataSource.getType().name());
        }
        if (taskParams.getSql() != null) {
            String sql =  taskParams.getSql();
            sql = resolveSpel(sql, env);
            taskParams.setSql(sql);
        }
        return JSONUtils.toJsonString(taskParams);
    }

}
