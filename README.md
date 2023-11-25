# BarbatosStoryBook

记录过去，展望未来。

## 描述

时间胶囊插件，允许玩家将故事写到书中，签名成成书后保存到数据库（和文件）中。

* 使用 MySQL 存储（也可选的同时向磁盘存储一份）
* 存储以下信息：书本 ItemStack 源数据，书标题，书作者，书内容（JSON+去除格式），时间，服务器名称
* 打开书时显示基本信息
* 支持重新打开&试试手气随机选书

## 命令

* /storybook - 将正在看着的容器设置为故事箱 - 权限：`barabtosstorybook.admin`
* /randomstorybook - 随机选一本故事书看（试试手气！） - 权限：`barabtosstorybook.use`
* /openspecificstorybook <id> - 打开指定 ID 的故事书 （被 “重新打开” 功能使用） - 权限：`barabtosstorybook.use`

## 演示

https://github.com/RIA-AED/BarbatosStoryBook/assets/30802565/56be1b1e-8400-489d-b88f-435a06600e19

## 配置文件 

```yaml
# 服务器号，会存储在数据库中，标记这本书来自于哪个服务器
server: zeroth
# 是否同时向磁盘写入故事书（false为仅写入到数据库）
also-write-to-file: true
# 数据库 配置信息
database:
  host: localhost
  port: 3306
  database: barbatos-storybook
  user: root
  password: passwd
  prefix: "storybook_"
  usessl: false
  properties:
    connection-timeout: 60000
    validation-timeout: 3000
    idle-timeout: 60000
    login-timeout: 10
    maxLifeTime: 60000
    maximum-pool-size: 8
    minimum-idle: 2
    cachePrepStmts: true
    prepStmtCacheSize: 250
    prepStmtCacheSqlLimit: 2048
    useUnicode: true
    characterEncoding: utf8
    allowPublicKeyRetrieval: true

submitted: "&x&a&6&7&1&1&2你将书轻轻放好并合上了盖子，里面的书本渐渐地化为了闪烁着点点光斑，散落开来..."
success-write-into-database: "&x&4&0&9&6&9&7你感受到有一阵轻风吹过，将你心中的话语带向了未知的远方..."
failure: "&c似乎在处理你的留言时出现了一点错误，联系一下服务器管理员吧！"

openbook: |
  &f===================================
    &8作者: &7%s
    &8标题：&7%s
    &8撰写于：&7%s
  &f===================================
  



```
