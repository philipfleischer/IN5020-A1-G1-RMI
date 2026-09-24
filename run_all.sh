#!/usr/bin/env bash
# Runs every naive/server_cache/client_cache x FIFO/OLDEST x T=50/T=20 config,
# one at a time, and saves each result under output/ with a name that will not
# get overwritten by the next run.
#
# The assignment only names three output files (naive_server.txt, server_cache.txt,
# client_cache.txt), but we also need to cover both T values and both eviction
# methods, so one file per name is not enough. We deliberately produce 10 files
# instead, one per combination, with names that show exactly which combination
# each one is. See the report and README for the full explanation.

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
    # deleting them first, this run graph instance would include every previous run
    # queue data mixed in too, which is not what we want, each config has to have its own logs.
    rm -f server_zone_*_queue_log.txt

    java -cp "$CP" com.ass1.proxy.Main > "output/logs/proxy_${tag}.log" 2>&1 &
    sleep 2

    # The Proxy hands each server a zone (ascending) as it registers.
    # We just start NUM_ZONES server processes on distinct ports and let that happen.
    for i in $(seq 1 "$NUM_ZONES"); do
        port=$((BASE_PORT + i))
        java -cp "$CP" com.ass1.server.Server "$port" "$DATASET" "$server_mode" \
            > "output/logs/server${i}_${tag}.log" 2>&1 &
    done
    # More servers to boot than before, so we give registration a bit more time.
    sleep 6

    # runs in the foreground on purpose, waits for all 3166 queries
    # to finish before we move on to starting the next config
    java -cp "$CP" com.ass1.client.Client "$mode" "$eviction" "$delay"

    # The Client.java files output filename only encodes mode+T, not the eviction method.
    # A FIFO run and an OLDEST run of the same mode would silently overwrite
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

    # One queue log per zone server now, not just one.
    # Move each into output/ tagged with both the run and its zone,
    # so plot_graphs.py picks up all of them.
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
# queue logs also end in .txt, so they have to be excluded here explicitly,
# otherwise this count is every result file PLUS every queue log added together.
echo "All done. Results in output/ - $(ls output/*.txt 2>/dev/null | grep -vc '_queue_log') output files, $(ls output/*_queue_log.txt 2>/dev/null | wc -l) queue logs."
