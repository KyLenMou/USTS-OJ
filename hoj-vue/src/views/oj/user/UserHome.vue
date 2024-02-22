<template>
  <div class="profile-container" v-loading="loading">
    <el-row :gutter="15">
      <el-col :span="24">
        <el-card>
          <el-row style="display: flex">
            <div style="flex: 0 0 auto">
              <div class="flex-display">
                <!--头像-->
                <span>
<!--                  <el-avatar shape="square" :size="120" :src="flex-display.avatar"></el-avatar>-->
                  <el-avatar shape="square" :size="120"
                             src="https://assets.leetcode.cn/aliyun-lc-upload/users/kylen-n/avatar_1636807022.png?x-oss-process=image%2Fformat%2Cwebp"></el-avatar>
                </span>
                <!--基本信息-->
                <span class="profile-emphasis">
                  <!--用户名-->
                  <span class="username">
                    {{ profile.username }}
                    <!--性别-->
                    <span class="gender-male male" v-if="profile.gender == 'male'">
                      <i class="fa fa-mars"></i>
                    </span>
                    <span class="gender-male female" v-else-if="profile.gender == 'female'">
                      <i class="fa fa-venus"></i>
                    </span>
                  </span>
                  <br>
                  <!--昵称-->
                  <span class="nickname" v-if="profile.nickname">
                    {{ profile.nickname }}
                  </span>
                  <br><br>
                  <!--头衔-->
                  <span v-if="profile.titleName">
                    <el-tag effect="dark" size="small" :color="profile.titleColor">
                      {{ profile.titleName }}
                    </el-tag>
                  </span>
                </span>
              </div>
            </div>
            <div style="flex: 1; margin: 0 20px">
              <!--个人简介-->
              <div class="signature-body">
                <Markdown
                    v-if="profile.signature"
                    :isAvoidXss="true"
                    :content="profile.signature">
                </Markdown>
                <div class="markdown-body" v-else>
                  <p>{{ $t('m.Not_set_yet') }}</p>
                </div>
              </div>
            </div>
            <div style="margin-left: auto; text-align: right; line-height: 2;">
              <!--最近上线时间-->
              <el-tooltip
                  :content="profile.recentLoginTime | localtime"
                  placement="top"
              >
                <el-tag type="success" effect="plain" size="medium">
                  <i class="fa fa-circle">
                    {{
                      $t('m.Recent_login_time')
                    }}{{ profile.recentLoginTime | fromNow }}</i
                  >
                </el-tag>
              </el-tooltip>
              <br>
              <!--学校-->
              <span class="default-info" v-if="profile.school">
                <i class="fa fa-graduation-cap" aria-hidden="true"></i>
                {{ profile.school }}
              </span>
              <br>
              <!--Github-->
              <span id="icons">
                <a
                    :href="profile.github"
                    v-if="profile.github"
                    class="icon"
                    target="_blank"
                >
                  <i class="fa fa-github"> {{ $t('m.Github') }}</i>
                </a>
              </span>
              <br>
              <!--博客-->
              <span id="icons">
              <a
                  :href="profile.blog"
                  v-if="profile.blog"
                  class="icon"
                  target="_blank"
              >
                <i class="fa fa-share-alt-square"> {{ $t('m.Blog') }}</i>
              </a>
            </span>
            </div>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="15" class="row-margin-top">
      <el-col :span="5">
        <el-card>
          <el-row>
            <el-col :span="24">
              <!--四个方块-->
              <el-card shadow="always" class="submission">
                <div style="display: flex; align-items: center;">
                  <span>
                    <i class="fa fa-th" aria-hidden="true"></i>
                    {{ $t('m.UserHome_Submissions') }}
                  </span>
                  <span class="data-number">{{ profile.total }}</span>
                </div>
              </el-card>
              <el-card shadow="always" class="solved">
                <div style="display: flex; align-items: center;">
                  <span>
                    <i class="fa fa-check-circle" aria-hidden="true"></i>
                    {{ $t('m.UserHome_Solved') }}
                  </span>
                  <span class="data-number">{{ profile.solvedList.length }}</span>
                </div>
              </el-card>
              <el-card shadow="always" class="score">
                <div style="display: flex; align-items: center;">
                  <span>
                    <i class="fa fa-star" aria-hidden="true"></i>
                    {{ $t('m.UserHome_Score') }}
                  </span>
                  <span class="data-number">{{ getSumScore(profile.scoreList) }}</span>
                </div>
              </el-card>
              <el-card shadow="always" class="rating">
                <div style="display: flex; align-items: center;">
                  <span>
                    <i class="fa fa-user-secret" aria-hidden="true"></i>
                    {{ $t('m.UserHome_Rating') }}
                  </span>
                  <span class="data-number">
                  {{ profile.rating ? profile.rating : '--' }}
                </span>
                </div>
              </el-card>
            </el-col>
          </el-row>
          <el-divider></el-divider>
          <!--能力知识模型-->
          <el-row>
            <el-tooltip
                style="text-align: center;color: #409EFF"
                placement="top"
                effect="light"
                content="各个标签对应的已通过题目在难度和提交通过以及总题数下的占比">
              <h1 style="margin: 0">能力达成度</h1>
            </el-tooltip>
            <div style="width: 100%">
              <ECharts :options="modelOption" style="width: 100%" :style="modelHeight" :autoresize="true"></ECharts>
            </div>
          </el-row>
          <el-divider></el-divider>
          <!--个性化推荐-->
          <el-row style="text-align: center;" v-if="profile.recommendProblems">
            <el-tooltip placement="top" effect="light" content="点击难度按钮到达题目页面">
              <h1 style="margin: 0;color: #409EFF">个性化推荐</h1>
            </el-tooltip>
            <el-table
                :data="profile.recommendProblems"
                :header-cell-style="{'text-align': 'center'}"
                :cell-style="{'text-align':'center', 'padding': '5px 0 !important'}"
                style="margin-top: 10px"
            >
              <el-table-column prop="title" label="题目"></el-table-column>
              <el-table-column label="难度">
                <template slot-scope="scope">
                  <el-button
                      size="mini"
                      :style="utils.getLevelColor(scope.row.difficulty)"
                      style="padding: 5px 10px"
                      @click="goProblem(scope.row.pid)"
                  >
                    {{ utils.getLevelName(scope.row.difficulty) }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-row>
        </el-card>
      </el-col>

      <el-col :span="19">
        <el-row>
          <el-col :span="24">
            <el-card>
              <calendar-heatmap
                  v-if="loadingCalendarHeatmap"
                  :values="calendarHeatmapValue"
                  :end-date="calendarHeatmapEndDate"
                  :tooltipUnit="$t('m.Calendar_Tooltip_Uint')"
                  :locale="calendarHeatLocale"
                  :range-color="['rgb(218, 226, 239)', '#9be9a8', '#40c463', '#30a14e', '#216e39']"
              >
              </calendar-heatmap>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="15" class="row-margin-top">
          <el-col :span="14">
            <!--标签难度饼图-->
            <el-card class="uniform-height" :body-style="{padding:0}">
              <div slot="header">
                <span class="panel-title home-title">
                  <i class="el-icon-pie-chart"></i>
                  已通过题目的标签和难度统计
                </span>
              </div>
              <template v-if="tagDifficultyOption.series[0].data.length && tagDifficultyOption.series[1].data.length">
                <ECharts :options="tagDifficultyOption" style="height: 350px; width: 100%" :autoresize="true"></ECharts>
              </template>
              <template v-else>
                <el-empty></el-empty>
              </template>
            </el-card>

            <el-card class="row-margin-top uniform-height" :body-style="{padding: 0}">
              <div slot="header">
                <span class="panel-title home-title">
                  <i class="el-icon-data-analysis"></i>
                  比赛情况
                  <el-select v-model="contestValue" placeholder="请选择比赛" style="float: right" size="mini">
                    <el-option
                        v-for="c in contestList"
                        :key="c.contestId"
                        :label="c.contestName"
                        :value="c.contestId">
                    </el-option>
                </el-select>
                </span>
              </div>
              <el-row>
                <!-- OI -->
                <template v-if="contestData.contestType">
                  <el-table :data="contestData.OIStatistic.statistics" border style="border-top-width: 0; width: 100%">
                    <el-table-column
                        v-for="(pid, index) in contestData.OIStatistic.pids"
                        :label="pid"
                        :key="index"
                        align="center"
                    >
                      <template v-if="contestData.OIStatistic.statistics[0][index].status > 0">
                        <div style="backgroundColor: #19BE6B; color: #FFFFFF;">
                          {{ contestData.OIStatistic.statistics[0][index].score }}
                          <br>
                          {{ contestData.OIStatistic.statistics[0][index].time }}ms
                        </div>
                      </template>
                      <template v-else-if="contestData.OIStatistic.statistics[0][index].status < 0">
                        <div style="backgroundColor: #E87272; color: #FFFFFF;">
                          {{ contestData.OIStatistic.statistics[0][index].score }}
                          <br>
                          --ms
                        </div>
                      </template>
                      <template v-else>
                        <div style="backgroundColor: #2D8CF0; color: #FFFFFF;">
                          {{ contestData.OIStatistic.statistics[0][index].score }}
                          <br>
                          --ms
                        </div>
                      </template>
                    </el-table-column>
                  </el-table>
                </template>
                <!-- ACM -->
                <template v-else>
                  <el-table :data="contestData.ACMStatistic.statistics" border style="border-top-width: 0; width: 100%">
                    <el-table-column
                        v-for="(pid, index) in contestData.ACMStatistic.pids"
                        :label="pid"
                        :key="index"
                        align="center"
                    >
                      <template v-if="contestData.ACMStatistic.statistics[0][index].total > 0">
                        <div style="backgroundColor: #19BE6B; color: #FFFFFF;">
                          +{{ contestData.ACMStatistic.statistics[0][index].total }}
                          <br>
                          {{ contestData.ACMStatistic.statistics[0][index].time }}ms
                        </div>
                      </template>
                      <template v-else-if="contestData.ACMStatistic.statistics[0][index].total < 0">
                        <div style="backgroundColor: #E87272; color: #FFFFFF;">
                          {{ contestData.ACMStatistic.statistics[0][index].total }}
                          <br>
                          {{ contestData.ACMStatistic.statistics[0][index].time }}ms
                        </div>
                      </template>
                    </el-table-column>
                  </el-table>
                </template>
              </el-row>
              <el-row>
                <ECharts :options="contestOption" :autoresize="true" style="height: 280px;width: 100%"></ECharts>
              </el-row>
              <el-row :gutter="10">
                <el-col :span="24">
                </el-col>
              </el-row>
            </el-card>
          </el-col>

          <el-col :span="10">
            <!--最近提交-->
            <el-card class="uniform-height" :body-style="{padding:'8px'}">
              <div slot="header">
                <span class="panel-title home-title">
                  <i class="el-icon-finished"></i>
                  TA的最近提交
                </span>
              </div>
              <template v-if="recentSubmission.length">
                <el-tooltip
                    v-for="({displayPid, title , status, submitTime, submitId},index) of recentSubmission"
                    :key="index"
                    :class="{'odd-submission': index % 2 === 0}"
                    placement="top"
                    effect="light"
                    open-delay="0.5"
                >
                  <div slot="content">
                    {{ "Run Id: " + submitId }}
                  </div>
                  <div class="recent-submission-row" @click="goSubmission(submitId)">
                    <span style="margin-left: 10px; font-size: 14px; font-weight: bolder">{{ displayPid + '.' }}</span>
                    <span style="margin-left: 5px; font-size: 18px;">{{ title }}</span>
                    <span style="margin-left: 10px; font-size: 12px; color: #8A8A8E">
                    {{ submitTime | fromNow }}
                  </span>
                    <span style="margin-left: auto; margin-right: 10px;" :class="getStatusColor(status)">
                    {{ JUDGE_STATUS[status].short }}
                  </span>
                  </div>
                </el-tooltip>
              </template>
              <template v-else>
                <el-empty></el-empty>
              </template>
            </el-card>


            <el-card class="row-margin-top uniform-height" :body-style="{padding: '0'}">
              <!--未涉足的标签-->
              <div slot="header">
                <span class="panel-title home-title">
                  <i class="el-icon-remove-outline"></i>
                  尚未涉足的标签
                </span>
              </div>
              <template v-if="profile.unsolvedList.length">
                <div class="tagScroll">
                  <el-collapse v-model="classificationItem" style="border-width: 0" accordion>
                    <el-collapse-item
                        v-for="({classification, tagList}, index) in profile.untouchedTags"
                        :key="index"
                        :name="index"
                        class="tag-collapse">
                      <div slot="title" style="font-weight: bold; font-size: large">
                        {{ classification ? classification.name : $t('m.Unclassified') }}
                      </div>
                      <el-button
                          v-for="(tag, id) in tagList"
                          :key="id"
                          size="mini"
                          :style="'margin-bottom:10px;color:#FFF;background-color:' + (tag.color ? tag.color : '#409eff')">
                        {{ tag.name }}
                      </el-button>
                    </el-collapse-item>
                  </el-collapse>
                </div>
              </template>
              <template v-else>
                <el-empty description="TA已经涉足了所有标签"></el-empty>
              </template>
            </el-card>
          </el-col>
        </el-row>

        <el-row class="row-margin-top">
          <el-col :span="24">
            <el-card :body-style="{padding: 0}">
              <el-collapse v-model="problemListName" accordion>
                <!--未通过题目-->
                <el-collapse-item name="unsolved">
                  <div slot="title" class="collapse-title">
                    <i class="el-icon-circle-close"></i>
                    未通过题目列表
                  </div>
                  <template v-if="profile.unsolvedList.length">
                    <br>
                    <div class="problem-btn tag-collapse" v-for="problemID of profile.unsolvedList" :key="problemID">
                      <el-button round @click="goProblem(problemID)" size="mini">
                        {{ problemID }}
                      </el-button>
                    </div>
                  </template>
                  <template v-else>
                    <el-empty description="TA没有未通过的题目"></el-empty>
                  </template>
                </el-collapse-item>
                <!--已通过的题目-->
                <el-collapse-item name="solved">
                  <div slot="title" class="collapse-title">
                    <i class="el-icon-circle-check"></i>
                    已通过题目列表
                  </div>
                  <template v-if="profile.solvedList.length">
                    <div class="problem-btn tag-collapse" v-for="problemID of profile.solvedList" :key="problemID">
                      <el-button round @click="goProblem(problemID)" size="mini">
                        {{ problemID }}
                      </el-button>
                    </div>
                  </template>
                  <template v-else>
                    <el-empty :description="$t('m.UserHome_Not_Data')"></el-empty>
                  </template>
                </el-collapse-item>
              </el-collapse>
            </el-card>
          </el-col>
        </el-row>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapActions } from 'vuex';
