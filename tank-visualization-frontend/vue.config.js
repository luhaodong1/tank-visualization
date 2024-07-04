const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave:false,
  devServer: {
    proxy: {
      '/api': {
        target: 'http://localhost:3602',
        ws: true, //如果要代理 websockets，配置这个参数
        changeOrigin: true, //用于控制代理服务器请求头中的host值；
        // 若changeOrigin: true时发送请求，代理服务器请求头中的host值与后端服务器的host值一样
        // 若changeOrigin: false时发送请求，代理服务器请求头中的host值不变（即：http://localhost:8080）
        // ws 和 changeOrigin的默认值都是true，一般都把changeOrigin设置为true
        pathRewrite: {
          '^/api': ''
        }
      },
    }
  }
})
