#!/bin/sh
. `dirname $0`/setenv.sh 

# User may increase Java memory setting(s) if desired:
JVM_ARGS=-Xmx256m

# If going through a proxy, set the proxy host and proxy port below, then uncomment the line
# JVM_ARGS="${JVM_ARGS} -DproxySet=true -Dhttp.proxyHost= -Dhttp.proxyPort="

# Please do not change any of the following lines:
CLASSPATH=`dirname $0`/../jlib/moxy/jsr173_api.jar:\
`dirname $0`/../jlib/moxy/jaxb-api.jar:\
`dirname $0`/../jlib/moxy/activation.jar:\
`dirname $0`/../jlib/moxy/jaxb-impl.jar:\
`dirname $0`/../jlib/moxy/jaxb-xjc.jar
JAVA_ARGS="$@"

${JAVA_HOME}/bin/java ${JVM_ARGS} -cp ${CLASSPATH} \
    com.sun.tools.xjc.XJCFacade ${JAVA_ARGS}

