import tempfile
import logging
import json
import aiohttp 
from concurrent.futures import CancelledError
import aiohttp.web
from quarty.common import initialise_repo, install_requirements, evaluate_pipeline, execute_pipeline
from quarty.utils import QuartyException, PipelineException

logging.basicConfig(level=logging.INFO)
log = logging.getLogger(__name__)

class States:
    START = 0
    INITIALISE = 1
    EVALUATE = 2
    EXECUTE = 3

def send_message(ws, msg_type, **kwargs):
    j = {"type": msg_type}
    j.update(kwargs)
    ws.send_str(json.dumps(j))

def progress_message(ws, message):
    send_message(ws, "progress", message=message)

def log_message(ws, stream, line):
    send_message(ws, "log", stream=stream, line=str(line))

def result_message(ws, result):
    send_message(ws, "result", result=result)

def error_message(ws, error):
    send_message(ws, "error", detail=error)

def assert_state(state, *expected):
    if state not in set(expected):
        raise QuartyException("Expected state {} but is {}".format(
            " | ".join(expected), state))

async def initialise(build_path, repo_url, repo_commit, ws):
    progress_message(ws, "Initialising repository")
    config = await initialise_repo(repo_url, repo_commit, build_path)

    progress_message(ws, "Installing requirements")
    await install_requirements(build_path)
    result_message(ws, None)
    return config

async def evaluate(config, build_path, ws):
    result = await evaluate_pipeline(config['pipeline_directory'],
                                     build_path,
                                     lambda l: log_message(ws, "stdout", l),
                                     lambda l: log_message(ws, "stderr", l))
    result_message(ws, result)

async def execute(config, build_path, step, namespace, ws):
    await execute_pipeline(config['pipeline_directory'],
                           build_path,
                           step,
                           namespace,
                           lambda l: log_message(ws, "stdout", l),
                           lambda l: log_message(ws, "stderr", l))

    result_message(ws, None)

async def decode_message(raw_msg):
    if raw_msg.type == aiohttp.WSMsgType.TEXT:
        return json.loads(raw_msg.data)
    else:
        raise QuartyException("Error")

async def websocket_handler(request):
    logging.info("Registering websocket connection")
    ws = aiohttp.web.WebSocketResponse()
    await ws.prepare(request)

    state = States.START
    config = None
    build_path = tempfile.mkdtemp()

    try:
        async for raw_msg in ws:
            log.info("Received message: %s", raw_msg)
            msg = await decode_message(raw_msg)
            if msg["type"] == "initialise":
                assert_state(state, States.START)
                repo_url = msg["repo_url"]
                repo_commit = msg["repo_commit"]
                config = await initialise(build_path, repo_url, repo_commit, ws)
                log.info(config)
                state = States.INITIALISE
                log.info("done")
            elif msg["type"] == "evaluate":
                assert_state(state, States.INITIALISE)
                await evaluate(config, build_path, ws)
                state = States.EVALUATE
            elif msg["type"] == "execute":
                assert_state(state, States.EVALUATE, States.EXECUTE)
                step = msg["step"]
                namespace = msg["namespace"]
                await execute(config, build_path, step, namespace, ws)
                state = States.EXECUTE
    except PipelineException as e:
        log.exception("Exception while running pipeline")
        error_message(ws, e.args[0])
    except CancelledError:
        pass
    except (QuartyException, Exception) as e:
        log.exception("Something weird happenned")
        error_message(ws, "Quarty exception: {}".format(e))
    finally:
        log.info("Closing WebSocket connection")
        await ws.close()
    return ws
app = aiohttp.web.Application()
app.router.add_get('/', websocket_handler)
aiohttp.web.run_app(app, port=8080)
