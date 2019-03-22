#!/bin/sh


# This is a quick and dirty, super hackish script to flip the optimization setting in
# truffle. An earlier attempt using environment variables was buggy, and it's not clear
# why/how exactly it should be done properly.
#
# We use textual replacement of the solc optimization setting lines
#
#    enabled: true
# and
#    runs: 0

SETTINGS=`dirname $0`/../../settings.cfg.sh
if ! . "$SETTINGS"; then
        echo Error: Cannot load settings file $SETTINGS - please run setup.sh
        exit 1
fi

if test "$USE_SOLCJS" = yes; then
	echo "Altering solcjs settings... (see USE_SOLCJS in settings.cfg.sh)"
elif test "$USE_SOLCJS" = no; then
	echo "Altering external solc settings... (see USE_SOLCJS in settings.cfg.sh)"
else
	echo Error: USE_SOLCJS not properly configured as yes/no in settings.cfg.sh
	exit 1
fi

BIN_DIR=`dirname $0`

OPT_SETTINGS_FILE="_opt.txt"

usage() {
	echo "Usage: truffle-optimization.setting.sh [print(opt)] [on|off] [runs-level(opt)]" 
	echo "       Edits node_nodules/bin/.truffle optimization settings."
	echo "       off                      - disable"
	echo "       on [runs-level]          - enable at specified runs level"
	echo "       off/on with print option - do not perform changes, only print them to $OPT_SETTINGS_FILE" 
	exit 1
}

if test "$1" = print; then
	ONLY_PRINT=yes
	shift
fi

if test "$#" != 1 && test "$#" != 2; then
	echo Invalid argument count
	usage
fi

if test "$1" != on && test "$1" != off; then
	echo Invalid argument $1 - should be on or off
	usage
fi


DESIRED_SETTING="$1"

if test "$DESIRED_SETTING" = on; then
	if test "$#" != 2; then
		echo on setting requires additional optimization runs argument
		usage
	fi
	DESIRED_RUNS="$2"
else
	if test "$#" = 2; then
		echo off setting takes no additional argument
		usage
	fi
fi
	


TRUFFLE_DIR="$BIN_DIR"/../node_modules/.bin
TRUFFLE_FILE="$TRUFFLE_DIR"/truffle
TRUFFLE_BACKUP="$TRUFFLE_DIR"/truffle.old

# Create backup
rm -rf "$TRUFFLE_BACKUP"
cp "$TRUFFLE_FILE" "$TRUFFLE_BACKUP"


get_current_setting() {
	MATCHES_ENABLED=`fgrep 'enabled: ' $TRUFFLE_FILE | wc -l`
	   MATCHES_RUNS=`fgrep 'runs: '    $TRUFFLE_FILE | wc -l`

	if test "$MATCHES_ENABLED" != 1; then
		echo "Error: Could not find expected single 'enabled: ' line in $TRUFFLE_FILE (possible truffle version mismatch)"
		exit 1
	fi

	if test "$MATCHES_RUNS" != 1; then
		echo "Error: Could not find expected single 'runs: ' line in $TRUFFLE_FILE (possible truffle version mismatch)"
		exit 1
	fi

	if fgrep 'enabled: true' $TRUFFLE_FILE >/dev/null 2>&1; then
		echo on
	elif fgrep 'enabled: false' $TRUFFLE_FILE >/dev/null 2>&1; then
		echo off
	else
		echo "Error: Invalid argument to 'enabled: ' line in $TRUFFLE_FILE"
		exit 1
	fi
}

get_current_runs() {
	# Assume "runs" line has already been checked to be avaialble
	fgrep 'runs: ' $TRUFFLE_FILE
}

CURRENT_SETTING=`get_current_setting`
if test "$DESIRED_SETTING" = on; then
	CURRENT_RUNS=`get_current_runs`
fi





if test "$CURRENT_SETTING" = "$DESIRED_SETTING"; then
	if test "$DESIRED_SETTING" = on && test "$CURRENT_RUNS" != "$DESIRED_RUNS"; then
		# Already on, but at a different optimization runs level
		# TODO CURRENT_RUNS still contains "runs: " prefix so current/desired are never
		# regarded as equal
		:
	else
		printf "Already $CURRENT_SETTING"
		if test "$CURRENT_SETTING" = on; then
			echo , runs $DESIRED_RUNS
		else
			echo
		fi
		exit 0
	fi
fi




 
printf "Switching to $DESIRED_SETTING"
if test "$DESIRED_SETTING" = on; then
	echo , runs level $DESIRED_RUNS

else
	echo
fi

change_setting() {
	if cat "$TRUFFLE_FILE" | sed "$1" >/tmp/truffle.tmp; then
		mv /tmp/truffle.tmp "$TRUFFLE_FILE"
	fi
}

if test "$DESIRED_SETTING" = on; then
	if test "$ONLY_PRINT" = yes; then
		echo \"enabled\": true,        >$OPT_SETTINGS_FILE
		echo \"runs\": $DESIRED_RUNS  >>$OPT_SETTINGS_FILE
	else
		change_setting 's/enabled: false/enabled: true/g'
		change_setting "s/$CURRENT_RUNS/runs: $DESIRED_RUNS/g"
	fi
else
	if test "$ONLY_PRINT" = yes; then
		echo \"enabled\": false,      >$OPT_SETTINGS_FILE
		echo \"runs\": 0             >>$OPT_SETTINGS_FILE
	else
		change_setting 's/enabled: true/enabled: false/g' 
	fi
fi

