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

import { reactive, ref, unref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getKerberosStartupState } from '@/service/modules/data-source'
import type { FormRules } from 'naive-ui'
import type {
  IDataSourceDetail,
  IDataBase,
  IDataBaseOption,
  IDataBaseOptionKeys,
  IDataSource
} from './types'
import utils from '@/utils'

// ===================== 常量抽离 - 便于维护 =====================
/** 需显示验证模式的数据源类型 */
const MODE_REQUIRED_TYPES = ['AZURESQL', 'REDSHIFT', 'SAGEMAKER'] as const
/** 特殊处理的数据源类型（无数据库名/JDBC参数等） */
const SPECIAL_TYPES = ['SSH', 'ZEPPELIN', 'SAGEMAKER', 'DELEGATE'] as const
/** 需显示AWS区域的数据源类型 */
const AWS_REGION_TYPES = ['ATHENA', 'SAGEMAKER'] as const
/** 无需数据库名必填的数据源类型 */
const NO_REQUIRED_DB_TYPES = ['POSTGRESQL', 'ATHENA'] as const

/** 表单初始值 */
const getInitialValues = (defaultType: IDataBase = 'MYSQL'): IDataSourceDetail => ({
  type: defaultType,
  label: defaultType,
  name: '',
  note: '',
  host: '',
  port: datasourceType[defaultType].defaultPort,
  principal: '',
  javaSecurityKrb5Conf: '',
  loginUserKeytabUsername: '',
  loginUserKeytabPath: '',
  mode: '',
  userName: '',
  password: '',
  database: '',
  connectType: '',
  other: '',
  endpoint: '',
  MSIClientId: '',
  dbUser: '',
  datawarehouse: ''
})

/** 显隐控制默认值 */
const DEFAULT_VISIBILITY_STATE = {
  requiredDataBase: true,
  showHost: true,
  showPort: true,
  showUserName: true,
  showPassword: true,
  showRealDatasource: false,
  showAwsRegion: false,
  showRestEndpoint: false,
  showCompatibleMode: false,
  showConnectType: false,
  showPrincipal: false,
  showMode: false,
  showDataBaseName: true,
  showJDBCConnectParameters: true,
  showPublicKey: false
}

