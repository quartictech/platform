from aiohttp import web
import asyncio
import logging
from quarty.common import initialise_repo
import json

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

async def progress_message(resp, message):
    message = json.dumps({
        "type": "progress",
        "message": message
    })
    resp.write("{}\n".format(message).encode())

async def fetch_dag(request):
    repo_url = request.query["repo_url"]
    repo_commit = request.query["repo_commit"]
    resp = web.StreamResponse(status=200, reason='OK',
                              headers={'Content-Type': 'application/x-ndjson'})
    await resp.prepare(request)

    await progress_message(resp, "Cloning repository")
    config = await initialise_repo(repo_url, repo_commit)
    await progress_message(resp, "Initialising repository")

    return resp

if __name__ == "__main__":
    app = web.Application()
    app.router.add_get('/dag', fetch_dag)
    web.run_app(app, port=8080)