import api from '@/common/api';
import myMessage from '@/common/message';
import { addCodeBtn } from '@/common/codeblock';
import Avatar from 'vue-avatar';
import 'vue-calendar-heatmap/dist/vue-calendar-heatmap.css'
import { CalendarHeatmap } from 'vue-calendar-heatmap'
import utils from '@/common/utils';
import Markdown from '@/components/oj/common/Markdown';
import {
  JUDGE_STATUS,
  CONTEST_STATUS,
  JUDGE_STATUS_RESERVE,
  RULE_TYPE,
  PROBLEM_LEVEL
} from "@/common/constants";

export default {
  components: {
    Avatar,
    CalendarHeatmap,
    Markdown
  },
  methods: {
    ...mapActions(['changeDomTitle']),
    initModelOption() {
      this.modelOption.yAxis.data = this.profile.modelData.yaxisData;
      this.modelOption.series[0].data = this.profile.modelData.data.map((item) => {
        return [item.x, item.y, item.rate];
      });
    },
    getUserCalendarHeatmap() {
      const uid = this.$route.query.uid;
      const username = this.$route.query.username;
      api.getUserCalendarHeatmap(uid, username).then((res) => {
        this.calendarHeatmapValue = res.data.data.dataList;
        this.calendarHeatmapEndDate = res.data.data.endDate;
        this.loadingCalendarHeatmap = true
      });
      this.PROBLEM_LEVEL = Object.assign({}, PROBLEM_LEVEL);
    },
    handleTagDifficultyOption() {
      // 标签难度统计
      if (this.profile.tagDifficultyStatistic.difficultyStatistics === null ||
          this.profile.tagDifficultyStatistic.tagStatistics === null) {
        return;
      }
      this.tagDifficultyOption.legend[0].data = Object.keys(this.profile.tagDifficultyStatistic.difficultyStatistics).map((key) => {
        return utils.getLevelName(key);
      });
      this.tagDifficultyOption.series[0].data = Object.entries(this.profile.tagDifficultyStatistic.difficultyStatistics).map(([key, value]) => {
        return {
          name: utils.getLevelName(key),
          value: value,
          itemStyle: {
            color: utils.getLevelPureColor(key)
          }
        }
      });
      this.tagDifficultyOption.legend[1].data = Object.keys(this.profile.tagDifficultyStatistic.tagStatistics);
      this.tagDifficultyOption.series[1].data = Object.entries(this.profile.tagDifficultyStatistic.tagStatistics).map(([key, value]) => {
        return {
          name: key,
          value: value
        }
      });
    },
    getRecentSubmission() {
      const uid = this.$route.query.uid;
      const username = this.$route.query.username;
      let onlyMine = false;
      if (uid == null || username == null) onlyMine = true;
      api.getSubmissionList(8, {
        uid: uid,
        username: username,
        onlyMine: onlyMine,
        currentPage: 1
      }).then((res) => {
        this.recentSubmission = res.data.data.records;
      })
    },
    getStatusColor(status) {
      return "el-tag el-tag--medium status-" + JUDGE_STATUS[status]["color"];
    },
    init() {
      const uid = this.$route.query.uid;
      const username = this.$route.query.username;
      this.profile.loading = true;
      api.getUserInfo(uid, username).then((res) => {
        this.changeDomTitle({title: res.data.username});
        this.profile = res.data.data;
        this.initModelOption();
        this.handleTagDifficultyOption();
        this.$nextTick((_) => {
          addCodeBtn();
        });
        this.profile.loading = false;
      }, (_) => {
        this.profile.loading = false;
      });
    },
    goProblem(problemID) {
      this.$router.push({
        name: 'ProblemDetails',
        params: {problemID: problemID},
      });
    },
    goSubmission(submitId) {
      this.$router.push({
        name: "SubmissionDetails",
        params: {submitID: submitId},
      })
    },
    getSumScore(scoreList) {
      if (scoreList) {
        var sum = 0;
        for (let i = 0; i < scoreList.length; i++) {
          sum += scoreList[i];
        }
        return sum;
      }
    },
  },
  computed: {
    utils() {
      return utils
    },
    modelHeight() {
      let h = 90;
      if (this.profile.modelData.yaxisData) h += this.profile.modelData.yaxisData.length * 30;
      return {height: h + 'px'};
    },
    JUDGE_STATUS() {
      return JUDGE_STATUS
    },
  },
  data() {
    return {
      problemListName: 'unsolved',
      classificationItem: 0,
      collapseItem: 'untouchedTagsItem',
      tagDifficultyOption: {
        tooltip: {
          trigger: 'item',
          formatter: function (params) {
            const dot = `<span style="display:inline-block;margin-right:5px;border-radius:50%;width:10px;height:10px;background-color:${params.color};"></span>`;
            return `${dot}${params.name}: ${params.value} (${params.percent}%)`;
          }
        },
        legend: [
          {
            orient: 'vertical',
            right: 80,
            data: [],
            textStyle: {
              textBorderColor: "#FFFFFF",
              textBorderWidth: 3,
              fontWeight: 'bolder',
            }
          },
          {
            type: 'scroll',
            orient: 'vertical',
            top: 0,
            right: 0,
            data: [],
            textStyle: {
              textBorderColor: "#FFFFFF",
              textBorderWidth: 3,
              fontWeight: 'bolder',
            }
          }
        ],
        series: [
          {
            right: '20%',
            left: 0,
            top: 0,
            bottom: '5%',
            type: 'pie',
            selectedMode: 'single',
            radius: [0, '35%'],
            label: {
              show: false
            },
            data: [],
            itemStyle: {
              borderColor: "#FFFFFF",
              borderRadius: 5,
              borderWidth: 2
            },
          },
          {
            right: '20%',
            left: 0,
            top: 0,
            bottom: '5%',
            type: 'pie',
            radius: ['53%', '77%'],
            selectedMode: 'single',
            label: {
              show: true,
              fontWeight: 'bolder'
            },
            labelLine: {
              lineStyle: {
                width: 2
              }
            },
            data: [],
            itemStyle: {
              borderColor: "#FFFFFF",
              borderRadius: 5,
              borderWidth: 2
            },
          }
        ]
      },
      modelOption: {
        tooltip: {
          position: 'top'
        },
        grid: {
          containLabel: true,
          height: 'auto',
          left: 0,
          right: 0,
          top: 10,
          bottom: 0
        },
        xAxis: {
          type: 'category',
          data: ['初级', '中级', '高级', '通过率', '解题量'],
          position: 'top',
          axisLabel: {
            show: true,
            formatter: function (value) {
              return value.split("").join("\n");
            },
            fontWeight: 'bold',
            fontSize: 14
          },
          axisTick: {
            show: false
          },
          axisLine: {
            show: false
          },
          axisPointer: {
            show: true,
            type: "line",
            label: {
              show: false
            },
            triggerTooltip: false
          }
        },
        yAxis: {
          type: 'category',
          data: [],
          splitArea: {
            show: true,
            areaStyle: {
              color: ['#F6F6F6', '#FFFFFF']
            }
          },
          axisTick: {
            show: false
          },
          axisLine: {
            show: false
          },
          axisLabel: {
            show: true,
          },
          axisPointer: {
            show: true,
            type: "line",
            label: {
              show: false
            },
            triggerTooltip: false
          }
        },
        visualMap: {
          min: 0,
          max: 1,
          show: false,
        },
        series: [
          {
            type: 'effectScatter',
            data: [],
            tooltip: {
              formatter: function (params) {
                const dot = `<span style="display:inline-block;margin-right:5px;border-radius:50%;width:10px;height:10px;background-color:${params.color};"></span>`
                const percentage = (params.value[2] * 100).toFixed(1);
                return `${dot}${percentage}%`;
              },
              extraCssText: 'height: 25px;',
              padding: [2, 5]
            }
          }
        ]
      },
      contestOption: {
        title: [
          {
            text: '比赛表现',
            left: 'center',
            top: 60
          },
          {
            text: "提交次数: 11",
            right: 50,
            top: 180,
          },
          {
            text: "击败40%参赛者",
            left: 35,
            top: 180,
          }
        ],
        tooltip: [
          {
            trigger: 'item',
            // confine: true, // 将tooltip框限制在图表的区域内
            appendToBody: true,
          }, {}, {}
        ],
        radar: {
          indicator: [
            {name: '通过率\n(过题数/过题总提交数)', max: 1},
            {name: '平均过题时间\n(罚时/过题数)', max: 99},
            {name: '过题数', max: 7},
          ],
          radius: ['0', '80'],
          center: ['50%', 200],
          startAngle: 210,
        },
        series: [
          {
            type: 'radar',
            tooltip: {
              trigger: 'item',
            },
            areaStyle: {},
            label: {
              show: true
            },
            data: [
              {
                value: [0.6, 51, 6],
                name: '比赛表现',
              }
            ],
          },
          {
            type: 'pie',
            top: 10,
            center: ['80%', 90],
            radius: ['40', '70'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 5,
              borderColor: '#FFFFFF',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            tooltip: {
              show: false
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 14,
                fontWeight: 'bold',
                formatter: '{b}\n{c} 次'
              }
            },
            data: [
              {value: 4, name: 'Accepted', itemStyle: {color: '#19BE6B'}},
              {value: 2, name: 'Partial Accepted', itemStyle: {color: '#2D8CF0'}},
              {value: 1, name: 'Compile Error', itemStyle: {color: '#FF9900'}},
              {value: 6, name: 'Wrong Answer', itemStyle: {color: '#ED3F14'}},
              {value: 3, name: 'Time Limit Exceeded', itemStyle: {color: '#ED3F14'}}
            ]
          },
          {
            type: "gauge",
            center: ['20%', 100],
            radius: '70',
            detail: {
              show: false
            },
            data: [
              {
                value: 40,
                name: '排名1',
                title: {
                  offsetCenter: ['0%', '40%']
                },
              },
            ],
            tooltip: {
              show: false
            },
            progress: {
              show: true,
              roundCap: true // 圆角
            },
            axisLine: {
              roundCap: true // 圆角
            },
            splitLine: {
              show: false // 大刻度
            },
            axisTick: {
              show: true, // 刻度
              splitNumber: 1,
              distance: 0
            },
            axisLabel: {
              show: false, // 刻度值
            },
            anchor: {
              show: true, // 轴心
              showAbove: true
            },
          }
        ]
      },
      contestValue: '1',
      contestData: {
        contestType: 1,
        OIStatistic: {
          pids: ['A', 'B', 'C', 'D', 'E', 'F'],
          statistics: [
            [{status: 1, score: 50, time: 19},
              {status: 1, score: 100, time: 34},
              {status: 0, score: 60, time: 0},
              {status: -1, score: -1, time: 0},
              {status: -1, score: -3, time: 0},
              {status: 1, score: 300, time: 44}]
          ]
        },
        ACMStatistic: {
          pids: ['A', 'B', 'C', 'D'],
          statistics: [
            [{total: 3, time: 15, first: 1},
              {total: -6, time: 23, first: 0},
              {total: 0, time: 54, first: 0},
              {total: 1, time: 76, first: 0}]
          ]
        },
        radarData: {},
      },
      contestList: [
        {
          contestName: '比赛1',
          contestId: '1'
        },
        {
          contestName: '比赛2',
          contestId: '2'
        },
      ],
      recentSubmission: [],
      profile: {
        username: '',
        nickname: '',
        gender: '',
        avatar: '',
        school: '',
        signature: '',
        total: 0,
        rating: 0,
        score: 0,
        solvedList: [],
        scoreList: [],
        // solvedGroupByDifficulty: null,
        calendarHeatLocale: null,
        calendarHeatmapValue: [],
        calendarHeatmapEndDate: '',
        loadingCalendarHeatmap: false,
        loading: false,
        // 以下为新增
        untouchedTags: [],
        unsolvedList: [],
        tagDifficultyStatistic: {
          tagStatistics: {},
          difficultyStatistics: {}
        },
        modelData: {
          yaxisData: [],
          data: []
        },
        recommendProblems: []
      },
      PROBLEM_LEVEL: {},
    };
  },
  created(){
    this.getUserCalendarHeatmap();
  },
  mounted() {
    this.calendarHeatLocale = {
      months: [
        this.$i18n.t('m.Jan'),
        this.$i18n.t('m.Feb'),
        this.$i18n.t('m.Mar'),
        this.$i18n.t('m.Apr'),
        this.$i18n.t('m.May'),
        this.$i18n.t('m.Jun'),
        this.$i18n.t('m.Jul'),
        this.$i18n.t('m.Aug'),
        this.$i18n.t('m.Sep'),
        this.$i18n.t('m.Oct'),
        this.$i18n.t('m.Nov'),
        this.$i18n.t('m.Dec')
      ],
      days: [
        this.$i18n.t('m.Sun'),
        this.$i18n.t('m.Mon'),
        this.$i18n.t('m.Tue'),
        this.$i18n.t('m.Wed'),
        this.$i18n.t('m.Thu'),
        this.$i18n.t('m.Fri'),
        this.$i18n.t('m.Sat')
      ],
      on: this.$i18n.t('m.on'),
      less: this.$i18n.t('m.Less'),
      more: this.$i18n.t('m.More')
    }
    this.init();
    this.getRecentSubmission();
    this.JUDGE_STATUS = Object.assign({}, JUDGE_STATUS);
    this.JUDGE_STATUS_LIST = Object.assign({}, JUDGE_STATUS);
    this.JUDGE_STATUS_RESERVE = Object.assign({}, JUDGE_STATUS_RESERVE);
    this.CONTEST_STATUS = Object.assign({}, CONTEST_STATUS);
    this.RULE_TYPE = Object.assign({}, RULE_TYPE);
  },
  watch: {
    $route(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.init();
      }
    },
    "$store.state.language"(newVal,oldVal){
      console.log(newVal,oldVal)
      this.calendarHeatLocale = {
        months: [
          this.$i18n.t('m.Jan'),
          this.$i18n.t('m.Feb'),
          this.$i18n.t('m.Mar'),
          this.$i18n.t('m.Apr'),
          this.$i18n.t('m.May'),
          this.$i18n.t('m.Jun'),
          this.$i18n.t('m.Jul'),
          this.$i18n.t('m.Aug'),
          this.$i18n.t('m.Sep'),
          this.$i18n.t('m.Oct'),
          this.$i18n.t('m.Nov'),
          this.$i18n.t('m.Dec')
        ],
        days: [
          this.$i18n.t('m.Sun'),
          this.$i18n.t('m.Mon'),
          this.$i18n.t('m.Tue'),
          this.$i18n.t('m.Wed'),
          this.$i18n.t('m.Thu'),
          this.$i18n.t('m.Fri'),
          this.$i18n.t('m.Sat')
        ],
        on: this.$i18n.t('m.on'),
        less: this.$i18n.t('m.Less'),
        more: this.$i18n.t('m.More')
      }
    }
  },
};
</script>
<style scoped>
.tagScroll {
  overflow: auto;
  max-height: 350px
}

