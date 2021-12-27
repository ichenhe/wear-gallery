import Vue from "vue";
import {
  Button,
  Row,
  Col,
  Upload,
  Progress,
  Message,
  Dropdown,
  DropdownMenu,
  DropdownItem,
} from "element-ui";

Vue.use(Row);
Vue.use(Col);
Vue.use(Button);
Vue.use(Upload);
Vue.use(Progress);
Vue.use(Dropdown);
Vue.use(DropdownMenu);
Vue.use(DropdownItem);
Vue.prototype.$message = Message;
