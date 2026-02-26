# 设置 JAVA_HOME 为本机 JDK 17（Eclipse Adoptium），然后启动 Spring Boot
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot"
& "$PSScriptRoot\mvnw.cmd" spring-boot:run @args
