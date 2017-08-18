import sys
import logging
import tempfile
import os
import asyncio
import json
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

async def evaluate(path, modules, stdout_cb, stderr_cb):
    cmd = ["python", "-u", "-m", "quartic.pipeline", "--evaluate", 
           "../steps.json", "--exception", "../exception.json"] + modules
    logger.info("Executing: %s", cmd)
    rc = await _stream_subprocess(cmd, stdout_cb, stderr_cb)
    if rc != 0:
        exception = json.load(open("../exception.json"))
        raise QuartyException(exception)
    else:
        return json.load(open("../steps.json"))



   
