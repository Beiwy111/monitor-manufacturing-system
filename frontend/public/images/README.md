# 核心能力区图片（6 张）

当前默认使用 `frontend/picture/` 下的图片，在 `frontend/src/config/homeImages.js` 中配置。

| 能力 | 默认文件 | 建议主题 |
|------|----------|----------|
| 1 订单与计划 | picture/5.jpg | 生产计划、车间排产 |
| 2 派工报工 | picture/2.jpg | 显示器装配线 |
| 3 质量检验 | picture/3.jpg | 屏幕检测、质检 |
| 4 仓储物流 | picture/1.jpg | 物料货架、成品仓储 |
| 5 设备安灯 | picture/6.jpg | 设备监控、MES 终端 |
| 6 发货追溯 | picture/4.jpg | 产线、包装发货 |

## 替换方式

**方式一（推荐）：** 直接覆盖 `frontend/picture/` 中对应 jpg 文件，保持文件名不变。

**方式二：** 将新图放到 `public/images/`，修改 `homeImages.js` 中的 import 路径。

**方式三：** 在 `homeImages.js` 里把某张图的 import 改为 `@picture/7.jpg` 等其他已有文件。
