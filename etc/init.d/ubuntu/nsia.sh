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
NAME=nsia
DESC='ThreatFactor NSIA'
INSTALL_DIR=/opt/nsia
PIDFILE=$INSTALL_DIR/pid

test -x "$DAEMON" || exit 0

# Include NSIA defaults if available
if [ -f /etc/default/nsia ] ; then
    . /etc/default/nsia
fi

set -e

. /lib/lsb/init-functions

case "$1" in
  start)
    echo -n "Starting $DESC: "
    start-stop-daemon -d $INSTALL_DIR -b --start --quiet --make-pidfile --pidfile $PIDFILE \
        --exec "$DAEMON" -- $DAEMON_OPTS || true
    echo "$NAME."
    ;;
  stop)
    echo -n "Stopping $DESC: "
    start-stop-daemon -d $INSTALL_DIR --stop --quiet --pidfile $PIDFILE \
        --exec "$DAEMON" || true
    echo "$NAME."
    ;;
  restart|force-reload)
    echo -n "Restarting $DESC: "
    start-stop-daemon -d $INSTALL_DIR --stop --quiet --pidfile \
        $PIDFILE --exec "$DAEMON" || true
    sleep 1
    start-stop-daemon -d $INSTALL_DIR -b --start --quiet --make-pidfile --pidfile \
        $PIDFILE --exec "$DAEMON" -- $DAEMON_OPTS || true
    echo "$NAME."
    ;;
  reload)
      echo -n "Reloading $DESC configuration: "
      start-stop-daemon -d $INSTALL_DIR --stop --signal HUP --quiet --pidfile $PIDFILE \
          --exec "$DAEMON" || true
      echo "$NAME."
      ;;
  status)
      status_of_proc -p $PIDFILE "$DAEMON" scanner_controller && exit 0 || exit $?
      ;;
  *)
    N=/etc/init.d/$NAME
    echo "Usage: $N {start|stop|restart|reload|force-reload|status}" >&2
    exit 1
    ;;
esac

exit 0