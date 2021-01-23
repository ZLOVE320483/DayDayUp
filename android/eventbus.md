## EventBus源码解析

### 1. EventBus概述
EventBus是一种用于Android的事件发布-订阅总线框架，由GreenRobot开发，Gihub地址是：[EventBus](https://github.com/greenrobot/EventBus)。它简化了应用程序内各个组件之间进行通信的复杂度，尤其是碎片之间进行通信的问题，可以避免由于使用广播通信而带来的诸多不便。

