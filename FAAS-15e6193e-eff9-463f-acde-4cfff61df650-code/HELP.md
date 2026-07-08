# 开发业务逻辑
    请在com.alibaba.work.faas.FaasEntry.java文件里开始开发你的业务逻辑
# 本地测试
    点击IDE右上角的Run Java 或者 Debug Java, 主函数选择com.alibaba.work.faas.MainApplication#main, 将在本地运行, 您将看到Spring Boot的输出信息, 直到看到 start up success
    运行成功后, 点击 Terminal -> new Terminal , 输入 curl -H "Content-Type: application/json" -X POST -d '{"inputs":{"title":"test"}}' 127.0.0.1:9000/invoke
# 构建部署jar包
    请点击Terminal -> new Terminal , 打开终端
    在终端内执行 ./builder.sh
    当终端内maven输出Build Success时, 表示构建成功
# 部署
    构建jar包成功后，点击IDE上的“部署代码”按钮即可将构建的jar包部署FC
# 测试
    在宜搭连接器工厂内点击Faas连接器详情里的“测试”，输入参数点击“调用”

# 如何将老Java Faas连接器导出的代码迁移到新Java FC IDE内(如果您有这个需求的话)
    本工程附带了2个能将class字节码文件转成.java源文件的工具, 用于将您之前的无法显示IDE编辑器的老Java Faas连接器导出的字节码转换成java源码
    首先导出老Java Faas连接器的代码, 然后解压导出的压缩包, 右键点击本IDE的工程目录文件夹并选择上传文件将存有您的业务逻辑的java字节码文件上传到当前IDE
    以下2种反编译方式可任选其一
    1. 使用cfr反编译
        打开Terminal执行 java -jar cfr-0.152.jar 你的字节码文件路径.class --outputdir ./decompiledJavaSrc
        class文件反编译得到的java源文件将输出到当前目录下的decompiledJavaSrc目录内(不存在则会自动新建此目录)
    2. 使用procyon反编译
        打开Terminal执行 java -jar procyon-decompiler-0.6.0.jar 你的字节码文件路径.class  --output-directory  ./decompiledJavaSrc
        class文件反编译得到的java源文件将输出到当前目录下的decompiledJavaSrc目录内(不存在则会自动新建此目录)
    工具是github上开源的: https://github.com/leibnitz27/cfr , https://github.com/mstrobel/procyon/tree/0.6-prerelease