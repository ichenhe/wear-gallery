<template>
  <div class="header">
    <el-row>
      <el-col :lg="24" :xl="24" class="header-content">
        <span class="title">{{ $t("logo") }}</span>
        <el-dropdown
          class="langSelector"
          trigger="click"
          type="primary"
          @command="handleCommand"
        >
          <span class="el-dropdown-link">
            {{ $t("language") }}
            <i class="el-icon-arrow-down el-icon--right" />
          </span>
          <el-dropdown-menu slot="dropdown">
            <el-dropdown-item command="zh">简体中文</el-dropdown-item>
            <el-dropdown-item command="en">English</el-dropdown-item>
          </el-dropdown-menu>
        </el-dropdown>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import Component from "vue-class-component";

@Component
export default class Header extends Vue {
  detectLanguage(): string {
    var nav = window.navigator,
      browserLanguagePropertyKeys: string[] = [
        "language",
        "browserLanguage",
        "systemLanguage",
        "userLanguage",
      ],
      i,
      language;

    if (Array.isArray(nav.languages)) {
      for (i = 0; i < nav.languages.length; i++) {
        language = nav.languages[i];
        if (language && language.length) {
          return language;
        }
      }
    }

    // support for other well known properties in browsers
    for (i = 0; i < browserLanguagePropertyKeys.length; i++) {
      language = nav[browserLanguagePropertyKeys[i]];
      if (language && language.length) {
        return language;
      }
    }
    return "en";
  }

  created(): void {
    let prefer = sessionStorage.getItem("language");
    let language = prefer ? prefer : this.detectLanguage();
    if (language.startsWith("zh")) {
      this.$i18n.locale = "zh";
    } else {
      this.$i18n.locale = "en";
    }
  }

  handleCommand(command: string): void {
    this.$i18n.locale = command;
  }
}
</script>

<style scoped>
.header {
  width: 100%;
  background-color: #ef5650;
}
@media screen and (max-width: 450px) {
  .header-content {
    padding: 20px 20px 20px 20px;
  }
}

@media screen and (min-width: 450px) {
  .header-content {
    padding: 20px 70px 20px 70px;
  }
}
.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.title {
  color: #fff;
  float: left;
  font-size: 22px;
}

.langSelector {
  padding: 6px;
  color: #fff;
  float: right;
}
</style>
