`npm install --registry=https://registry.npm.taobao.org`
关于新谷粒P16的前端项目使用npm install报错的问题，首先确保安装了python3.0以上版本，并配置全局变量
其次大部分错误是报node-sass4.9.0安装失败。
执行以下步骤可以完美解决
首先把项目文件夹下的package.json里面的node-sass4.9.0改成4.9.2（不改可能也没关系，不过我改了，防止踩坑）
然后项目文件夹下打开cmd命令窗口（和Visual Studio Code的终端命令是一样的）
执行：
`npm i node-sass --sass_binary_site=https://npm.taobao.org/mirrors/node-sass/`
执行成功看看有没有报错，如果没报错执行下面命令
`nnpm install --registry=https://registry.npm.taobao.org` ，
没报错就是安装成功，然后使用`npm run dev` 就ok了
注：这么做得原理就是先单独从淘宝镜像吧nod-sass下载下来，然后再进行编译，因为这句命令好像是不成功的，（npm config set registry http://registry.npm.taobao.org/），默认从github下载，导致报错的
如果之前安装失败的。先清理 缓存
清理缓存：`npm rebuild node-sass`
 `npm uninstall node-sass`

## renren-fast-vue
- renren-fast-vue基于vue、element-ui构建开发，实现[renren-fast](https://gitee.com/renrenio/renren-fast)后台管理前端功能，提供一套更优的前端解决方案
- 前后端分离，通过token进行数据交互，可独立部署
- 主题定制，通过scss变量统一一站式定制
- 动态菜单，通过菜单管理统一管理访问路由
- 数据切换，通过mock配置对接口数据／mock模拟数据进行切换
- 发布时，可动态配置CDN静态资源／切换新旧版本
- 演示地址：[http://demo.open.renren.io/renren-fast](http://demo.open.renren.io/renren-fast) (账号密码：admin/admin)

![输入图片说明](https://images.gitee.com/uploads/images/2019/0305/133529_ff15f192_63154.png "01.png")
![输入图片说明](https://images.gitee.com/uploads/images/2019/0305/133537_7a1b2d85_63154.png "02.png")


## 说明文档
项目开发、部署等说明都在[wiki](https://github.com/renrenio/renren-fast-vue/wiki)中。


## 更新日志
每个版本的详细更改都记录在[release notes](https://github.com/renrenio/renren-fast-vue/releases)中。
