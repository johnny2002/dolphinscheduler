/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.plugin.datasource.delegate.param;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.ApplicationContextUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.DataSource;
import org.apache.dolphinscheduler.dao.mapper.DataSourceMapper;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.AbstractDataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.DataSourceProcessor;
import org.apache.dolphinscheduler.plugin.datasource.api.plugin.DataSourceProcessorProvider;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.springframework.beans.factory.BeanInitializationException;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;


@AutoService(DataSourceProcessor.class)
@Slf4j
public class DelegateDataSourceProcessor extends AbstractDataSourceProcessor {

    public DelegateDataSourceProcessor() {
        log.info("DelegateDataSourceProcessor created");
    }

    @Override
    public BaseDataSourceParamDTO castDatasourceParamDTO(String paramJson) {
        return JSONUtils.parseObject(paramJson, DelegateDataSourceParamDTO.class);
    }

    @Override
    public BaseDataSourceParamDTO createDatasourceParamDTO(String connectionJson) {
        return JSONUtils.parseObject(connectionJson, DelegateDataSourceParamDTO.class);
    }

    @Override
    public BaseConnectionParam createConnectionParams(BaseDataSourceParamDTO dataSourceParam) {
        DelegateDataSourceParamDTO datasourceParam = (DelegateDataSourceParamDTO) dataSourceParam;
        String realDatasource = datasourceParam.getRealDatasource();
        return toRealConnectionParam(realDatasource);
    }

    private BaseConnectionParam toRealConnectionParam(String realDatasource) {
        DataSourceMapper dataSourceMapper = ApplicationContextUtils.getBean(DataSourceMapper.class);
        if (dataSourceMapper == null) {
            throw new BeanInitializationException("Spring context is not initialized");
        }

        String datasourceName = this.resolveRealDatasourceName(realDatasource);

        log.warn("Don't use DelegateDataSourceProcessor to get a real datasource.");
        log.warn("DelegateDataSourceProcessor#createConnectionParams is Deprecated. Please use DelegateDataSourcePreTaskCreationHandler to replace datasource info during Task Instance initialization.");

        List<DataSource> dataSources = dataSourceMapper.queryDataSourceByName(datasourceName);
        if (dataSources == null || dataSources.isEmpty()) {
            throw new RuntimeException("datasource not found:" + datasourceName);
        } else if (dataSources.size() > 1) {
            throw new RuntimeException("need one datasource for:" + datasourceName + ", while got:" + datasourceName);
        }
        DataSource dataSource = dataSources.get(0);

        BaseConnectionParam baseConnectionParam =
                (BaseConnectionParam) DataSourceUtils.buildConnectionParams(dataSource.getType(),
                        dataSource.getConnectionParams());
        baseConnectionParam.setDbType(dataSource.getType());
        return baseConnectionParam;
    }

    @Override
    public ConnectionParam createConnectionParams(String connectionJson) {
        DelegateConnectionParam delegateConnectionParam =
                JSONUtils.parseObject(connectionJson, DelegateConnectionParam.class);
        return toRealConnectionParam(delegateConnectionParam.getRealDatasource());
    }

    @Override
    public String getDatasourceDriver() {
        return "";
    }

    @Override
    public String getValidationQuery() {
        return null;
    }

    private String resolveRealDatasourceName(String realDatasource) {
        return realDatasource;
    }

    @Override
    public String getJdbcUrl(ConnectionParam connectionParam) {
        if (connectionParam instanceof BaseConnectionParam) {
            return ((BaseConnectionParam) connectionParam).getJdbcUrl();
        }
        throw new RuntimeException("invalid connection parameter");
    }

    @Override
    public Connection getConnection(ConnectionParam connectionParam) throws ClassNotFoundException, SQLException, IOException {
        if (connectionParam instanceof BaseConnectionParam) {
            BaseConnectionParam baseConnectionParam = (BaseConnectionParam) connectionParam;
            DataSourceProcessor dataSourceProcessor =
                    DataSourceProcessorProvider.getDataSourceProcessor(baseConnectionParam.getDbType());
            return dataSourceProcessor.getConnection(connectionParam);
        }
        throw new RuntimeException("invalid connection parameter");
    }

    @Override
    public DbType getDbType() {
        return DbType.DELEGATE;
    }

    @Override
    public DataSourceProcessor create() {
        return new DelegateDataSourceProcessor();
    }

    @Override
    public void checkDatasourceParam(BaseDataSourceParamDTO datasourceParamDTO) {
        DelegateDataSourceParamDTO dlgDataSourceParamDTO = (DelegateDataSourceParamDTO) datasourceParamDTO;
        if (StringUtils.isEmpty(dlgDataSourceParamDTO.getRealDatasource())) {
            throw new IllegalArgumentException("delegate datasource param is not valid");
        }
    }
}
