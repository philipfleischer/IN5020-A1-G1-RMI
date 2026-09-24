#!/usr/bin/env python3
"""
Reads every results/*.txt output file and *_queue_log.txt file that run_all.sh
produces, and draws two graphs:
  1) turnaround time per query, one graph per output file.
  2) queue length over time, one graph per server run.
Run this AFTER run_all.sh has finished.
Needs matplotlib:
    pip install matplotlib
"""
import re
import glob
import os
import matplotlib.pyplot as plt

RESULTS_DIR = "output"
OUT_DIR = "output/graphs"


def plot_turnaround(output_file):
    turnarounds = []
    with open(output_file) as f:
        for line in f:
            # skip the 4 summary lines at the bottom and any failed queries.
            if line.startswith("ERROR") or "avg turn-around" in line:
                continue
            match = re.search(r"turnaround time: (\d+) ms", line)
            if match:
                turnarounds.append(int(match.group(1)))

    if not turnarounds:
        print(f"  (skipped {output_file} - no turnaround data found)")
        return

    plt.figure()
    plt.plot(range(1, len(turnarounds) + 1), turnarounds, linewidth=0.8)
    plt.xlabel("Query number")
    plt.ylabel("Turnaround time (ms)")
    plt.title(os.path.basename(output_file))
    name = os.path.splitext(os.path.basename(output_file))[0]
    plt.savefig(f"{OUT_DIR}/{name}_turnaround.png", dpi=120)
    plt.close()


def plot_queue_log(log_file):
    # format is "timestamp,queueSize" per line.
    timestamps, sizes = [], []
    with open(log_file) as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            ts, size = line.split(",")
            timestamps.append(int(ts))
            sizes.append(int(size))

    if not timestamps:
        print(f"  (skipped {log_file} - empty)")
        return

    # X-axis as timestamps. Keep it relative to when the run started in seconds.
    start = timestamps[0]
    seconds = [(t - start) / 1000 for t in timestamps]

    bucketed = {}
    for t, size in zip(seconds, sizes):
        bucket = int(t)
        bucketed[bucket] = max(bucketed.get(bucket, 0), size)
    bucket_seconds = sorted(bucketed)
    bucket_sizes = [bucketed[b] for b in bucket_seconds]

    plt.figure(figsize=(12, 4))
    plt.step(bucket_seconds, bucket_sizes, where="post")
    plt.xlabel("Seconds since run start")
    plt.ylabel("Queue length")
    plt.title(os.path.basename(log_file))
    name = os.path.splitext(os.path.basename(log_file))[0]
    plt.savefig(f"{OUT_DIR}/{name}_queue.png", dpi=120)
    plt.close()


def main():
    os.makedirs(OUT_DIR, exist_ok=True)

    output_files = [f for f in glob.glob(f"{RESULTS_DIR}/*.txt") if "queue_log" not in f]
    print(f"Found {len(output_files)} output file(s) - plotting turnaround graphs...")
    for f in output_files:
        plot_turnaround(f)

    queue_logs = glob.glob(f"{RESULTS_DIR}/*_queue_log.txt")
    print(f"Found {len(queue_logs)} queue log(s) - plotting queue graphs...")
    for f in queue_logs:
        plot_queue_log(f)

    print(f"Done - graphs saved in {OUT_DIR}/")


if __name__ == "__main__":
    main()
