#!/usr/bin/env bash
# Runs every naive/server_cache/client_cache x FIFO/OLDEST x T=50/T=20 config,
# one at a time, and saves each result under output/ with a name that won't
# get overwritten by the next run. Edit CONFIGS below if the group confirms
# fewer runs are actually needed (see the TA question about 6 vs 10).

CP="target/classes"
DATASET="data/exercise_1_dataset.csv"
PORT=2000
ZONE=1

# columns: mode  server_cache_mode  eviction  delay(T)
# mode/eviction/delay get passed straight to Client.java's args.
# server_cache_mode gets passed to Server.java's cache-mode arg (args[3]).
CONFIGS=(
  "naive        none   FIFO   50"
  "naive        none   FIFO   20"
  "server_cache FIFO   FIFO   50"
  "server_cache FIFO   FIFO   20"
  "server_cache OLDEST OLDEST 50"
  "server_cache OLDEST OLDEST 20"
  "client_cache FIFO   FIFO   50"
  "client_cache FIFO   FIFO   20"
  "client_cache OLDEST OLDEST 50"
  "client_cache OLDEST OLDEST 20"
)

stop_everything() {
    pkill -f "com.ass1.proxy.Main" 2>/dev/null
    pkill -f "com.ass1.server.Server" 2>/dev/null
    sleep 1
}

run_one() {
    local mode=$1 server_mode=$2 eviction=$3 delay=$4
    local tag
    if [ "$mode" = "naive" ]; then
        tag="${mode}_T${delay}"
    else
        tag="${mode}_${eviction}_T${delay}"
    fi

    echo ""
    echo "=== [$(date +%H:%M:%S)] Running: $tag ==="

    stop_everything
    # the queue log file is opened in APPEND mode by Server.java, so without
    # deleting it first, this run's graph would include every previous run's
    # queue data mixed in too - not what we want, each config needs its own log.
    rm -f "server_zone_${ZONE}_queue_log.txt"

    java -cp "$CP" com.ass1.proxy.Main > "output/logs/proxy_${tag}.log" 2>&1 &
    sleep 2
    java -cp "$CP" com.ass1.server.Server $PORT $ZONE "$DATASET" "$server_mode" > "output/logs/server_${tag}.log" 2>&1 &
    sleep 3

    # runs in the foreground on purpose - waits for all ~3166 queries to finish
    # before we move on to starting the next config
    java -cp "$CP" com.ass1.client.Client "$mode" "$eviction" "$delay"

    # Client.java's output filename only encodes mode+T, not the eviction method -
    # so a FIFO run and an OLDEST run of the same mode would silently overwrite
    # each other without this rename step.
    local base
    case "$mode" in
        naive) base="naive_server" ;;
        server_cache) base="server_cache" ;;
        client_cache) base="client_cache" ;;
    esac

    if [ -f "${base}_T${delay}.txt" ]; then
        mv "${base}_T${delay}.txt" "output/${tag}.txt"
        echo "Saved -> output/${tag}.txt"
    else
        echo "!! WARNING: expected output file ${base}_T${delay}.txt was not created - something failed, check output/logs/"
    fi

    if [ -f "server_zone_${ZONE}_queue_log.txt" ]; then
        mv "server_zone_${ZONE}_queue_log.txt" "output/${tag}_queue_log.txt"
    fi

    stop_everything
}

mkdir -p output/logs
echo "Compiling..."
mvn -q clean compile

for row in "${CONFIGS[@]}"; do
    run_one $row
done

stop_everything
echo ""
# queue logs also end in .txt, so they have to be excluded here explicitly -
# otherwise this count is every result file PLUS every queue log added together.
echo "All done. Results in output/ - $(ls output/*.txt 2>/dev/null | grep -vc '_queue_log') output files, $(ls output/*_queue_log.txt 2>/dev/null | wc -l) queue logs."
