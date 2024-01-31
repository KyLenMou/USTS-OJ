<template>
  <div class="profile-container" v-loading="loading">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card>
          <el-row>
            <el-col :span="5">
              <div class="profile">
                <!--头像-->
                <span>
                  <!--<el-avatar shape="square" :size="120" :src="profile.avatar"></el-avatar>-->
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
            </el-col>

            <el-col :span="15">
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
            </el-col>
            <el-col :span="4" class="profile-right">
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
            </el-col>
          </el-row>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="row-margin-top">
      <el-col :span="5">
        <el-card>
          <el-row>
            <el-col :span="24">
              <!--四个方块-->
              <el-card shadow="always" class="submission">
                <span>
                  <i class="fa fa-th" aria-hidden="true"></i>
                  {{ $t('m.UserHome_Submissions') }}
                </span>
                <span class="data-number">{{ profile.total }}</span>
              </el-card>
              <el-card shadow="always" class="solved">
                <span>
                  <i class="fa fa-check-circle" aria-hidden="true"></i>
                  {{ $t('m.UserHome_Solved') }}
                </span>
                <span class="data-number">{{ profile.solvedList.length }}</span>
              </el-card>
              <el-card shadow="always" class="score">
                <span>
                  <i class="fa fa-star" aria-hidden="true"></i>
                  {{ $t('m.UserHome_Score') }}
                </span>
                <span class="data-number">{{ getSumScore(profile.scoreList) }}</span>
              </el-card>
              <el-card shadow="always" class="rating">
                <span>
                  <i class="fa fa-user-secret" aria-hidden="true"></i>
                  {{ $t('m.UserHome_Rating') }}
                </span>
                <span class="data-number">
                  {{ profile.rating ? profile.rating : '--' }}
                </span>
              </el-card>
            </el-col>
          </el-row>
          <!--能力知识模型-->
          <el-row>
            <e-charts :options="VerticalOption" class="VerticalHeatmap"></e-charts>
          </el-row>
          <!--个性化推荐-->
          <el-divider>个性化推荐</el-divider>
          <el-table
              :data="recommendProblems"
          >
            <el-table-column
                prop="id"
                label="题目id"
            ></el-table-column>
            <el-table-column
                prop="name"
                label="题目"
            ></el-table-column>
            <el-table-column
                prop="difficulty"
                label="难度"
            ></el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="19">
        <el-row>
          <el-col :span="24">
            <!--热力图-->
            <el-card>
              <calendar-heatmap
                  :values="profile.calendarHeatmapValue"
                  :end-date="profile.calendarHeatmapEndDate"
                  :tooltipUnit="$t('m.Calendar_Tooltip_Uint')"
                  :locale="profile.calendarHeatLocale"
                  :range-color="['rgb(218, 226, 239)', '#9be9a8', '#40c463', '#30a14e', '#216e39']"
              >
              </calendar-heatmap>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="20" class="row-margin-top">
          <el-col :span="14">
            <!--标签难度饼图-->
            <el-card class="uniform-height">
              <div slot="header">
                <span class="panel-title home-title">
                  <i class="el-icon-pie-chart"></i>
                  已通过题目的标签和难度统计
                </span>
              </div>
              <TagDifficultyStatistic></TagDifficultyStatistic>
              <!--              <e-charts :options="TagDifficultyOption" style="height: 330px;"></e-charts>-->
            </el-card>

            <el-card class="row-margin-top uniform-height">
              <div slot="header">
                <span class="panel-title home-title">
                  <i class="el-icon-data-analysis"></i>
                  比赛情况
                </span>
              </div>
              <!--              <e-charts :options="ContestsOption" style="height: 330px; width: 500px;"></e-charts>-->
            </el-card>
          </el-col>

          <el-col :span="10">
            <!--最近提交-->
            <el-card class="uniform-height" :body-style="{padding:'18px 10px'}">
              <div slot="header">
                <span class="panel-title home-title">
                  <i class="el-icon-finished"></i>
                  TA的最近提交
                </span>
              </div>
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
                  <span style="margin-left: 10px;">{{ displayPid + '.' }}</span>
                  <span style="margin-left: 5px">{{ title }}</span>
                  <span style="margin-left: 10px; font-size: 14px; color: #8A8A8E">
                    {{ submitTime | fromNow }}
                  </span>
                  <span style="margin-left: auto; margin-right: 10px;" :class="getStatusColor(status)">
                    {{ JUDGE_STATUS[status].name }}
                  </span>
                </div>
              </el-tooltip>
            </el-card>


            <el-card class="row-margin-top" :body-style="{padding: '0'}">
              <el-collapse v-model="collapseItem">
                <!--未涉足的标签-->
                <el-collapse-item name="untouchedTagsItem" class="untouchedTags">
                  <div slot="title" class="collapse-title">
                    <i class="el-icon-remove-outline"></i>
                    尚未涉足的标签
                  </div>
                  <el-collapse v-model="classificationItem">
                    <div v-for="({classification, tagList}, index) in profile.untouchedTags"
                         :key="index"
                         class="tag-collapse">
                      <el-collapse-item :name="index">
                        <div slot="title" style="font-weight: bold; font-size: large">
                          {{ classification ? classification.name : $t('m.Unclassified') }}
                        </div>
                        <el-button
                            v-for="(tag, id) in tagList"
                            :key="id"
                            type="ghost"
                            size="mini"
                            :style="'margin-bottom:10px;color:#FFF;background-color:' + (tag.color ? tag.color : '#409eff')">
                          {{ tag.name }}
                        </el-button>
                      </el-collapse-item>
                    </div>
                  </el-collapse>
                </el-collapse-item>
                <!--未通过题目-->
                <el-collapse-item>
                  <div slot="title" class="collapse-title">
                    <i class="el-icon-circle-close"></i>
                    未通过题目列表
                  </div>
                  <template v-if="profile.unsolvedList.length">
                    <div class="problem-btn tag-collapse" v-for="problemID of profile.unsolvedList" :key="problemID">
                      <el-button round @click="goProblem(problemID)" size="mini">
                        {{ problemID }}
                      </el-button>
                    </div>
                  </template>
                  <template v-else>
                    <p> TA没有未通过的题目 </p>
                  </template>
                </el-collapse-item>
                <!--已通过的题目-->
                <el-collapse-item>
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
                    <p>{{ $t('m.UserHome_Not_Data') }}</p>
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
import TagDifficultyStatistic from "@/components/oj/charts/TagDifficultyStatistic.vue";
import {mapActions} from 'vuex';
import api from '@/common/api';
import myMessage from '@/common/message';
import {addCodeBtn} from '@/common/codeblock';
import Avatar from 'vue-avatar';
import 'vue-calendar-heatmap/dist/vue-calendar-heatmap.css'
import {CalendarHeatmap} from 'vue-calendar-heatmap'
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
    TagDifficultyStatistic,
    Avatar,
    CalendarHeatmap,
    Markdown
  },
  methods: {
    init1() {
      const uid = this.$route.query.uid;
      const username = this.$route.query.username;
      api.getUserProblemsInfo(uid, username).then((res) => {
        if (res.data.data.total) {
          const difficultyColors = [
            '#19BE6B',
            '#2D8CF0',
            '#ED3F14'
          ];
          this.info = {...res.data.data};
          this.TagDifficultyOption.series[1].data = res.data.data.tagStatistics;
          this.TagDifficultyOption.series[0].data = res.data.data.difficultyStatistics.map(item => {
            return {
              name: item.name == '0' ? '简单' : item.name == '1' ? '中等' : '困难',
              value: item.value,
              itemStyle: {
                color: difficultyColors[parseInt(item.name)]
              }
            }
          });
          this.TagDifficultyOption.legend[0].data = res.data.data.tagStatistics.map(item => item.name);
          this.TagDifficultyOption.legend[1].data = res.data.data.difficultyStatistics.map(item => {
            return {
              name: item.name == '0' ? '简单' : item.name == '1' ? '中等' : '困难'
            }
          });
        } else {
          this.info = {...res.data.data};
        }
        // this.loading = false;
      }, (_) => {
        // this.loading = false;
      });

    },
    getRecentSubmission() {
      const uid = this.$route.query.uid;
      const username = this.$route.query.username;
      let onlyMine = false;
      if (uid == null || username == null) onlyMine = true;
      api.getSubmissionList(7, {
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
    ...mapActions(['changeDomTitle']),
    init() {
      const uid = this.$route.query.uid;
      const username = this.$route.query.username;
      this.profile.loading = true;
      api.getUserInfo(uid, username).then((res) => {
        this.changeDomTitle({title: res.data.username});
        this.profile = res.data.data;
        console.log(this.profile.untouchedTags);
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
    nicknameColor(nickname) {
      let typeArr = ['', 'success', 'info', 'danger', 'warning'];
      let index = nickname.length % 5;
      return typeArr[index];
    },
    getLevelColor(difficulty) {
      return utils.getLevelColor(difficulty);
    },
    getLevelName(difficulty) {
      return utils.getLevelName(difficulty);
    },
    getProblemListCount(list) {
      if (!list) {
        return 0;
      } else {
        return list.length;
      }
    }
  },
  computed: {
    JUDGE_STATUS() {
      return JUDGE_STATUS
    },
    HeatmapOption() {
      return {
        tooltip: {},
        visualMap: {
          type: 'piecewise',
          show: false,
          pieces: [
            {min: 0, max: 0, color: '#EBEDF0'},
            {min: 1, max: 1, color: '#91DA9E'},
            {min: 2, max: 3, color: '#40C463'},
            {min: 4, max: 6, color: '#30A14E'},
          ],
          outOfRange: {
            color: ['#216E39']
          }
        },
        calendar: {
          top: 20,
          left: 45,
          right: 15,
          cellSize: 'auto',
          range: [this.heatmap.start, this.heatmap.end],
          itemStyle: {
            borderWidth: 1.5,
            borderColor: "#FFFFFF"
          },
          yearLabel: {show: false},
          splitLine: false,
          dayLabel: {
            nameMap: ["", "周一", "", "周三", "", "周五", "",]
          },
          monthLabel: {
            nameMap: ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月']
          }
        },
        series: {
          type: 'heatmap',
          coordinateSystem: 'calendar',
          data: this.heatmap.data,
          itemStyle: {
            borderColor: "#FFFFFF",
          }
        }
      }
    }
  },
  data() {
    return {
      classificationItem: 0,
      collapseItem: 'untouchedTagsItem',
      recommendProblems: [
        {
          id: 1,
          name: 'DFS',
          difficulty: '困难'
        }
      ],
      info: {
        untouchedTags: [
          'BFS', '并查集'
        ]
      },
      ContestsOption: {
        tooltip: {
          trigger: 'item',
          formatter: function (params) {
            // 获取当前鼠标悬停的数据
            const data = params.data;
            if (data.tags) {
              // 返回需要展示的标签信息
              return data.tags.join(', '); // 假设标签信息以数组形式存储在data.tags中
            }
            return '暂无标签'; // 如果没有标签信息，则返回空
          }
        },
        series: [{
          type: 'pie',
          center: ['50%', '50%'],
          roseType: 'area',
          radius: [10, 140],
          itemStyle: {
            borderColor: "#FFFFFF",
            borderWidth: 2,
            borderRadius: 8
          },
          label: {
            show: true,
            fontWeight: 'bolder'
          },
          selectedMode: 'single',
          data: [
            {value: 5, name: '题目1', tags: ['DP', 'DFS']},
            {value: 4, name: '题目2', tags: ['并查集', '构造']},
            {value: 5, name: '题目3', tags: ['并查集2', '构造3']},
            {value: 6, name: '题目4', tags: ['并查集1', '构造5']},
            {value: 6, name: '题目5', tags: ['并查集1', '构造5']},
          ],
        }]
      },
      recentSubmission: [],
      TagDifficultyOption: {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: [
          {
            type: 'scroll',
            orient: 'vertical',
            right: 0,
            data: [],
            textStyle: {
              textBorderColor: "#FFFFFF",
              textBorderWidth: 3,
              fontWeight: 'bolder',
            }
          },
          {
            orient: 'vertical',
            right: '20%',
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
              borderWidth: 1
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
              borderWidth: 1
            },
          }
        ]
      },
      VerticalOption: {
        title: {
          top: 30,
          left: 'center',
          text: 'Daily Step Count'
        },
        tooltip: {},
        visualMap: {
          min: 0,
          max: 10000,
          type: 'piecewise',
          orient: 'horizontal',
          left: 'center',
          top: 65
        },
        calendar: {
          top: 120,
          left: 30,
          right: 30,
          cellSize: ['auto', 13],
          orient: 'vertical',
          range: ['2023-01-30', '2024-01-26'],
          itemStyle: {
            borderWidth: 0.5
          },
          yearLabel: {show: false}
        },
        series: {
          type: 'heatmap',
          coordinateSystem: 'calendar',
          data: []
        }
      },
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
        solvedGroupByDifficulty: null,
        calendarHeatLocale: null,
        calendarHeatmapValue: [],
        calendarHeatmapEndDate: '',
        loadingCalendarHeatmap: false,
        loading: false,
        // 以下为新增
        untouchedTags: [],
        unsolvedList: [],
      },
      PROBLEM_LEVEL: {},
    };
  },
  created() {
    const uid = this.$route.query.uid;
    const username = this.$route.query.username;
    api.getUserCalendarHeatmap(uid, username).then((res) => {
      this.profile.calendarHeatmapValue = res.data.data.dataList;
      this.profile.calendarHeatmapEndDate = res.data.data.endDate;
      this.profile.loadingCalendarHeatmap = true
    });
    this.PROBLEM_LEVEL = Object.assign({}, PROBLEM_LEVEL);
  },
  mounted() {
    // this.init1();
    this.JUDGE_STATUS = Object.assign({}, JUDGE_STATUS);
    this.JUDGE_STATUS_LIST = Object.assign({}, JUDGE_STATUS);
    this.JUDGE_STATUS_RESERVE = Object.assign({}, JUDGE_STATUS_RESERVE);
    this.CONTEST_STATUS = Object.assign({}, CONTEST_STATUS);
    this.RULE_TYPE = Object.assign({}, RULE_TYPE);
    this.profile.calendarHeatLocale = {
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
  },
  watch: {
    $route(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.init();
      }
    },
    "$store.state.language"(newVal, oldVal) {
      console.log(newVal, oldVal)
      this.profile.calendarHeatLocale = {
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
        more: this.$i18n.t('m.More'),
      }
    }
  },
};
</script>
<style scoped>
/deep/ .el-card__header {
  padding: 0.6rem 1.25rem !important;
}

.untouchedTags /deep/ .el-collapse-item__content {
  padding-bottom: 0;
}

.untouchedTags /deep/ .el-collapse-item__wrap{
  border-bottom-width: 0;
}



.row-margin-top {
  margin-top: 20px;
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
}

.profile-container p {
  margin-top: 8px;
  margin-bottom: 8px;
}

@media screen and (max-width: 1080px) {
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

@media screen and (min-width: 1080px) {
  .profile-container {
    position: relative;
    width: 80%;
    left: 10%;
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

.profile .titleName {
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

.profile-right {
  text-align: right;
  line-height: 2;
}

.profile {
  display: flex;
}

.profile-emphasis {
  margin-left: 15px;
  display: block;
}

.HorizontalHeatmap {
  height: 150px;
  width: 100%;
}

.VerticalHeatmap {
  height: 800px;
  width: 100%;
}

.tagButtons {
  display: inline-block;
  margin: 5px;
}

.uniform-height {
  height: 400px;
}

.odd-submission {
  background-color: #F7F7F8;
}

.recent-submission-row {
  height: 43px;
  font-size: 18px;
  margin-bottom: 2px;
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