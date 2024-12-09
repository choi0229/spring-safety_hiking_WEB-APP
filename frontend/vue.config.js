const { defineConfig } = require('@vue/cli-service');

module.exports = defineConfig({
  transpileDependencies: true,
  outputDir: '../src/main/resources/static',
  devServer: {
    host: 'localhost',  // 사용할 IP 주소 또는 'localhost'
    port: 8080,         // 포트 설정
    client: {
      overlay: false,   // 기존 overlay 옵션을 client.overlay로 변경
    },
    proxy: {
      '/api': {
        target: 'http://localhost:9000', // Spring Boot 서버
        changeOrigin: true,
      }
    }
  }
});
