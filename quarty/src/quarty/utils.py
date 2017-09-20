import asyncio

class PipelineException(Exception):
    "Exception in evaluating the pipeline. May be propagated to user"
    pass

class QuartyException(Exception):
    "Exception in Quarty. Should not be propagated to user"
    pass

# Borrowed from: https://kevinmccarthy.org/2016/07/25/streaming-subprocess-stdin-and-stdout-with-asyncio-in-python/
async def _read_stream(stream, cb):
    while True:
        line = await stream.readline()
        if line:
            cb(line.decode().rstrip())
        else:
            break

async def stream_subprocess(cmd, stdout_cb, stderr_cb, **kwargs):
    process = await asyncio.create_subprocess_exec(*cmd,
                                                   stdout=asyncio.subprocess.PIPE, stderr=asyncio.subprocess.PIPE,
                                                   **kwargs)
    await asyncio.wait([
        _read_stream(process.stdout, stdout_cb),
        _read_stream(process.stderr, stderr_cb)
    ])
    return await process.wait()

async def _run_subprocess(cmd, **kwargs):
    process = await asyncio.create_subprocess_exec(*cmd, **kwargs)
    return await process.wait()

async def run_subprocess_checked(cmd, exception, **kwargs):
    rc = await _run_subprocess(cmd, **kwargs)
    if rc != 0:
        raise QuartyException(exception)


