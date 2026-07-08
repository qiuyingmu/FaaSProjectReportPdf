#!/bin/sh

code_space="/code"

if [ -d "/root/.m2/repository/javax" ]; then
    echo "缓存目录存在"
else
    mkdir -p /root/.m2/repository
    cp ./settings.xml /root/.m2/
    cd /root/.m2/repository
    wget -O yida-faas-java-cache-layer.zip "https://images.devsapp.cn/bin/yida-faas-java-cache-layer.zip"
    unzip -o yida-faas-java-cache-layer.zip
fi

# 小账号模式下面
if [ -d "/mnt/webide_workspace" ]; then
    code_space="/mnt/webide_workspace"
else
    code_space="/code"
fi
echo "code workspace is: $code_space"

cd "$code_space"
mvn package -Dmaven.test.skip=true
rm -rf target/dependency