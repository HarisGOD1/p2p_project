@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  app startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and APP_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\app.jar;%APP_HOME%\lib\traversal_utils.jar;%APP_HOME%\lib\jvm-libp2p-1.2.2-RELEASE.jar;%APP_HOME%\lib\kotlinx-coroutines-core-jvm-1.6.4.jar;%APP_HOME%\lib\kotlin-stdlib-jdk8-1.8.0.jar;%APP_HOME%\lib\kotlin-stdlib-jdk7-1.8.0.jar;%APP_HOME%\lib\kotlin-stdlib-1.9.22.jar;%APP_HOME%\lib\netty-all-4.2.13.Final.jar;%APP_HOME%\lib\protobuf-java-4.34.1.jar;%APP_HOME%\lib\log4j-slf4j2-impl-2.26.0.jar;%APP_HOME%\lib\annotations-13.0.jar;%APP_HOME%\lib\guava-33.3.1-jre.jar;%APP_HOME%\lib\pcap4j-packetfactory-static-1.8.2.jar;%APP_HOME%\lib\pcap4j-core-1.8.2.jar;%APP_HOME%\lib\slf4j-api-2.0.17.jar;%APP_HOME%\lib\netty-codec-http-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-native-quic-4.2.13.Final-linux-x86_64.jar;%APP_HOME%\lib\netty-codec-native-quic-4.2.13.Final-linux-aarch_64.jar;%APP_HOME%\lib\netty-codec-native-quic-4.2.13.Final-osx-x86_64.jar;%APP_HOME%\lib\netty-codec-native-quic-4.2.13.Final-osx-aarch_64.jar;%APP_HOME%\lib\netty-codec-native-quic-4.2.13.Final-windows-x86_64.jar;%APP_HOME%\lib\netty-codec-classes-quic-4.2.13.Final.jar;%APP_HOME%\lib\netty-resolver-dns-native-macos-4.2.13.Final-osx-x86_64.jar;%APP_HOME%\lib\netty-resolver-dns-native-macos-4.2.13.Final-osx-aarch_64.jar;%APP_HOME%\lib\netty-resolver-dns-classes-macos-4.2.13.Final.jar;%APP_HOME%\lib\netty-resolver-dns-4.2.13.Final.jar;%APP_HOME%\lib\netty-handler-4.2.13.Final.jar;%APP_HOME%\lib\java-multibase-v1.1.1.jar;%APP_HOME%\lib\noise-java-22.1.0.jar;%APP_HOME%\lib\bcpkix-jdk18on-1.78.1.jar;%APP_HOME%\lib\bctls-jdk18on-1.78.1.jar;%APP_HOME%\lib\bcutil-jdk18on-1.78.1.jar;%APP_HOME%\lib\bcprov-jdk18on-1.78.1.jar;%APP_HOME%\lib\netty-codec-compression-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-dns-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-base-4.2.13.Final.jar;%APP_HOME%\lib\netty-transport-native-epoll-4.2.13.Final-linux-x86_64.jar;%APP_HOME%\lib\netty-transport-native-epoll-4.2.13.Final-linux-aarch_64.jar;%APP_HOME%\lib\netty-transport-native-epoll-4.2.13.Final-linux-riscv64.jar;%APP_HOME%\lib\netty-transport-native-io_uring-4.2.13.Final-linux-x86_64.jar;%APP_HOME%\lib\netty-transport-native-io_uring-4.2.13.Final-linux-aarch_64.jar;%APP_HOME%\lib\netty-transport-native-io_uring-4.2.13.Final-linux-riscv64.jar;%APP_HOME%\lib\netty-transport-native-kqueue-4.2.13.Final-osx-x86_64.jar;%APP_HOME%\lib\netty-transport-native-kqueue-4.2.13.Final-osx-aarch_64.jar;%APP_HOME%\lib\netty-transport-classes-epoll-4.2.13.Final.jar;%APP_HOME%\lib\netty-transport-classes-io_uring-4.2.13.Final.jar;%APP_HOME%\lib\netty-transport-classes-kqueue-4.2.13.Final.jar;%APP_HOME%\lib\netty-transport-native-unix-common-4.2.13.Final.jar;%APP_HOME%\lib\netty-transport-4.2.13.Final.jar;%APP_HOME%\lib\netty-buffer-4.2.13.Final.jar;%APP_HOME%\lib\netty-resolver-4.2.13.Final.jar;%APP_HOME%\lib\netty-common-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-haproxy-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-http2-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-http3-4.2.13.Final.jar;%APP_HOME%\lib\jctools-core-4.0.6.jar;%APP_HOME%\lib\netty-codec-memcache-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-mqtt-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-redis-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-smtp-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-socks-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-stomp-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-xml-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-protobuf-4.2.13.Final.jar;%APP_HOME%\lib\netty-codec-marshalling-4.2.13.Final.jar;%APP_HOME%\lib\netty-handler-proxy-4.2.13.Final.jar;%APP_HOME%\lib\netty-handler-ssl-ocsp-4.2.13.Final.jar;%APP_HOME%\lib\netty-transport-rxtx-4.2.13.Final.jar;%APP_HOME%\lib\netty-transport-sctp-4.2.13.Final.jar;%APP_HOME%\lib\netty-transport-udt-4.2.13.Final.jar;%APP_HOME%\lib\log4j-core-2.26.0.jar;%APP_HOME%\lib\log4j-api-2.26.0.jar;%APP_HOME%\lib\failureaccess-1.0.2.jar;%APP_HOME%\lib\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\checker-qual-3.43.0.jar;%APP_HOME%\lib\error_prone_annotations-2.28.0.jar;%APP_HOME%\lib\j2objc-annotations-3.0.0.jar;%APP_HOME%\lib\jna-5.3.1.jar


@rem Execute app
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %APP_OPTS%  -classpath "%CLASSPATH%" su.kamil.dev.chat.example.ChatterCliKt %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable APP_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%APP_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
