# 京东价格提醒 APK 项目

这是合法的价格提醒工具：手动添加京东商品、目标价和当前价；当当前价低于目标价时弹出本机通知。它不会伪装新用户，不会绕过京东规则。

## 生成 APK 方法

1. 在 GitHub 新建仓库。
2. 上传本压缩包解压后的全部文件，确保 `.github/workflows/build-apk.yml` 也上传。
3. 打开仓库的 Actions 页面，选择 `Build Android APK`。
4. 点 `Run workflow` 或者上传后自动运行。
5. 构建完成后，在页面底部 Artifacts 下载 `京东价格提醒-debug-apk`。
6. 解压后得到 `app-debug.apk`，发到安卓手机安装即可。

## 使用说明

- 点“添加商品”，输入商品名称、京东链接、目标价、当前价。
- 以后价格变动时点“更新价格”手动录入新价格。
- 当前价低于或等于目标价时，APP 会弹出提醒。

## 说明

由于 ChatGPT 当前运行环境没有 Android SDK、Gradle、aapt/d8/apksigner，所以这里提供完整安卓项目和自动打包流程。
