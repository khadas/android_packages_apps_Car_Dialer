#!/usr/bin/python3
import subprocess
import sys

process = subprocess.Popen(['lint', '--check', 'UnusedResources', sys.argv[1]],
                          stdout=subprocess.PIPE,
                          stderr=subprocess.PIPE)

stdout, stderr = process.communicate()
stdout = stdout.decode('utf-8')

results = []
for line in stdout.split('\n'):
    if '[UnusedResources]' in line:
        results.append(line)

if len(results) > 0:
    print('\n'.join(results))
    sys.exit(1)
else:
    sys.exit(0)