/deep/ .el-table .cell {
  padding: 0 !important;
}

/deep/ .el-table th {
  padding: 0 !important;
}

/deep/ .el-table td {
  padding: 0 !important;
}

.el-card {
  border-radius: .5rem !important;
}

/deep/ .el-card__header {
  padding: 0.6rem 1.25rem !important;
}

.row-margin-top {
  margin-top: 15px;
}

.submission {
  background: skyblue;
  color: #fff;
  font-size: 14px;
}

.solved {
  margin-top: 10px;
  background: #67c23a;
  color: #fff;
  font-size: 14px;
}

.score {
  margin-top: 10px;
  background: #e6a23c;
  color: #fff;
  font-size: 14px;
}

.rating {
  margin-top: 10px;
  background: #dd6161;
  color: #fff;
  font-size: 14px;
}

.default-info {
  font-size: 13px;
  padding-right: 5px;
}

.data-number {
  font-size: 20px;
  font-weight: 600;
  flex: 1;
  text-align: center;
}

.profile-container p {
  margin-top: 8px;
  margin-bottom: 8px;
}

@media screen and (max-width: 1440px) {
  .profile-container {
    position: relative;
    width: 100%;
    margin-top: 15px;
    //text-align: center;
  }

  .profile-container .avatar-container {
    position: absolute;
    left: 50%;
    transform: translate(-50%);
    z-index: 1;
    margin-top: -90px;
  }

  .profile-container .recent-login {
    text-align: center;
    margin-top: 30px;
  }
}

