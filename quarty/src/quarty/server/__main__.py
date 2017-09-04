import tempfile
import logging
import json
from aiohttp import web
from quarty.common import initialise_repo, install_requirements, evaluate_pipeline, execute_pipeline
from quarty.utils import QuartyException, PipelineException

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Global state variables
repo_url = None
repo_commit = None
build_path = None
config = None

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

def check_initialised():
    if not (repo_url and repo_commit and build_path and config):
        raise QuartyException("Must initialise Quarty first")

def wrapped(f):
    async def inner(request):
        resp = web.StreamResponse(status=200, reason='OK',
                              headers={'Content-Type': 'application/x-ndjson'})
        await resp.prepare(request)
        try:
            await f(request, resp)
        except PipelineException as e:
            error_message(resp, e.args[0])
            raise e
        except (QuartyException, Exception) as e:
            error_message(resp, "Quarty exception: {}".format(e))
            raise e
        return resp
    return inner

@wrapped
async def init(request, resp):
    global repo_url, repo_commit, build_path, config
    repo_url = request.query["repo_url"]
    repo_commit = request.query["repo_commit"]

    build_path = tempfile.mkdtemp()
    progress_message(resp, "Initialising repository")
    config = await initialise_repo(repo_url, repo_commit, build_path)

    progress_message(resp, "Installing requirements")
    await install_requirements(build_path)

@wrapped
async def evaluate(request, resp):
    check_initialised()
    result = await evaluate_pipeline(config['pipeline_directory'],
                                     build_path,
                                     lambda l: log_message(resp, "stdout", l),
                                     lambda l: log_message(resp, "stderr", l))

    result_message(resp, result)
    return resp

@wrapped
async def execute(request, resp):
    check_initialised()
    step = request.query["step"]
    namespace = request.query["namespace"]
    result = await execute_pipeline(config['pipeline_directory'],
                                    build_path,
                                    step,
                                    namespace,
                                    lambda l: log_message(resp, "stdout", l),
                                    lambda l: log_message(resp, "stderr", l))

    result_message(resp, result)
    return resp

app = web.Application()
app.router.add_get('/init', init)
app.router.add_get('/evaluate', evaluate)
app.router.add_get('/execute', execute)
web.run_app(app, port=8080)
