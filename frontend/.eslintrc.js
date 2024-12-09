module.exports = {
    root: true,
    env: {
      node: true,
    },
    extends: [
      "plugin:vue/vue3-essential",
      "eslint:recommended",
    ],
    parserOptions: {
      parser: "@babel/eslint-parser",
      requireConfigFile: false,
    },
    "rules": {
    "vue/no-unused-vars": "off"  // props를 사용하지 않았을 때 경고를 끄기 위해 추가
  }
  }