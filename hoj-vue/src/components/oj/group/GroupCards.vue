<template>
  <el-row :gutter="20">
    <el-col
      :xxl="4"
      :xl="6"
      :lg="8"
      :md="8"
      :sm="12"
      :xs="24"
      v-if="!groupList.length"
      style="margin-top: 10px; margin-bottom: 10px"
    >
      <el-card
        :body-style="{ padding: '0px' }"
        style="border-radius: 10px;"
      >
        <el-empty :description="$t('m.No_Groups')"></el-empty>
      </el-card>
    </el-col>
    <el-col
      :xxl="4"
      :xl="6"
      :lg="8"
      :md="8"
      :sm="12"
      :xs="24"
      v-for="group in groupList"
      :key="group.id"
      style="margin-top: 10px; margin-bottom: 10px"
    >
      <el-row :gutter="1">
        <el-col :span="7" style="text-align: center;">
          <el-card
            :body-style="{ padding: '0px' }"
            style="border-radius: 10px; height: 170px"
          >
            <template v-if="group.avatar">
              <el-image
                :src="group.avatar"
                @click="$emit('open-group', group.id)"
                fit="cover"
                style="height: 135px; width: 100%"
              ></el-image>

</template>
                <template v-else>
                  <el-image
                    :src="defaultAvatar"
                    @click="$emit('open-group', group.id)"
                    fit="cover"
                    style="height: 135px; width: 100%"
                  ></el-image>
                </template>
                <el-link
                  style="font-size: 16px"
                  type="primary"
                  :underline="false"
                  @click="$emit('open-user', group.owner)"
                  ><i class="el-icon-user-solid"></i> {{ group.owner }}
                </el-link>
              </el-card>
            </el-col>
            <el-col :span="17" :class="GROUP_TYPE_REVERSE[group.auth].name">
              <el-card
                :body-style="{ padding: '0px' }"
                style="border-radius: 10px; height: 170px"
              >
                <div slot="header" style="height: 24px">
                  <a class="group-name" @click="$emit('open-group', group.id)">
                    <Marquee :val="group.name" :id="group.id"></Marquee>
                  </a>
                </div>
                <div class="group-brief">
                  <span>{{ group.brief }}</span>
                </div>
                <el-divider></el-divider>
                <div style="font-size: 16px; padding: 3.5px">
                  <span>
                    <i class="el-icon-user-solid"></i>
                    <i class="el-icon-close">{{ group.memberCount }}</i>
                    <el-tooltip
                      :content="$t('m.' + GROUP_TYPE_REVERSE[group.auth].tips)"
                    >
                      <el-tag
                        class="group-auth"
                        size="medium"
                        :type="GROUP_TYPE_REVERSE[group.auth].color"
                        effect="plain"
                        @click="$emit('filter-auth', group.auth)"
                      >
                        {{
                          $t('m.Group_' + GROUP_TYPE_REVERSE[group.auth].name)
                        }}
                      </el-tag>
                    </el-tooltip>
                    <el-tooltip :content="$t('m.Group_Hidden_Tips')">
                      <el-tag
                        v-if="!group.visible"
                        class="group-auth"
                        size="medium"
                        type="primary"
                        effect="plain"
                      >
                        {{ $t('m.Group_Hidden') }}
                      </el-tag>
                    </el-tooltip>
                  </span>
                  <span style="float: right">
                    <i class="el-icon-time">
                      {{ group.gmtCreate | localtime((format = 'YYYY-MM-DD')) }}
                    </i>
                  </span>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </el-col>
      </el-row>
</template>

<script>
import { GROUP_TYPE_REVERSE } from '@/common/constants';
import Marquee from '@/components/oj/common/Marquee';
export default {
  components: { Marquee },
  props: { groupList: { type: Array, required: true } },
  data() {
    return { GROUP_TYPE_REVERSE, defaultAvatar: require('@/assets/default.jpg') };
  },
};
</script>

<style scoped>
.group-name {
  font-size: 1.15rem;
  font-weight: 600;
}

/deep/ .Public .el-card {
  border-color: rgba(103, 194, 58, 0.5);
}

/deep/ .Public .el-card__header {
  background-color: rgba(103, 194, 58, 0.2);
}

.Public .group-name {
  color: rgb(103, 194, 58);
}

/deep/ .Protected .el-card {
  border-color: rgba(230, 162, 60, 0.5);
}

/deep/ .Protected .el-card__header {
  background-color: rgba(230, 162, 60, 0.2);
}

.Protected .group-name {
  color: rgb(230, 162, 60);
}

/deep/ .Private .el-card {
  border-color: rgba(245, 108, 108, 0.5);
}

/deep/ .Private .el-card__header {
  background-color: rgba(245, 108, 108, 0.2);
}

.Private .group-name {
  color: rgb(245, 108, 108);
}

.group-brief {
  height: 86px;
  font-size: 14px;
  padding: 0 10px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
  overflow: hidden;
}
.group-auth {
  margin-left: 3px;
  margin-top: -1px;
  font-size: 14px;
}
.group-auth:hover {
  cursor: pointer;
}
/deep/ .el-card__header {
  padding: 10px;
  margin-bottom: 4px;
}
/deep/ .el-divider--horizontal {
  margin: 0;
}
</style>
