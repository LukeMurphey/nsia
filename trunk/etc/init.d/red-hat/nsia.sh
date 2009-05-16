#!/bin/bash
#
#       /etc/rc.d/init.d/nsia
# nsia      This shell script manages the starting and stopping
#           an NSIA instance 
#
# Author: Luke Murphey luke@threatfactor.com
#
# chkconfig: 2345 13 87
# description: NSIA (Network System Integrity Analysis) is a system designed to \
# monitor websites for intrusions, errors, data leaks and other potential problems.

# Source function library.
. /etc/init.d/functions

INSTALL_DIR=/opt/nsia
PIDFILE=$INSTALL_DIR/pid
STARTPIDFILE=$INSTALL_DIR/startpid

if [ -f /etc/sysconfig/nsia ]; then
        . /etc/sysconfig/nsia
fi


start() {
        echo -n "Starting NSIA: "
        if [ -f $STARTPIDFILE ]; then
                PID=`cat $STARTPIDFILE`
                echo NSIA already running: $PID
                exit 2;
        elif [ -f $PIDFILE ]; then
                PID=`cat $PIDFILE`
                echo NSIA already running: $PID
                exit 2;
        else
                cd $INSTALL_DIR
                daemon java -jar nsia.jar $OPTIONS
                RETVAL=$?
                echo
                [ $RETVAL -eq 0 ] && touch /var/lock/subsys/nsia
                return $RETVAL
        fi

}

stop() {
        echo -n "Shutting down ThreatFactor NSIA: "
        echo
        if [ -f $STARTPIDFILE ]; then
                PID=`cat $STARTPIDFILE`
        elif [ -f $PIDFILE ]; then
                PID=`cat $PIDFILE`
        fi
        kill pid $PID
        # killproc nsia
        echo
        rm -f /var/lock/subsys/nsia
        return 0
}

case "$1" in
    start)
        start
        ;;
    stop)
        stop
        ;;
    status)
        status nsia
        ;;
    restart)
        stop
        start
        ;;
    *)
        echo "Usage:  {start|stop|status|restart}"
        exit 1
        ;;
esac
exit $?