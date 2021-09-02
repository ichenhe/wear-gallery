import Vue from "vue";
import "./plugins/element.js";
import App from "./App.vue";
import i18n from "./i18n";
import axios from "axios";
import VueAxios from "vue-axios";

import "./assets/css/common.css";

Vue.use(VueAxios, axios);
Vue.config.productionTip = false;

new Vue({
  i18n,
  render: (h) => h(App),
}).$mount("#app");
