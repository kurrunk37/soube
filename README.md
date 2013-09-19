# 关于soube

这是一个基于dropbox的个人博客引擎，可以轻松创建一个简洁的博客。

* [下载war文件](http://pan.baidu.com/share/link?shareid=2749663359&uk=2366555814 "soube war")
* 默认样式可以参照这个博客:[kurrunk](http://blog.kurrunk.com "kurrunk")

## 运行环境

* 程序运行在java servlet环境，推荐使用tomcat、jetty。
* 需要一个数据库，目前仅支持mysql
* 需要您有dropbox帐号，并有开发者的key

## 部署步骤

1. 部署war文件到环境中
2. 设置环境变量 (参见下方)
3. 启动jetty/tomcat后，通过浏览器访问http://yourhostname/admin(会要求登录dropbox);
4. 启动mysql服务
5. 点击"工具->初始化数据库"按钮，这个会在您的db中建一个table;
6. 点击同步按钮，这样会把dropbox的\*.md文件同步到db中，站点部署就完成了

## 配置

可以通过设置系统环境变量或JVM环境变量两种方式来配置您的站点

|| *系统环境变量*		||	*JVM环境变量*		||	*说明*														||  
|| DB_SUBNAME				||	db.subname			|| 	db连接，如://127.0.0.1:3306/soube	||  
|| DB_USER					||	db.user					||	db用户名 ||  
|| DB_PASSWORD			||	db.password			||	db密码	||  
|| DROPBOX_KEY			||	dropbox.key			||	dropbox key	||  
|| DROPBOX_SECRET		||	dropbox.secret	||	dropbox.secret	||  
|| DROPBOX_UID			||	dropbox.uid			||	dropbox帐号白名单，用","分隔多个uid	||  
|| SITE_NAME				||	site.name				||	blog主题，将会显示在网页的title上	||  
|| SITE_DESCIPTION	||	site.desciption	||	blog简介	||

### 常见appengine平台的配置方法

#### jelastic

1. 创建environment,选择jetty,数据库用mariadb
2. 上传war文件
3. jetty->config:   
   打开server/variables.conf，内容如下格式:   
	-Ddropbox.key=   
	-Ddropbox.secret=   
	-Ddropbox.uid= #dropbox的uid,这里是同步文章用的   ,多个id请用英文逗号分隔
	\#你的数据库 
	-Ddb.subname=//mariadb-*.jelastic.servint.net/*?characterEncoding=UTF-8  
	-Ddb.user=***  
	-Ddb.password=***  
	\#你的站点信息  
	-Dsite.name=myblog  
	-Dsite.desciption=这是我的博客简介  

#### appfog

1. 创建app并绑定一个mysql数据库
2. 上传war文件
3. 配置环境变量(Env Variables):  
   * DROPBOX_KEY   
   * DROPBOX_SECRET   
   * DROPBOX_UID   
   * SITE_NAME   
   * SITE_DESCIPTION   

## 高级使用

soube支持更灵活的皮肤，也支持多博客(需要您有不同的域名)。但这样需要您动一下手，在本地编辑源文件来实现。

您需要 [Leiningen][1] 1.7.0 或者更高版本。

[1]: https://github.com/technomancy/leiningen

编辑`src/soube/config.clj`可以实现多个blog。

在`resources/yourhostname/`可以自定义站点样式。

### 本地运行

在本地启动你的博客，运行:

    lein ring server

### 生成war

	lein ring uberwar soube.war


Copyright © 2013 FIXME
