<template>
  <el-row :gutter="20">
    <el-col :span="24" style="margin-top: 10px; margin-bottom: 10px;">
      <el-card>
        <section>
          <span class="find-group">{{ $t('m.Search_Group') }}</span>
          <vxe-input
            v-model="query.keyword"
            :placeholder="$t('m.Enter_keyword')"
            type="search"
            size="medium"
            style="width:230px; margin-right: 1.7em"
            @keyup.enter.native="filterByKeyword"
            @search-click="filterByKeyword"
          ></vxe-input>
          <el-button
            class="hidden-xs-only"
            type="warning"
            icon="el-icon-search"
            size="small"
            :disabled="!isAuthenticated"
            @click="handleOnlyMine(!query.onlyMine)"
          >
            {{ query.onlyMine ? $t('m.All_Group') : $t('m.My_Group') }}
          </el-button>
          <el-button
            class="hidden-sm-and-up"
            type="warning"
            icon="el-icon-search"
            size="small"
            :disabled="!isAuthenticated"
            @click="handleOnlyMine(!query.onlyMine)"
          ></el-button>
          <el-button
            class="hidden-xs-only"
            type="primary"
            icon="el-icon-plus"
            size="small"
            @click="toCreateGroup"
          >
            {{ $t('m.Create_Group') }}
          </el-button>
          <el-button
            class="hidden-sm-and-up"
            type="primary"
            icon="el-icon-plus"
            size="small"
            @click="toCreateGroup"
          ></el-button>
        </section>
        <section>
          <b class="group-category">{{ $t('m.Group_Auth') }}</b>
          <div>
            <el-tag
              size="medium"
              class="category-item"
              :effect="query.auth ? 'plain' : 'dark'"
              @click="filterByAuth(null)"
            >
              {{ $t('m.All') }}
            </el-tag>
            <el-tag
              size="medium"
              class="category-item"
              v-for="(key, index) in GROUP_TYPE_REVERSE"
              :type="key.color"
              :effect="query.auth == index ? 'dark' : 'plain'"
              :key="index"
              @click="filterByAuth(index)"
            >
              {{ $t('m.Group_' + key.name) }}
            </el-tag>
          </div>
        </section>
      </el-card>
    </el-col>
    <el-col :span="24" v-loading="loading">
      <h3 class="group-section-title">{{ $t('m.Active_Groups') }}</h3>
      <GroupCards :group-list="groupList" @open-group="toGroup" @open-user="toUserHome" @filter-auth="filterByAuth" />
    </el-col>
    <Pagination
      :total="total"
      :page-size="query.limit"
      @on-change="currentChange"
      :current.sync="query.currentPage"
      @on-page-size-change="onPageSizeChange"
      style="margin-top: 10px; margin-bottom: 30px;"
      :layout="'prev, pager, next, sizes'"
      :pageSizes="[9, 15, 30, 60, 120]"
    ></Pagination>
    <el-col :span="24" class="archived-groups">
      <el-collapse v-model="expandedSections">
        <el-collapse-item name="archived">
          <template slot="title">
            <span class="group-section-title"><i class="el-icon-folder-opened"></i> {{ $t('m.Archived_Groups') }}</span>
            <el-tag size="small" type="info" class="archive-count">{{ archivedTotal }}</el-tag>
          </template>
          <div v-loading="archivedLoading">
            <GroupCards v-if="expandedSections.includes('archived')" :group-list="archivedGroupList" @open-group="toGroup" @open-user="toUserHome" @filter-auth="filterByAuth" />
            <Pagination :total="archivedTotal" :page-size="query.limit" :current.sync="archivedPage" @on-change="getArchivedGroupList" :layout="'prev, pager, next'" />
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-col>
    <el-dialog
      :title="$t('m.Create_Group')"
      :visible.sync="showEditGroupDialog"
      :fullscreen="true"
      @open="onOpenEditDialog"
    >
      <el-form label-position="top" :model="group" :rules="rules" ref="group">
        <el-row :gutter="20">
          <el-col :md="12" :xs="24">
            <el-form-item :label="$t('m.Group_Name')" required prop="name">
              <el-input
                v-model="group.name"
                :placeholder="$t('m.Group_Name')"
                class="title-input"
                minlength="5"
                maxlength="25"
                show-word-limit
              >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :md="12" :xs="24">
            <el-form-item
              :label="$t('m.Group_Short_Name')"
              required
              prop="shortName"
            >
              <el-input
                v-model="group.shortName"
                :placeholder="$t('m.Group_Short_Name')"
                class="title-input"
                minlength="5"
                maxlength="10"
                show-word-limit
              >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item :label="$t('m.Group_Brief')" required prop="brief">
              <el-input
                v-model="group.brief"
                :placeholder="$t('m.Group_Brief')"
                class="title-input"
                minlength="5"
                maxlength="50"
                show-word-limit
              >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Group_Auth')" required prop="auth">
              <el-select v-model="group.auth">
                <el-tooltip
                  :content="$t('m.Group_Public_Tips')"
                  placement="right"
                >
                  <el-option
                    :label="$t('m.Group_Public')"
                    :value="1"
                  ></el-option>
                </el-tooltip>
                <el-tooltip
                  :content="$t('m.Group_Protected_Tips')"
                  placement="right"
                >
                  <el-option
                    :label="$t('m.Group_Protected')"
                    :value="2"
                  ></el-option>
                </el-tooltip>
                <el-tooltip
                  :content="$t('m.Group_Private_Tips')"
                  placement="right"
                >
                  <el-option
                    :label="$t('m.Group_Private')"
                    :value="3"
                  ></el-option>
                </el-tooltip>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24" v-if="group.auth != 1">
            <el-form-item :label="$t('m.Group_Code')" required prop="code">
              <el-input
                v-model="group.code"
                :placeholder="$t('m.Group_Code')"
                class="title-input"
                minlength="6"
                maxlength="6"
                show-word-limit
              >
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :md="8" :xs="24">
            <el-form-item :label="$t('m.Visible')" required prop="visible">
              <el-switch
                v-model="group.visible"
                :active-text="$t('m.Group_Visible')"
                :inactive-text="$t('m.Group_Not_Visible')"
              >
              </el-switch>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item
              :label="$t('m.Group_Description')"
              required
              prop="description"
            >
              <Editor :value.sync="group.description"></Editor>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button type="danger" @click.native="showEditGroupDialog = false">{{
          $t('m.Cancel')
        }}</el-button>
        <el-button type="primary" @click.native="submitGroup">{{
          $t('m.OK')
        }}</el-button>
      </span>
    </el-dialog>
  </el-row>
