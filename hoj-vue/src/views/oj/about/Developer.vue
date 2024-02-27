<template>
  <div>
    <div class="intr-center">
      <paper-card type="server">
        <h1 class="text-darken">USTS Online Judge</h1>
        <h2>
          苏州科技大学算法Online Judge平台是苏州科技大学《高级算法设计与分析》在线开放课程实训平台，也是苏州科技大学ACM竞赛团队训练比赛专用OJ平台。
        </h2>
        <h2>
          苏州科技大学ACM竞赛团队由奚雪峰教授、程成博士于2019年联合创建，短短几年内硕果累累，汇聚了苏科大计算机算法领域顶尖优秀学生，形成了算法教学、研究及应用的浓厚学术氛围。
        </h2>
      </paper-card>
      <paper-card type="server">
        <h1 class="text-darken1">近年来获得奖项如下</h1>
        <p> 2020年江苏省大学生程序设计竞赛(JSCPC) 铜奖(bronze medal) </p>
        <p> 2020年国际大学生程序设计竞赛(ICPC) 银川站 铜奖(bronze medal) </p>
        <p>2021年中国大学生程序设计竞赛(CCPC) 桂林站 铜奖(bronze medal)</p>
        <p>2021年国际大学生程序设计竞赛(ICPC) 上海站 铜奖(bronze medal)</p>
        <p>2022年江苏省大学生程序设计竞赛(JSCPC) 铜奖*2(bronze medal)</p>
        <h2>2022年江苏省大学生程序设计竞赛(JSCPC) 银奖*1(silver medal)</h2>
        <h2>中国高校计算机大赛- 团体程序设计天梯赛 团体国家二等奖</h2>
        <p>中国高校计算机大赛- 团体程序设计天梯赛 个人国家二等奖*2</p>
        <p>中国高校计算机大赛- 团体程序设计天梯赛 个人国家三等奖*5</p>
        <h2>RoboCom机器人编程开发大赛 国家一等奖*2</h2>
        <p>RoboCom机器人编程开发大赛 国家二等奖、国家三等奖*10+</p>
        <h2>蓝桥杯全国软件和信息技术专业人才大赛 国家一等奖*2</h2>
        <p>蓝桥杯全国软件和信息技术专业人才大赛 国二、国三*10+</p>
      </paper-card>
      <paper-card type="server">
        <h1>贡献者</h1>
        <p>
          开发人员：蒋城、彭羽、盛弘康、韩世豪、时微淳、牟其霖，服务器维护人员：牟其霖
        </p>
        <p>
          指导老师：奚雪峰 教授
        </p>
      </paper-card>
    </div>
<!--    <el-row :gutter="20">-->
<!--      <el-col :xs="24" :md="12">-->
<!--        <paper-card type="server">-->
<!--          <h1>{{ $t('m.Distributed') }}</h1>-->
<!--          <p>-->
<!--            <small>{{ $t('m.Distributed_Desc') }}</small>-->
<!--          </p>-->
<!--          <p class="teal-text">-->
<!--            <i class="el-icon-circle-check"></i> {{ $t('m.Available') }}-->
<!--          </p>-->
<!--        </paper-card>-->
<!--      </el-col>-->
<!--      <el-col :xs="24" :md="12">-->
<!--        <paper-card type="server">-->
<!--          <h1>{{ $t('m.Customization') }}</h1>-->
<!--          <p>-->
<!--            <small>{{ $t('m.Customization_Desc') }}</small>-->
<!--          </p>-->
<!--          <p class="teal-text">-->
<!--            <i class="el-icon-circle-check"></i> {{ $t('m.Available') }}-->
<!--          </p>-->
<!--        </paper-card>-->
<!--      </el-col>-->
<!--      <el-col :xs="24" :md="12">-->
<!--        <paper-card type="server">-->
<!--          <h1>{{ $t('m.Security') }}</h1>-->
<!--          <p>-->
<!--            <small>{{ $t('m.Security_Desc') }}</small>-->
<!--          </p>-->
<!--          <p class="teal-text">-->
<!--            <i class="el-icon-circle-check"></i> {{ $t('m.Available') }}-->
<!--          </p>-->
<!--        </paper-card>-->
<!--      </el-col>-->
<!--      <el-col :xs="24" :md="12">-->
<!--        <paper-card type="server">-->
<!--          <h1>{{ $t('m.Diversity') }}</h1>-->
<!--          <p>-->
<!--            <small>{{ $t('m.Diversity_Desc') }}</small>-->
<!--          </p>-->
<!--          <p class="teal-text">-->
<!--            <i class="el-icon-circle-check"></i> {{ $t('m.Available') }}-->
<!--          </p>-->
<!--        </paper-card>-->
<!--      </el-col>-->
<!--    </el-row>-->
  </div>
</template>
<script>
</script>
<style scoped>
.intr-center {
  text-align: center;
  max-width: 100%;
  margin: 0 auto;
}

paper-card[type='server'] {
  display: block;
  text-align: center;
  border-radius: 4px;
  transition: 0.2s ease-out 0s;
  color: rgba(0, 0, 0, 0.63);
  background-image: linear-gradient(180deg, hsla(0, 0%, 100%, 0) 30%, #fff),
  linear-gradient(70deg, #e0f1ff 32%, #fffae3);
  padding: 1rem;
  position: relative;
  border: 1px solid rgba(0, 0, 0, 0.15);
  margin-bottom: 2rem;
}

paper-card:hover {
  box-shadow: rgb(0 0 0 / 15%) 0px 0px 40px;
}

.teal-text {
  color: #009688 !important;
}

.text-darken {
  margin-top: 0;
  margin-bottom: 0.5rem;
  color: #2196f3 !important;
  font-weight: 400;
  font-size: 40px;
}

.text-darken1 {
  margin-top: 0;
  margin-bottom: 0.5rem;
  font-weight: 400;
  font-size: 40px;
}

.icon {
  font-size: 1rem;
  line-height: 1.5;
  color: #212529;
}

.intr-center p {
  font-size: 1rem;
  line-height: 1.5;
}
</style>
