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

package org.apache.dolphinscheduler.plugin.task.sqoop.generator;

import static org.apache.dolphinscheduler.plugin.task.api.TaskConstants.*;
import static org.apache.dolphinscheduler.plugin.task.sqoop.SqoopConstants.*;

import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.sqoop.SqoopConstants;
import org.apache.dolphinscheduler.plugin.task.sqoop.parameter.SqoopParameters;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * common script generator
 */
@Slf4j
public class CommonGenerator {

    public String generate(SqoopParameters sqoopParameters) {

        StringBuilder commonSb = new StringBuilder();

        try {
            // sqoop task model
            commonSb.append(SqoopConstants.SQOOP)
                    .append(SPACE)
                    .append(sqoopParameters.getModelType());

//            引用jars
            commonSb.append(SPACE)
                    .append(SqoopConstants.LIB_JARS)
                    .append(SPACE)
                    .append(DOUBLE_QUOTES)
                    .append(SqoopConstants.JAR_FILES)
                    .append(DOUBLE_QUOTES);

            // sqoop sqoop.export.records.per.statement
            commonSb.append(SPACE).append(D).append(SPACE)
                    .append(String.format(FORMAT_S_S_S, SqoopConstants.SQOOP_EXPORT_RECORDS_PER_STATEMENT,
                            EQUAL_SIGN, 1));

            // sqoop map-reduce job name
            commonSb.append(SPACE).append(D).append(SPACE)
                    .append(String.format(FORMAT_S_S_S, SqoopConstants.SQOOP_MR_JOB_NAME,
                            EQUAL_SIGN, sqoopParameters.getJobName()));

            // hadoop custom param
            List<Property> hadoopCustomParams = sqoopParameters.getHadoopCustomParams();
            if (CollectionUtils.isNotEmpty(hadoopCustomParams)) {
                for (Property hadoopCustomParam : hadoopCustomParams) {
                    String hadoopCustomParamStr = String.format(FORMAT_S_S_S, hadoopCustomParam.getProp(),
                            EQUAL_SIGN, hadoopCustomParam.getValue());

                    commonSb.append(SPACE).append(D)
                            .append(SPACE).append(hadoopCustomParamStr);
                }
            }

            // sqoop custom params
            List<Property> sqoopAdvancedParams = sqoopParameters.getSqoopAdvancedParams();
            if (CollectionUtils.isNotEmpty(sqoopAdvancedParams)) {
                for (Property sqoopAdvancedParam : sqoopAdvancedParams) {
                    commonSb.append(SPACE).append(sqoopAdvancedParam.getProp())
                            .append(SPACE).append(sqoopAdvancedParam.getValue());
                }
            }

            // sqoop parallelism
            if (sqoopParameters.getConcurrency() > 0) {
                commonSb.append(SPACE).append(SqoopConstants.SQOOP_PARALLELISM)
                        .append(SPACE).append(sqoopParameters.getConcurrency());
                if (sqoopParameters.getConcurrency() > 1) {
                    commonSb.append(SPACE).append(SqoopConstants.SPLIT_BY)
                            .append(SPACE).append(sqoopParameters.getSplitBy());
                }
            }
        } catch (Exception e) {
            log.error(String.format("Sqoop task general param build failed: [%s]", e.getMessage()));
        }

        return commonSb.toString();
    }

    public String codeGenerate(SqoopParameters sqoopParameters) {

        StringBuilder commonSb = new StringBuilder();

        try {
//            定义路径及创建路径
            commonSb.append(SqoopConstants.PREFIX_OPER)
                    .append(LINE_SEPARATOR)
                    .append(SqoopConstants.MKDIR_OPER)
                    .append(LINE_SEPARATOR);

            // sqoop task model
            commonSb.append(SqoopConstants.SQOOP)
                    .append(SPACE)
                    .append(SqoopConstants.CODE_GEN);

            commonSb.append(SPACE)
                    .append(SqoopConstants.BIN_DIR)
                    .append(SPACE)
                    .append(DOUBLE_QUOTES)
                    .append(DOLLAR_CHAR)
                    .append(SqoopConstants.SQOOP_BIN_DIR)
                    .append(DOUBLE_QUOTES)
                    .append(SPACE);
        } catch (Exception e) {
            log.error(String.format("Sqoop task general param build failed: [%s]", e.getMessage()));
        }
        return commonSb.toString();
    }

    public String generateJarPath() {
        StringBuilder commonSb = new StringBuilder();
        try {
            commonSb.append(FIND_OPER).append(LINE_SEPARATOR);
        } catch (Exception e) {
            log.error(String.format("Sqoop task general param build failed: [%s]", e.getMessage()));
        }
        return commonSb.toString();
    }
}
