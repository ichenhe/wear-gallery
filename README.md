<p align="center"><img src="https://i.loli.net/2020/05/02/hXfD1v6wEgLuUtW.png" /></p>
<h1 align="center">腕间图库 WearGallery</h1>
<p align="center">这也许是最棒的开源手表图库应用 (๑•ᴗ•๑)</p>
<p align="center">
   <a href="https://github.com/liangchenhe55/wear-gallery/releases"><img src="https://img.shields.io/github/release-pre/liangchenhe55/wear-gallery.svg?style=flat-square"></a>
   <a target="_blank" href="https://play.google.com/store/apps/details?id=cc.chenhe.weargallery"><img src="https://img.shields.io/badge/download-play%20store-green.svg?style=flat-square"></a>
   <a href="https://github.com/liangchenhe55/wear-gallery/blob/master/LICENSE"><img src="https://img.shields.io/github/license/liangchenhe55/wear-gallery.svg?style=flat-square"></a>
</p>

## 立即使用

### 如何安装

- [Google Play](https://play.google.com/store/apps/details?id=cc.chenhe.weargallery)
- [Release](https://github.com/liangchenhe55/wear-gallery/releases)

现在 Wear OS 与 Ticwear 均不再支持应用同步，你必须前往**手表端**的应用商店安装，并确保手表与手机的腕间图库版本一致。

> 若手表端没有应用商店，或找不到匹配的版本，那么需要前往[发布页面](https://github.com/liangchenhe55/wear-gallery/releases)下载安装包，使用 adb 或其他方式进行安装。

### 支持设备

> 从 v6.0.0-preview 版本开始手机端禁止一切华为设备使用，原因见[这里](https://github.com/liangchenhe55/wear-gallery/wiki/huawei_zh)；手表端每次打开会弹出提示，确认后可继续使用。

**支持的手表操作系统**（配合 Android 手机客户端可实时预览图片）：

- Wear OS
- Ticwear

**非官方支持的手表**（也许能用，但无法做到实时预览，只安装手表端使用局域网传输即可）：

- OPPO 手表
- 其他基于 Android 系统魔改的手表

**不支持的手表**（无法安装）：

- 三星 Tizen 系统
- 华米、小米 Color 等轻智能手表
- Apple Watch

## 纯种自我介绍

腕间图库是一个主要运行于 Wear OS 的图库应用，同时兼容 Ticwear。也有着配套的 Android 手机应用，但这不是必须的。目前已收获 20w+ 累计下载量，帮助无数学生逃离挂蝌魔掌（逃

有着以下特性：

- 实时显示手机图片（仅限 Android 手机，需要配套应用）
- 高清显示
- 支持双击缩放、按钮旋转
- 支持自定义表盘（感谢 COT表盘 提供技术支持）
- 支持局域网传输（IOS 手机可用）
- 支持保存离线查看

## 应用架构

从 v6.0.0-preview(220600000) 版本开始项目已完全重构，包括以下变化：

- 迁移至 Kotlin。
- 迁移至 AndroidX。
- 使用 [AAC](https://developer.android.com/topic/libraries/architecture) 组件与 MVVM 架构。

**欢迎 ISSUE，欢迎 PR，欢迎一切贡献~**

## 关于

高中时期开发的，~~现在已经是个大学狗啦~~，现在准备考研啦。

- 欢迎关注我的微博 [@0晨鹤0](https://weibo.com/liangchenhe55)
- 欢迎订阅腕间图库 [TG 频道](https://t.me/weargallery_news)
- 欢迎加入 QQ 群 [549321774](https://jq.qq.com/?_wv=1027&k=5lUanq2)

**想请我喝杯奶茶嘛 (≧∀≦)ゞ**

COT表盘是我作为联合创始人的创业项目，内有很多精品低价原创表盘，跨平台漫游。强大的DIY与分享功能快马加鞭开发中，欢迎使用，手表端应用商店搜索「COT表盘」即可安装。

同时欢迎有兴趣的同学签约设计师，我们有目前业内最高的分成比例，并且无强制指标。开心第一工作第二 ~\(≧▽≦)/~

![](https://i.loli.net/2018/12/05/5c0796f667cf3.png)

## Licence

本项目在 AGPL-3.0 许可证下开源，详情参阅 [LICENSE 文件](https://github.com/liangchenhe55/wear-gallery/blob/master/LICENSE)。

**但请注意，您必须同时遵守下面的额外限制：**
**However, please note that you must also observe the following additional restrictions:**

- 基于（修改）本项目所开发的新项目，必须保留本项目对包括但不限于特定设备、地域、人群的限制，包括源代码和二进制都必须遵守此限制。
- Based on (modifying) the new project developed by this project must retain the limitations of this project including but not limited to specific devices, regions, and crowds, including source code and binary must comply with this restriction.
- 基于（修改）本项目所开发的新项目，其许可证必须包含所有这里列出的额外限制条件。
- Based on (modifying) the new project developed by this project, the license must include all additional restrictions listed here.