import logging
import tempfile
import os
import json
import asyncio
import yaml

from quarty.utils import run_subprocess_checked, stream_subprocess, PipelineException, QuartyException

GIT_CLONE_MAX_RETRIES = 5
GIT_CLONE_RETRY_DELAY_SECONDS = 5

logging.basicConfig(level=logging.INFO)
log = logging.getLogger(__name__)

def load_config(path):
    with open(path, "r", encoding="utf-8") as stream:
        return yaml.load(stream)

# This is a bit of a kludge as we sometimes see authentication
# exceptions or other errors on the first attempt cloning from git
async def clone_with_retry(repo_url, root_path):
    for i in range(1, GIT_CLONE_MAX_RETRIES + 1):
        log.info("Cloning repo: %s (attempt: %s / %s)", repo_url, i, GIT_CLONE_MAX_RETRIES)
        try:
            await run_subprocess_checked(["git", "clone", repo_url, root_path],
                                         "Error while cloning code from repository")
            return
        except QuartyException:
            log.warning("Clone failed. Sleeping for %s seconds.", GIT_CLONE_RETRY_DELAY_SECONDS)
            asyncio.sleep(GIT_CLONE_RETRY_DELAY_SECONDS)
    raise QuartyException("Too many failures trying to clone from github")

async def initialise_repo(repo_url, repo_commit, root_path):
    # Clone repo
    await clone_with_retry(repo_url, root_path)

    # Checkout revision
    log.info("Checking out commit: %s", repo_commit)
    await run_subprocess_checked(["git", "-c", "advice.detatchedHead=false", "checkout", repo_commit],
                                 "Exception while checking out commit: {}".format(repo_commit),
                                 cwd=root_path)
    try:
        # Load config
        config_path = os.path.join(root_path, "quartic.yml")
        if not os.path.exists(config_path):
            raise PipelineException("No quartic.yml present")

        return load_config(config_path)
    except Exception as e:
        raise QuartyException("Exception reading quartic.yml", e)

async def install_requirements(path):
    requirements_path = os.path.join(path, "requirements.txt")
    if os.path.exists(requirements_path):
        log.info("Installing requirements.txt")
        # Checkout revision
        await run_subprocess_checked(["pip", "install", "-r", requirements_path],
                                     "Exception while installing requirements", cwd=path)
    else:
        log.info("No requirements.txt found")

async def run_wrapped(cmd, stdout_cb, stderr_cb, cwd, exception_file, action):  # pylint: disable=too-many-arguments
    try:
        rc = await stream_subprocess(cmd, stdout_cb, stderr_cb, cwd=cwd)
        if rc != 0:
            if os.path.exists(exception_file):
                exception = json.load(open(exception_file))
                raise PipelineException(exception)
            else:
                raise PipelineException(
                    "Unknown exception (no exception.json found).")
    except Exception as e:
        raise QuartyException("Exception while: {}".format(action), e)

async def evaluate_pipeline(pipeline_dir, root_path, stdout_cb, stderr_cb):
    steps_file = tempfile.mkstemp()[1]
    exception_file = tempfile.mkstemp()[1]
    cmd = ["python", "-u", "-m", "quartic.pipeline.runner", "--evaluate",
           steps_file, "--exception", exception_file, pipeline_dir]
    log.info("Executing: %s", cmd)
    await run_wrapped(cmd, stdout_cb, stderr_cb, root_path, exception_file, "evaluating pipeline")
    return json.load(open(steps_file))

async def execute_pipeline(pipeline_dir, root_path, step_id, namespace, stdout_cb, stderr_cb):  # pylint: disable=too-many-arguments
    exception_file = tempfile.mkstemp()[1]
    cmd = ["python", "-u", "-m", "quartic.pipeline.runner",
           "--execute", step_id,
           "--exception", exception_file,
           "--namespace", namespace,
           pipeline_dir]
    log.info("Executing: %s", cmd)
    await run_wrapped(cmd, stdout_cb, stderr_cb, root_path, exception_file, "executing pipeline")
