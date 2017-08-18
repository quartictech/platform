import sys
import logging
import tempfile
import os
import asyncio
import yaml

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class QuartyException(Exception):
    pass

# Borrowed from: https://kevinmccarthy.org/2016/07/25/streaming-subprocess-stdin-and-stdout-with-asyncio-in-python/
async def _read_stream(stream, cb):
    while True:
        line = await stream.readline()
        if line:
            cb(line)
        else:
            break

async def _stream_subprocess(cmd, stdout_cb, stderr_cb):
    process = await asyncio.create_subprocess_exec(*cmd,
            stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE)

    await asyncio.wait([
        _read_stream(process.stdout, stdout_cb),
        _read_stream(process.stderr, stderr_cb)
    ])
    return await process.wait()

async def _run_subprocess(cmd):
    process = await asyncio.create_subprocess_exec(*cmd)
    return await process.wait()

def load_config(path):
    with open(path, "r", encoding='utf-8') as stream:
        return yaml.load(stream)

async def initialise_repo(repo_url, repo_commit):
    temp_path = tempfile.mkdtemp()
    os.chdir(temp_path)
    logger.info("Cloning repo: %s, commit: %s", repo_url, repo_commit)
    rc = await _run_subprocess(["git", "clone", repo_url, "build"])
    if rc != 0:
        raise QuartyException("Exception while cloning code from respository: {}".format(repo_url))

    os.chdir("build")

    # Checkout revision
    rc = await _run_subprocess(["git", "checkout", repo_commit])
    if rc != 0:
        raise QuartyException("Exception while checking out commit: {}".format(repo_commit))

    try:
        # Load config
        return load_config("quartic.yml")
    except Exception as e:
        raise QuartyException("Exception loading quartic.yml: {}", e)

   
