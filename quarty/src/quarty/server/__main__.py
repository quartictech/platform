import tempfile
import os
import asyncio
import logging
import json
from aiohttp import web
from quarty.common import initialise_repo, evaluate, QuartyException

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def send_message(resp, j):
    resp.write("{}\n".format(json.dumps(j)).encode())

def progress_message(resp, message):
    send_message(resp, {
        "type": "progress",
        "message": message
    })

def log_message(resp, stream, line):
    send_message(resp, {
        "type": "log",
        "stream": stream,
        "line": str(line)
    })

def result_message(resp, result):
    send_message(resp, {
        "type": "result",
        "result": result
    })

def error_message(resp, error):
    send_message(resp, {
        "type": "error",
        "detail": error
    })

async def pipeline(request):
    repo_url = request.query["repo_url"]
    repo_commit = request.query["repo_commit"]
    resp = web.StreamResponse(status=200, reason='OK',
                              headers={'Content-Type': 'application/x-ndjson'})
    await resp.prepare(request)
    progress_message(resp, "Cloning repository")

    temp_path = tempfile.mkdtemp()
    os.chdir(temp_path)
    config = await initialise_repo(repo_url, repo_commit)
    progress_message(resp, "Initialising repository")

    runner = config["runner"]
    if runner["type"] == "python":
        path = runner.get("path", "src/")
        modules = runner["modules"]

        try:
            result = await evaluate(path, modules, 
                                    lambda l: log_message(resp, "stdout", l), 
                                    lambda l: log_message(resp, "stderr", l))

            result_message(resp, result)        
        except QuartyException as e: 
            error_message(resp, e.args[0])

    return resp

if __name__ == "__main__":
    app = web.Application()
    app.router.add_get('/pipeline', pipeline)
    web.run_app(app, port=8080)
