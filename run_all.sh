#!/usr/bin/env bash
# Runs every naive/server_cache/client_cache x FIFO/OLDEST x T=50/T=20 config,
# one at a time, and saves each result under output/ with a name that won't
# get overwritten by the next run. Edit CONFIGS below if the group confirms
# fewer runs are actually needed (see the TA question about 6 vs 10).

CP="target/classes"
DATASET="data/exercise_1_dataset.csv"
BASE_PORT=2000
NUM_ZONES=5

# columns: mode  server_cache_mode  eviction  delay(T)
# mode/eviction/delay get passed straight to Client.java's args.
# server_cache_mode gets passed to Server.java's cache-mode arg (args[2]).
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
    # The queue log files are opened in APPEND mode by Server.java, so without
    # deleting them first, this run's graphs would include every previous run's
    # queue data mixed in too - not what we want, each config needs its own logs.
    rm -f server_zone_*_queue_log.txt

    java -cp "$CP" com.ass1.proxy.Main > "output/logs/proxy_${tag}.log" 2>&1 &
    sleep 2

    # Zone numbers are no longer picked here - the Proxy hands each server a
    # zone (ascending) as it registers. We just start NUM_ZONES server
    # processes on distinct ports and let that happen.
    for i in $(seq 1 "$NUM_ZONES"); do
        port=$((BASE_PORT + i))
        java -cp "$CP" com.ass1.server.Server "$port" "$DATASET" "$server_mode" \
            > "output/logs/server${i}_${tag}.log" 2>&1 &
    done
    # More servers to boot than before, so give registration a bit more time.
    sleep 6

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

    # One queue log per zone server now (server_zone_1_queue_log.txt .. _5_), not
    # just one - move each into output/ tagged with both the run and its zone,
    # so plot_graphs.py (which just globs *_queue_log.txt) picks up all of them.
    for f in server_zone_*_queue_log.txt; do
        [ -f "$f" ] || continue
        zone=$(echo "$f" | grep -oE '[0-9]+' | head -1)
        mv "$f" "output/${tag}_zone${zone}_queue_log.txt"
    done

    stop_everything
}

mkdir -p output/logs output/graphs
echo "Clearing old results from output/ (so nothing stale from a previous run is left behind)..."
rm -f output/*.txt output/graphs/*.png output/logs/*.log

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
