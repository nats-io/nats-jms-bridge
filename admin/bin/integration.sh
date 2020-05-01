#!/usr/bin/env sh

##############################################################################
##
##  nats-bridge-admin start up script for UN*X
##
##############################################################################

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`/.." >/dev/null
APP_HOME="`pwd -P`"
cd "$SAVED" >/dev/null

APP_NAME="nats-bridge-admin"
APP_BASE_NAME=`basename "$0"`

# Add default JVM options here. You can also use JAVA_OPTS and NATS_BRIDGE_ADMIN_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS=""

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "$*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

CLASSPATH=$APP_HOME/lib/nats-bridge-admin-0.3.0-ALPHA1.jar:$APP_HOME/lib/jackson-module-kotlin-2.9.5.jar:$APP_HOME/lib/kotlin-reflect-1.3.71.jar:$APP_HOME/lib/okhttp-4.5.0.jar:$APP_HOME/lib/okio-2.5.0.jar:$APP_HOME/lib/kotlin-stdlib-1.3.71.jar:$APP_HOME/lib/spring-boot-starter-web-2.2.6.RELEASE.jar:$APP_HOME/lib/spring-boot-starter-actuator-2.2.6.RELEASE.jar:$APP_HOME/lib/spring-boot-starter-security-2.2.6.RELEASE.jar:$APP_HOME/lib/spring-boot-starter-json-2.2.6.RELEASE.jar:$APP_HOME/lib/spring-boot-starter-validation-2.2.6.RELEASE.jar:$APP_HOME/lib/spring-boot-starter-2.2.6.RELEASE.jar:$APP_HOME/lib/spring-boot-starter-logging-2.2.6.RELEASE.jar:$APP_HOME/lib/logback-classic-1.2.3.jar:$APP_HOME/lib/springfox-swagger2-2.7.0.jar:$APP_HOME/lib/nats-jms-bridge-0.3.0-ALPHA1.jar:$APP_HOME/lib/springfox-swagger-ui-2.7.0.jar:$APP_HOME/lib/springfox-swagger-common-2.7.0.jar:$APP_HOME/lib/springfox-spring-web-2.7.0.jar:$APP_HOME/lib/swagger-models-1.5.13.jar:$APP_HOME/lib/springfox-schema-2.7.0.jar:$APP_HOME/lib/springfox-spi-2.7.0.jar:$APP_HOME/lib/springfox-core-2.7.0.jar:$APP_HOME/lib/spring-plugin-metadata-1.2.0.RELEASE.jar:$APP_HOME/lib/spring-plugin-core-1.2.0.RELEASE.jar:$APP_HOME/lib/log4j-to-slf4j-2.12.1.jar:$APP_HOME/lib/jul-to-slf4j-1.7.30.jar:$APP_HOME/lib/slf4j-api-1.7.25.jar:$APP_HOME/lib/logstash-logback-encoder-4.11.jar:$APP_HOME/lib/logback-core-1.2.3.jar:$APP_HOME/lib/jackson-module-parameter-names-2.9.5.jar:$APP_HOME/lib/jackson-datatype-jdk8-2.9.5.jar:$APP_HOME/lib/spring-boot-actuator-autoconfigure-2.2.6.RELEASE.jar:$APP_HOME/lib/spring-boot-actuator-2.2.6.RELEASE.jar:$APP_HOME/lib/jackson-datatype-jsr310-2.9.5.jar:$APP_HOME/lib/jackson-dataformat-yaml-2.9.8.jar:$APP_HOME/lib/micrometer-registry-prometheus-1.3.6.jar:$APP_HOME/lib/com.ibm.mq.allclient-9.1.5.0.jar:$APP_HOME/lib/jjwt-impl-0.11.1.jar:$APP_HOME/lib/jjwt-jackson-0.11.1.jar:$APP_HOME/lib/jjwt-api-0.11.1.jar:$APP_HOME/lib/jnats-2.6.7.jar:$APP_HOME/lib/kotlin-stdlib-common-1.3.71.jar:$APP_HOME/lib/annotations-13.0.jar:$APP_HOME/lib/jackson-databind-2.9.5.jar:$APP_HOME/lib/spring-boot-starter-tomcat-2.2.6.RELEASE.jar:$APP_HOME/lib/spring-webmvc-5.2.5.RELEASE.jar:$APP_HOME/lib/spring-security-web-5.2.2.RELEASE.jar:$APP_HOME/lib/spring-web-5.2.5.RELEASE.jar:$APP_HOME/lib/micrometer-core-1.3.6.jar:$APP_HOME/lib/jackson-core-2.9.5.jar:$APP_HOME/lib/jackson-annotations-2.9.0.jar:$APP_HOME/lib/snakeyaml-1.25.jar:$APP_HOME/lib/spring-security-config-5.2.2.RELEASE.jar:$APP_HOME/lib/spring-boot-autoconfigure-2.2.6.RELEASE.jar:$APP_HOME/lib/spring-boot-2.2.6.RELEASE.jar:$APP_HOME/lib/spring-security-core-5.2.2.RELEASE.jar:$APP_HOME/lib/spring-context-5.2.5.RELEASE.jar:$APP_HOME/lib/spring-aop-5.2.5.RELEASE.jar:$APP_HOME/lib/swagger-annotations-1.5.13.jar:$APP_HOME/lib/reflections-0.9.11.jar:$APP_HOME/lib/guava-20.0.jar:$APP_HOME/lib/hibernate-validator-6.0.18.Final.jar:$APP_HOME/lib/classmate-1.5.1.jar:$APP_HOME/lib/mapstruct-1.1.0.Final.jar:$APP_HOME/lib/artemis-jms-client-all-2.11.0.jar:$APP_HOME/lib/simpleclient_common-0.7.0.jar:$APP_HOME/lib/bcpkix-jdk15on-1.64.jar:$APP_HOME/lib/bcprov-jdk15on-1.64.jar:$APP_HOME/lib/javax.jms-api-2.0.1.jar:$APP_HOME/lib/json-20080701.jar:$APP_HOME/lib/eddsa-0.3.0.jar:$APP_HOME/lib/jakarta.annotation-api-1.3.5.jar:$APP_HOME/lib/spring-beans-5.2.5.RELEASE.jar:$APP_HOME/lib/spring-expression-5.2.5.RELEASE.jar:$APP_HOME/lib/spring-core-5.2.5.RELEASE.jar:$APP_HOME/lib/tomcat-embed-websocket-9.0.33.jar:$APP_HOME/lib/tomcat-embed-core-9.0.33.jar:$APP_HOME/lib/tomcat-embed-el-9.0.33.jar:$APP_HOME/lib/jakarta.validation-api-2.0.2.jar:$APP_HOME/lib/HdrHistogram-2.1.11.jar:$APP_HOME/lib/LatencyUtils-2.0.3.jar:$APP_HOME/lib/simpleclient-0.7.0.jar:$APP_HOME/lib/spring-jcl-5.2.5.RELEASE.jar:$APP_HOME/lib/jboss-logging-3.4.1.Final.jar:$APP_HOME/lib/javassist-3.21.0-GA.jar:$APP_HOME/lib/byte-buddy-1.10.8.jar:$APP_HOME/lib/log4j-api-2.12.1.jar

# Determine the Java command to use to start the JVM.
if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVACMD="$JAVA_HOME/jre/sh/java"
    else
        JAVACMD="$JAVA_HOME/bin/java"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "ERROR: JAVA_HOME is set to an invalid directory: $JAVA_HOME

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
    fi
else
    JAVACMD="java"
    which java >/dev/null 2>&1 || die "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.

Please set the JAVA_HOME variable in your environment to match the
location of your Java installation."
fi

# Increase the maximum file descriptors if we can.
if [ "$cygwin" = "false" -a "$darwin" = "false" -a "$nonstop" = "false" ] ; then
    MAX_FD_LIMIT=`ulimit -H -n`
    if [ $? -eq 0 ] ; then
        if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
            MAX_FD="$MAX_FD_LIMIT"
        fi
        ulimit -n $MAX_FD
        if [ $? -ne 0 ] ; then
            warn "Could not set maximum file descriptor limit: $MAX_FD"
        fi
    else
        warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
    fi
fi

# For Darwin, add options to specify how the application appears in the dock
if $darwin; then
    GRADLE_OPTS="$GRADLE_OPTS \"-Xdock:name=$APP_NAME\" \"-Xdock:icon=$APP_HOME/media/gradle.icns\""
fi

# For Cygwin, switch paths to Windows format before running java
if $cygwin ; then
    APP_HOME=`cygpath --path --mixed "$APP_HOME"`
    CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
    JAVACMD=`cygpath --unix "$JAVACMD"`

    # We build the pattern for arguments to be converted via cygpath
    ROOTDIRSRAW=`find -L / -maxdepth 1 -mindepth 1 -type d 2>/dev/null`
    SEP=""
    for dir in $ROOTDIRSRAW ; do
        ROOTDIRS="$ROOTDIRS$SEP$dir"
        SEP="|"
    done
    OURCYGPATTERN="(^($ROOTDIRS))"
    # Add a user-defined pattern to the cygpath arguments
    if [ "$GRADLE_CYGPATTERN" != "" ] ; then
        OURCYGPATTERN="$OURCYGPATTERN|($GRADLE_CYGPATTERN)"
    fi
    # Now convert the arguments - kludge to limit ourselves to /bin/sh
    i=0
    for arg in "$@" ; do
        CHECK=`echo "$arg"|egrep -c "$OURCYGPATTERN" -`
        CHECK2=`echo "$arg"|egrep -c "^-"`                                 ### Determine if an option

        if [ $CHECK -ne 0 ] && [ $CHECK2 -eq 0 ] ; then                    ### Added a condition
            eval `echo args$i`=`cygpath --path --ignore --mixed "$arg"`
        else
            eval `echo args$i`="\"$arg\""
        fi
        i=$((i+1))
    done
    case $i in
        (0) set -- ;;
        (1) set -- "$args0" ;;
        (2) set -- "$args0" "$args1" ;;
        (3) set -- "$args0" "$args1" "$args2" ;;
        (4) set -- "$args0" "$args1" "$args2" "$args3" ;;
        (5) set -- "$args0" "$args1" "$args2" "$args3" "$args4" ;;
        (6) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" ;;
        (7) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" ;;
        (8) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" ;;
        (9) set -- "$args0" "$args1" "$args2" "$args3" "$args4" "$args5" "$args6" "$args7" "$args8" ;;
    esac
fi

# Escape application args
save () {
    for i do printf %s\\n "$i" | sed "s/'/'\\\\''/g;1s/^/'/;\$s/\$/' \\\\/" ; done
    echo " "
}
APP_ARGS=$(save "$@")

# Collect all arguments for the java command, following the shell quoting and substitution rules
eval set -- $DEFAULT_JVM_OPTS $JAVA_OPTS $NATS_BRIDGE_ADMIN_OPTS -classpath "\"$CLASSPATH\"" io.nats.bridge.admin.integration.IntegrationMain "$APP_ARGS"

# by default we should be in the correct project dir, but when run from Finder on Mac, the cwd is wrong
if [ "$(uname)" = "Darwin" ] && [ "$HOME" = "$PWD" ]; then
  cd "$(dirname "$0")"
fi

exec "$JAVACMD" "$@"
