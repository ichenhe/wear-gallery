<template>
  <div id="app">
    <Header />
    <Content />
  </div>
</template>

<script>
import Header from "./components/Header.vue";
import Content from "./components/Content.vue";

export default {
  name: "app",
  components: {
    Header,
    Content,
  },
};
</script>

<style>
#app {
  font-family: "Avenir", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
}
</style>
