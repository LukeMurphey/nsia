#! /bin/sh

### BEGIN INIT INFO
# Provides:          nsia
# Required-Start:    $all
# Required-Stop:     $all
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: starts the NSIA scanner
# Description:       starts nsia using start-stop-daemon
### END INIT INFO

PATH=/usr/local/sbin:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin
DAEMON='/usr/bin/java'
DAEMON_OPTS='-jar /opt/nsia/bin/nsia.jar -s'

#The following is used for debugging
#DAEMON_OPTS='-XX:+HeapDumpOnOutOfMemoryError -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=1044 -jar /opt/nsia/bin/nsia.jar -s'

NAME=nsia
DESC='ThreatFactor NSIA'
INSTALL_DIR=/opt/nsia
PIDFILE=$INSTALL_DIR/var/pid

test -x "$DAEMON" || exit 0

# Include NSIA defaults if available
if [ -f /etc/default/nsia ] ; then
    . /etc/default/nsia
fi

set -e

. /lib/lsb/init-functions

case "$1" in
  start)
    log_daemon_msg "Starting $DESC" ":"
    start-stop-daemon -d $INSTALL_DIR/bin -b --start --quiet --make-pidfile --pidfile $PIDFILE \
        --exec "$DAEMON" -- $DAEMON_OPTS || true
    ret=$?
    log_end_msg $ret
    ;;
  stop)
    log_daemon_msg "Stopping $DESC" ":"
    start-stop-daemon -d $INSTALL_DIR/bin --stop --quiet --pidfile $PIDFILE \
        --exec "$DAEMON" || true
    log_end_msg $?
    ;;
  restart|force-reload)
    $0 stop && $0 start
    ;;
  reload)
      $0 stop && $0 start
      ;;
  status)
      status_of_proc -p $PIDFILE "$DAEMON" "$NAME" && exit 0 || exit $?
      ;;
  *)
    N=/etc/init.d/$NAME
    echo "Usage: $N {start|stop|restart|reload|force-reload|status}" >&2
    exit 1
    ;;
esac

exit 0