export function useForm(id?: number) {
  const { t } = useI18n()
  const defaultType = 'MYSQL' as IDataBase

  // ===================== 响应式状态管理 =====================
  const state = reactive({
    // 表单引用
    detailFormRef: ref(),
    // 表单数据
    detailForm: getInitialValues(defaultType),
    // 显隐控制（初始化为默认值）
    ...DEFAULT_VISIBILITY_STATE,
    // 表单校验规则
    rules: getFormRules(t, () => state) as FormRules,
    // 下拉选项配置
    modeOptions: [
      { label: 'SqlPassword', value: 'SqlPassword' },
      { label: 'ActiveDirectoryPassword', value: 'ActiveDirectoryPassword' },
      { label: 'ActiveDirectoryMSI', value: 'ActiveDirectoryMSI' },
      { label: 'ActiveDirectoryServicePrincipal', value: 'ActiveDirectoryServicePrincipal' },
      { label: 'accessToken', value: 'accessToken' }
    ],
    redShiftModeOptions: [
      { label: 'password', value: 'password' },
      { label: 'IAM-accessKey', value: 'IAM-accessKey' }
    ],
    sagemakerModeOption: [
      { label: 'IAM-accessKey', value: 'IAM-accessKey' }
    ]
  })

  // ===================== 核心方法 =====================
  /**
   * 切换数据源类型时更新显隐状态和表单值
   * @param type 数据源类型
   * @param options 数据源配置项
   */
  const changeType = async (type: IDataBase, options: IDataBaseOption) => {

    // 1. 重置显隐状态到默认值（避免状态残留）
    Object.assign(state, DEFAULT_VISIBILITY_STATE)

    // 2. 基础表单值更新
    state.detailForm.port = options.previousPort || options.defaultPort
    state.detailForm.type = type

    // 3. 通用显隐规则
    state.requiredDataBase = !NO_REQUIRED_DB_TYPES.includes(type)
    state.showHost = type !== 'ATHENA'
    state.showPort = type !== 'ATHENA'
    state.showAwsRegion = AWS_REGION_TYPES.includes(type)
    state.showMode = MODE_REQUIRED_TYPES.includes(type)
    state.showRealDatasource = type === 'DELEGATE'
    state.showRestEndpoint = type === 'ZEPPELIN'
    state.showConnectType = type === 'ORACLE'
    state.showCompatibleMode = type === 'OCEANBASE'

    // 4. Oracle 特殊处理（默认值）
    if (type === 'ORACLE' && !unref(id)) {
      state.detailForm.connectType = 'ORACLE_SERVICE_NAME'
    }

    // 5. Kerberos 相关（HIVE/SPARK）
    state.showPrincipal = (type === 'HIVE' || type === 'SPARK') 
      ? await getKerberosStartupState() 
      : false

    // 6. 特殊类型单独处理
    handleSpecialTypeVisibility(type)
  }

  /**
   * 处理特殊数据源类型的显隐逻辑
   * @param type 数据源类型
   */
  const handleSpecialTypeVisibility = (type: IDataBase) => {
    if (!SPECIAL_TYPES.includes(type)) return

    // 特殊类型通用隐藏项
    state.showDataBaseName = false
    state.requiredDataBase = false
    state.showJDBCConnectParameters = false
    state.showPublicKey = false

    // 各特殊类型个性化配置
    const typeConfig = {
      SSH: () => {
        state.showPublicKey = true
      },
      ZEPPELIN: () => {
        state.showHost = false
        state.showPort = false
      },
      SAGEMAKER: () => {
        state.showHost = false
        state.showPort = false
      },
      DELEGATE: () => {
        state.showHost = false
        state.showPort = false
        state.showUserName = false
        state.showPassword = false
      }
    }

    typeConfig[type]?.()
  }

  /**
   * 更新端口缓存
   */
  const changePort = () => {
    const type = state.detailForm.type
    if (!type) return
    
    const currentOption = datasourceType[type]
    if (currentOption) {
      currentOption.previousPort = state.detailForm.port
    }
  }

  /**
   * 重置表单值到初始状态
   */
  const resetFieldsValue = () => {
    state.detailForm = getInitialValues(defaultType)
  }

  /**
   * 设置表单值
   * @param values 数据源详情
   */
  const setFieldsValue = (values: IDataSource) => {
    state.detailForm = {
      ...state.detailForm,
      ...values,
      other: values.other ? JSON.stringify(values.other) : values.other
    }
  }

  /**
   * 获取当前表单值
   */
  const getFieldsValue = () => ({ ...state.detailForm })

  // ===================== 返回暴露的状态和方法 =====================
  return {
    state,
    changeType,
    changePort,
    resetFieldsValue,
    setFieldsValue,
    getFieldsValue
  }
}

// ===================== 工具函数（纯函数，无副作用） =====================
/**
 * 获取表单校验规则
 * @param t 国际化函数
 * @param getState 获取当前状态的函数
 */
function getFormRules(t: (...args: any[]) => string, getState: () => any) {
  return {
    name: {
      trigger: ['input'],
      validator() {
        const { detailForm } = getState()
        if (!detailForm.name) {
          return new Error(t('datasource.datasource_name_tips'))
        }
      }
    },
    host: {
      trigger: ['input'],
      validator() {
        const { detailForm, showHost } = getState()
        if (!detailForm.host && showHost) {
          return new Error(t('datasource.ip_tips'))
        }
      }
    },
    port: {
      trigger: ['input'],
      validator() {
        const { detailForm, showPort, showMode } = getState()
        if (showMode && detailForm.mode === 'IAM-accessKey') return
        if (!detailForm.port && showPort) {
          return new Error(t('datasource.port_tips'))
        }
      }
    },
    principal: {
      trigger: ['input'],
      validator() {
        const { detailForm, showPrincipal } = getState()
        if (!detailForm.principal && showPrincipal) {
          return new Error(t('datasource.principal_tips'))
        }
      }
    },
    mode: {
      trigger: ['blur'],
      validator() {
        const { detailForm, showMode } = getState()
        if (!detailForm.mode && showMode) {
          return new Error(t('datasource.mode_tips'))
        }
      }
    },
    userName: {
      trigger: ['input'],
      validator() {
        const { detailForm } = getState()
        if (
          !detailForm.userName &&
          detailForm.type !== 'AZURESQL' && detailForm.type !== 'DELEGATE'
        ) {
          return new Error(t('datasource.user_name_tips'))
        }
      }
    },
    awsRegion: {
      trigger: ['input'],
      validator() {
        const { detailForm, showAwsRegion } = getState()
        if (!detailForm.awsRegion && showAwsRegion) {
          return new Error(t('datasource.aws_region_tips'))
        }
      }
    },
    database: {
      trigger: ['input'],
      validator() {
        const { detailForm, requiredDataBase } = getState()
        if (!detailForm.database && requiredDataBase) {
          return new Error(t('datasource.database_name_tips'))
        }
      }
    },
    datawarehouse: {
      trigger: ['input'],
      validator() {
        const { detailForm } = getState()
        if (!detailForm.datawarehouse) {
          return new Error(t('datasource.datawarehouse_tips'))
        }
      }
    },
    connectType: {
      trigger: ['update'],
      validator() {
        const { detailForm, showConnectType } = getState()
        if (!detailForm.connectType && showConnectType) {
          return new Error(t('datasource.oracle_connect_type_tips'))
        }
      }
    },
    other: {
      trigger: ['input', 'blur'],
      validator() {
        const { detailForm } = getState()
        if (detailForm.other && !utils.isJson(detailForm.other)) {
          return new Error(t('datasource.jdbc_format_tips'))
        }
      }
    },
    endpoint: {
      trigger: ['input'],
      validator() {
        const { detailForm } = getState()
        if (
          !detailForm.endpoint &&
          detailForm.type === 'AZURESQL' &&
          detailForm.mode === 'accessToken'
        ) {
          return new Error(t('datasource.endpoint_tips'))
        }
      }
    },
    dbUser: {
      trigger: ['input'],
      validator() {
        const { detailForm, showMode } = getState()
        if (
          !detailForm.dbUser &&
          showMode &&
          detailForm.mode === 'IAM-accessKey' &&
          detailForm.type !== 'SAGEMAKER'
        ) {
          return new Error(t('datasource.IAM-accessKey'))
        }
      }
    }
  }
}

