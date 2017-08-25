import sys
import os
import subprocess
import tempfile
import yaml
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

QUARTIC_REPO = os.environ["QUARTIC_REPO"]
QUARTIC_COMMIT = os.environ["QUARTIC_COMMIT_REF"]
QUARTIC_PHASE = "test"

def load_config(path):
    with open(path, "r", encoding='utf-8') as stream:
        return yaml.load(stream)

if __name__ == "__main__":
    temp_path = tempfile.mkdtemp()
    os.chdir(temp_path)
    try:
        logger.info("Cloning repo: %s, commit: %s", QUARTIC_REPO, QUARTIC_COMMIT)
        subprocess.check_call(["git", "clone", QUARTIC_REPO, "build"])
        os.chdir("build")

        # Checkout revision
        subprocess.check_call(["git", "checkout", QUARTIC_COMMIT])

        # Load config
        config = load_config("quartic.yml")

        # Execute phase
        cmd = config[QUARTIC_PHASE]
        with tempfile.NamedTemporaryFile("w") as f:
            f.write(cmd)
            f.flush()
            logger.info("Executing %s", f.name)
            exit_code = subprocess.check_call(["bash", f.name])
            logger.info("Exit code: %s", exit_code)
    except subprocess.CalledProcessError as  e:
        logger.error(e)
        sys.exit(1)
    

