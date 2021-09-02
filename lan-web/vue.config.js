module.exports = {
  publicPath: "./",
  productionSourceMap: false,
  css: {
    sourceMap: false,
  },
  pluginOptions: {
    i18n: {
      locale: "en",
      fallbackLocale: "en",
      localeDir: "locales",
      enableInSFC: false,
    },
  },
};
