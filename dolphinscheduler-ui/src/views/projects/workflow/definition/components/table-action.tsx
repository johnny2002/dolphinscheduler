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

import { defineComponent, PropType, toRefs, h } from "vue";
import {
  NSpace,
  NTooltip,
  NButton,
  NIcon,
  NPopconfirm,
  NDropdown,
  useDialog,
} from "naive-ui";
import {
  DeleteOutlined,
  DownloadOutlined,
  FormOutlined,
  InfoCircleFilled,
  PlayCircleOutlined,
  ClockCircleOutlined,
  CopyOutlined,
  ExportOutlined,
  ApartmentOutlined,
  UploadOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  MoreOutlined,
} from "@vicons/antd";
import { useI18n } from "vue-i18n";
import { IDefinitionData } from "../types";

const props = {
  row: {
    type: Object as PropType<IDefinitionData>,
  },
};

export default defineComponent({
  name: "TableAction",
  props,
  emits: [
    "editWorkflow",
    "updateList",
    "startWorkflow",
    "timingWorkflow",
    "versionWorkflow",
    "deleteWorkflow",
    "releaseWorkflow",
    "releaseScheduler",
    "copyWorkflow",
    "exportWorkflow",
    "gotoWorkflowTree",
  ],
  setup(props, ctx) {
    const { t } = useI18n();

    const dialog = useDialog();

    const handleEditWorkflow = () => {
      ctx.emit("editWorkflow");
    };

    const handleStartWorkflow = () => {
      ctx.emit("startWorkflow");
    };

    const handleTimingWorkflow = () => {
      ctx.emit("timingWorkflow");
    };

    const handleVersionWorkflow = () => {
      ctx.emit("versionWorkflow");
    };

    const handleDeleteWorkflow = () => {
      ctx.emit("deleteWorkflow");
    };

    const handleReleaseWorkflow = () => {
      ctx.emit("releaseWorkflow");
    };

    const handleCopyWorkflow = () => {
      ctx.emit("copyWorkflow");
    };

    const handleExportWorkflow = () => {
      ctx.emit("exportWorkflow");
    };

    const handleGotoWorkflowTree = () => {
      ctx.emit("gotoWorkflowTree");
    };

    const handleReleaseScheduler = () => {
      ctx.emit("releaseScheduler");
    };

    const confirmDeleteWorkflow = () => {
      dialog.warning({
        title: t("project.workflow.delete_confirm"),
        content: "",
        positiveText: t("project.workflow.confirm"),
        negativeText: t("project.workflow.delete_cancel"),
        onPositiveClick: () => {
          handleDeleteWorkflow();
        },
        onNegativeClick: () => {
          console.log("用户取消了删除操作");
        },
      });
    };

    // 创建小尺寸的图标组件
    const createSmallIcon = (iconComponent: any, size = "14") => {
      return h(NIcon, { size }, () => h(iconComponent));
    };

    // 下拉菜单选项配置 - 使用自定义渲染缩小图标
    const dropdownOptions = [
      {
        label: () =>
          h(
            "div",
            {
              style: {
                display: "flex",
                alignItems: "center",
                padding: "6px 0",
              },
            },
            [
              createSmallIcon(ClockCircleOutlined),
              h(
                "span",
                {
                  style: {
                    marginLeft: "8px",
                    fontSize: "13px",
                  },
                },
                t("project.workflow.timing")
              ),
            ]
          ),
        key: "timingWorkflow",
        type: "default" as const, // 添加这一行
        props: {}, // 添加空props
      },
      {
        label: () => {
          const scheduleReleaseState = props.row?.scheduleReleaseState;
          const isDisabled = !props.row?.schedule || props.row?.releaseState !== "ONLINE";

          return h(
            "div",
            {
              style: {
                display: "flex",
                alignItems: "center",
                padding: "6px 0",
                opacity: isDisabled ? 0.5 : 1,
              },
            },
            [
              createSmallIcon(
                scheduleReleaseState === "ONLINE" ? ArrowDownOutlined : ArrowUpOutlined
              ),
              h(
                "span",
                {
                  style: {
                    marginLeft: "8px",
                    fontSize: "13px",
                  },
                },
                scheduleReleaseState === "ONLINE"
                  ? t("project.workflow.time_down_line")
                  : t("project.workflow.time_up_line")
              ),
            ]
          );
        },
        key: "releaseScheduler",
        type: "default" as const, // 添加这一行
        props: {
          disabled: !props.row?.schedule || props.row?.releaseState !== "ONLINE",
        },
      },
      {
        type: "divider",
        key: "divider1",
      },
      {
        label: () =>
          h(
            "div",
            {
              style: {
                display: "flex",
                alignItems: "center",
                padding: "6px 0",
              },
            },
            [
              createSmallIcon(CopyOutlined),
              h(
                "span",
                {
                  style: {
                    marginLeft: "8px",
                    fontSize: "13px",
                  },
                },
                t("project.workflow.copy_workflow")
              ),
            ]
          ),
        key: "copyWorkflow",
        type: "default" as const, // 添加这一行
        props: {}, 
      },
      {
        label: () => {
          const isDeleteDisabled = props.row?.releaseState === "ONLINE";
          return h(
            "div",
            {
              style: {
                display: "flex",
                alignItems: "center",
                padding: "6px 0",
                opacity: isDeleteDisabled ? 0.5 : 1,
              },
            },
            [
              createSmallIcon(DeleteOutlined),
              h(
                "span",
                {
                  style: {
                    marginLeft: "8px",
                    fontSize: "13px",
                    color: isDeleteDisabled ? "#ccc" : "#f56c6c",
                  },
                },
                t("project.workflow.delete")
              ),
            ]
          );
        },
        key: "deleteWorkflow",
        props: {
          disabled: props.row?.releaseState === "ONLINE",
        },
        type: "default" as const, // 添加这一行
      },
      {
        type: "divider",
        key: "divider2",
      },
      {
        label: () =>
          h(
            "div",
            {
              style: {
                display: "flex",
                alignItems: "center",
                padding: "6px 0",
              },
            },
            [
              createSmallIcon(ApartmentOutlined),
              h(
                "span",
                {
                  style: {
                    marginLeft: "8px",
                    fontSize: "13px",
                  },
                },
                t("project.workflow.tree_view")
              ),
            ]
          ),
        key: "gotoWorkflowTree",
        type: "default" as const, // 添加这一行
        props: {}, // 添加空props
      },
      {
        label: () =>
          h(
            "div",
            {
              style: {
                display: "flex",
                alignItems: "center",
                padding: "6px 0",
              },
            },
            [
              createSmallIcon(ExportOutlined),
              h(
                "span",
                {
                  style: {
                    marginLeft: "8px",
                    fontSize: "13px",
                  },
                },
                t("project.workflow.export")
              ),
            ]
          ),
        key: "exportWorkflow",
        type: "default" as const, // 添加这一行
        props: {}, // 添加空props
      },
      {
        label: () =>
          h(
            "div",
            {
              style: {
                display: "flex",
                alignItems: "center",
                padding: "6px 0",
              },
            },
            [
              createSmallIcon(InfoCircleFilled),
              h(
                "span",
                {
                  style: {
                    marginLeft: "8px",
                    fontSize: "13px",
                  },
                },
                t("project.workflow.version_info")
              ),
            ]
          ),
        key: "versionWorkflow",
        type: "default" as const, // 添加这一行
        props: {}, // 添加空props
      },
    ];

    const handleDropdownSelect = (key: string) => {
      switch (key) {
        case "timingWorkflow":
          handleTimingWorkflow();
          break;
        case "releaseScheduler":
          handleReleaseScheduler();
          break;
        case "copyWorkflow":
          handleCopyWorkflow();
          break;
        case "deleteWorkflow":
          if (props.row?.releaseState !== "ONLINE") {
            confirmDeleteWorkflow();
          }
          break;
        case "gotoWorkflowTree":
          handleGotoWorkflowTree();
          break;
        case "exportWorkflow":
          handleExportWorkflow();
          break;
        case "versionWorkflow":
          handleVersionWorkflow();
          break;
      }
    };

    return {
      t,
      handleEditWorkflow,
      handleStartWorkflow,
      handleTimingWorkflow,
      handleVersionWorkflow,
      handleDeleteWorkflow,
      handleReleaseWorkflow,
      handleCopyWorkflow,
      handleExportWorkflow,
      handleGotoWorkflowTree,
      handleReleaseScheduler,
      confirmDeleteWorkflow,
      handleDropdownSelect,
      dropdownOptions,
      createSmallIcon,
      ...toRefs(props),
    };
  },
  render() {
    const releaseState = this.row?.releaseState;

    return (
      <NSpace>
        {/* 保留编辑按钮 */}
        <NTooltip trigger={"hover"}>
          {{
            default: () => this.t("project.workflow.edit"),
            trigger: () => (
              <NButton
                size="small"
                type="info"
                tag="div"
                circle
                onClick={this.handleEditWorkflow}
                disabled={releaseState === "ONLINE"}
                class="btn-edit"
              >
                <NIcon size="16">
                  <FormOutlined />
                </NIcon>
              </NButton>
            ),
          }}
        </NTooltip>

        {/* 保留运行按钮 */}
        <NTooltip trigger={"hover"}>
          {{
            default: () => this.t("project.workflow.start"),
            trigger: () => (
              <NButton
                size="small"
                type="primary"
                tag="div"
                circle
                onClick={this.handleStartWorkflow}
                disabled={releaseState === "OFFLINE"}
                class="btn-run"
              >
                <NIcon size="16">
                  <PlayCircleOutlined />
                </NIcon>
              </NButton>
            ),
          }}
        </NTooltip>

        {/* 保留上线/下线按钮 */}
        <NTooltip trigger={"hover"}>
          {{
            default: () =>
              releaseState === "ONLINE"
                ? this.t("project.workflow.down_line")
                : this.t("project.workflow.up_line"),
            trigger: () => (
              <NPopconfirm onPositiveClick={this.handleReleaseWorkflow}>
                {{
                  default: () =>
                    releaseState === "OFFLINE"
                      ? this.t("project.workflow.confirm_to_online")
                      : this.t("project.workflow.confirm_to_offline"),
                  trigger: () => (
                    <NButton
                      size="small"
                      type={releaseState === "ONLINE" ? "warning" : "error"}
                      tag="div"
                      circle
                      class="btn-publish"
                    >
                      <NIcon size="16">
                        {releaseState === "ONLINE" ? (
                          <DownloadOutlined />
                        ) : (
                          <UploadOutlined />
                        )}
                      </NIcon>
                    </NButton>
                  ),
                }}
              </NPopconfirm>
            ),
          }}
        </NTooltip>

        {/* 更多操作下拉菜单 */}
        <NDropdown
          trigger="hover"
          placement="bottom-end"
          options={this.dropdownOptions as any}
          onSelect={this.handleDropdownSelect}
          show-arrow
        >
          <NButton size="small" type="info" tag="div" circle class="btn-more">
            <NIcon size="16">
              <MoreOutlined />
            </NIcon>
          </NButton>
        </NDropdown>
      </NSpace>
    );
  },
});
