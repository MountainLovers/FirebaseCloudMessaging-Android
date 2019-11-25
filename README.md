# SPHINX-DEMO-APP

## 背景

SPHINX是一个口令管理系统，是"SPHINX: A password store that perfectly hides passwords from itself." *2017 IEEE 37th International Conference on Distributed Computing Systems"的复现，使用Firebase Cloud Messaging传递消息，无交互方式。

该系统由两部分组成：

- Client端。请参考👉[sphinx-demo-web]( https://github.com/MountainLovers/sphinx-demo-web ).
- Device端。主体是一个Android Application，在协议过程中，它起到了一个双因子的作用，需要保存一个关键的随机k值，并在登录/注册过程中通过OPRF协议与C端交互，恢复密码。

![](https://github.com/MountainLovers/sphinx-demo-web/blob/master/chrome-extension/brief-protocol.png)

## 使用方法

1. Clone本仓库到本地。
2. 使用Android Studio打开工程。
3. 编译运行。
4. 在准备阶段，点击scan，扫描chrome extension生成的二维码，获取extension的firebase token。
5. 随后无需任何操作。

## 原理

### 协议流程

![](https://github.com/MountainLovers/sphinx-demo-web/blob/master/chrome-extension/protocol.png)

### 逻辑步骤

1. 通过扫描二维码，获取Extension的firebase token。
2. 发送消息给Extension，携带自己的firebase token。
3. 等待注册阶段Extension的k值请求，并返回k。
4. 等待登录阶段Extension传来的alpha，计算beta后返回。

### 项目结构

- zxing目录下是扫码相关的东西。
- MyFirebaseMessagingService.java响应FCM。
- ProtocolClass.java定义了密码学函数等协议需要的内容。

## TODO

1. 添加需要用户确认的模式。

## 声明

本项目仅用于展示，非商业用途。

原型项目Fork于 https://github.com/jirawatee/FirebaseCloudMessaging-Android ，主要是用了它的UI。

扫码部分参考了 https://github.com/yangxch/ScanZxing ，需要使用的是Zxing库。