</template>

<script>
import { GROUP_TYPE_REVERSE } from '@/common/constants';
import api from '@/common/api';
import mMessage from '@/common/message';
import { mapGetters } from 'vuex';
import Pagination from '@/components/oj/common/Pagination';
import Editor from '@/components/admin/Editor';
import GroupCards from '@/components/oj/group/GroupCards';
export default {
  name: 'GroupList',
  components: {
    Pagination,
    Editor,
    GroupCards,
  },
  data() {
    return {
      showEditGroupDialog: false,
      query: {
        keyword: '',
        auth: 0,
        currentPage:1,
        limit:15,
        onlyMine: false,
      },
      total: 0,
      archivedTotal: 0,
      archivedPage: 1,
      archivedGroupList: [],
      archivedLoading: false,
      expandedSections: [],
      group: {
        avatar: '',
        name: '',
        shortName: '',
        brief: '',
        description: '',
        owner: '',
        auth: 1,
        visible: true,
      },
      rules: {
        name: [
          {
            required: true,
            message: this.$i18n.t('m.Group_Name_Check_Required'),
            trigger: 'blur',
          },
          {
            min: 5,
            max: 25,
            message: this.$i18n.t('m.Group_Name_Check_Min_Max'),
            trigger: 'blur',
          },
        ],
        shortName: [
          {
            required: true,
            message: this.$i18n.t('m.Group_Short_Name_Check_Required'),
            trigger: 'blur',
          },
          {
            min: 5,
            max: 10,
            message: this.$i18n.t('m.Group_Short_Name_Check_Min_Max'),
            trigger: 'blur',
          },
        ],
        brief: [
          {
            required: true,
            message: this.$i18n.t('m.Group_Brief_Check_Required'),
            trigger: 'blur',
          },
          {
            min: 5,
            max: 50,
            message: this.$i18n.t('m.Group_Brief_Check_Min_Max'),
            trigger: 'blur',
          },
        ],
        code: [
          {
            required: true,
            message: this.$i18n.t('m.Group_Code_Check_Required'),
            trigger: 'blur',
          },
          {
            min: 6,
            max: 6,
            message: this.$i18n.t('m.Group_Code_Check_Min_Max'),
            trigger: 'blur',
          },
        ],
        auth: [
          {
            required: true,
            message: this.$i18n.t('m.Group_Auth_Check_Required'),
            trigger: 'blur',
          },
        ],
        description: [
          {
            required: true,
            message: this.$i18n.t('m.Group_Description_Check_Required'),
            trigger: 'blur',
          },
          {
            min: 5,
            max: 1000,
            message: this.$i18n.t('m.Group_Description_Check_Min_Max'),
            trigger: 'blur',
          },
        ],
      },
      backupGroup: null,
      groupList: [],
      loading: false,
    };
  },
  mounted() {
    this.init();
  },
  created() {
    this.GROUP_TYPE_REVERSE = Object.assign({}, GROUP_TYPE_REVERSE);
  },
  methods: {
    init() {
      let route = this.$route.query;
      this.query.auth = route.auth;
      this.query.keyword = route.keyword || '';
      this.query.onlyMine = route.onlyMine + '' == 'true' ? true : false;
      this.query.currentPage = Number(route.currentPage) || 1;
      this.query.limit = Number(route.limit) || 15;
      this.archivedPage = 1;
      this.getGroupList();
      this.getArchivedGroupList();
    },
    onPageSizeChange(pageSize) {
      this.query.currentPage = 1;
      this.query.limit = pageSize;
      this.handleRouter();
    },
    currentChange(page) {
      this.query.currentPage = page;
      this.handleRouter();
    },
    filterByKeyword() {
      this.query.currentPage = 1;
      this.handleRouter();
    },
    filterByAuth(auth) {
      this.query.currentPage = 1;
      this.query.auth = auth;
      this.handleRouter();
    },
    handleOnlyMine(onlyMine) {
      this.query.currentPage = 1;
      this.query.onlyMine = onlyMine;
      this.handleRouter();
    },
    handleAuth(auth) {
      this.query.currentPage = 1;
      this.query.auth = auth;
      this.handleRouter();
    },
    handleRouter() {
      this.$router.push({
        path: this.$route.path,
        query: this.query,
      });
    },
    getGroupList() {
      this.loading = true;
      api.getGroupList(this.query.currentPage, this.query.limit, this.query).then(
        (res) => {
          this.groupList = res.data.data.records;
          this.total = res.data.data.total;
          this.loading = false;
        },
        (err) => {
          this.loading = false;
        }
      );
    },
    getArchivedGroupList(page = this.archivedPage) {
      this.archivedPage = page;
      this.archivedLoading = true;
      const query = { ...this.query, currentPage: page, archived: true };
      api.getGroupList(page, this.query.limit, query).then(
        (res) => {
          this.archivedGroupList = res.data.data.records;
          this.archivedTotal = res.data.data.total;
          this.archivedLoading = false;
        },
        () => { this.archivedLoading = false; }
      );
    },
    toGroup(groupID) {
      if (!this.isAuthenticated) {
        mMessage.warning(this.$i18n.t('m.Please_login_first'));
        this.$store.dispatch('changeModalStatus', { visible: true });
      } else {
        this.$router.push({
          name: 'GroupDetails',
          params: { groupID: groupID },
        });
      }
    },
    toUserHome(username) {
      this.$router.push({
        name: 'UserHome',
        query: { username: username },
      });
    },
    toCreateGroup() {
      if (!this.isAuthenticated) {
        mMessage.warning(this.$i18n.t('m.Please_login_first'));
        this.$store.dispatch('changeModalStatus', { visible: true });
      } else {
        if (this.backupGroup) {
          this.group = this.backupGroup;
        } else {
          this.group = {
            id: null,
            avatar: '',
            name: '',
            shortName: '',
            brief: '',
            description: '',
            owner: '',
            auth: 1,
            code: '',
            status: null,
            visible: true,
          };
        }
        this.showEditGroupDialog = true;
      }
    },
    onOpenEditDialog() {
      setTimeout(() => {
        if (document.createEvent) {
          let event = document.createEvent('HTMLEvents');
          event.initEvent('resize', true, true);
          window.dispatchEvent(event);
        } else if (document.createEventObject) {
          window.fireEvent('onresize');
        }
      }, 0);
    },
    submitGroup() {
      this.$refs['group'].validate((valid) => {
        if (valid) {
          let group = Object.assign({}, this.group);
          api.addGroup(group).then((res) => {
            mMessage.success(this.$i18n.t('m.Create_Successfully'));
            this.showEditGroupDialog = false;
            this.init();
          });
        }
      });
    },
  },
  computed: {
    ...mapGetters(['isAuthenticated']),
  },
  watch: {
    $route(newVal, oldVal) {
      if (newVal !== oldVal) {
        this.init();
      }
    },
    group(newVal, oldVal) {
      if (newVal != oldVal) {
        this.backupGroup = this.group;
      }
    },
  },
};
</script>

<style scoped>
section {
  display: flex;
  min-height: 3em;
  margin-bottom: 1em;
  align-items: center;
}
.find-group {
  margin-right: 1em;
  white-space: nowrap;
  font-size: 1.7em;
  margin-top: 0;
  font-family: inherit;
  font-weight: bold;
  line-height: 1.2;
  color: inherit;
}
.group-category {
  margin-right: 1.5em;
  font-weight: bolder;
  white-space: nowrap;
  font-size: 16px;
  margin-top: 8px;
}
.category-item {
  margin-right: 1em;
  margin-top: 0.5em;
  font-size: 14px;
}
.category-item:hover {
  cursor: pointer;
}
.group-section-title { font-size: 16px; font-weight: 600; color: #606266; margin: 10px 0; }
.archived-groups { margin-bottom: 30px; }
.archive-count { margin-left: 10px; }
@media (max-width: 767px) {
  section { flex-wrap: wrap; gap: 8px; }
  .find-group { font-size: 1.3em; }
}
</style>
