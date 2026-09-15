const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  lintOnSave: false,
  devServer: {
    port: 8082,
    historyApiFallback: true,
    client: {
      overlay: {
        // Element Plus 表格/标签页布局测量偶发 ResizeObserver 告警（浏览器正常行为，
        // 非应用错误），开发浮层忽略它，避免全屏遮罩挡住页面
        runtimeErrors: error => {
          const ignore = [
            'ResizeObserver loop limit exceeded',
            'ResizeObserver loop completed with undelivered notifications'
          ]
          return !ignore.some(msg => (error && error.message || '').includes(msg))
        }
      }
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8888',
        changeOrigin: true,
        pathRewrite: { '^/api': '' }
      }
    }
  }
})