// ===================== 数据源类型配置（保持原有结构） =====================
export const datasourceType: IDataBaseOptionKeys = {
  MYSQL: { value: 'MYSQL', label: 'MYSQL', defaultPort: 3306 },
  POSTGRESQL: { value: 'POSTGRESQL', label: 'POSTGRESQL', defaultPort: 5432 },
  HIVE: { value: 'HIVE', label: 'HIVE/IMPALA', defaultPort: 10000 },
  KYUUBI: { value: 'KYUUBI', label: 'KYUUBI', defaultPort: 10000 },
  SPARK: { value: 'SPARK', label: 'SPARK', defaultPort: 10015 },
  CLICKHOUSE: { value: 'CLICKHOUSE', label: 'CLICKHOUSE', defaultPort: 8123 },
  ORACLE: { value: 'ORACLE', label: 'ORACLE', defaultPort: 1521 },
  SQLSERVER: { value: 'SQLSERVER', label: 'SQLSERVER', defaultPort: 1433 },
  DB2: { value: 'DB2', label: 'DB2', defaultPort: 50000 },
  VERTICA: { value: 'VERTICA', label: 'VERTICA', defaultPort: 5433 },
  PRESTO: { value: 'PRESTO', label: 'PRESTO', defaultPort: 8080 },
  REDSHIFT: { value: 'REDSHIFT', label: 'REDSHIFT', defaultPort: 5439 },
  ATHENA: { value: 'ATHENA', label: 'ATHENA', defaultPort: 0 },
  TRINO: { value: 'TRINO', label: 'TRINO', defaultPort: 8080 },
  AZURESQL: { value: 'AZURESQL', label: 'AZURESQL', defaultPort: 1433 },
  STARROCKS: { value: 'STARROCKS', label: 'STARROCKS', defaultPort: 9030 },
  DAMENG: { value: 'DAMENG', label: 'DAMENG', defaultPort: 5236 },
  OCEANBASE: { value: 'OCEANBASE', label: 'OCEANBASE', defaultPort: 2881 },
  SNOWFLAKE: { value: 'SNOWFLAKE', label: 'SNOWFLAKE', defaultPort: 3306 },
  SSH: { value: 'SSH', label: 'SSH', defaultPort: 22 },
  DATABEND: { value: 'DATABEND', label: 'DATABEND', defaultPort: 8000 },
  HANA: { value: 'HANA', label: 'HANA', defaultPort: 30015 },
  ZEPPELIN: { value: 'ZEPPELIN', label: 'ZEPPELIN', defaultPort: 8080 },
  DORIS: { value: 'DORIS', label: 'DORIS', defaultPort: 9030 },
  SAGEMAKER: { value: 'SAGEMAKER', label: 'SAGEMAKER', defaultPort: 0 },
  DELEGATE: { value: 'DELEGATE', label: 'DELEGATE', defaultPort: 0 }
}

/** 数据源类型列表（用于下拉选择） */
export const datasourceTypeList = Object.values(datasourceType).map(item => ({
  ...item,
  class: 'options-datasource-type'
}))