@media screen and (min-width: 1440px) {
  .profile-container {
    position: relative;
    width: 70%;
    left: 15%;
    margin-top: 15px;
    //text-align: center;
  }

  .profile-container .avatar-container {
    position: absolute;
    left: 50%;
    transform: translate(-50%);
    z-index: 1;
    margin-top: -8%;
  }

  .profile-container .recent-login {
    text-align: right;
    position: absolute;
    right: 1rem;
    top: 0.5rem;
  }

  .profile-container .user-info {
    margin-top: 50px;
  }
}

.profile-container .avatar {
  width: 140px;
  height: 140px;
  border-radius: 50%;
  box-shadow: 0 1px 1px 0;
}

.username {
  font-size: 32px;
  font-weight: 700;
  color: #409EFF;
}

.nickname {
  font-weight: 500;
  color: gray;
}

.flex-display .titleName {
  position: absolute;
  bottom: 0;
  left: 0;
}

#problems {
  padding-left: 30px;
  padding-right: 30px;
  font-size: 18px;
}

.level-card {
  width: calc(45% - 0.5em);
  margin: 1rem auto;
}

@media (max-width: 768px) {
  .level-card {
    margin: 1em 0;
    width: 100%;
  }

  #problems {
    padding-left: 0px;
    padding-right: 0px;
  }
}

.card-p-count {
  float: right;
  font-size: 1.1em;
  font-weight: bolder;
}

.problem-btn {
  display: inline-block;
  margin-left: 10px;
  margin-top: 10px;
}

#icons .icon {
  font-size: 13px !important;
  padding: 0 10px;
  color: #2196f3;
}

.signature-body {
  background: #fff;
  overflow: hidden;
  width: 100%;
  padding: 10px 10px;
  text-align: left;
  font-size: 14px;
  line-height: 1.6;
}

.gender-male {
  font-size: 16px;
  margin-left: 5px;
  color: white;
  border-radius: 4px;
  padding: 2px;
}

.male {
  background-color: #409eff;
}

.female {
  background-color: pink;
}

.card-title {
  font-size: 1.2rem;
  font-weight: 500;
  align-items: center;
  text-align: left;
  margin-bottom: 10px;
}

/deep/ .vch__day__square {
  cursor: pointer !important;
  transition: all .2s ease-in-out !important;
}

/deep/ .vch__day__square:hover {
  height: 11px !important;
  width: 11px !important;
}

/deep/ svg.vch__wrapper rect.vch__day__square:hover {
  stroke: rgb(115, 179, 243) !important;
}

/deep/ svg.vch__wrapper .vch__months__labels__wrapper text.vch__month__label,
/deep/ svg.vch__wrapper .vch__days__labels__wrapper text.vch__day__label,
/deep/ svg.vch__wrapper .vch__legend__wrapper text {
  font-size: 0.5rem !important;
  font-weight: 600 !important;
}

/deep/ rect {
  rx: 2;
  ry: 2;
}

.flex-display {
  display: flex;
  align-items: center;
}

.profile-emphasis {
  margin-left: 15px;
  display: block;
}

.uniform-height {
  height: 400px;
}

.odd-submission {
  background-color: #F7F7F8;
}

.recent-submission-row {
  height: 40px;
  margin-bottom: 3px;
  border-radius: 5px;
  transition: box-shadow 0.3s ease;
  display: flex;
  align-items: center;
}

.recent-submission-row:hover {
  cursor: pointer;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.1); /* 添加阴影效果 */
}

.collapse-title {
  color: #409eff;
  font-family: "Raleway";
  font-size: 21px;
  font-weight: 500;
  margin-left: 10px;
}

.tag-collapse {
  margin: 0 10px 10px 10px;
}

</style>